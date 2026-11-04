package com.example.freelan.forge;

import com.example.freelan.gui.FreeLanScreen;
import com.example.freelan.gui.PantialFriendsScreen119;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.ShareToLanScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Minecraft 1.19.2 & 1.19.4 Forge Arayuz Olay Dinleyicisi.
 * 1.19.2'de Button.builder bulunmaz; standart new Button(x, y, w, h, Component, OnPress) kullanılır.
 */
public class FreeLanForgeEvents119 {

    @SubscribeEvent
    public void onScreenInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof TitleScreen titleScreen) {
            int x = titleScreen.width / 2 - 50;
            int defaultY = titleScreen.height / 4 + 48 + 72 + 12;
            int y = Math.min(defaultY, titleScreen.height - 28);

            event.addListener(new Button(
                    x, y, 100, 20,
                    Component.literal("§6Pantial"),
                    btn -> Minecraft.getInstance().setScreen(new PantialFriendsScreen119(titleScreen))
            ));
        } else if (event.getScreen() instanceof PauseScreen pauseScreen) {
            int buttonWidth = 98;
            int x = Math.min(pauseScreen.width / 2 + 104, pauseScreen.width - buttonWidth - 6);
            int y = pauseScreen.height / 4 + 72 - 16;

            event.addListener(new Button(
                    x, y, buttonWidth, 20,
                    Component.literal("§6Pantial"),
                    btn -> Minecraft.getInstance().setScreen(new PantialFriendsScreen119(pauseScreen))
            ));
        } else if (event.getScreen() instanceof JoinMultiplayerScreen joinScreen) {
            int x = joinScreen.width / 2 - 154;
            int y = joinScreen.height - 28;

            event.addListener(new Button(
                    x, y, 100, 20,
                    Component.literal("§6Pantial"),
                    btn -> Minecraft.getInstance().setScreen(new PantialFriendsScreen119(joinScreen))
            ));
        } else if (event.getScreen() instanceof ShareToLanScreen) {
            Minecraft.getInstance().execute(() -> {
                Minecraft.getInstance().setScreen(new FreeLanScreen(null));
            });
        }
    }
}
