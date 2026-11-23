package com.example.freelan.gui;

import com.example.freelan.LanConfig;
import com.example.freelan.PantialClient;
import com.example.freelan.PantialProfile;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Arkadas listesi: kod ekleme/silme, acik dunyalara katilma, davet gonderme ve gelen davetler. */
public class PantialFriendsScreen extends Screen {
    private static final int MAX_FRIEND_ROWS = 6;
    private static final int MAX_INVITE_ROWS = 2;

    private final Screen lastScreen;
    private EditBox codeBox;
    private String pendingCode = "";
    private int seenRevision = -1;

    public PantialFriendsScreen(Screen lastScreen) {
        super(Component.literal("Pantial - Arkadaş Listesi"));
        this.lastScreen = lastScreen;
    }

    @Override
    protected void init() {
        PantialProfile profile = PantialProfile.getInstance();
        int centerX = this.width / 2;
        seenRevision = PantialClient.getRevision();

        this.addRenderableWidget(Button.builder(
                Component.literal("Kodumu Kopyala (" + profile.myUserCode + ")"),
                button -> {
                    if (this.minecraft != null) {
                        this.minecraft.keyboardHandler.setClipboard(profile.myUserCode);
                        PantialClient.showNotification("Pantial", "Arkadaş kodun panoya kopyalandı.");
                    }
                }
        ).bounds(centerX - 150, 36, 300, 20).build());

        // Arkadas ekleme
        this.codeBox = new EditBox(this.font, centerX - 150, 62, 196, 20, Component.literal("Arkadaş kodu"));
        this.codeBox.setMaxLength(9);
        this.codeBox.setHint(Component.literal("PANT-XXXX"));
        this.codeBox.setValue(pendingCode);
        this.codeBox.setResponder(text -> pendingCode = text);
        this.addRenderableWidget(this.codeBox);
        this.addRenderableWidget(Button.builder(Component.literal("Ekle"), button -> addFriend())
                .bounds(centerX + 50, 62, 100, 20).build());

        int y = 90;

        // Gelen davetler
        int shownInvites = 0;
        for (PantialClient.InviteStatus invite : PantialClient.invites) {
            if (shownInvites++ >= MAX_INVITE_ROWS) break;
            this.addRenderableWidget(Button.builder(
                    Component.literal("§eDavet: §f" + invite.senderName + " §7- Katıl"),
                    button -> {
                        PantialClient.dismissInvite(invite);
                        connectToWorld(invite.tunnelUrl, invite.senderName + " Dünyası");
                    }
            ).bounds(centerX - 150, y, 244, 20).build());
            this.addRenderableWidget(Button.builder(Component.literal("Reddet"), button -> {
                PantialClient.dismissInvite(invite);
                this.rebuildWidgets();
            }).bounds(centerX + 98, y, 52, 20).build());
            y += 24;
        }

        // Arkadaslar
        boolean amHosting = !LanConfig.tunnelUrl.isEmpty();
        List<PantialClient.FriendStatus> friends = PantialClient.friends;
        int shown = 0;
        for (PantialClient.FriendStatus friend : friends) {
            if (shown++ >= MAX_FRIEND_ROWS) break;
            boolean joinable = friend.online && friend.isHosting && friend.tunnelUrl != null && !friend.tunnelUrl.isEmpty();
            String statusText = friend.online ? (joinable ? "§a[DÜNYA AÇIK]" : "§e[Çevrim İçi]") : "§7[Çevrim Dışı]";

            Button row = Button.builder(
                    Component.literal(friend.username + " §8(" + friend.userCode + ") " + statusText),
                    button -> connectToWorld(friend.tunnelUrl, friend.username + " Dünyası")
            ).bounds(centerX - 150, y, 196, 20).build();
            row.active = joinable;
            this.addRenderableWidget(row);

            Button invite = Button.builder(Component.literal("Davet"), button -> PantialClient.sendInvite(friend.userCode))
                    .bounds(centerX + 50, y, 74, 20).build();
            invite.active = amHosting && friend.online;
            this.addRenderableWidget(invite);

            this.addRenderableWidget(Button.builder(Component.literal("§cX"), button -> {
                PantialProfile.getInstance().removeFriend(friend.userCode);
                PantialClient.friends.remove(friend);
                this.rebuildWidgets();
            }).bounds(centerX + 128, y, 22, 20).build());
            y += 24;
        }

        this.addRenderableWidget(Button.builder(Component.literal("Yenile"), button -> PantialClient.refreshNow())
                .bounds(centerX - 150, this.height - 28, 96, 20).build());
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, button -> this.onClose())
                .bounds(centerX - 50, this.height - 28, 200, 20).build());
    }

    private void addFriend() {
        String code = PantialProfile.normalize(pendingCode);
        if (!PantialProfile.isValidCode(code)) {
            PantialClient.showNotification("Geçersiz Kod", "Kod PANT-XXXX biçiminde olmalı.");
            return;
        }
        if (PantialProfile.getInstance().addFriend(code)) {
            pendingCode = "";
            PantialClient.refreshNow();
            this.rebuildWidgets();
        } else {
            PantialClient.showNotification("Eklenemedi", code + " zaten listende ya da senin kodun.");
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.codeBox != null) this.codeBox.tick();
        if (PantialClient.getRevision() != seenRevision) {
            this.rebuildWidgets();
        }
    }

    private void connectToWorld(String tunnelUrl, String serverName) {
        if (this.minecraft == null || tunnelUrl == null || tunnelUrl.isEmpty()) return;
        ServerAddress serverAddress = ServerAddress.parseString(tunnelUrl);
        ServerData serverData = new ServerData(serverName, tunnelUrl, false);
        ConnectScreen.startConnecting(this, this.minecraft, serverAddress, serverData, false);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.lastScreen);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFF);

        PantialProfile profile = PantialProfile.getInstance();
        String serverLine = PantialClient.isServerReachable()
                ? "§7Sunucu: §f" + profile.serverUrl
                : "§cSunucuya ulaşılamıyor: §f" + profile.serverUrl;
        guiGraphics.drawCenteredString(this.font, serverLine, this.width / 2, 22, 0xAAAAAA);

        if (PantialClient.friends.isEmpty() && PantialClient.invites.isEmpty()) {
            guiGraphics.drawCenteredString(this.font, "§7Henüz arkadaş yok. Kodunu paylaş ya da yukarıdan bir kod ekle.",
                    this.width / 2, 100, 0x888888);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}
