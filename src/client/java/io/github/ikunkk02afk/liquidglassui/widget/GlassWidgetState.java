package io.github.ikunkk02afk.liquidglassui.widget;

import io.github.ikunkk02afk.liquidglassui.animation.GlassAnimationState;
import io.github.ikunkk02afk.liquidglassui.config.LiquidGlassConfigData;
import net.minecraft.client.gui.components.AbstractButton;

public final class GlassWidgetState {
    private final AbstractButton button;
    private final GlassAnimationState animation = new GlassAnimationState();
    private boolean pressed;

    public GlassWidgetState(AbstractButton button) {
        this.button = button;
    }

    public void update(double delta, int mouseX, int mouseY, LiquidGlassConfigData config) {
        float normalizedX = button.getWidth() == 0 ? 0.5f : (mouseX - button.getX()) / (float) button.getWidth();
        float normalizedY = button.getHeight() == 0 ? 0.5f : (mouseY - button.getY()) / (float) button.getHeight();
        boolean hovered = (button.isMouseOver(mouseX, mouseY) || button.isFocused()) && button.active;
        if (!config.animation.enabled) {
            animation.update(delta, false, false, 0.5f, 0.5f, 1.0f, 1.0f, 500.0f, 80.0f, true);
            return;
        }
        animation.update(delta * config.animation.speed, hovered, pressed,
                config.animation.mouseFollow ? normalizedX : 0.5f,
                config.animation.mouseFollow ? normalizedY : 0.5f,
                config.animation.hoverScale, config.animation.pressedScale, config.animation.springStiffness,
                config.animation.damping, config.animation.reduceMotion);
    }

    public AbstractButton button() { return button; }
    public GlassAnimationState animation() { return animation; }
    public boolean pressed() { return pressed; }
    public void pressed(boolean pressed) { this.pressed = pressed; }
}
