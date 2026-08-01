package io.github.ikunkk02afk.liquidglassui.reglassport.screen;

import io.github.ikunkk02afk.liquidglassui.LiquidGlassUIClient;
import io.github.ikunkk02afk.liquidglassui.config.LiquidGlassConfigData;
import io.github.ikunkk02afk.liquidglassui.reglassport.ReGlassPortClient;
import io.github.ikunkk02afk.liquidglassui.reglassport.api.GlassStyle;
import io.github.ikunkk02afk.liquidglassui.reglassport.api.ReGlassPortApi;
import io.github.ikunkk02afk.liquidglassui.reglassport.api.ReGlassStyleMapper;
import io.github.ikunkk02afk.liquidglassui.reglassport.widget.GlassWidgetState;
import io.github.ikunkk02afk.liquidglassui.screen.LiquidGlassConfigButton;
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
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Owns only menu selection, grouping, interaction state, and deferred ReGlass submissions. */
public final class ReGlassMenuController {
    private final Map<Screen, MenuState> screens = new IdentityHashMap<>();

    public void register() {
        ScreenEvents.AFTER_INIT.register(this::afterInit);
    }

    private void afterInit(Minecraft client, Screen screen, int width, int height) {
        if (!(screen instanceof TitleScreen || screen instanceof PauseScreen)) return;

        MenuState state = new MenuState();
        List<AbstractButton> targets = new ArrayList<>();
        for (AbstractWidget widget : Screens.getButtons(screen)) {
            if (widget instanceof AbstractButton button && isTargetButton(screen, button)) targets.add(button);
        }
        targets.sort(Comparator.comparingInt(AbstractWidget::getY).thenComparingInt(AbstractWidget::getX));

        int nextIndependentGroup = 1000;
        for (AbstractButton button : targets) {
            int group = knownGroup(screen, translationKey(button.getMessage()));
            state.buttons.put(button, new MenuButtonState(group == ReGlassMenuGroups.UNKNOWN
                    ? nextIndependentGroup++ : group));
        }
        screens.put(screen, state);

        ScreenEvents.remove(screen).register(current -> {
            screens.remove(current);
            ReGlassPortClient.abandonFrame(current);
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

    public boolean handles(Screen screen) {
        return screens.containsKey(screen);
    }

    public boolean submit(Screen screen, AbstractWidget widget, GuiGraphics graphics, int mouseX, int mouseY) {
        MenuState state = screens.get(screen);
        if (state == null || !(widget instanceof AbstractButton button) || !button.visible) return false;
        MenuButtonState buttonState = state.buttons.get(button);
        if (buttonState == null) return false;

        LiquidGlassConfigData config = LiquidGlassUIClient.configManager().get();
        long stableId = Integer.toUnsignedLong(System.identityHashCode(button));
        float highlightX = (mouseX - (button.getX() + button.getWidth() * 0.5f))
                / Math.max(1.0f, button.getWidth() * 0.5f);
        float highlightY = (mouseY - (button.getY() + button.getHeight() * 0.5f))
                / Math.max(1.0f, button.getHeight() * 0.5f);
        float hovered = button.active && button.isMouseOver(mouseX, mouseY) ? 1.0f : 0.0f;
        float focused = button.active && button.isFocused() ? 1.0f : 0.0f;
        float fusionTarget = config.fusion.enabled ? 1.0f : 0.0f;
        GlassWidgetState animation = ReGlassPortClient.animate(stableId, button.getX(), button.getY(),
                hovered, focused, buttonState.pressed ? 1.0f : 0.0f,
                highlightX, highlightY, fusionTarget);

        float expansion = animation.shapeExpansion();
        float smoothing = config.fusion.enabled
                ? config.fusion.softness * config.animation.mergeStrength / Math.max(1.0f, screen.height)
                : 0.0f;
        GlassStyle style = ReGlassStyleMapper.fromConfig(config)
                .smoothing(smoothing)
                .shadow(10.0f, 0.24f, 0.0f, 2.0f);
        ReGlassPortApi.create(graphics)
                .dimensions(animation.x() - expansion, animation.y() - expansion,
                        button.getWidth() + expansion * 2.0f, button.getHeight() + expansion * 2.0f)
                .cornerRadius(Math.min(config.appearance.cornerRadius, button.getHeight() * 0.48f))
                .style(style)
                .hover(animation.hover())
                .focus(animation.focus())
                .press(animation.press())
                .fusion(animation.fusion())
                .highlight(animation.highlightX(), animation.highlightY())
                .group(buttonState.groupId)
                .id(stableId)
                .render();
        ReGlassPortClient.deferMenuButton(button);
        return true;
    }

    private static int knownGroup(Screen screen, String key) {
        if (screen instanceof TitleScreen) return ReGlassMenuGroups.titleGroup(key);
        return ReGlassMenuGroups.pauseGroup(key);
    }

    private static String translationKey(Component component) {
        return component.getContents() instanceof TranslatableContents translatable
                ? translatable.getKey() : "";
    }

    private static boolean isTargetButton(Screen screen, AbstractButton button) {
        if (button instanceof PlainTextButton || !isSupportedButton(button) || button.getWidth() < 90) return false;
        float center = button.getX() + button.getWidth() * 0.5f;
        return Math.abs(center - screen.width * 0.5f) <= 110.0f;
    }

    private static boolean isSupportedButton(AbstractButton button) {
        return button.getClass() == Button.class || button instanceof LiquidGlassConfigButton;
    }

    private void setMousePressed(Screen screen, double mouseX, double mouseY, int mouseButton, boolean pressed) {
        if (mouseButton != GLFW.GLFW_MOUSE_BUTTON_LEFT) return;
        MenuState state = screens.get(screen);
        if (state == null) return;
        for (Map.Entry<AbstractButton, MenuButtonState> entry : state.buttons.entrySet()) {
            if (!pressed || entry.getKey().isMouseOver(mouseX, mouseY)) entry.getValue().pressed = pressed;
        }
    }

    private void setKeyboardPressed(Screen screen, int key, boolean pressed) {
        if (key != GLFW.GLFW_KEY_ENTER && key != GLFW.GLFW_KEY_KP_ENTER && key != GLFW.GLFW_KEY_SPACE) return;
        MenuState state = screens.get(screen);
        if (state == null) return;
        for (Map.Entry<AbstractButton, MenuButtonState> entry : state.buttons.entrySet()) {
            if (!pressed || entry.getKey().isFocused()) entry.getValue().pressed = pressed;
        }
    }

    private static final class MenuState {
        private final Map<AbstractButton, MenuButtonState> buttons = new IdentityHashMap<>();
    }

    private static final class MenuButtonState {
        private final int groupId;
        private boolean pressed;

        private MenuButtonState(int groupId) {
            this.groupId = groupId;
        }
    }
}
