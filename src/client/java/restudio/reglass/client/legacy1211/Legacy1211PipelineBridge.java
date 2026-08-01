/*
 * Adapted from ReGlass by ReStudio / RedxAx.
 * Original project: https://github.com/RedxAx/ReGlass
 * Licensed under the MIT License.
 * Backported and modified for Minecraft Fabric 1.21.1.
 */
package restudio.reglass.client.legacy1211;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.ShaderInstance;

public final class Legacy1211PipelineBridge implements AutoCloseable {
    private VertexBuffer fullscreenQuad;

    public void drawFullscreen(ShaderInstance shader) {
        ensureQuad();
        shader.apply();
        fullscreenQuad.bind();
        fullscreenQuad.draw();
        VertexBuffer.unbind();
        shader.clear();
        BufferUploader.invalidate();
    }

    private void ensureQuad() {
        if (fullscreenQuad != null && !fullscreenQuad.isInvalid()) {
            return;
        }
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLIT_SCREEN);
        builder.addVertex(0.0f, 0.0f, 0.0f);
        builder.addVertex(1.0f, 0.0f, 0.0f);
        builder.addVertex(1.0f, 1.0f, 0.0f);
        builder.addVertex(0.0f, 1.0f, 0.0f);
        fullscreenQuad = new VertexBuffer(VertexBuffer.Usage.STATIC);
        fullscreenQuad.bind();
        fullscreenQuad.upload(builder.buildOrThrow());
        VertexBuffer.unbind();
    }

    @Override
    public void close() {
        if (fullscreenQuad != null) {
            fullscreenQuad.close();
            fullscreenQuad = null;
        }
    }
}
