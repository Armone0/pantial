package io.github.armone0.pantial.fabric;

import io.github.armone0.pantial.ui.Gfx;
import com.mojang.blaze3d.platform.InputConstants;
import io.github.armone0.pantial.ui.PantialMenu;
import io.github.armone0.pantial.ui.TextField;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Ince sarmalayici (26.x: GuiGraphicsExtractor ve girdi olay kayitlari). */
public class PantialScreen extends Screen {
    final Screen parent;
    private final PantialMenu menu = new PantialMenu();

    public PantialScreen(Screen parent) {
        super(Component.literal("Pantial"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        menu.resize(this.width, this.height);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        menu.render(new ExtractorGfx(graphics), mouseX, mouseY);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // Menu kendi arka planini ciziyor; bulanik arka plani atla.
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int button = event.button() == InputConstants.MOUSE_BUTTON_LEFT ? 0 : 1;
        return menu.mouseClicked(event.x(), event.y(), button) || super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        return menu.mouseScrolled(scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return menu.keyPressed(key(event.key()), mods(event.modifiers())) || super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return menu.charTyped(event.codepoint());
    }

    /**
     * 26.3 SDL3'e gecti: tus kodlari GLFW degil SDL tarama kodu. Bu dosya her 26.x surumu icin
     * ayri derlendigi icin InputConstants o surumun degerlerini verir; menunun bekledigi GLFW
     * kodlarina ceviriyoruz.
     */
    private static int key(int k) {
        if (k == InputConstants.KEY_BACKSPACE) return TextField.KEY_BACKSPACE;
        if (k == InputConstants.KEY_DELETE) return TextField.KEY_DELETE;
        if (k == InputConstants.KEY_RETURN) return TextField.KEY_ENTER;
        if (k == InputConstants.KEY_NUMPADENTER) return TextField.KEY_KP_ENTER;
        if (k == InputConstants.KEY_LEFT) return TextField.KEY_LEFT;
        if (k == InputConstants.KEY_RIGHT) return TextField.KEY_RIGHT;
        if (k == InputConstants.KEY_HOME) return TextField.KEY_HOME;
        if (k == InputConstants.KEY_END) return TextField.KEY_END;
        if (k == InputConstants.KEY_ESCAPE) return 256;
        if (k == InputConstants.KEY_A) return TextField.KEY_A;
        if (k == InputConstants.KEY_C) return TextField.KEY_C;
        if (k == InputConstants.KEY_V) return TextField.KEY_V;
        if (k == InputConstants.KEY_X) return TextField.KEY_X;
        return -1;
    }

    private static int mods(int m) {
        int r = 0;
        if ((m & InputConstants.MOD_CONTROL) != 0) r |= TextField.MOD_CONTROL;
        if ((m & InputConstants.MOD_SUPER) != 0) r |= TextField.MOD_SUPER;
        return r;
    }

    @Override
    public void onClose() {
        Compat.setScreen(parent);
    }

    @Override
    public void removed() {
        menu.removed();
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    private final class ExtractorGfx implements Gfx {
        private final GuiGraphicsExtractor g;

        ExtractorGfx(GuiGraphicsExtractor g) {
            this.g = g;
        }

        @Override
        public void fill(int x1, int y1, int x2, int y2, int argb) {
            g.fill(x1, y1, x2, y2, argb);
        }

        @Override
        public void text(String s, int x, int y, int argb, boolean shadow) {
            g.text(font, s, x, y, argb, shadow);
        }

        @Override
        public void scaledText(String s, int x, int y, int argb, float scale) {
            g.pose().pushMatrix();
            g.pose().translate(x, y);
            g.pose().scale(scale, scale);
            g.text(font, s, 0, 0, argb, true);
            g.pose().popMatrix();
        }

        @Override
        public int width(String s) {
            return font.width(s);
        }

        @Override
        public boolean clip(int x1, int y1, int x2, int y2) {
            g.enableScissor(x1, y1, x2, y2);
            return true;
        }

        @Override
        public void unclip() {
            g.disableScissor();
        }
    }
}
