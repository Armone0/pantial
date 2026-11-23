package io.github.armone0.pantial;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Paylasilan dunyaya kimin girebilecegine karar verir. Sunucu thread'inden (giris aninda)
 * ve render thread'inden (menu) okunur.
 */
public final class Access {
    public enum Mode { FRIENDS, ANYONE }

    public enum Commands { ME, EVERYONE }

    public static final class JoinRequest {
        public final String name;
        public final String uuid;
        public volatile long lastTry;

        JoinRequest(String name, String uuid) {
            this.name = name;
            this.uuid = uuid;
            this.lastTry = System.currentTimeMillis();
        }
    }

    public static volatile Mode mode = Mode.FRIENDS;
    public static volatile Commands commands = Commands.ME;
    /** Bu oturumda elle izin verilenler (normalize UUID ya da kucuk harf isim). */
    private static final Set<String> allowed = ConcurrentHashMap.newKeySet();
    public static final List<JoinRequest> requests = new CopyOnWriteArrayList<>();

    private Access() {
    }

    public static String normalizeUuid(String uuid) {
        return uuid == null ? "" : uuid.replace("-", "").toLowerCase(Locale.ROOT);
    }

    private static String key(String uuid, String name) {
        // Kimlik dogrulama kapaliyken UUID isimden turetilir; o durumda isimle eslestirmek zorundayiz.
        return LanState.onlineMode ? "u:" + normalizeUuid(uuid) : "n:" + (name == null ? "" : name.toLowerCase(Locale.ROOT));
    }

    /**
     * Giris denetimi (sunucu thread'i, PlayerList.canPlayerLogin).
     *
     * @param local    baglanti bu bilgisayardan mi (dunyanin sahibi her zaman yerelden baglanir)
     * @param hostName dunyanin sahibinin adi
     * @return null ise girebilir, degilse oyuncuya gosterilecek mesaj
     */
    public static String check(String uuid, String name, boolean local, String hostName) {
        if (local) return null;
        // Minecraft sahibi isme gore taniyor: uzaktan sahibin adiyla gelen biri tam yetki alirdi.
        if (hostName != null && hostName.equalsIgnoreCase(name)) {
            return "Bu isim dünyanın sahibine ait.";
        }
        if (mode == Mode.ANYONE) return null;
        String k = key(uuid, name);
        if (allowed.contains(k)) return null;
        for (PantialClient.FriendStatus f : PantialClient.friends) {
            if (!f.mutual()) continue;
            if (LanState.onlineMode ? normalizeUuid(f.uuid).equals(normalizeUuid(uuid))
                    : f.username != null && f.username.equalsIgnoreCase(name)) {
                return null;
            }
        }
        recordRequest(uuid, name);
        return "Bu dünya yalnızca sahibinin Pantial arkadaşlarına açık.\nSahibi seni onaylarsa tekrar bağlan.";
    }

    private static void recordRequest(String uuid, String name) {
        for (JoinRequest r : requests) {
            if (r.name.equalsIgnoreCase(name)) {
                r.lastTry = System.currentTimeMillis();
                return;
            }
        }
        requests.add(new JoinRequest(name, uuid));
        PantialClient.notify("Giriş isteği", name + " dünyana girmek istiyor");
    }

    public static void approve(JoinRequest r) {
        allowed.add(key(r.uuid, r.name));
        requests.remove(r);
    }

    public static void deny(JoinRequest r) {
        requests.remove(r);
    }

    public static void reset() {
        allowed.clear();
        requests.clear();
        mode = Mode.FRIENDS;
        commands = Commands.ME;
    }
}
