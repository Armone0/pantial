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

        // Test 13: 1.20.4 / 1.20.6 Geçiş Sürümleri ve NeoForge Manifest Doğrulaması
        const neoforgeTomlPath = path.join(__dirname, 'mods', '1.20.1', 'neoforge', 'META-INF', 'neoforge.mods.toml');
        assert(fs.existsSync(neoforgeTomlPath), 'neoforge.mods.toml mevcut olmalı');
        const neoforgeToml = fs.readFileSync(neoforgeTomlPath, 'utf8');
        assert(neoforgeToml.includes('modId="freelan"'), 'NeoForge modId freelan olmalı');
        assert(neoforgeToml.includes('versionRange="[1.20.4, 1.20.7)"'), 'NeoForge Minecraft versionRange [1.20.4, 1.20.7) olmalı');
        assert(neoforgeToml.includes('loaderVersion="[20.4,)'), 'NeoForge loaderVersion [20.4,) olmalı');

        const fabricModJson = JSON.parse(fs.readFileSync(fabricModPath, 'utf8'));
        assert(fabricModJson.depends.minecraft.includes('<=1.20.6'), 'fabric.mod.json 1.20.6 sürümünü kapsamalı');

        const forgeTomlContent = fs.readFileSync(forgeModsTomlPath, 'utf8');
        assert(forgeTomlContent.includes('1.20.7)'), 'Forge mods.toml 1.20.7 üst sınırını içermeli');

        console.log('✓ Test 13: 1.20.4 / 1.20.6 geçiş sürümü manifestleri ve NeoForge uyumluluğu doğrulandı.');

        // Test 14: Minecraft 1.19.2 & 1.19.4 (The Wild Update) Geriye Dönük Uyumluluk Doğrulaması
        const fabric119Path = path.join(__dirname, 'mods', '1.19.2', 'fabric', 'fabric.mod.json');
        const mixins119Path = path.join(__dirname, 'mods', '1.19.2', 'fabric', 'freelan.1.19.mixins.json');
        const forge119Path = path.join(__dirname, 'mods', '1.19.2', 'forge', 'META-INF', 'mods.toml');
        const screen119Path = path.join(__dirname, 'mods', '1.19.2', 'fabric', 'src', 'main', 'java', 'com', 'example', 'freelan', 'gui', 'PantialFriendsScreen119.java');
        const forgeEvents119Path = path.join(__dirname, 'mods', '1.19.2', 'forge', 'src', 'main', 'java', 'com', 'example', 'freelan', 'forge', 'FreeLanForgeEvents119.java');

        assert(fs.existsSync(fabric119Path), 'Fabric 1.19.2 fabric.mod.json mevcut olmalı');
        const fabric119Json = JSON.parse(fs.readFileSync(fabric119Path, 'utf8'));
        assert(fabric119Json.depends.minecraft.includes('<=1.19.4'), 'fabric.mod.json 1.19.4 sürümünü kapsamalı');

        assert(fs.existsSync(mixins119Path), 'freelan.1.19.mixins.json mevcut olmalı');
        const mixins119Json = JSON.parse(fs.readFileSync(mixins119Path, 'utf8'));
        assert.strictEqual(mixins119Json.compatibilityLevel, 'JAVA_17', '1.19 Mixin Java 17 olmalı');

        assert(fs.existsSync(forge119Path), 'Forge 1.19 mods.toml mevcut olmalı');
        const forge119Toml = fs.readFileSync(forge119Path, 'utf8');
        assert(forge119Toml.includes('1.19.5)'), 'Forge mods.toml 1.19.5 üst sınırını içermeli');
        assert(forge119Toml.includes('loaderVersion="[43,)'), 'Forge mods.toml loaderVersion [43,) olmalı');

        assert(fs.existsSync(screen119Path), 'PantialFriendsScreen119.java mevcut olmalı');
        const screen119Content = fs.readFileSync(screen119Path, 'utf8');
        assert(screen119Content.includes('PoseStack matrices'), 'PantialFriendsScreen119 PoseStack kullanmalı');
        assert(screen119Content.includes('new Button('), '1.19 arayüzü klasik new Button kullanmalı');

        assert(fs.existsSync(forgeEvents119Path), 'FreeLanForgeEvents119.java mevcut olmalı');
        const forgeEvents119Content = fs.readFileSync(forgeEvents119Path, 'utf8');
        assert(forgeEvents119Content.includes('new Button('), 'Forge 1.19 olay dinleyicisi new Button kullanmalı');

        console.log('✓ Test 14: 1.19.2 & 1.19.4 (The Wild Update) Fabric/Forge manifest ve PoseStack adaptörleri doğrulandı.');

        // Test 15: Minecraft 1.18.2 (Caves & Cliffs Part II) Geriye Dönük Uyumluluk Doğrulaması
        const fabric118Path = path.join(__dirname, 'mods', '1.18.2', 'fabric', 'fabric.mod.json');
        const mixins118Path = path.join(__dirname, 'mods', '1.18.2', 'fabric', 'freelan.1.18.mixins.json');
        const forge118Path = path.join(__dirname, 'mods', '1.18.2', 'forge', 'META-INF', 'mods.toml');
        const screen118Path = path.join(__dirname, 'mods', '1.18.2', 'fabric', 'src', 'main', 'java', 'com', 'example', 'freelan', 'gui', 'PantialFriendsScreen118.java');
        const forgeEvents118Path = path.join(__dirname, 'mods', '1.18.2', 'forge', 'src', 'main', 'java', 'com', 'example', 'freelan', 'forge', 'FreeLanForgeEvents118.java');

        assert(fs.existsSync(fabric118Path), 'Fabric 1.18.2 fabric.mod.json mevcut olmalı');
        const fabric118Json = JSON.parse(fs.readFileSync(fabric118Path, 'utf8'));
        assert(fabric118Json.depends.minecraft.includes('<=1.18.2'), 'fabric.mod.json 1.18.2 sürümünü kapsamalı');

        assert(fs.existsSync(mixins118Path), 'freelan.1.18.mixins.json mevcut olmalı');
        const mixins118Json = JSON.parse(fs.readFileSync(mixins118Path, 'utf8'));
        assert.strictEqual(mixins118Json.compatibilityLevel, 'JAVA_17', '1.18 Mixin Java 17 olmalı');

        assert(fs.existsSync(forge118Path), 'Forge 1.18.2 mods.toml mevcut olmalı');
        const forge118Toml = fs.readFileSync(forge118Path, 'utf8');
        assert(forge118Toml.includes('1.19)'), 'Forge mods.toml 1.19 üst sınırını içermeli');
        assert(forge118Toml.includes('loaderVersion="[40,)'), 'Forge mods.toml loaderVersion [40,) olmalı');

        assert(fs.existsSync(screen118Path), 'PantialFriendsScreen118.java mevcut olmalı');
        const screen118Content = fs.readFileSync(screen118Path, 'utf8');
        assert(screen118Content.includes('PoseStack matrices'), 'PantialFriendsScreen118 PoseStack kullanmalı');
        assert(screen118Content.includes('new TextComponent('), '1.18 arayüzü TextComponent kullanmalı');
        assert(screen118Content.includes('serverConnected'), '1.18 arayüzü sunucu hata durumu kontrolü içermeli');

        assert(fs.existsSync(forgeEvents118Path), 'FreeLanForgeEvents118.java mevcut olmalı');
        const forgeEvents118Content = fs.readFileSync(forgeEvents118Path, 'utf8');
        assert(forgeEvents118Content.includes('ScreenEvent.InitScreenEvent.Post'), 'Forge 1.18.2 olay dinleyicisi InitScreenEvent.Post kullanmalı');

        console.log('✓ Test 15: 1.18.2 (Caves & Cliffs Part II) Fabric/Forge manifest, TextComponent ve hata paneli doğrulandı.');

        console.log('\n========================================================');
        console.log('TÜM TESTLER BAŞARIYLA TAMAMLANDI (15/15 OK - %100 HATASIZ)');
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
