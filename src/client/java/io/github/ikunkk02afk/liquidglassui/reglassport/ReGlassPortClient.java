package io.github.ikunkk02afk.liquidglassui.reglassport;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.ikunkk02afk.liquidglassui.reglassport.render.ReGlassFrameCollector;
import io.github.ikunkk02afk.liquidglassui.reglassport.render.ReGlassLegacyRenderer;
import io.github.ikunkk02afk.liquidglassui.reglassport.render.ReGlassUniformData;
import io.github.ikunkk02afk.liquidglassui.reglassport.screen.ReGlassMenuController;
import io.github.ikunkk02afk.liquidglassui.reglassport.screen.ReGlassPlaygroundScreen;
import io.github.ikunkk02afk.liquidglassui.reglassport.widget.LiquidGlassPortWidget;
import io.github.ikunkk02afk.liquidglassui.reglassport.animation.ReGlassAnimationRuntime;
import io.github.ikunkk02afk.liquidglassui.reglassport.widget.GlassWidgetState;
import io.github.ikunkk02afk.liquidglassui.LiquidGlassUIClient;
import io.github.ikunkk02afk.liquidglassui.config.GlassRendererMode;
import io.github.ikunkk02afk.liquidglassui.config.LiquidGlassConfigData;
import io.github.ikunkk02afk.liquidglassui.render.fallback.SafeGlassRenderer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/** Client coordinator that keeps collection, capture, composite, and deferred labels ordered. */
public final class ReGlassPortClient {
    private static final ReGlassFrameCollector COLLECTOR = new ReGlassFrameCollector();
    private static final ArrayList<LiquidGlassPortWidget> DEFERRED = new ArrayList<>();
    private static final ArrayList<AbstractButton> DEFERRED_MENU_BUTTONS = new ArrayList<>();
    private static final ReGlassMenuController MENUS = new ReGlassMenuController();
    private static final ReGlassAnimationRuntime ANIMATION = new ReGlassAnimationRuntime();
    private static final SafeGlassRenderer FALLBACK = new SafeGlassRenderer();
    private static ReGlassLegacyRenderer renderer;
    private static Logger logger;
    private static Screen activeScreen;
    private static boolean menuFrame;
    private static long nextWarningNanos;
    private static boolean metricsLogged;

    private ReGlassPortClient() {
    }

    public static void initialize(Logger ownerLogger) {
        logger = ownerLogger;
        renderer = new ReGlassLegacyRenderer(ownerLogger);
        renderer.registerShaders();
        MENUS.register();
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
        beginCollectedFrame(screen, graphics, false);
        renderer.captureBackdrop();
    }

    /** Begins the menu pass after the concrete screen has drawn its background and before its buttons. */
    public static boolean beginMenuFrame(Screen screen, GuiGraphics graphics) {
        if (!menuConfigured(screen) || !MENUS.handles(screen) || renderer == null || !renderer.available()) {
            return false;
        }
        beginCollectedFrame(screen, graphics, true);
        if (renderer.captureBackdrop()) return true;
        abandonFrame(screen);
        return false;
    }

    public static void submit(ReGlassUniformData data) {
        if (activeScreen == null || !COLLECTOR.submit(data)) {
            warnRateLimited("Ignored an invalid, out-of-frame, or over-capacity ReGlass component submission");
        }
    }

    public static void defer(LiquidGlassPortWidget widget) {
        if (activeScreen != null && DEFERRED.size() < ReGlassFrameCollector.MAX_WIDGETS) DEFERRED.add(widget);
    }

    public static void deferMenuButton(AbstractButton button) {
        if (activeScreen != null && !DEFERRED_MENU_BUTTONS.contains(button)
                && DEFERRED_MENU_BUTTONS.size() < ReGlassFrameCollector.MAX_WIDGETS) {
            DEFERRED_MENU_BUTTONS.add(button);
        }
    }

