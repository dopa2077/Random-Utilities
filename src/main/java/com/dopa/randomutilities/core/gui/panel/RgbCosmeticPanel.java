package com.dopa.randomutilities.core.gui.panel;

import com.dopa.randomutilities.core.gui.widget.ChannelSlider;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/** RGB tray with an optional extra toggle (mute, particles, highlight match). */
public final class RgbCosmeticPanel extends AttachedPanel {
    private static final int BG = 0xFF7B5A96;
    private static final int SLIDER_W = 68;
    private static final int LABEL_COL_W = 40;
    private static final int RED_Y = 28;
    private static final int GREEN_Y = 44;
    private static final int BLUE_Y = 60;
    private static final int SWATCH_H = (BLUE_Y + 12) - RED_Y;
    private static final int SWATCH_GAP = 4;
    private static final int TOGGLE_LABEL_Y = 80;
    private static final int TOGGLE_BUTTON_Y = 94;
    private static final int TOGGLE_BUTTON_H = 18;
    private static final int TRAY_PAD = 4;
    private static final int HEIGHT_RGB_ONLY = 78;
    private static final int HEIGHT_FULL = 122;
    private static final ItemStack DYE_ICON = new ItemStack(Items.DYE.pink());

    @FunctionalInterface
    public interface WidgetSink {
        <T extends GuiEventListener & Renderable & NarratableEntry> T add(T widget);
    }

    public record Toggle(
            String headerKey,
            String tooltipKey,
            String onKey,
            String offKey,
            BooleanSupplier value,
            Runnable onToggle
    ) {}

    private final IntSupplier color;
    private final IntConsumer commitColor;
    private final WidgetSink widgets;
    private final String swatchTooltipKey;
    @Nullable
    private final Toggle toggle;
    private ChannelSlider redSlider;
    private ChannelSlider greenSlider;
    private ChannelSlider blueSlider;
    @Nullable
    private Button toggleButton;
    private boolean widgetsCreated;
    private int pendingColor;
    private boolean suppressCommit;

    public RgbCosmeticPanel(
            PanelAnchor anchor,
            IntSupplier color,
            IntConsumer commitColor,
            WidgetSink widgets,
            String swatchTooltipKey,
            @Nullable Toggle toggle
    ) {
        super(
                anchor,
                142,
                toggle != null ? HEIGHT_FULL : HEIGHT_RGB_ONLY,
                BG,
                Component.translatable("gui.dopasrandomutilities.panel.cosmetic")
        );
        this.color = color;
        this.commitColor = commitColor;
        this.widgets = widgets;
        this.swatchTooltipKey = swatchTooltipKey;
        this.toggle = toggle;
    }

    public void initWidgets() {
        if (widgetsCreated) {
            return;
        }
        widgetsCreated = true;
        pendingColor = color.getAsInt();

        redSlider = channelSlider((pendingColor >> 16) & 0xFF, 0xFF0000);
        greenSlider = channelSlider((pendingColor >> 8) & 0xFF, 0x00FF00);
        blueSlider = channelSlider(pendingColor & 0xFF, 0x0000FF);
        widgets.add(redSlider);
        widgets.add(greenSlider);
        widgets.add(blueSlider);

        if (toggle != null) {
            toggleButton = Button.builder(Component.empty(), b -> fireToggle())
                    .bounds(0, 0, 80, TOGGLE_BUTTON_H)
                    .tooltip(Tooltip.create(Component.translatable(toggle.tooltipKey())))
                    .build();
            widgets.add(toggleButton);
            refreshToggleButton();
        }
        updateWidgetVisibility(false);
    }

    private ChannelSlider channelSlider(int channel, int fillColor) {
        return new ChannelSlider(0, 0, SLIDER_W, channel, fillColor,
                ignored -> onChannelPreview(),
                this::commitPendingColor);
    }

    private void onChannelPreview() {
        pendingColor = (redSlider.getValue() << 16) | (greenSlider.getValue() << 8) | blueSlider.getValue();
    }

    private void commitPendingColor() {
        pendingColor = (redSlider.getValue() << 16) | (greenSlider.getValue() << 8) | blueSlider.getValue();
        if (!suppressCommit && pendingColor != color.getAsInt()) {
            commitColor.accept(pendingColor);
        }
    }

    private void fireToggle() {
        if (toggle == null || toggleButton == null) {
            return;
        }
        boolean next = !toggle.value().getAsBoolean();
        toggle.onToggle().run();
        toggleButton.setMessage(toggleLabel(next));
    }

    private void refreshToggleButton() {
        if (!widgetsCreated || toggle == null || toggleButton == null) {
            return;
        }
        toggleButton.setMessage(toggleLabel(toggle.value().getAsBoolean()));
    }

    private Component toggleLabel(boolean on) {
        return Component.translatable(on ? toggle.onKey() : toggle.offKey());
    }

    private void syncSlidersFromMenu() {
        if (!widgetsCreated || isDraggingSlider()) {
            return;
        }
        int menuColor = color.getAsInt();
        if (menuColor == pendingColor
                && redSlider.getValue() == ((menuColor >> 16) & 0xFF)
                && greenSlider.getValue() == ((menuColor >> 8) & 0xFF)
                && blueSlider.getValue() == (menuColor & 0xFF)) {
            refreshToggleButton();
            return;
        }
        int sliderColor = (redSlider.getValue() << 16) | (greenSlider.getValue() << 8) | blueSlider.getValue();
        if (menuColor != pendingColor && sliderColor == pendingColor) {
            refreshToggleButton();
            return;
        }
        pendingColor = menuColor;
        suppressCommit = true;
        redSlider.setValue((menuColor >> 16) & 0xFF);
        greenSlider.setValue((menuColor >> 8) & 0xFF);
        blueSlider.setValue(menuColor & 0xFF);
        suppressCommit = false;
        refreshToggleButton();
    }

