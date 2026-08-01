/*
 * Adapted from ReGlass by ReStudio / RedxAx.
 * Original project: https://github.com/RedxAx/ReGlass
 * Licensed under the MIT License.
 * Backported and modified for Minecraft Fabric 1.21.1.
 */
package restudio.reglass.client.legacy1211;

public final class ReGlassStd140Layout {
    public static final int MAX_WIDGETS = 64;
    public static final int MAX_BLUR_LEVELS = 5;

    public static final int SAMPLER_INFO_SIZE = 16;
    public static final int CUSTOM_UNIFORMS_SIZE = 128;
    public static final int BG_CONFIG_SIZE = 16;
    public static final int WIDGET_INFO_SIZE = 16 + MAX_WIDGETS * (16 * 12);

    public static final int COUNT_OFFSET = 0;
    public static final int RECTS_OFFSET = arrayOffset(0);
    public static final int RADS_OFFSET = arrayOffset(1);
    public static final int TINTS_OFFSET = arrayOffset(2);
    public static final int OPTICS0_OFFSET = arrayOffset(3);
    public static final int OPTICS1_OFFSET = arrayOffset(4);
    public static final int OPTICS2_OFFSET = arrayOffset(5);
    public static final int SMOOTHINGS_OFFSET = arrayOffset(6);
    public static final int SCISSOR_RECTS_OFFSET = arrayOffset(7);
    public static final int SHADOW0_OFFSET = arrayOffset(8);
    public static final int SHADOW_COLOR_OFFSET = arrayOffset(9);
    public static final int EXTRA0_OFFSET = arrayOffset(10);

    private ReGlassStd140Layout() {
    }

    public static int arrayOffset(int arrayIndex) {
        return 16 + arrayIndex * MAX_WIDGETS * 16;
    }

    public static int elementOffset(int arrayOffset, int widgetIndex) {
        if (widgetIndex < 0 || widgetIndex >= MAX_WIDGETS) {
            throw new IndexOutOfBoundsException("widgetIndex=" + widgetIndex);
        }
        return arrayOffset + widgetIndex * 16;
    }
}
