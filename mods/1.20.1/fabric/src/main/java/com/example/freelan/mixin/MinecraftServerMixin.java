package com.example.freelan.mixin;

import com.example.freelan.LanConfig;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {

    /**
     * Yerel ağ dünyalarında çevrim dışı (cracked / korsan) hesapların "Invalid session" hatası
     * almadan oyuna bağlanabilmesi için sunucu kimlik doğrulamasını (online-mode) LanConfig ayarına bağlar.
     */
    @Inject(method = "usesAuthentication", at = @At("HEAD"), cancellable = true)
    private void freelan$usesAuthentication(CallbackInfoReturnable<Boolean> cir) {
        if (!LanConfig.onlineMode) {
            cir.setReturnValue(false);
        }
    }
}
