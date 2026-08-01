package io.github.ikunkk02afk.liquidglassui.render;

import io.github.ikunkk02afk.liquidglassui.config.GlassDebugView;

public record GlassFrameContext(long frameId, int screenWidth, int screenHeight, int framebufferWidth,
                                int framebufferHeight, double guiScale, float mouseX, float mouseY,
                                GlassQualityBudget quality, GlassDebugView debugView, float blurRadius) {
    public GlassFrameContext(long frameId, int screenWidth, int screenHeight, int framebufferWidth,
                             int framebufferHeight, double guiScale, float mouseX, float mouseY,
                             GlassQualityBudget quality) {
        this(frameId, screenWidth, screenHeight, framebufferWidth, framebufferHeight, guiScale,
                mouseX, mouseY, quality, GlassDebugView.OFF, 6.0f);
    }
}
