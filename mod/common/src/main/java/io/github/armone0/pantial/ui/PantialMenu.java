package io.github.armone0.pantial.ui;

import io.github.armone0.pantial.LanState;
import io.github.armone0.pantial.Pantial;
import io.github.armone0.pantial.PantialClient;
import io.github.armone0.pantial.PantialProfile;
import io.github.armone0.pantial.Platform;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pantial ana menusu. Minecraft'a dokunmaz: her surumdeki ince Screen sarmalayicisi
 * render/tiklama/klavye olaylarini buraya iletir ve {@link Gfx} uygular.
 */
public final class PantialMenu {
    private static final String[] TABS = {"Arkadaşlar", "Davetler", "Dünyam", "Ayarlar"};
    private static final String[] MODES = {"Hayatta Kalma", "Yaratıcı", "Macera", "İzleyici"};
    private static final int ROW_H = 30;

    /** Menu tekrar acildiginda son sekme hatirlansin. */
    private static int lastTab = 0;

    private final Ui ui = new Ui();
    private final Platform platform = Pantial.platform();
    private int width, height;
    private int tab = lastTab;
    private final double[] scroll = new double[TABS.length];

    private final TextField addField = new TextField(9, "Arkadaş kodu: PANT-XXXX",
            c -> c == '-' || (c < 128 && Character.isLetterOrDigit(c)), true);
    private final TextField serverField = new TextField(200, PantialProfile.DEFAULT_SERVER, c -> c > 32 && c < 127, false);
    private final TextField portField = new TextField(5, "25565", Character::isDigit, false);
    private TextField focused;

    private String flash = "";
    private int flashColor = Theme.MUTED;
    private long flashUntil;
    private long copiedUntil;
    private String confirmRemove;
    private final Map<String, Long> invitedAt = new HashMap<>();

    private int modeIndex = 0;
    private boolean cheats = false;

    public PantialMenu() {
        addField.onEnter = this::addFriend;
        serverField.onEnter = this::saveServer;
        serverField.set(PantialProfile.getInstance().serverUrl);
        portField.set(String.valueOf(freePort()));
        if (Boolean.getBoolean("pantial.autotest")) tab = Integer.getInteger("pantial.autotest.tab", 0);
    }

    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    // ------------------------------------------------------------------ olaylar

    public boolean mouseClicked(double x, double y, int button) {
        if (button != 0) return false;
        focus(null);
        String pendingRemove = confirmRemove;
        boolean handled = ui.click(x, y);
        if (pendingRemove != null && pendingRemove.equals(confirmRemove)) confirmRemove = null;
        return handled;
    }

    public boolean mouseScrolled(double delta) {
        scroll[tab] -= delta * 18;
        return true;
    }

    /** @return true ise olay tuketildi (ESC odak varken yalnizca odagi birakir). */
    public boolean keyPressed(int key, int mods) {
        if (focused != null) {
            if (key == 256) {
                focus(null);
                return true;
            }
            return focused.keyPressed(key, mods, ui, platform::getClipboard, platform::setClipboard);
        }
        return false;
    }

    public boolean charTyped(int codepoint) {
        return focused != null && focused.charTyped(codepoint);
    }

    private void focus(TextField f) {
        if (focused == f) return;
        if (focused != null) focused.focused = false;
        focused = f;
        if (f != null) f.focused = true;
        platform.textInputFocus(f != null);
    }

    /** Menu kapanirken metin girisini birak. */
    public void removed() {
        focus(null);
    }

    // ------------------------------------------------------------------ cizim

    public void render(Gfx g, int mouseX, int mouseY) {
        ui.begin(g, mouseX, mouseY);
        lastTab = tab;

        ui.gradient(0, 0, width, height, Theme.BG_TOP, Theme.BG_BOTTOM);

        int pw = Math.min(440, width - 16);
        int ph = Math.min(280, height - 12);
        int x1 = (width - pw) / 2, y1 = (height - ph) / 2, x2 = x1 + pw, y2 = y1 + ph;

        // panel + golge
        ui.round(x1 + 2, y1 + 3, x2 + 2, y2 + 3, 0x60000000);
        ui.round(x1, y1, x2, y2, Theme.PANEL_BORDER);
        ui.round(x1 + 1, y1 + 1, x2 - 1, y2 - 1, Theme.PANEL);

        header(x1, y1, x2);
        int tabsY = y1 + 34;
        tabs(x1, tabsY, x2);

        int cx1 = x1 + 10, cy1 = tabsY + 24, cx2 = x2 - 10, cy2 = y2 - 30;
        switch (tab) {
            case 0 -> friendsTab(cx1, cy1, cx2, cy2);
            case 1 -> invitesTab(cx1, cy1, cx2, cy2);
            case 2 -> worldTab(cx1, cy1, cx2, cy2);
            default -> settingsTab(cx1, cy1, cx2, cy2);
        }
        footer(x1, y2 - 26, x2, y2);
        ui.end();
    }

