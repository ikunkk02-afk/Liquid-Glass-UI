package io.github.ikunkk02afk.liquidglassui.animation;

public final class SpringFloat {
    private float value;
    private float velocity;
    private float target;

    public SpringFloat(float initial) {
        value = initial;
        target = initial;
    }

    public void update(double deltaSeconds, float stiffness, float damping) {
        double remaining = Math.max(0.0, deltaSeconds);
        while (remaining > 0.0) {
            double step = Math.min(remaining, AnimationClock.MAX_SUBSTEP_SECONDS);
            float acceleration = stiffness * (target - value) - damping * velocity;
            velocity += acceleration * (float) step;
            value += velocity * (float) step;
            remaining -= step;
        }
        if (Math.abs(target - value) < 0.0001f && Math.abs(velocity) < 0.0001f) snap(target);
    }

    public void target(float target) {
        this.target = target;
    }

    public void snap(float value) {
        this.value = value;
        this.target = value;
        this.velocity = 0.0f;
    }

    public float value() { return value; }
    public float velocity() { return velocity; }
    public float target() { return target; }
}
