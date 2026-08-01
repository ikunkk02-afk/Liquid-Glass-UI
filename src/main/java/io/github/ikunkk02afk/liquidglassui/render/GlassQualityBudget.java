package io.github.ikunkk02afk.liquidglassui.render;

import io.github.ikunkk02afk.liquidglassui.config.GlassQualityPreset;
import io.github.ikunkk02afk.liquidglassui.config.GlassRefractionQuality;
import io.github.ikunkk02afk.liquidglassui.config.LiquidGlassConfigData;

public record GlassQualityBudget(float bufferScale, int blurPasses, GlassRefractionQuality refractionQuality, int sampleCount) {
    public static GlassQualityBudget from(LiquidGlassConfigData data) {
        return switch (data.performance.preset) {
            case LOW -> new GlassQualityBudget(0.25f, 2, GlassRefractionQuality.OFF, 2);
            case MEDIUM -> new GlassQualityBudget(0.5f, 3, GlassRefractionQuality.LOW, 4);
            case HIGH -> new GlassQualityBudget(1.0f, 5, GlassRefractionQuality.HIGH, 8);
            case CUSTOM -> new GlassQualityBudget(data.performance.customBufferScale, data.performance.customBlurPasses,
                    data.performance.customRefractionQuality, data.performance.customSampleCount);
        };
    }

    public boolean dynamicRefraction() { return refractionQuality != GlassRefractionQuality.OFF; }
}
