package com.example.freelan.mixin;

import com.example.freelan.gui.FreeLanScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ShareToLanScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShareToLanScreen.class)
public abstract class ShareToLanScreenMixin extends Screen {
    @Unique
    private Screen freelan$parentScreen;

    protected ShareToLanScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void freelan$captureParent(Screen parent, CallbackInfo ci) {
        this.freelan$parentScreen = parent;
    }

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void freelan$replaceScreen(CallbackInfo ci) {
        ci.cancel();
        Minecraft.getInstance().setScreen(new FreeLanScreen(this.freelan$parentScreen));
    }
}
