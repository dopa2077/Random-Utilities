package com.dopa.randomutilities.item.magnet.client.panel;

import com.dopa.randomutilities.core.gui.panel.AttachedPanel;
import com.dopa.randomutilities.core.gui.panel.PanelAnchor;
import com.dopa.randomutilities.core.gui.widget.StepperButton;
import com.dopa.randomutilities.item.magnet.client.MagnetScreen;
import com.dopa.randomutilities.item.magnet.menu.MagnetMenu;
import com.dopa.randomutilities.item.magnet.network.MagnetSettingPayload;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public final class MagnetConfigPanel extends AttachedPanel {
    private static final int BG = 0xFF1A4548;
    private static final ItemStack COMPARATOR_ICON = new ItemStack(Items.COMPARATOR);
    private static final int TRAY_PAD = 3;
    private static final int PANEL_W = 152;
    private static final int LABEL_INSET = 4;
    private static final int STEPPER_W = 20;
    private static final int STEPPER_H = 18;
    private static final int VALUE_W = 34;
    private static final int BUTTON_H = 18;
    private static final int ROW_PITCH = 26;
    private static final int RANGE_ROW_Y = 30;
    private static final int RANGE_BLOCK_GAP = 10;
    private static final int MODE_ROW_Y = RANGE_ROW_Y + STEPPER_H + RANGE_BLOCK_GAP;
    private static final int XP_ROW_Y = MODE_ROW_Y + ROW_PITCH;
    private static final int DELAY_ROW_Y = XP_ROW_Y + ROW_PITCH;
    private static final int SNEAK_ROW_Y = DELAY_ROW_Y + ROW_PITCH;
    private static final int CONTROL_W = 78;
    private static final int PANEL_H = SNEAK_ROW_Y + BUTTON_H + CONTENT_PAD;

    private final MagnetScreen screen;
    private StepperButton rangeMinus;
    private StepperButton rangePlus;
    private Button modeButton;
    private Button xpButton;
    private Button delayButton;
    private Button sneakButton;
    private boolean widgetsCreated;
    private boolean lastCollect;
    private boolean lastPullXp;
    private boolean lastIgnoreDelay;
    private boolean lastPauseSneak;
    private boolean toggleStatesInitialized;
    private int lastRange = -1;
    private int lastMaxRange = -1;

    public MagnetConfigPanel(MagnetScreen screen) {
        super(
                PanelAnchor.LEFT_BELOW,
                PANEL_W,
                PANEL_H,
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
        rangeMinus = new StepperButton(STEPPER_W, STEPPER_H, "-", rangeStepperTooltip(true), () -> adjustRange(-1));
        rangePlus = new StepperButton(STEPPER_W, STEPPER_H, "+", rangeStepperTooltip(false), () -> adjustRange(1));
        screen.addOverlayWidget(rangeMinus);
        screen.addOverlayWidget(rangePlus);

        MagnetMenu menu = screen.getMenu();
        modeButton = toggleButton(() -> sendFlag(MagnetSettingPayload.KIND_COLLECT, !menu.isCollectMode()));
        xpButton = toggleButton(() -> sendFlag(MagnetSettingPayload.KIND_PULL_XP, !menu.isPullXp()));
        delayButton = toggleButton(() -> sendFlag(MagnetSettingPayload.KIND_IGNORE_DELAY, !menu.isIgnorePickupDelay()));
        sneakButton = toggleButton(() -> sendFlag(MagnetSettingPayload.KIND_PAUSE_SNEAK, !menu.isPauseOnSneak()));
        screen.addOverlayWidget(modeButton);
        screen.addOverlayWidget(xpButton);
        screen.addOverlayWidget(delayButton);
        screen.addOverlayWidget(sneakButton);
        initButtonTooltips();
        syncToggleMessages();
        updateWidgetVisibility(false);
    }

    private static Component rangeStepperTooltip(boolean decrease) {
        return Component.translatable(decrease
                        ? "gui.dopasrandomutilities.item_magnet.range_decrease"
                        : "gui.dopasrandomutilities.item_magnet.range_increase")
                .append("\n")
                .append(Component.translatable(decrease
                                ? "gui.dopasrandomutilities.item_magnet.range_shift_min"
                                : "gui.dopasrandomutilities.item_magnet.range_shift_max")
                        .withStyle(ChatFormatting.GRAY));
    }

    private void adjustRange(int delta) {
        MagnetMenu menu = screen.getMenu();
        int max = menu.maxRange();
        int current = menu.getRange();
        int next;
        if (screen.isShiftHeldPublic()) {
            next = delta > 0 ? max : 0;
        } else {
            next = Math.max(0, Math.min(max, current + delta));
        }
        if (next == current) {
            return;
        }
        ClientPacketDistributor.sendToServer(new MagnetSettingPayload(MagnetSettingPayload.KIND_RANGE, next));
    }

    private Button toggleButton(Runnable onPress) {
        return Button.builder(Component.empty(), b -> onPress.run())
                .bounds(0, 0, CONTROL_W, BUTTON_H)
                .build();
    }

    public boolean isMouseOverInteractiveWidget(double mouseX, double mouseY) {
        if (!widgetsCreated || !contentsInteractive()) {
            return false;
        }
        return isOverVisible(rangeMinus, mouseX, mouseY)
                || isOverVisible(rangePlus, mouseX, mouseY)
                || isOverVisible(modeButton, mouseX, mouseY)
                || isOverVisible(xpButton, mouseX, mouseY)
                || isOverVisible(delayButton, mouseX, mouseY)
                || isOverVisible(sneakButton, mouseX, mouseY);
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
        int bx = bodyXOpen(leftPos, imageWidth);
        int by = bodyY(topPos);
        TrayBounds rangeTray = rangeStepperTray(bx, by);
        TrayBounds behaviorTray = behaviorButtonTray(bx, by);
        return isMouseOverRect(mouseX, mouseY, rangeTray.x(), rangeTray.y(), rangeTray.width(), rangeTray.height())
                || isMouseOverRect(mouseX, mouseY, behaviorTray.x(), behaviorTray.y(), behaviorTray.width(), behaviorTray.height());
    }

    private static void sendFlag(byte kind, boolean value) {
        ClientPacketDistributor.sendToServer(new MagnetSettingPayload(kind, value ? 1 : 0));
    }

    private int controlX(int bodyX) {
        return bodyX + panelWidth - CONTENT_PAD - CONTROL_W;
    }

    private TrayBounds rangeStepperTray(int bodyX, int bodyY) {
        return trayBoundsAt(controlX(bodyX), bodyY + RANGE_ROW_Y, CONTROL_W, STEPPER_H, TRAY_PAD);
    }

    private TrayBounds behaviorButtonTray(int bodyX, int bodyY) {
        return trayBoundsAt(
                controlX(bodyX),
                bodyY + MODE_ROW_Y,
                CONTROL_W,
                SNEAK_ROW_Y - MODE_ROW_Y + BUTTON_H,
                TRAY_PAD
        );
    }

    @Override
    public void layoutWidgets(int leftPos, int topPos, int imageWidth) {
        if (!widgetsCreated) {
            return;
        }
        int bx = bodyXOpen(leftPos, imageWidth);
        int by = bodyY(topPos);
        int cx = controlX(bx);
        int valueX = cx + STEPPER_W + 2;
        rangeMinus.setRectangle(STEPPER_W, STEPPER_H, cx, by + RANGE_ROW_Y);
        rangePlus.setRectangle(STEPPER_W, STEPPER_H, valueX + VALUE_W + 2, by + RANGE_ROW_Y);

        layoutToggle(modeButton, bx, by + MODE_ROW_Y);
        layoutToggle(xpButton, bx, by + XP_ROW_Y);
        layoutToggle(delayButton, bx, by + DELAY_ROW_Y);
        layoutToggle(sneakButton, bx, by + SNEAK_ROW_Y);
    }

    private void layoutToggle(Button button, int bodyX, int y) {
        button.setRectangle(CONTROL_W, BUTTON_H, controlX(bodyX), y);
    }

    @Override
    protected void updateWidgetVisibility(boolean interactive) {
        if (!widgetsCreated) {
            return;
        }
        rangeMinus.visible = interactive;
        rangePlus.visible = interactive;
        modeButton.visible = interactive;
        xpButton.visible = interactive;
        delayButton.visible = interactive;
        sneakButton.visible = interactive;
        modeButton.active = interactive;
        xpButton.active = interactive;
        delayButton.active = interactive;
        sneakButton.active = interactive;
        updateRangeStepperStates(interactive);
    }

    private void updateRangeStepperStates(boolean interactive) {
        if (!widgetsCreated) {
            return;
        }
        if (!interactive) {
            rangeMinus.active = false;
            rangePlus.active = false;
            lastRange = -1;
            lastMaxRange = -1;
            return;
        }
        MagnetMenu menu = screen.getMenu();
        int range = menu.getRange();
        int max = menu.maxRange();
        if (range == lastRange && max == lastMaxRange) {
            return;
        }
        lastRange = range;
        lastMaxRange = max;
        rangeMinus.active = range > 0;
        rangePlus.active = range < max;
    }

    @Override
    protected void onTick() {
        if (widgetsCreated && contentsInteractive()) {
            syncToggleMessages();
            updateRangeStepperStates(true);
        }
    }

    private void initButtonTooltips() {
        modeButton.setTooltip(Tooltip.create(Component.translatable(
                "gui.dopasrandomutilities.item_magnet.mode.tooltip")));
        xpButton.setTooltip(Tooltip.create(Component.translatable(
                "gui.dopasrandomutilities.item_magnet.pull_xp.tooltip")));
        delayButton.setTooltip(Tooltip.create(Component.translatable(
                "gui.dopasrandomutilities.item_magnet.ignore_delay.tooltip")));
        sneakButton.setTooltip(Tooltip.create(Component.translatable(
                "gui.dopasrandomutilities.item_magnet.pause_sneak.tooltip")));
    }

    private void syncToggleMessages() {
        MagnetMenu menu = screen.getMenu();
        boolean collect = menu.isCollectMode();
        if (!toggleStatesInitialized || collect != lastCollect) {
            lastCollect = collect;
            modeButton.setMessage(Component.translatable(collect
                    ? "gui.dopasrandomutilities.item_magnet.mode.collect"
                    : "gui.dopasrandomutilities.item_magnet.mode.attract"));
        }
        boolean pullXp = menu.isPullXp();
        if (!toggleStatesInitialized || pullXp != lastPullXp) {
            lastPullXp = pullXp;
            xpButton.setMessage(onOff(pullXp));
        }
        boolean ignoreDelay = menu.isIgnorePickupDelay();
        if (!toggleStatesInitialized || ignoreDelay != lastIgnoreDelay) {
            lastIgnoreDelay = ignoreDelay;
            delayButton.setMessage(onOff(ignoreDelay));
        }
        boolean pauseSneak = menu.isPauseOnSneak();
        if (!toggleStatesInitialized || pauseSneak != lastPauseSneak) {
            lastPauseSneak = pauseSneak;
            sneakButton.setMessage(onOff(pauseSneak));
        }
        toggleStatesInitialized = true;
    }

    private static Component onOff(boolean enabled) {
        return Component.translatable(enabled
                ? "gui.dopasrandomutilities.item_magnet.toggle.on"
                : "gui.dopasrandomutilities.item_magnet.toggle.off");
    }

    @Override
    protected void renderIcon(GuiGraphicsExtractor graphics, Font font, int centerX, int centerY) {
        graphics.item(COMPARATOR_ICON, centerX - 8, centerY - 8, centerX ^ centerY);
    }

    @Override
    protected void renderContents(GuiGraphicsExtractor graphics, Font font, int bodyX, int bodyY,
                                  int mouseX, int mouseY, float partialTick) {
        renderTitleRow(graphics, font, bodyX, bodyY);
        renderTray(graphics, rangeStepperTray(bodyX, bodyY), BG);
        renderTray(graphics, behaviorButtonTray(bodyX, bodyY), BG);
        drawRowLabel(graphics, font, Component.translatable("gui.dopasrandomutilities.item_magnet.range"),
                bodyX, bodyY + RANGE_ROW_Y, STEPPER_H);
        drawRowLabel(graphics, font, Component.translatable("gui.dopasrandomutilities.item_magnet.mode"),
                bodyX, bodyY + MODE_ROW_Y, BUTTON_H);
        drawRowLabel(graphics, font, Component.translatable("gui.dopasrandomutilities.item_magnet.pull_xp"),
                bodyX, bodyY + XP_ROW_Y, BUTTON_H);
        drawRowLabel(graphics, font, Component.translatable("gui.dopasrandomutilities.item_magnet.ignore_delay"),
                bodyX, bodyY + DELAY_ROW_Y, BUTTON_H);
        drawRowLabel(graphics, font, Component.translatable("gui.dopasrandomutilities.item_magnet.pause_sneak"),
                bodyX, bodyY + SNEAK_ROW_Y, BUTTON_H);
        drawRangeValue(graphics, font, bodyX, bodyY + RANGE_ROW_Y);
    }

    private void drawRowLabel(GuiGraphicsExtractor graphics, Font font, Component text, int bodyX, int rowY, int rowH) {
        int x = bodyX + LABEL_INSET;
        int textY = rowY + Math.max(0, (rowH - font.lineHeight) / 2);
        graphics.text(font, text, x, textY, LABEL_COLOR, false);
    }

    private void drawRangeValue(GuiGraphicsExtractor graphics, Font font, int bodyX, int rowY) {
        int valueX = controlX(bodyX) + STEPPER_W + 2;
        String text = Integer.toString(screen.getMenu().getRange());
        int textX = valueX + (VALUE_W - font.width(text)) / 2;
        int textY = rowY + (STEPPER_H - font.lineHeight) / 2 + 1;
        graphics.text(font, text, textX, textY, 0xFFFFFFFF, false);
    }
}
