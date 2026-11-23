const http = require('http');
const assert = require('assert');
const fs = require('fs');
const path = require('path');
const { server, players, getInvites, generateFriendCode, normalizeCode, rateBuckets } = require('./server');

const TEST_PORT = 3999;
const ALICE = { userCode: 'PANT-A2C4', secret: 'alice-secret-0123456789' };
const BOB = { userCode: 'PANT-B3D5', secret: 'bob-secret-0123456789ab' };

function request(method, urlPath, data = null, rawBody = null) {
    return new Promise((resolve, reject) => {
        const req = http.request({ hostname: '127.0.0.1', port: TEST_PORT, path: urlPath, method }, (res) => {
            let body = '';
            res.on('data', chunk => body += chunk);
            res.on('end', () => {
                let parsed = null;
                try {
                    parsed = body ? JSON.parse(body) : null;
                } catch (e) {
                    parsed = body;
                }
                resolve({ statusCode: res.statusCode, data: parsed });
            });
        });
        req.on('error', reject);
        if (data || rawBody) {
            req.setHeader('Content-Type', 'application/json');
            req.write(rawBody !== null ? rawBody : JSON.stringify(data));
        }
        req.end();
    });
}

const get = (p) => request('GET', p);
const post = (p, d) => request('POST', p, d);

const tests = [];
function test(name, fn) {
    tests.push({ name, fn });
}

test('Kod uretimi ve normalizasyon', async () => {
    const code = generateFriendCode();
    assert.match(code, /^PANT-[23456789ABCDEFGHJKLMNPQRSTUVWXYZ]{4}$/);
    assert.strictEqual(normalizeCode('  pant-12ab  '), 'PANT-12AB');
    assert.strictEqual(normalizeCode(null), '');
});

test('GET /api/health', async () => {
    const res = await get('/api/health');
    assert.strictEqual(res.statusCode, 200);
    assert.strictEqual(res.data.status, 'ok');
    assert.strictEqual(res.data.version, require('./package.json').version);
});

test('GET /api/generate-code', async () => {
    const res = await get('/api/generate-code');
    assert.strictEqual(res.statusCode, 200);
    assert.match(res.data.userCode, /^PANT-/);
});

test('POST /api/register: secret ve gecerli kod zorunlu', async () => {
    assert.strictEqual((await post('/api/register', { username: 'X', userCode: 'PANT-ZZZZ' })).statusCode, 400);
    assert.strictEqual((await post('/api/register', { username: 'X', userCode: 'PANT-ZZZZ', secret: 'kisa' })).statusCode, 400);
    assert.strictEqual((await post('/api/register', { username: 'X', userCode: 'ALICE', secret: ALICE.secret })).statusCode, 400);
    assert.strictEqual((await post('/api/register', { userCode: 'PANT-ZZZZ', secret: ALICE.secret })).statusCode, 400);
});

test('POST /api/register: Alice ve Bob kaydi', async () => {
    const a = await post('/api/register', { ...ALICE, username: 'Alice', uuid: 'uuid-a' });
    const b = await post('/api/register', { ...BOB, username: 'Bob', uuid: 'uuid-b' });
    assert.strictEqual(a.statusCode, 200);
    assert.strictEqual(b.statusCode, 200);
    assert.strictEqual(a.data.userCode, ALICE.userCode);
    assert(players[ALICE.userCode].online);
});

test('POST /api/register: kod baska secret ile sahiplenilemez', async () => {
    const res = await post('/api/register', { userCode: ALICE.userCode, secret: 'saldirgan-secret-xxxx', username: 'Mallory' });
    assert.strictEqual(res.statusCode, 403);
    assert.strictEqual(players[ALICE.userCode].username, 'Alice');
});

test('Kimlik dogrulamasiz istekler 401 doner', async () => {
    assert.strictEqual((await post('/api/host', { userCode: ALICE.userCode, tunnelUrl: 'evil.e4mc.link' })).statusCode, 401);
    assert.strictEqual((await post('/api/host', { userCode: ALICE.userCode, secret: BOB.secret, tunnelUrl: 'evil.e4mc.link' })).statusCode, 401);
    assert.strictEqual((await post('/api/status', { userCode: 'PANT-9999', secret: ALICE.secret })).statusCode, 401);
    assert.strictEqual(players[ALICE.userCode].tunnelUrl, '');
});

