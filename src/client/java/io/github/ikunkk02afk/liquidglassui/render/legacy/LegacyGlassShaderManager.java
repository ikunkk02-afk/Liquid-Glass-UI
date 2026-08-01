package io.github.ikunkk02afk.liquidglassui.render.legacy;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import io.github.ikunkk02afk.liquidglassui.LiquidGlassUIClient;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;

final class LegacyGlassShaderManager {
    private ShaderInstance blur;
    private ShaderInstance composite;
    private int generation;

    void register(Runnable reloadListener) {
        CoreShaderRegistrationCallback.EVENT.register(context -> {
            context.register(id("glass_blur"), DefaultVertexFormat.POSITION, shader -> {
                blur = shader;
                generation++;
                reloadListener.run();
            });
            context.register(id("glass_composite"), DefaultVertexFormat.POSITION, shader -> {
                composite = shader;
                generation++;
                reloadListener.run();
            });
        });
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(LiquidGlassUIClient.MOD_ID, path);
    }

    boolean ready() { return blur != null && composite != null; }
    ShaderInstance blur() { return blur; }
    ShaderInstance composite() { return composite; }
    int generation() { return generation; }
}
