package com.dopa.randomutilities.gui.machine;

import com.dopa.randomutilities.gui.panel.AttachedPanel;
import com.dopa.randomutilities.gui.panel.PanelAnchor;
import com.dopa.randomutilities.util.WorkingVolume;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
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

public final class WorkingVolumeConfigPanel extends AttachedPanel {
    private static final int BG = 0xFF1A4548;
    private static final ItemStack COMPARATOR_ICON = new ItemStack(Items.COMPARATOR);
    private static final int STEPPER_W = 18;
    private static final int STEPPER_H = 12;
    private static final int VALUE_W = 24;
    private static final int TRAY_PAD = 3;
    private static final int PANEL_W = 136;
    private static final int STEPPER_RIGHT_INSET = TRAY_PAD + 2;

    private static final int RANGE_X_ROW_Y = 30;
    private static final int RANGE_Y_ROW_Y = 46;
    private static final int RANGE_Z_ROW_Y = 62;
    private static final int OFFSET_X_ROW_Y = 90;
    private static final int OFFSET_Y_ROW_Y = 106;
    private static final int OFFSET_Z_ROW_Y = 122;

    private final VolumeMachineGui gui;
    private StepperButton rangeXMinus;
    private StepperButton rangeXPlus;
    private StepperButton rangeYMinus;
    private StepperButton rangeYPlus;
    private StepperButton rangeZMinus;
    private StepperButton rangeZPlus;
    private StepperButton offsetXMinus;
    private StepperButton offsetXPlus;
    private StepperButton offsetYMinus;
    private StepperButton offsetYPlus;
    private StepperButton offsetZMinus;
    private StepperButton offsetZPlus;
    private boolean widgetsCreated;

    public WorkingVolumeConfigPanel(VolumeMachineGui gui) {
        super(
                PanelAnchor.LEFT_BELOW,
                PANEL_W,
                146,
                BG,
                Component.translatable("gui.dopasrandomutilities.panel.config")
        );
        this.gui = gui;
    }

    public void initWidgets() {
        if (widgetsCreated) {
            return;
        }
        widgetsCreated = true;
        rangeXMinus = stepperButton("-", WorkingVolume.KIND_RANGE_X, -1);
        rangeXPlus = stepperButton("+", WorkingVolume.KIND_RANGE_X, 1);
        rangeYMinus = stepperButton("-", WorkingVolume.KIND_RANGE_Y, -1);
        rangeYPlus = stepperButton("+", WorkingVolume.KIND_RANGE_Y, 1);
        rangeZMinus = stepperButton("-", WorkingVolume.KIND_RANGE_Z, -1);
        rangeZPlus = stepperButton("+", WorkingVolume.KIND_RANGE_Z, 1);
        offsetXMinus = stepperButton("-", WorkingVolume.KIND_OFFSET_X, -1);
        offsetXPlus = stepperButton("+", WorkingVolume.KIND_OFFSET_X, 1);
        offsetYMinus = stepperButton("-", WorkingVolume.KIND_OFFSET_Y, -1);
        offsetYPlus = stepperButton("+", WorkingVolume.KIND_OFFSET_Y, 1);
        offsetZMinus = stepperButton("-", WorkingVolume.KIND_OFFSET_Z, -1);
        offsetZPlus = stepperButton("+", WorkingVolume.KIND_OFFSET_Z, 1);
        gui.addOverlayWidget(rangeXMinus);
        gui.addOverlayWidget(rangeXPlus);
        gui.addOverlayWidget(rangeYMinus);
        gui.addOverlayWidget(rangeYPlus);
        gui.addOverlayWidget(rangeZMinus);
        gui.addOverlayWidget(rangeZPlus);
        gui.addOverlayWidget(offsetXMinus);
        gui.addOverlayWidget(offsetXPlus);
        gui.addOverlayWidget(offsetYMinus);
        gui.addOverlayWidget(offsetYPlus);
        gui.addOverlayWidget(offsetZMinus);
        gui.addOverlayWidget(offsetZPlus);
        updateWidgetVisibility(false);
    }

