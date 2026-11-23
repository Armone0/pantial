package io.github.armone0.pantial.fabric;

import io.github.armone0.pantial.ui.Gfx;
import io.github.armone0.pantial.ui.PantialMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Ince sarmalayici (1.21.9+: GuiGraphics ve girdi olay kayitlari). */
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
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        menu.render(new ExtractorGfx(graphics), mouseX, mouseY);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Menu kendi arka planini ciziyor; bulanik arka plani atla.
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return menu.mouseClicked(event.x(), event.y(), event.button()) || super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        return menu.mouseScrolled(scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return menu.keyPressed(event.key(), event.modifiers()) || super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return menu.charTyped(event.codepoint());
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    private final class ExtractorGfx implements Gfx {
        private final GuiGraphics g;

        ExtractorGfx(GuiGraphics g) {
            this.g = g;
        }

        @Override
        public void fill(int x1, int y1, int x2, int y2, int argb) {
            g.fill(x1, y1, x2, y2, argb);
        }

        @Override
        public void text(String s, int x, int y, int argb, boolean shadow) {
            g.drawString(font, s, x, y, argb, shadow);
        }

        @Override
        public void scaledText(String s, int x, int y, int argb, float scale) {
            g.pose().pushMatrix();
            g.pose().translate(x, y);
            g.pose().scale(scale, scale);
            g.drawString(font, s, 0, 0, argb, true);
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
