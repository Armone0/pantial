const http = require('http');

const PORT = 3000;

// In-memory store: userCode -> { username, lastSeen, tunnelUrl, online }
const players = {};
let invites = [];

function parseJson(req) {
    return new Promise((resolve, reject) => {
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

    const url = req.url;

    if (req.method === 'POST') {
        const data = await parseJson(req);
        const { userCode } = data;

        if (url === '/api/invite') {
            const { from, to, tunnelUrl } = data;
            if (!from || !to || !tunnelUrl) {
                sendJson(res, 400, { error: 'Missing parameters' });
                return;
            }
            const sender = players[from];
            const senderName = sender ? sender.username : 'Unknown Player';
            const senderUuid = sender ? sender.uuid : '';

            // Remove existing invite between same pair
            invites = invites.filter(inv => !(inv.from === from && inv.to === to));

            invites.push({
                from,
                senderName,
                senderUuid,
                to,
                tunnelUrl,
                timestamp: Date.now()
            });
            console.log(`[Invite] ${senderName} (${from}) invited (${to}) to join at: ${tunnelUrl}`);
            sendJson(res, 200, { success: true });
            return;
        }

        if (!userCode) {
            sendJson(res, 400, { error: 'Missing userCode' });
            return;
        }

        if (url === '/api/register') {
            const { username, uuid } = data;
            if (!username) {
                sendJson(res, 400, { error: 'Missing username' });
                return;
            }

            players[userCode] = {
                username,
                uuid: uuid || '',
                lastSeen: Date.now(),
                tunnelUrl: players[userCode] ? players[userCode].tunnelUrl : '',
                online: true
            };
            console.log(`[Register] Player ${username} (${userCode}) is online with UUID ${uuid}.`);
            sendJson(res, 200, { success: true });
            return;
        }

        if (url === '/api/host') {
            const { tunnelUrl } = data;
            if (players[userCode]) {
                players[userCode].tunnelUrl = tunnelUrl || '';
                players[userCode].lastSeen = Date.now();
                players[userCode].online = true;
                console.log(`[Host] Player ${players[userCode].username} (${userCode}) started hosting at: ${tunnelUrl}`);
            }
            sendJson(res, 200, { success: true });
            return;
        }

        if (url === '/api/stop-host') {
            if (players[userCode]) {
                players[userCode].tunnelUrl = '';
                players[userCode].lastSeen = Date.now();
                console.log(`[Stop Host] Player ${players[userCode].username} (${userCode}) stopped hosting.`);
            }
            sendJson(res, 200, { success: true });
            return;
        }

        if (url === '/api/status') {
            const { friendCodes } = data;
            
            // Update sender status
            if (players[userCode]) {
                players[userCode].lastSeen = Date.now();
                players[userCode].online = true;
            }

            // Collect friend statuses
            const friendsStatus = (friendCodes || []).map(code => {
                const friend = players[code];
                return {
                    userCode: code,
                    username: friend ? friend.username : 'Unknown Player',
                    uuid: friend ? friend.uuid : '',
                    online: friend ? friend.online : false,
                    tunnelUrl: friend ? friend.tunnelUrl : ''
                };
            });

            // Find active invites for this user
            const now = Date.now();
            const activeInvites = invites.filter(inv => inv.to === userCode && now - inv.timestamp < 60000)
                                         .map(inv => ({
                                             senderCode: inv.from,
                                             senderName: inv.senderName,
                                             senderUuid: inv.senderUuid || '',
                                             tunnelUrl: inv.tunnelUrl
                                         }));

            sendJson(res, 200, { friends: friendsStatus, invites: activeInvites });
            return;
        }
    }

    sendJson(res, 404, { error: 'Not Found' });
});

// Cleanup loop: check for inactive heartbeats every 5 seconds
setInterval(() => {
    const now = Date.now();
    const TIMEOUT = 12000; // 12 seconds timeout

    for (const code in players) {
        const player = players[code];
        if (player.online && now - player.lastSeen > TIMEOUT) {
            player.online = false;
            player.tunnelUrl = '';
            console.log(`[Timeout] Player ${player.username} (${code}) went offline due to inactive heartbeat.`);
        }
    }

    // Clean up expired invites
    invites = invites.filter(inv => now - inv.timestamp < 60000);
}, 5000);

server.listen(PORT, () => {
    console.log(`FreeLAN Presence server running on port ${PORT}`);
});
