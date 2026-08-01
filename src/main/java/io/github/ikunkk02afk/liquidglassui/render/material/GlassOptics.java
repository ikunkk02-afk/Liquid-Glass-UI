/*
 * Optical parameter organization inspired by ReGlass by ReStudio / RedxAx.
 * Original project: https://github.com/RedxAx/ReGlass
 * Licensed under the MIT License. Adapted for Minecraft Fabric 1.21.1.
 */
package io.github.ikunkk02afk.liquidglassui.render.material;

public record GlassOptics(float thickness, float refraction, float edgeRefractionRange, float fresnel,
                          float edgeHighlight, float dispersionPixels, float innerShadow, float shadowStrength,
                          float backgroundClarity, float noiseStrength, float mouseHighlightRange) {
    public static GlassOptics defaults() {
        return new GlassOptics(5.0f, 0.025f, 0.28f, 0.24f, 0.30f, 0.25f,
                0.06f, 0.11f, 0.84f, 0.002f, 0.68f);
    }
}
