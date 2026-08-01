/*
 * Portions adapted from ReGlass by ReStudio / RedxAx.
 * Original project: https://github.com/RedxAx/ReGlass
 * Licensed under the MIT License.
 * Adapted for Minecraft Fabric 1.21.1.
 */
package io.github.ikunkk02afk.liquidglassui.reglassport.widget;

import io.github.ikunkk02afk.liquidglassui.reglassport.ReGlassPortClient;
import io.github.ikunkk02afk.liquidglassui.reglassport.api.GlassStyle;
import io.github.ikunkk02afk.liquidglassui.reglassport.api.ReGlassPortApi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.concurrent.atomic.AtomicLong;

/** A deferred liquid-glass widget whose label is drawn after the shared composite pass. */
public final class LiquidGlassPortWidget extends AbstractWidget {
    private static final AtomicLong NEXT_ID = new AtomicLong(1L);

    private final long stableId;
    private final GlassStyle style;
    private final Runnable action;
    private final boolean draggable;
    private float cornerRadius;
    private boolean pressed;
    private boolean dragging;
    private boolean movedDuringClick;
    private double dragOffsetX;
    private double dragOffsetY;
    private int boundWidth = Integer.MAX_VALUE;
    private int boundHeight = Integer.MAX_VALUE;

    public LiquidGlassPortWidget(int x, int y, int width, int height, Component message,
                                 float cornerRadius, GlassStyle style, boolean draggable, Runnable action) {
        super(x, y, width, height, message);
        this.stableId = NEXT_ID.getAndIncrement();
        this.cornerRadius = Math.max(0.0f, cornerRadius);
        this.style = style == null ? GlassStyle.create() : style;
        this.draggable = draggable;
        this.action = action == null ? () -> { } : action;
    }

    public long stableId() {
        return stableId;
    }

    public void setMovementBounds(int width, int height) {
        boundWidth = Math.max(getWidth(), width);
        boundHeight = Math.max(getHeight(), height);
        clampToBounds();
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        ReGlassPortApi.create(graphics)
                .fromWidget(this)
                .cornerRadius(cornerRadius)
                .style(style)
                .hover(isHovered() ? 1.0f : 0.0f)
                .focus(isFocused() ? 1.0f : 0.0f)
                .press(pressed ? 1.0f : 0.0f)
                .group(0)
                .id(stableId)
                .render();
        ReGlassPortClient.defer(this);
    }

    public void renderDeferred(GuiGraphics graphics, boolean compositeRendered) {
        if (!compositeRendered) {
            graphics.fill(getX(), getY(), getRight(), getBottom(), 0x70405060);
            graphics.fill(getX(), getY(), getRight(), getY() + 1, 0x80FFFFFF);
        }
        int textColor = active ? 0xFFFFFFFF : 0xFFA0A0A0;
        graphics.drawCenteredString(Minecraft.getInstance().font, getMessage(),
                getX() + getWidth() / 2, getY() + (getHeight() - 8) / 2, textColor);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        pressed = true;
        dragging = draggable;
        movedDuringClick = false;
        dragOffsetX = mouseX - getX();
        dragOffsetY = mouseY - getY();
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
        if (!dragging) return;
        int nextX = (int) Math.round(mouseX - dragOffsetX);
        int nextY = (int) Math.round(mouseY - dragOffsetY);
        movedDuringClick |= Math.abs(nextX - getX()) + Math.abs(nextY - getY()) > 1;
        setX(nextX);
        setY(nextY);
        clampToBounds();
    }

    @Override
    public void onRelease(double mouseX, double mouseY) {
        pressed = false;
        dragging = false;
        if (!movedDuringClick && active) action.run();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }

    private void clampToBounds() {
        setX(Math.max(0, Math.min(getX(), boundWidth - getWidth())));
        setY(Math.max(0, Math.min(getY(), boundHeight - getHeight())));
    }
}
