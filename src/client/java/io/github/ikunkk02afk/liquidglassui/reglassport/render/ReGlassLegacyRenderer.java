package io.github.ikunkk02afk.liquidglassui.reglassport.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.slf4j.Logger;

import java.util.List;

/** Independent Minecraft 1.21.1 backend for the ReGlass port. */
public final class ReGlassLegacyRenderer implements AutoCloseable {
    private final Logger logger;
    private final ReGlassShaderManager shaders;
    private final ReGlassFramebufferManager framebuffers = new ReGlassFramebufferManager();
    private final ReGlassBlurRuntime blur = new ReGlassBlurRuntime();
    private final ReGlassCompositePass composite = new ReGlassCompositePass();
    private int screenWidth;
    private int screenHeight;
    private int framebufferWidth;
    private int framebufferHeight;
    private final ReGlassGenerationLatch failureLatch = new ReGlassGenerationLatch();
    private boolean captured;
    private Metrics metrics = new Metrics(0, 0, 0, 0);

    public ReGlassLegacyRenderer(Logger logger) {
        this.logger = logger;
        this.shaders = new ReGlassShaderManager(logger);
    }

    public void registerShaders() {
        shaders.register(() -> {
            framebuffers.invalidate();
            failureLatch.successfulReload();
        });
    }

    public void beginFrame(int screenWidth, int screenHeight) {
        RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.framebufferWidth = main.viewWidth;
        this.framebufferHeight = main.viewHeight;
        this.captured = false;
        this.metrics = new Metrics(0, 0, 0, 0);
    }

    public boolean captureBackdrop() {
        if (!available()) return false;
        try (ReGlassRenderStateGuard ignored = ReGlassRenderStateGuard.capture()) {
            framebuffers.ensure(framebufferWidth, framebufferHeight, shaders.generation());
            RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, framebuffers.capture().frameBufferId);
            GL30.glBlitFramebuffer(0, 0, main.viewWidth, main.viewHeight, 0, 0,
                    framebuffers.capture().viewWidth, framebuffers.capture().viewHeight,
                    GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
            captured = true;
            metrics = new Metrics(1, 0, 0, 0);
            return true;
        } catch (Throwable throwable) {
            fail("background capture", throwable);
            return false;
        }
    }

    public boolean composite(List<ReGlassUniformData> widgets, int debugMode) {
        if (!available() || !captured || widgets.isEmpty()) return false;
        try (ReGlassRenderStateGuard ignored = ReGlassRenderStateGuard.capture()) {
            int rawTexture = framebuffers.capture().getColorTextureId();
            ReGlassBlurRuntime.Result blurResult = blur.render(shaders.blur(), framebuffers, widgets, rawTexture);
            composite.draw(shaders.composite(), widgets, screenWidth, screenHeight,
                    framebufferWidth, framebufferHeight, rawTexture, blurResult.levels(), debugMode);
            metrics = new Metrics(metrics.captureCount(), blurResult.passCount(), 1, widgets.size());
            return true;
        } catch (Throwable throwable) {
            fail("component upload or composite", throwable);
            return false;
        } finally {
            framebuffers.restoreMainTarget();
        }
    }

    public Metrics metrics() { return metrics; }
    public boolean available() { return shaders.ready() && !failureLatch.failed(shaders.generation()); }

    private void fail(String stage, Throwable throwable) {
        if (failureLatch.fail(shaders.generation())) {
            logger.error("ReGlass Port disabled for resource generation {} after {} failure",
                    shaders.generation(), stage, throwable);
        }
    }

    @Override
    public void close() {
        composite.close();
        framebuffers.close();
    }

    public record Metrics(int captureCount, int blurPassCount, int compositeCount, int widgetCount) {
    }
}
