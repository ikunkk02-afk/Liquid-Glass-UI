package io.github.ikunkk02afk.liquidglassui.render;

public interface GlassRenderBackend extends AutoCloseable {
    void beginFrame(GlassFrameContext context);
    boolean render(GlassSurface surface);
    void endFrame();
    void onResourceReload();
    GlassBackendStatus status();
    @Override void close();
}
