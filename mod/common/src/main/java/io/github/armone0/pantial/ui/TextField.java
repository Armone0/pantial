package io.github.armone0.pantial.ui;

import java.util.function.IntPredicate;

/** Basit tek satirlik metin kutusu (GLFW tus kodlariyla). */
public final class TextField {
    public static final int KEY_ENTER = 257, KEY_KP_ENTER = 335, KEY_BACKSPACE = 259, KEY_DELETE = 261;
    public static final int KEY_LEFT = 263, KEY_RIGHT = 262, KEY_HOME = 268, KEY_END = 269;
    public static final int KEY_A = 65, KEY_C = 67, KEY_V = 86, KEY_X = 88;
    public static final int MOD_CONTROL = 2, MOD_SUPER = 8;

    public String value = "";
    public int cursor;
    public boolean focused;
    private final int maxLength;
    private final String placeholder;
    private final IntPredicate allowed;
    private final boolean upperCase;
    public Runnable onEnter;

    public TextField(int maxLength, String placeholder, IntPredicate allowed, boolean upperCase) {
        this.maxLength = maxLength;
        this.placeholder = placeholder;
        this.allowed = allowed;
        this.upperCase = upperCase;
    }

    public void set(String v) {
        value = v == null ? "" : v;
        cursor = value.length();
    }

    public void draw(Ui ui, int x, int y, int w, int h, Runnable focusRequest) {
        boolean hov = ui.hover(x, y, x + w, y + h);
        ui.round(x, y, x + w, y + h, focused ? Theme.ACCENT : (hov ? Theme.BUTTON_HOVER : Theme.FIELD_BORDER));
        ui.round(x + 1, y + 1, x + w - 1, y + h - 1, Theme.FIELD);
        int ty = y + (h - 8) / 2;
        int inner = w - 10;
        if (value.isEmpty() && !focused) {
            ui.text(ui.ellipsize(placeholder, inner), x + 5, ty, Theme.FAINT);
        } else {
            // Imlec gorunur kalsin diye uzun metni soldan kaydir
            int start = 0;
            while (start < cursor && ui.g.width(value.substring(start, cursor)) > inner) start++;
            String shown = value.substring(start);
            while (!shown.isEmpty() && ui.g.width(shown) > inner) shown = shown.substring(0, shown.length() - 1);
            ui.text(shown, x + 5, ty, Theme.TEXT);
            if (focused && (System.currentTimeMillis() / 500) % 2 == 0) {
                int cx = x + 5 + ui.g.width(value.substring(start, Math.min(cursor, start + shown.length())));
                ui.g.fill(cx, ty - 1, cx + 1, ty + 9, Theme.ACCENT);
            }
        }
        ui.hit(x, y, x + w, y + h, focusRequest);
    }

    public boolean keyPressed(int key, int mods, Ui clipboardOwner, java.util.function.Supplier<String> getClip,
                              java.util.function.Consumer<String> setClip) {
        if (!focused) return false;
        boolean ctrl = (mods & (MOD_CONTROL | MOD_SUPER)) != 0;
        if (ctrl && key == KEY_V) {
            insert(getClip.get());
            return true;
        }
        if (ctrl && (key == KEY_C || key == KEY_X)) {
            setClip.accept(value);
            if (key == KEY_X) set("");
            return true;
        }
        if (ctrl && key == KEY_A) {
            cursor = value.length();
            return true;
        }
        switch (key) {
            case KEY_BACKSPACE -> {
                if (cursor > 0) {
                    value = value.substring(0, cursor - 1) + value.substring(cursor);
                    cursor--;
                }
                return true;
            }
            case KEY_DELETE -> {
                if (cursor < value.length()) value = value.substring(0, cursor) + value.substring(cursor + 1);
                return true;
            }
            case KEY_LEFT -> {
                cursor = Math.max(0, cursor - 1);
                return true;
            }
            case KEY_RIGHT -> {
                cursor = Math.min(value.length(), cursor + 1);
                return true;
            }
            case KEY_HOME -> {
                cursor = 0;
                return true;
            }
            case KEY_END -> {
                cursor = value.length();
                return true;
            }
            case KEY_ENTER, KEY_KP_ENTER -> {
                if (onEnter != null) onEnter.run();
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    public boolean charTyped(int codepoint) {
        if (!focused) return false;
        insert(new String(Character.toChars(codepoint)));
        return true;
    }

    private void insert(String s) {
        if (s == null) return;
        StringBuilder ok = new StringBuilder();
        s.codePoints().forEach(c -> {
            int cc = upperCase ? Character.toUpperCase(c) : c;
            if (cc >= 32 && allowed.test(cc)) ok.appendCodePoint(cc);
        });
        String add = ok.toString();
        int room = maxLength - value.length();
        if (room <= 0 || add.isEmpty()) return;
        if (add.length() > room) add = add.substring(0, room);
        value = value.substring(0, cursor) + add + value.substring(cursor);
        cursor += add.length();
    }
}
