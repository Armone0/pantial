const http = require('http');

const PORT = process.env.PORT || 3000;
const VERSION = require('./package.json').version;

/*
 * Bellekteki durum. Sunucu yeniden baslarsa istemciler 401 alip otomatik tekrar kayit olur,
 * arkadas listeleri de her yoklamada istemciden yeniden gelir; kalici depolamaya gerek yok.
 *
 * players[code] = { username, uuid, secret, lastSeen, online, isHosting, tunnelUrl,
 *                   friends: Set<code>, declined: Set<code> }
 */
const players = {};
let invites = [];

const HEARTBEAT_TIMEOUT_MS = 60 * 1000;
const INVITE_EXPIRATION_MS = 5 * 60 * 1000;
const CODE_CHARS = '23456789ABCDEFGHJKLMNPQRSTUVWXYZ'; // 0, 1, I, O yok
const CODE_PATTERN = /^PANT-[23456789ABCDEFGHJKLMNPQRSTUVWXYZ]{4}$/;
const MAX_BODY_BYTES = 16 * 1024;
const MAX_FRIEND_CODES = 200;

// IP basina istek siniri: kod tahmin etmeyi ve sunucuyu bogmayi zorlastirir.
const RATE_WINDOW_MS = 60 * 1000;
const RATE_MAX_REQUESTS = Number(process.env.PANTIAL_RATE_LIMIT || 120);
const rateBuckets = new Map();

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

function normalizeCodes(list) {
    if (!Array.isArray(list)) return [];
    return [...new Set(list.slice(0, MAX_FRIEND_CODES).map(normalizeCode).filter(c => CODE_PATTERN.test(c)))];
}

/** Iki oyuncu da birbirini eklemisse arkadastirlar. Adres, davet ve UUID yalnizca o zaman paylasilir. */
function isMutual(a, b) {
    const pa = players[a], pb = players[b];
    return !!pa && !!pb && pa.friends.has(b) && pb.friends.has(a);
}

// Tek temizlik dongusu (10 sn)
setInterval(() => {
    const now = Date.now();
    for (const [code, p] of Object.entries(players)) {
        if (p.online && (now - p.lastSeen > HEARTBEAT_TIMEOUT_MS)) {
            p.online = false;
            p.isHosting = false;
            p.tunnelUrl = '';
            console.log(`[Timeout] ${p.username} (${code}) cevrim disi.`);
        }
    }
    invites = invites.filter(inv => now - inv.timestamp < INVITE_EXPIRATION_MS);
    for (const [ip, bucket] of rateBuckets) {
        if (now - bucket.start > RATE_WINDOW_MS) rateBuckets.delete(ip);
    }
}, 10000).unref();

// Render/Railway/Fly gibi bir proxy arkasinda gercek istemci IP'si X-Forwarded-For'da gelir.
// Yalnizca TRUST_PROXY=1 iken okunur; aksi halde baslik sahtelenip sinir atlatilabilirdi.
const TRUST_PROXY = process.env.TRUST_PROXY === '1';

function clientIp(req) {
    if (TRUST_PROXY) {
        const fwd = req.headers['x-forwarded-for'];
        if (typeof fwd === 'string' && fwd.trim()) return fwd.split(',')[0].trim();
    }
    return req.socket.remoteAddress || '?';
}

function rateLimited(req) {
    const ip = clientIp(req);
    const now = Date.now();
    let bucket = rateBuckets.get(ip);
    if (!bucket || now - bucket.start > RATE_WINDOW_MS) {
        bucket = { start: now, count: 0 };
        rateBuckets.set(ip, bucket);
    }
    bucket.count++;
    return bucket.count > RATE_MAX_REQUESTS;
}

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
        req.on('error', () => resolve({}));
    });
}

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

function friendView(me, code) {
    const friend = players[code];
    if (!friend) {
        return { userCode: code, state: 'unknown', username: '', uuid: '', online: false, isHosting: false, tunnelUrl: '' };
    }
    if (!isMutual(me, code)) {
        // Istek gonderildi ama karsi taraf henuz kabul etmedi: hicbir bilgi verme.
        return { userCode: code, state: 'pending', username: '', uuid: '', online: false, isHosting: false, tunnelUrl: '' };
    }
    return {
        userCode: code,
        state: 'mutual',
        username: friend.username,
        uuid: friend.uuid,
        online: friend.online,
        isHosting: friend.online && !!friend.isHosting,
        tunnelUrl: friend.online ? friend.tunnelUrl : ''
    };
}

