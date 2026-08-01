package io.github.ikunkk02afk.liquidglassui;

import io.github.ikunkk02afk.liquidglassui.config.LiquidGlassConfigManager;
import io.github.ikunkk02afk.liquidglassui.reglassport.ReGlassPortClient;
import io.github.ikunkk02afk.liquidglassui.render.legacy.LegacyGlassRenderBackend;
import io.github.ikunkk02afk.liquidglassui.screen.LiquidGlassScreenManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LiquidGlassUIClient implements ClientModInitializer {
    public static final String MOD_ID = "liquid_glass_ui";
    public static final Logger LOGGER = LoggerFactory.getLogger("Liquid Glass UI");

    private static LiquidGlassConfigManager configManager;
    private static LegacyGlassRenderBackend renderBackend;
    private static LiquidGlassScreenManager screenManager;

    @Override
    public void onInitializeClient() {
        configManager = new LiquidGlassConfigManager(
                FabricLoader.getInstance().getConfigDir().resolve("liquid_glass_ui.json"), LOGGER);
        configManager.load();

        renderBackend = new LegacyGlassRenderBackend(LOGGER);
        renderBackend.registerShaders();
        screenManager = new LiquidGlassScreenManager(configManager, renderBackend);
        screenManager.register();

        ReGlassPortClient.initialize(LOGGER);

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            ReGlassPortClient.close();
            renderBackend.close();
        });
        LOGGER.info("Liquid Glass UI client initialized");
    }

    public static boolean tryRenderWidget(AbstractWidget widget, GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (ReGlassPortClient.tryRenderMenuWidget(widget, graphics, mouseX, mouseY)) return true;
        return screenManager != null && screenManager.tryRenderWidget(widget, graphics, mouseX, mouseY, delta);
    }

    public static void captureBackdrop(Screen screen, GuiGraphics graphics) {
        if (ReGlassPortClient.beginMenuFrame(screen, graphics)) return;
        if (screenManager != null) screenManager.captureBackdrop(screen, graphics);
    }

    public static void compositeAndRenderContents(Screen screen, GuiGraphics graphics) {
        if (ReGlassPortClient.finishMenuFrame(screen, graphics)) return;
        if (screenManager != null) screenManager.compositeAndRenderContents(screen, graphics);
    }

    public static boolean shouldCancelVanillaScreenBlur(Screen screen) {
        return ReGlassPortClient.shouldCancelVanillaScreenBlur(screen)
                || screenManager != null && screenManager.shouldCancelVanillaScreenBlur(screen);
    }

    public static LiquidGlassConfigManager configManager() {
        if (configManager == null) throw new IllegalStateException("Liquid Glass UI is not initialized");
        return configManager;
    }

    public static LegacyGlassRenderBackend renderBackend() {
        if (renderBackend == null) throw new IllegalStateException("Liquid Glass UI is not initialized");
        return renderBackend;
    }
}
