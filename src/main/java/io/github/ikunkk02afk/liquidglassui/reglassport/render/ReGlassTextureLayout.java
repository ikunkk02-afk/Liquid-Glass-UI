package io.github.ikunkk02afk.liquidglassui.reglassport.render;

/** Fixed RGBA32F widget-data texture contract shared by Java packing and GLSL fetches. */
public final class ReGlassTextureLayout {
    public static final int WIDTH = 64;
    public static final int ROWS = 12;
    public static final int CHANNELS = 4;
    public static final int FLOAT_COUNT = WIDTH * ROWS * CHANNELS;

    private ReGlassTextureLayout() {
    }

    public static int offset(int column, int row) {
        if (column < 0 || column >= WIDTH || row < 0 || row >= ROWS) {
            throw new IndexOutOfBoundsException("column=" + column + ", row=" + row);
        }
        return (row * WIDTH + column) * CHANNELS;
    }
}
