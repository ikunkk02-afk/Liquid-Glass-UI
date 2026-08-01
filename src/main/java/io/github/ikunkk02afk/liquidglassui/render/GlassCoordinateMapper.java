package io.github.ikunkk02afk.liquidglassui.render;

public record GlassCoordinateMapper(int guiWidth, int guiHeight, int framebufferWidth, int framebufferHeight) {
    public GlassCoordinateMapper {
        if (guiWidth <= 0 || guiHeight <= 0 || framebufferWidth <= 0 || framebufferHeight <= 0) {
            throw new IllegalArgumentException("GUI and framebuffer dimensions must be positive");
        }
    }

    public float scaleX() { return framebufferWidth / (float) guiWidth; }
    public float scaleY() { return framebufferHeight / (float) guiHeight; }

    public GlassRectangle toFramebuffer(GlassRectangle guiRectangle) {
        return new GlassRectangle(guiRectangle.x() * scaleX(), guiRectangle.y() * scaleY(),
                guiRectangle.width() * scaleX(), guiRectangle.height() * scaleY());
    }

    public float textureU(float guiX) {
        return clamp(guiX * scaleX() / framebufferWidth);
    }

    public float textureV(float guiY) {
        return clamp(1.0f - guiY * scaleY() / framebufferHeight);
    }

    private static float clamp(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
