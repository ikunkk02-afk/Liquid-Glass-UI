package io.github.ikunkk02afk.liquidglassui.render.fallback;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;

/** Shader-free fallback: never samples a texture, so an atlas can never leak into the panel. */
public final class SafeGlassRenderer {
    public void drawPanel(GuiGraphics graphics, AbstractButton button, float radius, float opacity) {
        int left = button.getX();
        int top = button.getY();
        int right = button.getRight();
        int bottom = button.getBottom();
        int rounded = Math.max(2, Math.min(Math.round(radius), Math.max(2, button.getHeight() / 2)));
        int alpha = Math.max(20, Math.min(128, Math.round(255.0f * opacity)));
        int panel = (alpha << 24) | (button.active ? 0xDDE7F2 : 0x9098A0);
        int edge = ((button.isHoveredOrFocused() ? 82 : 52) << 24) | 0xFFFFFF;
        graphics.fill(left + rounded, top, right - rounded, bottom, panel);
        graphics.fill(left, top + rounded, right, bottom - rounded, panel);
        graphics.fill(left + 1, top + rounded / 2, left + rounded, bottom - rounded / 2, panel);
        graphics.fill(right - rounded, top + rounded / 2, right - 1, bottom - rounded / 2, panel);
        graphics.fill(left + rounded, top, right - rounded, top + 1, edge);
        graphics.fill(left + rounded, bottom - 1, right - rounded, bottom, 0x18000000);
    }
}
