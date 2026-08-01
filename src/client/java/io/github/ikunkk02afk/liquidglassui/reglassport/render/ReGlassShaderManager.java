package io.github.ikunkk02afk.liquidglassui.reglassport.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.io.IOException;

/** Owns only the 1.21.1 shader handles used by the ReGlass port. */
public final class ReGlassShaderManager {
    private final Logger logger;
    private ShaderInstance composite;
    private ShaderInstance blur;
    private int generation;
    private boolean failureLogged;

    public ReGlassShaderManager(Logger logger) {
        this.logger = logger;
    }

    public void register(Runnable reloadListener) {
        CoreShaderRegistrationCallback.EVENT.register(context -> {
            try {
                context.register(ResourceLocation.fromNamespaceAndPath("liquid_glass_ui", "reglass_port/blur"),
                        DefaultVertexFormat.POSITION, shader -> {
                            blur = shader;
                            generation++;
                            failureLogged = false;
                            reloadListener.run();
                        });
                context.register(ResourceLocation.fromNamespaceAndPath("liquid_glass_ui", "reglass_port/liquid_glass_gui"),
                        DefaultVertexFormat.POSITION, shader -> {
                            composite = shader;
                            generation++;
                            failureLogged = false;
                            reloadListener.run();
                        });
            } catch (IOException | RuntimeException exception) {
                composite = null;
                blur = null;
                generation++;
                if (!failureLogged) {
                    failureLogged = true;
                    logger.error("ReGlass Port shader failed to load; the port stays disabled for this resource generation", exception);
                }
                reloadListener.run();
            }
        });
    }

    public boolean ready() { return composite != null && blur != null; }
    public ShaderInstance composite() { return composite; }
    public ShaderInstance blur() { return blur; }
    public int generation() { return generation; }
}
