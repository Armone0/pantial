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
import java.util.ArrayList;
import java.util.List;

public class PantialClient {
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static volatile boolean running = false;
    private static Thread pollerThread;
    private static boolean registered = false;
    private static boolean isCurrentlyHosting = false;
    private static String reportedTunnelUrl = "";

    public static final List<FriendStatus> friends = new ArrayList<>();
    public static final List<InviteStatus> invites = new ArrayList<>();

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

    public static synchronized void start() {
        if (running) return;
        running = true;

        pollerThread = new Thread(() -> {
            FreeLAN.LOGGER.info("[PantialClient 1.20.1] Arka plan yoklama servisi baslatildi.");
            while (running) {
                try {
                    PantialProfile profile = PantialProfile.getInstance();
                    Minecraft client = Minecraft.getInstance();

                    if (!registered && client.getUser() != null) {
                        register(profile.serverUrl, profile.myUserCode, client.getUser().getName(),
                                client.getUser().getProfileId() != null ? client.getUser().getProfileId().toString() : "");
                    }

                    if (registered) {
                        queryStatus(profile.serverUrl, profile.myUserCode, profile.friendCodes);
                        handleHostingStatus(profile.serverUrl, profile.myUserCode, client);
                    }

                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    FreeLAN.LOGGER.error("[PantialClient] Yoklama dongusunde hata:", e);
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ignored) {}
                }
            }
        }, "Pantial-Poller-1.20.1");

        pollerThread.setDaemon(true);
        pollerThread.start();
    }

    public static synchronized void stop() {
        running = false;
        if (pollerThread != null) {
            pollerThread.interrupt();
        }
    }

    private static void register(String baseUrl, String userCode, String username, String uuid) {
        try {
            JsonObject json = new JsonObject();
            json.addProperty("userCode", userCode);
            json.addProperty("username", username);
            json.addProperty("uuid", uuid);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/register"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(json)))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                registered = true;
                FreeLAN.LOGGER.info("[PantialClient] Varlik sunucusuna basariyla kayit olundu ({})", userCode);
            }
        } catch (Exception e) {
            FreeLAN.LOGGER.warn("[PantialClient] Varlik sunucusuna kayit baglantisi basarisiz: {}", e.getMessage());
        }
    }

    private static void queryStatus(String baseUrl, String userCode, List<String> friendCodes) {
        try {
            JsonObject json = new JsonObject();
            json.addProperty("userCode", userCode);
            json.add("friendCodes", GSON.toJsonTree(friendCodes));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/status"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(json)))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                StatusResponse status = GSON.fromJson(response.body(), StatusResponse.class);
                if (status != null) {
                    processStatusUpdates(status);
                }
            }
        } catch (Exception e) {
            // Sunucu gecici ulasilamazsa log spamini onle
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
    }

    private static void handleHostingStatus(String baseUrl, String userCode, Minecraft client) {
        try {
            IntegratedServer server = client.getSingleplayerServer();
            boolean hostingNow = server != null && server.isPublished() && LanConfig.enableTunnel;
            String currentUrl = LanConfig.tunnelUrl;

            if (hostingNow && (!isCurrentlyHosting || !currentUrl.equals(reportedTunnelUrl))) {
                JsonObject json = new JsonObject();
                json.addProperty("userCode", userCode);
                json.addProperty("tunnelUrl", currentUrl);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + "/api/host"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(json)))
                        .build();

                HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString());
                isCurrentlyHosting = true;
                reportedTunnelUrl = currentUrl;
                FreeLAN.LOGGER.info("[PantialClient] Paylasilan dunya varlik sunucusuna duyuruldu: {}", currentUrl);
            } else if (!hostingNow && isCurrentlyHosting) {
                JsonObject json = new JsonObject();
                json.addProperty("userCode", userCode);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + "/api/stop-host"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(json)))
                        .build();

                HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString());
                isCurrentlyHosting = false;
                reportedTunnelUrl = "";
                FreeLAN.LOGGER.info("[PantialClient] Dunya paylasimi sonlandirildi.");
            }
        } catch (Exception ignored) {}
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
