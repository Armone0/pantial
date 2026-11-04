package com.example.freelan.gui;

import com.example.freelan.PantialClient;
import com.example.freelan.PantialProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Minecraft 1.19.2 & 1.19.4 Uyumlu Pantial Arkadas Listesi Ekrani.
 * 1.20 GuiGraphics yerine 1.19.x PoseStack ve standart Button yapıcısı kullanır.
 */
public class PantialFriendsScreen119 extends Screen {

    private final Screen parentScreen;

    public PantialFriendsScreen119(Screen parentScreen) {
        super(Component.literal("Pantial Arkadaş Listesi (1.19.x)"));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;

        // 1.19.2 Button Yapıcısı: new Button(x, y, w, h, Component, OnPress)
        this.addRenderableWidget(new Button(
                centerX - 100,
                this.height - 36,
                200,
                20,
                Component.literal("Kapat"),
                button -> Minecraft.getInstance().setScreen(this.parentScreen)
        ));

        // Arkadas ekle butonu
        this.addRenderableWidget(new Button(
                centerX - 100,
                this.height - 62,
                96,
                20,
                Component.literal("Arkadaş Ekle"),
                button -> {
                    // Modal veya kod giris ekrani
                }
        ));

        // Yenile butonu
        this.addRenderableWidget(new Button(
                centerX + 4,
                this.height - 62,
                96,
                20,
                Component.literal("Yenile"),
                button -> PantialClient.getInstance().refreshFriends()
        ));
    }

    @Override
    public void render(PoseStack matrices, int mouseX, int mouseY, float delta) {
        // 1.19.2 Arka plan ve başlık çizimi (PoseStack)
        this.renderBackground(matrices);
        drawCenteredString(matrices, this.font, this.title, this.width / 2, 16, 0xFFFFFF);

        PantialProfile profile = PantialClient.getInstance().getCurrentProfile();
        String myCode = profile != null ? profile.getFriendCode() : "Bağlanıyor...";
        drawString(matrices, this.font, "Senin Kodun: §6" + myCode, 16, 16, 0xAAAAAA);

        List<PantialProfile.FriendEntry> friends = PantialClient.getInstance().getFriends();
        int startY = 46;

        if (friends.isEmpty()) {
            drawCenteredString(matrices, this.font, "§7Henüz arkadaş eklemedin. Arkadaş kodunu paylaş!", this.width / 2, this.height / 2 - 10, 0x888888);
        } else {
            for (int i = 0; i < friends.size(); i++) {
                PantialProfile.FriendEntry friend = friends.get(i);
                int itemY = startY + (i * 26);
                String statusColor = friend.isOnline() ? "§a● " : "§7○ ";
                String hostStatus = friend.hasActiveWorld() ? " §e[Dünya Açık - Katıl]" : "";

                drawString(matrices, this.font, statusColor + friend.getUsername() + " §8(" + friend.getFriendCode() + ")" + hostStatus, 24, itemY, 0xFFFFFF);
            }
        }

        super.render(matrices, mouseX, mouseY, delta);
    }
}
