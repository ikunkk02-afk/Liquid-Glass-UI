/*
 * Portions adapted from ReGlass by ReStudio / RedxAx.
 * Original project: https://github.com/RedxAx/ReGlass
 * Licensed under the MIT License.
 * Adapted for Minecraft Fabric 1.21.1.
 */
package io.github.ikunkk02afk.liquidglassui.reglassport.render;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.ikunkk02afk.liquidglassui.reglassport.api.GlassPortConfig;
import net.minecraft.client.renderer.ShaderInstance;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/** Separable Gaussian precompute: one X and one Y pass per distinct positive radius. */
public final class ReGlassBlurRuntime {
    public static final int MAX_LEVELS = 5;
    public static final int MAX_RADIUS = 64;

    public Result render(ShaderInstance shader, ReGlassFramebufferManager targets,
                         List<ReGlassUniformData> widgets, int rawTexture) {
        LinkedHashSet<Integer> requested = new LinkedHashSet<>();
        GlassPortConfig defaults = GlassPortConfig.DEFAULTS;
        for (ReGlassUniformData widget : widgets) {
            requested.add(Math.max(0, Math.min(MAX_RADIUS, widget.style().blurRadius(defaults))));
            if (requested.size() >= MAX_LEVELS) break;
        }
        if (requested.isEmpty()) requested.add(0);

        ArrayList<BlurLevel> levels = new ArrayList<>(requested.size());
        int passCount = 0;
        for (int radius : requested) {
            if (radius <= 0) {
                levels.add(new BlurLevel(0, rawTexture));
                continue;
            }
            TextureTarget temporary = targets.blurTemporary();
            TextureTarget output = targets.blurOutput(radius);
            float[] weights = gaussian(radius);
            draw(shader, temporary, rawTexture, 1.0f, 0.0f, radius, weights);
            draw(shader, output, temporary.getColorTextureId(), 0.0f, 1.0f, radius, weights);
            levels.add(new BlurLevel(radius, output.getColorTextureId()));
            passCount += 2;
        }
        return new Result(List.copyOf(levels), passCount);
    }

    private static void draw(ShaderInstance shader, TextureTarget target, int sourceTexture,
                             float directionX, float directionY, int radius, float[] weights) {
        target.bindWrite(true);
        RenderSystem.viewport(0, 0, target.viewWidth, target.viewHeight);
        bindSampler(shader, "DiffuseSampler", 0, sourceTexture);
        set(shader, "OutSize", target.viewWidth, target.viewHeight);
        set(shader, "Direction", directionX, directionY);
        setInteger(shader, "Radius", radius);
        Uniform kernel = shader.getUniform("Weights");
        if (kernel != null) kernel.set(weights);
        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.disableScissor();
        RenderSystem.disableCull();
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.setShader(() -> shader);
        drawFullscreenQuad();
    }

    static float[] gaussian(int radius) {
        int safeRadius = Math.max(0, Math.min(MAX_RADIUS, radius));
        float[] weights = new float[MAX_RADIUS + 1];
        if (safeRadius == 0) {
            weights[0] = 1.0f;
            return weights;
        }
        float sigma = Math.max(0.5f, safeRadius / 3.0f);
        float sum = 0.0f;
        for (int index = 0; index <= safeRadius; index++) {
            float weight = (float) Math.exp(-0.5f * index * index / (sigma * sigma));
            weights[index] = weight;
            sum += index == 0 ? weight : 2.0f * weight;
        }
        for (int index = 0; index <= safeRadius; index++) weights[index] /= sum;
        return weights;
    }

    private static void bindSampler(ShaderInstance shader, String name, int unit, int texture) {
        RenderSystem.activeTexture(GL13.GL_TEXTURE0 + unit);
        RenderSystem.setShaderTexture(unit, texture);
        RenderSystem.bindTexture(texture);
        shader.setSampler(name, texture);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
    }

    private static void set(ShaderInstance shader, String name, float x, float y) {
        Uniform uniform = shader.getUniform(name);
        if (uniform != null) uniform.set(x, y);
    }

    private static void setInteger(ShaderInstance shader, String name, int value) {
        Uniform uniform = shader.getUniform(name);
        if (uniform != null) uniform.set(value);
    }

    private static void drawFullscreenQuad() {
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        buffer.addVertex(0.0f, 0.0f, 0.0f);
        buffer.addVertex(1.0f, 0.0f, 0.0f);
        buffer.addVertex(1.0f, 1.0f, 0.0f);
        buffer.addVertex(0.0f, 1.0f, 0.0f);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    public record BlurLevel(int radius, int textureId) {
    }

    public record Result(List<BlurLevel> levels, int passCount) {
    }
}
