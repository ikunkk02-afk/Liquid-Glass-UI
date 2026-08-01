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
import io.github.ikunkk02afk.liquidglassui.config.LiquidGlassConfigData;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL30;
import org.slf4j.Logger;

import java.util.Objects;

public final class LegacyGlassRenderBackend implements GlassRenderBackend {
    private final Logger logger;
    private final GlassFailureLatch advancedFailure;
    private final GlassRenderFallback fallback = new GlassRenderFallback();
    private final GlassFramebufferManager framebufferState = new GlassFramebufferManager();
    private final GlassShaderManager shaderState = new GlassShaderManager();

    private ShaderInstance blurShader;
    private ShaderInstance surfaceShader;
    private TextureTarget ping;
    private TextureTarget pong;
    private TextureTarget blurred;
    private GlassFrameContext frame;
    private GuiGraphics graphics;
    private AbstractButton button;
    private LiquidGlassConfigData config;
    private long preparedFrame = Long.MIN_VALUE;
    private boolean fallbackFailed;

    public LegacyGlassRenderBackend(Logger logger) {
        this.logger = Objects.requireNonNull(logger, "logger");
        this.advancedFailure = new GlassFailureLatch((message, error) -> logger.error(
                "Liquid Glass UI advanced rendering failed; safe mode is active for this run: {}", message, error));
    }

    public void registerShaders() {
        CoreShaderRegistrationCallback.EVENT.register(context -> {
            try {
                context.register(ResourceLocation.fromNamespaceAndPath(LiquidGlassUIClient.MOD_ID, "glass_blur"),
                        DefaultVertexFormat.POSITION, shader -> {
                            blurShader = shader;
                            onResourceReload();
                        });
                context.register(ResourceLocation.fromNamespaceAndPath(LiquidGlassUIClient.MOD_ID, "glass_surface"),
                        DefaultVertexFormat.POSITION, shader -> {
                            surfaceShader = shader;
                            onResourceReload();
                        });
            } catch (Exception exception) {
                advancedFailure.trip("custom shader registration", exception);
            }
        });
    }

    @Override
    public void beginFrame(GlassFrameContext context) {
        frame = Objects.requireNonNull(context, "context");
        graphics = null;
        button = null;
        config = null;
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
        if (!advancedFailure.failed() && blurShader != null && surfaceShader != null) {
            try {
                prepareSharedBackground();
                renderAdvancedSurface(surface);
                renderButtonContents();
                return true;
            } catch (Throwable throwable) {
                advancedFailure.trip("framebuffer or glass shader pass", throwable);
                restoreMainTarget();
            }
        }
        return renderFallback(surface);
    }

