const http = require('http');

const PORT = process.env.PORT || 3000;

// In-memory store: userCode -> { username, uuid, lastSeen, tunnelUrl, online, isHosting }
const players = {};
let invites = [];

// Automatic cleanup configuration
const HEARTBEAT_TIMEOUT_MS = 60 * 1000; // 60 seconds inactivity timeout
const INVITE_EXPIRATION_MS = 5 * 60 * 1000; // 5 minutes invite timeout
const CODE_CHARS = '23456789ABCDEFGHJKLMNPQRSTUVWXYZ'; // Alphanumeric excluding 0, 1, I, O

function generateFriendCode() {
    let code;
    let attempts = 0;
    do {
        let part = '';
        for (let i = 0; i < 4; i++) {
            part += CODE_CHARS.charAt(Math.floor(Math.random() * CODE_CHARS.length));
        }
        code = `PANT-${part}`;
        attempts++;
    } while (players[code] && attempts < 100);
    return code;
}

function normalizeCode(code) {
    if (!code || typeof code !== 'string') return '';
    return code.trim().toUpperCase();
}

// Single consolidated cleanup loop (runs every 10 seconds)
setInterval(() => {
    const now = Date.now();
    for (const [code, p] of Object.entries(players)) {
        if (p.online && (now - p.lastSeen > HEARTBEAT_TIMEOUT_MS)) {
            p.online = false;
            p.isHosting = false;
            p.tunnelUrl = '';
            console.log(`[Timeout] ${p.username} (${code}) inaktif duruma gecti, cevrim disi yapildi.`);
        }
    }
    const beforeCount = invites.length;
    invites = invites.filter(inv => now - inv.timestamp < INVITE_EXPIRATION_MS);
    if (invites.length < beforeCount) {
        console.log(`[Temizlik] ${beforeCount - invites.length} suresi dolmus davet temizlendi.`);
    }
}, 10000);

function parseJson(req) {
    return new Promise((resolve) => {
        let body = '';
        req.on('data', chunk => {
            body += chunk.toString();
        });
        req.on('end', () => {
            try {
                resolve(body ? JSON.parse(body) : {});
            } catch (e) {
                resolve({});
            }
        });
    });
}

function sendJson(res, statusCode, data) {
    res.writeHead(statusCode, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify(data));
}

