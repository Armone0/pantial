package io.github.armone0.pantial.mixin;

import io.github.armone0.pantial.LanState;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Menudeki "Kimlik dogrulama" anahtari kapaliysa paylasilan dunya online-mode'u kapatir. */
@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    @Inject(method = "usesAuthentication", at = @At("HEAD"), cancellable = true)
    private void pantial$usesAuthentication(CallbackInfoReturnable<Boolean> cir) {
        if (!LanState.onlineMode) cir.setReturnValue(false);
    }
}
