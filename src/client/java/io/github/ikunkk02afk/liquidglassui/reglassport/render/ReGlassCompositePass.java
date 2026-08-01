/*
 * Portions adapted from ReGlass by ReStudio / RedxAx.
 * Original project: https://github.com/RedxAx/ReGlass
 * Licensed under the MIT License.
 * Adapted for Minecraft Fabric 1.21.1.
 */
package io.github.ikunkk02afk.liquidglassui.reglassport.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.ikunkk02afk.liquidglassui.reglassport.api.GlassOptics;
import io.github.ikunkk02afk.liquidglassui.reglassport.api.GlassPortConfig;
import io.github.ikunkk02afk.liquidglassui.reglassport.api.GlassStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.util.List;

/** Uploads one 64x12 float texture and executes the unified GUI composite. */
public final class ReGlassCompositePass implements AutoCloseable {
    public static final int WIDTH = 64;
    public static final int ROWS = 12;
    private static final int CHANNELS = 4;
    private final float[] packed = new float[WIDTH * ROWS * CHANNELS];
    private final FloatBuffer upload = MemoryUtil.memAllocFloat(packed.length);
    private int dataTexture;

    public void draw(ShaderInstance shader, List<ReGlassUniformData> widgets,
                     int screenWidth, int screenHeight, int framebufferWidth, int framebufferHeight,
                     int rawTexture, int debugMode) {
        if (widgets.isEmpty()) return;
        upload(widgets, screenWidth, screenHeight, framebufferWidth, framebufferHeight);
        RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
        main.bindWrite(true);
        RenderSystem.viewport(0, 0, main.viewWidth, main.viewHeight);
        bindSampler(shader, "RawSampler", 0, rawTexture);
        bindSampler(shader, "WidgetDataSampler", 1, dataTexture);
        set(shader, "FramebufferSize", framebufferWidth, framebufferHeight);
        setInteger(shader, "WidgetCount", widgets.size());
        setInteger(shader, "DebugMode", debugMode);
        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.disableScissor();
        RenderSystem.disableCull();
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.setShader(() -> shader);
        drawFullscreenQuad();
    }

