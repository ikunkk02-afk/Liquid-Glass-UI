package io.github.ikunkk02afk.liquidglassui.render.frame;

public final class GlassFrameCollector {
    private final GlassWidgetBuffer buffer = new GlassWidgetBuffer();
    private long frameId;
    private int count;
    private int capacity = GlassWidgetBuffer.ABSOLUTE_MAX_COMPONENTS;
    private boolean overflowed;

    public void begin(long frameId, int requestedCapacity) {
        buffer.reset(count);
        this.frameId = frameId;
        count = 0;
        overflowed = false;
        capacity = Math.max(1, Math.min(GlassWidgetBuffer.ABSOLUTE_MAX_COMPONENTS, requestedCapacity));
    }

    public GlassWidgetData add() {
        if (count >= capacity) {
            overflowed = true;
            return null;
        }
        GlassWidgetData widget = buffer.get(count++);
        widget.reset();
        return widget;
    }

    public GlassWidgetData get(int index) {
        if (index < 0 || index >= count) throw new IndexOutOfBoundsException(index);
        return buffer.get(index);
    }

    public int count() { return count; }
    public boolean overflowed() { return overflowed; }

    public GlassFrameState freeze() {
        return new GlassFrameState(frameId, buffer, count, overflowed);
    }
}
