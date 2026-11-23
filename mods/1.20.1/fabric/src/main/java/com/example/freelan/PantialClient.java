package com.example.freelan;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;

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
public class PantialClient {
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private static final long POLL_MS = 3000;

    private static volatile boolean running = false;
    private static Thread pollerThread;
    private static volatile boolean registered = false;
    private static volatile boolean serverReachable = false;
    private static boolean codeConflictReported = false;
    private static boolean isCurrentlyHosting = false;
    private static String reportedTunnelUrl = "";
    private static boolean wasPublished = false;
    /** Arkadas listesi degisince ekranlarin yeniden cizilmesi icin artar. */
    private static volatile int revision = 0;

    public static final List<FriendStatus> friends = new CopyOnWriteArrayList<>();
    public static final List<InviteStatus> invites = new CopyOnWriteArrayList<>();

    public static class FriendStatus {
        public String userCode;
        public String username;
        public String uuid;
        public boolean online;
        public boolean isHosting;
        public String tunnelUrl;
    }

    public static class InviteStatus {
        public String senderCode;
        public String senderName;
        public String senderUuid;
        public String tunnelUrl;
        public long timestamp;
    }

    public static class StatusResponse {
        public List<FriendStatus> friends;
        public List<InviteStatus> invites;
    }

    public static boolean isServerReachable() {
        return serverReachable;
    }

    public static int getRevision() {
        return revision;
    }

    public static synchronized void start() {
        if (running) return;
        running = true;

        pollerThread = new Thread(() -> {
            FreeLAN.LOGGER.info("[PantialClient] Arka plan yoklama servisi baslatildi.");
            while (running) {
                try {
                    pollOnce();
                    Thread.sleep(POLL_MS);
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    FreeLAN.LOGGER.error("[PantialClient] Yoklama dongusunde hata:", e);
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ignored) {
                        break;
                    }
                }
            }
        }, "Pantial-Poller");

