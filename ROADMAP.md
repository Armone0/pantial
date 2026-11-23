# Pantial Yol Haritası

### Mod

- [x] **1.20.1 Fabric** – Gradle projesi (`mods/1.20.1/fabric`), derleniyor, `runClient` ile ana menüye kadar açılıyor
  - [x] Ana menü / duraklatma / çok oyunculu ekranlarına Pantial butonu
  - [x] "LAN'a Aç" yerine FreeLAN ekranı (oyun modu, hileler, online-mode, port)
  - [x] e4mc adresini sohbetten yakalayıp duyurma (yalnızca kendi dünyan açıkken)
  - [x] Arkadaş ekleme/silme, açık dünyaya katılma, davet gönderme / kabul / ret
  - [ ] Gerçek iki oyunculu uçtan uca deneme (e4mc ile)
- [ ] **1.20.1 Forge** – `PantialProfile` FabricLoader kullanıyor; ortak kod + Forge Gradle projesi gerekli
- [ ] **1.20.4 / 1.20.6 NeoForge** – yalnızca `neoforge.mods.toml` var
- [ ] **1.19.2 / 1.19.4** – yalnızca `PantialFriendsScreen119` taslağı (var olmayan API'leri çağırıyor)
- [ ] **1.18.2** – yalnızca `PantialFriendsScreen118` taslağı (var olmayan API'leri çağırıyor)
- [ ] **1.21.x**, **1.16.5** – başlanmadı

Öneri: yeni sürümlere geçmeden önce ortak kodu (profil, istemci, ağ) yükleyiciden bağımsız bir modüle ayırmak;
sürüm başına yalnızca ekran/mixin katmanı değişsin.

### Sunucu

- [x] Heartbeat zaman aşımı, davet süresi dolması
- [x] `PANT-XXXX` kodları
- [x] Kod sahipliği (`secret`), istek boyutu sınırı, açık uçlardan tünel adresi sızmaması
- [x] `npm test`
- [ ] Kalıcı depolama (şu an her şey bellekte)
- [ ] Hız sınırlama
