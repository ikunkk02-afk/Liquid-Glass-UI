/*
 * Portions adapted from ReGlass by ReStudio / RedxAx.
 * Original project: https://github.com/RedxAx/ReGlass
 * Licensed under the MIT License.
 * Adapted for Minecraft Fabric 1.21.1.
 */
package io.github.ikunkk02afk.liquidglassui.reglassport.render;

import java.util.ArrayList;
import java.util.List;

/** Fixed-capacity, render-thread-only collector matching ReGlass's 64-widget budget. */
public final class ReGlassFrameCollector {
    public static final int MAX_WIDGETS = 64;
    private final ArrayList<ReGlassUniformData> widgets = new ArrayList<>(MAX_WIDGETS);
    private boolean active;

    public void beginFrame() {
        widgets.clear();
        active = true;
    }

    public boolean submit(ReGlassUniformData data) {
        if (!active || data == null || data.width() <= 0.0f || data.height() <= 0.0f
                || widgets.size() >= MAX_WIDGETS) return false;
        widgets.add(data);
        return true;
    }

    public List<ReGlassUniformData> finishFrame() {
        active = false;
        return List.copyOf(widgets);
    }

    public boolean active() { return active; }
    public int size() { return widgets.size(); }
}