    private void header(int x1, int y1, int x2) {
        ui.round(x1 + 1, y1 + 1, x2 - 1, y1 + 30, Theme.HEADER);
        ui.g.fill(x1 + 1, y1 + 30, x2 - 1, y1 + 31, Theme.PANEL_BORDER);
        ui.g.fill(x1 + 1, y1 + 1, x1 + 4, y1 + 30, Theme.ACCENT);
        ui.g.scaledText("PANTIAL", x1 + 11, y1 + 7, Theme.ACCENT, 1.5f);
        ui.text("port açmadan birlikte oyna", x1 + 11, y1 + 20, Theme.MUTED);

        // kod rozeti: tiklayinca kopyalar
        PantialProfile profile = PantialProfile.getInstance();
        boolean copied = System.currentTimeMillis() < copiedUntil;
        String label = copied ? "Kopyalandı!" : profile.myUserCode;
        int w = Math.max(ui.g.width("Kopyalandı!"), ui.g.width(profile.myUserCode)) + 16;
        int bx2 = x2 - 8, bx1 = bx2 - w, by1 = y1 + 6, by2 = y1 + 25;
        boolean hov = ui.hover(bx1, by1, bx2, by2);
        ui.round(bx1, by1, bx2, by2, hov ? Theme.BUTTON_HOVER : Theme.BUTTON);
        ui.center(label, (bx1 + bx2) / 2, by1 + 6, copied ? Theme.GREEN : Theme.TEXT);
        ui.text("Kodun", bx1 - ui.g.width("Kodun") - 6, by1 + 6, Theme.MUTED);
        ui.hit(bx1, by1, bx2, by2, () -> {
            platform.setClipboard(profile.myUserCode);
            copiedUntil = System.currentTimeMillis() + 1500;
        });
    }

    private void tabs(int x1, int y, int x2) {
        int x = x1 + 10;
        for (int i = 0; i < TABS.length; i++) {
            String label = TABS[i];
            int badge = i == 1 ? PantialClient.invites.size() : 0;
            int w = ui.g.width(label) + 14 + (badge > 0 ? 14 : 0);
            boolean active = i == tab;
            boolean hov = ui.hover(x, y, x + w, y + 18);
            if (active) ui.round(x, y, x + w, y + 18, Theme.CARD);
            else if (hov) ui.round(x, y, x + w, y + 18, 0x80252C47);
            ui.text(label, x + 7, y + 5, active ? Theme.TEXT : (hov ? Theme.TEXT : Theme.MUTED));
            if (active) ui.g.fill(x + 4, y + 17, x + w - 4, y + 19, Theme.ACCENT);
            if (badge > 0) {
                int bx = x + w - 16;
                ui.round(bx, y + 4, bx + 11, y + 14, Theme.RED);
                ui.center(String.valueOf(Math.min(badge, 9)), bx + 6, y + 5, 0xFFFFFFFF);
            }
            final int idx = i;
            ui.hit(x, y, x + w, y + 18, () -> {
                tab = idx;
                confirmRemove = null;
            });
            x += w + 4;
        }
        ui.g.fill(x1 + 10, y + 19, x2 - 10, y + 20, Theme.PANEL_BORDER);
    }

    private void footer(int x1, int y1, int x2, int y2) {
        ui.g.fill(x1 + 1, y1, x2 - 1, y1 + 1, Theme.PANEL_BORDER);
        int ty = y1 + 9;
        int color;
        String status;
        if (!PantialClient.hasContacted()) {
            color = Theme.YELLOW;
            status = "Sunucuya bağlanıyor...";
        } else if (PantialClient.isReachable()) {
            color = Theme.GREEN;
            long online = PantialClient.onlineFriendCount();
            status = online > 0 ? online + " arkadaş çevrim içi" : "Bağlı";
        } else {
            color = Theme.RED;
            status = "Sunucuya ulaşılamıyor";
        }
        if (System.currentTimeMillis() < flashUntil) {
            color = flashColor;
            status = flash;
        }
        ui.dot(x1 + 12, ty + 1, color);
        ui.text(ui.ellipsize(status, x2 - x1 - 150), x1 + 22, ty, Theme.MUTED);

        ui.button(x2 - 120, y1 + 4, 54, 18, "Yenile", Ui.Style.GHOST, true, PantialClient::refreshNow);
        ui.button(x2 - 62, y1 + 4, 54, 18, "Kapat", Ui.Style.NORMAL, true, platform::closeMenu);
    }

