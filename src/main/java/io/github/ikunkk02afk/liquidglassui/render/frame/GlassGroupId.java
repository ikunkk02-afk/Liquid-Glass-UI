package io.github.ikunkk02afk.liquidglassui.render.frame;

public record GlassGroupId(int value) {
    public static final GlassGroupId NONE = new GlassGroupId(-1);

    public GlassGroupId {
        if (value < -1) throw new IllegalArgumentException("Group id must be -1 or greater");
    }
}
