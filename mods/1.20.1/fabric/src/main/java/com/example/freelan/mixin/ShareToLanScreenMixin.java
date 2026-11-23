package com.example.freelan.mixin;

import com.example.freelan.gui.FreeLanScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ShareToLanScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Vanilla "LAN'a Ac" ekranini FreeLAN ekraniyla degistirir. */
@Mixin(ShareToLanScreen.class)
public abstract class ShareToLanScreenMixin extends Screen {
    @Shadow @Final private Screen lastScreen;

    protected ShareToLanScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void freelan$replaceScreen(CallbackInfo ci) {
        ci.cancel();
        if (this.minecraft != null) {
            this.minecraft.setScreen(new FreeLanScreen(this.lastScreen));
        }
    }
}
