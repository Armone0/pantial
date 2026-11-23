package io.github.armone0.pantial.mixin;

import com.mojang.authlib.GameProfile;
import io.github.armone0.pantial.Access;
import io.netty.channel.local.LocalAddress;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.SocketAddress;

/** Paylasilan dunyaya girisi denetler: varsayilan olarak yalnizca Pantial arkadaslari. */
@Mixin(PlayerList.class)
public abstract class PlayerListMixin {
    @Shadow
    public abstract MinecraftServer getServer();

    @Inject(method = "canPlayerLogin", at = @At("HEAD"), cancellable = true)
    private void pantial$checkAccess(SocketAddress address, GameProfile profile, CallbackInfoReturnable<Component> cir) {
        MinecraftServer server = getServer();
        if (!(server instanceof IntegratedServer)) return;
        String msg = Access.check(String.valueOf(profile.getId()), profile.getName(), address instanceof LocalAddress, server.getSingleplayerProfile() == null ? null : server.getSingleplayerProfile().getName());
        if (msg != null) cir.setReturnValue(Component.literal(msg));
    }
}
