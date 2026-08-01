/*
 * Adapted from ReGlass by ReStudio / RedxAx.
 * Original project: https://github.com/RedxAx/ReGlass
 * Licensed under the MIT License.
 * Backported and modified for Minecraft Fabric 1.21.1.
 */
package restudio.reglass.client.legacy1211;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import restudio.reglass.client.gui.LiquidGlassGuiElementRenderState;

public final class Legacy1211WidgetCollector {
    public static final int MAX_WIDGETS = 64;
    public static final int MAX_BLUR_LEVELS = 5;

    private static final Legacy1211WidgetCollector INSTANCE = new Legacy1211WidgetCollector();

    private final List<LiquidGlassGuiElementRenderState> widgets = new ArrayList<>(MAX_WIDGETS);
    private boolean compositeRequested;

    private Legacy1211WidgetCollector() {
    }

    public static Legacy1211WidgetCollector get() {
        return INSTANCE;
    }

    public void beginFrame() {
        widgets.clear();
        compositeRequested = false;
    }

    public void addWidget(LiquidGlassGuiElementRenderState element) {
        if (widgets.size() < MAX_WIDGETS) {
            widgets.add(element);
            compositeRequested = true;
        }
    }

    public void requestComposite() {
        compositeRequested = true;
    }

    public boolean isCompositeRequested() {
        return compositeRequested && !widgets.isEmpty();
    }

    public List<LiquidGlassGuiElementRenderState> widgets() {
        return Collections.unmodifiableList(widgets);
    }
}
