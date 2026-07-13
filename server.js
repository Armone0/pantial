const http = require('http');

const PORT = 3000;

// In-memory store: userCode -> { username, lastSeen, tunnelUrl, online }
const players = {};

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

        if (!userCode) {
            sendJson(res, 400, { error: 'Missing userCode' });
            return;
        }

        if (url === '/api/register') {
            const { username } = data;
            if (!username) {
                sendJson(res, 400, { error: 'Missing username' });
                return;
            }

            players[userCode] = {
                username,
                lastSeen: Date.now(),
                tunnelUrl: players[userCode] ? players[userCode].tunnelUrl : '',
                online: true
            };
            console.log(`[Register] Player ${username} (${userCode}) is online.`);
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
                    online: friend ? friend.online : false,
                    tunnelUrl: friend ? friend.tunnelUrl : ''
                };
            });

            sendJson(res, 200, { friends: friendsStatus });
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
}, 5000);

server.listen(PORT, () => {
    console.log(`FreeLAN Presence server running on port ${PORT}`);
});