const server = http.createServer(async (req, res) => {
    // Enable CORS
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

    if (req.method === 'OPTIONS') {
        res.writeHead(200);
        res.end();
        return;
    }

    const urlParts = req.url.split('?');
    const pathname = urlParts[0];

    // --- GET Endpointleri ---
    if (req.method === 'GET') {
        if (pathname === '/' || pathname === '/api/health') {
            const onlineCount = Object.values(players).filter(p => p.online).length;
            const hostingCount = Object.values(players).filter(p => p.online && p.isHosting).length;
            sendJson(res, 200, {
                status: 'ok',
                service: 'pantial-presence-server',
                version: '1.1.0',
                uptimeSeconds: Math.floor(process.uptime()),
                onlinePlayers: onlineCount,
                activeHosts: hostingCount,
                timestamp: Date.now()
            });
            return;
        }

        if (pathname === '/api/generate-code') {
            const newCode = generateFriendCode();
            sendJson(res, 200, {
                userCode: newCode,
                format: 'PANT-XXXX',
                success: true
            });
            return;
        }

        if (pathname === '/api/status') {
            const query = new URLSearchParams(urlParts[1] || '');
            const rawCode = query.get('userCode') || query.get('code');
            const userCode = normalizeCode(rawCode);

            if (!userCode || !players[userCode]) {
                sendJson(res, 200, {
                    userCode: userCode || '',
                    online: false,
                    isHosting: false,
                    tunnelUrl: ''
                });
                return;
            }

            const p = players[userCode];
            sendJson(res, 200, {
                userCode,
                username: p.username,
                uuid: p.uuid,
                online: p.online,
                isHosting: p.isHosting,
                tunnelUrl: p.tunnelUrl,
                lastSeen: p.lastSeen
            });
            return;
        }
    }

    // --- POST Endpointleri ---
    if (req.method === 'POST') {
        const data = await parseJson(req);
        const rawCode = data.userCode;
        const userCode = normalizeCode(rawCode);

        if (pathname === '/api/invite') {
            const from = normalizeCode(data.from);
            const to = normalizeCode(data.to);
            const tunnelUrl = data.tunnelUrl;

            if (!from || !to || !tunnelUrl) {
                sendJson(res, 400, { error: 'Eksik parametreler (from, to, tunnelUrl gereklidir)' });
                return;
            }
            const sender = players[from];
            const senderName = sender ? sender.username : 'Bilinmeyen Oyuncu';
            const senderUuid = sender ? sender.uuid : '';

            // Ayni cift arasindaki eski daveti sil
            invites = invites.filter(inv => !(inv.from === from && inv.to === to));

            invites.push({
                from,
                senderName,
                senderUuid,
                to,
                tunnelUrl,
                timestamp: Date.now()
            });
            console.log(`[Davet] ${senderName} (${from}) -> (${to}) dunyaya davet etti: ${tunnelUrl}`);
            sendJson(res, 200, { success: true });
            return;
        }

        if (pathname === '/api/reject-invite') {
            const from = normalizeCode(data.from);
            const to = normalizeCode(data.to);
            if (!from || !to) {
                sendJson(res, 400, { error: 'Eksik parametreler (from, to gereklidir)' });
                return;
            }
            invites = invites.filter(inv => !(inv.from === from && inv.to === to));
            console.log(`[Davet Reddi] ${from} tarafindan ${to} kullanicisina yapilan davet reddedildi.`);
            sendJson(res, 200, { success: true });
            return;
        }

        if (pathname === '/api/register') {
            const { username, uuid } = data;
            if (!username) {
                sendJson(res, 400, { error: 'Kullanici adi (username) gereklidir' });
                return;
            }

            const assignedCode = userCode || generateFriendCode();

            players[assignedCode] = {
                username,
                uuid: uuid || '',
                lastSeen: Date.now(),
                tunnelUrl: players[assignedCode] ? players[assignedCode].tunnelUrl : '',
                isHosting: players[assignedCode] ? players[assignedCode].isHosting : false,
                online: true
            };
            console.log(`[Kayit] ${username} (${assignedCode}) sisteme baglandi.`);
            sendJson(res, 200, { success: true, userCode: assignedCode });
            return;
        }

        if (!userCode) {
            sendJson(res, 400, { error: 'Gecersiz veya eksik userCode' });
            return;
        }

        if (pathname === '/api/host') {
            const { tunnelUrl } = data;
            if (players[userCode]) {
                players[userCode].tunnelUrl = tunnelUrl || '';
                players[userCode].isHosting = true;
                players[userCode].lastSeen = Date.now();
                players[userCode].online = true;
                console.log(`[Host] ${players[userCode].username} (${userCode}) dunya paylasimi acti: ${tunnelUrl || 'bekleniyor'}`);
            }
            sendJson(res, 200, { success: true });
            return;
        }

        if (pathname === '/api/stop-host') {
            if (players[userCode]) {
                players[userCode].tunnelUrl = '';
                players[userCode].isHosting = false;
                players[userCode].lastSeen = Date.now();
                console.log(`[Stop Host] ${players[userCode].username} (${userCode}) dunya paylasimini kapatti.`);
            }
            sendJson(res, 200, { success: true });
            return;
        }

        if (pathname === '/api/status') {
            const friendCodes = (data.friendCodes || []).map(normalizeCode);

            // Gonderenin son gorulme ve cevrim ici bilgisini guncelle
            if (players[userCode]) {
                players[userCode].lastSeen = Date.now();
                players[userCode].online = true;
            }

            // Arkadaslarin anlik durumlari
            const friendsStatus = friendCodes.map(code => {
                const friend = players[code];
                return {
                    userCode: code,
                    username: friend ? friend.username : 'Bilinmeyen Oyuncu',
                    uuid: friend ? friend.uuid : '',
                    online: friend ? friend.online : false,
                    tunnelUrl: friend ? friend.tunnelUrl : '',
                    isHosting: friend ? (friend.isHosting || false) : false
                };
            });

            // Bu oyuncuya gelen aktif davetler
            const now = Date.now();
            const activeInvites = invites.filter(inv => inv.to === userCode && (now - inv.timestamp < INVITE_EXPIRATION_MS))
                .map(inv => ({
                    senderCode: inv.from,
                    senderName: inv.senderName,
                    senderUuid: inv.senderUuid || '',
                    tunnelUrl: inv.tunnelUrl,
                    timestamp: inv.timestamp
                }));

            sendJson(res, 200, { friends: friendsStatus, invites: activeInvites });
            return;
        }
    }

    sendJson(res, 404, { error: 'Endpoint bulunamadi' });
});

if (require.main === module) {
    server.listen(PORT, () => {
        console.log(`Pantial Varlik Sunucusu port ${PORT} uzerinde calisiyor.`);
    });
}

module.exports = { server, players, invites, generateFriendCode, normalizeCode };
