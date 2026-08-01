/*
 * Portions adapted from ReGlass by ReStudio / RedxAx.
 * Original project: https://github.com/RedxAx/ReGlass
 * Licensed under the MIT License.
 * Adapted for Minecraft Fabric 1.21.1.
 */
package io.github.ikunkk02afk.liquidglassui.reglassport.render;

import io.github.ikunkk02afk.liquidglassui.reglassport.api.GlassStyle;

/** One immutable deferred glass-component submission in GUI coordinates. */
public record ReGlassUniformData(long stableId, float x, float y, float width, float height,
                                 float cornerRadius, GlassStyle style, float hover, float focus,
                                 float press, int groupId, float scissorX, float scissorY,
                                 float scissorWidth, float scissorHeight) {
}
