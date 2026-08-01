/*
 * Adapted from ReGlass by ReStudio / RedxAx.
 * Original project: https://github.com/RedxAx/ReGlass
 * Licensed under the MIT License.
 * Backported and modified for Minecraft Fabric 1.21.1.
 */
package restudio.reglass.mixin.logical;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import restudio.reglass.client.legacy1211.Legacy1211GuiHook;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void reglass$beginFrame(DeltaTracker tracker, boolean tick, CallbackInfo ci) {
        Legacy1211GuiHook.get().beginFrame(tracker.getRealtimeDeltaTicks() / 20.0);
    }
}
