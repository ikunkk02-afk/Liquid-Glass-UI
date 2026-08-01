/*
 * Adapted from ReGlass by ReStudio / RedxAx.
 * Original project: https://github.com/RedxAx/ReGlass
 * Licensed under the MIT License.
 * Backported and modified for Minecraft Fabric 1.21.1.
 */
package restudio.reglass.client.legacy1211;

import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

final class Legacy1211RenderStateGuard implements AutoCloseable {
    private final int framebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
    private final int program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
    private final int vertexArray = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
    private final int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
    private final boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
    private final boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
    private final boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
    private final boolean scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
    private final int[] viewport = new int[4];
    private final int[] textures = new int[12];

    Legacy1211RenderStateGuard() {
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        for (int i = 0; i < textures.length; i++) {
            textures[i] = RenderSystem.getShaderTexture(i);
        }
        GL13.glActiveTexture(activeTexture);
    }

    @Override
    public void close() {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
        GL20.glUseProgram(program);
        GL30.glBindVertexArray(vertexArray);
        GL11.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
        set(GL11.GL_BLEND, blend);
        set(GL11.GL_DEPTH_TEST, depth);
        set(GL11.GL_CULL_FACE, cull);
        set(GL11.GL_SCISSOR_TEST, scissor);
        for (int i = 0; i < textures.length; i++) {
            RenderSystem.setShaderTexture(i, textures[i]);
        }
        GL13.glActiveTexture(activeTexture);
    }

    private static void set(int capability, boolean enabled) {
        if (enabled) GL11.glEnable(capability); else GL11.glDisable(capability);
    }
}
