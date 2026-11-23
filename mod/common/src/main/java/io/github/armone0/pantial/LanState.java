package io.github.armone0.pantial;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Paylasilan dunyanin durumu. Render, sunucu ve yoklama thread'lerinden okunur. */
public final class LanState {
    private static final Pattern E4MC = Pattern.compile("([a-z0-9-]+\\.e4mc\\.link(:[0-9]+)?)", Pattern.CASE_INSENSITIVE);

    /** Varsayilan acik: tunel dunyayi internete acar, kapaliyken adresi bilen herkes istedigi isimle girer. */
    public static volatile boolean onlineMode = true;
    public static volatile boolean announce = true;
    public static volatile String tunnelUrl = "";

    private LanState() {
    }

    /**
     * Sohbete dusen mesajlardan e4mc adresini yakalar. Yalnizca kendi dunyamiz paylasimdayken
     * ve adres henuz bilinmiyorken: aksi halde dunyaya giren biri sohbete sahte adres yazip
     * arkadaslara duyurulan adresi degistirebilirdi.
     */
    public static void onChatMessage(String text) {
        if (text == null || !tunnelUrl.isEmpty()) return;
        Platform p = Pantial.platform();
        if (p == null || p.worldState() != Platform.WorldState.PUBLISHED) return;
        Matcher m = E4MC.matcher(text);
        if (m.find()) {
            tunnelUrl = m.group(1).toLowerCase(Locale.ROOT);
            Pantial.LOGGER.info("[Pantial] e4mc adresi yakalandi: {}", tunnelUrl);
            PantialClient.notify("Tünel hazır", tunnelUrl);
        }
    }

    public static void reset() {
        tunnelUrl = "";
        onlineMode = true;
        announce = true;
    }
}
