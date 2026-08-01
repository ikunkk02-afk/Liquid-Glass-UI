package io.github.ikunkk02afk.liquidglassui.config;

public final class HighQualityPolicy {
    private HighQualityPolicy() {
    }

    public static boolean requiresWarning(LiquidGlassConfigData data, GlassQualityPreset requested) {
        return requested == GlassQualityPreset.HIGH && !data.highQualityWarningAcknowledged;
    }

    public static void acknowledgeAndSelect(LiquidGlassConfigData data) {
        data.performance.preset = GlassQualityPreset.HIGH;
        data.highQualityWarningAcknowledged = true;
    }
}
