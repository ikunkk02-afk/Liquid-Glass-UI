package io.github.ikunkk02afk.liquidglassui.mixin.client;

import io.github.ikunkk02afk.liquidglassui.LiquidGlassUIClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
abstract class ScreenMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void liquidGlassUI$captureBackdropBeforeWidgets(GuiGraphics graphics, int mouseX, int mouseY,
                                                             float delta, CallbackInfo callback) {
        LiquidGlassUIClient.captureBackdrop((Screen) (Object) this, graphics);
    }

    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void liquidGlassUI$keepPauseBackdropClear(GuiGraphics graphics, int mouseX, int mouseY,
                                                       float delta, CallbackInfo callback) {
        if (LiquidGlassUIClient.shouldKeepPauseBackdropClear((Screen) (Object) this)) callback.cancel();
    }
}
