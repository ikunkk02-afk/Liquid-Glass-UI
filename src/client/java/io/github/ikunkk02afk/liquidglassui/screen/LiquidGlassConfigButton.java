package io.github.ikunkk02afk.liquidglassui.screen;

import io.github.ikunkk02afk.liquidglassui.config.LiquidGlassConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class LiquidGlassConfigButton extends Button {
    public LiquidGlassConfigButton(Screen parent) {
        super(6, Math.max(6, parent.height - 26), 150, DEFAULT_HEIGHT,
                Component.translatable("liquid_glass_ui.open_settings"),
                button -> Minecraft.getInstance().setScreen(LiquidGlassConfigScreen.create(parent)), DEFAULT_NARRATION);
    }
}
