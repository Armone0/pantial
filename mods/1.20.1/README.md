# Minecraft 1.20.1 – FreeLAN / Pantial

## Fabric (çalışan sürüm)

`fabric/` bağımsız bir Gradle (Fabric Loom 1.9, Mojang mappings) projesidir.

```bash
cd fabric
./gradlew build        # build/libs/pantial-1.0.0+1.20.1.jar
./gradlew runClient
```

- Minecraft `~1.20.1`, Fabric Loader ≥ 0.14.21, Fabric API, Java 17.
- 1.20.2 ve sonrası desteklenmez (ekran API'leri değişti); manifest buna göre sınırlandı.

```
fabric/src/main/
├── resources/
│   ├── fabric.mod.json
│   ├── freelan.mixins.json
│   └── assets/freelan/lang/en_us.json
└── java/com/example/freelan/
    ├── FreeLAN.java              # ClientModInitializer
    ├── LanConfig.java            # tünel adresi, online-mode
    ├── PantialProfile.java       # config/pantial_profile.json (kod, secret, arkadaşlar, sunucu)
    ├── PantialClient.java        # arka plan yoklama, duyuru, davetler
    ├── gui/
    │   ├── FreeLanScreen.java        # "LAN'a Aç" yerine geçen ekran
    │   └── PantialFriendsScreen.java # arkadaş listesi, ekle/sil, katıl, davet
    └── mixin/
        ├── TitleScreenMixin.java, PauseScreenMixin.java, JoinMultiplayerScreenMixin.java  # Pantial butonu
        ├── ShareToLanScreenMixin.java  # FreeLanScreen'e yönlendirme
        ├── ChatComponentMixin.java     # e4mc adresini yakalama
        └── MinecraftServerMixin.java   # online-mode ayarı
```

**Online-mode:** Varsayılan açık. Kapatırsan tünel adresini bilen herkes istediği isimle (senin isminle de) girebilir.

## Forge / NeoForge (taslak)

`forge/` ve `neoforge/` derlenmez: Forge sınıfları Fabric projesindeki `PantialProfile` (FabricLoader kullanır) ve
ekranlara doğrudan bağımlı, Gradle projesi de yok. Ortak kod yükleyiciden bağımsız hale getirilince taşınabilir.
