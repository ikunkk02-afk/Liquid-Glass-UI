package io.github.ikunkk02afk.liquidglassui.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.ikunkk02afk.liquidglassui.LiquidGlassUIClient;
import io.github.ikunkk02afk.liquidglassui.config.GlassDebugView;
import io.github.ikunkk02afk.liquidglassui.config.LiquidGlassConfigData;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import org.slf4j.Logger;

import java.util.Objects;

/**
 * The only class that speaks Minecraft 1.21.1 GUI, Blaze3D framebuffer, ShaderInstance, or raw OpenGL.
 */
public final class LegacyGlassRenderBackend implements GlassRenderBackend {
    private final Logger logger;
    private final GlassFailureLatch advancedFailure;
    private final GlassRenderFallback fallback = new GlassRenderFallback();
    private final GlassFramebufferManager framebufferState = new GlassFramebufferManager();
    private final GlassShaderManager shaderState = new GlassShaderManager();

    private ShaderInstance blurShader;
    private ShaderInstance maskShader;
    private ShaderInstance surfaceShader;

    private TextureTarget backgroundCaptureTarget;
    private TextureTarget blurTargetA;
    private TextureTarget blurTargetB;
    private TextureTarget finalBlurTarget;

    private GlassFrameContext frame;
    private GuiGraphics graphics;
    private AbstractButton button;
    private LiquidGlassConfigData config;
    private long capturedFrame = Long.MIN_VALUE;
    private boolean fallbackFailed;
    private String lastResourceLogKey = "";
    private String lastCompositeLogKey = "";

    public LegacyGlassRenderBackend(Logger logger) {
        this.logger = Objects.requireNonNull(logger, "logger");
        this.advancedFailure = new GlassFailureLatch((message, error) -> logger.error(
                "Liquid Glass UI rendering failed; safe mode is active for this run: {}", message, error));
    }

    public void registerShaders() {
        CoreShaderRegistrationCallback.EVENT.register(context -> {
            try {
                context.register(id("glass_blur"), DefaultVertexFormat.POSITION, shader -> {
                    blurShader = shader;
                    onResourceReload();
                });
                context.register(id("glass_mask"), DefaultVertexFormat.POSITION, shader -> {
                    maskShader = shader;
                    onResourceReload();
                });
                context.register(id("glass_surface"), DefaultVertexFormat.POSITION, shader -> {
                    surfaceShader = shader;
                    onResourceReload();
                });
            } catch (Exception exception) {
                advancedFailure.trip("custom shader registration", exception);
            }
        });
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(LiquidGlassUIClient.MOD_ID, path);
    }

    @Override
    public void beginFrame(GlassFrameContext context) {
        frame = Objects.requireNonNull(context, "context");
        graphics = null;
        button = null;
        config = null;
    }

    /** Called from Screen.render HEAD, after the screen background and before any widget or text. */
    public void captureBackground(GuiGraphics graphics, LiquidGlassConfigData config) {
        if (frame == null || capturedFrame == frame.frameId() || advancedFailure.failed()) return;
        if (config.performance.debugView == GlassDebugView.SOLID_MASK) return;
        if (surfaceShader == null || blurShader == null) {
            advancedFailure.trip("required backdrop shaders are not loaded", null);
            return;
        }

        this.graphics = graphics;
        this.config = config;
        RenderStateSnapshot state = RenderStateSnapshot.capture();
        try {
            graphics.flush();
            ensureTargets();
            captureMainColor();
            if (needsBlur(config.performance.debugView)) {
                downsampleCapture();
                runBlurPasses();
            } else {
                finalBlurTarget = null;
            }
            capturedFrame = frame.frameId();
            logCompositeInputs();
        } catch (Throwable throwable) {
            advancedFailure.trip("background capture, downsample, or blur", throwable);
        } finally {
            state.restore();
            restoreMainTarget();
        }
    }

