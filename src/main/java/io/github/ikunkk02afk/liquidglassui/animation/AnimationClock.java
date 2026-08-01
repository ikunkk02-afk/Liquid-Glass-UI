package io.github.ikunkk02afk.liquidglassui.animation;

import java.util.function.LongSupplier;

public final class AnimationClock {
    public static final double MAX_DELTA_SECONDS = 0.050;
    public static final double DISCONTINUITY_SECONDS = 0.250;
    public static final double MAX_SUBSTEP_SECONDS = 1.0 / 120.0;

    private final LongSupplier nanoTime;
    private long previousNanos = Long.MIN_VALUE;

    public AnimationClock() {
        this(System::nanoTime);
    }

    public AnimationClock(LongSupplier nanoTime) {
        this.nanoTime = nanoTime;
    }

    public double tick(boolean focused) {
        long now = nanoTime.getAsLong();
        if (previousNanos == Long.MIN_VALUE) {
            previousNanos = now;
            return 0.0;
        }
        double delta = Math.max(0.0, (now - previousNanos) / 1_000_000_000.0);
        previousNanos = now;
        if (!focused || delta > DISCONTINUITY_SECONDS) return 0.0;
        return Math.min(delta, MAX_DELTA_SECONDS);
    }

    public void reset() {
        previousNanos = Long.MIN_VALUE;
    }
}
