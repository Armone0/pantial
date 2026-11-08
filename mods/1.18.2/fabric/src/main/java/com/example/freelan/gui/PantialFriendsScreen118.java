package com.example.freelan.gui;

import com.example.freelan.PantialClient;
import com.example.freelan.PantialProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;

import java.util.List;

/**
 * Minecraft 1.18.2 (Caves & Cliffs Part II) Uyumlu Pantial Arkadas Listesi Ekrani.
 * 1.18.2'de TextComponent ve standart new Button(...) yapisi kullanilir.
 * Sunucuya erisilemediginde kullaniciyi uyaran dinamik bildirim cubugu icerir.
 */
public class PantialFriendsScreen118 extends Screen {

    private final Screen parentScreen;
    private boolean serverConnected = true;
    private String errorMessage = null;

    public PantialFriendsScreen118(Screen parentScreen) {
        super(new TextComponent("Pantial Arkadaş Listesi (1.18.2)"));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;

        // 1.18.2 Kapat Butonu
        this.addRenderableWidget(new Button(
                centerX - 100,
                this.height - 36,
                200,
                20,
                new TextComponent("Kapat"),
                button -> Minecraft.getInstance().setScreen(this.parentScreen)
        ));

        // Arkadas Ekle Butonu
        this.addRenderableWidget(new Button(
                centerX - 100,
                this.height - 62,
                96,
                20,
                new TextComponent("Arkadaş Ekle"),
                button -> {
                    // Arkadas kodu ekleme modali
                }
        ));

        // Yenile Butonu
        this.addRenderableWidget(new Button(
                centerX + 4,
                this.height - 62,
                96,
                20,
                new TextComponent("Yenile"),
                button -> refreshFriends()
        ));

        refreshFriends();
    }

    private void refreshFriends() {
        try {
            // Pantial varlik sunucusu saglik ve arkadas durumu sorgusu
            PantialClient.refreshFriendsStatus();
            this.serverConnected = true;
            this.errorMessage = null;
        } catch (Exception e) {
            this.serverConnected = false;
            this.errorMessage = "Pantial sunucusuna bağlanılamadı. Yerel ağ modu çevrim dışı.";
        }
    }

    @Override
    public void render(PoseStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);

        int centerX = this.width / 2;

        // Baslik cizimi
        drawCenteredString(matrices, this.font, this.title, centerX, 16, 0xFFFFAA00);

        // Kullanici Kodu
        String myCode = PantialProfile.getUserCode();
        if (myCode != null && !myCode.isEmpty()) {
            drawCenteredString(matrices, this.font, "Kodunuz: §a" + myCode + " §7(Tıkla & Kopyala)", centerX, 32, 0xFFFFFFFF);
        }

        // Sunucu baglanti hatasi bildirimi (Banner)
        if (!this.serverConnected && this.errorMessage != null) {
            fill(matrices, centerX - 140, 46, centerX + 140, 64, 0x88CC2222);
            drawCenteredString(matrices, this.font, "§c⚠ " + this.errorMessage, centerX, 51, 0xFFFFFFFF);
        }

        // Arkadas Listesi
        List<PantialProfile.FriendEntry> friends = PantialProfile.getFriends();
        if (friends == null || friends.isEmpty()) {
            int yPos = !this.serverConnected ? 76 : 60;
            drawCenteredString(matrices, this.font, "§7Henüz ekli bir arkadaşınız bulunmuyor.", centerX, yPos, 0xFFAAAAAA);
            drawCenteredString(matrices, this.font, "§8PANT-XXXX formatındaki kod ile arkadaşınızı ekleyin.", centerX, yPos + 14, 0xFF888888);
        } else {
            int y = !this.serverConnected ? 72 : 54;
            for (PantialProfile.FriendEntry friend : friends) {
                if (y > this.height - 76) break;

                String statusColor = friend.isOnline() ? "§a●" : "§7○";
                String hostText = friend.hasActiveHost() ? " §e[Dünyaya Katıl]" : "";
                drawString(matrices, this.font, statusColor + " §f" + friend.getUsername() + " §8(" + friend.getUserCode() + ")" + hostText, centerX - 96, y, 0xFFFFFFFF);

                y += 18;
            }
        }

        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
