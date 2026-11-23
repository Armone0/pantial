package io.github.armone0.pantial.ui;

import java.util.ArrayList;
import java.util.List;

/**
 * Anlik-mod (immediate mode) arayuz yardimcisi: her karede cizilen dugmeler tiklanabilir
 * alanlarini kaydeder, bir sonraki tiklama bu listeye gore islenir.
 */
public final class Ui {
    public enum Style { NORMAL, PRIMARY, DANGER, GHOST }

    private record Hit(int x1, int y1, int x2, int y2, Runnable action) {
    }

    public Gfx g;
    public int mx, my;
    private final List<Hit> hits = new ArrayList<>();
    private List<Hit> lastHits = new ArrayList<>();
    private int cx1 = Integer.MIN_VALUE, cy1 = Integer.MIN_VALUE, cx2 = Integer.MAX_VALUE, cy2 = Integer.MAX_VALUE;

    public void begin(Gfx g, int mx, int my) {
        this.g = g;
        this.mx = mx;
        this.my = my;
        hits.clear();
        noClip();
    }

    public void end() {
        lastHits = new ArrayList<>(hits);
    }

    /** Son karede kaydedilen alanlardan tiklananin aksiyonunu calistirir. Ustteki (sonra eklenen) once. */
    public boolean click(double x, double y) {
        for (int i = lastHits.size() - 1; i >= 0; i--) {
            Hit h = lastHits.get(i);
            if (x >= h.x1 && x < h.x2 && y >= h.y1 && y < h.y2) {
                h.action.run();
                return true;
            }
        }
        return false;
    }

    /** Tiklama/hover alanlarini sinirlar (kaydirilan listeler icin). */
    public void clipArea(int x1, int y1, int x2, int y2) {
        cx1 = x1;
        cy1 = y1;
        cx2 = x2;
        cy2 = y2;
    }

    public void noClip() {
        cx1 = cy1 = Integer.MIN_VALUE;
        cx2 = cy2 = Integer.MAX_VALUE;
    }

    public void hit(int x1, int y1, int x2, int y2, Runnable action) {
        int a = Math.max(x1, cx1), b = Math.max(y1, cy1), c = Math.min(x2, cx2), d = Math.min(y2, cy2);
        if (a < c && b < d) hits.add(new Hit(a, b, c, d, action));
    }

    public boolean hover(int x1, int y1, int x2, int y2) {
        return mx >= Math.max(x1, cx1) && mx < Math.min(x2, cx2) && my >= Math.max(y1, cy1) && my < Math.min(y2, cy2);
    }

    // --- cizim ---

    /** Koseleri 1px kirpilmis dikdortgen: Minecraft pikselinde yuvarlak kose hissi verir. */
    public void round(int x1, int y1, int x2, int y2, int color) {
        g.fill(x1 + 1, y1, x2 - 1, y2, color);
        g.fill(x1, y1 + 1, x1 + 1, y2 - 1, color);
        g.fill(x2 - 1, y1 + 1, x2, y2 - 1, color);
    }

    public void outline(int x1, int y1, int x2, int y2, int color) {
        g.fill(x1 + 1, y1, x2 - 1, y1 + 1, color);
        g.fill(x1 + 1, y2 - 1, x2 - 1, y2, color);
        g.fill(x1, y1 + 1, x1 + 1, y2 - 1, color);
        g.fill(x2 - 1, y1 + 1, x2, y2 - 1, color);
    }

    public void gradient(int x1, int y1, int x2, int y2, int top, int bottom) {
        int h = Math.max(1, y2 - y1);
        for (int y = y1; y < y2; y += 4) {
            g.fill(x1, y, x2, Math.min(y + 4, y2), Theme.lerp(top, bottom, (y - y1) / (float) h));
        }
    }

    public void dot(int x, int y, int color) {
        g.fill(x + 1, y, x + 4, y + 5, color);
        g.fill(x, y + 1, x + 5, y + 4, color);
    }

    public void text(String s, int x, int y, int color) {
        g.text(s, x, y, color, false);
    }

    public void center(String s, int cx, int y, int color) {
        g.text(s, cx - g.width(s) / 2, y, color, false);
    }

    public String ellipsize(String s, int maxWidth) {
        if (g.width(s) <= maxWidth) return s;
        String dots = "...";
        int end = s.length();
        while (end > 0 && g.width(s.substring(0, end) + dots) > maxWidth) end--;
        return s.substring(0, end) + dots;
    }

    public boolean button(int x, int y, int w, int h, String label, Style style, boolean enabled, Runnable onClick) {
        boolean hov = enabled && hover(x, y, x + w, y + h);
        int bg, fg;
        switch (style) {
            case PRIMARY -> {
                bg = hov ? Theme.ACCENT_HOVER : Theme.ACCENT;
                fg = Theme.ACCENT_DARK;
            }
            case DANGER -> {
                bg = hov ? Theme.RED_BG_HOVER : Theme.RED_BG;
                fg = Theme.RED;
            }
            case GHOST -> {
                bg = hov ? Theme.BUTTON : 0;
                fg = hov ? Theme.TEXT : Theme.MUTED;
            }
            default -> {
                bg = hov ? Theme.BUTTON_HOVER : Theme.BUTTON;
                fg = Theme.TEXT;
            }
        }
        if (!enabled) {
            bg = Theme.DISABLED;
            fg = Theme.FAINT;
        }
        if (bg != 0) round(x, y, x + w, y + h, bg);
        if (style == Style.PRIMARY && enabled) g.fill(x + 1, y + h - 1, x + w - 1, y + h, 0x40000000);
        String l = ellipsize(label, w - 6);
        g.text(l, x + (w - g.width(l)) / 2, y + (h - 8) / 2, fg, false);
        if (enabled) hit(x, y, x + w, y + h, onClick);
        return hov;
    }

    /** Acik/kapali anahtari: kaydirici gorunumlu. */
    public void toggle(int x, int y, boolean on, Runnable onClick) {
        int w = 22, h = 12;
        boolean hov = hover(x, y, x + w, y + h);
        round(x, y, x + w, y + h, on ? (hov ? Theme.ACCENT_HOVER : Theme.ACCENT) : (hov ? Theme.BUTTON_HOVER : Theme.BUTTON));
        int kx = on ? x + w - 10 : x + 2;
        round(kx, y + 2, kx + 8, y + h - 2, on ? Theme.ACCENT_DARK : Theme.MUTED);
        hit(x, y, x + w, y + h, onClick);
    }
}
