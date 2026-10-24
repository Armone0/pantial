package com.example.freelan.mixin;

import com.example.freelan.gui.PantialFriendsScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin extends Screen {
    protected PauseScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void addPantialPauseButton(CallbackInfo ci) {
        int buttonWidth = 98;
        int buttonHeight = 20;
        int x = this.width / 2 + 104;
        int y = this.height / 4 + 72 + -16;

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