    public boolean renderButton(AbstractButton button, GuiGraphics graphics, GlassSurface surface,
                                LiquidGlassConfigData config) {
        this.button = button;
        this.graphics = graphics;
        this.config = config;
        return render(surface);
    }

    @Override
    public boolean render(GlassSurface surface) {
        if (frame == null || graphics == null || button == null || config == null) return false;

        if (!advancedFailure.failed()) {
            try {
                if (config.performance.debugView == GlassDebugView.SOLID_MASK) {
                    renderSolidMask(surface);
                } else {
                    if (capturedFrame != frame.frameId()) {
                        throw new IllegalStateException("Backdrop was not captured before widget rendering");
                    }
                    renderSampledSurface(surface);
                }
                renderButtonContents();
                return true;
            } catch (Throwable throwable) {
                advancedFailure.trip("glass mask or final local composition", throwable);
                restoreMainTarget();
            }
        }
        return renderFallback(surface);
    }

    private void ensureTargets() {
        if (!framebufferState.needsRebuild(frame, shaderState.generation())
                && backgroundCaptureTarget != null && blurTargetA != null && blurTargetB != null) return;

        destroyTargets();
        int captureWidth = Math.max(1, frame.framebufferWidth());
        int captureHeight = Math.max(1, frame.framebufferHeight());
        int blurWidth = Math.max(1, Math.round(captureWidth * frame.quality().bufferScale()));
        int blurHeight = Math.max(1, Math.round(captureHeight * frame.quality().bufferScale()));

        backgroundCaptureTarget = createTarget(captureWidth, captureHeight);
        blurTargetA = createTarget(blurWidth, blurHeight);
        blurTargetB = createTarget(blurWidth, blurHeight);
        framebufferState.markBuilt(frame, shaderState.generation());

        GlassCoordinateMapper mapper = new GlassCoordinateMapper(frame.screenWidth(), frame.screenHeight(),
                frame.framebufferWidth(), frame.framebufferHeight());
        String key = captureWidth + "x" + captureHeight + '/' + blurWidth + "x" + blurHeight + '/'
                + frame.guiScale() + '/' + frame.quality() + '/' + backgroundCaptureTarget.getColorTextureId()
                + '/' + blurTargetA.getColorTextureId() + '/' + blurTargetB.getColorTextureId();
        if (!key.equals(lastResourceLogKey)) {
            lastResourceLogKey = key;
            logger.info("Liquid Glass UI render targets rebuilt: window={}x{}, gui={}x{}, guiScale={}, "
                            + "mainTarget={}x{}, captureTarget={}x{} texture={}, blurA={}x{} texture={}, "
                            + "blurB={}x{} texture={}, quality={}, guiToFramebufferScale={}x{}",
                    frame.framebufferWidth(), frame.framebufferHeight(), frame.screenWidth(), frame.screenHeight(),
                    frame.guiScale(), Minecraft.getInstance().getMainRenderTarget().viewWidth,
                    Minecraft.getInstance().getMainRenderTarget().viewHeight,
                    backgroundCaptureTarget.viewWidth, backgroundCaptureTarget.viewHeight,
                    backgroundCaptureTarget.getColorTextureId(), blurTargetA.viewWidth, blurTargetA.viewHeight,
                    blurTargetA.getColorTextureId(), blurTargetB.viewWidth, blurTargetB.viewHeight,
                    blurTargetB.getColorTextureId(), config.performance.preset, mapper.scaleX(), mapper.scaleY());
        }
    }

    private static boolean needsBlur(GlassDebugView view) {
        return view == GlassDebugView.OFF || view == GlassDebugView.RAW_BLUR
                || view == GlassDebugView.FINAL_COMPOSITE;
    }

    private TextureTarget createTarget(int width, int height) {
        TextureTarget target = new TextureTarget(width, height, false, Minecraft.ON_OSX);
        configureTexture(target.getColorTextureId());
        return target;
    }

