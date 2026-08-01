package io.github.ikunkk02afk.liquidglassui.reglassport;

import io.github.ikunkk02afk.liquidglassui.reglassport.api.GlassOptics;
import io.github.ikunkk02afk.liquidglassui.reglassport.api.GlassPortConfig;
import io.github.ikunkk02afk.liquidglassui.reglassport.api.GlassStyle;
import io.github.ikunkk02afk.liquidglassui.reglassport.render.ReGlassFrameCollector;
import io.github.ikunkk02afk.liquidglassui.reglassport.render.ReGlassUniformData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ReGlassPortFoundationTest {
    @Test
    void preservesReGlassOpticsDefaults() {
        GlassPortConfig defaults = GlassPortConfig.DEFAULTS;
        GlassOptics optics = GlassStyle.create().optics(defaults);

        assertEquals(12, GlassStyle.create().blurRadius(defaults));
        assertEquals(20.0f, optics.refractionThickness());
        assertEquals(1.4f, optics.refractionFactor());
        assertEquals(7.0f, optics.dispersion());
        assertEquals(30.0f, optics.fresnelRange());
        assertEquals(90.0f, optics.glareFactor());
    }

    @Test
    void styleClampsUnsafeBuilderValues() {
        GlassStyle style = GlassStyle.create()
                .tint(0x123456, 4.0f)
                .blurRadius(-8)
                .cornerRadius(-3.0f)
                .dispersion(-1.0f);

        assertEquals(1.0f, style.tintAlpha(GlassPortConfig.DEFAULTS));
        assertEquals(0, style.blurRadius(GlassPortConfig.DEFAULTS));
        assertEquals(0.0f, style.resolvedCornerRadius(5.0f));
        assertEquals(0.0f, style.optics(GlassPortConfig.DEFAULTS).dispersion());
    }

    @Test
    void collectorRejectsInvalidOutOfFrameAndSixtyFifthWidget() {
        ReGlassFrameCollector collector = new ReGlassFrameCollector();
        ReGlassUniformData valid = widget(1L, 20.0f, 10.0f);

        assertFalse(collector.submit(valid));
        collector.beginFrame();
        assertFalse(collector.submit(widget(2L, 0.0f, 10.0f)));
        for (int index = 0; index < ReGlassFrameCollector.MAX_WIDGETS; index++) {
            assertTrue(collector.submit(widget(index + 10L, 20.0f, 10.0f)));
        }
        assertFalse(collector.submit(widget(999L, 20.0f, 10.0f)));
        assertEquals(ReGlassFrameCollector.MAX_WIDGETS, collector.finishFrame().size());
    }

    private static ReGlassUniformData widget(long id, float width, float height) {
        return new ReGlassUniformData(id, 0.0f, 0.0f, width, height, 4.0f,
                GlassStyle.create(), 0.0f, 0.0f, 0.0f, 0,
                0.0f, 0.0f, 100.0f, 100.0f);
    }
}
