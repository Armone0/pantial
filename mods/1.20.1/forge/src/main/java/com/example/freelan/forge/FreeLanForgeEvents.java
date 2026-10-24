package com.example.freelan.forge;

import com.example.freelan.gui.PantialFriendsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class FreeLanForgeEvents {

    @SubscribeEvent
    public void onScreenInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof TitleScreen titleScreen) {
            int x = titleScreen.width / 2 - 50;
            int y = titleScreen.height / 4 + 48 + 72 + 12;

            event.addListener(Button.builder(
                    Component.literal("§6Pantial"),
                    btn -> Minecraft.getInstance().setScreen(new PantialFriendsScreen(titleScreen))
            ).bounds(x, y, 100, 20).build());
        } else if (event.getScreen() instanceof PauseScreen pauseScreen) {
            int x = pauseScreen.width / 2 + 104;
            int y = pauseScreen.height / 4 + 72 - 16;

            event.addListener(Button.builder(
                    Component.literal("§6Pantial"),
                    btn -> Minecraft.getInstance().setScreen(new PantialFriendsScreen(pauseScreen))
            ).bounds(x, y, 98, 20).build());
        }
    }
}