    private static void configureTexture(int textureId) {
        int previousActive = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        RenderSystem.activeTexture(GL13.GL_TEXTURE0);
        int previousTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        RenderSystem.bindTextureForSetup(textureId);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_BASE_LEVEL, 0);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, 0);
        RenderSystem.bindTextureForSetup(previousTexture);
        RenderSystem.activeTexture(previousActive);
    }

    private void captureMainColor() {
        RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, backgroundCaptureTarget.frameBufferId);
        GL30.glBlitFramebuffer(0, 0, main.viewWidth, main.viewHeight,
                0, 0, backgroundCaptureTarget.viewWidth, backgroundCaptureTarget.viewHeight,
                GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
    }

    private void downsampleCapture() {
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, backgroundCaptureTarget.frameBufferId);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, blurTargetA.frameBufferId);
        GL30.glBlitFramebuffer(0, 0, backgroundCaptureTarget.viewWidth, backgroundCaptureTarget.viewHeight,
                0, 0, blurTargetA.viewWidth, blurTargetA.viewHeight,
                GL11.GL_COLOR_BUFFER_BIT, GL11.GL_LINEAR);
    }

    private void runBlurPasses() {
        TextureTarget input = blurTargetA;
        TextureTarget output = blurTargetB;
        float normalizedRadius = Math.max(0.35f,
                config.optics.blurRadius * config.optics.blurIntensity * frame.quality().bufferScale());

        for (int pass = 0; pass < frame.quality().blurPasses(); pass++) {
            output.bindWrite(true);
            RenderSystem.viewport(0, 0, output.viewWidth, output.viewHeight);
            bindSampler0(blurShader, input.getColorTextureId());
            set(blurShader, "TexelSize", 1.0f / input.viewWidth, 1.0f / input.viewHeight);
            set(blurShader, "Direction", pass % 2 == 0 ? 1.0f : 0.0f, pass % 2 == 0 ? 0.0f : 1.0f);
            set(blurShader, "Radius", normalizedRadius);
            RenderSystem.setShader(() -> blurShader);
            drawNormalizedQuad();

            TextureTarget swap = input;
            input = output;
            output = swap;
        }
        finalBlurTarget = input;
    }

    private void renderSolidMask(GlassSurface surface) {
        if (maskShader == null) throw new IllegalStateException("Solid-mask shader is not loaded");
        RenderStateSnapshot state = RenderStateSnapshot.capture();
        try {
            set(maskShader, "GuiSize", frame.screenWidth(), frame.screenHeight());
            set(maskShader, "Rect", surface.bounds().x(), surface.bounds().y(), surface.bounds().width(), surface.bounds().height());
            set(maskShader, "CornerRadius", surface.cornerRadius());
            float[] tint = parseColor(config.appearance.mainColor);
            set(maskShader, "Tint", tint[0], tint[1], tint[2]);
            set(maskShader, "Alpha", 0.14f);
            set(maskShader, "EdgeAlpha", 0.26f);
            prepareGuiComposition(maskShader);
            drawSurfaceBounds(surface.bounds(), surface.bounds());
        } finally {
            state.restore();
            restoreMainTarget();
        }
    }

    private void renderSampledSurface(GlassSurface surface) {
        TextureTarget input = switch (config.performance.debugView) {
            case RAW_CAPTURE, UV_GRID -> backgroundCaptureTarget;
            case RAW_BLUR, OFF, FINAL_COMPOSITE -> finalBlurTarget;
            case SOLID_MASK -> null;
        };
        if (input == null) throw new IllegalStateException("Selected composition input texture is unavailable");

        RenderStateSnapshot state = RenderStateSnapshot.capture();
        try {
            bindSampler0(surfaceShader, input.getColorTextureId());
            GlassCoordinateMapper mapper = new GlassCoordinateMapper(frame.screenWidth(), frame.screenHeight(),
                    frame.framebufferWidth(), frame.framebufferHeight());
            set(surfaceShader, "GuiSize", frame.screenWidth(), frame.screenHeight());
            set(surfaceShader, "FramebufferSize", frame.framebufferWidth(), frame.framebufferHeight());
            set(surfaceShader, "GuiToFramebufferScale", mapper.scaleX(), mapper.scaleY());
            set(surfaceShader, "SampleTextureSize", input.viewWidth, input.viewHeight);
            set(surfaceShader, "Rect", surface.bounds().x(), surface.bounds().y(), surface.bounds().width(), surface.bounds().height());
            set(surfaceShader, "PreviousRect", surface.previousBounds().x(), surface.previousBounds().y(),
                    surface.previousBounds().width(), surface.previousBounds().height());
            set(surfaceShader, "CornerRadius", surface.cornerRadius());
            set(surfaceShader, "Merge", isFinalView() ? surface.merge() : 0.0f);
            setInteger(surfaceShader, "DebugMode", debugMode(config.performance.debugView));
            set(surfaceShader, "Opacity", config.appearance.opacity * surface.opacity());
            set(surfaceShader, "TintIntensity", config.appearance.tintIntensity);
            set(surfaceShader, "EdgeWidth", config.appearance.edgeWidth);
            set(surfaceShader, "EdgeHighlight", config.appearance.edgeHighlightIntensity + surface.hover() * 0.18f);
            set(surfaceShader, "InnerShadow", config.appearance.innerShadowIntensity);
            set(surfaceShader, "MouseHighlight", config.optics.mouseHighlightIntensity * surface.hover());
            set(surfaceShader, "MouseRange", config.optics.mouseHighlightRange);
            set(surfaceShader, "Refraction", isFinalView() && frame.quality().dynamicRefraction()
                    ? config.optics.refractionIntensity : 0.0f);
            set(surfaceShader, "RefractionRange", config.optics.edgeRefractionRange);
            setInteger(surfaceShader, "SampleCount", isFinalView() ? frame.quality().sampleCount() : 1);
            set(surfaceShader, "Noise", isFinalView() ? config.optics.surfaceNoiseIntensity : 0.0f);
            set(surfaceShader, "AdaptBrightness", config.appearance.adaptToBackgroundBrightness ? 1.0f : 0.0f);
            set(surfaceShader, "HighlightPosition", surface.highlightX(), surface.highlightY());
            float[] tint = parseColor(config.appearance.mainColor);
            set(surfaceShader, "Tint", tint[0], tint[1], tint[2]);

            prepareGuiComposition(surfaceShader);
            drawSurfaceBounds(surface.bounds(), isFinalView() ? surface.previousBounds() : surface.bounds());
        } finally {
            state.restore();
            restoreMainTarget();
        }
    }

    private boolean isFinalView() {
        return config.performance.debugView == GlassDebugView.OFF
                || config.performance.debugView == GlassDebugView.FINAL_COMPOSITE;
    }

    private static int debugMode(GlassDebugView view) {
        return switch (view) {
            case RAW_CAPTURE -> 1;
            case RAW_BLUR -> 2;
            case UV_GRID -> 3;
            case OFF, FINAL_COMPOSITE -> 4;
            case SOLID_MASK -> 0;
        };
    }

    private static void prepareGuiComposition(ShaderInstance shader) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableScissor();
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.setShader(() -> shader);
    }

    private static void bindSampler0(ShaderInstance shader, int textureId) {
        RenderSystem.activeTexture(GL13.GL_TEXTURE0);
        RenderSystem.setShaderTexture(0, textureId);
        RenderSystem.bindTexture(textureId);
        shader.setSampler("Sampler0", textureId);
    }

    private void logCompositeInputs() {
        int mainTexture = Minecraft.getInstance().getMainRenderTarget().getColorTextureId();
        int captureTexture = backgroundCaptureTarget == null ? -1 : backgroundCaptureTarget.getColorTextureId();
        int blurInput = blurTargetA == null ? -1 : blurTargetA.getColorTextureId();
        int blurOutput = finalBlurTarget == null ? -1 : finalBlurTarget.getColorTextureId();
        int composite = switch (config.performance.debugView) {
            case RAW_CAPTURE, UV_GRID -> captureTexture;
            case RAW_BLUR, OFF, FINAL_COMPOSITE -> blurOutput;
            case SOLID_MASK -> -1;
        };
        String key = frame.framebufferWidth() + "x" + frame.framebufferHeight() + '/' + config.performance.preset
                + '/' + config.performance.debugView + '/' + mainTexture + '/' + captureTexture + '/' + blurInput
                + '/' + blurOutput + '/' + composite;
        if (!key.equals(lastCompositeLogKey)) {
            lastCompositeLogKey = key;
            logger.info("Liquid Glass UI render inputs: quality={}, debugView={}, mainColorTexture={}, "
                            + "backgroundCaptureTexture={}, blurInputTexture={}, blurOutputTexture={}, compositeTexture={}",
                    config.performance.preset, config.performance.debugView, mainTexture, captureTexture,
                    blurInput, blurOutput, composite);
        }
    }

    private boolean renderFallback(GlassSurface surface) {
        if (fallbackFailed) return false;
        try {
            GlassRectangle bounds = surface.bounds();
            int left = Math.round(bounds.x());
            int top = Math.round(bounds.y());
            int right = Math.round(bounds.x() + bounds.width());
            int bottom = Math.round(bounds.y() + bounds.height());
            int radius = Math.max(2, Math.min(Math.round(surface.cornerRadius()), Math.max(2, (bottom - top) / 2)));
            int panel = fallback.panelColor(surface.active(), Math.min(0.45f, config.appearance.opacity) * surface.opacity());
            int edge = fallback.edgeColor(surface.active(), surface.hover());
            graphics.fill(left + radius, top, right - radius, bottom, panel);
            graphics.fill(left, top + radius, right, bottom - radius, panel);
            graphics.fill(left + 2, top + 1, right - 2, top + 2, edge);
            graphics.fill(left + radius, bottom - 2, right - radius, bottom - 1, 0x18000000);
            renderButtonContents();
            return true;
        } catch (Throwable throwable) {
            fallbackFailed = true;
            logger.error("Liquid Glass UI safe-mode renderer failed; restoring vanilla widget rendering", throwable);
            return false;
        }
    }

    private void renderButtonContents() {
        int textColor = button.active ? 0xFFFFFFFF : 0xFFA0A0A0;
        button.renderString(graphics, Minecraft.getInstance().font, textColor);
    }

    private static void drawNormalizedQuad() {
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        buffer.addVertex(0, 0, 0);
        buffer.addVertex(0, 1, 0);
        buffer.addVertex(1, 1, 0);
        buffer.addVertex(1, 0, 0);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    private static void drawSurfaceBounds(GlassRectangle bounds, GlassRectangle previous) {
        float left = Math.min(bounds.x(), previous.x()) - 3.0f;
        float top = Math.min(bounds.y(), previous.y()) - 3.0f;
        float right = Math.max(bounds.x() + bounds.width(), previous.x() + previous.width()) + 3.0f;
        float bottom = Math.max(bounds.y() + bounds.height(), previous.y() + previous.height()) + 3.0f;
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        buffer.addVertex(left, top, 0);
        buffer.addVertex(left, bottom, 0);
        buffer.addVertex(right, bottom, 0);
        buffer.addVertex(right, top, 0);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    private static void set(ShaderInstance shader, String name, float value) {
        Uniform uniform = shader.getUniform(name);
        if (uniform != null) uniform.set(value);
    }

    private static void set(ShaderInstance shader, String name, float first, float second) {
        Uniform uniform = shader.getUniform(name);
        if (uniform != null) uniform.set(first, second);
    }

    private static void set(ShaderInstance shader, String name, float first, float second, float third) {
        Uniform uniform = shader.getUniform(name);
        if (uniform != null) uniform.set(first, second, third);
    }

    private static void set(ShaderInstance shader, String name, float first, float second, float third, float fourth) {
        Uniform uniform = shader.getUniform(name);
        if (uniform != null) uniform.set(first, second, third, fourth);
    }

    private static void setInteger(ShaderInstance shader, String name, int value) {
        Uniform uniform = shader.getUniform(name);
        if (uniform != null) uniform.set(value);
    }

    private static float[] parseColor(String hex) {
        int rgb = Integer.parseInt(hex.substring(1), 16);
        return new float[]{((rgb >> 16) & 255) / 255.0f, ((rgb >> 8) & 255) / 255.0f, (rgb & 255) / 255.0f};
    }

    private static void restoreMainTarget() {
        RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
        main.bindWrite(true);
        RenderSystem.viewport(0, 0, main.viewWidth, main.viewHeight);
        RenderSystem.activeTexture(GL13.GL_TEXTURE0);
        RenderSystem.colorMask(true, true, true, true);
    }

    @Override
    public void endFrame() {
        graphics = null;
        button = null;
        config = null;
    }

    @Override
    public void onResourceReload() {
        shaderState.reloaded();
        framebufferState.invalidate();
        capturedFrame = Long.MIN_VALUE;
        lastResourceLogKey = "";
        lastCompositeLogKey = "";
        destroyTargets();
    }

    @Override
    public GlassBackendStatus status() {
        return advancedFailure.failed() ? GlassBackendStatus.degraded(advancedFailure.reason()) : GlassBackendStatus.ready();
    }

    @Override
    public void close() {
        destroyTargets();
        frame = null;
    }

    private void destroyTargets() {
        if (backgroundCaptureTarget != null) backgroundCaptureTarget.destroyBuffers();
        if (blurTargetA != null) blurTargetA.destroyBuffers();
        if (blurTargetB != null) blurTargetB.destroyBuffers();
        backgroundCaptureTarget = null;
        blurTargetA = null;
        blurTargetB = null;
        finalBlurTarget = null;
    }

    private record RenderStateSnapshot(int readFramebuffer, int drawFramebuffer, int viewportX, int viewportY,
                                       int viewportWidth, int viewportHeight, int activeTexture, int texture0,
                                       ShaderInstance shader, boolean blend, boolean depth, boolean scissor) {
        private static RenderStateSnapshot capture() {
            int[] viewport = new int[4];
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
            int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
            RenderSystem.activeTexture(GL13.GL_TEXTURE0);
            int texture0 = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            RenderSystem.activeTexture(activeTexture);
            return new RenderStateSnapshot(GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING),
                    GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING), viewport[0], viewport[1], viewport[2],
                    viewport[3], activeTexture, texture0, RenderSystem.getShader(),
                    GL11.glIsEnabled(GL11.GL_BLEND), GL11.glIsEnabled(GL11.GL_DEPTH_TEST),
                    GL11.glIsEnabled(GL11.GL_SCISSOR_TEST));
        }

        private void restore() {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFramebuffer);
            RenderSystem.viewport(viewportX, viewportY, viewportWidth, viewportHeight);
            RenderSystem.activeTexture(GL13.GL_TEXTURE0);
            RenderSystem.setShaderTexture(0, texture0);
            RenderSystem.bindTexture(texture0);
            RenderSystem.activeTexture(activeTexture);
            if (shader != null) RenderSystem.setShader(() -> shader);
            setEnabled(GL11.GL_BLEND, blend);
            setEnabled(GL11.GL_DEPTH_TEST, depth);
            setEnabled(GL11.GL_SCISSOR_TEST, scissor);
            RenderSystem.colorMask(true, true, true, true);
            RenderSystem.setShaderColor(1, 1, 1, 1);
        }

        private static void setEnabled(int capability, boolean enabled) {
            if (enabled) GL11.glEnable(capability);
            else GL11.glDisable(capability);
        }
    }
}
