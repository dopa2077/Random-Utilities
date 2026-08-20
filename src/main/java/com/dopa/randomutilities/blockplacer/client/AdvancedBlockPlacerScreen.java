package com.dopa.randomutilities.blockplacer.client;

import com.dopa.randomutilities.blockplacer.menu.AdvancedBlockPlacerMenu;
import com.dopa.randomutilities.blockplacer.network.AdvancedBlockPlacerSettingPayload;
import com.dopa.randomutilities.client.WorkingVolumeOverlay;
import com.dopa.randomutilities.gui.widget.FilterModeButton;
import com.dopa.randomutilities.gui.widget.FilterModeIcon;
import com.dopa.randomutilities.gui.widget.FilterRow;
import com.dopa.randomutilities.gui.widget.IconButton;
import com.dopa.randomutilities.gui.widget.JeiGhostDragState;
import com.dopa.randomutilities.gui.machine.MuteCosmeticPanel;
import com.dopa.randomutilities.gui.panel.PanelAnchor;
import com.dopa.randomutilities.gui.panel.PanelHost;
import com.dopa.randomutilities.gui.panel.ScrollingInfoPanel;
import com.dopa.randomutilities.gui.machine.VolumeMachineGui;
import com.dopa.randomutilities.gui.machine.WorkingVolumeConfigPanel;
import com.dopa.randomutilities.machine.RedstoneMode;
import com.dopa.randomutilities.gui.machine.EnergyUpgradeRemoveConfirm;
import com.dopa.randomutilities.gui.machine.MachineEnergyBar;
import com.dopa.randomutilities.gui.machine.UpgradeSlotTooltips;
import com.dopa.randomutilities.gui.machine.MachineEnergyPanel;
import com.dopa.randomutilities.gui.machine.MachineRedstonePanel;
import com.dopa.randomutilities.gui.machine.MachineUpgradePanel;
import com.dopa.randomutilities.util.WorkingVolume;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
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

