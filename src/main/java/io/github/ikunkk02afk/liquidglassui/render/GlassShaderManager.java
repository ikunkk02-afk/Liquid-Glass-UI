package io.github.ikunkk02afk.liquidglassui.render;

public final class GlassShaderManager {
    private int resourceGeneration;

    public int generation() { return resourceGeneration; }
    public void reloaded() { resourceGeneration++; }
}
