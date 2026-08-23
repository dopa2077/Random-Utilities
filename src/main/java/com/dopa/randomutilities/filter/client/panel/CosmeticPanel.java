package com.dopa.randomutilities.filter.client.panel;

import com.dopa.randomutilities.gui.panel.AttachedPanel;
import com.dopa.randomutilities.gui.widget.ChannelSlider;
import com.dopa.randomutilities.gui.panel.PanelAnchor;

import com.dopa.randomutilities.filter.client.FilterScreen;
import com.dopa.randomutilities.filter.network.FilterSettingPayload;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import org.jetbrains.annotations.Nullable;

/**
 * Item colour RGB controls and optional selected-slot highlight match toggle.
 */
public final class CosmeticPanel extends AttachedPanel {
    private static final int BG = 0xFF7B5A96;
    private static final int SLIDER_W = 68;
    private static final int LABEL_COL_W = 40;
    private static final int RED_Y = 28;
    private static final int GREEN_Y = 44;
    private static final int BLUE_Y = 60;
    private static final int SWATCH_H = (BLUE_Y + 12) - RED_Y;
    private static final int SWATCH_GAP = 4;
    private static final int MATCH_LABEL_Y = 80;
    private static final int MATCH_BUTTON_Y = 94;
    private static final int MATCH_BUTTON_H = 18;
    private static final int TRAY_PAD = 4;
    private static final int HEIGHT_RGB_ONLY = 78;
    private static final int HEIGHT_FULL = 122;
    private static final ItemStack DYE_ICON = new ItemStack(Items.DYE.pink());

    private final FilterScreen screen;
    private final boolean showHighlight;
    private ChannelSlider redSlider;
    private ChannelSlider greenSlider;
    private ChannelSlider blueSlider;
    @Nullable
    private Button matchButton;
    private boolean widgetsCreated;
    private int pendingColor;
    private boolean suppressCommit;

    public CosmeticPanel(FilterScreen screen, PanelAnchor anchor, boolean showHighlight) {
        super(
                anchor,
                142,
                showHighlight ? HEIGHT_FULL : HEIGHT_RGB_ONLY,
                BG,
                Component.translatable("gui.dopasrandomutilities.panel.cosmetic")
        );
        this.screen = screen;
        this.showHighlight = showHighlight;
    }

    public void initWidgets() {
        if (widgetsCreated) {
            return;
        }
        widgetsCreated = true;
        pendingColor = screen.getMenu().getColor();

        redSlider = channelSlider(0, 0, (pendingColor >> 16) & 0xFF, 0xFF0000);
        greenSlider = channelSlider(0, 0, (pendingColor >> 8) & 0xFF, 0x00FF00);
        blueSlider = channelSlider(0, 0, pendingColor & 0xFF, 0x0000FF);
        screen.addOverlayWidget(redSlider);
        screen.addOverlayWidget(greenSlider);
        screen.addOverlayWidget(blueSlider);

        if (showHighlight) {
            matchButton = Button.builder(Component.empty(), b -> toggleMatch())
                    .bounds(0, 0, 80, MATCH_BUTTON_H)
                    .tooltip(Tooltip.create(Component.translatable("gui.dopasrandomutilities.panel.cosmetic.highlight_match.tooltip")))
                    .build();
            screen.addOverlayWidget(matchButton);
            refreshMatchButton();
        }

        updateWidgetVisibility(false);
    }

    private ChannelSlider channelSlider(int x, int y, int channel, int fillColor) {
        return new ChannelSlider(x, y, SLIDER_W, channel, fillColor,
                ignored -> onChannelPreview(),
                this::commitPendingColor);
    }

    private void onChannelPreview() {
        pendingColor = (redSlider.getValue() << 16) | (greenSlider.getValue() << 8) | blueSlider.getValue();
    }

    private void commitPendingColor() {
        pendingColor = (redSlider.getValue() << 16) | (greenSlider.getValue() << 8) | blueSlider.getValue();
        if (!suppressCommit && pendingColor != screen.getMenu().getColor()) {
            ClientPacketDistributor.sendToServer(FilterSettingPayload.color(pendingColor));
        }
    }

