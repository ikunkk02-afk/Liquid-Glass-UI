package io.github.ikunkk02afk.liquidglassui.widget;

import io.github.ikunkk02afk.liquidglassui.animation.GlassGroupMotion;
import io.github.ikunkk02afk.liquidglassui.config.LiquidGlassConfigData;
import io.github.ikunkk02afk.liquidglassui.render.GlassRectangle;
import net.minecraft.client.gui.components.AbstractButton;

public final class GlassGroupController {
    private GlassGroupMotion motion;
    private AbstractButton activeButton;

    public void updateTarget(AbstractButton button) {
        if (button == null || button == activeButton) return;
        GlassRectangle next = bounds(button);
        if (motion == null) motion = new GlassGroupMotion(next);
        else motion.target(next);
        activeButton = button;
    }

    public void update(double delta, LiquidGlassConfigData config) {
        if (motion != null) motion.update(delta, config.animation.springStiffness, config.animation.damping,
                config.animation.speed, config.fusion.connectionDurationSeconds,
                config.animation.reduceMotion || !config.animation.enabled);
    }

    public boolean isActive(AbstractButton button) { return button == activeButton && motion != null; }
    public GlassRectangle activeBounds(AbstractButton button) { return isActive(button) ? motion.active() : bounds(button); }
    public GlassRectangle previousBounds(AbstractButton button) { return isActive(button) ? motion.previous() : bounds(button); }
    public float merge(AbstractButton button, float strength) { return isActive(button) ? motion.mergeAmount(strength) : 0.0f; }
    public boolean hasMotion() { return motion != null; }
    public GlassRectangle activeBounds() { return motion == null ? new GlassRectangle(0, 0, 0, 0) : motion.active(); }
    public GlassRectangle previousBounds() { return motion == null ? new GlassRectangle(0, 0, 0, 0) : motion.previous(); }
    public float merge(float strength) { return motion == null ? 0.0f : motion.mergeAmount(strength); }

    public static GlassRectangle bounds(AbstractButton button) {
        return new GlassRectangle(button.getX(), button.getY(), button.getWidth(), button.getHeight());
    }
}
