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

## Desteklenen Sürümler

| Minecraft Sürümü | Fabric | NeoForge / Forge | Durum |
| :--- | :---: | :---: | :--- |
| **1.21.1** | Destekleniyor | Destekleniyor | Stabil |
| **1.20.1** | Geliştiriliyor | Geliştiriliyor | Yol Haritasında |
| **1.20.4** | Planlandı | Planlandı | Yol Haritasında |
| **1.19.2** | Planlandı | Planlandı | Yol Haritasında |
| **1.18.2** | Planlandı | Planlandı | Yol Haritasında |
| **1.16.5** | Planlandı | Planlandı | Yol Haritasında |

Detaylı gelişim takvimi için [ROADMAP.md](ROADMAP.md) dosyasını inceleyebilirsiniz.

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
- `POST /api/register` - Kullanıcı kaydı ve oturum açma
- `POST /api/host` - Yerel dünya tünel adresini duyurma
- `POST /api/status` - Arkadaş listesi ve gelen davetleri sorgulama
- `POST /api/invite` - Arkadaşını paylaşılan dünyaya davet etme
- `POST /api/reject-invite` - Daveti geri çevirme
- `POST /api/stop-host` - Dünya paylaşımını sonlandırma

---

## Lisans ve Kullanım Şartları
Bu proje yalnızca yerel ağ bağlantısı kolaylaştırma ve açık kaynaklı eğitim/deney amacıyla geliştirilmektedir.
