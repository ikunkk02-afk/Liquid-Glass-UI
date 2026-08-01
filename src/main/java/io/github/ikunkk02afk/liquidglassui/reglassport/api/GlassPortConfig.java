/*
 * Portions adapted from ReGlass by ReStudio / RedxAx.
 * Original project: https://github.com/RedxAx/ReGlass
 * Licensed under the MIT License.
 * Adapted for Minecraft Fabric 1.21.1.
 */
package io.github.ikunkk02afk.liquidglassui.reglassport.api;

/** ReGlass 1.1.0 defaults kept in their original parameter space. */
public final class GlassPortConfig {
    public static final GlassPortConfig DEFAULTS = new GlassPortConfig();

    public final int defaultTintColor = 0x000000;
    public final float defaultTintAlpha = 0.0f;
    public final float defaultSmoothing = 0.003f;
    public final int defaultBlurRadius = 12;
    public final float defaultShadowExpand = 30.0f;
    public final float defaultShadowFactor = 0.25f;
    public final float defaultShadowOffsetX = 0.0f;
    public final float defaultShadowOffsetY = 2.0f;
    public final int defaultShadowColor = 0x000000;
    public final float defaultShadowColorAlpha = 1.0f;
    public final float defaultRefractionThickness = 20.0f;
    public final float defaultRefractionFactor = 1.4f;
    public final float defaultDispersion = 7.0f;
    public final float defaultFresnelRange = 30.0f;
    public final float defaultFresnelHardness = 20.0f;
    public final float defaultFresnelFactor = 20.0f;
    public final float defaultGlareRange = 30.0f;
    public final float defaultGlareHardness = 20.0f;
    public final float defaultGlareConvergence = 50.0f;
    public final float defaultGlareOppositeFactor = 80.0f;
    public final float defaultGlareFactor = 90.0f;
    public final float defaultGlareAngle = (float) Math.toRadians(-45.0);
    public final float hoverScalePx = 1.5f;
    public final float focusScalePx = 2.5f;
    public final float focusBorderWidthPx = 2.0f;
    public final float focusBorderIntensity = 0.75f;
    public final float focusBorderSpeed = 1.6f;

    private GlassPortConfig() {
    }
}
