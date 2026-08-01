package io.github.ikunkk02afk.liquidglassui.render;

public record GlassSurface(GlassRectangle bounds, GlassRectangle previousBounds, float merge,
                           float cornerRadius, float scale, float opacity, float hover,
                           float highlightX, float highlightY, boolean active) {
}
