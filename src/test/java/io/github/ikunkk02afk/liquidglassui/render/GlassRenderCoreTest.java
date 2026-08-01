package io.github.ikunkk02afk.liquidglassui.render;

import io.github.ikunkk02afk.liquidglassui.config.GlassRefractionQuality;
import io.github.ikunkk02afk.liquidglassui.config.LiquidGlassConfigData;
import io.github.ikunkk02afk.liquidglassui.render.frame.GlassFrameCollector;
import io.github.ikunkk02afk.liquidglassui.render.frame.GlassWidgetData;
import io.github.ikunkk02afk.liquidglassui.render.frame.GlassWidgetTexturePacker;
import io.github.ikunkk02afk.liquidglassui.render.material.GlassMaterial;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class GlassRenderCoreTest {
    @Test
    void presetsProduceExpectedBudgets() {
        LiquidGlassConfigData data = LiquidGlassConfigData.defaults();
        assertEquals(new GlassQualityBudget(0.5f, 4, GlassRefractionQuality.LOW, 4), GlassQualityBudget.from(data));
        data.performance.preset = io.github.ikunkk02afk.liquidglassui.config.GlassQualityPreset.LOW;
        assertEquals(0.4f, GlassQualityBudget.from(data).bufferScale());
        assertTrue(GlassQualityBudget.from(data).dynamicRefraction());
        assertEquals(GlassRefractionQuality.LOW, GlassQualityBudget.from(data).refractionQuality());
        data.performance.preset = io.github.ikunkk02afk.liquidglassui.config.GlassQualityPreset.HIGH;
        assertEquals(6, GlassQualityBudget.from(data).blurPasses());
        assertEquals(1440, GlassQualityBudget.from(data).targetWidth(1920));
        assertEquals(810, GlassQualityBudget.from(data).targetHeight(1080));
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

    @Test
    void guiCoordinatesMapToPhysicalPixelsAndFlipYExactlyOnce() {
        for (int scale : new int[]{2, 3, 4}) {
            GlassCoordinateMapper mapper = new GlassCoordinateMapper(640, 360, 640 * scale, 360 * scale);
            GlassRectangle physical = mapper.toFramebuffer(new GlassRectangle(100, 50, 200, 20));
            assertEquals(100 * scale, physical.x());
            assertEquals(50 * scale, physical.y());
            assertEquals(200 * scale, physical.width());
            assertEquals(20 * scale, physical.height());
            assertEquals(100.0f / 640.0f, mapper.textureU(100), 0.00001f);
            assertEquals(1.0f - 50.0f / 360.0f, mapper.textureV(50), 0.00001f);
        }
    }

    @Test
    void nonIntegerHighDpiScalePreservesNormalizedCoordinates() {
        GlassCoordinateMapper mapper = new GlassCoordinateMapper(853, 479, 1920, 1080);
        assertEquals(1920.0f / 853.0f, mapper.scaleX(), 0.00001f);
        assertEquals(1080.0f / 479.0f, mapper.scaleY(), 0.00001f);
        assertEquals(0.5f, mapper.textureU(426.5f), 0.00001f);
        assertEquals(0.5f, mapper.textureV(239.5f), 0.00001f);
    }

    @Test
    void collectorLocksCapacityAndReportsOverflow() {
        GlassFrameCollector collector = new GlassFrameCollector();
        collector.begin(41, 2);
        assertNotNull(collector.add());
        assertNotNull(collector.add());
        assertNull(collector.add());
        assertEquals(2, collector.freeze().widgetCount());
        assertTrue(collector.freeze().overflowed());
    }

    @Test
    void dataTexturePackerAppliesNonIntegerScaleAndSingleYFlip() {
        GlassFrameCollector collector = new GlassFrameCollector();
        collector.begin(7, 64);
        GlassWidgetData widget = collector.add();
        assertNotNull(widget);
        widget.x = 100.0f;
        widget.y = 50.0f;
        widget.width = 200.0f;
        widget.height = 20.0f;
        widget.cornerRadius = 8.0f;
        GlassFrameContext frame = new GlassFrameContext(7, 853, 479, 1920, 1080,
                2.25, 426.5f, 239.5f, new GlassQualityBudget(0.5f, 4, GlassRefractionQuality.LOW, 4));
        float[] packed = new GlassWidgetTexturePacker().pack(collector.freeze(), frame);

        assertEquals(100.0f * 1920.0f / 853.0f, packed[0], 0.0001f);
        assertEquals(1080.0f - 70.0f * 1080.0f / 479.0f, packed[1], 0.0001f);
        assertEquals(200.0f * 1920.0f / 853.0f, packed[2], 0.0001f);
        assertEquals(20.0f * 1080.0f / 479.0f, packed[3], 0.0001f);
    }

    @Test
    void materialDensityAndOpticalRangesArePackedSeparatelyFromAnimationOpacity() {
        LiquidGlassConfigData data = LiquidGlassConfigData.defaults();
        data.appearance.opacity = 0.30f;
        data.optics.edgeRefractionRange = 0.44f;
        data.optics.mouseHighlightRange = 0.73f;
        GlassQualityBudget quality = new GlassQualityBudget(0.75f, 6, GlassRefractionQuality.HIGH, 8);

        GlassFrameCollector collector = new GlassFrameCollector();
        collector.begin(9, 64);
        GlassWidgetData widget = collector.add();
        assertNotNull(widget);
        widget.width = 200.0f;
        widget.height = 20.0f;
        widget.animationOpacity = 0.42f;
        widget.material = GlassMaterial.from(data, quality);
        GlassFrameContext frame = new GlassFrameContext(9, 960, 540, 1920, 1080,
                2.0, 0.0f, 0.0f, quality);
        float[] packed = new GlassWidgetTexturePacker().pack(collector.freeze(), frame);

        int animationRow = (5 * GlassWidgetTexturePacker.WIDTH) * GlassWidgetTexturePacker.CHANNELS;
        int materialRow = (11 * GlassWidgetTexturePacker.WIDTH) * GlassWidgetTexturePacker.CHANNELS;
        assertEquals(0.42f, packed[animationRow + 2], 0.0001f);
        assertEquals(0.30f, packed[materialRow], 0.0001f);
        assertEquals(0.44f, packed[materialRow + 1], 0.0001f);
        assertEquals(0.73f, packed[materialRow + 2], 0.0001f);
    }
}
