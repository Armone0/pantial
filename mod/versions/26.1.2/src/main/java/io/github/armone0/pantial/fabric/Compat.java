package io.github.armone0.pantial.fabric;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.world.level.GameType;

/** 26.x alt surumleri arasindaki kucuk API farklari (yalnizca bu Minecraft surumu icin). */
final class Compat {
    static final boolean SUPPORTS_GAME_MODE = true;

    private Compat() {
    }

    static void setScreen(Screen screen) {
        Minecraft.getInstance().setScreen(screen);
    }

    static Screen screen() {
        return Minecraft.getInstance().screen;
    }

    static ToastManager toasts() {
        return Minecraft.getInstance().getToastManager();
    }

    static boolean publish(IntegratedServer server, GameType mode, boolean cheats, int port) {
        return server.publishServer(mode, cheats, port);
    }
}
