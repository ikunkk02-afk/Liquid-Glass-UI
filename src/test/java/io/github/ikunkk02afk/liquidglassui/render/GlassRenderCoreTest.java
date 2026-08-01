package io.github.ikunkk02afk.liquidglassui.render;

import io.github.ikunkk02afk.liquidglassui.config.GlassRefractionQuality;
import io.github.ikunkk02afk.liquidglassui.config.LiquidGlassConfigData;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class GlassRenderCoreTest {
    @Test
    void presetsProduceExpectedBudgets() {
        LiquidGlassConfigData data = LiquidGlassConfigData.defaults();
        assertEquals(new GlassQualityBudget(0.5f, 3, GlassRefractionQuality.LOW, 4), GlassQualityBudget.from(data));
        data.performance.preset = io.github.ikunkk02afk.liquidglassui.config.GlassQualityPreset.LOW;
        assertEquals(0.25f, GlassQualityBudget.from(data).bufferScale());
        assertFalse(GlassQualityBudget.from(data).dynamicRefraction());
        data.performance.preset = io.github.ikunkk02afk.liquidglassui.config.GlassQualityPreset.HIGH;
        assertEquals(5, GlassQualityBudget.from(data).blurPasses());
        assertEquals(8, GlassQualityBudget.from(data).sampleCount());
    }

    @Test
    void failureLatchReportsOnlyOnce() {
        AtomicInteger reports = new AtomicInteger();
        GlassFailureLatch latch = new GlassFailureLatch((message, error) -> reports.incrementAndGet());
        assertTrue(latch.trip("shader", new IllegalStateException()));
        assertFalse(latch.trip("framebuffer", new IllegalStateException()));
        assertTrue(latch.failed());
        assertEquals("shader", latch.reason());
        assertEquals(1, reports.get());
    }
}
