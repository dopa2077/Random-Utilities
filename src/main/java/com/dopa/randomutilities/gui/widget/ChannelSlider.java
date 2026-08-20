package com.dopa.randomutilities.gui.widget;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.function.IntConsumer;

/** RGB channel slider used by cosmetic panels. */
public final class ChannelSlider extends AbstractWidget {
    private static final int TRACK_BG = 0xFF000000;
    private static final int KNOB_COLOR = 0xFFFFFFFF;
    private final IntConsumer onPreview;
    private final Runnable onRelease;
    private final int fillColor;
    private int value;
    private boolean dragging;

    public ChannelSlider(
            int x,
            int y,
            int width,
            int initialValue,
            int fillColor,
            IntConsumer onPreview,
            Runnable onRelease
    ) {
        super(x, y, width, 12, Component.empty());
        this.value = Mth.clamp(initialValue, 0, 255);
        this.fillColor = fillColor;
        this.onPreview = onPreview;
        this.onRelease = onRelease;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int newValue) {
        value = Mth.clamp(newValue, 0, 255);
    }

    public boolean isDraggingChannel() {
        return dragging;
    }

    private void applyValue(int newValue) {
        newValue = Mth.clamp(newValue, 0, 255);
        if (newValue != value) {
            value = newValue;
            onPreview.accept(value);
        }
    }

    private void updateFromMouse(double mouseX) {
        int innerWidth = Math.max(1, width - 4);
        double t = Mth.clamp((mouseX - (getX() + 2)) / innerWidth, 0.0D, 1.0D);
        applyValue((int) Math.round(t * 255.0D));
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        dragging = true;
        updateFromMouse(event.x());
    }

    @Override
    protected void onDrag(MouseButtonEvent event, double dx, double dy) {
        dragging = true;
        updateFromMouse(event.x());
    }

    @Override
    public void onRelease(MouseButtonEvent event) {
        if (dragging) {
            dragging = false;
            onRelease.run();
        }
        super.onRelease(event);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, Component.literal(Integer.toString(value)));
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int px = getX();
        int py = getY();
        graphics.fill(px, py, px + width, py + height, TRACK_BG);
        int innerWidth = width - 4;
        int fillWidth = (int) (innerWidth * (value / 255.0F));
        if (fillWidth > 0) {
            graphics.fill(px + 2, py + 2, px + 2 + fillWidth, py + height - 2, 0xFF000000 | fillColor);
        }
        int knobX = px + 2 + Mth.clamp((int) (innerWidth * (value / 255.0F)) - 1, 0, Math.max(0, innerWidth - 2));
        graphics.fill(knobX, py + 1, knobX + 2, py + height - 1, KNOB_COLOR);
    }
}
