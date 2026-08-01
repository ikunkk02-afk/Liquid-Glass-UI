package io.github.ikunkk02afk.liquidglassui.animation;

public final class GlassAnimationState {
    private final SpringFloat scale = new SpringFloat(0.96f);
    private final SpringFloat opacity = new SpringFloat(0.0f);
    private final SpringFloat hover = new SpringFloat(0.0f);
    private final SpringVector2 highlight = new SpringVector2(0.5f, 0.5f);

    public void update(double delta, boolean hovered, boolean pressed, float mouseX, float mouseY,
                       float hoverScale, float pressedScale, float stiffness, float damping, boolean reduceMotion) {
        scale.target(reduceMotion ? 1.0f : pressed ? pressedScale : hovered ? hoverScale : 1.0f);
        opacity.target(1.0f);
        hover.target(hovered ? 1.0f : 0.0f);
        if (!reduceMotion) highlight.target(mouseX, mouseY);
        scale.update(delta, stiffness, reduceMotion ? damping * 2.0f : damping);
        opacity.update(delta, stiffness * 0.55f, damping);
        hover.update(delta, stiffness * 0.75f, damping);
        highlight.update(delta, stiffness * 0.65f, damping);
    }

    public float scale() { return scale.value(); }
    public float opacity() { return opacity.value(); }
    public float hover() { return hover.value(); }
    public float highlightX() { return highlight.x(); }
    public float highlightY() { return highlight.y(); }
}
