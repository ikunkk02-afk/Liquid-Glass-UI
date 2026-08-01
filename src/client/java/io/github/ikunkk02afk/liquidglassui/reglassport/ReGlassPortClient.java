package io.github.ikunkk02afk.liquidglassui.reglassport;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.ikunkk02afk.liquidglassui.reglassport.render.ReGlassFrameCollector;
import io.github.ikunkk02afk.liquidglassui.reglassport.render.ReGlassLegacyRenderer;
import io.github.ikunkk02afk.liquidglassui.reglassport.render.ReGlassUniformData;
import io.github.ikunkk02afk.liquidglassui.reglassport.screen.ReGlassPlaygroundScreen;
import io.github.ikunkk02afk.liquidglassui.reglassport.widget.LiquidGlassPortWidget;
import io.github.ikunkk02afk.liquidglassui.reglassport.animation.ReGlassAnimationRuntime;
import io.github.ikunkk02afk.liquidglassui.reglassport.widget.GlassWidgetState;
import io.github.ikunkk02afk.liquidglassui.LiquidGlassUIClient;
import io.github.ikunkk02afk.liquidglassui.config.GlassRendererMode;
import io.github.ikunkk02afk.liquidglassui.config.LiquidGlassConfigData;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/** Client coordinator that keeps collection, capture, composite, and deferred labels ordered. */
public final class ReGlassPortClient {
    private static final ReGlassFrameCollector COLLECTOR = new ReGlassFrameCollector();
    private static final ArrayList<LiquidGlassPortWidget> DEFERRED = new ArrayList<>();
    private static final ReGlassAnimationRuntime ANIMATION = new ReGlassAnimationRuntime();
    private static ReGlassLegacyRenderer renderer;
    private static Logger logger;
    private static Screen activeScreen;
    private static long nextWarningNanos;
    private static boolean metricsLogged;

    private ReGlassPortClient() {
    }

    public static void initialize(Logger ownerLogger) {
        logger = ownerLogger;
        renderer = new ReGlassLegacyRenderer(ownerLogger);
        renderer.registerShaders();
        KeyMapping openPlayground = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.liquid_glass_ui.open_reglass_playground",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                "key.categories.liquid_glass_ui"));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openPlayground.consumeClick()) {
                if (!(client.screen instanceof ReGlassPlaygroundScreen)) {
                    openPlayground(client.screen);
                }
            }
        });
    }

    public static void beginFrame(Screen screen, GuiGraphics graphics) {
        if (!(screen instanceof ReGlassPlaygroundScreen) || renderer == null || !enabled()) return;
        graphics.flush();
        ANIMATION.beginFrame(System.nanoTime());
        activeScreen = screen;
        DEFERRED.clear();
        COLLECTOR.beginFrame();
        renderer.beginFrame(screen.width, screen.height);
        renderer.captureBackdrop();
    }

    public static void submit(ReGlassUniformData data) {
        if (activeScreen == null || !COLLECTOR.submit(data)) {
            warnRateLimited("Ignored an invalid, out-of-frame, or over-capacity ReGlass component submission");
        }
    }

    public static void defer(LiquidGlassPortWidget widget) {
        if (activeScreen != null && DEFERRED.size() < ReGlassFrameCollector.MAX_WIDGETS) DEFERRED.add(widget);
    }

    public static boolean frameActive() {
        return activeScreen != null && COLLECTOR.active();
    }

    public static GlassWidgetState animate(long stableId, float x, float y,
                                           float hover, float focus, float press,
                                           float highlightX, float highlightY, float fusionTarget) {
        LiquidGlassConfigData config = LiquidGlassUIClient.configManager().get();
        if (!config.animation.enabled || config.animation.reduceMotion) {
            return new GlassWidgetState(x, y, hover, focus, press,
                    hover * 1.5f + focus * 2.5f - press * 0.8f,
                    fusionTarget, highlightX, highlightY);
        }
        return ANIMATION.sample(stableId, x, y, hover, focus, press,
                highlightX, highlightY, fusionTarget,
                config.animation.springStiffness, config.animation.damping);
    }

    public static void finishFrame(Screen screen, GuiGraphics graphics, int debugMode) {
        if (screen != activeScreen) return;
        graphics.flush();
        List<ReGlassUniformData> widgets = COLLECTOR.finishFrame();
        boolean compositeRendered = renderer.composite(widgets, debugMode);
        for (LiquidGlassPortWidget widget : DEFERRED) widget.renderDeferred(graphics, compositeRendered);
        if (!metricsLogged && compositeRendered && logger != null) {
            ReGlassLegacyRenderer.Metrics metrics = renderer.metrics();
            logger.info("ReGlass Port frame metrics: capture={}, blurPasses={}, composite={}, widgets={}, debugMode={}",
                    metrics.captureCount(), metrics.blurPassCount(), metrics.compositeCount(),
                    metrics.widgetCount(), debugMode);
            metricsLogged = true;
        }
        activeScreen = null;
        DEFERRED.clear();
    }

    public static ReGlassLegacyRenderer.Metrics metrics() {
        return renderer == null ? new ReGlassLegacyRenderer.Metrics(0, 0, 0, 0) : renderer.metrics();
    }

    public static void openPlayground(Screen parent) {
        metricsLogged = false;
        Minecraft.getInstance().setScreen(new ReGlassPlaygroundScreen(parent));
    }

    public static void close() {
        if (renderer != null) renderer.close();
        renderer = null;
        activeScreen = null;
        DEFERRED.clear();
    }

    private static boolean enabled() {
        LiquidGlassConfigData config = LiquidGlassUIClient.configManager().get();
        return config.ui.reGlassPortEnabled && config.ui.rendererMode == GlassRendererMode.REGLASS_PORT;
    }

    private static void warnRateLimited(String message) {
        long now = System.nanoTime();
        if (logger != null && now >= nextWarningNanos) {
            nextWarningNanos = now + 5_000_000_000L;
            logger.warn(message);
        }
    }
}