    private void flash(String msg, int color) {
        flash = msg;
        flashColor = color;
        flashUntil = System.currentTimeMillis() + 3500;
    }

    // ------------------------------------------------------------------ Arkadaslar

    private void friendsTab(int x1, int y1, int x2, int y2) {
        addField.draw(ui, x1, y1, x2 - x1 - 66, 20, () -> focus(addField));
        ui.button(x2 - 62, y1, 62, 20, "+ Ekle", Ui.Style.PRIMARY, !addField.value.isEmpty(), this::addFriend);

        List<String> codes = PantialProfile.getInstance().getFriendCodes();
        int top = y1 + 26;
        if (codes.isEmpty()) {
            empty(x1, top, x2, y2, "Henüz arkadaşın yok",
                    "Kodunu (sağ üstte) paylaş ya da arkadaşının kodunu yukarı yaz.");
            return;
        }

        // Cevrim icler ustte, dunyasi acik olanlar en ustte
        codes.sort((a, b) -> Integer.compare(rank(status(b)), rank(status(a))));
        list(x1, top, x2, y2, codes.size(), (i, ry) -> friendRow(codes.get(i), x1, ry, x2));
    }

    private static int rank(PantialClient.FriendStatus s) {
        if (s == null) return 0;
        return s.joinable() ? 3 : s.online ? 2 : 1;
    }

    private static PantialClient.FriendStatus status(String code) {
        for (PantialClient.FriendStatus f : PantialClient.friends) {
            if (f.userCode != null && f.userCode.equalsIgnoreCase(code)) return f;
        }
        return null;
    }

    private void friendRow(String code, int x1, int y, int x2) {
        PantialClient.FriendStatus f = status(code);
        boolean known = f != null && f.username != null && !"Bilinmeyen Oyuncu".equals(f.username);
        String name = known ? f.username : code;

        boolean hov = ui.hover(x1, y, x2, y + ROW_H - 4);
        ui.round(x1, y, x2, y + ROW_H - 4, hov ? Theme.CARD_HOVER : Theme.CARD);
        avatar(name, x1 + 4, y + 3);

        int statusColor;
        String statusText;
        if (f == null) {
            statusColor = Theme.FAINT;
            statusText = "bekleniyor";
        } else if (!known) {
            statusColor = Theme.FAINT;
            statusText = "henüz hiç görülmedi";
        } else if (f.joinable()) {
            statusColor = Theme.GREEN;
            statusText = "dünyası açık";
        } else if (f.online) {
            statusColor = Theme.BLUE;
            statusText = "çevrim içi";
        } else {
            statusColor = Theme.FAINT;
            statusText = "çevrim dışı";
        }

        // sag taraftaki dugmeler
        int bx = x2 - 4;
        boolean confirming = code.equals(confirmRemove);
        int rw = confirming ? 36 : 18;
        bx -= rw;
        ui.button(bx, y + 4, rw, 18, confirming ? "Sil?" : "x", confirming ? Ui.Style.DANGER : Ui.Style.GHOST, true, () -> {
            if (code.equals(confirmRemove)) {
                PantialProfile.getInstance().removeFriend(code);
                PantialClient.friends.removeIf(s -> code.equalsIgnoreCase(s.userCode));
                confirmRemove = null;
                flash(name + " listeden çıkarıldı", Theme.MUTED);
            } else {
                confirmRemove = code;
            }
        });
        if (f != null && f.joinable()) {
            bx -= 50;
            ui.button(bx, y + 4, 46, 18, "Katıl", Ui.Style.PRIMARY, true,
                    () -> platform.connect(f.tunnelUrl, name + " - Pantial"));
        }
        boolean canInvite = f != null && f.online && !f.joinable() && !LanState.tunnelUrl.isEmpty();
        if (canInvite) {
            Long at = invitedAt.get(code);
            boolean sent = at != null && System.currentTimeMillis() - at < 5000;
            bx -= 62;
            ui.button(bx, y + 4, 58, 18, sent ? "Gönderildi" : "Davet et", Ui.Style.NORMAL, !sent, () -> {
                invitedAt.put(code, System.currentTimeMillis());
                PantialClient.sendInvite(code, null);
            });
        }

        int tx = x1 + 30, tw = bx - tx - 6;
        ui.text(ui.ellipsize(name, tw), tx, y + 4, Theme.TEXT);
        ui.dot(tx, y + 15, statusColor);
        String sub = statusText + (known ? "  " + code : "");
        ui.text(ui.ellipsize(sub, tw - 8), tx + 8, y + 14, Theme.MUTED);
    }

