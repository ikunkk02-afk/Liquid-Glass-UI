/*
 * Adapted from ReGlass by ReStudio / RedxAx.
 * Original project: https://github.com/RedxAx/ReGlass
 * Licensed under the MIT License.
 * Backported and modified for Minecraft Fabric 1.21.1.
 */
package restudio.reglass.client.legacy1211;

import com.mojang.blaze3d.pipeline.RenderTarget;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.ShaderInstance;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL31;

public final class Legacy1211BlurRuntime implements AutoCloseable {
    public static final int MAX_RADIUS = 64;
    private static final int CONFIG_SIZE = 16 + (MAX_RADIUS + 1) * 16;
    private static final int CONFIG_BINDING = 4;

    private final Legacy1211FramebufferManager framebuffers;
    private final Legacy1211PipelineBridge pipeline;
    private final Legacy1211UniformUploader uniforms;
    private final Map<Integer, RenderTarget> results = new HashMap<>();
    private int configUboX;
    private int configUboY;

    public Legacy1211BlurRuntime(Legacy1211FramebufferManager framebuffers, Legacy1211PipelineBridge pipeline, Legacy1211UniformUploader uniforms) {
        this.framebuffers = framebuffers;
        this.pipeline = pipeline;
        this.uniforms = uniforms;
    }

    public void run(List<Integer> radii) {
        results.clear();
        ShaderInstance shader = Legacy1211ShaderManager.blurShader();
        if (shader == null) return;
        ensureBuffers();
        for (int radius : radii) {
            int clamped = Math.max(0, Math.min(MAX_RADIUS, radius));
            if (clamped == 0) {
                results.put(radius, framebuffers.snapshot());
                continue;
            }
            uploadConfig(configUboX, 1.0f, 0.0f, clamped);
            uploadConfig(configUboY, 0.0f, 1.0f, clamped);
            renderPass(shader, framebuffers.snapshot(), framebuffers.blurTemp(), configUboX);
            RenderTarget output = framebuffers.blurred(radius);
            renderPass(shader, framebuffers.blurTemp(), output, configUboY);
            results.put(radius, output);
        }
    }

    public RenderTarget result(int radius) {
        return results.getOrDefault(radius, framebuffers.snapshot());
    }

    public static float[] gaussian(int radius) {
        int clamped = Math.max(0, Math.min(MAX_RADIUS, radius));
        if (clamped == 0) return new float[]{1.0f};
        float sigma = clamped / 3.0f;
        float[] weights = new float[clamped + 1];
        float sum = 0.0f;
        for (int i = 0; i <= clamped; i++) {
            float weight = (float) Math.exp(-0.5f * i * i / (sigma * sigma));
            weights[i] = weight;
            sum += i == 0 ? weight : 2.0f * weight;
        }
        for (int i = 0; i <= clamped; i++) weights[i] /= sum;
        return weights;
    }

    private void renderPass(ShaderInstance shader, RenderTarget source, RenderTarget output, int configUbo) {
        output.bindWrite(true);
        GL11.glViewport(0, 0, output.width, output.height);
        shader.setSampler("DiffuseSampler", source.getColorTextureId());
        bindBlock(shader.getId(), "SamplerInfo", 0, uniforms.samplerInfoUbo());
        bindBlock(shader.getId(), "Config", CONFIG_BINDING, configUbo);
        pipeline.drawFullscreen(shader);
    }

    private void ensureBuffers() {
        if (configUboX != 0) return;
        configUboX = createUbo();
        configUboY = createUbo();
    }

    private static int createUbo() {
        int buffer = GL15.glGenBuffers();
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, buffer);
        GL15.glBufferData(GL31.GL_UNIFORM_BUFFER, CONFIG_SIZE, GL15.GL_DYNAMIC_DRAW);
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, 0);
        return buffer;
    }

    private static void uploadConfig(int buffer, float dx, float dy, int radius) {
        ByteBuffer data = ByteBuffer.allocateDirect(CONFIG_SIZE).order(ByteOrder.nativeOrder());
        data.putFloat(0, dx);
        data.putFloat(4, dy);
        data.putFloat(8, radius);
        float[] weights = gaussian(radius);
        for (int i = 0; i <= MAX_RADIUS; i++) data.putFloat(16 + i * 16, i <= radius ? weights[i] : 0.0f);
        data.position(0).limit(data.capacity());
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, buffer);
        GL15.glBufferSubData(GL31.GL_UNIFORM_BUFFER, 0, data);
    }

    private static void bindBlock(int program, String name, int binding, int buffer) {
        int index = GL31.glGetUniformBlockIndex(program, name);
        if (index != GL31.GL_INVALID_INDEX) {
            GL31.glUniformBlockBinding(program, index, binding);
            GL31.glBindBufferBase(GL31.GL_UNIFORM_BUFFER, binding, buffer);
        }
    }

    @Override
    public void close() {
        if (configUboX != 0) GL15.glDeleteBuffers(configUboX);
        if (configUboY != 0) GL15.glDeleteBuffers(configUboY);
        configUboX = configUboY = 0;
        results.clear();
    }
}
