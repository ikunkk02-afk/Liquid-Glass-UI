package io.github.ikunkk02afk.liquidglassui.render.frame;

public record GlassFrameState(long frameId, GlassWidgetBuffer buffer, int widgetCount, boolean overflowed) {
    public GlassWidgetData widget(int index) {
        if (index < 0 || index >= widgetCount) throw new IndexOutOfBoundsException(index);
        return buffer.get(index);
    }
}
