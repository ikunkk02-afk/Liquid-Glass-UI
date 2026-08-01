/*
 * Adapted from ReGlass by ReStudio / RedxAx.
 * Original project: https://github.com/RedxAx/ReGlass
 * Licensed under the MIT License.
 * Backported and modified for Minecraft Fabric 1.21.1.
 */
package restudio.reglass.client.gui;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import restudio.reglass.client.api.WidgetStyle;

public record LiquidGlassGuiElementRenderState(
        int x1,
        int y1,
        int x2,
        int y2,
        float cornerRadius,
        @Nullable Component text,
        WidgetStyle style,
        @Nullable ScreenRectangle scissorArea,
        float hover,
        float focus
) {
}
