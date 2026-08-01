package io.github.ikunkk02afk.liquidglassui.render.legacy;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.ShaderInstance;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;

final class LegacyRenderStateGuard implements AutoCloseable {
    private final int readFramebuffer;
    private final int drawFramebuffer;
    private final int[] viewport = new int[4];
    private final int[] scissorBox = new int[4];
    private final int activeTexture;
    private final int pixelUnpackBuffer;
    private final int unpackAlignment;
    private final int unpackRowLength;
    private final int unpackSkipRows;
    private final int unpackSkipPixels;
    private final int[] textures = new int[4];
    private final ShaderInstance shader;
    private final boolean blend;
    private final boolean depth;
    private final boolean scissor;
    private final boolean cull;
    private final boolean[] colorMask = new boolean[4];
    private final int blendSrcRgb;
    private final int blendDstRgb;
    private final int blendSrcAlpha;
    private final int blendDstAlpha;
    private final int blendEquationRgb;
    private final int blendEquationAlpha;

    private LegacyRenderStateGuard() {
        readFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        drawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, scissorBox);
        activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        pixelUnpackBuffer = GL11.glGetInteger(GL21.GL_PIXEL_UNPACK_BUFFER_BINDING);
        unpackAlignment = GL11.glGetInteger(GL11.GL_UNPACK_ALIGNMENT);
        unpackRowLength = GL11.glGetInteger(GL11.GL_UNPACK_ROW_LENGTH);
        unpackSkipRows = GL11.glGetInteger(GL11.GL_UNPACK_SKIP_ROWS);
        unpackSkipPixels = GL11.glGetInteger(GL11.GL_UNPACK_SKIP_PIXELS);
        for (int i = 0; i < textures.length; i++) {
            RenderSystem.activeTexture(GL13.GL_TEXTURE0 + i);
            textures[i] = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        }
        RenderSystem.activeTexture(activeTexture);
        shader = RenderSystem.getShader();
        blend = GL11.glIsEnabled(GL11.GL_BLEND);
        depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        var mask = BufferUtils.createByteBuffer(4);
        GL11.glGetBooleanv(GL11.GL_COLOR_WRITEMASK, mask);
        for (int i = 0; i < colorMask.length; i++) colorMask[i] = mask.get(i) != 0;
        blendSrcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
        blendDstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        blendSrcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
        blendDstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        blendEquationRgb = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_RGB);
        blendEquationAlpha = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_ALPHA);
    }

    static LegacyRenderStateGuard capture() { return new LegacyRenderStateGuard(); }

    @Override
    public void close() {
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFramebuffer);
        RenderSystem.viewport(viewport[0], viewport[1], viewport[2], viewport[3]);
        GL11.glScissor(scissorBox[0], scissorBox[1], scissorBox[2], scissorBox[3]);
        for (int i = 0; i < textures.length; i++) {
            RenderSystem.activeTexture(GL13.GL_TEXTURE0 + i);
            RenderSystem.setShaderTexture(i, textures[i]);
            RenderSystem.bindTexture(textures[i]);
        }
        RenderSystem.activeTexture(activeTexture);
        GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, pixelUnpackBuffer);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, unpackAlignment);
        GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, unpackRowLength);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, unpackSkipRows);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, unpackSkipPixels);
        if (shader != null) RenderSystem.setShader(() -> shader);
        setEnabled(GL11.GL_BLEND, blend);
        setEnabled(GL11.GL_DEPTH_TEST, depth);
        setEnabled(GL11.GL_SCISSOR_TEST, scissor);
        setEnabled(GL11.GL_CULL_FACE, cull);
        GL14.glBlendFuncSeparate(blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha);
        GL20.glBlendEquationSeparate(blendEquationRgb, blendEquationAlpha);
        RenderSystem.colorMask(colorMask[0], colorMask[1], colorMask[2], colorMask[3]);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private static void setEnabled(int capability, boolean enabled) {
        if (enabled) GL11.glEnable(capability);
        else GL11.glDisable(capability);
    }
}
