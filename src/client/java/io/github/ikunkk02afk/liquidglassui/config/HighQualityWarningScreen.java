package io.github.ikunkk02afk.liquidglassui.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class HighQualityWarningScreen extends ConfirmScreen {
    public HighQualityWarningScreen(Screen parent, Runnable confirm) {
        super(accepted -> {
            if (accepted) confirm.run();
            Minecraft.getInstance().setScreen(parent);
        }, Component.translatable("liquid_glass_ui.high_quality_warning.title"),
                Component.translatable("liquid_glass_ui.high_quality_warning.body"),
                Component.translatable("liquid_glass_ui.high_quality_warning.enable"),
                Component.translatable("gui.cancel"));
    }
}
