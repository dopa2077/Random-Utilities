package com.dopa.randomutilities.itemcollector.client;

import com.dopa.randomutilities.filter.client.GhostFilterClicks;
import com.dopa.randomutilities.gui.widget.FilterModeButton;
import com.dopa.randomutilities.gui.widget.FilterModeIcon;
import com.dopa.randomutilities.gui.widget.FilterRow;
import com.dopa.randomutilities.gui.widget.IconButton;
import com.dopa.randomutilities.gui.widget.JeiGhostDragState;
import com.dopa.randomutilities.gui.panel.PanelAnchor;
import com.dopa.randomutilities.gui.panel.PanelHost;
import com.dopa.randomutilities.itemcollector.menu.ItemCollectorMenu;
import com.dopa.randomutilities.itemcollector.network.ItemCollectorSettingPayload;
import com.dopa.randomutilities.itemcollector.client.panel.ItemCollectorConfigPanel;
import com.dopa.randomutilities.itemcollector.client.panel.ItemCollectorCosmeticPanel;
import com.dopa.randomutilities.gui.panel.ScrollingInfoPanel;
import com.dopa.randomutilities.itemcollector.ItemCollectorType;
import com.dopa.randomutilities.machine.RedstoneMode;
import com.dopa.randomutilities.gui.machine.UpgradeSlotTooltips;
import com.dopa.randomutilities.gui.machine.MachineRedstonePanel;
import com.dopa.randomutilities.gui.machine.MachineUpgradePanel;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.function.Supplier;

