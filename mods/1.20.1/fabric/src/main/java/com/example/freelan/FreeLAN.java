package com.example.freelan;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FreeLAN implements ModInitializer {
    public static final String MOD_ID = "freelan";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("[FreeLAN 1.20.1] Başlatılıyor - Minecraft LTS P2P ve Varlık Modu");
        
        // Kullanıcı profilini ve ayarları yükle
        PantialProfile profile = PantialProfile.getInstance();
        profile.load();

        // Arka plan yoklama ve varlık istemcisini başlat
        PantialClient.start();
    }
}
