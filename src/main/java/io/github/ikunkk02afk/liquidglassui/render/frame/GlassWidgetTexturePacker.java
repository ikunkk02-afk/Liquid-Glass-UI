package io.github.ikunkk02afk.liquidglassui.render.frame;

import io.github.ikunkk02afk.liquidglassui.render.GlassFrameContext;
import io.github.ikunkk02afk.liquidglassui.render.material.GlassMaterial;
import io.github.ikunkk02afk.liquidglassui.render.material.GlassOptics;

import java.util.Arrays;

/** Packs logical GUI component data into the 64 x 12 RGBA32F legacy data texture. */
public final class GlassWidgetTexturePacker {
    public static final int WIDTH = 64;
    public static final int ROWS = 12;
    public static final int CHANNELS = 4;
    private final float[] data = new float[WIDTH * ROWS * CHANNELS];

    public float[] pack(GlassFrameState frame, GlassFrameContext context) {
        Arrays.fill(data, 0.0f);
        float scaleX = context.framebufferWidth() / (float) Math.max(1, context.screenWidth());
        float scaleY = context.framebufferHeight() / (float) Math.max(1, context.screenHeight());
        float radiusScale = Math.min(scaleX, scaleY);
        int count = Math.min(WIDTH, frame.widgetCount());
        for (int column = 0; column < count; column++) {
            GlassWidgetData widget = frame.widget(column);
            float fbX = widget.x * scaleX;
            float fbY = context.framebufferHeight() - (widget.y + widget.height) * scaleY;
            float fbW = widget.width * scaleX;
            float fbH = widget.height * scaleY;
            put(column, 0, fbX, fbY, fbW, fbH);
            put(column, 1, widget.cornerRadius * radiusScale, widget.hover, widget.press, widget.focus);
            GlassMaterial material = widget.material;
            put(column, 2, material.red(), material.green(), material.blue(), material.tintIntensity());
            GlassOptics optics = material.optics();
            put(column, 3, optics.thickness() * radiusScale, optics.refraction(), optics.dispersionPixels(), optics.fresnel());
            put(column, 4, optics.edgeHighlight(), optics.innerShadow(), optics.shadowStrength(), optics.backgroundClarity());
            put(column, 5, widget.mouseX * scaleX, context.framebufferHeight() - widget.mouseY * scaleY,
                    widget.animationOpacity, 1.0f);
            put(column, 6, widget.groupId, widget.shape, widget.smoothing * radiusScale, widget.hover);
            float sx = widget.scissorX * scaleX;
            float sy = context.framebufferHeight() - (widget.scissorY + widget.scissorHeight) * scaleY;
            float sw = widget.scissorWidth <= 0.0f ? context.framebufferWidth() : widget.scissorWidth * scaleX;
            float sh = widget.scissorHeight <= 0.0f ? context.framebufferHeight() : widget.scissorHeight * scaleY;
            put(column, 7, widget.scissorWidth <= 0.0f ? 0.0f : sx,
                    widget.scissorHeight <= 0.0f ? 0.0f : sy,
                    widget.scissorWidth <= 0.0f ? context.framebufferWidth() : sx + sw,
                    widget.scissorHeight <= 0.0f ? context.framebufferHeight() : sy + sh);
            put(column, 8, widget.capsuleStartX * scaleX,
                    context.framebufferHeight() - widget.capsuleStartY * scaleY,
                    widget.capsuleEndX * scaleX,
                    context.framebufferHeight() - widget.capsuleEndY * scaleY);
            put(column, 9, widget.capsuleRadius * radiusScale, 12.0f * radiusScale,
                    material.blurMix(), 1.0f);
            put(column, 10, optics.noiseStrength(), material.adaptBrightness() ? 1.0f : 0.0f,
                    material.edgeWidth() * radiusScale, material.mouseHighlight());
            put(column, 11, material.materialOpacity(), optics.edgeRefractionRange(),
                    optics.mouseHighlightRange(), 0.0f);
        }
        return data;
    }

    private void put(int column, int row, float x, float y, float z, float w) {
        int index = (row * WIDTH + column) * CHANNELS;
        data[index] = x;
        data[index + 1] = y;
        data[index + 2] = z;
        data[index + 3] = w;
    }
}
