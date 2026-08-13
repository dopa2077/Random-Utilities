package com.dopa.randomutilities.itemcollector.client.panel;

import com.dopa.randomutilities.client.gui.AttachedPanel;
import com.dopa.randomutilities.client.gui.PanelAnchor;
import com.dopa.randomutilities.itemcollector.ItemCollectorType;
import com.dopa.randomutilities.itemcollector.client.ItemCollectorScreen;
import com.dopa.randomutilities.itemcollector.config.ItemCollectorConfig;
import com.dopa.randomutilities.itemcollector.menu.ItemCollectorMenu;
import com.dopa.randomutilities.itemcollector.network.ItemCollectorSettingPayload;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.Nullable;

public final class ItemCollectorConfigPanel extends AttachedPanel {
    private static final int BG = 0xFF1A4548;
    private static final ItemStack COMPARATOR_ICON = new ItemStack(Items.COMPARATOR);
    private static final int STEPPER_W = 18;
    private static final int STEPPER_H = 12;
    private static final int VALUE_W = 24;
    private static final int TRAY_PAD = 3;
    private static final int PANEL_W = 136;
    /** Keep steppers clear of the panel attachment bevel. */
    private static final int STEPPER_RIGHT_INSET = TRAY_PAD + 2;

    /** Labels and stepper rows share the same Y (compact left/right layout). */
    private static final int RANGE_X_ROW_Y = 30;
    private static final int RANGE_Y_ROW_Y = 46;
    private static final int RANGE_Z_ROW_Y = 62;
    private static final int DELAY_LABEL_Y = 84;
    private static final int DELAY_BOX_Y = 94;
    private static final int BATCH_LABEL_Y = 116;
    private static final int BATCH_BOX_Y = 126;
    private static final int LOS_LABEL_Y = 144;
    private static final int LOS_BUTTON_Y = 158;
    private static final int LOS_BUTTON_H = 14;

    private final ItemCollectorScreen screen;
    private StepperButton rangeXMinus;
    private StepperButton rangeXPlus;
    private StepperButton rangeYMinus;
    private StepperButton rangeYPlus;
    private StepperButton rangeZMinus;
    private StepperButton rangeZPlus;
    private EditBox delayBox;
    private EditBox batchBox;
    private Button infiniteDelayButton;
    private Button losButton;
    private boolean widgetsCreated;
    private boolean delayWasFocused;
    private boolean batchWasFocused;
    @Nullable
    private String pendingDelay;
    @Nullable
    private String pendingBatch;

    public ItemCollectorConfigPanel(ItemCollectorScreen screen) {
        super(
                PanelAnchor.LEFT_BELOW,
                PANEL_W,
                screen.getMenu().collectorType() == ItemCollectorType.ADVANCED ? 180 : 148,
                BG,
                Component.translatable("gui.dopasrandomutilities.panel.config")
        );
        this.screen = screen;
    }

