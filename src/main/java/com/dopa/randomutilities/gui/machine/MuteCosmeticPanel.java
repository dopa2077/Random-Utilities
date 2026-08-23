package com.dopa.randomutilities.gui.machine;

import com.dopa.randomutilities.gui.panel.AttachedPanel;
import com.dopa.randomutilities.gui.panel.PanelAnchor;
import com.dopa.randomutilities.gui.widget.ChannelSlider;
import com.dopa.randomutilities.util.WorkingVolume;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Range-highlight RGB controls with a sounds toggle underneath, matching item collectors. */
public final class MuteCosmeticPanel extends AttachedPanel {
    private static final int BG = 0xFF7B5A96;
    private static final int SLIDER_W = 68;
    private static final int LABEL_COL_W = 40;
    private static final int RED_Y = 28;
    private static final int GREEN_Y = 44;
    private static final int BLUE_Y = 60;
    private static final int SWATCH_H = (BLUE_Y + 12) - RED_Y;
    private static final int SWATCH_GAP = 4;
    private static final int MUTE_LABEL_Y = 80;
    private static final int MUTE_BUTTON_Y = 94;
    private static final int MUTE_BUTTON_H = 18;
    private static final int TRAY_PAD = 4;
    private static final int PANEL_H = 122;
    private static final ItemStack DYE_ICON = new ItemStack(Items.DYE.pink());

    private final VolumeMachineGui gui;
    private ChannelSlider redSlider;
    private ChannelSlider greenSlider;
    private ChannelSlider blueSlider;
    private Button muteButton;
    private boolean widgetsCreated;
    private int pendingColor;
    private boolean suppressCommit;

    public MuteCosmeticPanel(VolumeMachineGui gui) {
        super(
                PanelAnchor.LEFT_LOW,
                142,
                PANEL_H,
                BG,
                Component.translatable("gui.dopasrandomutilities.panel.cosmetic")
        );
        this.gui = gui;
    }

    public void initWidgets() {
        if (widgetsCreated) {
            return;
        }
        widgetsCreated = true;
        pendingColor = gui.overlayColor();

        redSlider = channelSlider((pendingColor >> 16) & 0xFF, 0xFF0000);
        greenSlider = channelSlider((pendingColor >> 8) & 0xFF, 0x00FF00);
        blueSlider = channelSlider(pendingColor & 0xFF, 0x0000FF);
        gui.addOverlayWidget(redSlider);
        gui.addOverlayWidget(greenSlider);
        gui.addOverlayWidget(blueSlider);

        muteButton = Button.builder(Component.empty(), b -> toggleMute())
                .bounds(0, 0, 80, MUTE_BUTTON_H)
                .tooltip(Tooltip.create(Component.translatable(
                        "gui.dopasrandomutilities.working_volume.mute.tooltip")))
                .build();
        gui.addOverlayWidget(muteButton);
        refreshMuteButton();
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
        if (!suppressCommit && pendingColor != gui.overlayColor()) {
            gui.sendVolumeSetting(WorkingVolume.KIND_COLOR, pendingColor);
        }
    }

    private void toggleMute() {
        boolean next = !gui.isMuted();
        gui.sendVolumeSetting(WorkingVolume.KIND_MUTE, next ? 1 : 0);
        muteButton.setMessage(muteLabel(next));
    }

    private void refreshMuteButton() {
        if (!widgetsCreated || muteButton == null) {
            return;
        }
        muteButton.setMessage(muteLabel(gui.isMuted()));
    }

    private static Component muteLabel(boolean muted) {
        return Component.translatable(muted
                ? "gui.dopasrandomutilities.working_volume.mute.disabled"
                : "gui.dopasrandomutilities.working_volume.mute.enabled");
    }

    private void syncSlidersFromMenu() {
        if (!widgetsCreated || isDraggingSlider()) {
            return;
        }
        int color = gui.overlayColor();
        if (color == pendingColor
                && redSlider.getValue() == ((color >> 16) & 0xFF)
                && greenSlider.getValue() == ((color >> 8) & 0xFF)
                && blueSlider.getValue() == (color & 0xFF)) {
            refreshMuteButton();
            return;
        }
        int sliderColor = (redSlider.getValue() << 16) | (greenSlider.getValue() << 8) | blueSlider.getValue();
        if (color != pendingColor && sliderColor == pendingColor) {
            refreshMuteButton();
            return;
        }
        pendingColor = color;
        suppressCommit = true;
        redSlider.setValue((color >> 16) & 0xFF);
        greenSlider.setValue((color >> 8) & 0xFF);
        blueSlider.setValue(color & 0xFF);
        suppressCommit = false;
        refreshMuteButton();
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
                || isOver(muteButton, mouseX, mouseY);
    }

    private static boolean isOver(AbstractWidget widget, double mouseX, double mouseY) {
        return widget != null
                && widget.visible
                && mouseX >= widget.getX()
                && mouseY >= widget.getY()
                && mouseX < widget.getX() + widget.getWidth()
                && mouseY < widget.getY() + widget.getHeight();
    }

    private TrayBounds muteTray(int bodyX, int bodyY) {
        return innerButtonTray(bodyX, bodyY, MUTE_BUTTON_Y, MUTE_BUTTON_H, TRAY_PAD);
    }

    @Override
    public boolean isMouseOverDecorativeArea(double mouseX, double mouseY, int leftPos, int topPos, int imageWidth) {
        if (!contentsInteractive()) {
            return false;
        }
        if (isMouseOverInteractiveWidget(mouseX, mouseY)) {
            return false;
        }
        TrayBounds tray = muteTray(bodyXOpen(leftPos, imageWidth), bodyY(topPos));
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

        muteButton.setX(bx + CONTENT_PAD);
        muteButton.setY(by + MUTE_BUTTON_Y);
        muteButton.setWidth(panelWidth - CONTENT_PAD * 2);
        muteButton.setHeight(MUTE_BUTTON_H);
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
        muteButton.visible = interactive;
        muteButton.active = interactive;
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
            graphics.setTooltipForNextFrame(font,
                    Component.translatable("gui.dopasrandomutilities.working_volume.cosmetic.swatch.tooltip"),
                    mouseX, mouseY);
        }

        drawLabel(
                graphics,
                font,
                Component.translatable("gui.dopasrandomutilities.working_volume.mute"),
                bodyX,
                bodyY + MUTE_LABEL_Y
        );
        renderTray(graphics, muteTray(bodyX, bodyY), BG);
    }
}
