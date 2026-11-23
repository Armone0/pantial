package io.github.armone0.pantial;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Pantial {
    public static final String MOD_ID = "pantial";
    public static final Logger LOGGER = LoggerFactory.getLogger("Pantial");
    private static Platform platform;

    private Pantial() {
    }

    public static Platform platform() {
        return platform;
    }

    /** Platform katmani istemci baslarken bir kez cagirir. */
    public static void init(Platform p) {
        platform = p;
        PantialProfile.getInstance().load();
        PantialClient.start();
        LOGGER.info("[Pantial] Minecraft {} icin baslatildi, kod: {}", p.minecraftVersion(),
                PantialProfile.getInstance().myUserCode);
    }
}
