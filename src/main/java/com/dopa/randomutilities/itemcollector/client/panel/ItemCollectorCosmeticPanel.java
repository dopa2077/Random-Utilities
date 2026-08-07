package com.dopa.randomutilities.itemcollector.client.panel;

import com.dopa.randomutilities.client.gui.AttachedPanel;
import com.dopa.randomutilities.client.gui.PanelAnchor;
import com.dopa.randomutilities.itemcollector.client.ItemCollectorScreen;
import com.dopa.randomutilities.itemcollector.network.ItemCollectorSettingPayload;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.function.IntConsumer;

/** Hitbox colour RGB controls and particle enable toggle for item collectors. */
public final class ItemCollectorCosmeticPanel extends AttachedPanel {
    private static final int BG = 0xFF7B5A96;
    private static final int SLIDER_W = 68;
    private static final int LABEL_COL_W = 40;
    private static final int RED_Y = 28;
    private static final int GREEN_Y = 44;
    private static final int BLUE_Y = 60;
    private static final int SWATCH_H = (BLUE_Y + 12) - RED_Y;
    private static final int SWATCH_GAP = 4;
    private static final int PARTICLES_LABEL_Y = 80;
    private static final int PARTICLES_BUTTON_Y = 92;
    private static final int PARTICLES_BUTTON_H = 18;
    private static final int PANEL_H = 120;
    private static final ItemStack DYE_ICON = new ItemStack(Items.DYE.pink());

    private final ItemCollectorScreen screen;
    private ChannelSlider redSlider;
    private ChannelSlider greenSlider;
    private ChannelSlider blueSlider;
    private Button particlesButton;
    private boolean widgetsCreated;
    private int pendingColor;
    private boolean suppressCommit;

    public ItemCollectorCosmeticPanel(ItemCollectorScreen screen) {
        super(
                PanelAnchor.LEFT_LOW,
                142,
                PANEL_H,
                BG,
                Component.translatable("gui.dopasrandomutilities.panel.cosmetic")
        );
        this.screen = screen;
    }

    public void initWidgets() {
        if (widgetsCreated) {
            return;
        }
        widgetsCreated = true;
        pendingColor = screen.getMenu().getOverlayColor();

        redSlider = channelSlider(0, 0, (pendingColor >> 16) & 0xFF, 0xFF0000);
        greenSlider = channelSlider(0, 0, (pendingColor >> 8) & 0xFF, 0x00FF00);
        blueSlider = channelSlider(0, 0, pendingColor & 0xFF, 0x0000FF);
        screen.addOverlayWidget(redSlider);
        screen.addOverlayWidget(greenSlider);
        screen.addOverlayWidget(blueSlider);

        particlesButton = Button.builder(Component.empty(), b -> toggleParticles())
                .bounds(0, 0, 80, PARTICLES_BUTTON_H)
                .tooltip(Tooltip.create(Component.translatable(
                        "gui.dopasrandomutilities.item_collector.particles.tooltip")))
                .build();
        screen.addOverlayWidget(particlesButton);
        refreshParticlesButton();

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
        if (!suppressCommit && pendingColor != screen.getMenu().getOverlayColor()) {
            ClientPacketDistributor.sendToServer(new ItemCollectorSettingPayload(
                    ItemCollectorSettingPayload.KIND_COLOR,
                    pendingColor
            ));
        }
    }

    private void toggleParticles() {
        boolean next = !screen.getMenu().isParticlesEnabled();
        ClientPacketDistributor.sendToServer(new ItemCollectorSettingPayload(
                ItemCollectorSettingPayload.KIND_PARTICLES,
                next ? 1 : 0
        ));
        particlesButton.setMessage(Component.translatable(next
                ? "gui.dopasrandomutilities.item_collector.particles.enabled"
                : "gui.dopasrandomutilities.item_collector.particles.disabled"));
    }

    private void refreshParticlesButton() {
        if (!widgetsCreated || particlesButton == null) {
            return;
        }
        boolean enabled = screen.getMenu().isParticlesEnabled();
        particlesButton.setMessage(Component.translatable(enabled
                ? "gui.dopasrandomutilities.item_collector.particles.enabled"
                : "gui.dopasrandomutilities.item_collector.particles.disabled"));
    }

    private void syncSlidersFromMenu() {
        if (!widgetsCreated || isDraggingSlider()) {
            return;
        }
        int color = screen.getMenu().getOverlayColor();
        if (color == pendingColor
                && redSlider.getValue() == ((color >> 16) & 0xFF)
                && greenSlider.getValue() == ((color >> 8) & 0xFF)
                && blueSlider.getValue() == (color & 0xFF)) {
            refreshParticlesButton();
            return;
        }
        int sliderColor = (redSlider.getValue() << 16) | (greenSlider.getValue() << 8) | blueSlider.getValue();
        if (color != pendingColor && sliderColor == pendingColor) {
            refreshParticlesButton();
            return;
        }
        pendingColor = color;
        suppressCommit = true;
        redSlider.setValue((color >> 16) & 0xFF);
        greenSlider.setValue((color >> 8) & 0xFF);
        blueSlider.setValue(color & 0xFF);
        suppressCommit = false;
        refreshParticlesButton();
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

        particlesButton.setX(bx + CONTENT_PAD);
        particlesButton.setY(by + PARTICLES_BUTTON_Y);
        particlesButton.setWidth(panelWidth - CONTENT_PAD * 2);
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
        particlesButton.visible = interactive;
        particlesButton.active = interactive;
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
                    Component.translatable("gui.dopasrandomutilities.item_collector.cosmetic.swatch.tooltip"),
                    mouseX, mouseY);
        }

        drawLabel(graphics, font, Component.translatable("gui.dopasrandomutilities.item_collector.particles"),
                bodyX, bodyY + PARTICLES_LABEL_Y);
    }

    private static final class ChannelSlider extends AbstractWidget {
        private static final int TRACK_BG = 0xFF000000;
        private static final int KNOB_COLOR = 0xFFFFFFFF;
        private final IntConsumer onPreview;
        private final Runnable onRelease;
        private final int fillColor;
        private int value;
        private boolean dragging;

        ChannelSlider(int x, int y, int width, int initialValue, int fillColor,
                      IntConsumer onPreview, Runnable onRelease) {
            super(x, y, width, 12, Component.empty());
            this.value = Mth.clamp(initialValue, 0, 255);
            this.fillColor = fillColor;
            this.onPreview = onPreview;
            this.onRelease = onRelease;
        }

        int getValue() {
            return value;
        }

        void setValue(int newValue) {
            value = Mth.clamp(newValue, 0, 255);
        }

        boolean isDraggingChannel() {
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
}
