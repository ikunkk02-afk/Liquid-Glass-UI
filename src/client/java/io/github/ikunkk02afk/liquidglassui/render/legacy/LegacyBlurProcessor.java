package io.github.ikunkk02afk.liquidglassui.render.legacy;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.ikunkk02afk.liquidglassui.render.GlassFrameContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;

final class LegacyBlurProcessor {
    void capture(LegacyFramebufferManager targets) {
        RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
        TextureTarget capture = targets.capture();
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, capture.frameBufferId);
        GL30.glBlitFramebuffer(0, 0, main.viewWidth, main.viewHeight, 0, 0,
                capture.viewWidth, capture.viewHeight, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
    }

    Result blur(LegacyFramebufferManager targets, ShaderInstance shader, GlassFrameContext frame) {
        blit(targets.capture(), targets.blurA(), GL11.GL_LINEAR);
        TextureTarget input = targets.blurA();
        TextureTarget output = targets.blurB();
        int passes = frame.quality().blurPasses();
        int lightAt = Math.min(passes, 2);
        int samplePairs = Math.max(1, Math.min(6, (frame.quality().sampleCount() + 1) / 2));
        float framebufferScale = Math.min(
                frame.framebufferWidth() / (float) Math.max(1, frame.screenWidth()),
                frame.framebufferHeight() / (float) Math.max(1, frame.screenHeight()));
        int passesPerAxis = Math.max(1, (passes + 1) / 2);
        float radiusInBufferPixels = Math.max(0.5f,
                frame.blurRadius() * framebufferScale * frame.quality().bufferScale());
        float stepSize = radiusInBufferPixels / (samplePairs * (float) Math.sqrt(passesPerAxis));
        TextureTarget light = null;
        for (int pass = 0; pass < passes; pass++) {
            output.bindWrite(true);
            RenderSystem.viewport(0, 0, output.viewWidth, output.viewHeight);
            bindSampler(shader, "Sampler0", 0, input.getColorTextureId());
            set(shader, "TexelSize", 1.0f / input.viewWidth, 1.0f / input.viewHeight);
            set(shader, "Direction", pass % 2 == 0 ? 1.0f : 0.0f, pass % 2 == 0 ? 0.0f : 1.0f);
            set(shader, "StepSize", stepSize);
            setInteger(shader, "SamplePairs", samplePairs);
            RenderSystem.disableDepthTest();
            RenderSystem.disableBlend();
            RenderSystem.disableScissor();
            RenderSystem.disableCull();
            RenderSystem.colorMask(true, true, true, true);
            RenderSystem.setShader(() -> shader);
            drawFullscreenQuad();
            TextureTarget swap = input;
            input = output;
            output = swap;
            if (pass + 1 == lightAt) {
                blit(input, targets.lightBlur(), GL11.GL_LINEAR);
                light = targets.lightBlur();
            }
        }
        if (light == null) {
            blit(input, targets.lightBlur(), GL11.GL_LINEAR);
            light = targets.lightBlur();
        }
        return new Result(light, input);
    }

    private static void blit(RenderTarget source, RenderTarget destination, int filter) {
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, source.frameBufferId);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, destination.frameBufferId);
        GL30.glBlitFramebuffer(0, 0, source.viewWidth, source.viewHeight, 0, 0,
                destination.viewWidth, destination.viewHeight, GL11.GL_COLOR_BUFFER_BIT, filter);
    }

    static void bindSampler(ShaderInstance shader, String name, int unit, int texture) {
        RenderSystem.activeTexture(GL13.GL_TEXTURE0 + unit);
        RenderSystem.setShaderTexture(unit, texture);
        RenderSystem.bindTexture(texture);
        shader.setSampler(name, texture);
    }

    static void drawFullscreenQuad() {
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        buffer.addVertex(0.0f, 0.0f, 0.0f);
        buffer.addVertex(1.0f, 0.0f, 0.0f);
        buffer.addVertex(1.0f, 1.0f, 0.0f);
        buffer.addVertex(0.0f, 1.0f, 0.0f);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    private static void set(ShaderInstance shader, String name, float value) {
        Uniform uniform = shader.getUniform(name);
        if (uniform != null) uniform.set(value);
    }

    private static void set(ShaderInstance shader, String name, float x, float y) {
        Uniform uniform = shader.getUniform(name);
        if (uniform != null) uniform.set(x, y);
    }

    private static void setInteger(ShaderInstance shader, String name, int value) {
        Uniform uniform = shader.getUniform(name);
        if (uniform != null) uniform.set(value);
    }

    record Result(TextureTarget light, TextureTarget full) {}
}
