/*
 * Adapted from ReGlass by ReStudio / RedxAx.
 * Original project: https://github.com/RedxAx/ReGlass
 * Licensed under the MIT License.
 * Backported and modified for Minecraft Fabric 1.21.1.
 */
package restudio.reglass.client.legacy1211;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import restudio.reglass.client.LiquidGlassWidget;
import restudio.reglass.client.api.ReGlassConfig;
import restudio.reglass.client.runtime.ReGlassAnim;

public final class Legacy1211GuiHook {
    private static final Legacy1211GuiHook INSTANCE = new Legacy1211GuiHook();

    private final Legacy1211CompositePass composite = new Legacy1211CompositePass();
    private double dtSeconds;
    private boolean screenWantsBlur;
    private boolean composited;

    private Legacy1211GuiHook() {
    }

    public static Legacy1211GuiHook get() {
        return INSTANCE;
    }

    public void beginFrame(double dtSeconds) {
        this.dtSeconds = Math.max(0.0, dtSeconds);
        this.screenWantsBlur = false;
        this.composited = false;
        Legacy1211WidgetCollector.get().beginFrame();
        ReGlassAnim.INSTANCE.update(ReGlassConfig.INSTANCE, this.dtSeconds);
    }

    public void markScreenWantsBlur() {
        screenWantsBlur = true;
    }

    public void beforeVanillaWidgetRender(Screen screen, GuiGraphics graphics) {
        if (composited || !Legacy1211ShaderManager.ready()) return;
        for (GuiEventListener child : screen.children()) {
            if (child instanceof LiquidGlassWidget glassWidget && glassWidget.visible) {
                glassWidget.submitGlass(graphics);
            }
        }
        Legacy1211WidgetCollector collector = Legacy1211WidgetCollector.get();
        if (!collector.isCompositeRequested()) return;
        graphics.flush();
        composited = composite.render(collector, dtSeconds, screenWantsBlur);
    }

    public boolean wasComposited() {
        return composited;
    }
}
