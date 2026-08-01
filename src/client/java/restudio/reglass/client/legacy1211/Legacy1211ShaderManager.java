/*
 * Adapted from ReGlass by ReStudio / RedxAx.
 * Original project: https://github.com/RedxAx/ReGlass
 * Licensed under the MIT License.
 * Backported and modified for Minecraft Fabric 1.21.1.
 */
package restudio.reglass.client.legacy1211;

import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import restudio.reglass.ReGlass;

public final class Legacy1211ShaderManager {
    private static ShaderInstance liquidGlassShader;
    private static ShaderInstance blurShader;
    private static boolean registered;

    private Legacy1211ShaderManager() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        CoreShaderRegistrationCallback.EVENT.register(context -> {
            context.register(id("liquid_glass_gui"), DefaultVertexFormat.POSITION, shader -> {
                liquidGlassShader = shader;
                ReGlass.LOGGER.info("Loaded ReGlass 1.21.1 liquid glass shader");
            });
            context.register(id("blur"), DefaultVertexFormat.POSITION, shader -> {
                blurShader = shader;
                ReGlass.LOGGER.info("Loaded ReGlass 1.21.1 Gaussian blur shader");
            });
        });
    }

    public static ShaderInstance liquidGlassShader() {
        return liquidGlassShader;
    }

    public static ShaderInstance blurShader() {
        return blurShader;
    }

    public static boolean ready() {
        return liquidGlassShader != null && blurShader != null;
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("reglass", path);
    }
}
