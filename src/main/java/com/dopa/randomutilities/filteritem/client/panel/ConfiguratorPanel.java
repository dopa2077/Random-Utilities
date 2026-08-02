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
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Advanced /dev/null settings: max stack, slot add/remove, paging, and colour.
 */
public final class ConfiguratorPanel extends AttachedPanel {
    private static final int BG = 0xFF1A4548;
    private static final int LABEL_COLOR = 0xFFD0D0D0;
    private static final int MUTED_LABEL_COLOR = 0xFFA8A8A8;
    private static final int BUTTON_SIZE = 18;
    private static final int CHANGE_COLOUR_BUTTON_H = 20;

    private static final int STACK_LABEL_Y = 20;
    private static final int STACK_BOX_Y = 30;
    private static final int SLOTS_LABEL_Y = 51;
    private static final int SLOT_BUTTONS_Y = 60;
    private static final int PAGE_LABEL_Y = 86;
    private static final int PAGE_BUTTONS_Y = 96;
    private static final int CHANGE_COLOUR_BUTTON_Y = 118;

    private final FilterScreen screen;

    private EditBox maxStackBox;
    private Button changeColourButton;
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
                106,
                145,
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

        changeColourButton = Button.builder(
                Component.translatable("gui.dopasrandomutilities.change_color"),
                button -> screen.openColorPicker()
        ).bounds(0, 0, innerWidth, CHANGE_COLOUR_BUTTON_H)
                .tooltip(Tooltip.create(Component.translatable("gui.dopasrandomutilities.change_color.tooltip")))
                .build();
        screen.addOverlayWidget(changeColourButton);

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

    @Override
    public void layoutWidgets(int leftPos, int topPos, int imageWidth) {
        if (!widgetsCreated) {
            return;
        }
        int bx = bodyXOpen(leftPos, imageWidth);
        int by = bodyY(topPos);
        int innerWidth = panelWidth - CONTENT_PAD * 2;

        maxStackBox.setX(bx + CONTENT_PAD);
        maxStackBox.setY(by + STACK_BOX_Y);
        maxStackBox.setWidth(innerWidth);

        removeSlotButton.setX(bx + CONTENT_PAD);
        removeSlotButton.setY(by + SLOT_BUTTONS_Y);
        addSlotButton.setX(bx + CONTENT_PAD + BUTTON_SIZE + 4);
        addSlotButton.setY(by + SLOT_BUTTONS_Y);

        prevPageButton.setX(bx + CONTENT_PAD);
        prevPageButton.setY(by + PAGE_BUTTONS_Y);
        nextPageButton.setX(bx + CONTENT_PAD + BUTTON_SIZE + 4);
        nextPageButton.setY(by + PAGE_BUTTONS_Y);

        changeColourButton.setX(bx + CONTENT_PAD);
        changeColourButton.setY(by + CHANGE_COLOUR_BUTTON_Y);
        changeColourButton.setWidth(innerWidth);
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
        changeColourButton.visible = interactive;
        changeColourButton.active = interactive;
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
        changeColourButton.active = contentsInteractive();
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
        int c = 0xFF7EC8C8;
        // Gear-ish: center hub + teeth
        graphics.fill(centerX - 2, centerY - 2, centerX + 3, centerY + 3, c);
        graphics.fill(centerX - 1, centerY - 6, centerX + 2, centerY - 3, c);
        graphics.fill(centerX - 1, centerY + 4, centerX + 2, centerY + 7, c);
        graphics.fill(centerX - 6, centerY - 1, centerX - 3, centerY + 2, c);
        graphics.fill(centerX + 4, centerY - 1, centerX + 7, centerY + 2, c);
        graphics.fill(centerX - 5, centerY - 5, centerX - 2, centerY - 2, 0xFF5AA0A0);
        graphics.fill(centerX + 3, centerY - 5, centerX + 6, centerY - 2, 0xFF5AA0A0);
        graphics.fill(centerX - 5, centerY + 3, centerX - 2, centerY + 6, 0xFF5AA0A0);
        graphics.fill(centerX + 3, centerY + 3, centerX + 6, centerY + 6, 0xFF5AA0A0);
    }

    @Override
    protected void renderContents(GuiGraphicsExtractor graphics, Font font, int bodyX, int bodyY,
                                  int mouseX, int mouseY, float partialTick) {
        FilterMenu menu = screen.getMenu();
        graphics.text(font, title, bodyX + CONTENT_PAD, bodyY + 6, 0xFFE0F0F0, false);
        graphics.text(font, Component.translatable("gui.dopasrandomutilities.stack_size"),
                bodyX + CONTENT_PAD, bodyY + STACK_LABEL_Y, LABEL_COLOR, false);

        Component prefix = Component.translatable("gui.dopasrandomutilities.slots_prefix");
        int sx = bodyX + CONTENT_PAD;
        int sy = bodyY + SLOTS_LABEL_Y;
        graphics.text(font, prefix, sx, sy, LABEL_COLOR, false);
        graphics.text(font, Component.literal(Integer.toString(menu.getSlotCountSetting())),
                sx + font.width(prefix), sy, MUTED_LABEL_COLOR, false);

        graphics.text(font, Component.translatable("gui.dopasrandomutilities.page",
                        menu.getPage() + 1, menu.getPageCount()),
                bodyX + CONTENT_PAD, bodyY + PAGE_LABEL_Y, LABEL_COLOR, false);
    }
}
