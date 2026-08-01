package io.github.ikunkk02afk.liquidglassui.render;

public final class GlassFramebufferManager {
    private int width = -1;
    private int height = -1;
    private double guiScale = -1.0;
    private float qualityScale = -1.0f;
    private int resourceGeneration = -1;

    public boolean needsRebuild(GlassFrameContext frame, int generation) {
        return width != frame.framebufferWidth() || height != frame.framebufferHeight()
                || Double.compare(guiScale, frame.guiScale()) != 0
                || Float.compare(qualityScale, frame.quality().bufferScale()) != 0
                || resourceGeneration != generation;
    }

    public void markBuilt(GlassFrameContext frame, int generation) {
        width = frame.framebufferWidth();
        height = frame.framebufferHeight();
        guiScale = frame.guiScale();
        qualityScale = frame.quality().bufferScale();
        resourceGeneration = generation;
    }

    public void invalidate() {
        width = -1;
        height = -1;
        guiScale = -1.0;
        qualityScale = -1.0f;
        resourceGeneration = -1;
    }
}