    public void initWidgets() {
        if (widgetsCreated) {
            return;
        }
        widgetsCreated = true;
        ItemCollectorType type = screen.getMenu().collectorType();
        int innerWidth = panelWidth - CONTENT_PAD * 2;

        rangeXMinus = stepperButton("-", ItemCollectorSettingPayload.KIND_RANGE_X, -1);
        rangeXPlus = stepperButton("+", ItemCollectorSettingPayload.KIND_RANGE_X, 1);
        rangeYMinus = stepperButton("-", ItemCollectorSettingPayload.KIND_RANGE_Y, -1);
        rangeYPlus = stepperButton("+", ItemCollectorSettingPayload.KIND_RANGE_Y, 1);
        rangeZMinus = stepperButton("-", ItemCollectorSettingPayload.KIND_RANGE_Z, -1);
        rangeZPlus = stepperButton("+", ItemCollectorSettingPayload.KIND_RANGE_Z, 1);

        delayBox = axisBox(innerWidth - 24);
        delayBox.setTooltip(Tooltip.create(Component.translatable(
                "gui.dopasrandomutilities.item_collector.pickup_delay.tooltip")));
        batchBox = axisBox(innerWidth);
        batchBox.setTooltip(Tooltip.create(Component.translatable(
                "gui.dopasrandomutilities.item_collector.pickup_batch.tooltip")));

        infiniteDelayButton = Button.builder(Component.literal("\u221E"), b -> setInfiniteDelay())
                .bounds(0, 0, 20, 12)
                .tooltip(Tooltip.create(Component.translatable("gui.dopasrandomutilities.item_collector.infinite_delay")))
                .build();

        if (type.supportsLineOfSight()) {
            losButton = Button.builder(Component.empty(), b -> toggleLos())
                    .bounds(0, 0, innerWidth, LOS_BUTTON_H)
                    .build();
            screen.addOverlayWidget(losButton);
            refreshLosButton();
        }

        screen.addOverlayWidget(rangeXMinus);
        screen.addOverlayWidget(rangeXPlus);
        screen.addOverlayWidget(rangeYMinus);
        screen.addOverlayWidget(rangeYPlus);
        screen.addOverlayWidget(rangeZMinus);
        screen.addOverlayWidget(rangeZPlus);
        screen.addOverlayWidget(delayBox);
        screen.addOverlayWidget(batchBox);
        screen.addOverlayWidget(infiniteDelayButton);
        syncUnfocusedFromMenu();
        updateWidgetVisibility(false);
    }

    private StepperButton stepperButton(String label, byte kind, int delta) {
        return new StepperButton(label, stepperTooltip(delta < 0), () -> adjustRange(kind, delta));
    }

    private static Component stepperTooltip(boolean decrease) {
        return Component.translatable(decrease
                        ? "gui.dopasrandomutilities.item_collector.range_decrease"
                        : "gui.dopasrandomutilities.item_collector.range_increase")
                .append("\n")
                .append(Component.translatable(decrease
                                ? "gui.dopasrandomutilities.item_collector.range_shift_min"
                                : "gui.dopasrandomutilities.item_collector.range_shift_max")
                        .withStyle(ChatFormatting.GRAY));
    }

    private static int stepperGroupWidth() {
        return STEPPER_W + 2 + VALUE_W + 2 + STEPPER_W;
    }

    private static int stepperGroupHeight() {
        return RANGE_Z_ROW_Y - RANGE_X_ROW_Y + STEPPER_H;
    }

    private int stepperGroupX(int bodyX) {
        return bodyX + panelWidth - CONTENT_PAD - STEPPER_RIGHT_INSET - stepperGroupWidth();
    }

    private TrayBounds losTray(int bodyX, int bodyY) {
        return innerButtonTray(bodyX, bodyY, LOS_BUTTON_Y, LOS_BUTTON_H, TRAY_PAD);
    }

    private TrayBounds rangeStepperTrayBounds(int bodyX, int bodyY) {
        return trayBoundsAt(
                stepperGroupX(bodyX),
                bodyY + RANGE_X_ROW_Y,
                stepperGroupWidth(),
                stepperGroupHeight(),
                TRAY_PAD
        );
    }

    /** True if the cursor is over any interactive config control (steppers / edit boxes). */
    public boolean isMouseOverInteractiveWidget(double mouseX, double mouseY) {
        if (!widgetsCreated || !contentsInteractive()) {
            return false;
        }
        return isOverVisible(rangeXMinus, mouseX, mouseY)
                || isOverVisible(rangeXPlus, mouseX, mouseY)
                || isOverVisible(rangeYMinus, mouseX, mouseY)
                || isOverVisible(rangeYPlus, mouseX, mouseY)
                || isOverVisible(rangeZMinus, mouseX, mouseY)
                || isOverVisible(rangeZPlus, mouseX, mouseY)
                || isOverVisible(delayBox, mouseX, mouseY)
                || isOverVisible(batchBox, mouseX, mouseY)
                || isOverVisible(infiniteDelayButton, mouseX, mouseY)
                || (losButton != null && isOverVisible(losButton, mouseX, mouseY));
    }

    private static boolean isOverVisible(AbstractWidget widget, double mouseX, double mouseY) {
        return widget.visible
                && mouseX >= widget.getX()
                && mouseY >= widget.getY()
                && mouseX < widget.getX() + widget.getWidth()
                && mouseY < widget.getY() + widget.getHeight();
    }

