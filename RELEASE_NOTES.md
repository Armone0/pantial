## Pantial – arkadaşlarınla port açmadan oyna

### Hangi dosyayı indirmeliyim?

`pantial-fabric-<sürüm>+<minecraft>.jar` dosyalarından **Minecraft sürümüne uyanı** indir ve `mods` klasörüne at.
Gerekenler: [Fabric Loader](https://fabricmc.net/use/) ve [Fabric API](https://modrinth.com/mod/fabric-api).
Dünyayı internete açmak için [e4mc](https://modrinth.com/mod/e4mc) önerilir.

| Jar | Çalıştığı Minecraft sürümleri |
| :-- | :-- |
| `+1.18.2` | 1.18.2 |
| `+1.19.2` | 1.19 – 1.19.2 |
| `+1.19.4` | 1.19.3 – 1.19.4 |
| `+1.20.1` | 1.20 – 1.20.1 |
| `+1.20.4` | 1.20.3 – 1.20.4 |
| `+1.20.6` | 1.20.5 – 1.20.6 |
| `+1.21.1` | 1.21 – 1.21.1 |
| `+1.21.4` | 1.21.2 – 1.21.4 |
| `+1.21.5` | 1.21.5 |
| `+1.21.8` | 1.21.6 – 1.21.8 |
| `+1.21.10` | 1.21.9 – 1.21.10 |
| `+1.21.11` | 1.21.11 |
| `+26.1.2` | 26.1.x |
| `+26.2` | 26.2.x |
| `+26.3` | 26.3.x |

**Dünyayı paylaşan kişide [e4mc](https://modrinth.com/mod/e4mc) olmalı** (dünyayı internete açar; katılanlarda gerekmez).
Herkes aynı **Pantial sunucusunu** kullanmalı: `pantial-server.zip` içindeki sunucuyu bir yere kur
(README'de Render.com için tek tıklık kurulum var) ve adresini oyunda **Pantial → Ayarlar**'a yaz.

Her jar, kendi sürümünün geliştirme istemcisinde menüde tıklama, kod yazma, arkadaş ekleme/silme ve kapatma
adımlarıyla denendi. Ev sahibi + misafir iki istemcili deneme (reddetme, onaylama, yönetici yapma, atma)
1.21.10 ve 1.20.1 üzerinde yapıldı; e4mc tüneli üzerinden internetten bağlanma elle test edilmedi.

### 1.2.0'daki yenilikler
- **Kimler girebilir?** Varsayılan *sadece arkadaşlarım*. Arkadaşın olmayan biri girmeye çalışırsa reddedilir,
  sana bildirim gelir ve Dünyam sekmesinden **İzin ver** diyebilirsin.
- **Oyuncu bazlı yetki:** "Hileler herkese açık" yerine komutlar varsayılan olarak yalnızca sende; içerideki
  her oyuncu için **Yönetici** anahtarı ve **At** düğmesi.
- **Arkadaşlık istekleri:** Kod eklemek istek gönderir; karşı taraf kabul edince arkadaş olursunuz.
  Dünya adresi, isim ve davetler yalnızca karşılıklı arkadaşlar arasında görünür.
- Uzaktan sahibin adıyla bağlanma engellendi; kimlik doğrulamayı kapatmak onay istiyor.
- "Sunucu ayarlanmamış" ve "e4mc yok" durumları menüde açıkça anlatılıyor.
- Sunucu: IP başına istek sınırı, herkese açık uçlardan oyuncu bilgisi kaldırıldı, Render/Docker kurulum dosyaları.

**Uyumluluk:** Sunucu protokolü değişti; arkadaşlarınızla birlikte 1.2.0'a geçin ve sunucuyu da güncelleyin.

### 1.1.0
- Yeni Pantial menüsü, 15 Minecraft sürümü, kod sahipliği
