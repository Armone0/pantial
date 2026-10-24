package com.example.freelan.gui;

import com.example.freelan.LanConfig;
import com.example.freelan.PantialClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.HttpUtil;
import net.minecraft.world.level.GameType;

public class FreeLanScreen extends Screen {
    private final Screen lastScreen;

    private GameType gameType = GameType.SURVIVAL;
    private boolean allowCommands = false;
    private boolean onlineMode = false; // Varsayılan: Çevrim dışı / cracked dostu
    private boolean enableTunnel = true;
    private int port = 25565;
    private EditBox portEditBox;

    public FreeLanScreen(Screen lastScreen) {
        super(Component.literal("FreeLAN - Yerel Ağı Dünyaya Aç"));
        this.lastScreen = lastScreen;
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;

        try {
            this.port = HttpUtil.getAvailablePort();
        } catch (Throwable ignored) {
            this.port = 25565;
        }

        // 1. Oyun Modu Seçici
        this.addRenderableWidget(CycleButton.builder(GameType::getShortDisplayName)
                .withValues(GameType.SURVIVAL, GameType.CREATIVE, GameType.ADVENTURE, GameType.SPECTATOR)
                .withInitialValue(this.gameType)
                .create(centerX - 155, 50, 150, 20, Component.literal("Oyun Modu"), (button, value) -> this.gameType = value));

        // 2. Hilelere İzin Ver
        this.addRenderableWidget(CycleButton.onOffBuilder(this.allowCommands)
                .create(centerX + 5, 50, 150, 20, Component.literal("Hileler"), (button, value) -> this.allowCommands = value));

        // 3. Çevrim İçi Kimlik Doğrulama (Online-Mode / Korsan Desteği)
        this.addRenderableWidget(CycleButton.onOffBuilder(this.onlineMode)
                .create(centerX - 155, 80, 150, 20, Component.literal("Orijinal Hesap Şartı"), (button, value) -> this.onlineMode = value));

        // 4. e4mc P2P Tünel Modu
        this.addRenderableWidget(CycleButton.onOffBuilder(this.enableTunnel)
                .create(centerX + 5, 80, 150, 20, Component.literal("e4mc Tünel"), (button, value) -> this.enableTunnel = value));

        // 5. Port Giriş Kutusu
        this.portEditBox = new EditBox(this.font, centerX - 155, 115, 310, 20, Component.literal("Port"));
        this.portEditBox.setValue(String.valueOf(this.port));
        this.portEditBox.setResponder(text -> {
            try {
                this.port = Integer.parseInt(text.trim());
            } catch (NumberFormatException ignored) {}
        });
        this.addRenderableWidget(this.portEditBox);

        // 6. Dünyayı Paylaşıma Aç Butonu
        this.addRenderableWidget(Button.builder(
                Component.literal("§aYerel Ağı Başlat"),
                button -> startHosting()
        ).bounds(centerX - 155, 150, 150, 20).build());

        // 7. İptal Butonu
        this.addRenderableWidget(Button.builder(
                CommonComponents.GUI_CANCEL,
                button -> {
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(this.lastScreen);
                    }
                }
        ).bounds(centerX + 5, 150, 150, 20).build());
    }

    private void startHosting() {
        if (this.minecraft == null) return;
        IntegratedServer server = this.minecraft.getSingleplayerServer();

        if (server != null) {
            LanConfig.onlineMode = this.onlineMode;
            LanConfig.enableTunnel = this.enableTunnel;

            boolean published = server.publishServer(this.gameType, this.allowCommands, this.port);
            if (published) {
                PantialClient.showNotification("FreeLAN", "Yerel ağ dünyası " + this.port + " portunda başlatıldı.");
                this.minecraft.setScreen(null);
            } else {
                PantialClient.showNotification("Hata", "Yerel ağ dünyası başlatılamadı!");
            }
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font, "§7Arkadaşlarınız port açmadan doğrudan bağlanabilir.", this.width / 2, 30, 0xAAAAAA);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}
