package io.github.ikunkk02afk.liquidglassui.animation;

public final class SpringVector2 {
    private final SpringFloat x;
    private final SpringFloat y;

    public SpringVector2(float x, float y) {
        this.x = new SpringFloat(x);
        this.y = new SpringFloat(y);
    }

    public void target(float x, float y) { this.x.target(x); this.y.target(y); }
    public void update(double delta, float stiffness, float damping) { x.update(delta, stiffness, damping); y.update(delta, stiffness, damping); }
    public void snap(float x, float y) { this.x.snap(x); this.y.snap(y); }
    public float x() { return x.value(); }
    public float y() { return y.value(); }
}