    private void avatar(String name, int x, int y) {
        int c = Theme.avatarColor(name);
        ui.round(x, y, x + 20, y + 20, c);
        ui.g.fill(x + 1, y + 14, x + 19, y + 19, 0x30000000);
        String initial = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(java.util.Locale.ROOT);
        ui.center(initial, x + 10, y + 6, 0xFFFFFFFF);
    }

    private void addFriend() {
        String code = PantialProfile.normalize(addField.value);
        if (code.length() == 4) code = "PANT-" + code;
        switch (PantialProfile.getInstance().addFriend(code)) {
            case ADDED -> {
                addField.set("");
                flash(code + " eklendi", Theme.GREEN);
                PantialClient.refreshNow();
            }
            case INVALID -> flash("Kod PANT-XXXX biçiminde olmalı", Theme.RED);
            case SELF -> flash("Bu senin kendi kodun", Theme.YELLOW);
            case DUPLICATE -> flash(code + " zaten listende", Theme.YELLOW);
        }
    }

    // ------------------------------------------------------------------ Davetler

    private void invitesTab(int x1, int y1, int x2, int y2) {
        List<PantialClient.InviteStatus> invites = List.copyOf(PantialClient.invites);
        if (invites.isEmpty()) {
            empty(x1, y1, x2, y2, "Bekleyen davet yok",
                    "Bir arkadaşın seni dünyasına çağırınca burada görünecek.");
            return;
        }
        list(x1, y1, x2, y2, invites.size(), (i, y) -> {
            PantialClient.InviteStatus inv = invites.get(i);
            boolean hov = ui.hover(x1, y, x2, y + ROW_H - 4);
            ui.round(x1, y, x2, y + ROW_H - 4, hov ? Theme.CARD_HOVER : Theme.CARD);
            avatar(inv.senderName, x1 + 4, y + 3);
            int bx = x2 - 4 - 50;
            ui.button(bx, y + 4, 50, 18, "Reddet", Ui.Style.GHOST, true, () -> PantialClient.dismissInvite(inv));
            bx -= 50;
            ui.button(bx, y + 4, 46, 18, "Katıl", Ui.Style.PRIMARY, true, () -> {
                PantialClient.dismissInvite(inv);
                platform.connect(inv.tunnelUrl, inv.senderName + " - Pantial");
            });
            int tw = bx - x1 - 36;
            ui.text(ui.ellipsize(inv.senderName + " seni dünyasına çağırıyor", tw), x1 + 30, y + 4, Theme.TEXT);
            ui.text(ago(inv.timestamp), x1 + 30, y + 14, Theme.MUTED);
        });
    }

    private static String ago(long ts) {
        long s = Math.max(0, (System.currentTimeMillis() - ts) / 1000);
        if (s < 60) return "az önce";
        return (s / 60) + " dk önce";
    }

    // ------------------------------------------------------------------ Dunyam

