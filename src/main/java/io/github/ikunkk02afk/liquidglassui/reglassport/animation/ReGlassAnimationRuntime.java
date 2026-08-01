/*
 * Portions adapted from ReGlass by ReStudio / RedxAx.
 * Original project: https://github.com/RedxAx/ReGlass
 * Licensed under the MIT License.
 * Adapted for Minecraft Fabric 1.21.1.
 */
package io.github.ikunkk02afk.liquidglassui.reglassport.animation;

import io.github.ikunkk02afk.liquidglassui.reglassport.widget.GlassWidgetState;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** Refresh-rate-independent interaction fades and critically damped motion keyed by stable widget id. */
public final class ReGlassAnimationRuntime {
    private static final double MAX_DELTA = 0.05;
    private final Map<Long, State> states = new HashMap<>();
    private long lastNanos;
    private long frame;
    private double deltaSeconds;

    public void beginFrame(long nowNanos) {
        if (lastNanos == 0L || nowNanos <= lastNanos) deltaSeconds = 0.0;
        else deltaSeconds = Math.min(MAX_DELTA, (nowNanos - lastNanos) / 1_000_000_000.0);
        lastNanos = nowNanos;
        beginFrameInternal();
    }

    /** Deterministic clock entry point used by automated refresh-rate tests. */
    public void beginFrameDelta(double requestedDeltaSeconds) {
        deltaSeconds = Math.max(0.0, Math.min(MAX_DELTA,
                Double.isFinite(requestedDeltaSeconds) ? requestedDeltaSeconds : 0.0));
        beginFrameInternal();
    }

    public void resetClock() {
        lastNanos = 0L;
        deltaSeconds = 0.0;
    }

    public GlassWidgetState sample(long id, float targetX, float targetY,
                                   float hoverTarget, float focusTarget, float pressTarget,
                                   float highlightTargetX, float highlightTargetY,
                                   float fusionTarget, float stiffness, float damping) {
        State state = states.computeIfAbsent(id, ignored -> new State(targetX, targetY));
        state.lastSeenFrame = frame;
        state.hover = fade(state.hover, clamp01(hoverTarget), 0.12);
        state.focus = fade(state.focus, clamp01(focusTarget), 0.18);
        state.press = fade(state.press, clamp01(pressTarget), 0.075);
        state.highlightX = fade(state.highlightX, clamp(highlightTargetX, -1.0f, 1.0f), 0.08);
        state.highlightY = fade(state.highlightY, clamp(highlightTargetY, -1.0f, 1.0f), 0.08);
        float x = (float) state.x.step(targetX, deltaSeconds, stiffness, damping);
        float y = (float) state.y.step(targetY, deltaSeconds, stiffness, damping);
        float expansionTarget = state.hover * 1.5f + state.focus * 2.5f - state.press * 0.8f;
        float expansion = (float) state.expansion.step(expansionTarget, deltaSeconds, stiffness, damping);
        float fusion = (float) state.fusion.step(clamp01(fusionTarget), deltaSeconds, stiffness, damping);
        return new GlassWidgetState(x, y, state.hover, state.focus, state.press,
                expansion, clamp01(fusion), state.highlightX, state.highlightY);
    }

    public int stateCount() { return states.size(); }
    public double deltaSeconds() { return deltaSeconds; }

    private void beginFrameInternal() {
        frame++;
        if ((frame & 127L) == 0L) {
            Iterator<State> iterator = states.values().iterator();
            while (iterator.hasNext()) {
                if (frame - iterator.next().lastSeenFrame > 300L) iterator.remove();
            }
        }
    }

    private float fade(float current, float target, double tau) {
        if (deltaSeconds <= 0.0) return current;
        float alpha = (float) (1.0 - Math.exp(-deltaSeconds / tau));
        float value = current + (target - current) * alpha;
        return Math.abs(value - target) < 1e-4f ? target : value;
    }

    private static float clamp01(float value) { return clamp(value, 0.0f, 1.0f); }
    private static float clamp(float value, float min, float max) {
        if (!Float.isFinite(value)) return min;
        return Math.max(min, Math.min(max, value));
    }

    private static final class State {
        final ReGlassSpringState x = new ReGlassSpringState();
        final ReGlassSpringState y = new ReGlassSpringState();
        final ReGlassSpringState expansion = new ReGlassSpringState();
        final ReGlassSpringState fusion = new ReGlassSpringState();
        float hover;
        float focus;
        float press;
        float highlightX;
        float highlightY;
        long lastSeenFrame;

        State(float initialX, float initialY) {
            x.snap(initialX);
            y.snap(initialY);
            expansion.snap(0.0);
            fusion.snap(0.0);
        }
    }
}