    @Override
    public boolean isMouseOverDecorativeArea(double mouseX, double mouseY, int leftPos, int topPos, int imageWidth) {
        if (!contentsInteractive()) {
            return false;
        }
        // Never treat stepper/edit hits as decorative — those must reach the widgets.
        if (isMouseOverInteractiveWidget(mouseX, mouseY)) {
            return false;
        }
        TrayBounds tray = rangeStepperTrayBounds(bodyXOpen(leftPos, imageWidth), bodyY(topPos));
        if (isMouseOverRect(mouseX, mouseY, tray.x(), tray.y(), tray.width(), tray.height())) {
            return true;
        }
        if (losButton == null) {
            return false;
        }
        TrayBounds los = losTray(bodyXOpen(leftPos, imageWidth), bodyY(topPos));
        return isMouseOverRect(mouseX, mouseY, los.x(), los.y(), los.width(), los.height());
    }

    private void adjustRange(byte kind, int delta) {
        ItemCollectorMenu menu = screen.getMenu();
        ItemCollectorType type = menu.collectorType();
        int current = switch (kind) {
            case ItemCollectorSettingPayload.KIND_RANGE_X -> menu.getRangeX();
            case ItemCollectorSettingPayload.KIND_RANGE_Y -> menu.getRangeY();
            case ItemCollectorSettingPayload.KIND_RANGE_Z -> menu.getRangeZ();
            default -> 0;
        };
        int next;
        if (screen.isShiftHeldPublic()) {
            next = delta > 0 ? type.maxRange() : 0;
        } else {
            next = type.clampRange(current + delta);
        }
        if (next == current) {
            return;
        }
        ClientPacketDistributor.sendToServer(new ItemCollectorSettingPayload(kind, next));
    }

    private EditBox axisBox(int width) {
        EditBox box = new EditBox(screen.getFont(), 0, 0, width, 12, Component.empty());
        box.setMaxLength(10);
        box.setCanLoseFocus(true);
        return box;
    }

    private void setInfiniteDelay() {
        pendingDelay = "\u221E";
        ClientPacketDistributor.sendToServer(new ItemCollectorSettingPayload(
                ItemCollectorSettingPayload.KIND_PICKUP_DELAY,
                Integer.MAX_VALUE
        ));
        delayBox.setValue("\u221E");
    }

    private void toggleLos() {
        if (!ItemCollectorConfig.lineOfSightEnabled()) {
            return;
        }
        boolean next = !screen.getMenu().isRequireLineOfSight();
        ClientPacketDistributor.sendToServer(new ItemCollectorSettingPayload(
                ItemCollectorSettingPayload.KIND_REQUIRE_LOS,
                next ? 1 : 0
        ));
        refreshLosButton();
    }

    private void refreshLosButton() {
        if (losButton == null) {
            return;
        }
        boolean configEnabled = ItemCollectorConfig.lineOfSightEnabled();
        boolean enabled = screen.getMenu().isRequireLineOfSight();
        losButton.setMessage(Component.translatable(enabled
                ? "gui.dopasrandomutilities.item_collector.los.enabled"
                : "gui.dopasrandomutilities.item_collector.los.disabled"));
        losButton.active = contentsInteractive() && configEnabled;
        losButton.setTooltip(Tooltip.create(losTooltip(configEnabled)));
    }

    private static Component losTooltip(boolean configEnabled) {
        if (!configEnabled) {
            return Component.translatable("gui.dopasrandomutilities.item_collector.los.tooltip.config_disabled")
                    .withStyle(ChatFormatting.RED);
        }
        return Component.translatable("gui.dopasrandomutilities.item_collector.los.tooltip")
                .append("\n\n")
                .append(Component.translatable("gui.dopasrandomutilities.item_collector.los.tooltip.bugged")
                        .withStyle(ChatFormatting.YELLOW));
    }

