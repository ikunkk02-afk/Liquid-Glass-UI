package io.github.ikunkk02afk.liquidglassui.render.legacy;

import io.github.ikunkk02afk.liquidglassui.render.GlassBackendStatus;
import io.github.ikunkk02afk.liquidglassui.render.GlassFailureLatch;
import io.github.ikunkk02afk.liquidglassui.render.GlassFrameContext;
import io.github.ikunkk02afk.liquidglassui.render.GlassRenderBackend;
import io.github.ikunkk02afk.liquidglassui.render.frame.GlassFrameState;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;

import java.util.Objects;

public final class LegacyGlassRenderBackend implements GlassRenderBackend {
    private final Logger logger;
    private final GlassFailureLatch failure;
    private final LegacyGlassShaderManager shaders = new LegacyGlassShaderManager();
    private final LegacyFramebufferManager targets = new LegacyFramebufferManager();
    private final LegacyBlurProcessor blur = new LegacyBlurProcessor();
    private final LegacyCompositePass composite = new LegacyCompositePass();
    private GlassFrameContext frame;
    private long capturedFrame = Long.MIN_VALUE;
    private String resourceLogKey = "";
    private String inputLogKey = "";
    private long geometryLoggedFrame = Long.MIN_VALUE;

    public LegacyGlassRenderBackend(Logger logger) {
        this.logger = Objects.requireNonNull(logger, "logger");
        failure = new GlassFailureLatch((reason, throwable) -> logger.error(
                "Liquid Glass UI advanced renderer failed; safe mode is locked for this run: {}", reason, throwable));
    }

    public void registerShaders() {
        shaders.register(this::onResourceReload);
    }

    @Override
    public void beginFrame(GlassFrameContext context) {
        frame = Objects.requireNonNull(context, "context");
    }

    @Override
    public boolean captureBackdrop() {
        if (frame == null || capturedFrame == frame.frameId() || failure.failed()) return false;
        if (!shaders.ready()) {
            failure.trip("required blur/composite shaders are not loaded", null);
            return false;
        }
        try (LegacyRenderStateGuard ignored = LegacyRenderStateGuard.capture()) {
            boolean rebuilt = targets.ensure(frame, shaders.generation());
            blur.capture(targets);
            capturedFrame = frame.frameId();
            if (rebuilt) logResources();
            return true;
        } catch (Throwable throwable) {
            failure.trip("background capture or RenderTarget creation", throwable);
            return false;
        } finally {
            targets.restoreMainTarget();
        }
    }

    @Override
    public boolean composite(GlassFrameState state) {
        if (frame == null || state == null || state.frameId() != frame.frameId() || state.overflowed()
                || capturedFrame != frame.frameId() || failure.failed()) return false;
        try (LegacyRenderStateGuard ignored = LegacyRenderStateGuard.capture()) {
            if (geometryLoggedFrame == Long.MIN_VALUE && state.widgetCount() > 0) {
                var first = state.widget(0);
                geometryLoggedFrame = frame.frameId();
                logger.info("Liquid Glass UI first composite: frame={}, widgets={}, firstGuiRect=[{},{},{},{}], debug={}",
                        frame.frameId(), state.widgetCount(), first.x, first.y, first.width, first.height,
                        frame.debugView());
            }
            LegacyBlurProcessor.Result result = blur.blur(targets, shaders.blur(), frame);
            targets.restoreMainTarget();
            composite.draw(shaders.composite(), state, frame, targets.capture(), result.light(), result.full());
            logInputs(result);
            return true;
        } catch (Throwable throwable) {
            failure.trip("shared blur, data upload, or final composite", throwable);
            return false;
        } finally {
            targets.restoreMainTarget();
        }
    }

    private void logResources() {
        String key = frame.framebufferWidth() + "x" + frame.framebufferHeight() + '/' + targets.blurWidth() + "x"
                + targets.blurHeight() + '/' + frame.guiScale() + '/' + frame.quality();
        if (key.equals(resourceLogKey)) return;
        resourceLogKey = key;
        var window = Minecraft.getInstance().getWindow();
        logger.info("Liquid Glass UI targets: window={}x{}, gui={}x{}, guiScale={}, main={}x{}, capture={}x{} tex={}, "
                        + "blur={}x{}, quality={}, passes={}, guiScaleXY={}x{}",
                window.getWidth(), window.getHeight(), frame.screenWidth(), frame.screenHeight(), frame.guiScale(),
                Minecraft.getInstance().getMainRenderTarget().viewWidth,
                Minecraft.getInstance().getMainRenderTarget().viewHeight,
                targets.capture().viewWidth, targets.capture().viewHeight, targets.capture().getColorTextureId(),
                targets.blurWidth(), targets.blurHeight(), frame.quality(), frame.quality().blurPasses(),
                frame.framebufferWidth() / (float) Math.max(1, frame.screenWidth()),
                frame.framebufferHeight() / (float) Math.max(1, frame.screenHeight()));
    }

    private void logInputs(LegacyBlurProcessor.Result result) {
        String key = frame.frameId() + "/" + targets.capture().getColorTextureId() + '/'
                + result.light().getColorTextureId() + '/' + result.full().getColorTextureId() + '/'
                + composite.dataTexture();
        if (key.equals(inputLogKey)) return;
        inputLogKey = key;
        logger.debug("Liquid Glass UI frame {} samplers: raw(unit0)={}, lightBlur(unit1)={}, fullBlur(unit2)={}, "
                        + "widgetData(unit3)={}", frame.frameId(), targets.capture().getColorTextureId(),
                result.light().getColorTextureId(), result.full().getColorTextureId(), composite.dataTexture());
    }

    @Override
    public void endFrame() {
        frame = null;
    }

    @Override
    public void onResourceReload() {
        targets.invalidate();
        capturedFrame = Long.MIN_VALUE;
        resourceLogKey = "";
        inputLogKey = "";
    }

    @Override
    public GlassBackendStatus status() {
        return failure.failed() ? GlassBackendStatus.degraded(failure.reason()) : GlassBackendStatus.ready();
    }

    @Override
    public void close() {
        targets.close();
        composite.close();
        frame = null;
    }
}
