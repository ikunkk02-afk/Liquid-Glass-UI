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

    @Inject(method = "render", at = @At("RETURN"))
    private void liquidGlassUI$compositeBeforeSubclassContents(GuiGraphics graphics, int mouseX, int mouseY,
                                                                float delta, CallbackInfo callback) {
        LiquidGlassUIClient.compositeAndRenderContents((Screen) (Object) this, graphics);
    }

    @Inject(method = "renderBlurredBackground", at = @At("HEAD"), cancellable = true)
    private void liquidGlassUI$disableVanillaFullScreenBlur(float delta, CallbackInfo callback) {
        if (LiquidGlassUIClient.shouldCancelVanillaScreenBlur((Screen) (Object) this)) callback.cancel();
    }
}
