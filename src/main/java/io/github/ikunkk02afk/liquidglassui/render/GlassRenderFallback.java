package io.github.ikunkk02afk.liquidglassui.render;

public final class GlassRenderFallback {
    public int panelColor(boolean active, float opacity) {
        int alpha = Math.max(0, Math.min(255, Math.round(opacity * (active ? 120 : 72))));
        return alpha << 24 | 0xDDE7F2;
    }

    public int edgeColor(boolean active, float hover) {
        int alpha = Math.max(0, Math.min(255, Math.round((active ? 76 : 42) + hover * 50)));
        return alpha << 24 | 0xFFFFFF;
    }
}
