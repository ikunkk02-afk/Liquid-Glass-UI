package io.github.ikunkk02afk.liquidglassui.render.frame;

import io.github.ikunkk02afk.liquidglassui.render.material.GlassMaterial;

/** Mutable, reusable per-widget frame data. Coordinates are GUI logical units. */
public final class GlassWidgetData {
    public static final int SHAPE_ROUNDED_RECT = 0;
    public static final int SHAPE_CAPSULE = 1;

    public float x;
    public float y;
    public float width;
    public float height;
    public float cornerRadius;
    public float hover;
    public float press;
    public float focus;
    public float opacity;
    public int groupId;
    public int materialId;
    public int shape = SHAPE_ROUNDED_RECT;
    public float smoothing;
    public float mouseX;
    public float mouseY;
    public float scissorX;
    public float scissorY;
    public float scissorWidth;
    public float scissorHeight;
    public float capsuleStartX;
    public float capsuleStartY;
    public float capsuleEndX;
    public float capsuleEndY;
    public float capsuleRadius;
    public GlassMaterial material = GlassMaterial.defaults();

    public void reset() {
        x = y = width = height = cornerRadius = 0.0f;
        hover = press = focus = 0.0f;
        opacity = 1.0f;
        groupId = -1;
        materialId = 0;
        shape = SHAPE_ROUNDED_RECT;
        smoothing = 0.0f;
        mouseX = mouseY = 0.0f;
        scissorX = scissorY = scissorWidth = scissorHeight = 0.0f;
        capsuleStartX = capsuleStartY = capsuleEndX = capsuleEndY = capsuleRadius = 0.0f;
        material = GlassMaterial.defaults();
    }
}
