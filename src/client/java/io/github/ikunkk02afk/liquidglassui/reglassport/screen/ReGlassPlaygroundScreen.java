package io.github.ikunkk02afk.liquidglassui.reglassport.screen;

import io.github.ikunkk02afk.liquidglassui.LiquidGlassUIClient;
import io.github.ikunkk02afk.liquidglassui.reglassport.ReGlassPortClient;
import io.github.ikunkk02afk.liquidglassui.reglassport.api.GlassStyle;
import io.github.ikunkk02afk.liquidglassui.reglassport.api.ReGlassStyleMapper;
import io.github.ikunkk02afk.liquidglassui.reglassport.widget.LiquidGlassPortWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Isolated visual test surface; menu rendering is intentionally untouched. */
public final class ReGlassPlaygroundScreen extends Screen {
    private final Screen parent;
    private final List<LiquidGlassPortWidget> glassWidgets = new ArrayList<>();
    private int debugMode;
    private int nextTemporaryGroup = 1000;

    public ReGlassPlaygroundScreen(Screen parent) {
        super(Component.translatable("liquid_glass_ui.reglass_port.playground.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        glassWidgets.clear();
        int center = width / 2;

        addGlass(10, 42, 108, 150, "panel", 26.0f,
                mapped().tint(0xD9EDFF, 0.10f).blurRadius(12), 10);

        addGlass(128, 48, 94, 25, "button_one", 10.0f, mapped(), 20);
        addGlass(128, 82, 94, 25, "button_two", 10.0f, mapped(), 20);
        addGlass(128, 116, 94, 25, "button_three", 10.0f, mapped(), 20);

        addGlass(center + 30, 48, 70, 28, "fusion_a", 12.0f,
                mapped().smoothing(0.035f), 30);
        addGlass(center + 94, 48, 70, 28, "fusion_b", 12.0f,
                mapped().smoothing(0.035f), 30);
        addGlass(center + 58, 86, 88, 29, "drag_fusion", 12.0f,
                mapped().smoothing(0.035f), 30);

        addGlass(128, 163, 94, 30, "colored", 12.0f,
                mapped().tint(0xFF83BA, 0.22f).blurRadius(10), 40);
        addGlass(center + 30, 137, 82, 31, "refraction_only", 12.0f,
                mapped().blurRadius(0).refractionFactor(1.8f).dispersion(8.0f), 50);
        addGlass(center + 118, 137, 86, 31, "high_blur", 12.0f,
                mapped().blurRadius(24).refractionFactor(1.06f).dispersion(1.0f), 60);
    }

    private GlassStyle mapped() {
        return ReGlassStyleMapper.fromConfig(LiquidGlassUIClient.configManager().get())
                .shadow(10.0f, 0.24f, 0.0f, 2.0f);
    }

    private LiquidGlassPortWidget addGlass(int x, int y, int widgetWidth, int widgetHeight,
                                           String translationSuffix, float radius,
                                           GlassStyle style, int group) {
        LiquidGlassPortWidget widget = new LiquidGlassPortWidget(
                clamp(x, 0, Math.max(0, width - widgetWidth)),
                clamp(y, 30, Math.max(30, height - widgetHeight - 24)),
                widgetWidth, widgetHeight,
                Component.translatable("liquid_glass_ui.reglass_port.playground." + translationSuffix),
                radius, style, group, true, () -> { });
        widget.setMovementBounds(width, height - 20);
        glassWidgets.add(addRenderableWidget(widget));
        return widget;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        drawReferenceBackground(graphics);
        ReGlassPortClient.beginFrame(this, graphics);
        super.render(graphics, mouseX, mouseY, delta);
        ReGlassPortClient.finishFrame(this, graphics, debugMode);

        graphics.drawCenteredString(font, title.copy().append("  [").append(debugLabel()).append("]"),
                width / 2, 16, 0xFFFFFFFF);
        graphics.drawCenteredString(font,
                Component.translatable("liquid_glass_ui.reglass_port.playground.instructions_full"),
                width / 2, height - 16, 0xFFE8EDF4);
    }

    private void drawReferenceBackground(GuiGraphics graphics) {
        int split = width / 2;
        graphics.fill(0, 0, split, height, 0xFFDBE7F1);
        graphics.fill(split, 0, width, height, 0xFF152130);
        int cell = 14;
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
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && glassWidgets.stream().noneMatch(widget -> widget.isMouseOver(mouseX, mouseY))) {
            int widgetWidth = 86;
            int widgetHeight = 30;
            addGlass((int) mouseX - widgetWidth / 2, (int) mouseY - widgetHeight / 2,
                    widgetWidth, widgetHeight, "temporary", 12.0f,
                    mapped().tint(0xA7E8FF, 0.16f), nextTemporaryGroup++);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_0 || keyCode == GLFW.GLFW_KEY_KP_0) debugMode = 0;
        else if (keyCode >= GLFW.GLFW_KEY_1 && keyCode <= GLFW.GLFW_KEY_6) debugMode = keyCode - GLFW.GLFW_KEY_0;
        else if (keyCode >= GLFW.GLFW_KEY_KP_1 && keyCode <= GLFW.GLFW_KEY_KP_6) debugMode = keyCode - GLFW.GLFW_KEY_KP_0;
        else return super.keyPressed(keyCode, scanCode, modifiers);
        return true;
    }

    private Component debugLabel() {
        return Component.translatable("liquid_glass_ui.reglass_port.debug." + debugMode);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
