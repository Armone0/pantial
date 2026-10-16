# Pantial - Minecraft Çoklu Sürüm Korsan & Arkadaş Ağı

Pantial; port açma derdi olmadan, Hamachi veya Radmin gibi karmaşık VPN programlarına gerek kalmadan, arkadaşlarınızla Minecraft dünyalarınızı paylaşmanızı sağlayan **offline/korsan (cracked) hesap uyumlu** bir mod ve sunucu ekosistemidir.

---

## Nasıl Çalışır?

1. **`e4mc (cracked)`:** Dünyanızı yerel ağa (LAN) açtığınızda e4mc tünel protokolü üzerinden otomatik bir genel adres (`*.e4mc.link`) üretir. Orijinal olmayan Minecraft hesaplarının kimlik doğrulama engeline takılmadan bağlanabilmesini sağlar.
2. **`FreeLAN` Modu:** Essential moduna benzer modern bir arkadaş listesi ve arayüz sunar. Oyuncu kodunuzu arkadaşınızla paylaştığınızda, arka planda tünel adresini alıp otomatik olarak birbirinizi görmenizi ve tek tıkla oyuna katılmanızı sağlar.
3. **`pantial-presence-server` (`server.js`):** Oyuncuların çevrim içi durumlarını, davetlerini ve tünel bağlantılarını koordine eden hafif Node.js varlık sunucusudur.

---

## Desteklenen Sürümler ve Mod Yükleyicileri

| Minecraft Sürümü | Fabric | NeoForge / Forge | Durum |
| :--- | :---: | :---: | :--- |
| **1.21.1** | Destekleniyor (`mods/1.21.1/fabric`) | Destekleniyor (`mods/1.21.1/neoforge`) | Stabil |
| **1.20.1** | Geliştiriliyor | Geliştiriliyor | Yol Haritasında |
| **1.20.4** | Planlandı | Planlandı | Yol Haritasında |
| **1.19.2** | Planlandı | Planlandı | Yol Haritasında |
| **1.18.2** | Planlandı | Planlandı | Yol Haritasında |
| **1.16.5** | Planlandı | Planlandı | Yol Haritasında |

Detaylı gelişim takvimi için [ROADMAP.md](ROADMAP.md) dosyasını inceleyebilirsiniz.

---

## Kurulum

### Oyuncular İçin (İstemci)
1. Kullandığınız Minecraft sürümüne ve mod yükleyicinize (Fabric veya NeoForge) uygun klasördeki (`mods/<surum>/<loader>`) 2 adet `.jar` dosyasını indirin:
   * `e4mc-*.jar`
   * `freelan-*.jar`
2. Bu iki dosyayı `.minecraft/mods` klasörünüze atın.
3. Oyunu başlatın. Ana menüde ve duraklatma ekranında Pantial/FreeLAN arayüzünü göreceksiniz.

### Sunucu Yöneticisi İçin (Presence Server)
```bash
node server.js
```
Varsayılan olarak `3000` portunda çalışır.
