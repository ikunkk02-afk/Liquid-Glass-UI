/*
 * Portions adapted from ReGlass by ReStudio / RedxAx.
 * Original project: https://github.com/RedxAx/ReGlass
 * Licensed under the MIT License.
 * Adapted for Minecraft Fabric 1.21.1.
 */
package io.github.ikunkk02afk.liquidglassui.reglassport.api;

/** Fluent per-widget overrides preserving ReGlass WidgetStyle semantics. */
public final class GlassStyle {
    private Integer tintColor;
    private Float tintAlpha;
    private Integer blurRadius;
    private Float smoothing;
    private Float cornerRadius;
    private Float refractionThickness;
    private Float refractionFactor;
    private Float dispersion;
    private Float fresnelRange;
    private Float fresnelHardness;
    private Float fresnelFactor;
    private Float glareRange;
    private Float glareHardness;
    private Float glareConvergence;
    private Float glareOppositeFactor;
    private Float glareFactor;
    private Float glareAngle;
    private Float shadowExpand;
    private Float shadowFactor;
    private Float shadowOffsetX;
    private Float shadowOffsetY;
    private Integer shadowColor;
    private Float shadowColorAlpha;

    public static GlassStyle create() { return new GlassStyle(); }
    public GlassStyle tint(int color, float alpha) { tintColor = color & 0xFFFFFF; tintAlpha = clamp01(alpha); return this; }
    public GlassStyle tintColor(int color) { tintColor = color & 0xFFFFFF; return this; }
    public GlassStyle tintAlpha(float alpha) { tintAlpha = clamp01(alpha); return this; }
    public GlassStyle blurRadius(int radius) { blurRadius = Math.max(0, radius); return this; }
    public GlassStyle smoothing(float value) { smoothing = value; return this; }
    public GlassStyle cornerRadius(float value) { cornerRadius = Math.max(0.0f, value); return this; }
    public GlassStyle refractionThickness(float value) { refractionThickness = Math.max(0.0f, value); return this; }
    public GlassStyle refractionFactor(float value) { refractionFactor = Math.max(0.001f, value); return this; }
    public GlassStyle dispersion(float value) { dispersion = Math.max(0.0f, value); return this; }
    public GlassStyle fresnelRange(float value) { fresnelRange = Math.max(0.001f, value); return this; }
    public GlassStyle fresnelHardness(float value) { fresnelHardness = value; return this; }
    public GlassStyle fresnelFactor(float value) { fresnelFactor = Math.max(0.0f, value); return this; }
    public GlassStyle glareRange(float value) { glareRange = Math.max(0.001f, value); return this; }
    public GlassStyle glareHardness(float value) { glareHardness = value; return this; }
    public GlassStyle glareConvergence(float value) { glareConvergence = value; return this; }
    public GlassStyle glareOppositeFactor(float value) { glareOppositeFactor = value; return this; }
    public GlassStyle glareFactor(float value) { glareFactor = Math.max(0.0f, value); return this; }
    public GlassStyle glareAngle(float radians) { glareAngle = radians; return this; }
    public GlassStyle shadow(float expand, float factor, float offsetX, float offsetY) {
        shadowExpand = Math.max(0.0f, expand); shadowFactor = Math.max(0.0f, factor);
        shadowOffsetX = offsetX; shadowOffsetY = offsetY; return this;
    }
    public GlassStyle shadowColor(int color, float alpha) {
        shadowColor = color & 0xFFFFFF; shadowColorAlpha = clamp01(alpha); return this;
    }

    public int tintColor(GlassPortConfig config) { return tintColor != null ? tintColor : config.defaultTintColor; }
    public float tintAlpha(GlassPortConfig config) { return tintAlpha != null ? tintAlpha : config.defaultTintAlpha; }
    public int blurRadius(GlassPortConfig config) { return blurRadius != null ? blurRadius : config.defaultBlurRadius; }
    public float smoothing(GlassPortConfig config) { return smoothing != null ? smoothing : config.defaultSmoothing; }
    public float resolvedCornerRadius(float fallback) { return cornerRadius != null ? cornerRadius : fallback; }
    public float shadowExpand(GlassPortConfig config) { return shadowExpand != null ? shadowExpand : config.defaultShadowExpand; }
    public float shadowFactor(GlassPortConfig config) { return shadowFactor != null ? shadowFactor : config.defaultShadowFactor; }
    public float shadowOffsetX(GlassPortConfig config) { return shadowOffsetX != null ? shadowOffsetX : config.defaultShadowOffsetX; }
    public float shadowOffsetY(GlassPortConfig config) { return shadowOffsetY != null ? shadowOffsetY : config.defaultShadowOffsetY; }
    public int shadowColor(GlassPortConfig config) { return shadowColor != null ? shadowColor : config.defaultShadowColor; }
    public float shadowColorAlpha(GlassPortConfig config) { return shadowColorAlpha != null ? shadowColorAlpha : config.defaultShadowColorAlpha; }

    public GlassOptics optics(GlassPortConfig config) {
        return new GlassOptics(
                refractionThickness != null ? refractionThickness : config.defaultRefractionThickness,
                refractionFactor != null ? refractionFactor : config.defaultRefractionFactor,
                dispersion != null ? dispersion : config.defaultDispersion,
                fresnelRange != null ? fresnelRange : config.defaultFresnelRange,
                fresnelHardness != null ? fresnelHardness : config.defaultFresnelHardness,
                fresnelFactor != null ? fresnelFactor : config.defaultFresnelFactor,
                glareRange != null ? glareRange : config.defaultGlareRange,
                glareHardness != null ? glareHardness : config.defaultGlareHardness,
                glareConvergence != null ? glareConvergence : config.defaultGlareConvergence,
                glareOppositeFactor != null ? glareOppositeFactor : config.defaultGlareOppositeFactor,
                glareFactor != null ? glareFactor : config.defaultGlareFactor,
                glareAngle != null ? glareAngle : config.defaultGlareAngle);
    }

    private static float clamp01(float value) {
        if (Float.isNaN(value)) return 0.0f;
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
