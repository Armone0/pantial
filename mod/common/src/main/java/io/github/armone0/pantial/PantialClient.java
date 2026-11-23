package io.github.armone0.pantial;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Varlik sunucusunu arka planda yoklar: kayit, arkadas durumlari, davetler ve dunya duyurusu.
 * Listeler arka plan thread'inde yazilip render thread'inde okundugu icin CopyOnWriteArrayList.
 */
public final class PantialClient {
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private static final long POLL_MS = 3000;

    private static volatile boolean running;
    private static Thread poller;
    private static volatile boolean registered;
    private static volatile boolean reachable;
    private static volatile boolean everContacted;
    private static boolean conflictReported;
    private static boolean hostingAnnounced;
    private static String announcedUrl = "";
    private static boolean wasPublished;

    public static final List<FriendStatus> friends = new CopyOnWriteArrayList<>();
    public static final List<InviteStatus> invites = new CopyOnWriteArrayList<>();
    public static final List<FriendRequest> requests = new CopyOnWriteArrayList<>();

    public static class FriendStatus {
        /** "mutual" (karsilikli), "pending" (istek gonderildi) ya da "unknown" (kod yok). */
        public String state;
        public String userCode;
        public String username;
        public String uuid;
        public boolean online;
        public boolean isHosting;
        public String tunnelUrl;

        public boolean mutual() {
            return "mutual".equals(state);
        }

        public boolean joinable() {
            return mutual() && online && isHosting && tunnelUrl != null && !tunnelUrl.isEmpty();
        }
    }

    public static class InviteStatus {
        public String senderCode;
        public String senderName;
        public String senderUuid;
        public String tunnelUrl;
        public long timestamp;
    }

    public static class FriendRequest {
        public String userCode;
        public String username;
    }

    private static class StatusResponse {
        List<FriendStatus> friends;
        List<FriendRequest> requests;
        List<InviteStatus> invites;
    }

    private PantialClient() {
    }

    public static boolean isReachable() {
        return reachable;
    }

    /** Ilk yanit gelmeden "ulasilamiyor" yazmamak icin. */
    public static boolean hasContacted() {
        return everContacted;
    }

    public static boolean isAnnounced() {
        return hostingAnnounced;
    }

    public static long onlineFriendCount() {
        return friends.stream().filter(f -> f.mutual() && f.online).count();
    }