    @Override
    public void layoutWidgets(int leftPos, int topPos, int imageWidth) {
        if (!widgetsCreated) {
            return;
        }
        int bx = bodyXOpen(leftPos, imageWidth);
        int by = bodyY(topPos);
        int innerWidth = panelWidth - CONTENT_PAD * 2;
        int groupX = stepperGroupX(bx);
        int valueX = groupX + STEPPER_W + 2;

        layoutStepperRow(by, RANGE_X_ROW_Y, rangeXMinus, rangeXPlus, groupX, valueX);
        layoutStepperRow(by, RANGE_Y_ROW_Y, rangeYMinus, rangeYPlus, groupX, valueX);
        layoutStepperRow(by, RANGE_Z_ROW_Y, rangeZMinus, rangeZPlus, groupX, valueX);

        delayBox.setX(bx + CONTENT_PAD);
        delayBox.setY(by + DELAY_BOX_Y);
        delayBox.setWidth(innerWidth - 24);
        infiniteDelayButton.setX(bx + CONTENT_PAD + innerWidth - 20);
        infiniteDelayButton.setY(by + DELAY_BOX_Y);

        batchBox.setX(bx + CONTENT_PAD);
        batchBox.setY(by + BATCH_BOX_Y);
        batchBox.setWidth(innerWidth);

        if (losButton != null) {
            losButton.setX(bx + CONTENT_PAD);
            losButton.setY(by + LOS_BUTTON_Y);
            losButton.setWidth(innerWidth);
            losButton.setHeight(LOS_BUTTON_H);
        }
    }

    private void layoutStepperRow(int by, int rowY, StepperButton minus, StepperButton plus, int groupX, int valueX) {
        minus.setRectangle(STEPPER_W, STEPPER_H, groupX, by + rowY);
        plus.setRectangle(STEPPER_W, STEPPER_H, valueX + VALUE_W + 2, by + rowY);
    }

    @Override
    protected void updateWidgetVisibility(boolean interactive) {
        if (!widgetsCreated) {
            return;
        }
        rangeXMinus.visible = interactive;
        rangeXPlus.visible = interactive;
        rangeYMinus.visible = interactive;
        rangeYPlus.visible = interactive;
        rangeZMinus.visible = interactive;
        rangeZPlus.visible = interactive;
        delayBox.visible = interactive;
        delayBox.active = interactive;
        batchBox.visible = interactive;
        batchBox.active = interactive;
        infiniteDelayButton.visible = interactive;
        infiniteDelayButton.active = interactive;
        if (losButton != null) {
            losButton.visible = interactive;
            losButton.active = interactive && ItemCollectorConfig.lineOfSightEnabled();
        }
        updateStepperStates(interactive);
        if (!interactive && (delayBox.isFocused() || batchBox.isFocused())) {
            screen.clearFocus();
        }
    }

    private void updateStepperStates(boolean interactive) {
        if (!widgetsCreated) {
            return;
        }
        ItemCollectorMenu menu = screen.getMenu();
        ItemCollectorType type = menu.collectorType();
        int max = type.maxRange();
        rangeXMinus.active = interactive && menu.getRangeX() > 0;
        rangeXPlus.active = interactive && menu.getRangeX() < max;
        rangeYMinus.active = interactive && menu.getRangeY() > 0;
        rangeYPlus.active = interactive && menu.getRangeY() < max;
        rangeZMinus.active = interactive && menu.getRangeZ() > 0;
        rangeZPlus.active = interactive && menu.getRangeZ() < max;
    }

    @Override
    protected void onTick() {
        if (!widgetsCreated || !contentsInteractive()) {
            return;
        }
        trackFocus(delayBox, delayWasFocused, this::commitDelay);
        delayWasFocused = delayBox.isFocused();
        trackFocus(batchBox, batchWasFocused, this::commitBatch);
        batchWasFocused = batchBox.isFocused();
        syncUnfocusedFromMenu();
        updateStepperStates(true);
        refreshLosButton();
    }

    private static void trackFocus(EditBox box, boolean wasFocused, Runnable commit) {
        if (wasFocused && !box.isFocused()) {
            commit.run();
        }
    }