const MALLORY = { userCode: 'PANT-M4LR', secret: 'mallory-secret-012345678' };

test('Tek tarafli ekleme: adres ve isim gizli, karsi tarafa istek olarak gider', async () => {
    assert.strictEqual((await post('/api/host', { ...ALICE, tunnelUrl: 'play.e4mc.link' })).statusCode, 200);
    // Bob, Alice'i ekliyor; Alice henuz eklemedi
    const bob = await post('/api/status', { ...BOB, friendCodes: [ALICE.userCode.toLowerCase(), 'PANT-7777'] });
    assert.strictEqual(bob.statusCode, 200);
    assert.strictEqual(bob.data.friends[0].state, 'pending');
    assert.strictEqual(bob.data.friends[0].tunnelUrl, '');
    assert.strictEqual(bob.data.friends[0].username, '');
    assert.strictEqual(bob.data.friends[1].state, 'unknown');
    // Alice'e istek olarak gorunur
    const alice = await post('/api/status', { ...ALICE, friendCodes: [] });
    assert.deepStrictEqual(alice.data.requests, [{ userCode: BOB.userCode, username: 'Bob' }]);
});

test('Reddedilen istek tekrar gosterilmez', async () => {
    const alice = await post('/api/status', { ...ALICE, friendCodes: [], declinedCodes: [BOB.userCode] });
    assert.strictEqual(alice.data.requests.length, 0);
});

test('Karsilikli arkadaslik: adres, isim ve UUID paylasilir', async () => {
    await post('/api/status', { ...ALICE, friendCodes: [BOB.userCode] });
    const bob = await post('/api/status', { ...BOB, friendCodes: [ALICE.userCode] });
    const f = bob.data.friends[0];
    assert.strictEqual(f.state, 'mutual');
    assert.strictEqual(f.username, 'Alice');
    assert.strictEqual(f.uuid, 'uuid-a');
    assert.strictEqual(f.isHosting, true);
    assert.strictEqual(f.tunnelUrl, 'play.e4mc.link');
});

test('Kodu bilen yabanci adresi goremez ve davet atamaz', async () => {
    await post('/api/register', { ...MALLORY, username: 'Mallory' });
    const m = await post('/api/status', { ...MALLORY, friendCodes: [ALICE.userCode] });
    assert.strictEqual(m.data.friends[0].state, 'pending');
    assert.strictEqual(m.data.friends[0].tunnelUrl, '');
    const inv = await post('/api/invite', { ...MALLORY, to: ALICE.userCode, tunnelUrl: 'evil.e4mc.link' });
    assert.strictEqual(inv.statusCode, 403);
});

test('Herkese acik GET uclari oyuncu bilgisi vermez', async () => {
    assert.strictEqual((await get('/api/players')).statusCode, 404);
    assert.strictEqual((await get(`/api/status?userCode=${ALICE.userCode}`)).statusCode, 404);
});

test('POST /api/invite: gonderen kimligi sunucudan gelir', async () => {
    const res = await post('/api/invite', { ...ALICE, from: BOB.userCode, to: BOB.userCode, tunnelUrl: 'play.e4mc.link' });
    assert.strictEqual(res.statusCode, 200);
    const status = await post('/api/status', { ...BOB, friendCodes: [ALICE.userCode] });
    assert.strictEqual(status.data.invites.length, 1);
    assert.strictEqual(status.data.invites[0].senderCode, ALICE.userCode);
    assert.strictEqual(status.data.invites[0].senderName, 'Alice');
});

test('Arkadasliktan cikinca bekleyen davet de gizlenir', async () => {
    await post('/api/status', { ...BOB, friendCodes: [] });
    await post('/api/status', { ...BOB, friendCodes: [ALICE.userCode] });
    // Alice, Bob'u listeden cikarirsa Bob artik daveti goremez
    await post('/api/status', { ...ALICE, friendCodes: [] });
    const status = await post('/api/status', { ...BOB, friendCodes: [ALICE.userCode] });
    assert.strictEqual(status.data.invites.length, 0);
    await post('/api/status', { ...ALICE, friendCodes: [BOB.userCode] });
});

