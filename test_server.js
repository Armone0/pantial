const http = require('http');

// Simple healthcheck test for pantial presence server
const req = http.request({
    hostname: '127.0.0.1',
    port: 3000,
    path: '/api/status?code=test-user',
    method: 'GET'
}, (res) => {
    let data = '';
    res.on('data', chunk => data += chunk);
    res.on('end', () => {
        console.log(`Server responded with status: ${res.statusCode}`);
        process.exit(res.statusCode === 200 || res.statusCode === 400 || res.statusCode === 404 ? 0 : 1);
    });
});

req.on('error', (err) => {
    console.error('Server check failed:', err.message);
    process.exit(1);
});

req.end();