    private StepperButton stepperButton(String label, byte kind, int delta) {
        return new StepperButton(label, stepperTooltip(delta < 0), () -> adjust(kind, delta));
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

    private static int rangeStepperGroupHeight() {
        return RANGE_Z_ROW_Y - RANGE_X_ROW_Y + STEPPER_H;
    }

    private static int offsetStepperGroupHeight() {
        return OFFSET_Z_ROW_Y - OFFSET_X_ROW_Y + STEPPER_H;
    }

    private int stepperGroupX(int bodyX) {
        return bodyX + panelWidth - CONTENT_PAD - STEPPER_RIGHT_INSET - stepperGroupWidth();
    }

    private TrayBounds rangeStepperTrayBounds(int bodyX, int bodyY) {
        return trayBoundsAt(
                stepperGroupX(bodyX),
                bodyY + RANGE_X_ROW_Y,
                stepperGroupWidth(),
                rangeStepperGroupHeight(),
                TRAY_PAD
        );
    }

    private TrayBounds offsetStepperTrayBounds(int bodyX, int bodyY) {
        return trayBoundsAt(
                stepperGroupX(bodyX),
                bodyY + OFFSET_X_ROW_Y,
                stepperGroupWidth(),
                offsetStepperGroupHeight(),
                TRAY_PAD
        );
    }

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
                || isOverVisible(offsetXMinus, mouseX, mouseY)
                || isOverVisible(offsetXPlus, mouseX, mouseY)
                || isOverVisible(offsetYMinus, mouseX, mouseY)
                || isOverVisible(offsetYPlus, mouseX, mouseY)
                || isOverVisible(offsetZMinus, mouseX, mouseY)
                || isOverVisible(offsetZPlus, mouseX, mouseY);
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
        if (isMouseOverInteractiveWidget(mouseX, mouseY)) {
            return false;
        }
        TrayBounds rangeTray = rangeStepperTrayBounds(bodyXOpen(leftPos, imageWidth), bodyY(topPos));
        TrayBounds offsetTray = offsetStepperTrayBounds(bodyXOpen(leftPos, imageWidth), bodyY(topPos));
        return isMouseOverRect(mouseX, mouseY, rangeTray.x(), rangeTray.y(), rangeTray.width(), rangeTray.height())
                || isMouseOverRect(mouseX, mouseY, offsetTray.x(), offsetTray.y(), offsetTray.width(), offsetTray.height());
    }

    private void adjust(byte kind, int delta) {
        int current = gui.volumeValue(kind);
        boolean offset = kind >= WorkingVolume.KIND_OFFSET_X && kind <= WorkingVolume.KIND_OFFSET_Z;
        int cap = offset ? gui.maxOffset() : gui.maxRange();
        int min = offset ? -cap : 0;
        int max = cap;
        int next;
        if (gui.isShiftHeld()) {
            next = offset ? shiftOffset(current, delta, min, max) : (delta > 0 ? max : min);
        } else {
            next = offset
                    ? WorkingVolume.clampOffset(current + delta, cap)
                    : WorkingVolume.clampRange(current + delta, cap);
        }
        if (next == current) {
            return;
        }
        gui.sendVolumeSetting(kind, next);
    }

    /** Offset shift-click: min ↔ 0 ↔ max, instead of jumping straight between the extremes. */
    private static int shiftOffset(int current, int delta, int min, int max) {
        if (delta > 0) {
            return current < 0 ? 0 : max;
        }
        return current > 0 ? 0 : min;
    }

    @Override
    public void layoutWidgets(int leftPos, int topPos, int imageWidth) {
        if (!widgetsCreated) {
            return;
        }
        int bx = bodyXOpen(leftPos, imageWidth);
        int by = bodyY(topPos);
        int groupX = stepperGroupX(bx);
        int valueX = groupX + STEPPER_W + 2;
        layoutStepperRow(by, RANGE_X_ROW_Y, rangeXMinus, rangeXPlus, groupX, valueX);
        layoutStepperRow(by, RANGE_Y_ROW_Y, rangeYMinus, rangeYPlus, groupX, valueX);
        layoutStepperRow(by, RANGE_Z_ROW_Y, rangeZMinus, rangeZPlus, groupX, valueX);
        layoutStepperRow(by, OFFSET_X_ROW_Y, offsetXMinus, offsetXPlus, groupX, valueX);
        layoutStepperRow(by, OFFSET_Y_ROW_Y, offsetYMinus, offsetYPlus, groupX, valueX);
        layoutStepperRow(by, OFFSET_Z_ROW_Y, offsetZMinus, offsetZPlus, groupX, valueX);
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
        setRowVisible(rangeXMinus, rangeXPlus, interactive);
        setRowVisible(rangeYMinus, rangeYPlus, interactive);
        setRowVisible(rangeZMinus, rangeZPlus, interactive);
        setRowVisible(offsetXMinus, offsetXPlus, interactive);
        setRowVisible(offsetYMinus, offsetYPlus, interactive);
        setRowVisible(offsetZMinus, offsetZPlus, interactive);
        updateStepperStates(interactive);
    }