const server = http.createServer(async (req, res) => {
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

    if (req.method === 'OPTIONS') {
        res.writeHead(200);
        res.end();
        return;
    }

    const pathname = req.url.split('?')[0];

    if (req.method === 'GET') {
        if (pathname === '/' || pathname === '/api/health') {
            const all = Object.values(players);
            sendJson(res, 200, {
                status: 'ok',
                service: 'pantial-presence-server',
                version: VERSION,
                uptimeSeconds: Math.floor(process.uptime()),
                onlinePlayers: all.filter(p => p.online).length,
                activeHosts: all.filter(p => p.online && p.isHosting).length,
                timestamp: Date.now()
            });
            return;
        }
        if (pathname === '/api/generate-code') {
            sendJson(res, 200, { userCode: generateFriendCode(), format: 'PANT-XXXX', success: true });
            return;
        }
        sendJson(res, 404, { error: 'Endpoint bulunamadi' });
        return;
    }

    if (req.method !== 'POST') {
        sendJson(res, 404, { error: 'Endpoint bulunamadi' });
        return;
    }

    if (rateLimited(req)) {
        sendJson(res, 429, { error: 'Cok fazla istek, biraz bekleyin' });
        return;
    }

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
        const code = userCode || generateFriendCode();
        const existing = players[code];
        if (existing && existing.secret !== secret) {
            sendJson(res, 403, { error: 'Bu kod baska bir oyuncuya ait' });
            return;
        }
        players[code] = {
            username,
            uuid: uuid || (existing ? existing.uuid : ''),
            secret,
            lastSeen: Date.now(),
            online: true,
            isHosting: existing ? existing.isHosting : false,
            tunnelUrl: existing ? existing.tunnelUrl : '',
            friends: existing ? existing.friends : new Set(),
            declined: existing ? existing.declined : new Set()
        };
        console.log(`[Kayit] ${username} (${code}) baglandi.`);
        sendJson(res, 200, { success: true, userCode: code });
        return;
    }

    const known = ['/api/host', '/api/stop-host', '/api/status', '/api/invite', '/api/reject-invite'];
    if (!known.includes(pathname)) {
        sendJson(res, 404, { error: 'Endpoint bulunamadi' });
        return;
    }
    if (!userCode) {
        sendJson(res, 400, { error: 'Gecersiz veya eksik userCode' });
        return;
    }
    if (!authorize(userCode, secret)) {
        sendJson(res, 401, { error: 'Kimlik dogrulanamadi, once /api/register' });
        return;
    }
    const me = players[userCode];
    me.lastSeen = Date.now();
    me.online = true;

    if (pathname === '/api/status') {
        // Istemci her yoklamada arkadas ve reddettigi istek listesini gonderir.
        me.friends = new Set(normalizeCodes(data.friendCodes).filter(c => c !== userCode));
        me.declined = new Set(normalizeCodes(data.declinedCodes));

        const friends = [...me.friends].map(code => friendView(userCode, code));

        // Gelen arkadaslik istekleri: beni eklemis ama benim eklemedigim (ve reddetmedigim) oyuncular
        const requests = Object.entries(players)
            .filter(([code, p]) => code !== userCode && p.friends.has(userCode)
                && !me.friends.has(code) && !me.declined.has(code))
            .map(([code, p]) => ({ userCode: code, username: p.username }));

        const now = Date.now();
        const activeInvites = invites
            .filter(inv => inv.to === userCode && now - inv.timestamp < INVITE_EXPIRATION_MS && isMutual(userCode, inv.from))
            .map(inv => ({
                senderCode: inv.from,
                senderName: inv.senderName,
                senderUuid: inv.senderUuid || '',
                tunnelUrl: inv.tunnelUrl,
                timestamp: inv.timestamp
            }));

        sendJson(res, 200, { friends, requests, invites: activeInvites });
        return;
    }

    if (pathname === '/api/invite') {
        const to = normalizeCode(data.to);
        const tunnelUrl = typeof data.tunnelUrl === 'string' ? data.tunnelUrl.trim().slice(0, 255) : '';
        if (!to || !tunnelUrl) {
            sendJson(res, 400, { error: 'Eksik parametreler (to, tunnelUrl gereklidir)' });
            return;
        }
        if (!isMutual(userCode, to)) {
            sendJson(res, 403, { error: 'Yalnizca karsilikli arkadaslar davet edilebilir' });
            return;
        }
        invites = invites.filter(inv => !(inv.from === userCode && inv.to === to));
        invites.push({ from: userCode, senderName: me.username, senderUuid: me.uuid, to, tunnelUrl, timestamp: Date.now() });
        console.log(`[Davet] ${me.username} (${userCode}) -> ${to}`);
        sendJson(res, 200, { success: true });
        return;
    }

    if (pathname === '/api/reject-invite') {
        const from = normalizeCode(data.from);
        if (!from) {
            sendJson(res, 400, { error: 'Eksik parametre (from gereklidir)' });
            return;
        }
        invites = invites.filter(inv => !(inv.from === from && inv.to === userCode));
        sendJson(res, 200, { success: true });
        return;
    }

    if (pathname === '/api/host') {
        me.tunnelUrl = typeof data.tunnelUrl === 'string' ? data.tunnelUrl.trim().slice(0, 255) : '';
        me.isHosting = true;
        console.log(`[Host] ${me.username} (${userCode}) dunyasini paylasti.`);
        sendJson(res, 200, { success: true });
        return;
    }

    if (pathname === '/api/stop-host') {
        me.tunnelUrl = '';
        me.isHosting = false;
        sendJson(res, 200, { success: true });
    }
});

if (require.main === module) {
    server.listen(PORT, () => {
        console.log(`Pantial Varlik Sunucusu ${VERSION}, port ${PORT}.`);
    });
}

module.exports = { server, players, getInvites: () => invites, generateFriendCode, normalizeCode, rateBuckets };
