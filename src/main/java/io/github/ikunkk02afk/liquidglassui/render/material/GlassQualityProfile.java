package io.github.ikunkk02afk.liquidglassui.render.material;

import io.github.ikunkk02afk.liquidglassui.render.GlassQualityBudget;

public record GlassQualityProfile(GlassQualityBudget budget, boolean chromaticDispersion,
                                  boolean fullRefraction, int maximumWidgets) {
    public GlassQualityProfile {
        maximumWidgets = Math.max(1, Math.min(64, maximumWidgets));
    }
}
