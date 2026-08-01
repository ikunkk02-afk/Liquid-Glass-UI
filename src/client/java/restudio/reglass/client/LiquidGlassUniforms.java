/*
 * Adapted from ReGlass by ReStudio / RedxAx.
 * Original project: https://github.com/RedxAx/ReGlass
 * Licensed under the MIT License.
 * Backported and modified for Minecraft Fabric 1.21.1.
 */
package restudio.reglass.client;

import net.minecraft.client.gui.GuiGraphics;
import restudio.reglass.client.gui.LiquidGlassGuiElementRenderState;
import restudio.reglass.client.legacy1211.Legacy1211GuiHook;
import restudio.reglass.client.legacy1211.Legacy1211WidgetCollector;

public final class LiquidGlassUniforms {
    public static final int MAX_WIDGETS = 64;
    public static final int MAX_BLUR_LEVELS = 5;
    private static final LiquidGlassUniforms INSTANCE = new LiquidGlassUniforms();

    private LiquidGlassUniforms() {
    }

    public static LiquidGlassUniforms get() {
        return INSTANCE;
    }

    public void beginFrame(double dtSeconds) {
        Legacy1211GuiHook.get().beginFrame(dtSeconds);
    }

    public void setScreenWantsBlur(boolean wantsBlur) {
        if (wantsBlur) Legacy1211GuiHook.get().markScreenWantsBlur();
    }

    public void tryApplyBlur(GuiGraphics context) {
        Legacy1211WidgetCollector.get().requestComposite();
    }

    public void addWidget(LiquidGlassGuiElementRenderState element) {
        Legacy1211WidgetCollector.get().addWidget(element);
    }
}