    private void syncUnfocusedFromMenu() {
        syncPendingBox(delayBox, formatDelay(screen.getMenu().getPickupDelay()), pendingDelay, v -> pendingDelay = v);
        syncPendingBox(batchBox, Integer.toString(screen.getMenu().getPickupBatch()), pendingBatch, v -> pendingBatch = v);
    }

    private static void syncPendingBox(
            EditBox box,
            String menuValue,
            @Nullable String pending,
            java.util.function.Consumer<String> setPending
    ) {
        if (box.isFocused()) {
            return;
        }
        if (pending != null) {
            if (menuValue.equals(pending)) {
                setPending.accept(null);
            } else {
                if (!pending.equals(box.getValue())) {
                    box.setValue(pending);
                }
                return;
            }
        }
        if (!menuValue.equals(box.getValue())) {
            box.setValue(menuValue);
        }
    }

    public void onScreenClose() {
        if (!widgetsCreated) {
            return;
        }
        if (delayBox.isFocused()) {
            commitDelay();
        }
        if (batchBox.isFocused()) {
            commitBatch();
        }
    }

    public void clearFocusIfOutside(double mouseX, double mouseY) {
        if (delayBox != null && delayBox.isFocused() && !delayBox.isMouseOver(mouseX, mouseY)) {
            screen.clearFocus();
        } else if (batchBox != null && batchBox.isFocused() && !batchBox.isMouseOver(mouseX, mouseY)) {
            screen.clearFocus();
        }
    }

    private void commitBatch() {
        try {
            int value = Integer.parseInt(batchBox.getValue().trim());
            int clamped = screen.getMenu().collectorType().clampPickupBatch(value);
            pendingBatch = Integer.toString(clamped);
            batchBox.setValue(pendingBatch);
            ClientPacketDistributor.sendToServer(new ItemCollectorSettingPayload(
                    ItemCollectorSettingPayload.KIND_PICKUP_BATCH,
                    clamped
            ));
        } catch (NumberFormatException ignored) {
            pendingBatch = null;
            syncUnfocusedFromMenu();
        }
    }

    private void commitDelay() {
        String text = delayBox.getValue().trim();
        if ("\u221E".equals(text) || "inf".equalsIgnoreCase(text)) {
            pendingDelay = "\u221E";
            delayBox.setValue(pendingDelay);
            ClientPacketDistributor.sendToServer(new ItemCollectorSettingPayload(
                    ItemCollectorSettingPayload.KIND_PICKUP_DELAY,
                    Integer.MAX_VALUE
            ));
            return;
        }
        try {
            int value = Integer.parseInt(text);
            int clamped = screen.getMenu().collectorType().clampPickupDelay(value);
            pendingDelay = formatDelay(clamped);
            delayBox.setValue(pendingDelay);
            ClientPacketDistributor.sendToServer(new ItemCollectorSettingPayload(
                    ItemCollectorSettingPayload.KIND_PICKUP_DELAY,
                    clamped
            ));
        } catch (NumberFormatException ignored) {
            pendingDelay = null;
            syncUnfocusedFromMenu();
        }
    }

    private static String formatDelay(int delay) {
        return delay == Integer.MAX_VALUE ? "\u221E" : Integer.toString(delay);
    }

    @Override
    protected void renderIcon(GuiGraphicsExtractor graphics, Font font, int centerX, int centerY) {
        graphics.item(COMPARATOR_ICON, centerX - 8, centerY - 8, centerX ^ centerY);
    }

