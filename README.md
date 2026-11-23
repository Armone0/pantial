# Pantial - Minecraft P2P LAN & Varlık Ağı

Pantial; port yönlendirme (port forwarding) gerektirmeden, arkadaşlarınızla yerel Minecraft dünyalarınızı paylaşmanızı ve tek tıkla birlikte oynamanızı sağlayan açık kaynaklı bir mod ve sunucu ekosistemidir.

---

## Mimari ve Bileşenler

1. **Tünel Entegrasyonu (`e4mc` Protokolü):**  
   Dünyanızı yerel ağa (LAN) açtığınızda e4mc tünel protokolü üzerinden otomatik bir genel adres (`*.e4mc.link`) oluşturur. Karmaşık sanal ağ (VPN) programlarına gerek bırakmaz.
2. **`FreeLAN` İstemci Modu:**  
   Essential tarzı modern bir arkadaş listesi ve arayüz sunar. Akılda kalıcı oyuncu kodunuzu (`PANT-XXXX`) arkadaşınızla paylaştığınızda, arka planda tünel adresini alıp birbirinizi çevrim içi görmenizi ve tek tıkla oyuna katılmanızı sağlar.
3. **`pantial-presence-server` (`server.js`):**  
   Oyuncuların çevrim içi durumlarını, davetlerini ve tünel bağlantılarını koordine eden hafif Node.js varlık sunucusudur.

---

## Sürüm Durumu

| Minecraft | Yükleyici | Durum |
| :--- | :--- | :--- |
| **1.20.1** | Fabric | Derleniyor, oyunda açılıyor (`mods/1.20.1/fabric`) |
| 1.20.1 | Forge | Taslak – derlenmez (Fabric sınıflarına bağımlı) |
| 1.20.4 / 1.20.6 | NeoForge | Taslak – yalnızca manifest var |
| 1.19.2 / 1.19.4 | Fabric / Forge | Taslak – yalnızca tek ekran sınıfı, derlenmez |
| 1.18.2 | Fabric / Forge | Taslak – yalnızca tek ekran sınıfı, derlenmez |
| 1.21.x, 1.16.5 | – | Başlanmadı |

Taslak klasörler bir sonraki port için başlangıç noktası olarak duruyor; gerçek bir Gradle projesi olmadan
"destekleniyor" sayılmazlar. Detaylar için [ROADMAP.md](ROADMAP.md).

### Modu derleme (Fabric 1.20.1)

```bash
cd mods/1.20.1/fabric
./gradlew build        # jar: build/libs/pantial-1.0.0+1.20.1.jar
./gradlew runClient    # geliştirme istemcisi
```

Java 17+ gerekir. Oyunda: arkadaş kodu `PANT-XXXX`, ayarlar `config/pantial_profile.json`
(`serverUrl` varsayılan `http://localhost:3000`). Dünyayı internete açmak için yanına
[e4mc](https://modrinth.com/mod/e4mc) modu kurulmalı; Pantial e4mc'nin sohbete yazdığı adresi yakalayıp arkadaşlara duyurur.

---

## Sunucu Kurulumu ve Çalıştırma

### Hızlı Başlatma
- **Windows:** `start.bat` dosyasını çift tıklayın veya çalıştırın.
- **Linux / macOS:** `./start.sh` veya `npm start` çalıştırın.

```bash
# Sunucuyu başlatma
node server.js

# Test paketini çalıştırma
npm test
```
Varsayılan olarak `3000` portunda çalışır.

### Başlıca API Endpoint'leri
- `GET /api/health` - Sunucu durum ve aktif oyuncu istatistikleri
- `GET /api/generate-code` - Benzersiz `PANT-XXXX` formatında arkadaş kodu üretme
- `GET /api/players`, `GET /api/status?userCode=` - Herkese açık özet (tünel adresi içermez)
- `POST /api/register` - `{userCode, secret, username, uuid}` ile kayıt
- `POST /api/host` - Yerel dünya tünel adresini duyurma
- `POST /api/status` - Arkadaş listesi ve gelen davetleri sorgulama
- `POST /api/invite` - Arkadaşını paylaşılan dünyaya davet etme (`to`, `tunnelUrl`)
- `POST /api/reject-invite` - Sana gelen daveti kapatma (`from`)
- `POST /api/stop-host` - Dünya paylaşımını sonlandırma

**Kimlik:** İstemci ilk kayıtta rastgele bir `secret` üretir ve sunucu bunu koda bağlar. `register`
dışındaki tüm POST istekleri `userCode` + `secret` ister; yanlışsa `401`, kod başkasına aitse `403` döner.
Böylece biri senin kodunu alıp arkadaşlarına sahte dünya adresi duyuramaz. Kayıtlar bellekte tutulur,
sunucu yeniden başlayınca istemciler otomatik tekrar kayıt olur. Sunucuyu internete açacaksan önüne HTTPS koy.

---

## Lisans
Manifestlerde MIT yazıyor ama repoda henüz `LICENSE` dosyası yok.
