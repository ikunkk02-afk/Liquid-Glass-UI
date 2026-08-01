package io.github.ikunkk02afk.liquidglassui.animation;

import io.github.ikunkk02afk.liquidglassui.render.GlassRectangle;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class AnimationSystemTest {
    @Test
    void springIsNearlyFrameRateIndependent() {
        float at60 = simulate(60);
        float at180 = simulate(180);
        assertEquals(at60, at180, 0.006f);
        assertTrue(at60 > 0.98f);
    }

    @Test
    void clockFreezesDiscontinuitiesAndUnfocusedFrames() {
        AtomicLong time = new AtomicLong();
        AnimationClock clock = new AnimationClock(time::get);
        assertEquals(0.0, clock.tick(true));
        time.addAndGet(16_000_000);
        assertEquals(0.016, clock.tick(true), 0.000001);
        time.addAndGet(300_000_000);
        assertEquals(0.0, clock.tick(true));
        time.addAndGet(16_000_000);
        assertEquals(0.0, clock.tick(false));
    }

    @Test
    void rapidGroupRetargetingRemainsFiniteAndBounded() {
        GlassGroupMotion group = new GlassGroupMotion(new GlassRectangle(0, 0, 200, 20));
        for (int frame = 0; frame < 300; frame++) {
            if (frame % 3 == 0) group.target(new GlassRectangle((frame % 10) * 24, (frame % 7) * 22, 200, 20));
            group.update(1.0 / 144.0, 220.0f, 28.0f, 1.0f, false);
            GlassRectangle value = group.active();
            assertTrue(Float.isFinite(value.x()) && Float.isFinite(value.y()));
            assertTrue(value.width() >= 0 && value.width() < 260);
            assertTrue(group.mergeAmount(0.55f) >= 0 && group.mergeAmount(0.55f) <= 0.55f);
        }
    }

    private float simulate(int frameRate) {
        SpringFloat spring = new SpringFloat(0.0f);
        spring.target(1.0f);
        for (int i = 0; i < frameRate * 2; i++) spring.update(1.0 / frameRate, 220.0f, 28.0f);
        return spring.value();
    }
}
