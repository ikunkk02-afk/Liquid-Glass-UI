/*
 * Adapted from ReGlass by ReStudio / RedxAx.
 * Original project: https://github.com/RedxAx/ReGlass
 * Licensed under the MIT License.
 * Backported and modified for Minecraft Fabric 1.21.1.
 */
package restudio.reglass.mixin.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import restudio.reglass.client.legacy1211.Legacy1211GuiHook;

@Mixin(Screen.class)
public abstract class ScreenMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void reglass$beforeWidgets(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        Legacy1211GuiHook.get().beforeVanillaWidgetRender((Screen) (Object) this, graphics);
    }

    @Inject(method = "renderBlurredBackground", at = @At("HEAD"))
    private void reglass$markBlur(float delta, CallbackInfo ci) {
        Legacy1211GuiHook.get().markScreenWantsBlur();
    }
}
