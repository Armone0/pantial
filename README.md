# Pantial - Minecraft P2P LAN & Varlık Ağı

Pantial; port yönlendirme (port forwarding) gerektirmeden, arkadaşlarınızla yerel Minecraft dünyalarınızı paylaşmanızı ve tek tıkla birlikte oynamanızı sağlayan açık kaynaklı bir mod ve sunucu ekosistemidir.

---

![Pantial menüsü](docs/menu.png)

## Başlarken: neye ihtiyaç var?

| | Dünyayı paylaşan | Katılan arkadaş |
| :-- | :-- | :-- |
| Fabric Loader + Fabric API | Gerekli | Gerekli |
| Pantial (bu mod) | Gerekli | Gerekli (tek tıkla katılma, arkadaş listesi) |
| [e4mc](https://modrinth.com/mod/e4mc) | **Gerekli** – dünyayı internete açan tüneli kurar | Gerekmez |
| Ortak bir Pantial sunucusu | Gerekli | Gerekli – herkes **aynı adresi** kullanmalı |

e4mc olmadan dünya yalnızca aynı ağdan (aynı Wi-Fi) görülebilir. Pantial sunucusu olmadan menü
"Pantial sunucusu ayarlanmamış" der; kurulum için aşağıdaki **Sunucuyu internete koymak** bölümüne bak.

## İndirme

[**Releases**](https://github.com/Armone0/pantial/releases) sayfasından Minecraft sürümüne uyan
`pantial-fabric-…+<minecraft>.jar` dosyasını indirip `mods` klasörüne at.

| Minecraft | Jar |
| :--- | :--- |
| 1.18.2 | `+1.18.2` |
| 1.19 – 1.19.2 / 1.19.3 – 1.19.4 | `+1.19.2` / `+1.19.4` |
| 1.20 – 1.20.1 / 1.20.3 – 1.20.4 / 1.20.5 – 1.20.6 | `+1.20.1` / `+1.20.4` / `+1.20.6` |
| 1.21 – 1.21.1 / 1.21.2 – 1.21.4 / 1.21.5 | `+1.21.1` / `+1.21.4` / `+1.21.5` |
| 1.21.6 – 1.21.8 / 1.21.9 – 1.21.10 / 1.21.11 | `+1.21.8` / `+1.21.10` / `+1.21.11` |
| 26.1.x / 26.2.x / 26.3.x | `+26.1.2` / `+26.2` / `+26.3` |

Şimdilik yalnızca **Fabric**. Forge/NeoForge yol haritasında.

## Oyunda

Ana menü, duraklatma ve çok oyunculu ekranlarının sağ üstünde **Pantial** düğmesi çıkar. Menüde:

- **Arkadaşlar** – `PANT-XXXX` koduyla arkadaşlık isteği gönder; karşı taraf kabul edince kimin çevrim içi
  olduğunu ve dünyası açık olanları görür, tek tıkla katılır, kendi dünyana davet edersin
- **İstekler** – gelen arkadaşlık istekleri ve dünya davetleri
- **Dünyam** – tek oyunculu dünyanı paylaş:
  - **Kimler girebilir?** *Sadece arkadaşlarım* (varsayılan) ya da *adresi bilen herkes*.
    Arkadaşın olmayan biri girmeye çalışırsa reddedilir ve sana **İzin ver / Reddet** diye sorulur.
  - **Komut yetkisi:** *Sadece ben* (varsayılan) ya da *herkes yönetici*.
    Paylaşımdayken içerideki her oyuncu için ayrı **Yönetici** anahtarı ve **At** düğmesi var.
  - Oyun modu, port, arkadaşlara duyurma; kimlik doğrulamayı kapatmak onay ister.
- **Ayarlar** – Pantial sunucusu adresi, bildirimler, ana menü düğmesi

Kodun sağ üstte; tıklayınca panoya kopyalanır. Ayarlar `config/pantial.json` içinde.

### Güvenlik notları
- Dünya adresi ve davetler yalnızca **karşılıklı** arkadaşlar arasında paylaşılır; kodunu bilen biri tek
  taraflı ekleyerek adresini göremez.
- Uzaktan senin adınla bağlanmaya çalışan biri her zaman reddedilir (Minecraft sahibi isme göre tanıdığı için).
- *Kimlik doğrulama* kapalıyken isimler doğrulanmaz; biri bir arkadaşının adını kullanabilir. Gerekmedikçe açık bırak.

## Mimari

```
server.js            Varlık sunucusu (Node.js, bağımlılık yok)
mod/
├── common/          Sürümden bağımsız kod: profil, sunucu istemcisi, menü (kendi çizim katmanıyla)
├── platform/<aile>/ Minecraft API ailesi başına ince katman (çizim, girdi, ekranlar, bildirim)
└── versions/<mc>/   Her Minecraft sürümü için ayrı alt proje → ayrı jar
```

Menü Minecraft widget'larını kullanmaz; `Gfx` arayüzü üzerinden kendisi çizer. Bu yüzden her sürümde aynı
görünür ve yeni bir sürüm eklemek genelde yalnızca platform katmanında birkaç satır değişiklik ister.

### Derleme

```bash
cd mod
./gradlew collectJars                       # tüm sürümler → mod/build/release/
./gradlew collectJars -Ppantial.only=1.20.1 # tek sürüm
./gradlew :1.21.11:runClient                # geliştirme istemcisi
```

Java 25 gerekir (26.x için); eski sürümler `--release 17/21` ile derlenir.

### Yeni sürüm yayınlama

İki yol var, ikisi de tüm sürümleri derleyip jar'ları ve sunucu paketini Release'e ekler:

- `v` ile başlayan bir etiket push'la (ör. `v1.1.0`), ya da
- GitHub'da **Actions → Build & Release → Run workflow** deyip `release_tag` alanına etiketi yaz.

Açıklama metni `RELEASE_NOTES.md` dosyasından gelir; yeni sürümde `mod/gradle.properties` içindeki `mod_version`'ı da artır.

---

## Sunucu Kurulumu ve Çalıştırma

### Sunucuyu internete koymak
Arkadaşlarınla kullanmak için sunucu herkesin erişebileceği bir yerde çalışmalı. Bağımlılığı yok, çok az kaynak harcar.

- **Render.com (ücretsiz):** Hesabına bu repoyu bağla, *New → Blueprint* de; `render.yaml` sunucuyu kurar.
  Çıkan `https://…onrender.com` adresini herkes oyunda **Pantial → Ayarlar**'a yazsın.
  Ücretsiz planda sunucu 15 dakika boşta kalınca uyur; ilk bağlantı birkaç saniye sürebilir.
- **Docker / VPS:** `docker build -t pantial-server . && docker run -p 3000:3000 pantial-server`
  ya da doğrudan `node server.js`. Önüne HTTPS koy.
- Bir proxy arkasındaysa (Render, Railway, Fly, nginx) `TRUST_PROXY=1` ver; yoksa istek sınırı tüm oyuncuları tek kişi sayar.

### Yerelde çalıştırma
- **Windows:** `start.bat` dosyasını çift tıklayın veya çalıştırın.
- **Linux / macOS:** `./start.sh` veya `npm start` çalıştırın.

```bash
node server.js   # varsayılan port 3000 (PORT ile değiştirilebilir)
npm test
```

### Başlıca API Endpoint'leri
- `GET /api/health` - Sunucu durumu ve sayılar (oyuncu bilgisi içermez)
- `POST /api/register` - `{userCode, secret, username, uuid}` ile kayıt
- `POST /api/status` - `{friendCodes, declinedCodes}` gönderir; arkadaşlar (`state`: `mutual`/`pending`/`unknown`),
  gelen arkadaşlık istekleri ve davetler döner. Adres, isim ve UUID yalnızca `mutual` için verilir.
- `POST /api/host`, `POST /api/stop-host` - Dünya adresini duyurma / kapatma
- `POST /api/invite` - Karşılıklı arkadaşı davet etme (`to`, `tunnelUrl`)
- `POST /api/reject-invite` - Sana gelen daveti kapatma (`from`)

**Kimlik:** İstemci ilk kayıtta rastgele bir `secret` üretir ve sunucu bunu koda bağlar. `register` dışındaki
tüm POST istekleri `userCode` + `secret` ister; yanlışsa `401`, kod başkasına aitse `403` döner. IP başına
dakikada 120 istek sınırı var (`PANTIAL_RATE_LIMIT`). Kayıtlar bellekte tutulur; sunucu yeniden başlayınca
istemciler otomatik tekrar kayıt olur ve arkadaş listelerini yeniden gönderir.

---

## Lisans
Manifestlerde MIT yazıyor ama repoda henüz `LICENSE` dosyası yok.