        pollerThread.setDaemon(true);
        pollerThread.start();
    }

    public static synchronized void stop() {
        running = false;
        if (pollerThread != null) {
            pollerThread.interrupt();
        }
    }

    /** Arayuzdeki "Yenile" butonu icin; yoklamayi bekletmeden bir tur calistirir. */
    public static void refreshNow() {
        Thread t = new Thread(() -> {
            try {
                pollOnce();
            } catch (Exception e) {
                FreeLAN.LOGGER.warn("[PantialClient] Yenileme basarisiz: {}", e.getMessage());
            }
        }, "Pantial-Refresh");
        t.setDaemon(true);
        t.start();
    }

    private static synchronized void pollOnce() {
        PantialProfile profile = PantialProfile.getInstance();
        Minecraft client = Minecraft.getInstance();

        if (!registered && client.getUser() != null) {
            register(profile, client.getUser().getName(),
                    client.getUser().getProfileId() != null ? client.getUser().getProfileId().toString() : "");
        }

        if (registered) {
            queryStatus(profile);
            handleHostingStatus(profile, client);
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
        return HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static void register(PantialProfile profile, String username, String uuid) {
        try {
            JsonObject json = new JsonObject();
            json.addProperty("username", username);
            json.addProperty("uuid", uuid);

            HttpResponse<String> response = post(profile, "/api/register", json);
            serverReachable = true;
            if (response.statusCode() == 200) {
                registered = true;
                FreeLAN.LOGGER.info("[PantialClient] Varlik sunucusuna kayit olundu ({})", profile.myUserCode);
            } else if (response.statusCode() == 403 && !codeConflictReported) {
                codeConflictReported = true;
                FreeLAN.LOGGER.warn("[PantialClient] {} kodu sunucuda baska bir anahtarla kayitli.", profile.myUserCode);
                showNotification("Kod Çakışması", profile.myUserCode + " başka biri tarafından kullanılıyor.");
            }
        } catch (Exception e) {
            serverReachable = false;
            FreeLAN.LOGGER.debug("[PantialClient] Kayit baglantisi basarisiz: {}", e.getMessage());
        }
    }

    private static void queryStatus(PantialProfile profile) {
        try {
            JsonObject json = new JsonObject();
            json.add("friendCodes", GSON.toJsonTree(profile.getFriendCodes()));

            HttpResponse<String> response = post(profile, "/api/status", json);
            serverReachable = true;
            if (response.statusCode() == 200) {
                StatusResponse status = GSON.fromJson(response.body(), StatusResponse.class);
                if (status != null) {
                    processStatusUpdates(status);
                }
            } else if (response.statusCode() == 401) {
                // Sunucu yeniden baslamis ve bizi unutmus: tekrar kayit ol, duyuruyu tazele.
                registered = false;
                isCurrentlyHosting = false;
            }
        } catch (Exception e) {
            serverReachable = false; // gecici erisim sorunlarinda log spamini onle
        }
    }

    private static void processStatusUpdates(StatusResponse status) {
        if (status.friends != null) {
            for (FriendStatus newFriend : status.friends) {
                FriendStatus oldFriend = friends.stream()
                        .filter(f -> f.userCode.equalsIgnoreCase(newFriend.userCode))
                        .findFirst().orElse(null);

                if (oldFriend != null) {
                    if (!oldFriend.online && newFriend.online) {
                        showNotification("Arkadaş Çevrim İçi", newFriend.username + " oyuna girdi!");
                    }
                    if (!oldFriend.isHosting && newFriend.isHosting) {
                        showNotification("Dünya Paylaşımı Açıldı", newFriend.username + " bir dünya başlattı!");
                    }
                }
            }
            friends.clear();
            friends.addAll(status.friends);
        }

        if (status.invites != null) {
            for (InviteStatus newInv : status.invites) {
                boolean isNew = invites.stream().noneMatch(i -> i.senderCode.equalsIgnoreCase(newInv.senderCode));
                if (isNew) {
                    showNotification("Yeni Oyun Daveti", newInv.senderName + " seni dünyasına davet etti!");
                }
            }
            invites.clear();
            invites.addAll(status.invites);
        }
        revision++;
    }

    private static void handleHostingStatus(PantialProfile profile, Minecraft client) {
        IntegratedServer server = client.getSingleplayerServer();
        boolean published = server != null && server.isPublished();
        String currentUrl = LanConfig.tunnelUrl;
        // Adres henuz yakalanmadiysa (e4mc yok / yavas) bos adres duyurmanin anlami yok.
        boolean announce = published && LanConfig.enableTunnel && !currentUrl.isEmpty();

        try {
            if (announce && (!isCurrentlyHosting || !currentUrl.equals(reportedTunnelUrl))) {
                JsonObject json = new JsonObject();
                json.addProperty("tunnelUrl", currentUrl);
                if (post(profile, "/api/host", json).statusCode() == 200) {
                    isCurrentlyHosting = true;
                    reportedTunnelUrl = currentUrl;
                    FreeLAN.LOGGER.info("[PantialClient] Paylasilan dunya duyuruldu: {}", currentUrl);
                }
            } else if (!announce && isCurrentlyHosting) {
                if (post(profile, "/api/stop-host", new JsonObject()).statusCode() == 200) {
                    isCurrentlyHosting = false;
                    reportedTunnelUrl = "";
                    FreeLAN.LOGGER.info("[PantialClient] Dunya paylasimi sonlandirildi.");
                }
            }
        } catch (Exception e) {
            serverReachable = false;
        }

        if (wasPublished && !published) {
            // Dunya kapandi: bir sonraki dunyaya eski adres/ayar tasinmasin.
            LanConfig.tunnelUrl = "";
            LanConfig.tunnelStatus = "Hazır";
            LanConfig.onlineMode = true;
        }
        wasPublished = published;
    }

    /** Arkadasi kendi acik dunyamiza davet eder. */
    public static void sendInvite(String friendCode) {
        String url = LanConfig.tunnelUrl;
        if (url.isEmpty()) {
            showNotification("Davet Gönderilemedi", "Önce dünyanı FreeLAN ile paylaşmalısın.");
            return;
        }
        Thread t = new Thread(() -> {
            try {
                JsonObject json = new JsonObject();
                json.addProperty("to", friendCode);
                json.addProperty("tunnelUrl", url);
                int code = post(PantialProfile.getInstance(), "/api/invite", json).statusCode();
                showNotification(code == 200 ? "Davet Gönderildi" : "Davet Gönderilemedi", friendCode);
            } catch (Exception e) {
                showNotification("Davet Gönderilemedi", "Sunucuya ulaşılamıyor.");
            }
        }, "Pantial-Invite");
        t.setDaemon(true);
        t.start();
    }

    /** Daveti sunucudan siler (katildiktan veya reddettikten sonra). */
    public static void dismissInvite(InviteStatus invite) {
        invites.remove(invite);
        revision++;
        Thread t = new Thread(() -> {
            try {
                JsonObject json = new JsonObject();
                json.addProperty("from", invite.senderCode);
                post(PantialProfile.getInstance(), "/api/reject-invite", json);
            } catch (Exception ignored) {
            }
        }, "Pantial-Invite");
        t.setDaemon(true);
        t.start();
    }

    public static void showNotification(String title, String message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;

        mc.execute(() -> {
            try {
                SystemToast.addOrUpdate(
                        mc.getToasts(),
                        SystemToast.SystemToastIds.PERIODIC_NOTIFICATION,
                        Component.literal("§6[Pantial] §f" + title),
                        Component.literal(message)
                );
            } catch (Throwable t) {
                FreeLAN.LOGGER.info("[Pantial Toast] {}: {}", title, message);
            }
        });
    }
}
