# Pantial Minecraft Çoklu Sürüm Yol Haritası (Roadmap)

Pantial; `e4mc (cracked)` tünel modu ile `FreeLAN` varlık/arkadaş istemcisini bir araya getirerek Minecraft'ta port açmadan ve orijinal hesap zorunluluğu olmadan arkadaşların tek tıkla birlikte oynamasını sağlar.

Her hafta sonu (Cumartesi & Pazar) otomasyon sıradaki sürüm hedefini veya iyileştirmeyi tamamlayıp test eder ve repoya commit atar.

---

### Sürüm Destek Durumu

- [x] **1.21.1 (Modern Standart):**
  - [x] Fabric: `e4mc-fabric-26.1.2-cracked.jar` + `freelan-1.0.0.jar`
  - [x] NeoForge: `e4mc-neoforge-1.21.1-cracked.jar` + `freelan-neoforge-1.0.0.jar`
  - [x] Varlık sunucusu entegrasyonu ve tünel regex eşleşmesi

- [ ] **1.20.1 (Altın Çağ Mod Paketi Sürümü):**
  - [ ] Fabric: e4mc 1.20.1 cracked yaması ve FreeLAN Mixin portu
  - [ ] Forge / NeoForge: 1.20.1 uyumlu bağlantı ve arayüz sınıfları
  - [ ] 1.20.1 ağ paketi ve ekran yöneticisi testleri

- [ ] **1.20.4 / 1.20.6 (Geçiş Sürümleri):**
  - [ ] Fabric & NeoForge mod manifest ve bağımlılık aralığı genişletme
  - [ ] Arayüz buton konumlandırma uyumluluğu

- [ ] **1.19.2 & 1.19.4 (Wild Update):**
  - [ ] Fabric & Forge 1.19.x geriye dönük uyumluluk katmanı
  - [ ] Eski ChatComponent ve Screen API adaptörleri

- [ ] **1.18.2 (Caves & Cliffs Part II):**
  - [ ] Java 17 tabanlı Fabric/Forge 1.18.2 desteği
  - [ ] e4mc tünel protokolü uyumluluk doğrulaması

- [ ] **1.16.5 (Klasik Modding Temeli):**
  - [ ] Java 8 / 1.16.5 Forge uyumluluğu
  - [ ] Eski sunucu açma (ShareToLan) arayüzü Mixin uyarlaması

---

### Sunucu ve Altyapı İyileştirmeleri
- [x] `server.js`: Bağlantı kopmalarında otomatik zaman aşımı (heartbeat timeout) ekleme
- [ ] `server.js`: Arkadaş kodları için daha kısa ve akılda kalıcı format desteği
- [ ] Mod içi: Sunucuya bağlanamadığında kullanıcıya hata mesajı gösteren bildirim paneli
- [ ] Otomatik kurulum scripti (Windows & Linux için tek tıkla çalıştırılabilir başlatıcı)
