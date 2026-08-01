package io.github.ikunkk02afk.liquidglassui.reglassport.api;

import io.github.ikunkk02afk.liquidglassui.config.LiquidGlassConfigData;

/** The single compatibility boundary from project settings to ReGlass parameter semantics. */
public final class ReGlassStyleMapper {
    private ReGlassStyleMapper() {
    }

    public static GlassStyle fromConfig(LiquidGlassConfigData config) {
        LiquidGlassConfigData safe = config == null ? LiquidGlassConfigData.defaults() : config.sanitizedCopy();
        int tint = Integer.parseInt(safe.appearance.mainColor.substring(1), 16);
        float refractionFactor = 1.0f + safe.optics.refractionIntensity * 16.0f;
        float smoothing = GlassPortConfig.DEFAULTS.defaultSmoothing
                * safe.animation.mergeStrength / 0.55f;
        return GlassStyle.create()
                .tint(tint, safe.appearance.tintIntensity)
                .blurRadius(Math.round(safe.optics.blurRadius))
                .refractionFactor(refractionFactor)
                .smoothing(Math.max(0.0f, smoothing));
    }
}
