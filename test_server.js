const http = require('http');
const assert = require('assert');
const fs = require('fs');
const path = require('path');
const { server, players, invites, generateFriendCode, normalizeCode } = require('./server');

const TEST_PORT = 3999;

function request(options, data = null) {
    return new Promise((resolve, reject) => {
        const req = http.request(options, (res) => {
            let body = '';
            res.on('data', chunk => body += chunk);
            res.on('end', () => {
                let parsed = null;
                try {
                    parsed = body ? JSON.parse(body) : null;
                } catch (e) {
                    parsed = body;
                }
                resolve({ statusCode: res.statusCode, data: parsed, headers: res.headers });
            });
        });
        req.on('error', reject);
        if (data) {
            req.setHeader('Content-Type', 'application/json');
            req.write(typeof data === 'string' ? data : JSON.stringify(data));
        }
        req.end();
    });
}

async function runTests() {
    console.log('========================================================');
    console.log('--- Pantial Varlık Sunucusu & Mod Doğrulama Testi ---');
    console.log('========================================================');

    await new Promise((resolve) => server.listen(TEST_PORT, resolve));
    console.log(`[Test] Test sunucusu port ${TEST_PORT} üzerinde açıldı.`);

    try {
        // Test 1: Yardımcı fonksiyonlar (Kod üretimi & Normalizasyon)
        const code1 = generateFriendCode();
        assert(code1.startsWith('PANT-'), 'Kod PANT- ile başlamalı');
        assert.strictEqual(code1.length, 9, 'Kod 9 karakter (PANT-XXXX) olmalı');
        assert.strictEqual(normalizeCode('  pant-12ab  '), 'PANT-12AB', 'Normalizasyon boşlukları temizlemeli ve büyük harf yapmalı');
        assert.strictEqual(normalizeCode(null), '', 'Null girdi boş string dönmeli');
        console.log('✓ Test 1: Arkadaş kodu üretimi ve format normalizasyonu başarılı.');

        // Test 2: Sağlık Kontrolü (GET /api/health)
        const healthRes = await request({
            hostname: '127.0.0.1',
            port: TEST_PORT,
            path: '/api/health',
            method: 'GET'
        });
        assert.strictEqual(healthRes.statusCode, 200);
        assert.strictEqual(healthRes.data.status, 'ok');
        assert.strictEqual(healthRes.data.service, 'pantial-presence-server');
        assert(typeof healthRes.data.uptimeSeconds === 'number');
        console.log('✓ Test 2: GET /api/health sağlık kontrolü başarılı.');

        // Test 3: Kod Üretim API (GET /api/generate-code)
        const genRes = await request({
            hostname: '127.0.0.1',
            port: TEST_PORT,
            path: '/api/generate-code',
            method: 'GET'
        });
        assert.strictEqual(genRes.statusCode, 200);
        assert(genRes.data.userCode.startsWith('PANT-'));
        assert.strictEqual(genRes.data.format, 'PANT-XXXX');
        console.log('✓ Test 3: GET /api/generate-code API başarılı.');

        // Test 4: Oyuncu Kaydı (POST /api/register)
        const regAlice = await request({
            hostname: '127.0.0.1',
            port: TEST_PORT,
            path: '/api/register',
            method: 'POST'
        }, { username: 'Alice', userCode: 'PANT-ALIC' });
        assert.strictEqual(regAlice.statusCode, 200);
        assert.strictEqual(regAlice.data.userCode, 'PANT-ALIC');

        const regBob = await request({
            hostname: '127.0.0.1',
            port: TEST_PORT,
            path: '/api/register',
            method: 'POST'
        }, { username: 'Bob' }); // Kod verilmeden kayıt -> otomatik üretim
        assert.strictEqual(regBob.statusCode, 200);
        const bobCode = regBob.data.userCode;
        assert(bobCode.startsWith('PANT-'));
        console.log(`✓ Test 4: POST /api/register kayıtları başarılı (Alice: PANT-ALIC, Bob: ${bobCode}).`);

        // Test 5: Oyuncu Listesi (GET /api/players)
        const playersRes = await request({
            hostname: '127.0.0.1',
            port: TEST_PORT,
            path: '/api/players',
            method: 'GET'
        });
        assert.strictEqual(playersRes.statusCode, 200);
        assert(playersRes.data.totalCount >= 2);
        assert(playersRes.data.onlineCount >= 2);
        console.log('✓ Test 5: GET /api/players aktif oyuncu listesi sorgusu başarılı.');

        // Test 6: Yerel Ağ Dünyasını Paylaşma (POST /api/host)
        const hostRes = await request({
            hostname: '127.0.0.1',
            port: TEST_PORT,
            path: '/api/host',
            method: 'POST'
        }, { userCode: 'PANT-ALIC', tunnelUrl: 'play.e4mc.link:25565' });
        assert.strictEqual(hostRes.statusCode, 200);
        console.log('✓ Test 6: POST /api/host tünel duyurusu başarılı.');

        // Test 7: Durum Sorgulama (POST /api/status)
        const bobStatusRes = await request({
            hostname: '127.0.0.1',
            port: TEST_PORT,
            path: '/api/status',
            method: 'POST'
        }, { userCode: bobCode, friendCodes: ['PANT-ALIC'] });
        assert.strictEqual(bobStatusRes.statusCode, 200);
        assert.strictEqual(bobStatusRes.data.friends.length, 1);
        assert.strictEqual(bobStatusRes.data.friends[0].username, 'Alice');
        assert.strictEqual(bobStatusRes.data.friends[0].online, true);
        assert.strictEqual(bobStatusRes.data.friends[0].isHosting, true);
        assert.strictEqual(bobStatusRes.data.friends[0].tunnelUrl, 'play.e4mc.link:25565');
        console.log('✓ Test 7: POST /api/status arkadaş durumu ve tünel adresi eşleşmesi doğrulandı.');

        // Test 8: Davet Gönderme ve Alma (POST /api/invite & durum kontrolü)
        const inviteRes = await request({
            hostname: '127.0.0.1',
            port: TEST_PORT,
            path: '/api/invite',
            method: 'POST'
        }, { from: 'PANT-ALIC', to: bobCode, tunnelUrl: 'play.e4mc.link:25565' });
        assert.strictEqual(inviteRes.statusCode, 200);

        const bobStatusWithInvite = await request({
            hostname: '127.0.0.1',
            port: TEST_PORT,
            path: '/api/status',
            method: 'POST'
        }, { userCode: bobCode, friendCodes: ['PANT-ALIC'] });
        assert.strictEqual(bobStatusWithInvite.data.invites.length, 1);
        assert.strictEqual(bobStatusWithInvite.data.invites[0].senderName, 'Alice');
        console.log('✓ Test 8: POST /api/invite ve davet kuyruğu entegrasyonu başarılı.');

        // Test 9: Davet Reddetme (POST /api/reject-invite)
        const rejectRes = await request({
            hostname: '127.0.0.1',
            port: TEST_PORT,
            path: '/api/reject-invite',
            method: 'POST'
        }, { from: 'PANT-ALIC', to: bobCode });
        assert.strictEqual(rejectRes.statusCode, 200);

        const bobStatusAfterReject = await request({
            hostname: '127.0.0.1',
            port: TEST_PORT,
            path: '/api/status',
            method: 'POST'
        }, { userCode: bobCode });
        assert.strictEqual(bobStatusAfterReject.data.invites.length, 0);
        console.log('✓ Test 9: POST /api/reject-invite davet reddetme işlemi doğrulandı.');

        // Test 10: Hosting Durdurma (POST /api/stop-host)
        const stopRes = await request({
            hostname: '127.0.0.1',
            port: TEST_PORT,
            path: '/api/stop-host',
            method: 'POST'
        }, { userCode: 'PANT-ALIC' });
        assert.strictEqual(stopRes.statusCode, 200);

        const aliceGetStatus = await request({
            hostname: '127.0.0.1',
            port: TEST_PORT,
            path: '/api/status?code=PANT-ALIC',
            method: 'GET'
        });
        assert.strictEqual(aliceGetStatus.data.isHosting, false);
        assert.strictEqual(aliceGetStatus.data.tunnelUrl, '');
        console.log('✓ Test 10: POST /api/stop-host ve GET /api/status sorgusu başarılı.');

        // Test 11: Minecraft 1.20.1 Fabric Manifest ve Mixin Doğrulaması
        const fabricModPath = path.join(__dirname, 'mods', '1.20.1', 'fabric', 'fabric.mod.json');
        const mixinsPath = path.join(__dirname, 'mods', '1.20.1', 'fabric', 'freelan.1.20.1.mixins.json');

        assert(fs.existsSync(fabricModPath), 'fabric.mod.json mevcut olmalı');
        const fabricMod = JSON.parse(fs.readFileSync(fabricModPath, 'utf8'));
        assert.strictEqual(fabricMod.id, 'freelan');
        assert(fabricMod.depends.minecraft.includes('1.20.1'), 'Minecraft 1.20.1 sürüm kısıtlaması içermeli');
        assert.strictEqual(fabricMod.depends.java, '>=17', 'Java 17 gereksinimi belirtilmeli');

        assert(fs.existsSync(mixinsPath), 'freelan.1.20.1.mixins.json mevcut olmalı');
        const mixinsJson = JSON.parse(fs.readFileSync(mixinsPath, 'utf8'));
        assert.strictEqual(mixinsJson.compatibilityLevel, 'JAVA_17', 'Mixin Java 17 uyumluluğu kontrolü');
        
        const expectedMixins = [
            'ShareToLanScreenMixin',
            'MinecraftServerMixin',
            'ChatComponentMixin',
            'TitleScreenMixin',
            'PauseScreenMixin',
            'JoinMultiplayerScreenMixin'
        ];
        for (const mixin of expectedMixins) {
            assert(mixinsJson.client.includes(mixin), `freelan.1.20.1.mixins.json '${mixin}' içermeli`);
            const mixinFilePath = path.join(__dirname, 'mods', '1.20.1', 'fabric', 'src', 'main', 'java', 'com', 'example', 'freelan', 'mixin', `${mixin}.java`);
            assert(fs.existsSync(mixinFilePath), `${mixin}.java kaynak dosyası mevcut olmalı`);
        }
        console.log('✓ Test 11: Fabric 1.20.1 mod manifest ve 6 Mixin sınıfının kaynak dosyaları doğrulandı.');

        // Test 12: Minecraft 1.20.1 Forge ve Arayüz Sınıfları Doğrulaması
        const forgeModsTomlPath = path.join(__dirname, 'mods', '1.20.1', 'forge', 'META-INF', 'mods.toml');
        const freeLanScreenPath = path.join(__dirname, 'mods', '1.20.1', 'fabric', 'src', 'main', 'java', 'com', 'example', 'freelan', 'gui', 'FreeLanScreen.java');
        const pantialFriendsPath = path.join(__dirname, 'mods', '1.20.1', 'fabric', 'src', 'main', 'java', 'com', 'example', 'freelan', 'gui', 'PantialFriendsScreen.java');

        assert(fs.existsSync(forgeModsTomlPath), 'Forge mods.toml mevcut olmalı');
        const modsToml = fs.readFileSync(forgeModsTomlPath, 'utf8');
        assert(modsToml.includes('modId="freelan"'), 'Forge modId freelan olmalı');
        assert(modsToml.includes('1.20.1'), 'Forge 1.20.1 bağımlılığı içermeli');

        assert(fs.existsSync(freeLanScreenPath), 'FreeLanScreen.java mevcut olmalı');
        assert(fs.existsSync(pantialFriendsPath), 'PantialFriendsScreen.java mevcut olmalı');

        console.log('✓ Test 12: Forge 1.20.1 mods.toml ve FreeLanScreen / PantialFriendsScreen arayüzleri doğrulandı.');

        console.log('\n========================================================');
        console.log('TÜM TESTLER BAŞARIYLA TAMAMLANDI (12/12 OK - %100 HATASIZ)');
        console.log('========================================================\n');
        process.exitCode = 0;
    } catch (err) {
        console.error('❌ Test sırasında hata meydana geldi:', err);
        process.exitCode = 1;
    } finally {
        server.close(() => {
            process.exit(process.exitCode || 0);
        });
    }
}

runTests();
