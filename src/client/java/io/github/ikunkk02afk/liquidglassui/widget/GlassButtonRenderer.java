package io.github.ikunkk02afk.liquidglassui.widget;

import io.github.ikunkk02afk.liquidglassui.config.LiquidGlassConfigData;
import io.github.ikunkk02afk.liquidglassui.render.GlassRectangle;
import io.github.ikunkk02afk.liquidglassui.render.GlassSurface;
import io.github.ikunkk02afk.liquidglassui.render.LegacyGlassRenderBackend;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;

public final class GlassButtonRenderer {
    private final LegacyGlassRenderBackend backend;

    public GlassButtonRenderer(LegacyGlassRenderBackend backend) {
        this.backend = backend;
    }

    public boolean render(AbstractButton button, GuiGraphics graphics, GlassWidgetState state,
                          GlassGroupController group, LiquidGlassConfigData config) {
        var animation = state.animation();
        GlassRectangle bounds = group.activeBounds(button).scaled(animation.scale());
        GlassSurface surface = new GlassSurface(bounds, group.previousBounds(button),
                group.merge(button, config.animation.mergeStrength), config.appearance.cornerRadius,
                animation.scale(), animation.opacity(), animation.hover(), animation.highlightX(),
                animation.highlightY(), button.active);
        return backend.renderButton(button, graphics, surface, config);
    }
}
