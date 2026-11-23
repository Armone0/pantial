package io.github.armone0.pantial.fabric;

import io.github.armone0.pantial.Platform;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
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
        SystemToast.addOrUpdate(mc().getToasts(), SystemToast.SystemToastIds.PERIODIC_NOTIFICATION,
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
        if (mc.level != null) {
            mc.level.disconnect();
            mc.clearLevel(new GenericDirtMessageScreen(Component.translatable("menu.savingLevel")));
        }
        Screen parent = new JoinMultiplayerScreen(new TitleScreen());
        ConnectScreen.startConnecting(parent, mc, ServerAddress.parseString(address),
                new ServerData(displayName, address, false), false);
    }

    @Override
    public java.util.List<WorldPlayer> worldPlayers() {
        IntegratedServer server = mc().getSingleplayerServer();
        java.util.List<WorldPlayer> out = new java.util.ArrayList<>();
        if (server == null) return out;
        try {
            for (net.minecraft.server.level.ServerPlayer p : new java.util.ArrayList<>(server.getPlayerList().getPlayers())) {
                var prof = p.getGameProfile();
                boolean host = server.isSingleplayerOwner(prof);
                out.add(new WorldPlayer(prof.getName(), String.valueOf(prof.getId()), server.getPlayerList().isOp(prof), host));
            }
        } catch (RuntimeException ignored) {
            // liste sunucu thread'inde degisirken okunduysa bir sonraki karede tekrar denenir
        }
        return out;
    }

    private static net.minecraft.server.level.ServerPlayer findPlayer(IntegratedServer server, String uuid) {
        for (net.minecraft.server.level.ServerPlayer p : server.getPlayerList().getPlayers()) {
            var prof = p.getGameProfile();
            if (String.valueOf(prof.getId()).equals(uuid)) return p;
        }
        return null;
    }

    @Override
    public void setOp(String uuid, String name, boolean op) {
        IntegratedServer server = mc().getSingleplayerServer();
        if (server == null) return;
        server.execute(() -> {
            net.minecraft.server.level.ServerPlayer p = findPlayer(server, uuid);
            if (p == null) return;
            if (op) server.getPlayerList().op(p.getGameProfile());
            else server.getPlayerList().deop(p.getGameProfile());
        });
    }

    @Override
    public void kick(String uuid, String message) {
        IntegratedServer server = mc().getSingleplayerServer();
        if (server == null) return;
        server.execute(() -> {
            net.minecraft.server.level.ServerPlayer p = findPlayer(server, uuid);
            if (p != null) p.connection.disconnect(Component.literal(message));
        });
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
