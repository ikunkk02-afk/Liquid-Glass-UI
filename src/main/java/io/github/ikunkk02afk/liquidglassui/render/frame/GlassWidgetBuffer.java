package io.github.ikunkk02afk.liquidglassui.render.frame;

public final class GlassWidgetBuffer {
    public static final int ABSOLUTE_MAX_COMPONENTS = 64;
    private final GlassWidgetData[] widgets = new GlassWidgetData[ABSOLUTE_MAX_COMPONENTS];

    public GlassWidgetBuffer() {
        for (int i = 0; i < widgets.length; i++) widgets[i] = new GlassWidgetData();
    }

    public GlassWidgetData get(int index) {
        if (index < 0 || index >= widgets.length) throw new IndexOutOfBoundsException(index);
        return widgets[index];
    }

    public void reset(int count) {
        for (int i = 0; i < count; i++) widgets[i].reset();
    }
}