    private void worldTab(int x1, int y1, int x2, int y2) {
        Platform.WorldState state = platform.worldState();
        if (state == Platform.WorldState.NONE) {
            empty(x1, y1, x2, y2, "Bir dünyada değilsin",
                    "Tek oyunculu bir dünyaya gir, sonra bu menüyü duraklatma ekranından aç.");
            return;
        }
        if (state == Platform.WorldState.REMOTE) {
            empty(x1, y1, x2, y2, "Başka bir sunucudasın",
                    "Paylaşım yalnızca kendi tek oyunculu dünyanda çalışır.");
            return;
        }
        boolean e4mc = platform.isModLoaded("e4mc");

        if (state == Platform.WorldState.PUBLISHED) {
            int y = y1;
            ui.round(x1, y, x2, y + 44, Theme.CARD);
            ui.g.fill(x1, y + 1, x1 + 2, y + 43, Theme.GREEN);
            ui.text("Dünyan paylaşımda", x1 + 10, y + 6, Theme.TEXT);
            ui.text("Yerel port " + platform.publishedPort()
                    + (LanState.onlineMode ? "  ·  kimlik doğrulama açık" : "  ·  kimlik doğrulama KAPALI"), x1 + 10, y + 18,
                    LanState.onlineMode ? Theme.MUTED : Theme.YELLOW);
            String url = LanState.tunnelUrl;
            String addr = !url.isEmpty() ? url : (e4mc ? "tünel adresi bekleniyor..." : "e4mc yüklü değil: yalnızca aynı ağdan girilebilir");
            ui.text(ui.ellipsize(addr, x2 - x1 - 90), x1 + 10, y + 30, url.isEmpty() ? Theme.FAINT : Theme.ACCENT);
            ui.button(x2 - 72, y + 13, 66, 18, "Kopyala", Ui.Style.NORMAL, !url.isEmpty(), () -> {
                platform.setClipboard(url);
                flash("Adres panoya kopyalandı", Theme.GREEN);
            });
            y += 52;

            ui.text("Arkadaşlara duyur", x1 + 2, y + 2, Theme.TEXT);
            ui.text(PantialClient.isAnnounced() ? "arkadaş listende \"dünyası açık\" görünüyorsun" : "duyurulmuyor",
                    x1 + 2, y + 13, Theme.MUTED);
            ui.toggle(x2 - 24, y + 4, LanState.announce, () -> LanState.announce = !LanState.announce);
            y += 30;

            List<PantialClient.FriendStatus> online = PantialClient.friends.stream().filter(f -> f.online && !f.joinable()).toList();
            ui.button(x1, y, x2 - x1, 20, online.isEmpty() ? "Davet edilecek çevrim içi arkadaş yok"
                            : "Çevrim içi " + online.size() + " arkadaşı davet et", Ui.Style.PRIMARY,
                    !online.isEmpty() && !url.isEmpty(), () -> {
                        online.forEach(f -> PantialClient.sendInvite(f.userCode, null));
                        flash(online.size() + " davet gönderildi", Theme.GREEN);
                    });
            return;
        }

        // SINGLEPLAYER: paylasim ayarlari
        int y = y1;
        int lw = x2 - x1;
        if (platform.supportsGameMode()) {
            row(x1, y, "Oyun modu", null);
            ui.button(x2 - 110, y + 1, 110, 18, MODES[modeIndex], Ui.Style.NORMAL, true, () -> modeIndex = (modeIndex + 1) % MODES.length);
            y += 24;
        }
        row(x1, y, "Hileler", null);
        ui.toggle(x2 - 24, y + 4, cheats, () -> cheats = !cheats);
        y += 24;
        row(x1, y, "Kimlik doğrulama", LanState.onlineMode ? null : "Kapalıyken adresi bilen herkes istediği isimle girer");
        ui.toggle(x2 - 24, y + 4, LanState.onlineMode, () -> LanState.onlineMode = !LanState.onlineMode);
        y += 24;
        row(x1, y, "Arkadaşlara duyur", null);
        ui.toggle(x2 - 24, y + 4, LanState.announce, () -> LanState.announce = !LanState.announce);
        y += 24;
        row(x1, y, "Port", null);
        portField.draw(ui, x2 - 60, y + 1, 60, 18, () -> focus(portField));
        y += 26;

        if (!e4mc) {
            ui.text(ui.ellipsize("e4mc kurulu değil: arkadaşların yalnızca aynı ağdan bağlanabilir.", lw), x1, y, Theme.YELLOW);
            y += 12;
        }
        ui.button(x1, Math.min(y, y2 - 22), lw, 22, "Dünyamı Paylaş", Ui.Style.PRIMARY, true, this::publish);
    }

    private void row(int x1, int y, String label, String warning) {
        ui.text(label, x1 + 2, y + (warning == null ? 6 : 1), Theme.TEXT);
        if (warning != null) ui.text(ui.ellipsize(warning, 260), x1 + 2, y + 11, Theme.YELLOW);
    }

