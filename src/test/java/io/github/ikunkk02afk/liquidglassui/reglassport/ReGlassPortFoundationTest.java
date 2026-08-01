package io.github.ikunkk02afk.liquidglassui.reglassport;

import io.github.ikunkk02afk.liquidglassui.reglassport.api.GlassOptics;
import io.github.ikunkk02afk.liquidglassui.reglassport.api.GlassPortConfig;
import io.github.ikunkk02afk.liquidglassui.reglassport.api.GlassStyle;
import io.github.ikunkk02afk.liquidglassui.reglassport.api.ReGlassStyleMapper;
import io.github.ikunkk02afk.liquidglassui.reglassport.animation.ReGlassAnimationRuntime;
import io.github.ikunkk02afk.liquidglassui.reglassport.render.ReGlassGenerationLatch;
import io.github.ikunkk02afk.liquidglassui.reglassport.render.ReGlassFrameCollector;
import io.github.ikunkk02afk.liquidglassui.reglassport.render.ReGlassTextureLayout;
import io.github.ikunkk02afk.liquidglassui.reglassport.render.ReGlassUniformData;
import io.github.ikunkk02afk.liquidglassui.reglassport.widget.GlassWidgetState;
import io.github.ikunkk02afk.liquidglassui.config.LiquidGlassConfigData;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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

    @Test
    void shaderResourcesKeepRequiredSdfAndOpticalStages() throws IOException {
        Path shaderRoot = Path.of("src/client/resources/assets/liquid_glass_ui/shaders/core/reglass_port");
        String composite = Files.readString(shaderRoot.resolve("liquid_glass_gui.fsh"));
        String blur = Files.readString(shaderRoot.resolve("blur.fsh"));

        for (String required : new String[]{"struct SDFResult", "sdgBox", "opSmoothUnion", "opHardUnion",
                "opHardSubtract", "fieldWidgets", "sampleBlur", "refractionOffset", "dispersed",
                "fresnelFactor", "glareDirection", "applyShadows"}) {
            assertTrue(composite.contains(required), () -> "missing shader stage " + required);
        }
        for (int mode = 1; mode <= 6; mode++) {
            assertTrue(composite.contains("DebugMode == " + mode));
        }
        assertTrue(composite.contains("uniform sampler2D RawSampler"));
        assertTrue(composite.contains("uniform sampler2D WidgetDataSampler"));
        assertTrue(composite.contains("groupAlreadyProcessed"));
        assertTrue(composite.contains("field = fieldInitialized ? opHardUnion"));
        assertTrue(blur.contains("uniform float Weights[65]"));
        assertTrue(blur.contains("Direction / OutSize"));
        assertFalse(blur.contains("0.25 * ("), "old four-corner average blur must not return");
    }

    @Test
    void configMapperOverridesOnlyThePublicReGlassControls() {
        LiquidGlassConfigData config = LiquidGlassConfigData.defaults();
        config.appearance.mainColor = "#123456";
        config.appearance.tintIntensity = 0.2f;
        config.optics.blurRadius = 9.0f;
        config.optics.refractionIntensity = 0.05f;
        config.animation.mergeStrength = 0.55f;

        GlassStyle style = ReGlassStyleMapper.fromConfig(config);
        assertEquals(0x123456, style.tintColor(GlassPortConfig.DEFAULTS));
        assertEquals(0.2f, style.tintAlpha(GlassPortConfig.DEFAULTS));
        assertEquals(9, style.blurRadius(GlassPortConfig.DEFAULTS));
        assertEquals(1.8f, style.optics(GlassPortConfig.DEFAULTS).refractionFactor(), 0.0001f);
        assertEquals(GlassPortConfig.DEFAULTS.defaultSmoothing,
                style.smoothing(GlassPortConfig.DEFAULTS), 0.0001f);
    }

    @Test
    void animationConvergesConsistentlyAcrossCommonRefreshRates() {
        GlassWidgetState reference = simulateAnimation(60);
        for (int refreshRate : new int[]{120, 144, 180}) {
            GlassWidgetState candidate = simulateAnimation(refreshRate);
            assertEquals(reference.x(), candidate.x(), 0.35f, "x at " + refreshRate + "Hz");
            assertEquals(reference.hover(), candidate.hover(), 0.01f, "hover at " + refreshRate + "Hz");
            assertEquals(reference.fusion(), candidate.fusion(), 0.01f, "fusion at " + refreshRate + "Hz");
        }
    }

    @Test
    void animationClampsLostFocusDeltaAndKeepsStableIdState() {
        ReGlassAnimationRuntime runtime = new ReGlassAnimationRuntime();
        runtime.beginFrameDelta(10.0);
        runtime.sample(7L, 0, 0, 0, 0, 0, 0, 0, 1, 220, 28);
        runtime.beginFrameDelta(1.0 / 60.0);
        runtime.sample(7L, 20, 0, 1, 0, 0, 0, 0, 1, 220, 28);
        assertEquals(1, runtime.stateCount());
        runtime.beginFrameDelta(10.0);
        assertEquals(0.05, runtime.deltaSeconds(), 1e-9);
        runtime.resetClock();
        assertEquals(0.0, runtime.deltaSeconds(), 1e-9);
    }

    @Test
    void dataTextureContractIsExactlySixtyFourByTwelveRgbaFloats() {
        assertEquals(64, ReGlassTextureLayout.WIDTH);
        assertEquals(12, ReGlassTextureLayout.ROWS);
        assertEquals(64 * 12 * 4, ReGlassTextureLayout.FLOAT_COUNT);
        assertEquals((11 * 64 + 63) * 4, ReGlassTextureLayout.offset(63, 11));
    }

    @Test
    void failureLatchUnlocksOnlyAfterSuccessfulResourceGeneration() {
        ReGlassGenerationLatch latch = new ReGlassGenerationLatch();
        assertTrue(latch.fail(3));
        assertFalse(latch.fail(3));
        assertTrue(latch.failed(3));
        assertFalse(latch.failed(4));
        latch.successfulReload();
        assertFalse(latch.failed(3));
        assertTrue(latch.fail(3));
    }

    private static GlassWidgetState simulateAnimation(int refreshRate) {
        ReGlassAnimationRuntime runtime = new ReGlassAnimationRuntime();
        runtime.beginFrameDelta(0.0);
        runtime.sample(42L, 0, 0, 0, 0, 0, 0, 0, 0, 220, 28);
        GlassWidgetState state = null;
        for (int frame = 0; frame < refreshRate * 2; frame++) {
            runtime.beginFrameDelta(1.0 / refreshRate);
            state = runtime.sample(42L, 100, 30, 1, 1, 0, 0.5f, -0.5f, 1, 220, 28);
        }
        return state;
    }

    private static ReGlassUniformData widget(long id, float width, float height) {
        return new ReGlassUniformData(id, 0.0f, 0.0f, width, height, 4.0f,
                GlassStyle.create(), 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0,
                0.0f, 0.0f, 100.0f, 100.0f);
    }
}