public class ItemCollectorScreen extends AbstractContainerScreen<ItemCollectorMenu>
        implements MachineRedstonePanel.Host {
    /** Hopper chrome; the 5 hopper wells are covered and replaced by {@code FilterRow}. */
    private static final Identifier BACKGROUND =
            Identifier.withDefaultNamespace("textures/gui/container/hopper.png");
    private static final Identifier RANGE_OVERLAY_ICON =
            Identifier.fromNamespaceAndPath("dopasrandomutilities", "textures/gui/widget/hitbox.png");

    private static final int TEXTURE_SIZE = 256;
    private static final int BODY_COLOR = 0xFFC6C6C6;
    private static final int IMAGE_HEIGHT = 133;
    private static final int HOPPER_WELL_X = 43;
    private static final int HOPPER_WELL_Y = 19;
    private static final int HOPPER_WELL_W = 90;
    private static final int HOPPER_WELL_H = 18;
    private static final int OVERLAY_BUTTON_SIZE = 13;

    private final PanelHost panelHost = new PanelHost();
    @Nullable
    private MachineRedstonePanel redstonePanel;
    @Nullable
    private ItemCollectorConfigPanel configPanel;
    @Nullable
    private ItemCollectorCosmeticPanel cosmeticPanel;
    @Nullable
    private FilterModeButton modeButton;
    @Nullable
    private IconButton rangeOverlayButton;

    public ItemCollectorScreen(ItemCollectorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, IMAGE_HEIGHT);
        this.inventoryLabelY = this.imageHeight - 94;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
    }

    public ItemCollectorMenu getMenu() {
        return menu;
    }

    public PanelHost getPanelHost() {
        return panelHost;
    }

    public int leftPos() {
        return leftPos;
    }

    public int topPos() {
        return topPos;
    }

    public int imageWidth() {
        return imageWidth;
    }

    public boolean isRangeOverlayEnabled() {
        var be = menu.blockEntity();
        var level = be.getLevel();
        if (level == null) {
            return false;
        }
        return ItemCollectorClientOverlay.isEnabled(level.dimension(), be.getBlockPos());
    }

    @Override
    public <T extends GuiEventListener & Renderable & NarratableEntry> T addOverlayWidget(T widget) {
        return addRenderableWidget(widget);
    }

    @Override
    public Supplier<RedstoneMode> redstoneMode() {
        return menu::redstoneMode;
    }

    @Override
    protected void init() {
        super.init();
        panelHost.clear();
        configPanel = null;
        cosmeticPanel = null;
        redstonePanel = null;

        panelHost.add(new ScrollingInfoPanel(infoParagraphKeys(menu.collectorType())));

        configPanel = new ItemCollectorConfigPanel(this);
        panelHost.add(configPanel);
        configPanel.initWidgets();

        cosmeticPanel = new ItemCollectorCosmeticPanel(this);
        panelHost.add(cosmeticPanel);
        cosmeticPanel.initWidgets();

        panelHost.add(new MachineUpgradePanel(menu.getUpgradeSlots(), PanelAnchor.RIGHT_TOP, 0));

        redstonePanel = new MachineRedstonePanel(
                this,
                PanelAnchor.RIGHT_BELOW,
                0,
                mode -> ClientPacketDistributor.sendToServer(new ItemCollectorSettingPayload(
                        ItemCollectorSettingPayload.KIND_REDSTONE,
                        mode.ordinal()
                ))
        );
        panelHost.add(redstonePanel);
        redstonePanel.initWidgets();

        panelHost.layoutWidgets(leftPos, topPos, imageWidth);

        rangeOverlayButton = new IconButton(
                leftPos + imageWidth - OVERLAY_BUTTON_SIZE - 4,
                topPos + 4,
                OVERLAY_BUTTON_SIZE,
                RANGE_OVERLAY_ICON,
                rangeOverlayTooltip(),
                this::toggleRangeOverlay
        );
        addRenderableWidget(rangeOverlayButton);

        int iconX = ItemCollectorMenu.iconX(menu.collectorType());
        modeButton = new FilterModeButton(
                leftPos + iconX,
                topPos + ItemCollectorMenu.FILTER_SLOT_Y,
                filterModeTooltip(),
                this::toggleFilterMode
        );
        addRenderableWidget(modeButton);
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

    private Component rangeOverlayTooltip() {
        boolean enabled = isRangeOverlayEnabled();
        return Component.translatable("gui.dopasrandomutilities.item_collector.range_overlay")
                .append("\n")
                .append(Component.translatable(enabled
                                ? "gui.dopasrandomutilities.item_collector.range_overlay.enabled"
                                : "gui.dopasrandomutilities.item_collector.range_overlay.disabled")
                        .withStyle(enabled ? ChatFormatting.AQUA : ChatFormatting.GRAY));
    }

    private void toggleRangeOverlay() {
        var be = menu.blockEntity();
        var level = be.getLevel();
        if (level == null) {
            return;
        }
        ItemCollectorClientOverlay.toggle(level.dimension(), be.getBlockPos());
        if (rangeOverlayButton != null) {
            rangeOverlayButton.updateTooltip(rangeOverlayTooltip());
        }
    }

    private void toggleFilterMode() {
        boolean next = !menu.isWhitelistMode();
        ClientPacketDistributor.sendToServer(new ItemCollectorSettingPayload(
                ItemCollectorSettingPayload.KIND_FILTER_MODE,
                next ? 1 : 0
        ));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        panelHost.layoutWidgets(leftPos, topPos, imageWidth);
        if (modeButton != null) {
            modeButton.updateTooltip(filterModeTooltip());
        }
    }

    @Override
    protected void slotClicked(Slot slot, int slotIndex, int mouseButton, ContainerInput type) {
        if (GhostFilterClicks.blockDrag(slot, mouseButton, type)) {
            return;
        }
        super.slotClicked(slot, slotIndex, mouseButton, type);
    }

    @Override
    public void onClose() {
        GhostFilterClicks.reset();
        JeiGhostDragState.endDrag();
        if (configPanel != null) {
            configPanel.onScreenClose();
        }
        super.onClose();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        panelHost.render(graphics, font, leftPos, topPos, imageWidth, mouseX, mouseY, partialTick);

        int xo = leftPos;
        int yo = topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, xo, yo, 0.0F, 0.0F,
                imageWidth, imageHeight, TEXTURE_SIZE, TEXTURE_SIZE);
        graphics.fill(
                xo + HOPPER_WELL_X,
                yo + HOPPER_WELL_Y,
                xo + HOPPER_WELL_X + HOPPER_WELL_W,
                yo + HOPPER_WELL_Y + HOPPER_WELL_H,
                BODY_COLOR
        );

        int filterCount = menu.collectorType().filterSlotCount();
        int filterStart = menu.filterSlotStart();
        FilterRow.blit(
                graphics,
                xo + ItemCollectorMenu.iconX(menu.collectorType()),
                yo + ItemCollectorMenu.FILTER_SLOT_Y,
                filterCount,
                i -> menu.slots.get(filterStart + i).hasItem()
        );
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractContents(graphics, mouseX, mouseY, partialTick);
        renderGhostSlotTints(graphics);
        renderFilterModeIcon(graphics, mouseX, mouseY);
        JeiGhostDragState.renderLine(graphics, mouseX, mouseY);
        Component tabTooltip = panelHost.hoveredTabTooltip(mouseX, mouseY, leftPos, topPos, imageWidth);
        if (tabTooltip != null) {
            graphics.setTooltipForNextFrame(font, tabTooltip, mouseX, mouseY);
            return;
        }
        UpgradeSlotTooltips.applyHover(
                graphics,
                font,
                mouseX,
                mouseY,
                hoveredSlot,
                hoveredSlot != null && menu.isUpgradeSlotIndex(hoveredSlot.index),
                menu.blockEntity().upgrades()
        );
        FilterRow.applyEmptyHover(graphics, font, mouseX, mouseY, hoveredSlot);
    }

    private void renderGhostSlotTints(GuiGraphicsExtractor graphics) {
        int count = menu.collectorType().filterSlotCount();
        int start = menu.filterSlotStart();
        for (int i = 0; i < count; i++) {
            Slot slot = menu.slots.get(start + i);
            if (!slot.hasItem()) {
                continue;
            }
            int x = leftPos + slot.x;
            int y = topPos + slot.y;
            graphics.fill(x, y, x + 16, y + 16, 0x40FFFFFF);
        }
    }

    private void renderFilterModeIcon(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int iconX = ItemCollectorMenu.iconX(menu.collectorType());
        boolean hovered = modeButton != null && modeButton.isHovered();
        FilterModeIcon.render(
                graphics,
                menu.isWhitelistMode(),
                leftPos + iconX,
                topPos + ItemCollectorMenu.FILTER_SLOT_Y,
                hovered
        );
    }

    private Component filterModeTooltip() {
        return Component.translatable(menu.isWhitelistMode()
                ? "gui.dopasrandomutilities.item_collector.whitelist"
                : "gui.dopasrandomutilities.item_collector.blacklist");
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        boolean overTab = false;
        for (var panel : panelHost.panels()) {
            if (panel.isMouseOverTab(event.x(), event.y(), leftPos, topPos, imageWidth)) {
                overTab = true;
                break;
            }
        }
        var occupying = panelHost.openPanel();
        boolean overBody = occupying != null
                && occupying.isMouseOverBody(event.x(), event.y(), leftPos, topPos, imageWidth);
        // Config steppers sit near the inventory attachment; treat their hitboxes as panel clicks
        // even when the cursor straddles leftPos by a few pixels.
        boolean overConfigControl = configPanel != null
                && configPanel.contentsInteractive()
                && configPanel.isMouseOverInteractiveWidget(event.x(), event.y());
        boolean overCosmeticControl = cosmeticPanel != null
                && cosmeticPanel.contentsInteractive()
                && cosmeticPanel.isMouseOverInteractiveWidget(event.x(), event.y());
        // Open body covers sibling tabs at the attachment edge — handle body/scrollbar first.
        // Closed sibling tabs can still overlap config widgets; prefer the widget when on one.
        if (overBody || overConfigControl || overCosmeticControl) {
            for (int i = children().size() - 1; i >= 0; i--) {
                GuiEventListener child = children().get(i);
                if (child.mouseClicked(event, doubleClick)) {
                    setFocused(child);
                    // Buttons/steppers: no drag. EditBoxes and ChannelSliders need setDragging for drag.
                    if (child instanceof Button) {
                        clearFocus();
                    } else {
                        setDragging(true);
                    }
                    if (configPanel != null) {
                        configPanel.clearFocusIfOutside(event.x(), event.y());
                    }
                    return true;
                }
            }
            for (int i = children().size() - 1; i >= 0; i--) {
                GuiEventListener child = children().get(i);
                if (child instanceof AbstractWidget widget
                        && widget.visible
                        && !widget.active
                        && isOverWidget(widget, event.x(), event.y())) {
                    return true;
                }
            }
            if (overBody) {
                Slot slotUnder = findActiveSlotAt(event.x(), event.y());
                if (slotUnder != null && menu.isUpgradeSlotIndex(slotUnder.index)) {
                    return super.mouseClicked(event, doubleClick);
                }
                if (panelHost.mouseClicked(event.x(), event.y())) {
                    return true;
                }
                return panelHost.handleTabClick(event.x(), event.y(), leftPos, topPos, imageWidth);
            }
            return false;
        }
        if (overTab) {
            return panelHost.handleTabClick(event.x(), event.y(), leftPos, topPos, imageWidth);
        }
        boolean handled = super.mouseClicked(event, doubleClick);
        if (handled) {
            if (!(getFocused() instanceof EditBox)) {
                clearFocus();
            }
            if (configPanel != null) {
                configPanel.clearFocusIfOutside(event.x(), event.y());
            }
            return true;
        }
        if (panelHost.handleTabClick(event.x(), event.y(), leftPos, topPos, imageWidth)) {
            return true;
        }
        if (configPanel != null) {
            configPanel.clearFocusIfOutside(event.x(), event.y());
        }
        return false;
    }
    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        GhostFilterClicks.clearRightDrag();
        boolean panelHandled = panelHost.mouseReleased();
        GuiEventListener focused = getFocused();
        boolean handled = focused != null && focused.mouseReleased(event);
        setDragging(false);
        if (panelHandled || handled) {
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (panelHost.mouseDragged(event.x(), event.y())) {
            return true;
        }
        GhostFilterClicks.onMouseDragged(event);
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (panelHost.mouseScrolled(mouseX, mouseY, scrollY, leftPos, topPos, imageWidth, font)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private Slot findActiveSlotAt(double mouseX, double mouseY) {
        for (Slot slot : menu.slots) {
            if (slot.isActive() && isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                return slot;
            }
        }
        return null;
    }

    private static boolean isOverWidget(AbstractWidget widget, double mouseX, double mouseY) {
        return mouseX >= widget.getX()
                && mouseY >= widget.getY()
                && mouseX < widget.getX() + widget.getWidth()
                && mouseY < widget.getY() + widget.getHeight();
    }

    private static String[] infoParagraphKeys(ItemCollectorType type) {
        if (type == ItemCollectorType.BASIC) {
            return new String[] {
                    "gui.dopasrandomutilities.panel.info.item_collector.intro",
                    "gui.dopasrandomutilities.panel.info.item_collector.basic"
            };
        }
        return new String[] {
                "gui.dopasrandomutilities.panel.info.item_collector.intro",
                "gui.dopasrandomutilities.panel.info.item_collector.advanced"
        };
    }
}
