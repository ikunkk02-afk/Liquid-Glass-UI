/*
 * Adapted from ReGlass by ReStudio / RedxAx.
 * Original project: https://github.com/RedxAx/ReGlass
 * Licensed under the MIT License.
 * Backported and modified for Minecraft Fabric 1.21.1.
 */
package restudio.reglass.client;

import net.minecraft.client.renderer.ShaderInstance;
import restudio.reglass.client.legacy1211.Legacy1211ShaderManager;

public final class LiquidGlassPipelines {
    private LiquidGlassPipelines() {
    }

    public static ShaderInstance getGuiPipeline() {
        return Legacy1211ShaderManager.liquidGlassShader();
    }
}