    private void toggleMatch() {
        if (matchButton == null) {
            return;
        }
        boolean next = !screen.getMenu().isHighlightMatchColor();
        ClientPacketDistributor.sendToServer(FilterSettingPayload.highlightMatch(next));
        matchButton.setMessage(Component.translatable(next
                ? "gui.dopasrandomutilities.panel.cosmetic.highlight_match.enabled"
                : "gui.dopasrandomutilities.panel.cosmetic.highlight_match.disabled"));
    }

    private void refreshMatchButton() {
        if (!widgetsCreated || matchButton == null) {
            return;
        }
        boolean match = screen.getMenu().isHighlightMatchColor();
        matchButton.setMessage(Component.translatable(match
                ? "gui.dopasrandomutilities.panel.cosmetic.highlight_match.enabled"
                : "gui.dopasrandomutilities.panel.cosmetic.highlight_match.disabled"));
    }

    private void syncSlidersFromMenu() {
        if (!widgetsCreated || isDraggingSlider()) {
            return;
        }
        int color = screen.getMenu().getColor();
        if (color == pendingColor
                && redSlider.getValue() == ((color >> 16) & 0xFF)
                && greenSlider.getValue() == ((color >> 8) & 0xFF)
                && blueSlider.getValue() == (color & 0xFF)) {
            refreshMatchButton();
            return;
        }
        // Optimistic local color while waiting for server ack — don't snap knobs back to old menu.
        int sliderColor = (redSlider.getValue() << 16) | (greenSlider.getValue() << 8) | blueSlider.getValue();
        if (color != pendingColor && sliderColor == pendingColor) {
            refreshMatchButton();
            return;
        }
        pendingColor = color;
        suppressCommit = true;
        redSlider.setValue((color >> 16) & 0xFF);
        greenSlider.setValue((color >> 8) & 0xFF);
        blueSlider.setValue(color & 0xFF);
        suppressCommit = false;
        refreshMatchButton();
    }

    private boolean isDraggingSlider() {
        return redSlider.isDraggingChannel() || greenSlider.isDraggingChannel() || blueSlider.isDraggingChannel();
    }

    @Override
    protected void onTick() {
        if (contentsInteractive()) {
            syncSlidersFromMenu();
        }
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

        if (matchButton != null) {
            matchButton.setX(bx + CONTENT_PAD);
            matchButton.setY(by + MATCH_BUTTON_Y);
            matchButton.setWidth(panelWidth - CONTENT_PAD * 2);
            matchButton.setHeight(MATCH_BUTTON_H);
        }
    }

    private TrayBounds matchTray(int bodyX, int bodyY) {
        return innerButtonTray(bodyX, bodyY, MATCH_BUTTON_Y, MATCH_BUTTON_H, TRAY_PAD);
    }

    @Override
    public boolean isMouseOverDecorativeArea(double mouseX, double mouseY, int leftPos, int topPos, int imageWidth) {
        if (!contentsInteractive() || matchButton == null) {
            return false;
        }
        TrayBounds tray = matchTray(bodyXOpen(leftPos, imageWidth), bodyY(topPos));
        return isMouseOverRect(mouseX, mouseY, tray.x(), tray.y(), tray.width(), tray.height());
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
        if (matchButton != null) {
            matchButton.visible = interactive;
            matchButton.active = interactive;
        }
    }

    @Override
    protected void renderIcon(GuiGraphicsExtractor graphics, Font font, int centerX, int centerY) {
        graphics.item(DYE_ICON, centerX - 8, centerY - 8, centerX ^ centerY);
    }

    @Override
    protected void renderContents(GuiGraphicsExtractor graphics, Font font, int bodyX, int bodyY,
                                  int mouseX, int mouseY, float partialTick) {
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
                    Component.translatable("gui.dopasrandomutilities.panel.cosmetic.swatch.tooltip"),
                    mouseX, mouseY);
        }

        if (showHighlight) {
            drawLabel(graphics, font, Component.translatable("gui.dopasrandomutilities.panel.cosmetic.highlight_match"),
                    bodyX, bodyY + MATCH_LABEL_Y);
            renderTray(graphics, matchTray(bodyX, bodyY), BG);
        }
    }
}
