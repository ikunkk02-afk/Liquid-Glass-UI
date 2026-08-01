/*
 * Portions adapted from ReGlass by ReStudio / RedxAx.
 * Original project: https://github.com/RedxAx/ReGlass
 * Licensed under the MIT License.
 * Adapted for Minecraft Fabric 1.21.1.
 */
package io.github.ikunkk02afk.liquidglassui.reglassport.api;

import io.github.ikunkk02afk.liquidglassui.reglassport.ReGlassPortClient;
import io.github.ikunkk02afk.liquidglassui.reglassport.render.ReGlassUniformData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;

/** Fluent, deferred component submission API modeled after ReGlassApi. */
public final class ReGlassPortApi {
    private final GuiGraphics graphics;
    private float x;
    private float y;
    private float width;
    private float height;
    private float cornerRadius = Float.NaN;
    private GlassStyle style = GlassStyle.create();
    private float hover;
    private float focus;
    private float press;
    private int groupId;
    private long stableId;

    private ReGlassPortApi(GuiGraphics graphics) {
        this.graphics = graphics;
    }

    public static ReGlassPortApi create(GuiGraphics graphics) {
        if (graphics == null) throw new IllegalArgumentException("graphics must not be null");
        return new ReGlassPortApi(graphics);
    }

    public ReGlassPortApi fromWidget(AbstractWidget widget) {
        if (widget == null) return this;
        dimensions(widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight());
        hover(widget.isHovered() ? 1.0f : 0.0f);
        focus(widget.isFocused() ? 1.0f : 0.0f);
        id(Integer.toUnsignedLong(System.identityHashCode(widget)));
        return this;
    }

    public ReGlassPortApi position(float x, float y) {
        this.x = finite(x);
        this.y = finite(y);
        return this;
    }

    public ReGlassPortApi size(float width, float height) {
        this.width = Math.max(0.0f, finite(width));
        this.height = Math.max(0.0f, finite(height));
        return this;
    }

    public ReGlassPortApi dimensions(float x, float y, float width, float height) {
        return position(x, y).size(width, height);
    }

    public ReGlassPortApi cornerRadius(float radius) {
        cornerRadius = Math.max(0.0f, finite(radius));
        return this;
    }

    public ReGlassPortApi style(GlassStyle style) {
        this.style = style == null ? GlassStyle.create() : style;
        return this;
    }

    public ReGlassPortApi hover(float amount) {
        hover = clamp01(amount);
        return this;
    }

    public ReGlassPortApi focus(float amount) {
        focus = clamp01(amount);
        return this;
    }

    public ReGlassPortApi press(float amount) {
        press = clamp01(amount);
        return this;
    }

    public ReGlassPortApi group(int groupId) {
        this.groupId = groupId;
        return this;
    }

    public ReGlassPortApi id(long stableId) {
        this.stableId = stableId;
        return this;
    }

    /** Registers data for the frame; no blur or shader draw occurs here. */
    public void render() {
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        float radius = Float.isNaN(cornerRadius) ? Math.min(width, height) * 0.5f : cornerRadius;
        ReGlassPortClient.submit(new ReGlassUniformData(
                stableId, x, y, width, height, Math.min(radius, Math.min(width, height) * 0.5f),
                style, hover, focus, press, groupId, 0.0f, 0.0f, screenWidth, screenHeight));
    }

    private static float finite(float value) {
        return Float.isFinite(value) ? value : 0.0f;
    }

    private static float clamp01(float value) {
        if (!Float.isFinite(value)) return 0.0f;
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