    private void upload(List<ReGlassUniformData> widgets, int screenWidth, int screenHeight,
                        int framebufferWidth, int framebufferHeight) {
        ensureDataTexture();
        java.util.Arrays.fill(packed, 0.0f);
        float scaleX = framebufferWidth / (float) Math.max(1, screenWidth);
        float scaleY = framebufferHeight / (float) Math.max(1, screenHeight);
        float radiusScale = Math.min(scaleX, scaleY);
        GlassPortConfig defaults = GlassPortConfig.DEFAULTS;
        for (int column = 0; column < widgets.size() && column < WIDTH; column++) {
            ReGlassUniformData widget = widgets.get(column);
            GlassStyle style = widget.style();
            GlassOptics optics = style.optics(defaults);
            float x = widget.x() * scaleX;
            float y = framebufferHeight - (widget.y() + widget.height()) * scaleY;
            float width = widget.width() * scaleX;
            float height = widget.height() * scaleY;
            float radius = style.resolvedCornerRadius(widget.cornerRadius()) * radiusScale;
            put(column, 0, x, y, width, height);
            put(column, 1, radius, radius, radius, radius);
            int tint = style.tintColor(defaults);
            put(column, 2, red(tint), green(tint), blue(tint), style.tintAlpha(defaults));
            put(column, 3, optics.refractionThickness() * radiusScale, optics.refractionFactor(),
                    optics.dispersion() * radiusScale, optics.fresnelRange() * radiusScale);
            put(column, 4, optics.fresnelHardness(), optics.fresnelFactor(),
                    optics.glareRange() * radiusScale, optics.glareHardness());
            put(column, 5, optics.glareConvergence(), optics.glareOppositeFactor(),
                    optics.glareFactor(), optics.glareAngle());
            put(column, 6, style.smoothing(defaults) * framebufferHeight, widget.groupId(),
                    style.blurRadius(defaults), 0.0f);
            float scissorX = widget.scissorX() * scaleX;
            float scissorY = framebufferHeight - (widget.scissorY() + widget.scissorHeight()) * scaleY;
            put(column, 7, scissorX, scissorY, scissorX + widget.scissorWidth() * scaleX,
                    scissorY + widget.scissorHeight() * scaleY);
            put(column, 8, style.shadowExpand(defaults) * radiusScale, style.shadowFactor(defaults),
                    style.shadowOffsetX(defaults) * scaleX, style.shadowOffsetY(defaults) * scaleY);
            int shadow = style.shadowColor(defaults);
            put(column, 9, red(shadow), green(shadow), blue(shadow), style.shadowColorAlpha(defaults));
            put(column, 10, widget.hover(), widget.focus(), widget.press(), seed(widget.stableId()));
            put(column, 11, 0.0f, 0.0f, 0.0f, 1.0f);
        }
        upload.clear();
        upload.put(packed).flip();
        GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, 0);
        resetPixelUnpackState();
        RenderSystem.activeTexture(GL13.GL_TEXTURE1);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, dataTexture);
        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, WIDTH, ROWS,
                GL11.GL_RGBA, GL11.GL_FLOAT, upload);
    }

    private void put(int column, int row, float x, float y, float z, float w) {
        int offset = (row * WIDTH + column) * CHANNELS;
        packed[offset] = x;
        packed[offset + 1] = y;
        packed[offset + 2] = z;
        packed[offset + 3] = w;
    }

    private void ensureDataTexture() {
        if (dataTexture != 0) return;
        dataTexture = GL11.glGenTextures();
        GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, 0);
        resetPixelUnpackState();
        RenderSystem.activeTexture(GL13.GL_TEXTURE1);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, dataTexture);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_BASE_LEVEL, 0);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, 0);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RGBA32F, WIDTH, ROWS, 0,
                GL11.GL_RGBA, GL11.GL_FLOAT, (FloatBuffer) null);
    }

    private static void bindSampler(ShaderInstance shader, String name, int unit, int texture) {
        RenderSystem.activeTexture(GL13.GL_TEXTURE0 + unit);
        RenderSystem.setShaderTexture(unit, texture);
        RenderSystem.bindTexture(texture);
        shader.setSampler(name, texture);
    }

    private static void drawFullscreenQuad() {
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        buffer.addVertex(0.0f, 0.0f, 0.0f);
        buffer.addVertex(1.0f, 0.0f, 0.0f);
        buffer.addVertex(1.0f, 1.0f, 0.0f);
        buffer.addVertex(0.0f, 1.0f, 0.0f);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    private static void set(ShaderInstance shader, String name, float x, float y) {
        Uniform uniform = shader.getUniform(name);
        if (uniform != null) uniform.set(x, y);
    }

    private static void setInteger(ShaderInstance shader, String name, int value) {
        Uniform uniform = shader.getUniform(name);
        if (uniform != null) uniform.set(value);
    }

    private static void resetPixelUnpackState() {
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 4);
        GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, 0);
    }

    private static float red(int color) { return ((color >> 16) & 0xFF) / 255.0f; }
    private static float green(int color) { return ((color >> 8) & 0xFF) / 255.0f; }
    private static float blue(int color) { return (color & 0xFF) / 255.0f; }
    private static float seed(long id) {
        long mixed = id ^ (id >>> 33);
        mixed *= 0xff51afd7ed558ccdl;
        mixed ^= mixed >>> 33;
        return (mixed & 0xFFFFFFL) / (float) 0x1000000;
    }

    @Override
    public void close() {
        if (dataTexture != 0) GL11.glDeleteTextures(dataTexture);
        dataTexture = 0;
        MemoryUtil.memFree(upload);
    }
}
