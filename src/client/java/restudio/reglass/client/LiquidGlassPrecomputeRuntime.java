/*
 * Adapted from ReGlass by ReStudio / RedxAx.
 * Original project: https://github.com/RedxAx/ReGlass
 * Licensed under the MIT License.
 * Backported and modified for Minecraft Fabric 1.21.1.
 */
package restudio.reglass.client;

import java.util.ArrayList;
import java.util.List;

public final class LiquidGlassPrecomputeRuntime {
    private static final LiquidGlassPrecomputeRuntime INSTANCE = new LiquidGlassPrecomputeRuntime();
    private List<Integer> requestedRadii = new ArrayList<>();

    private LiquidGlassPrecomputeRuntime() {
    }

    public static LiquidGlassPrecomputeRuntime get() {
        return INSTANCE;
    }

    public void setRequestedRadii(List<Integer> ordered) {
        requestedRadii = new ArrayList<>(ordered);
    }

    public List<Integer> getRequestedRadii() {
        return List.copyOf(requestedRadii);
    }
}
