# Minecraft 1.18.2 (Caves & Cliffs Part II) FreeLAN / Pantial Mod Entegrasyonu

Minecraft 1.18.2, genişletilmiş dünya derinliği (Y: -64 ila 320) ve büyük dağ/mağara biyomları getiren dönüm noktası sürümlerden biridir. Pantial mod istemcisi, bu sürüm için hem **Fabric** hem de **Minecraft Forge** mod yükleyicileriyle tam uyumlu olarak yapılandırılmıştır.

---

## 1. Temel Özellikler & 1.19 / 1.20 Farklılıkları

- **Hedef Java Sürümü:** Java 17 (`compatibilityLevel = JAVA_17`). 1.18 sürümüyle birlikte Minecraft resmi olarak Java 17 gereksinimine geçmiştir.
- **Metin & Bileşen Modeli:** 1.19'da gelen `Component.literal(...)` yerine 1.18.2 standart `new TextComponent(...)` ve `new TranslatableComponent(...)` arayüzü kullanılmıştır.
- **Render Arayüzü:** 1.20 `GuiGraphics` yerine 1.18.2 standart `PoseStack matrices` ve `renderBackground(matrices)` arayüzü kullanılır.
- **Buton Yapısı:** `new Button(x, y, width, height, component, onPress)` yapıcısı kullanılmıştır.
- **Forge Olay Yöneticisi:** `ScreenEvent.InitScreenEvent.Post` dinleyicisi üzerinden butonlar TitleScreen, PauseScreen ve JoinMultiplayerScreen ekranlarına otomatik enjekte edilir.
- **Sunucu Hata Bildirimi:** Pantial varlık sunucusuna erişilemediğinde ekran üstünde kullanıcıyı bilgilendiren uyarı çubuğu (banner) yer alır.
- **e4mc Tünel Entegrasyonu:** e4mc v3/v4 protokolü üzerinden yerel dünyalar için anında `*.e4mc.link` genel tünel adresi üretir.

---

## 2. Dizin Yapısı

```
mods/1.18.2/
├── fabric/
│   ├── fabric.mod.json                 # Fabric mod manifesti (Minecraft 1.18 - 1.18.2)
│   ├── freelan.1.18.mixins.json        # 1.18.2 Mixin eşleşme konfigürasyonu
│   └── src/main/java/com/example/freelan/gui/
│       └── PantialFriendsScreen118.java # 1.18.2 PoseStack & TextComponent arayüzü
├── forge/
│   ├── META-INF/
│   │   └── mods.toml                   # Forge 1.18.2 (loader [40,)) mod tanımlayıcısı
│   └── src/main/java/com/example/freelan/forge/
│       └── FreeLanForgeEvents118.java  # 1.18.2 InitScreenEvent.Post dinleyicisi
└── README.md
```

---

## 3. Test ve Doğrulama
Mod yapılandırmaları, manifestler ve Java sınıfları `npm test` üzerinden otomatik olarak doğrulanmaktadır.