    private void prepareSharedBackground() {
        if (preparedFrame == frame.frameId()) return;
        Minecraft minecraft = Minecraft.getInstance();
        RenderTarget main = minecraft.getMainRenderTarget();
        graphics.flush();
        ensureTargets();

        try {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, ping.frameBufferId);
            GL30.glBlitFramebuffer(0, 0, main.viewWidth, main.viewHeight, 0, 0, ping.viewWidth, ping.viewHeight,
                    GL30.GL_COLOR_BUFFER_BIT, GL30.GL_LINEAR);

            TextureTarget source = ping;
            TextureTarget target = pong;
            for (int pass = 0; pass < frame.quality().blurPasses(); pass++) {
                target.bindWrite(true);
                RenderSystem.viewport(0, 0, target.viewWidth, target.viewHeight);
                blurShader.setSampler("Sampler0", source.getColorTextureId());
                set(blurShader, "TexelSize", 1.0f / source.viewWidth, 1.0f / source.viewHeight);
                set(blurShader, "Direction", pass % 2 == 0 ? 1.0f : 0.0f, pass % 2 == 0 ? 0.0f : 1.0f);
                set(blurShader, "Radius", Math.max(0.5f, config.optics.blurRadius * config.optics.blurIntensity));
                RenderSystem.setShader(() -> blurShader);
                drawNormalizedQuad();
                TextureTarget swap = source;
                source = target;
                target = swap;
            }
            blurred = source;
            preparedFrame = frame.frameId();
        } finally {
            restoreMainTarget();
        }
    }

    private void ensureTargets() {
        if (!framebufferState.needsRebuild(frame, shaderState.generation()) && ping != null && pong != null) return;
        destroyTargets();
        int width = Math.max(1, Math.round(frame.framebufferWidth() * frame.quality().bufferScale()));
        int height = Math.max(1, Math.round(frame.framebufferHeight() * frame.quality().bufferScale()));
        ping = new TextureTarget(width, height, false, Minecraft.ON_OSX);
        pong = new TextureTarget(width, height, false, Minecraft.ON_OSX);
        ping.setFilterMode(GL30.GL_LINEAR);
        pong.setFilterMode(GL30.GL_LINEAR);
        framebufferState.markBuilt(frame, shaderState.generation());
    }

    private void renderAdvancedSurface(GlassSurface surface) {
        GlassRectangle bounds = surface.bounds();
        GlassRectangle previous = surface.previousBounds();
        float left = Math.min(bounds.x(), previous.x()) - 3.0f;
        float top = Math.min(bounds.y(), previous.y()) - 3.0f;
        float right = Math.max(bounds.x() + bounds.width(), previous.x() + previous.width()) + 3.0f;
        float bottom = Math.max(bounds.y() + bounds.height(), previous.y() + previous.height()) + 3.0f;

        surfaceShader.setSampler("Sampler0", blurred.getColorTextureId());
        set(surfaceShader, "GuiSize", frame.screenWidth(), frame.screenHeight());
        set(surfaceShader, "Rect", bounds.x(), bounds.y(), bounds.width(), bounds.height());
        set(surfaceShader, "PreviousRect", previous.x(), previous.y(), previous.width(), previous.height());
        set(surfaceShader, "CornerRadius", surface.cornerRadius());
        set(surfaceShader, "Merge", surface.merge());
        set(surfaceShader, "Opacity", config.appearance.opacity * surface.opacity());
        set(surfaceShader, "TintIntensity", config.appearance.tintIntensity);
        set(surfaceShader, "EdgeWidth", config.appearance.edgeWidth);
        set(surfaceShader, "EdgeHighlight", config.appearance.edgeHighlightIntensity + surface.hover() * 0.18f);
        set(surfaceShader, "InnerShadow", config.appearance.innerShadowIntensity);
        set(surfaceShader, "MouseHighlight", config.optics.mouseHighlightIntensity * surface.hover());
        set(surfaceShader, "MouseRange", config.optics.mouseHighlightRange);
        set(surfaceShader, "Refraction", frame.quality().dynamicRefraction() ? config.optics.refractionIntensity : 0.0f);
        set(surfaceShader, "RefractionRange", config.optics.edgeRefractionRange);
        setInteger(surfaceShader, "SampleCount", frame.quality().sampleCount());
        set(surfaceShader, "Noise", config.optics.surfaceNoiseIntensity);
        set(surfaceShader, "AdaptBrightness", config.appearance.adaptToBackgroundBrightness ? 1.0f : 0.0f);
        set(surfaceShader, "HighlightPosition", surface.highlightX(), surface.highlightY());
        float[] tint = parseColor(config.appearance.mainColor);
        set(surfaceShader, "Tint", tint[0], tint[1], tint[2]);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(() -> surfaceShader);
        drawPixelQuad(left, top, right, bottom);
        RenderSystem.enableDepthTest();
        RenderSystem.setShaderColor(1, 1, 1, 1);
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
            int panel = fallback.panelColor(surface.active(), config.appearance.opacity * surface.opacity());
            int edge = fallback.edgeColor(surface.active(), surface.hover());
            graphics.fill(left + radius, top, right - radius, bottom, panel);
            graphics.fill(left, top + radius, right, bottom - radius, panel);
            graphics.fill(left + 2, top + 1, right - 2, top + 2, edge);
            graphics.fill(left + radius, bottom - 2, right - radius, bottom - 1, 0x28000000);
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

    private static void drawPixelQuad(float left, float top, float right, float bottom) {
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

    private void restoreMainTarget() {
        RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
        main.bindWrite(true);
        RenderSystem.viewport(0, 0, main.viewWidth, main.viewHeight);
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
        preparedFrame = Long.MIN_VALUE;
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
        if (ping != null) ping.destroyBuffers();
        if (pong != null) pong.destroyBuffers();
        ping = null;
        pong = null;
        blurred = null;
    }
}
