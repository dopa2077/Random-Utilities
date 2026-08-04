package com.dopa.randomutilities.filteritem.client.panel;

import com.dopa.randomutilities.config.DevNullConfig;
import com.dopa.randomutilities.filteritem.client.FilterScreen;
import com.dopa.randomutilities.filteritem.menu.FilterMenu;
import com.dopa.randomutilities.filteritem.network.FilterSettingPayload;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Advanced /dev/null settings: max stack, slot add/remove, and paging.
 */
public final class ConfiguratorPanel extends AttachedPanel {
    private static final int BG = 0xFF1A4548;
    private static final int BUTTON_SIZE = 20;
    private static final int BUTTON_GAP = 7;
    private static final int TRAY_PAD = 3;
    private static final ItemStack COMPARATOR_ICON = new ItemStack(Items.COMPARATOR);

    private static final int STACK_LABEL_Y = 28;
    private static final int STACK_BOX_Y = 38;
    private static final int SLOTS_LABEL_Y = 55;
    private static final int SLOT_BUTTONS_Y = 66;
    private static final int PAGE_LABEL_Y = 95;
    private static final int PAGE_BUTTONS_Y = 107;

    private final FilterScreen screen;

    private EditBox maxStackBox;
    private Button removeSlotButton;
    private Button addSlotButton;
    private Button prevPageButton;
    private Button nextPageButton;

    private boolean removeConfirmPending;
    private boolean removeConfirmBulk;
    private boolean maxStackBoxWasFocused;
    private boolean widgetsCreated;

