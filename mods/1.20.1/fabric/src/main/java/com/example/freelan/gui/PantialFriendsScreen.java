package com.example.freelan.gui;

import com.example.freelan.PantialClient;
import com.example.freelan.PantialProfile;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class PantialFriendsScreen extends Screen {
    private final Screen lastScreen;

    public PantialFriendsScreen(Screen lastScreen) {
        super(Component.literal("Pantial - Arkadaş Listesi & Varlık Ağı"));
        this.lastScreen = lastScreen;
    }

    @Override
    protected void init() {
        super.init();
        PantialProfile profile = PantialProfile.getInstance();

        int centerX = this.width / 2;

        // Kodu panoya kopyala butonu
        this.addRenderableWidget(Button.builder(
                Component.literal("Kodumu Kopyala (" + profile.myUserCode + ")"),
                button -> {
                    if (this.minecraft != null) {
                        this.minecraft.keyboardHandler.setClipboard(profile.myUserCode);
                        PantialClient.showNotification("Pantial", "Arkadaş kodunuz panoya kopyalandı!");
                    }
                }
        ).bounds(centerX - 120, 40, 240, 20).build());

        // Arkadaş listesi dinamik butonları
        int startY = 70;
        int index = 0;
        for (PantialClient.FriendStatus friend : PantialClient.friends) {
            final PantialClient.FriendStatus targetFriend = friend;
            String statusText = targetFriend.online ? (targetFriend.isHosting ? "§a[DÜNYA AÇIK]" : "§e[Çevrim İçi]") : "§7[Çevrim Dışı]";
            String btnText = targetFriend.username + " (" + targetFriend.userCode + ") " + statusText;

            Button friendBtn = Button.builder(
                    Component.literal(btnText),
                    button -> {
                        if (targetFriend.isHosting && targetFriend.tunnelUrl != null && !targetFriend.tunnelUrl.isEmpty()) {
                            connectToWorld(targetFriend.tunnelUrl, targetFriend.username + " Dünyası");
                        }
                    }
            ).bounds(centerX - 150, startY + (index * 24), 300, 20).build();

            friendBtn.active = targetFriend.isHosting && targetFriend.tunnelUrl != null && !targetFriend.tunnelUrl.isEmpty();
            this.addRenderableWidget(friendBtn);

            index++;
            if (index >= 6) break; // İlk 6 arkadaşı göster
        }

        // Geri Dön butonu
        this.addRenderableWidget(Button.builder(
                CommonComponents.GUI_BACK,
                button -> {
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(this.lastScreen);
                    }
                }
        ).bounds(centerX - 100, this.height - 30, 200, 20).build());
    }

    private void connectToWorld(String tunnelUrl, String serverName) {
        if (this.minecraft == null) return;
        ServerAddress serverAddress = ServerAddress.parseString(tunnelUrl);
        ServerData serverData = new ServerData(serverName, tunnelUrl, false);
        ConnectScreen.startConnecting(this, this.minecraft, serverAddress, serverData, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);

        PantialProfile profile = PantialProfile.getInstance();
        guiGraphics.drawCenteredString(this.font, "§7Sunucu: §f" + profile.serverUrl, this.width / 2, 28, 0xAAAAAA);

        if (PantialClient.friends.isEmpty()) {
            guiGraphics.drawCenteredString(this.font, "§7Henüz ekli arkadaş bulunmuyor. pantial_profile.json içine kod ekleyin.", this.width / 2, 100, 0x888888);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}
