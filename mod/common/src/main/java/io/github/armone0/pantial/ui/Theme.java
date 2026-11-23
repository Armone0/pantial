package io.github.armone0.pantial.ui;

public final class Theme {
    public static final int BG_TOP = 0xFF0B0E17;
    public static final int BG_BOTTOM = 0xFF171C2E;
    public static final int SHADE = 0xB0000000;
    public static final int PANEL = 0xF2141826;
    public static final int PANEL_BORDER = 0xFF2B3354;
    public static final int HEADER = 0xFF1A2034;
    public static final int CARD = 0xFF1D2338;
    public static final int CARD_HOVER = 0xFF252C47;
    public static final int FIELD = 0xFF0F1320;
    public static final int FIELD_BORDER = 0xFF3A4366;
    public static final int BUTTON = 0xFF2A3253;
    public static final int BUTTON_HOVER = 0xFF37416B;
    public static final int DISABLED = 0xFF20263B;
    public static final int ACCENT = 0xFFFFB020;
    public static final int ACCENT_HOVER = 0xFFFFC452;
    public static final int ACCENT_DARK = 0xFF2A1A00;
    public static final int TEXT = 0xFFEDEFF7;
    public static final int MUTED = 0xFF8F97B3;
    public static final int FAINT = 0xFF596079;
    public static final int GREEN = 0xFF4ADE80;
    public static final int YELLOW = 0xFFFACC15;
    public static final int RED = 0xFFF87171;
    public static final int RED_BG = 0xFF5B1E26;
    public static final int RED_BG_HOVER = 0xFF7A2733;
    public static final int BLUE = 0xFF60A5FA;

    private static final int[] AVATAR = {
            0xFFEF4444, 0xFFF97316, 0xFFEAB308, 0xFF22C55E, 0xFF14B8A6,
            0xFF3B82F6, 0xFF6366F1, 0xFFA855F7, 0xFFEC4899
    };

    private Theme() {
    }

    public static int avatarColor(String seed) {
        return AVATAR[Math.floorMod(seed == null ? 0 : seed.hashCode(), AVATAR.length)];
    }

    /** Renkler arasi dogrusal gecis (0..1). */
    public static int lerp(int a, int b, float t) {
        int r = 0;
        for (int shift = 0; shift <= 24; shift += 8) {
            int ca = (a >>> shift) & 0xFF, cb = (b >>> shift) & 0xFF;
            r |= ((int) (ca + (cb - ca) * t) & 0xFF) << shift;
        }
        return r;
    }
}
