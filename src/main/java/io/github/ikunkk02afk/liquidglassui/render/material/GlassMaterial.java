package io.github.ikunkk02afk.liquidglassui.render.material;

import io.github.ikunkk02afk.liquidglassui.config.GlassRefractionQuality;
import io.github.ikunkk02afk.liquidglassui.config.LiquidGlassConfigData;
import io.github.ikunkk02afk.liquidglassui.render.GlassQualityBudget;

public record GlassMaterial(float red, float green, float blue, float tintIntensity, float materialOpacity,
                            float blurMix, float mouseHighlight, float edgeWidth, boolean adaptBrightness,
                            GlassOptics optics) {
    private static final GlassMaterial DEFAULT = new GlassMaterial(
            0.918f, 0.949f, 0.980f, 0.06f, 0.10f, 0.62f, 0.18f, 0.65f, true,
            GlassOptics.defaults());

    public static GlassMaterial defaults() { return DEFAULT; }

    public static GlassMaterial from(LiquidGlassConfigData config, GlassQualityBudget quality) {
        int rgb;
        try {
            rgb = Integer.parseInt(config.appearance.mainColor.substring(1), 16);
        } catch (RuntimeException ignored) {
            rgb = 0xEAF2FA;
        }
        float refractionScale = quality.refractionQuality() == GlassRefractionQuality.OFF ? 0.0f
                : quality.refractionQuality() == GlassRefractionQuality.LOW ? 0.65f : 1.0f;
        float dispersion = quality.refractionQuality() == GlassRefractionQuality.HIGH
                ? config.optics.dispersionStrength : 0.0f;
        GlassOptics optics = new GlassOptics(config.optics.glassThickness,
                config.optics.refractionIntensity * refractionScale, config.optics.edgeRefractionRange,
                config.optics.fresnelStrength, config.appearance.edgeHighlightIntensity, dispersion,
                config.appearance.innerShadowIntensity, config.optics.shadowStrength,
                config.optics.backgroundClarity, config.optics.surfaceNoiseIntensity,
                config.optics.mouseHighlightRange);
        return new GlassMaterial(((rgb >> 16) & 255) / 255.0f, ((rgb >> 8) & 255) / 255.0f,
                (rgb & 255) / 255.0f, config.appearance.tintIntensity, config.appearance.opacity,
                config.optics.blurIntensity, config.optics.mouseHighlightIntensity,
                config.appearance.edgeWidth, config.appearance.adaptToBackgroundBrightness, optics);
    }
}