test('POST /api/reject-invite: yalnizca alici silebilir', async () => {
    await post('/api/reject-invite', { ...ALICE, from: ALICE.userCode });
    assert.strictEqual(getInvites().length, 1);
    await post('/api/reject-invite', { ...BOB, from: ALICE.userCode });
    assert.strictEqual(getInvites().length, 0);
});

test('POST /api/stop-host', async () => {
    assert.strictEqual((await post('/api/stop-host', ALICE)).statusCode, 200);
    const res = await post('/api/status', { ...BOB, friendCodes: [ALICE.userCode] });
    assert.strictEqual(res.data.friends[0].isHosting, false);
    assert.strictEqual(res.data.friends[0].tunnelUrl, '');
});

test('Bozuk JSON ve bilinmeyen uc', async () => {
    assert.strictEqual((await request('POST', '/api/status', null, '{bozuk')).statusCode, 400);
    assert.strictEqual((await post('/api/yok', ALICE)).statusCode, 404);
    assert.strictEqual((await get('/api/yok')).statusCode, 404);
});

test('Istek siniri asilinca 429', async () => {
    rateBuckets.clear();
    let last;
    for (let i = 0; i < 130; i++) last = await post('/api/status', { ...BOB, friendCodes: [] });
    assert.strictEqual(last.statusCode, 429);
    rateBuckets.clear();
});

test('Mod manifesti, mixinler ve her surumun platform katmani tutarli', async () => {
    const mod = path.join(__dirname, 'mod');
    const res = path.join(mod, 'common', 'src', 'main', 'resources');
    const manifest = JSON.parse(fs.readFileSync(path.join(res, 'fabric.mod.json'), 'utf8'));
    assert.strictEqual(manifest.id, 'pantial');
    const mixins = JSON.parse(fs.readFileSync(path.join(res, manifest.mixins[0]), 'utf8'));
    const mixinPath = (root, name) => path.join(root, ...mixins.package.split('.'), `${name}.java`);
    const entry = manifest.entrypoints.client[0].split('.');
    const versions = fs.readdirSync(path.join(mod, 'versions'));
    assert(versions.length >= 10, 'surum listesi eksik');
    for (const v of versions) {
        const props = Object.fromEntries(fs.readFileSync(path.join(mod, 'versions', v, 'gradle.properties'), 'utf8')
            .split('\n').filter(l => l.includes('=')).map(l => l.split('=')));
        assert.strictEqual(props.minecraft_version, v);
        const file = path.join(mod, 'platform', props.platform, 'java', ...entry) + '.java';
        assert(fs.existsSync(file), `${v}: entrypoint yok (${props.platform})`);
        // Her mixin ya ortak kodda ya da bu surumun platform katmaninda olmali
        for (const name of mixins.client) {
            const found = [path.join(mod, 'common', 'src', 'main', 'java'), path.join(mod, 'platform', props.platform, 'java')]
                .some(root => fs.existsSync(mixinPath(root, name)));
            assert(found, `${v}: mixin sinifi yok: ${name}`);
        }
    }
});

async function run() {
    await new Promise((resolve) => server.listen(TEST_PORT, '127.0.0.1', resolve));
    const log = console.log;
    console.log = () => {}; // sunucu loglarini sustur
    let failed = 0;
    for (const t of tests) {
        try {
            await t.fn();
            log(`✓ ${t.name}`);
        } catch (e) {
            failed++;
            log(`✗ ${t.name}\n    ${e.message}`);
        }
    }
    console.log = log;
    server.close();
    console.log(`\n${tests.length - failed}/${tests.length} test gecti.`);
    console.log('Not: Bu testler sunucuyu ve manifest tutarliligini kontrol eder; modun derlendigini');
    console.log('gormek icin mod/ icinde ./gradlew collectJars calistirin.');
    process.exit(failed ? 1 : 0);
}

run();
