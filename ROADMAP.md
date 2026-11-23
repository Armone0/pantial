# Pantial Yol Haritası

### Mod (Fabric)

- [x] Tek kod tabanı, sürüm başına ince platform katmanı (`mod/`)
- [x] 15 jar: 1.18.2 → 26.3 (bkz. README'deki tablo)
- [x] Yeni menü: Arkadaşlar / Davetler / Dünyam / Ayarlar
- [x] Ana menü, duraklatma ve çok oyunculu ekranlarında Pantial düğmesi
- [x] Etiketle otomatik GitHub Release (tüm jar'lar + sunucu paketi)
- [ ] Gerçek iki oyunculu uçtan uca deneme (e4mc ile), her sürüm ailesinde
- [ ] Oyuncu kafası (skin) avatarları
- [ ] Menüyü açan kısayol tuşu

### Diğer yükleyiciler

- [ ] NeoForge (1.20.6+): `common/` aynen kullanılabilir, yalnızca giriş noktası ve ekran olayları yazılmalı
- [ ] Forge 1.20.1

### Sunucu

- [x] Heartbeat zaman aşımı, davet süresi dolması, `PANT-XXXX` kodları
- [x] Kod sahipliği (`secret`), istek boyutu sınırı, açık uçlardan tünel adresi sızmaması
- [x] `npm test`
- [ ] Kalıcı depolama (şu an her şey bellekte)
- [ ] Hız sınırlama
