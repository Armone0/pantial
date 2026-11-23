## Pantial – arkadaşlarınla port açmadan oyna

### Bu sürümde yalnızca Minecraft 26.3 var

`pantial-fabric-1.2.0+26.3.jar` dosyasını indirip `mods` klasörüne at (Fabric Loader + Fabric API gerekir).
Diğer Minecraft sürümleri için [v1.1.0](https://github.com/Armone0/pantial/releases/tag/v1.1.0)'daki jar'lar geçerli;
yeni sunucuyla da çalışırlar ama aşağıdaki yeni özellikler (arkadaşlık istekleri, giriş onayı, oyuncu bazlı yetki) onlarda yok.

**Dünyayı paylaşan kişide [e4mc](https://modrinth.com/mod/e4mc) olmalı** (dünyayı internete açar; katılanlarda gerekmez).
Herkes aynı **Pantial sunucusunu** kullanmalı: `pantial-server.zip` içindeki sunucuyu bir yere kur
(README'de Render.com için tek tıklık kurulum var) ve adresini oyunda **Pantial → Ayarlar**'a yaz.

26.3'te ev sahibi + misafir iki istemciyle denendi: arkadaş olmayanın reddedilmesi, giriş isteğini onaylama,
yönetici yetkisi (önce komut reddedildi, yetki verilince çalıştı) ve atma. e4mc tüneli üzerinden internetten
bağlanma elle test edilmedi.

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

**Uyumluluk:** Sunucu 1.3.0'a güncellenmeli (`pantial-server.zip`). 1.1.0 modları yeni sunucuya bağlanır;
karşılıklı eklenmiş arkadaşları görür ama arkadaşlık isteklerini göremez.

### 1.1.0
- Yeni Pantial menüsü, 15 Minecraft sürümü, kod sahipliği
