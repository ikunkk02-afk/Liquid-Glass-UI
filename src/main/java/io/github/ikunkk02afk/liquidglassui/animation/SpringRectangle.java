package io.github.ikunkk02afk.liquidglassui.animation;

import io.github.ikunkk02afk.liquidglassui.render.GlassRectangle;

public final class SpringRectangle {
    private final SpringFloat x;
    private final SpringFloat y;
    private final SpringFloat width;
    private final SpringFloat height;

    public SpringRectangle(GlassRectangle initial) {
        x = new SpringFloat(initial.x());
        y = new SpringFloat(initial.y());
        width = new SpringFloat(initial.width());
        height = new SpringFloat(initial.height());
    }

    public void target(GlassRectangle rectangle) {
        x.target(rectangle.x()); y.target(rectangle.y()); width.target(rectangle.width()); height.target(rectangle.height());
    }

    public void update(double delta, float stiffness, float damping) {
        x.update(delta, stiffness, damping); y.update(delta, stiffness, damping);
        width.update(delta, stiffness, damping); height.update(delta, stiffness, damping);
    }

    public void snap(GlassRectangle rectangle) {
        x.snap(rectangle.x()); y.snap(rectangle.y()); width.snap(rectangle.width()); height.snap(rectangle.height());
    }

    public GlassRectangle value() { return new GlassRectangle(x.value(), y.value(), width.value(), height.value()); }
}
