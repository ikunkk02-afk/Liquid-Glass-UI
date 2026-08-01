package io.github.ikunkk02afk.liquidglassui.render;

public record GlassRectangle(float x, float y, float width, float height) {
    public GlassRectangle {
        width = Math.max(0.0f, width);
        height = Math.max(0.0f, height);
    }

    public float centerX() { return x + width * 0.5f; }
    public float centerY() { return y + height * 0.5f; }

    public GlassRectangle scaled(float scale) {
        float nextWidth = width * scale;
        float nextHeight = height * scale;
        return new GlassRectangle(centerX() - nextWidth * 0.5f, centerY() - nextHeight * 0.5f, nextWidth, nextHeight);
    }
}
