/*
 * Adapted from ReGlass by ReStudio / RedxAx.
 * Original project: https://github.com/RedxAx/ReGlass
 * Licensed under the MIT License.
 * Backported and modified for Minecraft Fabric 1.21.1.
 */
package restudio.reglass.client.legacy1211;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

public final class Legacy1211FramebufferManager implements AutoCloseable {
    private TextureTarget snapshot;
    private TextureTarget blurTemp;
    private final Map<Integer, TextureTarget> blurredByRadius = new HashMap<>();
    private int width;
    private int height;

    public void ensureSize(int requestedWidth, int requestedHeight) {
        if (requestedWidth == width && requestedHeight == height && snapshot != null) {
            return;
        }
        close();
        width = requestedWidth;
        height = requestedHeight;
        snapshot = target(width, height);
        blurTemp = target(width, height);
    }

    public void captureMain(RenderTarget main) {
        ensureSize(main.width, main.height);
        int oldRead = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int oldDraw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, snapshot.frameBufferId);
        GL30.glBlitFramebuffer(0, 0, main.width, main.height, 0, 0, width, height, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, oldRead);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, oldDraw);
    }

    public RenderTarget snapshot() {
        return snapshot;
    }

    public RenderTarget blurTemp() {
        return blurTemp;
    }

    public RenderTarget blurred(int radius) {
        return blurredByRadius.computeIfAbsent(radius, ignored -> target(width, height));
    }

    private static TextureTarget target(int width, int height) {
        TextureTarget target = new TextureTarget(width, height, false, Minecraft.ON_OSX);
        target.setFilterMode(GL11.GL_LINEAR);
        target.setClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        return target;
    }

    @Override
    public void close() {
        if (snapshot != null) snapshot.destroyBuffers();
        if (blurTemp != null) blurTemp.destroyBuffers();
        blurredByRadius.values().forEach(RenderTarget::destroyBuffers);
        blurredByRadius.clear();
        snapshot = null;
        blurTemp = null;
        width = 0;
        height = 0;
    }
}
