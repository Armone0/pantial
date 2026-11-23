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
}, 10000).unref();

const MAX_BODY_BYTES = 16 * 1024;
const MAX_FRIEND_CODES = 200;
const CODE_PATTERN = /^PANT-[23456789ABCDEFGHJKLMNPQRSTUVWXYZ]{4}$/;

function parseJson(req) {
    return new Promise((resolve) => {
        let body = '';
        req.on('data', chunk => {
            body += chunk.toString();
            if (body.length > MAX_BODY_BYTES) {
                body = '';
                req.destroy();
            }
        });
        req.on('end', () => {
            try {
                resolve(body ? JSON.parse(body) : {});
            } catch (e) {
                resolve({});
            }
        });
        req.on('error', () => {
            resolve({});
        });
    });
}

// Kod sahipligi: ilk kayitta istemcinin gonderdigi secret koda baglanir, sonraki
// tum yazma/okuma istekleri ayni secret'i gostermek zorunda. Boylece biri baskasinin
// kodunu alip arkadaslarina sahte bir dunya adresi duyuramaz.
function isValidSecret(secret) {
    return typeof secret === 'string' && secret.length >= 16 && secret.length <= 128;
}

function authorize(userCode, secret) {
    const p = players[userCode];
    return !!p && isValidSecret(secret) && p.secret === secret;
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
                version: require('./package.json').version,
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

        if (pathname === '/api/players') {
            const playerList = Object.entries(players).map(([code, p]) => ({
                userCode: code,
                username: p.username,
                uuid: p.uuid,
                online: p.online,
                isHosting: p.isHosting,
                lastSeen: p.lastSeen
            }));
            sendJson(res, 200, {
                totalCount: playerList.length,
                onlineCount: playerList.filter(p => p.online).length,
                hostingCount: playerList.filter(p => p.online && p.isHosting).length,
                players: playerList
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
                    isHosting: false
                });
                return;
            }

            // Tunel adresi yalnizca kimligi dogrulanmis arkadas sorgusunda (POST /api/status) verilir.
            const p = players[userCode];
            sendJson(res, 200, {
                userCode,
                username: p.username,
                uuid: p.uuid,
                online: p.online,
                isHosting: p.isHosting,
                lastSeen: p.lastSeen
            });
            return;
        }
    }

    // --- POST Endpointleri ---
    if (req.method === 'POST') {
        const data = await parseJson(req);
        const userCode = normalizeCode(data.userCode);
        const secret = data.secret;

        if (pathname === '/api/register') {
            const username = typeof data.username === 'string' ? data.username.trim().slice(0, 32) : '';
            const uuid = typeof data.uuid === 'string' ? data.uuid.slice(0, 64) : '';
            if (!username) {
                sendJson(res, 400, { error: 'Kullanici adi (username) gereklidir' });
                return;
            }
            if (!isValidSecret(secret)) {
                sendJson(res, 400, { error: 'Gecerli bir secret (16-128 karakter) gereklidir' });
                return;
            }
            if (userCode && !CODE_PATTERN.test(userCode)) {
                sendJson(res, 400, { error: 'Gecersiz kod formati (PANT-XXXX)' });
                return;
            }

            const assignedCode = userCode || generateFriendCode();
            const existing = players[assignedCode];
            if (existing && existing.secret !== secret) {
                sendJson(res, 403, { error: 'Bu kod baska bir oyuncuya ait' });
                return;
            }

            players[assignedCode] = {
                username,
                uuid: uuid || (existing ? existing.uuid : ''),
                secret,
                lastSeen: Date.now(),
                tunnelUrl: existing ? existing.tunnelUrl : '',
                isHosting: existing ? existing.isHosting : false,
                online: true
            };
            console.log(`[Kayit] ${username} (${assignedCode}) sisteme baglandi.`);
            sendJson(res, 200, { success: true, userCode: assignedCode });
            return;
        }

        const knownEndpoints = ['/api/host', '/api/stop-host', '/api/status', '/api/invite', '/api/reject-invite'];
        if (!knownEndpoints.includes(pathname)) {
            sendJson(res, 404, { error: 'Endpoint bulunamadi' });
            return;
        }
        if (!userCode) {
            sendJson(res, 400, { error: 'Gecersiz veya eksik userCode' });
            return;
        }
        if (!authorize(userCode, secret)) {
            // 401: istemci yeniden /api/register yapmali (orn. sunucu yeniden basladi)
            sendJson(res, 401, { error: 'Kimlik dogrulanamadi, once /api/register' });
            return;
        }
        const me = players[userCode];
        me.lastSeen = Date.now();
        me.online = true;

        if (pathname === '/api/invite') {
            const to = normalizeCode(data.to);
            const tunnelUrl = typeof data.tunnelUrl === 'string' ? data.tunnelUrl.trim().slice(0, 255) : '';
            if (!to || !tunnelUrl) {
                sendJson(res, 400, { error: 'Eksik parametreler (to, tunnelUrl gereklidir)' });
                return;
            }

            // Ayni cift arasindaki eski daveti sil
            invites = invites.filter(inv => !(inv.from === userCode && inv.to === to));
            invites.push({
                from: userCode,
                senderName: me.username,
                senderUuid: me.uuid,
                to,
                tunnelUrl,
                timestamp: Date.now()
            });
            console.log(`[Davet] ${me.username} (${userCode}) -> (${to}) dunyaya davet etti: ${tunnelUrl}`);
            sendJson(res, 200, { success: true });
            return;
        }

        if (pathname === '/api/reject-invite') {
            // Yalnizca alici kendisine gelen daveti silebilir
            const from = normalizeCode(data.from);
            if (!from) {
                sendJson(res, 400, { error: 'Eksik parametre (from gereklidir)' });
                return;
            }
            invites = invites.filter(inv => !(inv.from === from && inv.to === userCode));
            console.log(`[Davet Reddi] ${userCode}, ${from} kullanicisinin davetini kapatti.`);
            sendJson(res, 200, { success: true });
            return;
        }

        if (pathname === '/api/host') {
            const tunnelUrl = typeof data.tunnelUrl === 'string' ? data.tunnelUrl.trim().slice(0, 255) : '';
            me.tunnelUrl = tunnelUrl;
            me.isHosting = true;
            console.log(`[Host] ${me.username} (${userCode}) dunya paylasimi acti: ${tunnelUrl || 'bekleniyor'}`);
            sendJson(res, 200, { success: true });
            return;
        }

        if (pathname === '/api/stop-host') {
            me.tunnelUrl = '';
            me.isHosting = false;
            console.log(`[Stop Host] ${me.username} (${userCode}) dunya paylasimini kapatti.`);
            sendJson(res, 200, { success: true });
            return;
        }

        if (pathname === '/api/status') {
            const rawCodes = Array.isArray(data.friendCodes) ? data.friendCodes.slice(0, MAX_FRIEND_CODES) : [];
            const friendCodes = rawCodes.map(normalizeCode).filter(Boolean);

            const friendsStatus = friendCodes.map(code => {
                const friend = players[code];
                const online = friend ? friend.online : false;
                return {
                    userCode: code,
                    username: friend ? friend.username : 'Bilinmeyen Oyuncu',
                    uuid: friend ? friend.uuid : '',
                    online,
                    tunnelUrl: friend && online ? friend.tunnelUrl : '',
                    isHosting: friend && online ? !!friend.isHosting : false
                };
            });

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

module.exports = { server, players, getInvites: () => invites, generateFriendCode, normalizeCode };