    public ConfiguratorPanel(FilterScreen screen) {
        super(
                PanelAnchor.LEFT_BELOW,
                118,
                136,
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
        FilterMenu menu = screen.getMenu();
        int innerWidth = panelWidth - CONTENT_PAD * 2;

        maxStackBox = new EditBox(screen.getFont(), 0, 0, innerWidth, 12,
                Component.translatable("gui.dopasrandomutilities.max_stack"));
        maxStackBox.setMaxLength(10);
        maxStackBox.setValue(Integer.toString(menu.getMaxStackSizeSetting()));
        maxStackBox.setCanLoseFocus(true);
        maxStackBox.setTooltip(Tooltip.create(Component.translatable("gui.dopasrandomutilities.stack_size.tooltip")));
        screen.addOverlayWidget(maxStackBox);

        removeSlotButton = settingsButton(Component.literal("-"), removeSlotTooltip(), this::onRemoveSlotPressed);
        addSlotButton = settingsButton(Component.literal("+"),
                twoLineTooltip("gui.dopasrandomutilities.add_slot.tooltip",
                        "gui.dopasrandomutilities.add_slot.tooltip_shift"),
                this::onAddSlotPressed);
        screen.addOverlayWidget(removeSlotButton);
        screen.addOverlayWidget(addSlotButton);

        prevPageButton = settingsButton(Component.literal("<"),
                Component.translatable("gui.dopasrandomutilities.prev_page.tooltip"),
                () -> screen.sendMenuButton(FilterMenu.BTN_PREV_PAGE));
        nextPageButton = settingsButton(Component.literal(">"),
                Component.translatable("gui.dopasrandomutilities.next_page.tooltip"),
                () -> screen.sendMenuButton(FilterMenu.BTN_NEXT_PAGE));
        screen.addOverlayWidget(prevPageButton);
        screen.addOverlayWidget(nextPageButton);

        removeConfirmPending = false;
        updateWidgetVisibility(false);
        updateButtonStates();
    }

    private Button settingsButton(Component label, Component tooltip, Runnable action) {
        return Button.builder(label, b -> action.run())
                .bounds(0, 0, BUTTON_SIZE, BUTTON_SIZE)
                .tooltip(Tooltip.create(tooltip))
                .build();
    }

    private static int buttonGroupWidth() {
        return BUTTON_SIZE * 2 + BUTTON_GAP;
    }

    private TrayBounds slotTrayBounds(int bodyX, int bodyY) {
        return trayBounds(bodyX, panelWidth, buttonGroupWidth(), bodyY + SLOT_BUTTONS_Y, BUTTON_SIZE, TRAY_PAD);
    }

    private TrayBounds pageTrayBounds(int bodyX, int bodyY) {
        return trayBounds(bodyX, panelWidth, buttonGroupWidth(), bodyY + PAGE_BUTTONS_Y, BUTTON_SIZE, TRAY_PAD);
    }

    @Override
    public boolean isMouseOverDecorativeArea(double mouseX, double mouseY, int leftPos, int topPos, int imageWidth) {
        if (!contentsInteractive()) {
            return false;
        }
        int bodyX = bodyXOpen(leftPos, imageWidth);
        int bodyY = bodyY(topPos);
        TrayBounds slots = slotTrayBounds(bodyX, bodyY);
        if (isMouseOverRect(mouseX, mouseY, slots.x(), slots.y(), slots.width(), slots.height())) {
            return true;
        }
        TrayBounds page = pageTrayBounds(bodyX, bodyY);
        return isMouseOverRect(mouseX, mouseY, page.x(), page.y(), page.width(), page.height());
    }

    @Override
    public void layoutWidgets(int leftPos, int topPos, int imageWidth) {
        if (!widgetsCreated) {
            return;
        }
        int bx = bodyXOpen(leftPos, imageWidth);
        int by = bodyY(topPos);
        int innerWidth = panelWidth - CONTENT_PAD * 2;
        int groupW = buttonGroupWidth();
        int groupX = bx + (panelWidth - groupW) / 2;

        maxStackBox.setX(bx + CONTENT_PAD);
        maxStackBox.setY(by + STACK_BOX_Y);
        maxStackBox.setWidth(innerWidth);

        removeSlotButton.setX(groupX);
        removeSlotButton.setY(by + SLOT_BUTTONS_Y);
        addSlotButton.setX(groupX + BUTTON_SIZE + BUTTON_GAP);
        addSlotButton.setY(by + SLOT_BUTTONS_Y);

        prevPageButton.setX(groupX);
        prevPageButton.setY(by + PAGE_BUTTONS_Y);
        nextPageButton.setX(groupX + BUTTON_SIZE + BUTTON_GAP);
        nextPageButton.setY(by + PAGE_BUTTONS_Y);
    }

    @Override
    protected void updateWidgetVisibility(boolean interactive) {
        if (!widgetsCreated) {
            return;
        }
        maxStackBox.visible = interactive;
        maxStackBox.active = interactive;
        removeSlotButton.visible = interactive;
        addSlotButton.visible = interactive;
        prevPageButton.visible = interactive;
        nextPageButton.visible = interactive;
        if (!interactive && maxStackBox.isFocused()) {
            screen.clearFocus();
        }
        if (interactive) {
            updateButtonStates();
        }
    }

    @Override
    protected void onTick() {
        if (!widgetsCreated || !contentsInteractive()) {
            return;
        }
        boolean focused = maxStackBox.isFocused();
        if (maxStackBoxWasFocused && !focused) {
            commitMaxStackSetting();
        }
        maxStackBoxWasFocused = focused;

        if (removeConfirmPending) {
            if (!screen.getMenu().wouldRemoveVoidItems(removeConfirmBulk)
                    || screen.isShiftHeldPublic() != removeConfirmBulk) {
                removeConfirmPending = false;
            }
        }
        updateButtonStates();
        syncEditBoxesFromMenu();
    }

    public void updateButtonStates() {
        if (!widgetsCreated) {
            return;
        }
        FilterMenu menu = screen.getMenu();
        removeSlotButton.active = contentsInteractive() && menu.canRemoveSlot();
        removeSlotButton.setTooltip(Tooltip.create(removeSlotTooltip()));
        addSlotButton.active = contentsInteractive() && menu.getSlotCountSetting() < DevNullConfig.advancedMaxSlots();
        prevPageButton.active = contentsInteractive() && menu.getPage() > 0;
        nextPageButton.active = contentsInteractive() && menu.getPage() < menu.getPageCount() - 1;
    }

    public int effectiveMaxStackForDisplay() {
        if (maxStackBox != null && maxStackBox.isFocused()) {
            try {
                long parsed = Long.parseLong(maxStackBox.getValue().trim());
                if (parsed >= 1) {
                    return DevNullConfig.clampAdvancedMaxStack(
                            (int) Math.min(DevNullConfig.advancedMaxStackSize(), parsed)
                    );
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return screen.getMenu().getMaxStackSizeSetting();
    }

    public void commitMaxStackSetting() {
        if (maxStackBox == null) {
            return;
        }
        String trimmed = maxStackBox.getValue().trim();
        try {
            long parsed = Long.parseLong(trimmed);
            if (parsed < 1) {
                syncEditBoxesFromMenu();
                return;
            }
            int value = DevNullConfig.clampAdvancedMaxStack(
                    (int) Math.min(DevNullConfig.advancedMaxStackSize(), parsed));
            maxStackBox.setValue(Integer.toString(value));
            if (value != screen.getMenu().getMaxStackSizeSetting()) {
                ClientPacketDistributor.sendToServer(FilterSettingPayload.maxStack(value));
            }
        } catch (NumberFormatException ignored) {
            syncEditBoxesFromMenu();
        }
    }

    public void syncEditBoxesFromMenu() {
        if (maxStackBox != null && !maxStackBox.isFocused()) {
            String expected = Integer.toString(screen.getMenu().getMaxStackSizeSetting());
            if (!expected.equals(maxStackBox.getValue())) {
                maxStackBox.setValue(expected);
            }
        }
    }

    public void clearFocusIfOutside(double mouseX, double mouseY) {
        if (maxStackBox != null && maxStackBox.isFocused() && !maxStackBox.isMouseOver(mouseX, mouseY)) {
            screen.clearFocus();
        }
    }

    public void onScreenClose() {
        if (maxStackBox != null && maxStackBox.isFocused()) {
            commitMaxStackSetting();
        }
    }

    public void clearRemoveConfirm() {
        removeConfirmPending = false;
    }

    private void onRemoveSlotPressed() {
        boolean bulk = screen.isShiftHeldPublic();
        FilterMenu menu = screen.getMenu();
        if (menu.wouldRemoveVoidItems(bulk)) {
            if (removeConfirmPending && removeConfirmBulk == bulk) {
                removeConfirmPending = false;
                screen.clearGatherConfirm();
                screen.sendMenuButton(bulk ? FilterMenu.BTN_REMOVE_ROW : FilterMenu.BTN_REMOVE_SLOT);
            } else {
                removeConfirmPending = true;
                removeConfirmBulk = bulk;
                screen.clearGatherConfirm();
                updateButtonStates();
            }
            return;
        }
        removeConfirmPending = false;
        screen.clearGatherConfirm();
        screen.sendMenuButton(bulk ? FilterMenu.BTN_REMOVE_ROW : FilterMenu.BTN_REMOVE_SLOT);
    }

    private void onAddSlotPressed() {
        removeConfirmPending = false;
        screen.clearGatherConfirm();
        screen.sendSlotButton(FilterMenu.BTN_ADD_SLOT, FilterMenu.BTN_ADD_ROW);
    }

    private static Component twoLineTooltip(String firstKey, String secondKey) {
        return Component.translatable(firstKey)
                .append("\n")
                .append(Component.translatable(secondKey).withStyle(ChatFormatting.GRAY));
    }

    private Component removeSlotTooltip() {
        boolean bulk = screen.isShiftHeldPublic();
        if (screen.getMenu().wouldRemoveVoidItems(bulk)) {
            String warningKey = bulk
                    ? "gui.dopasrandomutilities.remove_slot.tooltip_void_bulk"
                    : "gui.dopasrandomutilities.remove_slot.tooltip_void";
            MutableComponent tooltip = Component.translatable(warningKey).withStyle(ChatFormatting.RED);
            if (removeConfirmPending && removeConfirmBulk == bulk) {
                tooltip.append("\n\n")
                        .append(Component.translatable("gui.dopasrandomutilities.remove_slot.tooltip_void_confirm")
                                .withStyle(ChatFormatting.DARK_RED));
            }
            return tooltip;
        }
        return twoLineTooltip(
                "gui.dopasrandomutilities.remove_slot.tooltip",
                "gui.dopasrandomutilities.remove_slot.tooltip_shift"
        );
    }

    @Override
    protected void renderIcon(GuiGraphicsExtractor graphics, Font font, int centerX, int centerY) {
        graphics.item(COMPARATOR_ICON, centerX - 8, centerY - 8, centerX ^ centerY);
    }

    @Override
    protected void renderContents(GuiGraphicsExtractor graphics, Font font, int bodyX, int bodyY,
                                  int mouseX, int mouseY, float partialTick) {
        FilterMenu menu = screen.getMenu();
        renderTitleRow(graphics, font, bodyX, bodyY);
        drawLabel(graphics, font, Component.translatable("gui.dopasrandomutilities.stack_size"),
                bodyX, bodyY + STACK_LABEL_Y);

        Component slotsPrefix = Component.translatable("gui.dopasrandomutilities.slots_prefix");
        Component slotsValue = Component.literal(Integer.toString(menu.getSlotCountSetting()));
        int slotsLabelWidth = font.width(slotsPrefix) + font.width(slotsValue);
        int slotsLabelX = bodyX + (panelWidth - slotsLabelWidth) / 2;
        int sy = bodyY + SLOTS_LABEL_Y;
        drawLabel(graphics, font, slotsPrefix, bodyX, slotsLabelX, sy);
        drawValue(graphics, font, slotsValue, bodyX, slotsLabelX + font.width(slotsPrefix), sy);
        renderTray(graphics, slotTrayBounds(bodyX, bodyY), BG);

        Component pageLabel = Component.translatable("gui.dopasrandomutilities.page_label");
        Component currentPage = Component.literal(Integer.toString(menu.getPage() + 1));
        Component pageSeparator = Component.literal(" / ");
        Component pageCount = Component.literal(Integer.toString(menu.getPageCount()));
        int pageRowWidth = font.width(pageLabel) + 4 + font.width(currentPage)
                + font.width(pageSeparator) + font.width(pageCount);
        int pageLabelX = bodyX + (panelWidth - pageRowWidth) / 2;
        int py = bodyY + PAGE_LABEL_Y;
        int px = pageLabelX;
        drawLabel(graphics, font, pageLabel, bodyX, px, py);
        px += font.width(pageLabel) + 4;
        drawValue(graphics, font, currentPage, bodyX, px, py);
        px += font.width(currentPage);
        // Draw separator directly — drawLabel/wrapText splits on spaces and drops the leading one.
        graphics.text(font, pageSeparator, px, py, LABEL_COLOR, true);
        px += font.width(pageSeparator);
        drawValue(graphics, font, pageCount, bodyX, px, py);
        renderTray(graphics, pageTrayBounds(bodyX, bodyY), BG);
    }
}
