package io.github.ikunkk02afk.liquidglassui.render;

import io.github.ikunkk02afk.liquidglassui.render.frame.GlassFrameState;

public interface GlassRenderBackend extends AutoCloseable {
    void beginFrame(GlassFrameContext context);
    boolean captureBackdrop();
    boolean composite(GlassFrameState frame);
    void endFrame();
    void onResourceReload();
    GlassBackendStatus status();
    @Override void close();
}
