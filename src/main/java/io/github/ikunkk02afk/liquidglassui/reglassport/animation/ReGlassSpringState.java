package io.github.ikunkk02afk.liquidglassui.reglassport.animation;

/** Bounded semi-implicit critical spring integrated in at most 1/120-second substeps. */
public final class ReGlassSpringState {
    private double value;
    private double velocity;
    private boolean initialized;

    public void snap(double target) {
        value = finite(target);
        velocity = 0.0;
        initialized = true;
    }

    public double step(double target, double deltaSeconds, double stiffness, double damping) {
        target = finite(target);
        if (!initialized) snap(target);
        double remaining = Math.max(0.0, Math.min(0.05, deltaSeconds));
        double safeStiffness = Math.max(1.0, stiffness);
        double safeDamping = Math.max(0.0, damping);
        while (remaining > 1e-9) {
            double step = Math.min(1.0 / 120.0, remaining);
            double acceleration = safeStiffness * (target - value) - safeDamping * velocity;
            velocity += acceleration * step;
            value += velocity * step;
            remaining -= step;
        }
        if (Math.abs(target - value) < 1e-5 && Math.abs(velocity) < 1e-4) snap(target);
        return value;
    }

    public double value() { return value; }
    public double velocity() { return velocity; }

    private static double finite(double value) {
        return Double.isFinite(value) ? value : 0.0;
    }
}
