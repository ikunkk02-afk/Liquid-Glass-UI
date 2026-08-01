/*
 * Adapted from ReGlass by ReStudio / RedxAx.
 * Original project: https://github.com/RedxAx/ReGlass
 * Licensed under the MIT License.
 * Backported and modified for Minecraft Fabric 1.21.1.
 */
package restudio.reglass.client.legacy1211;

import com.mojang.blaze3d.pipeline.RenderTarget;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import restudio.reglass.client.runtime.ReGlassAnim;

public final class Legacy1211CompositePass implements AutoCloseable {
    private final Legacy1211FramebufferManager framebuffers = new Legacy1211FramebufferManager();
    private final Legacy1211PipelineBridge pipeline = new Legacy1211PipelineBridge();
    private final Legacy1211UniformUploader uniforms = new Legacy1211UniformUploader();
    private final Legacy1211BlurRuntime blur = new Legacy1211BlurRuntime(framebuffers, pipeline, uniforms);

    public boolean render(Legacy1211WidgetCollector collector, double dtSeconds, boolean screenWantsBlur) {
        ShaderInstance shader = Legacy1211ShaderManager.liquidGlassShader();
        if (shader == null || collector.widgets().isEmpty()) return false;

        Minecraft minecraft = Minecraft.getInstance();
        RenderTarget main = minecraft.getMainRenderTarget();
        float scale = (float) minecraft.getWindow().getGuiScale();
        double[] cursorX = new double[1];
        double[] cursorY = new double[1];
        GLFW.glfwGetCursorPos(minecraft.getWindow().getWindow(), cursorX, cursorY);

        try (Legacy1211RenderStateGuard ignored = new Legacy1211RenderStateGuard()) {
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            framebuffers.captureMain(main);
            uniforms.uploadShared(main.width, main.height, (float) GLFW.glfwGetTime(), (float) cursorX[0] * scale, main.height - (float) cursorY[0] * scale, screenWantsBlur);
            List<Integer> radii = uniforms.uploadWidgets(collector.widgets(), main.width, main.height, scale, dtSeconds);
            blur.run(radii);

            main.bindWrite(true);
            GL11.glViewport(0, 0, main.width, main.height);
            shader.setSampler("Sampler0", framebuffers.snapshot().getColorTextureId());
            for (int i = 0; i < 5; i++) {
                int radius = radii.get(Math.min(i, radii.size() - 1));
                shader.setSampler("Sampler" + (i + 1), blur.result(radius).getColorTextureId());
            }
            uniforms.bindCompositeBlocks(shader.getId());
            pipeline.drawFullscreen(shader);
        }
        return true;
    }

    @Override
    public void close() {
        blur.close();
        uniforms.close();
        pipeline.close();
        framebuffers.close();
    }
}
