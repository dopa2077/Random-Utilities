package com.dopa.randomutilities.filteritem.client;

import com.dopa.randomutilities.filteritem.FilterContents;
import com.dopa.randomutilities.filteritem.client.panel.ConfiguratorPanel;
import com.dopa.randomutilities.filteritem.client.panel.EnergyPanel;
import com.dopa.randomutilities.filteritem.client.panel.InformativePanel;
import com.dopa.randomutilities.filteritem.client.panel.PanelHost;
import com.dopa.randomutilities.filteritem.client.panel.UpgradePanel;
import com.dopa.randomutilities.filteritem.menu.FilterMenu;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class FilterScreen extends AbstractContainerScreen<FilterMenu> implements ColorPickerHost {
    private static final Identifier CHEST_BACKGROUND =
            Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");
    private static final Identifier GATHER_TEXTURE =
            Identifier.fromNamespaceAndPath("dopasrandomutilities", "textures/gui/gather.png");

    private static final int PLAYER_INV_HEIGHT = 96;
    private static final int TEXTURE_SIZE = 256;
    private static final int INTERIOR_U = 7;
    private static final int INTERIOR_W = 162;
    private static final int DIVIDER_V = 126;
    private static final int DIVIDER_STRIP_H = 7;

    private static final int LABEL_COLOR = 0xFF404040;
    private static final int BODY_COLOR = 0xFFC6C6C6;

    private static final int BASIC_FOOTER_Y = 35;
    private static final int BASIC_IMAGE_HEIGHT = 114 + 18;
    private static final int LARGE_SLOT = 26;

    private static final int GATHER_BUTTON_SIZE = 13;
    private static final int GATHER_ICON_SIZE = 11;
    private static final int HIGHLIGHT_BORDER = 2;
    private static final float OVER_CAP_PULSE_SPEED = 0.25F;

    private static double pendingMouseX = Double.NaN;
    private static double pendingMouseY = Double.NaN;

    private final int containerRows;
    private final FilterColorPicker colorPicker = new FilterColorPicker();
    private final PanelHost panelHost = new PanelHost();

    @Nullable
    private ConfiguratorPanel configuratorPanel;
    private IconButton gatherButton;
    private boolean gatherConfirmPending;

    public FilterScreen(FilterMenu menu, Inventory inventory, Component title) {
        super(
                menu,
                inventory,
                title,
                176,
                menu.isBasic() ? BASIC_IMAGE_HEIGHT : 114 + menu.getRows() * 18
        );
        this.containerRows = menu.isBasic() ? 1 : menu.getRows();
        this.inventoryLabelY = this.imageHeight - 94;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
    }

    public FilterMenu getMenu() {
        return this.menu;
    }

    public <T extends net.minecraft.client.gui.components.events.GuiEventListener & net.minecraft.client.gui.components.Renderable & net.minecraft.client.gui.narration.NarratableEntry> T addOverlayWidget(T widget) {
        return this.addRenderableWidget(widget);
    }

    public void removeOverlayWidget(net.minecraft.client.gui.components.events.GuiEventListener widget) {
        this.removeWidget(widget);
    }

    public void sendMenuButton(int buttonId) {
        preserveMousePosition(this.minecraft);
        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
    }

    public void sendSlotButton(int normalButtonId, int rowButtonId) {
        sendMenuButton(isShiftHeldPublic() ? rowButtonId : normalButtonId);
    }

    public boolean isShiftHeldPublic() {
        if (this.minecraft == null) {
            return false;
        }
        if (this.minecraft.hasShiftDown()) {
            return true;
        }
        long window = this.minecraft.getWindow().handle();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
    }

    public void openColorPicker() {
        this.colorPicker.open(this);
    }

    public void clearGatherConfirm() {
        this.gatherConfirmPending = false;
    }

    @Override
    public int getPickerColor() {
        return this.menu.getColor();
    }

    @Override
    public void onPickerColorCommitted(int rgb) {
        ClientPacketDistributor.sendToServer(
                com.dopa.randomutilities.filteritem.network.FilterSettingPayload.color(rgb));
    }

    @Override
    public <T extends net.minecraft.client.gui.components.events.GuiEventListener & net.minecraft.client.gui.components.Renderable & net.minecraft.client.gui.narration.NarratableEntry> T addPickerWidget(T widget) {
        return addOverlayWidget(widget);
    }

    @Override
    public void removePickerWidget(net.minecraft.client.gui.components.events.GuiEventListener widget) {
        removeOverlayWidget(widget);
    }

    @Override
    public net.minecraft.client.gui.Font getFont() {
        return this.font;
    }

    @Override
    public int width() {
        return this.width;
    }

    @Override
    public int height() {
        return this.height;
    }

    public static void preserveMousePosition(Minecraft minecraft) {
        if (minecraft == null) {
            return;
        }
        long window = minecraft.getWindow().handle();
        double[] x = new double[1];
        double[] y = new double[1];
        GLFW.glfwGetCursorPos(window, x, y);
        pendingMouseX = x[0];
        pendingMouseY = y[0];
    }

    private void restoreMousePositionIfPending() {
        if (this.minecraft == null || Double.isNaN(pendingMouseX)) {
            return;
        }
        GLFW.glfwSetCursorPos(this.minecraft.getWindow().handle(), pendingMouseX, pendingMouseY);
        this.minecraft.mouseHandler.setIgnoreFirstMove();
        pendingMouseX = Double.NaN;
        pendingMouseY = Double.NaN;
    }

    private Component gatherTooltip() {
        MutableComponent tooltip = Component.translatable("gui.dopasrandomutilities.gather.tooltip");
        if (this.menu.wouldGatherChange()) {
            tooltip.append("\n\n")
                    .append(Component.translatable("gui.dopasrandomutilities.gather.tooltip_warning")
                            .withStyle(ChatFormatting.RED));
            if (this.gatherConfirmPending) {
                tooltip.append("\n\n")
                        .append(Component.translatable("gui.dopasrandomutilities.remove_slot.tooltip_void_confirm")
                                .withStyle(ChatFormatting.DARK_RED));
            }
        }
        return tooltip;
    }

    private void onGatherPressed() {
        if (this.menu.wouldGatherChange()) {
            if (this.gatherConfirmPending) {
                this.gatherConfirmPending = false;
                sendMenuButton(FilterMenu.BTN_GATHER);
            } else {
                this.gatherConfirmPending = true;
                if (this.configuratorPanel != null) {
                    this.configuratorPanel.clearRemoveConfirm();
                }
                if (this.gatherButton != null) {
                    this.gatherButton.updateTooltip(gatherTooltip());
                }
            }
            return;
        }
        this.gatherConfirmPending = false;
        sendMenuButton(FilterMenu.BTN_GATHER);
    }

    @Override
    protected void init() {
        super.init();
        this.panelHost.clear();
        this.configuratorPanel = null;

        this.panelHost.add(new InformativePanel(this.menu.isBasic()));

        if (!this.menu.isBasic()) {
            this.configuratorPanel = new ConfiguratorPanel(this);
            this.panelHost.add(this.configuratorPanel);
            this.panelHost.add(new EnergyPanel());
            this.panelHost.add(new UpgradePanel(this.menu.getUpgradeSlots()));

            this.configuratorPanel.initWidgets();

            this.gatherButton = new IconButton(
                    this.leftPos + this.imageWidth - GATHER_BUTTON_SIZE - 4,
                    this.topPos + 4,
                    GATHER_BUTTON_SIZE,
                    GATHER_ICON_SIZE,
                    GATHER_TEXTURE,
                    gatherTooltip(),
                    this::onGatherPressed
            );
            this.addRenderableWidget(this.gatherButton);
            this.gatherConfirmPending = false;
        }

        this.panelHost.layoutWidgets(this.leftPos, this.topPos, this.imageWidth);
        restoreMousePositionIfPending();
    }

    @Override
    public void containerTick() {
        super.containerTick();
        this.panelHost.tick();
        this.panelHost.layoutWidgets(this.leftPos, this.topPos, this.imageWidth);

        if (!this.menu.isBasic()) {
            if (this.gatherConfirmPending && !this.menu.wouldGatherChange()) {
                this.gatherConfirmPending = false;
            }
            if (this.gatherButton != null) {
                this.gatherButton.updateTooltip(gatherTooltip());
            }
        }
    }

    @Override
    public void onClose() {
        if (this.configuratorPanel != null) {
            this.configuratorPanel.onScreenClose();
        }
        if (!this.menu.isBasic() && this.colorPicker.isOpen()) {
            this.colorPicker.close(this);
        }
        super.onClose();
    }

    private void coverUnusedSlotsOnLastRow(GuiGraphicsExtractor graphics, int xo, int yo, int slotCount) {
        int usedOnLastRow = slotCount % 9;
        if (usedOnLastRow == 0) {
            return;
        }
        int row = slotCount / 9;
        int coverY = yo + 18 + row * 18;
        for (int col = usedOnLastRow; col < 9; col++) {
            VanillaContainerPanel.blitRegion(graphics, xo + 8 + col * 18, coverY, 18, 18,
                    INTERIOR_U, DIVIDER_V, INTERIOR_W, DIVIDER_STRIP_H);
        }
    }

    private void renderBasicBackground(GuiGraphicsExtractor graphics) {
        int xo = this.leftPos;
        int yo = this.topPos;

        graphics.blit(RenderPipelines.GUI_TEXTURED, CHEST_BACKGROUND, xo, yo, 0.0F, 0.0F,
                this.imageWidth, BASIC_FOOTER_Y, TEXTURE_SIZE, TEXTURE_SIZE);
        graphics.fill(xo + 7, yo + 17, xo + this.imageWidth - 7, yo + BASIC_FOOTER_Y, BODY_COLOR);
        graphics.blit(RenderPipelines.GUI_TEXTURED, CHEST_BACKGROUND, xo, yo + BASIC_FOOTER_Y,
                0.0F, 126.0F, this.imageWidth, PLAYER_INV_HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE);

        int frameX = xo + FilterMenu.BASIC_SLOT_X - (LARGE_SLOT - 18) / 2;
        int frameY = yo + FilterMenu.BASIC_SLOT_Y - (LARGE_SLOT - 18) / 2;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, frameX, frameY, LARGE_SLOT, LARGE_SLOT);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        if (this.menu.isBasic()) {
            renderBasicBackground(graphics);
        } else {
            int xo = this.leftPos;
            int yo = this.topPos;
            graphics.blit(RenderPipelines.GUI_TEXTURED, CHEST_BACKGROUND, xo, yo, 0.0F, 0.0F,
                    this.imageWidth, this.containerRows * 18 + 17, TEXTURE_SIZE, TEXTURE_SIZE);
            graphics.blit(RenderPipelines.GUI_TEXTURED, CHEST_BACKGROUND, xo, yo + this.containerRows * 18 + 17,
                    0.0F, 126.0F, this.imageWidth, PLAYER_INV_HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE);
            coverUnusedSlotsOnLastRow(graphics, xo, yo, this.menu.getPageSlotCount());
        }
        this.panelHost.tick();
        this.panelHost.render(graphics, this.font, this.leftPos, this.topPos, this.imageWidth,
                mouseX, mouseY, partialTick);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (!this.menu.isBasic()) {
            renderSelectedSlotHighlight(graphics);
        }
        super.extractContents(graphics, mouseX, mouseY, partialTick);
        if (!this.menu.isBasic()) {
            renderOverCapWarnings(graphics, partialTick);
            if (this.colorPicker.isOpen()) {
                this.colorPicker.renderOnTop(graphics, this, mouseX, mouseY, partialTick);
            }
        }
    }

    private void renderOverCapWarnings(GuiGraphicsExtractor graphics, float partialTick) {
        int maxStack = this.configuratorPanel != null
                ? this.configuratorPanel.effectiveMaxStackForDisplay()
                : this.menu.getMaxStackSizeSetting();
        float pulse = (float) (0.35F + 0.25F * (0.5F + 0.5F * Math.sin(partialTick * OVER_CAP_PULSE_SPEED)));
        int alpha = (int) (pulse * 255.0F) << 24;
        int tint = alpha | 0xFF0000;

        for (int i = 0; i < this.menu.getPageSlotCount(); i++) {
            Slot slot = this.menu.slots.get(i);
            ItemStack stack = slot.getItem();
            if (stack.isEmpty() || stack.getCount() <= maxStack) {
                continue;
            }
            int x = this.leftPos + slot.x;
            int y = this.topPos + slot.y;
            graphics.fill(x, y, x + 16, y + 16, tint);
        }
    }

    private void renderSelectedSlotHighlight(GuiGraphicsExtractor graphics) {
        int selected = this.menu.getSelectedSlot();
        int local = selected - this.menu.getPage() * FilterContents.SLOTS_PER_PAGE;
        if (local < 0 || local >= this.menu.getPageSlotCount()) {
            return;
        }
        Slot slot = this.menu.slots.get(local);
        if (this.menu.isFilterSlotOverCap(local)) {
            return;
        }
        int color = 0xFF000000 | this.menu.getColor();
        int x = this.leftPos + slot.x;
        int y = this.topPos + slot.y;
        int t = HIGHLIGHT_BORDER;
        graphics.fill(x - t, y - t, x + 16 + t, y, color);
        graphics.fill(x - t, y + 16, x + 16 + t, y + 16 + t, color);
        graphics.fill(x - t, y, x, y + 16, color);
        graphics.fill(x + 16, y, x + 16 + t, y + 16, color);
    }

    @Override
    protected void renderSlotContents(GuiGraphicsExtractor graphics, ItemStack itemStack, Slot slot, @Nullable String itemCount) {
        if (!itemStack.isEmpty() && isFilterSlot(slot) && itemStack.getCount() > 1) {
            itemCount = CompactCountFormat.format(itemStack.getCount());
        }
        super.renderSlotContents(graphics, itemStack, slot, itemCount);
    }

    private boolean isFilterSlot(Slot slot) {
        if (this.menu.isBasic()) {
            return slot.index == 0;
        }
        return slot.index < this.menu.getPageSlotCount();
    }

    @Override
    protected List<Component> getTooltipFromContainerItem(ItemStack itemStack) {
        List<Component> tooltip = new ArrayList<>(super.getTooltipFromContainerItem(itemStack));
        if (!this.menu.isBasic() && this.hoveredSlot != null && isFilterSlot(this.hoveredSlot)
                && this.menu.isFilterSlotOverCap(this.hoveredSlot.index)) {
            tooltip.add(Component.empty());
            tooltip.add(Component.translatable("gui.dopasrandomutilities.over_cap.tooltip")
                    .withStyle(ChatFormatting.RED));
        }
        return tooltip;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, LABEL_COLOR, false);
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, LABEL_COLOR, false);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!this.menu.isBasic() && this.colorPicker.isOpen()) {
            if (event.isEscape()) {
                this.colorPicker.close(this);
                return true;
            }
            if (this.minecraft != null && this.minecraft.options.keyInventory.matches(event)) {
                this.colorPicker.close(this);
                return true;
            }
            return true;
        }
        if (this.getFocused() instanceof EditBox editBox) {
            if (event.isConfirmation()) {
                this.clearFocus();
                return true;
            }
            if (editBox.keyPressed(event)) {
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!this.menu.isBasic() && this.colorPicker.isOpen()) {
            if (!this.colorPicker.contains(event.x(), event.y())) {
                this.colorPicker.close(this);
                return true;
            }
            super.mouseClicked(event, doubleClick);
            return true;
        }
        if (this.panelHost.handleTabClick(event.x(), event.y(), this.leftPos, this.topPos, this.imageWidth)) {
            return true;
        }
        boolean handled = super.mouseClicked(event, doubleClick);
        if (this.configuratorPanel != null) {
            this.configuratorPanel.clearFocusIfOutside(event.x(), event.y());
        }
        return handled;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (!this.menu.isBasic() && this.colorPicker.isOpen()) {
            return this.getFocused() != null && this.isDragging() && event.button() == 0
                    && this.getFocused().mouseDragged(event, dx, dy);
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (!this.menu.isBasic() && this.colorPicker.isOpen()) {
            if (event.button() == 0 && this.isDragging()) {
                this.setDragging(false);
                if (this.getFocused() != null) {
                    return this.getFocused().mouseReleased(event);
                }
            }
            return super.mouseReleased(event);
        }
        return super.mouseReleased(event);
    }

    private static final class IconButton extends AbstractWidget {
        private final Identifier texture;
        private final int iconSize;
        private final Runnable onPress;

        IconButton(int x, int y, int size, int iconSize, Identifier texture, Component tooltip, Runnable onPress) {
            super(x, y, size, size, Component.empty());
            this.texture = texture;
            this.iconSize = iconSize;
            this.onPress = onPress;
            this.setTooltip(Tooltip.create(tooltip));
        }

        void updateTooltip(Component tooltip) {
            this.setTooltip(Tooltip.create(tooltip));
        }

        @Override
        public void onClick(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
            if (this.active) {
                this.onPress.run();
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            output.add(NarratedElementType.TITLE, this.getMessage());
        }

        @Override
        public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            int x = this.getX();
            int y = this.getY();
            int bg = this.isHovered() ? 0xFFC6C6C6 : 0xFF8B8B8B;
            graphics.fill(x, y, x + this.width, y + this.height, bg);
            graphics.fill(x + 1, y + 1, x + this.width - 1, y + this.height - 1, 0xFF373737);
            int iconX = x + (this.width - iconSize) / 2;
            int iconY = y + (this.height - iconSize) / 2;
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture, iconX, iconY, 0.0F, 0.0F,
                    iconSize, iconSize, iconSize, iconSize);
        }
    }
}
