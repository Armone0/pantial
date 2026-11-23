package io.github.armone0.pantial.fabric;

import io.github.armone0.pantial.Pantial;
import io.github.armone0.pantial.PantialClient;
import io.github.armone0.pantial.PantialProfile;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;

public class PantialFabric implements ClientModInitializer {
    private static boolean autotestDone;

    @Override
    public void onInitializeClient() {
        Pantial.init(new FabricPlatform());

        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            boolean title = screen instanceof TitleScreen;
            if (title && Boolean.getBoolean("pantial.autotest") && !autotestDone) {
                autotestDone = true;
                client.execute(() -> open(screen));
            }
            if ((title && PantialProfile.getInstance().titleButton) || screen instanceof PauseScreen
                    || screen instanceof JoinMultiplayerScreen) {
                Screens.getWidgets(screen).add(Button.builder(Component.literal(label()), b -> open(screen))
                        .bounds(w - 82, 6, 76, 20).build());
            }
        });
    }

    static String label() {
        int n = PantialClient.invites.size();
        return n > 0 ? "§6Pantial §c(" + n + ")" : "§6Pantial";
    }

    static void open(Screen parent) {
        Minecraft.getInstance().gui.setScreen(new PantialScreen(parent));
    }
}
