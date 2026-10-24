package com.example.freelan.mixin;

import com.example.freelan.FreeLAN;
import com.example.freelan.LanConfig;
import com.example.freelan.PantialClient;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {
    private static final Pattern E4MC_REGEX = Pattern.compile("([a-z0-9-]+\\.e4mc\\.link(:[0-9]+)?)", Pattern.CASE_INSENSITIVE);

    @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;)V", at = @At("HEAD"))
    private void onChatMessage(Component message, CallbackInfo ci) {
        if (message == null) return;
        String rawText = message.getString();

        Matcher matcher = E4MC_REGEX.matcher(rawText);
        if (matcher.find()) {
            String foundUrl = matcher.group(1);
            LanConfig.tunnelUrl = foundUrl;
            LanConfig.tunnelStatus = "Aktif (" + foundUrl + ")";
            FreeLAN.LOGGER.info("[Pantial e4mc] Tünel adresi sohbetten otomatik yakalandı: {}", foundUrl);
            PantialClient.showNotification("Pantial Tünel Yakalandı", "Dünya adresi: " + foundUrl);
        }
    }
}
