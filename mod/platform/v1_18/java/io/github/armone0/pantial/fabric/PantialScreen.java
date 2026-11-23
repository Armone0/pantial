package io.github.armone0.pantial.fabric;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.armone0.pantial.ui.Gfx;
import io.github.armone0.pantial.ui.PantialMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;

/** Ince sarmalayici: olaylari surumden bagimsiz PantialMenu'ye iletir (1.18: PoseStack). */
public class PantialScreen extends Screen {
    final Screen parent;
    private final PantialMenu menu = new PantialMenu();

    public PantialScreen(Screen parent) {
        super(new TextComponent("Pantial"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        menu.resize(this.width, this.height);
    }

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        menu.render(new PoseGfx(pose), mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        return menu.mouseClicked(x, y, button) || super.mouseClicked(x, y, button);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double delta) {
        return menu.mouseScrolled(delta);
    }

    @Override
    public boolean keyPressed(int key, int scancode, int mods) {
        return menu.keyPressed(key, mods) || super.keyPressed(key, scancode, mods);
    }

    @Override
    public boolean charTyped(char c, int mods) {
        return menu.charTyped(c);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    private final class PoseGfx implements Gfx {
        private final PoseStack pose;

        PoseGfx(PoseStack pose) {
            this.pose = pose;
        }

        @Override
        public void fill(int x1, int y1, int x2, int y2, int argb) {
            GuiComponent.fill(pose, x1, y1, x2, y2, argb);
        }

        @Override
        public void text(String s, int x, int y, int argb, boolean shadow) {
            if (shadow) font.drawShadow(pose, s, x, y, argb);
            else font.draw(pose, s, x, y, argb);
        }

        @Override
        public void scaledText(String s, int x, int y, int argb, float scale) {
            pose.pushPose();
            pose.translate(x, y, 0);
            pose.scale(scale, scale, 1);
            font.drawShadow(pose, s, 0, 0, argb);
            pose.popPose();
        }

        @Override
        public int width(String s) {
            return font.width(s);
        }

        /** 1.18/1.19'da GUI kirpma yardimcisi yok; pencere pikseline cevirip RenderSystem ile kirp. */
        @Override
        public boolean clip(int x1, int y1, int x2, int y2) {
            Window w = Minecraft.getInstance().getWindow();
            double scale = w.getGuiScale();
            RenderSystem.enableScissor((int) (x1 * scale), (int) (w.getHeight() - y2 * scale),
                    Math.max(0, (int) ((x2 - x1) * scale)), Math.max(0, (int) ((y2 - y1) * scale)));
            return true;
        }

        @Override
        public void unclip() {
            RenderSystem.disableScissor();
        }
    }
}