    @Override
    protected void renderContents(GuiGraphicsExtractor graphics, Font font, int bodyX, int bodyY,
                                  int mouseX, int mouseY, float partialTick) {
        renderTitleRow(graphics, font, bodyX, bodyY);
        renderTray(graphics, rangeStepperTrayBounds(bodyX, bodyY), BG);
        drawLabel(graphics, font, Component.translatable("gui.dopasrandomutilities.item_collector.range_x"),
                bodyX, bodyX + CONTENT_PAD, bodyY + RANGE_X_ROW_Y + 2);
        drawLabel(graphics, font, Component.translatable("gui.dopasrandomutilities.item_collector.range_y"),
                bodyX, bodyX + CONTENT_PAD, bodyY + RANGE_Y_ROW_Y + 2);
        drawLabel(graphics, font, Component.translatable("gui.dopasrandomutilities.item_collector.range_z"),
                bodyX, bodyX + CONTENT_PAD, bodyY + RANGE_Z_ROW_Y + 2);
        drawRangeValue(graphics, font, bodyX, bodyY + RANGE_X_ROW_Y, screen.getMenu().getRangeX());
        drawRangeValue(graphics, font, bodyX, bodyY + RANGE_Y_ROW_Y, screen.getMenu().getRangeY());
        drawRangeValue(graphics, font, bodyX, bodyY + RANGE_Z_ROW_Y, screen.getMenu().getRangeZ());
        drawLabel(graphics, font, Component.translatable("gui.dopasrandomutilities.item_collector.pickup_delay"),
                bodyX, bodyY + DELAY_LABEL_Y);
        drawLabel(graphics, font, Component.translatable("gui.dopasrandomutilities.item_collector.pickup_batch"),
                bodyX, bodyY + BATCH_LABEL_Y);
        tooltipIfOverLabel(graphics, font, mouseX, mouseY, bodyX, bodyY + DELAY_LABEL_Y,
                "gui.dopasrandomutilities.item_collector.pickup_delay.tooltip");
        tooltipIfOverLabel(graphics, font, mouseX, mouseY, bodyX, bodyY + BATCH_LABEL_Y,
                "gui.dopasrandomutilities.item_collector.pickup_batch.tooltip");
        if (losButton != null) {
            drawLabel(graphics, font, Component.translatable("gui.dopasrandomutilities.item_collector.los"),
                    bodyX, bodyY + LOS_LABEL_Y);
            renderTray(graphics, losTray(bodyX, bodyY), BG);
        }
    }

    private void tooltipIfOverLabel(
            GuiGraphicsExtractor graphics,
            Font font,
            int mouseX,
            int mouseY,
            int bodyX,
            int labelY,
            String tooltipKey
    ) {
        if (isMouseOverRect(mouseX, mouseY, bodyX + CONTENT_PAD, labelY, panelWidth - CONTENT_PAD * 2, font.lineHeight)) {
            graphics.setTooltipForNextFrame(font, Component.translatable(tooltipKey), mouseX, mouseY);
        }
    }

    private void drawRangeValue(GuiGraphicsExtractor graphics, Font font, int bodyX, int rowY, int value) {
        int valueX = stepperGroupX(bodyX) + STEPPER_W + 2;
        String text = Integer.toString(value);
        int textX = valueX + (VALUE_W - font.width(text)) / 2;
        graphics.text(font, text, textX, rowY + 2, 0xFFFFFFFF, false);
    }

    /** Compact +/- control with hitbox matching the drawn sprite exactly. */
    private static final class StepperButton extends AbstractWidget {
        private static final WidgetSprites SPRITES = new WidgetSprites(
                Identifier.withDefaultNamespace("widget/button"),
                Identifier.withDefaultNamespace("widget/button_disabled"),
                Identifier.withDefaultNamespace("widget/button_highlighted")
        );

        private final String label;
        private final Runnable onPress;

        StepperButton(String label, Component tooltip, Runnable onPress) {
            super(0, 0, STEPPER_W, STEPPER_H, Component.literal(label));
            this.label = label;
            this.onPress = onPress;
            setTooltip(Tooltip.create(tooltip));
        }

        @Override
        public void onClick(MouseButtonEvent event, boolean doubleClick) {
            if (active) {
                onPress.run();
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            output.add(NarratedElementType.TITLE, getMessage());
        }

        @Override
        public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            int x = getX();
            int y = getY();
            graphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    SPRITES.get(active, isHoveredOrFocused()),
                    x,
                    y,
                    width,
                    height,
                    ARGB.white(alpha)
            );
            Font font = net.minecraft.client.Minecraft.getInstance().font;
            int textX = x + (width - font.width(label)) / 2;
            int textY = y + (height - font.lineHeight) / 2 + 1;
            int color = active ? 0xFFFFFFFF : 0xFFA0A0A0;
            graphics.text(font, label, textX, textY, color, false);
        }
    }
}
