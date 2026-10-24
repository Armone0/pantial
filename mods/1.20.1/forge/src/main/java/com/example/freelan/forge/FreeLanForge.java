package com.example.freelan.forge;

import com.example.freelan.FreeLAN;
import com.example.freelan.PantialClient;
import com.example.freelan.PantialProfile;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("freelan")
public class FreeLanForge {
    public FreeLanForge() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onClientSetup);
        MinecraftForge.EVENT_BUS.register(new FreeLanForgeEvents());
    }

    private void onClientSetup(final FMLClientSetupEvent event) {
        FreeLAN.LOGGER.info("[FreeLAN Forge 1.20.1] İstemci kurulumu tamamlanıyor...");
        PantialProfile.getInstance().load();
        PantialClient.start();
    }
}
