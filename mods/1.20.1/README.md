# Minecraft 1.20.1 (LTS) FreeLAN / Pantial Mod Entegrasyonu

Minecraft 1.20.1, modlama ekosisteminin en stabil ve geniş mod paketlerine sahip LTS (Long Term Support) sürümüdür. Pantial mod istemcisi, bu sürüm için hem **Fabric** hem de **Minecraft Forge** yükleyicileriyle tam uyumlu olarak yapılandırılmıştır.

---

## 1. Temel Özellikler & Farklılıklar

- **Hedef Java Sürümü:** Java 17 (`compatibilityLevel = JAVA_17`).
- **Fabric Loader:** `>=0.14.21` desteği ve Fabric API entegrasyonu.
- **Minecraft Forge:** `>=47.1.0` standart Forge olay yöneticisi (`ScreenEvent.Init.Post`).
- **Görsel Bildirim Sistemi:** 1.20.1 `SystemToast.SystemToastIds.PERIODIC_NOTIFICATION` arayüzü ile arkadaşların çevrim içi olma ve dünya açma bildirimleri.
- **e4mc Tünel Entegrasyonu:** Sohbet penceresine düşen `*.e4mc.link` adreslerini otomatik regex ile yakalama ve varlık sunucusuna duyurma.

---

## 2. Dizin Yapısı

```
mods/1.20.1/
├── fabric/
│   ├── fabric.mod.json                 # Fabric mod manifesti (Minecraft 1.20.1, Java 17)
│   ├── freelan.1.20.1.mixins.json      # Mixin eşleşme konfigürasyonu
│   └── src/main/java/com/example/freelan/
│       ├── FreeLAN.java                # Fabric ModInitializer
│       ├── LanConfig.java              # Tünel ve yerel ağ durumları
│       ├── PantialProfile.java         # PANT-XXXX kodu ve sunucu profili
│       ├── PantialClient.java          # Java 17 HttpClient yoklama istemcisi
│       ├── gui/
│       │   └── PantialFriendsScreen.java # 1.20.1 GuiGraphics & Button arayüzü
│       └── mixin/
│           ├── TitleScreenMixin.java   # Ana menüye Pantial butonu ekleme
│           ├── PauseScreenMixin.java   # Oyun içi menüye Pantial butonu ekleme
│           ├── ShareToLanScreenMixin.java
│           └── ChatComponentMixin.java # e4mc regex yakalama
└── forge/
    ├── META-INF/
    │   └── mods.toml                   # Forge 1.20.1 mod tanımlayıcısı
    └── src/main/java/com/example/freelan/forge/
        ├── FreeLanForge.java           # @Mod("freelan") Forge başlangıcı
        └── FreeLanForgeEvents.java     # Forge ScreenEvent dinleyicileri
```

---

## 3. Test ve Doğrulama
Mod yapılandırmaları ve JSON şemaları `npm test` üzerinden otomatik olarak doğrulanmaktadır.
