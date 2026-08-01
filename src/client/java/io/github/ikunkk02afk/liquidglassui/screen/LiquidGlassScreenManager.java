package io.github.ikunkk02afk.liquidglassui.screen;

import io.github.ikunkk02afk.liquidglassui.animation.AnimationClock;
import io.github.ikunkk02afk.liquidglassui.config.LiquidGlassConfigData;
import io.github.ikunkk02afk.liquidglassui.config.LiquidGlassConfigManager;
import io.github.ikunkk02afk.liquidglassui.render.GlassFrameContext;
import io.github.ikunkk02afk.liquidglassui.render.GlassQualityBudget;
import io.github.ikunkk02afk.liquidglassui.render.GlassRectangle;
import io.github.ikunkk02afk.liquidglassui.render.fallback.SafeGlassRenderer;
import io.github.ikunkk02afk.liquidglassui.render.frame.GlassFrameCollector;
import io.github.ikunkk02afk.liquidglassui.render.frame.GlassWidgetData;
import io.github.ikunkk02afk.liquidglassui.render.legacy.LegacyGlassRenderBackend;
import io.github.ikunkk02afk.liquidglassui.render.material.GlassMaterial;
import io.github.ikunkk02afk.liquidglassui.widget.GlassGroupController;
import io.github.ikunkk02afk.liquidglassui.widget.GlassWidgetState;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class LiquidGlassScreenManager {
    private final LiquidGlassConfigManager configManager;
    private final LegacyGlassRenderBackend backend;
    private final GlassFrameCollector collector = new GlassFrameCollector();
    private final SafeGlassRenderer fallback = new SafeGlassRenderer();
    private final Map<Screen, ScreenState> screens = new IdentityHashMap<>();
    private long frameId;

    public LiquidGlassScreenManager(LiquidGlassConfigManager configManager, LegacyGlassRenderBackend backend) {
        this.configManager = configManager;
        this.backend = backend;
    }

    public void register() {
        ScreenEvents.AFTER_INIT.register(this::afterInit);
    }

    private void afterInit(Minecraft client, Screen screen, int width, int height) {
        if (!(screen instanceof TitleScreen || screen instanceof PauseScreen || screen instanceof ConfirmScreen)) return;

        LiquidGlassConfigButton configButton = null;
        if (screen instanceof TitleScreen || screen instanceof PauseScreen) {
            int commonWidth = 200;
            int maxBottom = 0;
            for (AbstractWidget widget : Screens.getButtons(screen)) {
                if (isPrimaryMenuButton(screen, widget)) {
                    commonWidth = Math.max(commonWidth, widget.getWidth());
                    maxBottom = Math.max(maxBottom, widget.getBottom());
                }
            }
            commonWidth = Math.min(commonWidth, Math.max(80, screen.width - 20));
            int x = (screen.width - commonWidth) / 2;
            int y = Math.min(Math.max(6, screen.height - Button.DEFAULT_HEIGHT - 6), maxBottom + 4);
            configButton = new LiquidGlassConfigButton(screen, x, y, commonWidth);
            Screens.getButtons(screen).add(configButton);
        }

        ScreenState state = new ScreenState();
        List<AbstractButton> eligible = new ArrayList<>();
        for (AbstractWidget widget : Screens.getButtons(screen)) {
            if (widget instanceof AbstractButton button && isSupportedButton(button)
                    && (button == configButton || isPrimaryMenuButton(screen, button))) eligible.add(button);
        }
        eligible.sort(Comparator.comparingInt(AbstractWidget::getY).thenComparingInt(AbstractWidget::getX));
        assignGroups(screen, eligible, configButton, state);
        for (AbstractButton button : eligible) {
            state.widgets.put(button, new GlassWidgetState(button));
            state.order.add(button);
        }
        screens.put(screen, state);

        ScreenEvents.beforeRender(screen).register((current, graphics, mouseX, mouseY, delta) ->
                beforeRender(current, mouseX, mouseY));
        ScreenEvents.afterRender(screen).register((current, graphics, mouseX, mouseY, delta) -> backend.endFrame());
        ScreenEvents.remove(screen).register(current -> {
            screens.remove(current);
            backend.endFrame();
        });
        ScreenMouseEvents.beforeMouseClick(screen).register((current, mouseX, mouseY, button) ->
                setMousePressed(current, mouseX, mouseY, button, true));
        ScreenMouseEvents.beforeMouseRelease(screen).register((current, mouseX, mouseY, button) ->
                setMousePressed(current, mouseX, mouseY, button, false));
        ScreenKeyboardEvents.beforeKeyPress(screen).register((current, key, scanCode, modifiers) ->
                setKeyboardPressed(current, key, true));
        ScreenKeyboardEvents.beforeKeyRelease(screen).register((current, key, scanCode, modifiers) ->
                setKeyboardPressed(current, key, false));
    }

    private static void assignGroups(Screen screen, List<AbstractButton> buttons, AbstractButton configButton,
                                     ScreenState state) {
        int nonSettingsIndex = 0;
        for (AbstractButton button : buttons) {
            int group;
            if (button == configButton) {
                group = 3;
            } else if (screen instanceof TitleScreen) {
                group = nonSettingsIndex++ < 3 ? 1 : 2;
            } else {
                group = 1;
            }
            state.groupIds.put(button, group);
            state.controllers.computeIfAbsent(group, ignored -> new GlassGroupController());
        }
    }

    private void beforeRender(Screen screen, int mouseX, int mouseY) {
        ScreenState state = screens.get(screen);
        if (state == null) return;
        Minecraft client = Minecraft.getInstance();
        LiquidGlassConfigData config = configManager.get();
        state.enabled = enabledFor(screen, config);
        state.deferred.clear();
        state.deferredSet.clear();
        double delta = state.clock.tick(client.isWindowActive());
        long currentFrame = ++frameId;
        collector.begin(currentFrame, config.performance.maxGlassComponents);

        for (GlassWidgetState widget : state.widgets.values()) {
            widget.update(delta, mouseX, mouseY, config);
            AbstractButton button = widget.button();
            if ((button.isMouseOver(mouseX, mouseY) || button.isFocused()) && button.active) {
                state.controllers.get(state.groupIds.get(button)).updateTarget(button);
            }
        }
        for (GlassGroupController controller : state.controllers.values()) controller.update(delta, config);

        GlassQualityBudget quality = GlassQualityBudget.from(config);
        var main = client.getMainRenderTarget();
        backend.beginFrame(new GlassFrameContext(currentFrame, screen.width, screen.height,
                main.viewWidth, main.viewHeight, client.getWindow().getGuiScale(), mouseX, mouseY,
                quality, config.performance.debugView, config.optics.blurRadius));
        if (state.enabled) collectWidgets(state, config, quality, mouseX, mouseY);
    }

    private void collectWidgets(ScreenState state, LiquidGlassConfigData config, GlassQualityBudget quality,
                                int mouseX, int mouseY) {
        GlassMaterial material = GlassMaterial.from(config, quality);
        for (AbstractButton button : state.order) {
            if (!button.visible) continue;
            GlassWidgetState widgetState = state.widgets.get(button);
            var animation = widgetState.animation();
            GlassRectangle bounds = GlassGroupController.bounds(button).scaled(animation.scale());
            float expansion = config.fusion.hoverExpansion * animation.hover();
            bounds = new GlassRectangle(bounds.x() - expansion, bounds.y() - expansion,
                    bounds.width() + expansion * 2.0f, bounds.height() + expansion * 2.0f);
            GlassWidgetData widget = collector.add();
            if (widget == null) break;
            widget.x = bounds.x();
            widget.y = bounds.y();
            widget.width = bounds.width();
            widget.height = bounds.height();
            widget.cornerRadius = Math.min(config.appearance.cornerRadius, bounds.height() * 0.48f);
            widget.hover = animation.hover();
            widget.press = widgetState.pressed() ? 1.0f : 0.0f;
            widget.focus = button.isFocused() ? 1.0f : 0.0f;
            widget.opacity = animation.opacity();
            widget.groupId = state.groupIds.get(button);
            widget.smoothing = config.fusion.enabled && config.fusion.staticConnection ? config.fusion.softness : 0.0f;
            widget.mouseX = mouseX;
            widget.mouseY = mouseY;
            widget.material = material;
        }
        if (!config.fusion.enabled || config.animation.reduceMotion) return;
        for (Map.Entry<Integer, GlassGroupController> entry : state.controllers.entrySet()) {
            addFusionCapsule(entry.getKey(), entry.getValue(), config, material, mouseX, mouseY);
        }
    }

    private void addFusionCapsule(int groupId, GlassGroupController controller, LiquidGlassConfigData config,
                                  GlassMaterial material, int mouseX, int mouseY) {
        if (!controller.hasMotion()) return;
        float merge = controller.merge(config.animation.mergeStrength);
        if (merge <= 0.001f) return;
        GlassRectangle previous = controller.previousBounds();
        GlassRectangle active = controller.activeBounds();
        float dx = active.centerX() - previous.centerX();
        float dy = active.centerY() - previous.centerY();
        float centerDistance = (float) Math.sqrt(dx * dx + dy * dy);
        float allowed = config.fusion.distance + Math.max(previous.width(), previous.height()) * 0.5f
                + Math.max(active.width(), active.height()) * 0.5f;
        if (centerDistance > allowed || centerDistance < 0.01f) return;
        GlassWidgetData capsule = collector.add();
        if (capsule == null) return;
        capsule.shape = GlassWidgetData.SHAPE_CAPSULE;
        capsule.groupId = groupId;
        capsule.capsuleStartX = previous.centerX();
        capsule.capsuleStartY = previous.centerY();
        capsule.capsuleEndX = active.centerX();
        capsule.capsuleEndY = active.centerY();
        capsule.capsuleRadius = Math.min(previous.height(), active.height()) * (0.12f + 0.30f * merge);
        capsule.smoothing = config.fusion.softness * merge;
        capsule.hover = merge;
        capsule.opacity = Math.min(1.0f, previous.height() <= 0.0f ? 0.0f : merge * 1.5f);
        capsule.mouseX = mouseX;
        capsule.mouseY = mouseY;
        capsule.material = material;
    }

    public boolean tryRenderWidget(AbstractWidget widget, GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        Minecraft client = Minecraft.getInstance();
        ScreenState state = screens.get(client.screen);
        if (state == null || !state.enabled || !(widget instanceof AbstractButton button)
                || !state.widgets.containsKey(button) || !isSupportedButton(button)) return false;
        if (state.deferredSet.put(button, Boolean.TRUE) == null) state.deferred.add(button);
        return true;
    }

    public void captureBackdrop(Screen screen, GuiGraphics graphics) {
        ScreenState state = screens.get(screen);
        if (state == null || !state.enabled) return;
        graphics.flush();
        backend.captureBackdrop();
    }

    public void compositeAndRenderContents(Screen screen, GuiGraphics graphics) {
        ScreenState state = screens.get(screen);
        if (state == null || !state.enabled || state.deferred.isEmpty()) return;
        graphics.flush();
        boolean rendered = backend.composite(collector.freeze());
        LiquidGlassConfigData config = configManager.get();
        if (!rendered) {
            for (AbstractButton button : state.deferred) {
                fallback.drawPanel(graphics, button, config.appearance.cornerRadius,
                        Math.min(0.42f, config.appearance.opacity + 0.14f));
            }
        }
        for (AbstractButton button : state.deferred) {
            int textColor = button.active ? 0xFFFFFFFF : 0xFFA0A0A0;
            button.renderString(graphics, Minecraft.getInstance().font, textColor);
        }
        state.deferred.clear();
        state.deferredSet.clear();
    }

    public boolean shouldCancelVanillaScreenBlur(Screen screen) {
        ScreenState state = screens.get(screen);
        return state != null && state.enabled;
    }

    private boolean enabledFor(Screen screen, LiquidGlassConfigData config) {
        if (!config.appearance.enabled || !config.ui.replaceCommonButtons) return false;
        if (screen instanceof TitleScreen) return config.ui.mainMenu;
        if (screen instanceof PauseScreen) return config.ui.pauseMenu;
        return screen instanceof ConfirmScreen && config.ui.confirmDialogs;
    }

    private static boolean isSupportedButton(AbstractButton button) {
        return button.getClass() == Button.class || button instanceof LiquidGlassConfigButton;
    }

    private static boolean isPrimaryMenuButton(Screen screen, AbstractWidget widget) {
        if (!(widget instanceof AbstractButton button) || button instanceof PlainTextButton
                || !isSupportedButton(button) || widget.getWidth() < 90) return false;
        float center = widget.getX() + widget.getWidth() * 0.5f;
        return Math.abs(center - screen.width * 0.5f) <= 110.0f;
    }

    private void setMousePressed(Screen screen, double mouseX, double mouseY, int mouseButton, boolean pressed) {
        if (mouseButton != GLFW.GLFW_MOUSE_BUTTON_LEFT) return;
        ScreenState state = screens.get(screen);
        if (state == null) return;
        for (GlassWidgetState widget : state.widgets.values()) {
            if (!pressed || widget.button().isMouseOver(mouseX, mouseY)) widget.pressed(pressed);
        }
    }

    private void setKeyboardPressed(Screen screen, int key, boolean pressed) {
        if (key != GLFW.GLFW_KEY_ENTER && key != GLFW.GLFW_KEY_KP_ENTER && key != GLFW.GLFW_KEY_SPACE) return;
        ScreenState state = screens.get(screen);
        if (state == null) return;
        for (GlassWidgetState widget : state.widgets.values()) {
            if (!pressed || widget.button().isFocused()) widget.pressed(pressed);
        }
    }

    private static final class ScreenState {
        private final AnimationClock clock = new AnimationClock();
        private final Map<AbstractButton, GlassWidgetState> widgets = new IdentityHashMap<>();
        private final Map<AbstractButton, Integer> groupIds = new IdentityHashMap<>();
        private final Map<Integer, GlassGroupController> controllers = new HashMap<>();
        private final List<AbstractButton> order = new ArrayList<>();
        private final List<AbstractButton> deferred = new ArrayList<>();
        private final Map<AbstractButton, Boolean> deferredSet = new IdentityHashMap<>();
        private boolean enabled;
    }
}
