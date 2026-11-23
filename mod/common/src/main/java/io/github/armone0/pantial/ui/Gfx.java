package io.github.armone0.pantial.ui;

/** Surumden bagimsiz cizim yuzeyi; her surumun platform katmani uygular. Renkler ARGB. */
public interface Gfx {
    void fill(int x1, int y1, int x2, int y2, int argb);

    void text(String s, int x, int y, int argb, boolean shadow);

    void scaledText(String s, int x, int y, int argb, float scale);

    int width(String s);

    /** Kirpma destekleniyorsa true; desteklemeyen surumlerde kismen gorunen satirlar hic cizilmez. */
    default boolean clip(int x1, int y1, int x2, int y2) {
        return false;
    }

    default void unclip() {
    }
}
