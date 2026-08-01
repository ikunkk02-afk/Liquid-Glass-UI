package io.github.ikunkk02afk.liquidglassui.animation;

import io.github.ikunkk02afk.liquidglassui.render.GlassRectangle;

public final class GlassGroupMotion {
    private final SpringRectangle active;
    private GlassRectangle previous;
    private GlassRectangle target;
    private float transition;

    public GlassGroupMotion(GlassRectangle initial) {
        active = new SpringRectangle(initial);
        previous = initial;
        target = initial;
        transition = 1.0f;
    }

    public void target(GlassRectangle next) {
        previous = active.value();
        target = next;
        active.target(next);
        transition = 0.0f;
    }

    public void update(double delta, float stiffness, float damping, float speed, boolean reduceMotion) {
        if (reduceMotion) {
            active.snap(target);
            previous = target;
            transition = 1.0f;
            return;
        }
        active.update(delta * speed, stiffness, damping);
        transition = Math.min(1.0f, transition + (float) (delta * speed * 5.0));
        if (transition >= 0.999f) previous = target;
    }

    public GlassRectangle active() { return active.value(); }
    public GlassRectangle previous() { return previous; }
    public float mergeAmount(float configuredStrength) {
        return Math.max(0.0f, Math.min(1.0f, configuredStrength)) * (1.0f - transition);
    }
}
