package io.github.ikunkk02afk.liquidglassui;

import io.github.ikunkk02afk.liquidglassui.config.LiquidGlassConfigManager;
import io.github.ikunkk02afk.liquidglassui.render.LegacyGlassRenderBackend;
import io.github.ikunkk02afk.liquidglassui.screen.LiquidGlassScreenManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
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

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> renderBackend.close());
        LOGGER.info("Liquid Glass UI client initialized");
    }

    public static boolean tryRenderWidget(AbstractWidget widget, GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        return screenManager != null && screenManager.tryRenderWidget(widget, graphics, mouseX, mouseY, delta);
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
