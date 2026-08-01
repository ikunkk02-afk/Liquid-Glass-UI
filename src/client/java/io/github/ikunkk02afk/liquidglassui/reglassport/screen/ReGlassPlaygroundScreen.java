package io.github.ikunkk02afk.liquidglassui.reglassport.screen;

import io.github.ikunkk02afk.liquidglassui.reglassport.ReGlassPortClient;
import io.github.ikunkk02afk.liquidglassui.reglassport.api.GlassStyle;
import io.github.ikunkk02afk.liquidglassui.reglassport.widget.LiquidGlassPortWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Isolated visual test surface; menu rendering is intentionally untouched. */
public final class ReGlassPlaygroundScreen extends Screen {
    private final Screen parent;
    private LiquidGlassPortWidget sample;

    public ReGlassPlaygroundScreen(Screen parent) {
        super(Component.translatable("liquid_glass_ui.reglass_port.playground.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        sample = addRenderableWidget(new LiquidGlassPortWidget(
                width / 2 - 90, height / 2 - 32, 180, 64,
                Component.translatable("liquid_glass_ui.reglass_port.playground.drag_me"),
                24.0f,
                GlassStyle.create().tint(0xDCEEFF, 0.13f).blurRadius(12).shadow(10.0f, 0.26f, 0.0f, 3.0f),
                true,
                () -> { }));
        sample.setMovementBounds(width, height);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        drawReferenceBackground(graphics);
        ReGlassPortClient.beginFrame(this, graphics);
        super.render(graphics, mouseX, mouseY, delta);
        ReGlassPortClient.finishFrame(this, graphics, 0);

        graphics.drawCenteredString(font, title, width / 2, 18, 0xFFFFFFFF);
        graphics.drawCenteredString(font,
                Component.translatable("liquid_glass_ui.reglass_port.playground.instructions"),
                width / 2, height - 18, 0xFFE8EDF4);
    }

    private void drawReferenceBackground(GuiGraphics graphics) {
        int split = width / 2;
        graphics.fill(0, 0, split, height, 0xFFDBE7F1);
        graphics.fill(split, 0, width, height, 0xFF152130);
        int cell = 20;
        for (int y = 0; y < height; y += cell) {
            for (int x = 0; x < width; x += cell) {
                boolean alternate = ((x / cell) + (y / cell)) % 2 == 0;
                int color = x < split
                        ? (alternate ? 0x283D7EA6 : 0x10184A6A)
                        : (alternate ? 0x304BA3B8 : 0x18246A78);
                graphics.fill(x, y, Math.min(width, x + cell), Math.min(height, y + cell), color);
            }
        }
        graphics.fill(split - 2, 0, split + 2, height, 0x70FFFFFF);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