    public static boolean tryRenderMenuWidget(AbstractWidget widget, GuiGraphics graphics, int mouseX, int mouseY) {
        return menuFrame && activeScreen != null && MENUS.submit(activeScreen, widget, graphics, mouseX, mouseY);
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
        if (screen != activeScreen || menuFrame) return;
        graphics.flush();
        List<ReGlassUniformData> widgets = COLLECTOR.finishFrame();
        boolean compositeRendered = renderer.composite(widgets, debugMode);
        for (LiquidGlassPortWidget widget : DEFERRED) widget.renderDeferred(graphics, compositeRendered);
        logMetricsOnce(compositeRendered, debugMode);
        clearFrame();
    }

    /** Composites menu glass first, then restores only the original button labels. */
    public static boolean finishMenuFrame(Screen screen, GuiGraphics graphics) {
        if (screen != activeScreen || !menuFrame) return false;
        graphics.flush();
        List<ReGlassUniformData> widgets = COLLECTOR.finishFrame();
        boolean compositeRendered = renderer.composite(widgets, 0);
        LiquidGlassConfigData config = LiquidGlassUIClient.configManager().get();
        if (!compositeRendered) {
            for (AbstractButton button : DEFERRED_MENU_BUTTONS) {
                FALLBACK.drawPanel(graphics, button, config.appearance.cornerRadius,
                        Math.min(0.42f, config.appearance.opacity + 0.14f));
            }
        }
        for (AbstractButton button : DEFERRED_MENU_BUTTONS) {
            int textColor = button.active ? 0xFFFFFFFF : 0xFFA0A0A0;
            button.renderString(graphics, Minecraft.getInstance().font, textColor);
        }
        logMetricsOnce(compositeRendered, 0);
        clearFrame();
        return true;
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
        clearFrame();
    }

    public static void abandonFrame(Screen screen) {
        if (screen != activeScreen) return;
        if (COLLECTOR.active()) COLLECTOR.finishFrame();
        clearFrame();
    }

    /** True only when a requested menu can use the new renderer this frame. */
    public static boolean menuRendererAvailable() {
        return renderer != null && renderer.available();
    }

    public static boolean shouldCancelVanillaScreenBlur(Screen screen) {
        return menuConfigured(screen) && MENUS.handles(screen) && menuRendererAvailable();
    }

    /** Lets the retained backend take over on explicit disable or a latched ReGlass shader failure. */
    public static boolean shouldUseLegacyFallback(Screen screen) {
        LiquidGlassConfigData config = LiquidGlassUIClient.configManager().get();
        return (screen instanceof TitleScreen || screen instanceof PauseScreen)
                && config.ui.rendererMode == GlassRendererMode.REGLASS_PORT
                && (!config.ui.reGlassPortEnabled || !menuRendererAvailable());
    }

    private static void beginCollectedFrame(Screen screen, GuiGraphics graphics, boolean menu) {
        graphics.flush();
        Minecraft client = Minecraft.getInstance();
        if (!client.isWindowActive()) ANIMATION.resetClock();
        ANIMATION.beginFrame(System.nanoTime());
        activeScreen = screen;
        menuFrame = menu;
        DEFERRED.clear();
        DEFERRED_MENU_BUTTONS.clear();
        COLLECTOR.beginFrame();
        renderer.beginFrame(screen.width, screen.height);
    }

    private static boolean menuConfigured(Screen screen) {
        LiquidGlassConfigData config = LiquidGlassUIClient.configManager().get();
        if (!enabled() || !config.appearance.enabled || !config.ui.replaceCommonButtons) return false;
        if (screen instanceof TitleScreen) return config.ui.mainMenu;
        return screen instanceof PauseScreen && config.ui.pauseMenu;
    }

    private static void logMetricsOnce(boolean compositeRendered, int debugMode) {
        if (!metricsLogged && compositeRendered && logger != null) {
            ReGlassLegacyRenderer.Metrics metrics = renderer.metrics();
            logger.info("ReGlass Port frame metrics: capture={}, blurPasses={}, composite={}, widgets={}, debugMode={}",
                    metrics.captureCount(), metrics.blurPassCount(), metrics.compositeCount(),
                    metrics.widgetCount(), debugMode);
            metricsLogged = true;
        }
    }

    private static void clearFrame() {
        activeScreen = null;
        menuFrame = false;
        DEFERRED.clear();
        DEFERRED_MENU_BUTTONS.clear();
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
