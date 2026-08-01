package io.github.ikunkk02afk.liquidglassui.reglassport.api;

/** Fully resolved optical parameters uploaded for one component. */
public record GlassOptics(float refractionThickness, float refractionFactor, float dispersion,
                          float fresnelRange, float fresnelHardness, float fresnelFactor,
                          float glareRange, float glareHardness, float glareConvergence,
                          float glareOppositeFactor, float glareFactor, float glareAngle) {
}
