package io.github.ikunkk02afk.liquidglassui.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.ikunkk02afk.liquidglassui.LiquidGlassUIClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractWidget.class)
abstract class AbstractWidgetMixin {
    @WrapOperation(
            method = "render",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/AbstractWidget;renderWidget(Lnet/minecraft/client/gui/GuiGraphics;IIF)V")
    )
    private void liquidGlassUI$renderWidget(AbstractWidget instance, GuiGraphics graphics, int mouseX, int mouseY,
                                            float delta, Operation<Void> original) {
        if (!LiquidGlassUIClient.tryRenderWidget(instance, graphics, mouseX, mouseY, delta)) {
            original.call(instance, graphics, mouseX, mouseY, delta);
        }
    }
}