    public static synchronized void start() {
        if (running) return;
        running = true;
        poller = new Thread(() -> {
            while (running) {
                try {
                    pollOnce();
                    Thread.sleep(POLL_MS);
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    Pantial.LOGGER.error("[Pantial] Yoklama hatasi", e);
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ignored) {
                        break;
                    }
                }
            }
        }, "Pantial-Poller");
        poller.setDaemon(true);
        poller.start();
    }

    /** "Yenile" ve ayar degisiklikleri icin: beklemeden bir tur calistirir. */
    public static void refreshNow() {
        async("Pantial-Refresh", PantialClient::pollOnce);
    }

    /** Sunucu adresi degisince yeniden kayit olur. */
    public static void serverChanged() {
        registered = false;
        reachable = false;
        everContacted = false;
        hostingAnnounced = false;
        conflictReported = false;
        friends.clear();
        invites.clear();
        requests.clear();
        refreshNow();
    }

    private static synchronized void pollOnce() {
        Platform platform = Pantial.platform();
        PantialProfile profile = PantialProfile.getInstance();
        String name = platform.playerName();
        if (!registered && name != null) {
            register(profile, name, platform.playerUuid());
        }
        if (registered) {
            queryStatus(profile);
            handleHosting(profile, platform);
        }
    }

    private static HttpResponse<String> post(PantialProfile profile, String path, JsonObject json) throws Exception {
        json.addProperty("userCode", profile.myUserCode);
        json.addProperty("secret", profile.secret);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(profile.serverUrl + path))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(json)))
                .build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        reachable = true;
        everContacted = true;
        return response;
    }

    private static void register(PantialProfile profile, String username, String uuid) {
        try {
            JsonObject json = new JsonObject();
            json.addProperty("username", username);
            json.addProperty("uuid", uuid == null ? "" : uuid);
            HttpResponse<String> response = post(profile, "/api/register", json);
            if (response.statusCode() == 200) {
                registered = true;
                Pantial.LOGGER.info("[Pantial] Sunucuya kayit olundu ({})", profile.myUserCode);
            } else if (response.statusCode() == 403 && !conflictReported) {
                conflictReported = true;
                notify("Kod çakışması", profile.myUserCode + " başka biri tarafından kullanılıyor.");
            }
        } catch (Exception e) {
            offline();
        }
    }

    private static void queryStatus(PantialProfile profile) {
        try {
            JsonObject json = new JsonObject();
            json.add("friendCodes", GSON.toJsonTree(profile.getFriendCodes()));
            json.add("declinedCodes", GSON.toJsonTree(profile.getDeclinedCodes()));
            HttpResponse<String> response = post(profile, "/api/status", json);
            if (response.statusCode() == 200) {
                StatusResponse status = GSON.fromJson(response.body(), StatusResponse.class);
                if (status != null) apply(status);
            } else if (response.statusCode() == 401) {
                // Sunucu yeniden baslamis: tekrar kayit ol, duyuruyu tazele.
                registered = false;
                hostingAnnounced = false;
            }
        } catch (Exception e) {
            offline();
        }
    }

    private static void offline() {
        reachable = false;
        everContacted = true;
    }

    private static void apply(StatusResponse status) {
        if (status.friends != null) {
            for (FriendStatus now : status.friends) {
                for (FriendStatus before : friends) {
                    if (!before.userCode.equalsIgnoreCase(now.userCode)) continue;
                    if (!before.mutual() && now.mutual()) notify("Arkadaşlık kabul edildi", now.username + " artık arkadaşın");
                    else if (!before.online && now.online) notify("Arkadaşın çevrim içi", now.username + " oyuna girdi");
                    if (!before.joinable() && now.joinable()) notify("Dünya açıldı", now.username + " dünyasını paylaştı");
                }
            }
            friends.clear();
            friends.addAll(status.friends);
        }
        if (status.requests != null) {
            for (FriendRequest r : status.requests) {
                if (requests.stream().noneMatch(x -> x.userCode.equalsIgnoreCase(r.userCode))) {
                    notify("Arkadaşlık isteği", r.username + " seni eklemek istiyor");
                }
            }
            requests.clear();
            requests.addAll(status.requests);
        }
        if (status.invites != null) {
            for (InviteStatus inv : status.invites) {
                if (invites.stream().noneMatch(i -> i.senderCode.equalsIgnoreCase(inv.senderCode))) {
                    notify("Yeni davet", inv.senderName + " seni dünyasına çağırıyor");
                }
            }
            invites.clear();
            invites.addAll(status.invites);
        }
    }

    private static void handleHosting(PantialProfile profile, Platform platform) {
        boolean published = platform.worldState() == Platform.WorldState.PUBLISHED;
        String url = LanState.tunnelUrl;
        boolean announce = published && LanState.announce && !url.isEmpty();
        try {
            if (announce && (!hostingAnnounced || !url.equals(announcedUrl))) {
                JsonObject json = new JsonObject();
                json.addProperty("tunnelUrl", url);
                if (post(profile, "/api/host", json).statusCode() == 200) {
                    hostingAnnounced = true;
                    announcedUrl = url;
                }
            } else if (!announce && hostingAnnounced) {
                if (post(profile, "/api/stop-host", new JsonObject()).statusCode() == 200) {
                    hostingAnnounced = false;
                    announcedUrl = "";
                }
            }
        } catch (Exception e) {
            offline();
        }
        if (wasPublished && !published) {
            // Dunya kapandi: sonraki dunyaya eski adres/ayar tasinmasin.
            LanState.reset();
        }
        wasPublished = published;
    }

    public static void sendInvite(String friendCode, Runnable onDone) {
        String url = LanState.tunnelUrl;
        if (url.isEmpty()) return;
        async("Pantial-Invite", () -> {
            try {
                JsonObject json = new JsonObject();
                json.addProperty("to", friendCode);
                json.addProperty("tunnelUrl", url);
                post(PantialProfile.getInstance(), "/api/invite", json);
            } catch (Exception e) {
                offline();
            }
            if (onDone != null) onDone.run();
        });
    }

    public static void dismissInvite(InviteStatus invite) {
        invites.remove(invite);
        async("Pantial-Invite", () -> {
            try {
                JsonObject json = new JsonObject();
                json.addProperty("from", invite.senderCode);
                post(PantialProfile.getInstance(), "/api/reject-invite", json);
            } catch (Exception e) {
                offline();
            }
        });
    }

    public static void notify(String title, String message) {
        if (!PantialProfile.getInstance().notifications) return;
        Platform p = Pantial.platform();
        if (p != null) p.runOnClientThread(() -> p.toast(title, message));
    }

    private static void async(String name, Runnable task) {
        Thread t = new Thread(task, name);
        t.setDaemon(true);
        t.start();
    }
}
