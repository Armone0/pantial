package io.github.armone0.pantial.mixin;

import io.github.armone0.pantial.LanState;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** e4mc paylasimda tunel adresini sohbete yazar; adresi buradan yakalariz. */
@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {
    @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;)V", at = @At("HEAD"))
    private void pantial$captureTunnel(Component message, CallbackInfo ci) {
        if (message != null) LanState.onChatMessage(message.getString());
    }
}
