/*
 * Portions adapted from ReGlass by ReStudio / RedxAx.
 * Original project: https://github.com/RedxAx/ReGlass
 * Licensed under the MIT License. Adapted for Minecraft Fabric 1.21.1.
 */
package io.github.ikunkk02afk.liquidglassui.render.legacy;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.ikunkk02afk.liquidglassui.config.GlassDebugView;
import io.github.ikunkk02afk.liquidglassui.render.GlassFrameContext;
import io.github.ikunkk02afk.liquidglassui.render.frame.GlassFrameState;
import io.github.ikunkk02afk.liquidglassui.render.frame.GlassWidgetTexturePacker;
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

final class LegacyCompositePass implements AutoCloseable {
    private final GlassWidgetTexturePacker packer = new GlassWidgetTexturePacker();
    private final FloatBuffer uploadBuffer = MemoryUtil.memAllocFloat(
            GlassWidgetTexturePacker.WIDTH * GlassWidgetTexturePacker.ROWS * GlassWidgetTexturePacker.CHANNELS);
    private int dataTexture;

    int dataTexture() {
        ensureDataTexture();
        return dataTexture;
    }

    void draw(ShaderInstance shader, GlassFrameState state, GlassFrameContext frame,
              TextureTarget capture, TextureTarget lightBlur, TextureTarget fullBlur) {
        if (state.widgetCount() == 0) return;
        upload(state, frame);
        RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
        main.bindWrite(true);
        RenderSystem.viewport(0, 0, main.viewWidth, main.viewHeight);
        LegacyBlurProcessor.bindSampler(shader, "RawSampler", 0, capture.getColorTextureId());
        LegacyBlurProcessor.bindSampler(shader, "LightBlurSampler", 1, lightBlur.getColorTextureId());
        LegacyBlurProcessor.bindSampler(shader, "FullBlurSampler", 2, fullBlur.getColorTextureId());
        LegacyBlurProcessor.bindSampler(shader, "WidgetDataSampler", 3, dataTexture);
        set(shader, "FramebufferSize", frame.framebufferWidth(), frame.framebufferHeight());
        set(shader, "CaptureSize", capture.viewWidth, capture.viewHeight);
        set(shader, "WidgetDataSize", GlassWidgetTexturePacker.WIDTH, GlassWidgetTexturePacker.ROWS);
        setInteger(shader, "WidgetCount", state.widgetCount());
        setInteger(shader, "DebugMode", debugMode(frame.debugView()));
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableScissor();
        RenderSystem.disableCull();
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.setShader(() -> shader);
        LegacyBlurProcessor.drawFullscreenQuad();
    }

    private void upload(GlassFrameState state, GlassFrameContext frame) {
        ensureDataTexture();
        float[] packed = packer.pack(state, frame);
        uploadBuffer.clear();
        uploadBuffer.put(packed).flip();
        // Font/glyph uploads may leave a pixel-unpack buffer bound. With a PBO bound, OpenGL treats
        // the FloatBuffer address as a byte offset; stale row/skip state can also read past the buffer.
        GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, 0);
        resetPixelUnpackState();
        RenderSystem.activeTexture(GL13.GL_TEXTURE3);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, dataTexture);
        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, GlassWidgetTexturePacker.WIDTH,
                GlassWidgetTexturePacker.ROWS, GL11.GL_RGBA, GL11.GL_FLOAT, uploadBuffer);
    }

    private void ensureDataTexture() {
        if (dataTexture != 0) return;
        dataTexture = GL11.glGenTextures();
        GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, 0);
        resetPixelUnpackState();
        RenderSystem.activeTexture(GL13.GL_TEXTURE3);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, dataTexture);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_BASE_LEVEL, 0);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, 0);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RGBA32F, GlassWidgetTexturePacker.WIDTH,
                GlassWidgetTexturePacker.ROWS, 0, GL11.GL_RGBA, GL11.GL_FLOAT, (FloatBuffer) null);
    }

    private static void resetPixelUnpackState() {
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 4);
        GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, 0);
    }

    private static int debugMode(GlassDebugView view) {
        return switch (view) {
            case OFF, FINAL_COMPOSITE -> 0;
            case SDF_DISTANCE -> 1;
            case SDF_NORMAL -> 2;
            case GROUP_ID -> 3;
            case RAW_CAPTURE -> 4;
            case RAW_BLUR -> 5;
            case REFRACTION_ONLY -> 6;
            case FRESNEL_ONLY -> 7;
        };
    }

    private static void set(ShaderInstance shader, String name, float x, float y) {
        Uniform uniform = shader.getUniform(name);
        if (uniform != null) uniform.set(x, y);
    }

    private static void setInteger(ShaderInstance shader, String name, int value) {
        Uniform uniform = shader.getUniform(name);
        if (uniform != null) uniform.set(value);
    }

    @Override
    public void close() {
        if (dataTexture != 0) {
            GL11.glDeleteTextures(dataTexture);
            dataTexture = 0;
        }
        MemoryUtil.memFree(uploadBuffer);
    }
}
