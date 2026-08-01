package io.github.ikunkk02afk.liquidglassui.screen;

import io.github.ikunkk02afk.liquidglassui.animation.AnimationClock;
import io.github.ikunkk02afk.liquidglassui.config.LiquidGlassConfigData;
import io.github.ikunkk02afk.liquidglassui.config.LiquidGlassConfigManager;
import io.github.ikunkk02afk.liquidglassui.render.GlassFrameContext;
import io.github.ikunkk02afk.liquidglassui.render.GlassQualityBudget;
import io.github.ikunkk02afk.liquidglassui.render.LegacyGlassRenderBackend;
import io.github.ikunkk02afk.liquidglassui.widget.GlassButtonRenderer;
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
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.lwjgl.glfw.GLFW;

import java.util.IdentityHashMap;
import java.util.Map;

public final class LiquidGlassScreenManager {
    private final LiquidGlassConfigManager configManager;
    private final LegacyGlassRenderBackend backend;
    private final GlassButtonRenderer renderer;
    private final Map<Screen, ScreenState> screens = new IdentityHashMap<>();
    private long frameId;

    public LiquidGlassScreenManager(LiquidGlassConfigManager configManager, LegacyGlassRenderBackend backend) {
        this.configManager = configManager;
        this.backend = backend;
        this.renderer = new GlassButtonRenderer(backend);
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
            int y = Math.min(Math.max(6, screen.height - 26), maxBottom + 4);
            configButton = new LiquidGlassConfigButton(screen, x, y, commonWidth);
            Screens.getButtons(screen).add(configButton);
        }

        ScreenState state = new ScreenState();
        for (AbstractWidget widget : Screens.getButtons(screen)) {
            if (widget instanceof AbstractButton button && !(button instanceof PlainTextButton)
                    && (button == configButton || isPrimaryMenuButton(screen, button))) {
                GlassWidgetState widgetState = new GlassWidgetState(button);
                state.widgets.put(button, widgetState);
                state.groups.put(button, button == configButton ? state.settingsGroup : state.mainGroup);
            }
        }
        screens.put(screen, state);

        ScreenEvents.beforeRender(screen).register((current, graphics, mouseX, mouseY, delta) -> beforeRender(current, mouseX, mouseY));
        ScreenEvents.afterRender(screen).register((current, graphics, mouseX, mouseY, delta) -> backend.endFrame());
        ScreenEvents.remove(screen).register(current -> {
            screens.remove(current);
            backend.endFrame();
        });
        ScreenMouseEvents.beforeMouseClick(screen).register((current, mouseX, mouseY, button) -> setMousePressed(current, mouseX, mouseY, button, true));
        ScreenMouseEvents.beforeMouseRelease(screen).register((current, mouseX, mouseY, button) -> setMousePressed(current, mouseX, mouseY, button, false));
        ScreenKeyboardEvents.beforeKeyPress(screen).register((current, key, scanCode, modifiers) -> setKeyboardPressed(current, key, true));
        ScreenKeyboardEvents.beforeKeyRelease(screen).register((current, key, scanCode, modifiers) -> setKeyboardPressed(current, key, false));
    }

    private void beforeRender(Screen screen, int mouseX, int mouseY) {
        ScreenState state = screens.get(screen);
        if (state == null) return;
        Minecraft client = Minecraft.getInstance();
        LiquidGlassConfigData config = configManager.get();
        double delta = state.clock.tick(client.isWindowActive());

        AbstractButton hoveredMain = null;
        AbstractButton hoveredSettings = null;
        for (GlassWidgetState widget : state.widgets.values()) {
            widget.update(delta, mouseX, mouseY, config);
            if (widget.button().isHoveredOrFocused() && widget.button().active) {
                if (state.groups.get(widget.button()) == state.settingsGroup) hoveredSettings = widget.button();
                else hoveredMain = widget.button();
            }
        }
        if (hoveredMain != null) state.mainGroup.updateTarget(hoveredMain);
        if (hoveredSettings != null) state.settingsGroup.updateTarget(hoveredSettings);
        state.mainGroup.update(delta, config);
        state.settingsGroup.update(delta, config);

        var window = client.getWindow();
        backend.beginFrame(new GlassFrameContext(++frameId, screen.width, screen.height, window.getWidth(),
                window.getHeight(), window.getGuiScale(), mouseX, mouseY, GlassQualityBudget.from(config)));
    }

    public boolean tryRenderWidget(AbstractWidget widget, GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        Minecraft client = Minecraft.getInstance();
        ScreenState state = screens.get(client.screen);
        if (state == null || !(widget instanceof AbstractButton button)) return false;
        GlassWidgetState widgetState = state.widgets.get(button);
        if (widgetState == null || !enabledFor(client.screen, configManager.get())) return false;
        return renderer.render(button, graphics, widgetState, state.groups.get(button), configManager.get());
    }

    public void captureBackdrop(Screen screen, GuiGraphics graphics) {
        ScreenState state = screens.get(screen);
        LiquidGlassConfigData config = configManager.get();
        if (state != null && enabledFor(screen, config)) backend.captureBackground(graphics, config);
    }

    public boolean shouldKeepPauseBackdropClear(Screen screen) {
        return screen instanceof PauseScreen && enabledFor(screen, configManager.get());
    }

    private boolean enabledFor(Screen screen, LiquidGlassConfigData config) {
        if (!config.appearance.enabled || !config.ui.replaceCommonButtons) return false;
        if (screen instanceof TitleScreen) return config.ui.mainMenu;
        if (screen instanceof PauseScreen) return config.ui.pauseMenu;
        return screen instanceof ConfirmScreen && config.ui.confirmDialogs;
    }

    private static boolean isPrimaryMenuButton(Screen screen, AbstractWidget widget) {
        if (!(widget instanceof AbstractButton) || widget instanceof PlainTextButton || widget.getWidth() < 90) return false;
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
        private final Map<AbstractButton, GlassGroupController> groups = new IdentityHashMap<>();
        private final GlassGroupController mainGroup = new GlassGroupController();
        private final GlassGroupController settingsGroup = new GlassGroupController();
    }
}
