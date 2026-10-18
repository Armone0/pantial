# Minecraft 1.20.1 Uyumluluk ve Mod Yapılandırması

Minecraft 1.20.1, günümüz mod ekosisteminde en popüler LTS (Long Term Support) sürümlerinden biridir. 1.21.1 sürümünden farklı olarak Java 17 ve eski Mixin kütüphane imzalarını kullanır.

## Gereksinimler
- **Java Sürümü:** Java 17 (Java 21 zorunluluğu 1.20.5 ile gelmiştir)
- **Mod Yükleyicileri:**
  - Fabric Loader: `>=0.14.21` (Fabric API `>=0.83.0+1.20.1`)
  - Forge: `>=47.1.0` (1.20.1 döneminde NeoForge henüz bağımsızlaşma aşamasındaydı, ana yükleyici standart Minecraft Forge'dur)

## e4mc & FreeLAN 1.20.1 Entegrasyon Notları
1. **e4mc 1.20.1:** Orijinal `e4mc-minecraft-1.20.1-fabric` ve `forge` sürümlerinin offline/cracked yama protokolü.
2. **FreeLAN 1.20.1:** `ShareToLanScreenMixin` ve `TitleScreenMixin` sınıflarında 1.20.1 GUI element indeksleri (`addRenderableWidget`) ile uyumlu arayüz entegrasyonu.
3. **Varlık Sunucusu (`server.js`):** Sürümden bağımsız olarak `http://localhost:3000` (veya barındırılan sunucu adresi) üzerinden çalışır.