    private void publish() {
        int port;
        try {
            port = Integer.parseInt(portField.value.trim());
        } catch (NumberFormatException e) {
            port = -1;
        }
        if (port < 1024 || port > 65535) {
            flash("Port 1024-65535 arasında olmalı", Theme.RED);
            return;
        }
        LanState.tunnelUrl = "";
        boolean ok = platform.publishWorld(Platform.GameMode.values()[modeIndex], cheats, port);
        if (ok) flash("Dünya " + port + " portunda açıldı", Theme.GREEN);
        else flash("Dünya açılamadı (port kullanımda olabilir)", Theme.RED);
    }

    private static int freePort() {
        try (java.net.ServerSocket s = new java.net.ServerSocket(0)) {
            return s.getLocalPort();
        } catch (Exception e) {
            return 25565;
        }
    }

    // ------------------------------------------------------------------ Ayarlar

    private void settingsTab(int x1, int y1, int x2, int y2) {
        PantialProfile profile = PantialProfile.getInstance();
        int y = y1;
        ui.text("Pantial sunucusu", x1 + 2, y, Theme.TEXT);
        y += 11;
        serverField.draw(ui, x1, y, x2 - x1 - 70, 20, () -> focus(serverField));
        boolean changed = !PantialProfile.cleanUrl(serverField.value).equals(profile.serverUrl);
        ui.button(x2 - 66, y, 66, 20, "Kaydet", Ui.Style.PRIMARY, changed, this::saveServer);
        y += 30;

        row(x1, y, "Bildirimler", null);
        ui.toggle(x2 - 24, y + 4, profile.notifications, () -> {
            profile.notifications = !profile.notifications;
            profile.save();
        });
        y += 24;
        row(x1, y, "Ana menüde Pantial düğmesi", null);
        ui.toggle(x2 - 24, y + 4, profile.titleButton, () -> {
            profile.titleButton = !profile.titleButton;
            profile.save();
        });
        y += 30;

        ui.g.fill(x1, y, x2, y + 1, Theme.PANEL_BORDER);
        y += 8;
        ui.text("Minecraft " + platform.minecraftVersion(), x1 + 2, y, Theme.FAINT);
        String hint = "Ayar dosyası: config/pantial.json";
        ui.text(hint, x2 - ui.g.width(hint), y, Theme.FAINT);
    }

    private void saveServer() {
        PantialProfile.getInstance().setServerUrl(serverField.value);
        serverField.set(PantialProfile.getInstance().serverUrl);
        PantialClient.serverChanged();
        flash("Sunucu kaydedildi, bağlanılıyor...", Theme.GREEN);
    }

    // ------------------------------------------------------------------ ortak

    private interface RowPainter {
        void paint(int index, int y);
    }

    /** Kaydirilabilir satir listesi: kirpma destekli surumlerde kismi satirlar da cizilir. */
    private void list(int x1, int y1, int x2, int y2, int count, RowPainter painter) {
        int total = count * ROW_H - 4;
        int view = y2 - y1;
        double max = Math.max(0, total - view);
        scroll[tab] = Math.max(0, Math.min(max, scroll[tab]));
        int off = (int) scroll[tab];
        boolean clipped = ui.g.clip(x1, y1, x2 + 1, y2);
        ui.clipArea(x1, y1, x2 + 1, y2);
        int rx2 = max > 0 ? x2 - 6 : x2;
        for (int i = 0; i < count; i++) {
            int y = y1 + i * ROW_H - off;
            if (y + ROW_H - 4 <= y1 || y >= y2) continue;
            if (!clipped && (y < y1 || y + ROW_H - 4 > y2)) continue;
            painter.paint(i, y);
        }
        ui.noClip();
        if (clipped) ui.g.unclip();
        if (max > 0) {
            int barH = Math.max(16, (int) (view * (view / (double) total)));
            int barY = y1 + (int) ((view - barH) * (off / max));
            ui.g.fill(x2 - 3, y1, x2, y2, 0x40000000);
            ui.round(x2 - 3, barY, x2, barY + barH, Theme.FIELD_BORDER);
        }
    }

    private void empty(int x1, int y1, int x2, int y2, String title, String sub) {
        int cy = (y1 + y2) / 2 - 10;
        int cx = (x1 + x2) / 2;
        ui.center(title, cx, cy, Theme.TEXT);
        ui.center(ui.ellipsize(sub, x2 - x1 - 10), cx, cy + 13, Theme.MUTED);
    }

    /** Yerelde (render thread) bir kare cizmeden tiklamalari gormez; otomatik test icin. */
    public void selectTab(int index) {
        tab = Math.max(0, Math.min(TABS.length - 1, index));
    }
}
