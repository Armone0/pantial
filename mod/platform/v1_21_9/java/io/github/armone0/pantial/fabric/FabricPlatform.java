package io.github.armone0.pantial.fabric;

import io.github.armone0.pantial.Platform;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;

import java.nio.file.Path;

public class FabricPlatform implements Platform {
    private static Minecraft mc() {
        return Minecraft.getInstance();
    }

    @Override
    public String minecraftVersion() {
        return FabricLoader.getInstance().getModContainer("minecraft")
                .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("?");
    }

    @Override
    public Path configDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public String playerName() {
        return mc().getUser() == null ? null : mc().getUser().getName();
    }

    @Override
    public String playerUuid() {
        return mc().getUser() == null || mc().getUser().getProfileId() == null ? "" : mc().getUser().getProfileId().toString();
    }

    @Override
    public boolean isModLoaded(String id) {
        return FabricLoader.getInstance().isModLoaded(id);
    }

    @Override
    public void runOnClientThread(Runnable task) {
        mc().execute(task);
    }

    @Override
    public void toast(String title, String message) {
        SystemToast.addOrUpdate(mc().getToastManager(), SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                Component.literal("§6Pantial §f" + title), Component.literal(message));
    }

    @Override
    public WorldState worldState() {
        IntegratedServer server = mc().getSingleplayerServer();
        if (server != null) return server.isPublished() ? WorldState.PUBLISHED : WorldState.SINGLEPLAYER;
        return mc().getConnection() != null ? WorldState.REMOTE : WorldState.NONE;
    }

    @Override
    public int publishedPort() {
        IntegratedServer server = mc().getSingleplayerServer();
        return server != null && server.isPublished() ? server.getPort() : -1;
    }

    @Override
    public boolean publishWorld(GameMode mode, boolean allowCheats, int port) {
        IntegratedServer server = mc().getSingleplayerServer();
        return server != null && server.publishServer(GameType.byId(mode.ordinal()), allowCheats, port);
    }

    @Override
    public void connect(String address, String displayName) {
        Minecraft mc = mc();
        if (mc.level != null) mc.disconnectWithSavingScreen();
        Screen parent = new JoinMultiplayerScreen(new TitleScreen());
        ConnectScreen.startConnecting(parent, mc, ServerAddress.parseString(address),
                new ServerData(displayName, address, ServerData.Type.OTHER), false, null);
    }

    @Override
    public String getClipboard() {
        return mc().keyboardHandler.getClipboard();
    }

    @Override
    public void setClipboard(String text) {
        mc().keyboardHandler.setClipboard(text);
    }

    @Override
    public void closeMenu() {
        Screen current = mc().screen;
        mc().setScreen(current instanceof PantialScreen ps ? ps.parent : null);
    }
}
