# Minecraft 1.19.2 & 1.19.4 (The Wild Update) FreeLAN / Pantial Mod Entegrasyonu

Minecraft 1.19.2 ve 1.19.4, modlama dünyasında derin mağaralar ve antik şehirler (Deep Dark) dönemi için en yaygın kullanılan mod paketi sürümlerindendir. Pantial mod istemcisi, bu sürüm için hem **Fabric** hem de **Minecraft Forge** yükleyicileriyle tam uyumlu olarak yapılandırılmıştır.

---

## 1. Temel Özellikler & 1.20 Farklılıkları

- **Hedef Java Sürümü:** Java 17 (`compatibilityLevel = JAVA_17`).
- **Render Arayüzü:** 1.20 sürümündeki `GuiGraphics` yerine 1.19.2 standart `PoseStack` ve `renderBackground(matrices)` arayüzü kullanılır.
- **Buton Yapısı:** 1.20'de gelen `Button.builder(...)` yerine 1.19.2'de geçerli olan `new Button(x, y, width, height, text, onPress)` yapıcısı kullanılmıştır.
- **Fabric Loader:** `>=0.14.9` desteği ve Fabric API entegrasyonu.
- **Minecraft Forge:** `[43.1.0,)` standart Forge olay yöneticisi (`ScreenEvent.Init.Post`).
- **e4mc Tünel Entegrasyonu:** Chat log yakalama ve `*.e4mc.link` duyurusu ile port açmadan doğrudan dünyalara katılma.

---

## 2. Dizin Yapısı

```
mods/1.19.2/
├── fabric/
│   ├── fabric.mod.json                 # Fabric mod manifesti (Minecraft 1.19 - 1.19.4)
│   ├── freelan.1.19.mixins.json        # Mixin eşleşme konfigürasyonu
│   └── src/main/java/com/example/freelan/gui/
│       └── PantialFriendsScreen119.java # 1.19.x PoseStack & Button arayüzü
├── forge/
│   ├── META-INF/
│   │   └── mods.toml                   # Forge 1.19.2-1.19.4 mod tanımlayıcısı
│   └── src/main/java/com/example/freelan/forge/
│       └── FreeLanForgeEvents119.java  # 1.19.2 PoseStack uyumlu ScreenEvent dinleyicileri
└── README.md
```

---

## 3. Test ve Doğrulama
Mod yapılandırmaları ve JSON şemaları `npm test` üzerinden otomatik olarak doğrulanmaktadır.
