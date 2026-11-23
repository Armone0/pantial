package com.example.freelan.mixin;

import com.example.freelan.FreeLAN;
import com.example.freelan.LanConfig;
import com.example.freelan.PantialClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * e4mc dunya paylasildiginda tunel adresini sohbete yazar; o adresi buradan yakalariz.
 * Yalnizca kendi dunyamiz LAN'a acikken ve henuz adres yakalanmamisken calisir: aksi halde
 * dunyaya katilan biri sohbete sahte bir *.e4mc.link yazarak arkadaslara duyurulan adresi degistirebilirdi.
 */
@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {
    @Unique
    private static final Pattern E4MC_REGEX = Pattern.compile("([a-z0-9-]+\\.e4mc\\.link(:[0-9]+)?)", Pattern.CASE_INSENSITIVE);

    @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;)V", at = @At("HEAD"))
    private void freelan$captureTunnel(Component message, CallbackInfo ci) {
        if (message == null || !LanConfig.tunnelUrl.isEmpty()) return;
        IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server == null || !server.isPublished()) return;

        Matcher matcher = E4MC_REGEX.matcher(message.getString());
        if (matcher.find()) {
            String foundUrl = matcher.group(1).toLowerCase();
            LanConfig.tunnelUrl = foundUrl;
            LanConfig.tunnelStatus = "Aktif (" + foundUrl + ")";
            FreeLAN.LOGGER.info("[Pantial e4mc] Tunel adresi yakalandi: {}", foundUrl);
            PantialClient.showNotification("Tünel Hazır", foundUrl);
        }
    }
}