public class AdvancedBlockPlacerScreen extends AbstractContainerScreen<AdvancedBlockPlacerMenu>
        implements VolumeMachineGui, MachineEnergyPanel.Host {
    private static final Identifier BACKGROUND =
            Identifier.fromNamespaceAndPath("dopasrandomutilities", "textures/gui/machine/advanced_block_placer.png");
    private static final Identifier RANGE_OVERLAY_ICON =
            Identifier.fromNamespaceAndPath("dopasrandomutilities", "textures/gui/widget/hitbox.png");

    private static final int TEXTURE_SIZE = 256;
    private static final int LABEL_COLOR = 0xFF404040;
    private static final int OVERLAY_BUTTON_SIZE = 13;

    private final PanelHost panelHost = new PanelHost();
    @Nullable
    private WorkingVolumeConfigPanel configPanel;
    @Nullable
    private MuteCosmeticPanel mutePanel;
    @Nullable
    private MachineRedstonePanel redstonePanel;
    @Nullable
    private FilterModeButton modeButton;
    @Nullable
    private IconButton rangeOverlayButton;
    private final EnergyUpgradeRemoveConfirm energyRemoveConfirm = new EnergyUpgradeRemoveConfirm();

    public AdvancedBlockPlacerScreen(AdvancedBlockPlacerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, AdvancedBlockPlacerMenu.IMAGE_HEIGHT);
        this.inventoryLabelY = this.imageHeight - 94;
        this.titleLabelY = 6;
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

    @Override
    public Font getFont() {
        return font;
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
    public boolean isShiftHeld() {
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

    @Override
    public int rangeX() {
        return menu.getRangeX();
    }

    @Override
    public int rangeY() {
        return menu.getRangeY();
    }

    @Override
    public int rangeZ() {
        return menu.getRangeZ();
    }

    @Override
    public int offsetX() {
        return menu.getOffsetX();
    }

    @Override
    public int offsetY() {
        return menu.getOffsetY();
    }

    @Override
    public int offsetZ() {
        return menu.getOffsetZ();
    }

    @Override
    public boolean isMuted() {
        return menu.isMuted();
    }

    @Override
    public int overlayColor() {
        return menu.getOverlayColor();
    }

    @Override
    public int maxRange() {
        return menu.maxRange();
    }

    @Override
    public int energyStored() {
        return menu.energyStored();
    }

    @Override
    public int energyCapacity() {
        return menu.energyCapacity();
    }

    @Override
    public int energyUsage() {
        return menu.energyUsage();
    }

    @Override
    public int energyMaxReceive() {
        return menu.energyMaxReceive();
    }

    @Override
    public void sendVolumeSetting(byte kind, int value) {
        ClientPacketDistributor.sendToServer(new AdvancedBlockPlacerSettingPayload(kind, value));
    }

    public boolean isRangeOverlayEnabled() {
        var be = menu.blockEntity();
        var level = be.getLevel();
        if (level == null) {
            return false;
        }
        return WorkingVolumeOverlay.isEnabled(level.dimension(), be.getBlockPos());
    }

    @Override
    protected void init() {
        super.init();
        panelHost.clear();
        configPanel = null;
        mutePanel = null;
        redstonePanel = null;

        panelHost.add(new ScrollingInfoPanel(
                "gui.dopasrandomutilities.panel.info.advanced_block_placer.intro"
        ));

        configPanel = new WorkingVolumeConfigPanel(this);
        panelHost.add(configPanel);
        configPanel.initWidgets();

        mutePanel = new MuteCosmeticPanel(this);
        panelHost.add(mutePanel);
        mutePanel.initWidgets();

        panelHost.add(new MachineEnergyPanel(this));
        panelHost.add(new MachineUpgradePanel(menu.getUpgradeSlots(), PanelAnchor.RIGHT_BELOW, 0));

        redstonePanel = new MachineRedstonePanel(
                this,
                PanelAnchor.RIGHT_LOW,
                0,
                mode -> sendVolumeSetting(WorkingVolume.KIND_REDSTONE, mode.ordinal())
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

        modeButton = new FilterModeButton(
                leftPos + AdvancedBlockPlacerMenu.ICON_X,
                topPos + AdvancedBlockPlacerMenu.FILTER_SLOT_Y,
                filterModeTooltip(),
                this::toggleFilterMode
        );
        addRenderableWidget(modeButton);
    }

    private Component rangeOverlayTooltip() {
        boolean enabled = isRangeOverlayEnabled();
        return Component.translatable("gui.dopasrandomutilities.working_volume.range_overlay")
                .append("\n")
                .append(Component.translatable(enabled
                                ? "gui.dopasrandomutilities.item_collector.range_overlay.enabled"
                                : "gui.dopasrandomutilities.item_collector.range_overlay.disabled")
                        .withStyle(enabled ? ChatFormatting.AQUA : ChatFormatting.GRAY));
    }

    private Component filterModeTooltip() {
        return Component.translatable(menu.isWhitelistMode()
                ? "gui.dopasrandomutilities.item_collector.whitelist"
                : "gui.dopasrandomutilities.item_collector.blacklist");
    }

    private void toggleRangeOverlay() {
        var be = menu.blockEntity();
        var level = be.getLevel();
        if (level == null) {
            return;
        }
        WorkingVolumeOverlay.toggle(level.dimension(), be.getBlockPos());
        if (rangeOverlayButton != null) {
            rangeOverlayButton.updateTooltip(rangeOverlayTooltip());
        }
    }

    private void toggleFilterMode() {
        sendVolumeSetting(WorkingVolume.KIND_FILTER_MODE, menu.isWhitelistMode() ? 0 : 1);
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
    public void onClose() {
        JeiGhostDragState.endDrag();
        super.onClose();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        panelHost.render(graphics, font, leftPos, topPos, imageWidth, mouseX, mouseY, partialTick);

        int xo = leftPos;
        int yo = topPos;
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                BACKGROUND,
                xo,
                yo,
                0.0F,
                0.0F,
                imageWidth,
                imageHeight,
                TEXTURE_SIZE,
                TEXTURE_SIZE
        );
        FilterRow.blit(
                graphics,
                xo + AdvancedBlockPlacerMenu.ICON_X,
                yo + AdvancedBlockPlacerMenu.FILTER_SLOT_Y,
                AdvancedBlockPlacerMenu.FILTER_SLOT_COUNT,
                i -> menu.slots.get(menu.filterSlotStart() + i).hasItem()
        );
        MachineEnergyBar.render(
                graphics,
                xo + AdvancedBlockPlacerMenu.ENERGY_BAR_X,
                yo + AdvancedBlockPlacerMenu.ENERGY_BAR_Y,
                AdvancedBlockPlacerMenu.ENERGY_BAR_W,
                AdvancedBlockPlacerMenu.ENERGY_BAR_H,
                menu.energyStored(),
                menu.energyCapacity()
        );
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractContents(graphics, mouseX, mouseY, partialTick);
        renderGhostSlotTints(graphics);
        renderFilterModeIcon(graphics);
        JeiGhostDragState.renderLine(graphics, mouseX, mouseY);
        Component tabTooltip = panelHost.hoveredTabTooltip(mouseX, mouseY, leftPos, topPos, imageWidth);
        if (tabTooltip != null) {
            graphics.setTooltipForNextFrame(font, tabTooltip, mouseX, mouseY);
            return;
        }
        if (UpgradeSlotTooltips.applyHover(
                graphics,
                font,
                mouseX,
                mouseY,
                hoveredSlot,
                hoveredSlot != null && menu.isUpgradeSlotIndex(hoveredSlot.index),
                menu.blockEntity().upgrades(),
                energyRemoveConfirm.pendingFor(hoveredSlot, menu.blockEntity().upgrades(), menu.energyStored())
        )) {
            return;
        }
        FilterRow.applyEmptyHover(graphics, font, mouseX, mouseY, hoveredSlot);
        if (MachineEnergyBar.isHover(
                mouseX,
                mouseY,
                leftPos + AdvancedBlockPlacerMenu.ENERGY_BAR_X,
                topPos + AdvancedBlockPlacerMenu.ENERGY_BAR_Y,
                AdvancedBlockPlacerMenu.ENERGY_BAR_W,
                AdvancedBlockPlacerMenu.ENERGY_BAR_H
        )) {
            MachineEnergyBar.renderHoverTooltip(graphics, font, mouseX, mouseY, menu.energyStored(), menu.energyCapacity());
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, (imageWidth - font.width(title)) / 2, titleLabelY, LABEL_COLOR, false);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, LABEL_COLOR, false);
    }

    private void renderGhostSlotTints(GuiGraphicsExtractor graphics) {
        int start = menu.filterSlotStart();
        for (int i = 0; i < AdvancedBlockPlacerMenu.FILTER_SLOT_COUNT; i++) {
            Slot slot = menu.slots.get(start + i);
            if (!slot.hasItem()) {
                continue;
            }
            int x = leftPos + slot.x;
            int y = topPos + slot.y;
            graphics.fill(x, y, x + 16, y + 16, 0x40FFFFFF);
        }
    }

    private void renderFilterModeIcon(GuiGraphicsExtractor graphics) {
        FilterModeIcon.render(
                graphics,
                menu.isWhitelistMode(),
                leftPos + AdvancedBlockPlacerMenu.ICON_X,
                topPos + AdvancedBlockPlacerMenu.FILTER_SLOT_Y,
                modeButton != null && modeButton.isHovered()
        );
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
        boolean overConfigControl = configPanel != null
                && configPanel.contentsInteractive()
                && configPanel.isMouseOverInteractiveWidget(event.x(), event.y());
        boolean overMuteControl = mutePanel != null
                && mutePanel.contentsInteractive()
                && mutePanel.isMouseOverInteractiveWidget(event.x(), event.y());
        if (overBody || overConfigControl || overMuteControl) {
            for (int i = children().size() - 1; i >= 0; i--) {
                GuiEventListener child = children().get(i);
                if (child.mouseClicked(event, doubleClick)) {
                    setFocused(child);
                    if (child instanceof Button) {
                        clearFocus();
                    } else {
                        setDragging(true);
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
            return true;
        }
        return panelHost.handleTabClick(event.x(), event.y(), leftPos, topPos, imageWidth);
    }

    @Override
    protected void slotClicked(Slot slot, int slotIndex, int mouseButton, ContainerInput type) {
        if (energyRemoveConfirm.block(
                slot,
                mouseButton,
                type,
                menu.getCarried(),
                menu.blockEntity().upgrades(),
                menu.energyStored()
        )) {
            return;
        }
        super.slotClicked(slot, slotIndex, mouseButton, type);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
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
}
