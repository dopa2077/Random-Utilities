package com.dopa.randomutilities.filteritem.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.function.IntConsumer;

/** Reusable RGB picker overlay. Attach to any screen via {@link ColorPickerHost}. */
public class FilterColorPicker {
    public static final int WIDTH = 148;
    public static final int HEIGHT = 96;

    private static final int LABEL_COLOR = 0xFF404040;
    private static final int MUTED_LABEL_COLOR = 0xFF606060;
    private static final int SLIDER_W = 72;
    private static final int SLIDER_X = 48;
    private static final int RED_Y = 38;
    private static final int GREEN_Y = 54;
    private static final int BLUE_Y = 70;
    private static final int SWATCH_Y = 36;

    private boolean open;
    private int x;
    private int y;
    private ChannelSlider redSlider;
    private ChannelSlider greenSlider;
    private ChannelSlider blueSlider;
    private Button closeButton;
    private int pendingColor;

    public boolean isOpen() {
        return open;
    }

    public boolean contains(double mouseX, double mouseY) {
        return open && mouseX >= x && mouseX < x + WIDTH && mouseY >= y && mouseY < y + HEIGHT;
    }

    public void open(ColorPickerHost host) {
        if (open) {
            return;
        }
        open = true;
        this.x = (host.width() - WIDTH) / 2;
        this.y = (host.height() - HEIGHT) / 2;
        host.clearFocus();
        this.closeButton = Button.builder(Component.literal("\u00D7"), button -> close(host))
                .bounds(this.x + WIDTH - 18, this.y + 4, 12, 12).build();
        this.pendingColor = host.getPickerColor();
        this.redSlider = channelSlider(this.x + SLIDER_X, this.y + RED_Y, (pendingColor >> 16) & 0xFF, 0xFF0000);
        this.greenSlider = channelSlider(this.x + SLIDER_X, this.y + GREEN_Y, (pendingColor >> 8) & 0xFF, 0x00FF00);
        this.blueSlider = channelSlider(this.x + SLIDER_X, this.y + BLUE_Y, pendingColor & 0xFF, 0x0000FF);
        host.addPickerWidget(closeButton);
        host.addPickerWidget(redSlider);
        host.addPickerWidget(greenSlider);
        host.addPickerWidget(blueSlider);
    }

    public void close(ColorPickerHost host) {
        if (!open) {
            return;
        }
        commitColor(host);
        open = false;
        host.clearFocus();
        removeWidget(host, closeButton);
        removeWidget(host, redSlider);
        removeWidget(host, greenSlider);
        removeWidget(host, blueSlider);
        closeButton = null;
        redSlider = null;
        greenSlider = null;
        blueSlider = null;
    }

    private static void removeWidget(ColorPickerHost host, net.minecraft.client.gui.components.events.GuiEventListener widget) {
        if (widget != null) {
            host.removePickerWidget(widget);
        }
    }

    private ChannelSlider channelSlider(int sliderX, int sliderY, int channel, int fillColor) {
        return new ChannelSlider(sliderX, sliderY, SLIDER_W, channel, fillColor, ignored -> updatePendingColor());
    }

    private void updatePendingColor() {
        pendingColor = (redSlider.getValue() << 16) | (greenSlider.getValue() << 8) | blueSlider.getValue();
    }

    private void commitColor(ColorPickerHost host) {
        updatePendingColor();
        if (pendingColor != host.getPickerColor()) {
            host.onPickerColorCommitted(pendingColor);
        }
    }

    public void renderOnTop(GuiGraphicsExtractor graphics, ColorPickerHost host, int mouseX, int mouseY, float partialTick) {
        if (!open) {
            return;
        }
        VanillaContainerPanel.blit(graphics, x, y, WIDTH, HEIGHT);
        graphics.text(host.getFont(), Component.translatable("gui.dopasrandomutilities.color"), x + 8, y + 6, LABEL_COLOR, false);
        graphics.text(host.getFont(), Component.translatable("gui.dopasrandomutilities.channel_red"), x + 8, y + RED_Y + 2, MUTED_LABEL_COLOR, false);
        graphics.text(host.getFont(), Component.translatable("gui.dopasrandomutilities.channel_green"), x + 8, y + GREEN_Y + 2, MUTED_LABEL_COLOR, false);
        graphics.text(host.getFont(), Component.translatable("gui.dopasrandomutilities.channel_blue"), x + 8, y + BLUE_Y + 2, MUTED_LABEL_COLOR, false);
        int swatchX = x + WIDTH - 22;
        int swatchY = y + SWATCH_Y;
        graphics.fill(swatchX - 1, swatchY - 1, swatchX + 11, swatchY + 13, 0xFF000000);
        graphics.fill(swatchX, swatchY, swatchX + 10, swatchY + 12, 0xFF000000 | pendingColor);
        renderWidget(closeButton, graphics, mouseX, mouseY, partialTick);
        renderWidget(redSlider, graphics, mouseX, mouseY, partialTick);
        renderWidget(greenSlider, graphics, mouseX, mouseY, partialTick);
        renderWidget(blueSlider, graphics, mouseX, mouseY, partialTick);
    }

    private static void renderWidget(Renderable widget, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (widget != null) {
            widget.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }
    }

    private static final class ChannelSlider extends AbstractWidget {
        private static final int TRACK_BG = 0xFF000000;
        private static final int KNOB_COLOR = 0xFFFFFFFF;
        private final IntConsumer onValueChanged;
        private final int fillColor;
        private int value;

        ChannelSlider(int x, int y, int width, int initialValue, int fillColor, IntConsumer onValueChanged) {
            super(x, y, width, 12, Component.empty());
            this.value = Mth.clamp(initialValue, 0, 255);
            this.fillColor = fillColor;
            this.onValueChanged = onValueChanged;
        }

        int getValue() {
            return value;
        }

        private void applyValue(int newValue) {
            newValue = Mth.clamp(newValue, 0, 255);
            if (newValue != value) {
                value = newValue;
                onValueChanged.accept(value);
            }
        }

        private void updateFromMouse(double mouseX) {
            int innerWidth = Math.max(1, width - 4);
            double t = Mth.clamp((mouseX - (getX() + 2)) / innerWidth, 0.0D, 1.0D);
            applyValue((int) Math.round(t * 255.0D));
        }

        @Override
        public void onClick(MouseButtonEvent event, boolean doubleClick) {
            updateFromMouse(event.x());
        }

        @Override
        protected void onDrag(MouseButtonEvent event, double dx, double dy) {
            updateFromMouse(event.x());
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
            int knobX = px + 2 + Mth.clamp((int) (innerWidth * (value / 255.0F)) - 1, 0, innerWidth - 2);
            graphics.fill(knobX, py + 1, knobX + 2, py + height - 1, KNOB_COLOR);
        }
    }
}
