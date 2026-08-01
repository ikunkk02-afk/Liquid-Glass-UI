package io.github.ikunkk02afk.liquidglassui.reglassport.widget;

/** Immutable per-frame interaction and motion state uploaded for one stable widget id. */
public record GlassWidgetState(float x, float y, float hover, float focus, float press,
                               float shapeExpansion, float fusion, float highlightX, float highlightY) {
}
