package io.github.ikunkk02afk.liquidglassui.render.legacy;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.ikunkk02afk.liquidglassui.render.GlassFrameContext;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;

final class LegacyFramebufferManager implements AutoCloseable {
    private TextureTarget capture;
    private TextureTarget blurA;
    private TextureTarget blurB;
    private TextureTarget lightBlur;
    private int framebufferWidth = -1;
    private int framebufferHeight = -1;
    private int blurWidth = -1;
    private int blurHeight = -1;
    private int resourceGeneration = -1;

    boolean ensure(GlassFrameContext frame, int generation) {
        int targetWidth = frame.quality().targetWidth(frame.framebufferWidth());
        int targetHeight = frame.quality().targetHeight(frame.framebufferHeight());
        if (capture != null && framebufferWidth == frame.framebufferWidth()
                && framebufferHeight == frame.framebufferHeight() && blurWidth == targetWidth
                && blurHeight == targetHeight && resourceGeneration == generation) return false;
        close();
        framebufferWidth = frame.framebufferWidth();
        framebufferHeight = frame.framebufferHeight();
        blurWidth = targetWidth;
        blurHeight = targetHeight;
        resourceGeneration = generation;
        capture = create(framebufferWidth, framebufferHeight);
        blurA = create(blurWidth, blurHeight);
        blurB = create(blurWidth, blurHeight);
        lightBlur = create(blurWidth, blurHeight);
        return true;
    }

    private static TextureTarget create(int width, int height) {
        TextureTarget target = new TextureTarget(width, height, false, Minecraft.ON_OSX);
        configureColorTexture(target.getColorTextureId());
        return target;
    }

    private static void configureColorTexture(int textureId) {
        int previousActive = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        RenderSystem.activeTexture(GL13.GL_TEXTURE0);
        int previousTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        RenderSystem.bindTextureForSetup(textureId);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_BASE_LEVEL, 0);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, 0);
        RenderSystem.bindTextureForSetup(previousTexture);
        RenderSystem.activeTexture(previousActive);
    }

    TextureTarget capture() { return capture; }
    TextureTarget blurA() { return blurA; }
    TextureTarget blurB() { return blurB; }
    TextureTarget lightBlur() { return lightBlur; }
    int blurWidth() { return blurWidth; }
    int blurHeight() { return blurHeight; }

    void restoreMainTarget() {
        RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
        main.bindWrite(true);
        RenderSystem.viewport(0, 0, main.viewWidth, main.viewHeight);
    }

    void invalidate() {
        resourceGeneration = -1;
    }

    @Override
    public void close() {
        if (capture != null) capture.destroyBuffers();
        if (blurA != null) blurA.destroyBuffers();
        if (blurB != null) blurB.destroyBuffers();
        if (lightBlur != null) lightBlur.destroyBuffers();
        capture = blurA = blurB = lightBlur = null;
    }
}