    private static void setRowVisible(StepperButton minus, StepperButton plus, boolean interactive) {
        minus.visible = interactive;
        plus.visible = interactive;
    }

    private void updateStepperStates(boolean interactive) {
        if (!widgetsCreated) {
            return;
        }
        updateRangeRow(rangeXMinus, rangeXPlus, gui.rangeX(), interactive);
        updateRangeRow(rangeYMinus, rangeYPlus, gui.rangeY(), interactive);
        updateRangeRow(rangeZMinus, rangeZPlus, gui.rangeZ(), interactive);
        updateOffsetRow(offsetXMinus, offsetXPlus, gui.offsetX(), interactive);
        updateOffsetRow(offsetYMinus, offsetYPlus, gui.offsetY(), interactive);
        updateOffsetRow(offsetZMinus, offsetZPlus, gui.offsetZ(), interactive);
    }

    private void updateRangeRow(StepperButton minus, StepperButton plus, int value, boolean interactive) {
        minus.active = interactive && value > 0;
        plus.active = interactive && value < gui.maxRange();
    }

    private void updateOffsetRow(StepperButton minus, StepperButton plus, int value, boolean interactive) {
        int cap = gui.maxOffset();
        minus.active = interactive && value > -cap;
        plus.active = interactive && value < cap;
    }

    @Override
    protected void onTick() {
        if (!widgetsCreated || !contentsInteractive()) {
            return;
        }
        updateStepperStates(true);
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
        renderTray(graphics, offsetStepperTrayBounds(bodyX, bodyY), BG);
        drawLabel(graphics, font, Component.translatable("gui.dopasrandomutilities.item_collector.range_x"),
                bodyX, bodyX + CONTENT_PAD, bodyY + RANGE_X_ROW_Y + 2);
        drawLabel(graphics, font, Component.translatable("gui.dopasrandomutilities.item_collector.range_y"),
                bodyX, bodyX + CONTENT_PAD, bodyY + RANGE_Y_ROW_Y + 2);
        drawLabel(graphics, font, Component.translatable("gui.dopasrandomutilities.item_collector.range_z"),
                bodyX, bodyX + CONTENT_PAD, bodyY + RANGE_Z_ROW_Y + 2);
        drawLabel(graphics, font, Component.translatable("gui.dopasrandomutilities.working_volume.offset_x"),
                bodyX, bodyX + CONTENT_PAD, bodyY + OFFSET_X_ROW_Y + 2);
        drawLabel(graphics, font, Component.translatable("gui.dopasrandomutilities.working_volume.offset_y"),
                bodyX, bodyX + CONTENT_PAD, bodyY + OFFSET_Y_ROW_Y + 2);
        drawLabel(graphics, font, Component.translatable("gui.dopasrandomutilities.working_volume.offset_z"),
                bodyX, bodyX + CONTENT_PAD, bodyY + OFFSET_Z_ROW_Y + 2);
        drawValue(graphics, font, bodyX, bodyY + RANGE_X_ROW_Y, gui.rangeX());
        drawValue(graphics, font, bodyX, bodyY + RANGE_Y_ROW_Y, gui.rangeY());
        drawValue(graphics, font, bodyX, bodyY + RANGE_Z_ROW_Y, gui.rangeZ());
        drawValue(graphics, font, bodyX, bodyY + OFFSET_X_ROW_Y, gui.offsetX());
        drawValue(graphics, font, bodyX, bodyY + OFFSET_Y_ROW_Y, gui.offsetY());
        drawValue(graphics, font, bodyX, bodyY + OFFSET_Z_ROW_Y, gui.offsetZ());
    }

    private void drawValue(GuiGraphicsExtractor graphics, Font font, int bodyX, int rowY, int value) {
        int valueX = stepperGroupX(bodyX) + STEPPER_W + 2;
        String text = Integer.toString(value);
        int textX = valueX + (VALUE_W - font.width(text)) / 2;
        graphics.text(font, text, textX, rowY + 2, 0xFFFFFFFF, false);
    }

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
