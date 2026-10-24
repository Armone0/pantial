package com.example.freelan.mixin;

import com.example.freelan.gui.PantialFriendsScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(JoinMultiplayerScreen.class)
public abstract class JoinMultiplayerScreenMixin extends Screen {
    protected JoinMultiplayerScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void addPantialMultiplayerButton(CallbackInfo ci) {
        int buttonWidth = 100;
        int buttonHeight = 20;
        int x = this.width / 2 - 154;
        int y = this.height - 28;

        this.addRenderableWidget(Button.builder(
                Component.literal("§6Pantial"),
                button -> {
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(new PantialFriendsScreen(this));
                    }
                }
        ).bounds(x, y, buttonWidth, buttonHeight).build());
    }
}