    private boolean isDraggingSlider() {
        return redSlider.isDraggingChannel() || greenSlider.isDraggingChannel() || blueSlider.isDraggingChannel();
    }

    public boolean isMouseOverInteractiveWidget(double mouseX, double mouseY) {
        if (!widgetsCreated || !contentsInteractive()) {
            return false;
        }
        return isOver(redSlider, mouseX, mouseY)
                || isOver(greenSlider, mouseX, mouseY)
                || isOver(blueSlider, mouseX, mouseY)
                || isOver(toggleButton, mouseX, mouseY);
    }

    private static boolean isOver(AbstractWidget widget, double mouseX, double mouseY) {
        return widget != null
                && widget.visible
                && mouseX >= widget.getX()
                && mouseY >= widget.getY()
                && mouseX < widget.getX() + widget.getWidth()
                && mouseY < widget.getY() + widget.getHeight();
    }

    private TrayBounds toggleTray(int bodyX, int bodyY) {
        return innerButtonTray(bodyX, bodyY, TOGGLE_BUTTON_Y, TOGGLE_BUTTON_H, TRAY_PAD);
    }

    @Override
    public boolean isMouseOverDecorativeArea(double mouseX, double mouseY, int leftPos, int topPos, int imageWidth) {
        if (!contentsInteractive() || toggle == null) {
            return false;
        }
        if (isMouseOverInteractiveWidget(mouseX, mouseY)) {
            return false;
        }
        TrayBounds tray = toggleTray(bodyXOpen(leftPos, imageWidth), bodyY(topPos));
        return isMouseOverRect(mouseX, mouseY, tray.x(), tray.y(), tray.width(), tray.height());
    }

    @Override
    public void layoutWidgets(int leftPos, int topPos, int imageWidth) {
        if (!widgetsCreated) {
            return;
        }
        int bx = bodyXOpen(leftPos, imageWidth);
        int by = bodyY(topPos);
        int sliderX = bx + CONTENT_PAD + LABEL_COL_W;

        redSlider.setX(sliderX);
        redSlider.setY(by + RED_Y);
        greenSlider.setX(sliderX);
        greenSlider.setY(by + GREEN_Y);
        blueSlider.setX(sliderX);
        blueSlider.setY(by + BLUE_Y);

        if (toggleButton != null) {
            toggleButton.setX(bx + CONTENT_PAD);
            toggleButton.setY(by + TOGGLE_BUTTON_Y);
            toggleButton.setWidth(panelWidth - CONTENT_PAD * 2);
            toggleButton.setHeight(TOGGLE_BUTTON_H);
        }
    }

    @Override
    protected void updateWidgetVisibility(boolean interactive) {
        if (!widgetsCreated) {
            return;
        }
        redSlider.visible = interactive;
        greenSlider.visible = interactive;
        blueSlider.visible = interactive;
        redSlider.active = interactive;
        greenSlider.active = interactive;
        blueSlider.active = interactive;
        if (toggleButton != null) {
            toggleButton.visible = interactive;
            toggleButton.active = interactive;
        }
    }

    @Override
    protected void onTick() {
        if (contentsInteractive()) {
            syncSlidersFromMenu();
        }
    }

    @Override
    protected void renderIcon(GuiGraphicsExtractor graphics, Font font, int centerX, int centerY) {
        graphics.item(DYE_ICON, centerX - 8, centerY - 8, centerX ^ centerY);
    }

    @Override
    protected void renderContents(
            GuiGraphicsExtractor graphics,
            Font font,
            int bodyX,
            int bodyY,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        renderTitleRow(graphics, font, bodyX, bodyY);

        drawLabel(graphics, font, Component.translatable("gui.dopasrandomutilities.channel_red"),
                bodyX, bodyX + CONTENT_PAD, bodyY + RED_Y + 2);
        drawLabel(graphics, font, Component.translatable("gui.dopasrandomutilities.channel_green"),
                bodyX, bodyX + CONTENT_PAD, bodyY + GREEN_Y + 2);
        drawLabel(graphics, font, Component.translatable("gui.dopasrandomutilities.channel_blue"),
                bodyX, bodyX + CONTENT_PAD, bodyY + BLUE_Y + 2);

        int swatchX = bodyX + CONTENT_PAD + LABEL_COL_W + SLIDER_W + SWATCH_GAP;
        int swatchY = bodyY + RED_Y;
        graphics.fill(swatchX - 1, swatchY - 1, swatchX + 11, swatchY + SWATCH_H + 1, 0xFF000000);
        graphics.fill(swatchX, swatchY, swatchX + 10, swatchY + SWATCH_H, 0xFF000000 | pendingColor);
        if (isMouseOverRect(mouseX, mouseY, swatchX, swatchY, 10, SWATCH_H)) {
            graphics.setTooltipForNextFrame(font, Component.translatable(swatchTooltipKey), mouseX, mouseY);
        }

        if (toggle != null) {
            drawLabel(graphics, font, Component.translatable(toggle.headerKey()), bodyX, bodyY + TOGGLE_LABEL_Y);
            renderTray(graphics, toggleTray(bodyX, bodyY), BG);
        }
    }
}
