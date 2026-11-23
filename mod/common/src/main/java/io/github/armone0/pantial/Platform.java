package io.github.armone0.pantial;

import java.nio.file.Path;

/**
 * Minecraft surumune ozel her sey bu arayuzun arkasinda. Ortak kod (profil, ag, menu)
 * net.minecraft siniflarina hic dokunmaz; her surumun platform katmani bunu uygular.
 */
public interface Platform {
    enum WorldState { NONE, REMOTE, SINGLEPLAYER, PUBLISHED }

    enum GameMode { SURVIVAL, CREATIVE, ADVENTURE, SPECTATOR }

    String minecraftVersion();

    Path configDir();

    /** Oturumdaki oyuncu adi; henuz yoksa null. */
    String playerName();

    String playerUuid();

    boolean isModLoaded(String id);

    void runOnClientThread(Runnable task);

    void toast(String title, String message);

    WorldState worldState();

    /** Paylasilan dunyanin yerel portu; paylasim yoksa -1. */
    int publishedPort();

    boolean publishWorld(GameMode mode, boolean allowCheats, int port);

    /** 26.x'te LAN acarken oyun modu secilmiyor; menu bu satiri gizler. */
    default boolean supportsGameMode() {
        return true;
    }

    /** Menuyu kapatip verilen adrese baglanir. */
    void connect(String address, String displayName);

    String getClipboard();

    void setClipboard(String text);

    /** Paylasilan dunyadaki bir oyuncu (menude yetki/atma listesi icin). */
    final class WorldPlayer {
        public final String name;
        public final String uuid;
        public final boolean op;
        public final boolean host;

        public WorldPlayer(String name, String uuid, boolean op, boolean host) {
            this.name = name;
            this.uuid = uuid;
            this.op = op;
            this.host = host;
        }
    }

    /** Kendi dunyamizdaki oyuncular; dunya yoksa bos. */
    java.util.List<WorldPlayer> worldPlayers();

    /** Oyuncuya yonetici (op) yetkisi ver/al. uuid ve isim worldPlayers()'tan gelir. */
    void setOp(String uuid, String name, boolean op);

    /** Oyuncuyu dunyadan atar. */
    void kick(String uuid, String message);

    /** Metin kutusu odak alinca/birakinca (26.x/SDL3'te klavyeden metin almak icin gerekli). */
    default void textInputFocus(boolean focused) {
    }

    /** Menuyu kapatip onceki ekrana doner. */
    void closeMenu();
}
