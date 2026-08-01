package io.github.ikunkk02afk.liquidglassui.reglassport.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;

/** Independent 1.21.1 render-target lifecycle for the port. */
public final class ReGlassFramebufferManager implements AutoCloseable {
    private TextureTarget capture;
    private int width = -1;
    private int height = -1;
    private int generation = -1;

    public void ensure(int framebufferWidth, int framebufferHeight, int resourceGeneration) {
        if (capture != null && width == framebufferWidth && height == framebufferHeight
                && generation == resourceGeneration) return;
        close();
        width = framebufferWidth;
        height = framebufferHeight;
        generation = resourceGeneration;
        capture = create(framebufferWidth, framebufferHeight);
    }

    private static TextureTarget create(int width, int height) {
        TextureTarget target = new TextureTarget(width, height, false, Minecraft.ON_OSX);
        configureColorTexture(target.getColorTextureId(), GL11.GL_LINEAR);
        return target;
    }

    static void configureColorTexture(int texture, int filter) {
        int previousActive = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        RenderSystem.activeTexture(GL13.GL_TEXTURE0);
        int previousTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        RenderSystem.bindTextureForSetup(texture);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, filter);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, filter);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_BASE_LEVEL, 0);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, 0);
        RenderSystem.bindTextureForSetup(previousTexture);
        RenderSystem.activeTexture(previousActive);
    }

    public TextureTarget capture() { return capture; }

    public void restoreMainTarget() {
        RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
        main.bindWrite(true);
        RenderSystem.viewport(0, 0, main.viewWidth, main.viewHeight);
    }

    public void invalidate() { generation = -1; }

    @Override
    public void close() {
        if (capture != null) capture.destroyBuffers();
        capture = null;
        width = height = generation = -1;
    }
}
