/*
 * Optical parameter organization inspired by ReGlass by ReStudio / RedxAx.
 * Original project: https://github.com/RedxAx/ReGlass
 * Licensed under the MIT License. Adapted for Minecraft Fabric 1.21.1.
 */
package io.github.ikunkk02afk.liquidglassui.render.material;

public record GlassOptics(float thickness, float refraction, float fresnel, float edgeHighlight,
                          float dispersionPixels, float innerShadow, float shadowStrength,
                          float backgroundClarity, float noiseStrength) {
    public static GlassOptics defaults() {
        return new GlassOptics(5.5f, 0.025f, 0.32f, 0.26f, 0.65f, 0.12f, 0.18f, 0.62f, 0.008f);
    }
}
