package io.github.ikunkk02afk.liquidglassui.render;

public record GlassFrameContext(long frameId, int screenWidth, int screenHeight, int framebufferWidth,
                                int framebufferHeight, double guiScale, float mouseX, float mouseY,
                                GlassQualityBudget quality) {
}
