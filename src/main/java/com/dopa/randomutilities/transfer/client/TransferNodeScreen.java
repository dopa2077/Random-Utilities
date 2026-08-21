package com.dopa.randomutilities.transfer.client;

import com.dopa.randomutilities.gui.widget.FilterModeButton;
import com.dopa.randomutilities.gui.widget.FilterModeIcon;
import com.dopa.randomutilities.gui.widget.FilterRow;
import com.dopa.randomutilities.gui.widget.JeiGhostDragState;
import com.dopa.randomutilities.gui.panel.PanelAnchor;
import com.dopa.randomutilities.gui.panel.PanelHost;
import com.dopa.randomutilities.gui.panel.ScrollingInfoPanel;
import com.dopa.randomutilities.machine.RedstoneMode;
import com.dopa.randomutilities.gui.machine.MachineRedstonePanel;
import com.dopa.randomutilities.gui.machine.MachineUpgradePanel;
import com.dopa.randomutilities.gui.machine.UpgradeSlotTooltips;
import com.dopa.randomutilities.transfer.HeadKind;
import com.dopa.randomutilities.transfer.TransferNodeItem;
import com.dopa.randomutilities.transfer.menu.TransferNodeMenu;
import com.dopa.randomutilities.transfer.network.TransferNodeSettingPayload;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class TransferNodeScreen extends AbstractContainerScreen<TransferNodeMenu>
        implements MachineRedstonePanel.Host {
    private static final Identifier BACKGROUND =
            Identifier.fromNamespaceAndPath("dopasrandomutilities", "textures/gui/special/trash_can.png");
    private static final int TEXTURE_SIZE = 256;

    private final PanelHost panelHost = new PanelHost();
    private final int tabYBias = TransferNodeMenu.TAB_Y_BIAS;
    @Nullable
    private MachineRedstonePanel redstonePanel;
    @Nullable
    private FilterModeButton modeButton;

    public TransferNodeScreen(TransferNodeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, TransferNodeMenu.IMAGE_WIDTH, TransferNodeMenu.IMAGE_HEIGHT);
        this.inventoryLabelY = this.imageHeight - 94;
        this.titleLabelX = 8;
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
    public <T extends GuiEventListener & net.minecraft.client.gui.components.Renderable
            & net.minecraft.client.gui.narration.NarratableEntry> T addOverlayWidget(T widget) {
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
        redstonePanel = null;

        panelHost.add(new ScrollingInfoPanel(infoKeys()));
        panelHost.add(new MachineUpgradePanel(menu.getUpgradeSlots(), PanelAnchor.RIGHT_TOP, tabYBias));
        redstonePanel = new MachineRedstonePanel(this, PanelAnchor.RIGHT_BELOW, tabYBias);
        panelHost.add(redstonePanel);
        redstonePanel.initWidgets();
        panelHost.layoutWidgets(leftPos, topPos, imageWidth);

        modeButton = new FilterModeButton(
                leftPos + TransferNodeMenu.FILTER_ICON_X,
                topPos + TransferNodeMenu.FILTER_SLOT_Y,
                filterModeTooltip(),
                this::toggleFilterMode
        );
        addRenderableWidget(modeButton);
    }

    private String[] infoKeys() {
        if (menu.kind() == HeadKind.FLUID) {
            return new String[] {
                    "gui.dopasrandomutilities.panel.info.generator.visual",
                    "gui.dopasrandomutilities.panel.info.transfer_node_fluid.intro",
                    "gui.dopasrandomutilities.panel.info.transfer_node_fluid.speed",
                    "gui.dopasrandomutilities.panel.info.transfer_node_fluid.upgrades"
            };
        }
        return new String[] {
                "gui.dopasrandomutilities.panel.info.generator.visual",
                "gui.dopasrandomutilities.panel.info.transfer_node.intro",
                "gui.dopasrandomutilities.panel.info.transfer_node.speed",
                "gui.dopasrandomutilities.panel.info.transfer_node.upgrades"
        };
    }

    private void toggleFilterMode() {
        boolean next = !menu.isWhitelistMode();
        ClientPacketDistributor.sendToServer(new TransferNodeSettingPayload(next));
    }

    private Component filterModeTooltip() {
        return Component.translatable(menu.isWhitelistMode()
                ? "gui.dopasrandomutilities.item_collector.whitelist"
                : "gui.dopasrandomutilities.item_collector.blacklist");
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
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                BACKGROUND,
                leftPos,
                topPos,
                0.0F,
                0.0F,
                imageWidth,
                imageHeight,
                TEXTURE_SIZE,
                TEXTURE_SIZE
        );
        FilterRow.blit(
                graphics,
                leftPos + TransferNodeMenu.FILTER_ICON_X,
                topPos + TransferNodeMenu.FILTER_SLOT_Y,
                TransferNodeMenu.FILTER_SLOT_COUNT,
                i -> menu.slots.get(TransferNodeMenu.FILTER_START + i).hasItem()
        );
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractContents(graphics, mouseX, mouseY, partialTick);
        FilterModeIcon.render(
                graphics,
                menu.isWhitelistMode(),
                leftPos + TransferNodeMenu.FILTER_ICON_X,
                topPos + TransferNodeMenu.FILTER_SLOT_Y,
                modeButton != null && modeButton.isHovered()
        );
        int ghostEnd = TransferNodeMenu.FILTER_START + TransferNodeMenu.FILTER_SLOT_COUNT;
        Slot displaySlot = menu.slots.get(TransferNodeMenu.DISPLAY_SLOT);
        tintGhostSlot(graphics, displaySlot);
        for (int i = TransferNodeMenu.FILTER_START; i < ghostEnd; i++) {
            tintGhostSlot(graphics, menu.slots.get(i));
        }
        JeiGhostDragState.renderLine(graphics, mouseX, mouseY);
        Component tabTooltip = panelHost.hoveredTabTooltip(mouseX, mouseY, leftPos, topPos, imageWidth);
        if (tabTooltip != null) {
            graphics.setTooltipForNextFrame(font, tabTooltip, mouseX, mouseY);
            return;
        }
        if (hoveredSlot != null && menu.isDisplaySlot(hoveredSlot) && !hoveredSlot.hasItem()) {
            List<FormattedCharSequence> lines = new ArrayList<>();
            for (Component line : displaySlotTooltipLines()) {
                lines.add(line.getVisualOrderText());
            }
            graphics.setTooltipForNextFrame(font, lines, mouseX, mouseY);
            return;
        }
        UpgradeSlotTooltips.applyHover(
                graphics,
                font,
                mouseX,
                mouseY,
                hoveredSlot,
                hoveredSlot != null && menu.isUpgradeSlotIndex(hoveredSlot.index),
                menu.upgrades()
        );
        FilterRow.applyEmptyHover(graphics, font, mouseX, mouseY, hoveredSlot);
    }

    @Override
    protected List<Component> getTooltipFromContainerItem(ItemStack itemStack) {
        List<Component> tooltip = new ArrayList<>(super.getTooltipFromContainerItem(itemStack));
        if (hoveredSlot != null && menu.isDisplaySlot(hoveredSlot)) {
            tooltip.addAll(displaySlotTooltipLines());
        }
        return tooltip;
    }

    private List<Component> displaySlotTooltipLines() {
        List<Component> lines = new ArrayList<>(2);
        lines.add(Component.translatable(menu.kind() == HeadKind.FLUID
                        ? "gui.dopasrandomutilities.transfer_node_fluid.visual"
                        : "gui.dopasrandomutilities.transfer_node.visual")
                .withStyle(ChatFormatting.GRAY));
        var upgrades = menu.upgrades();
        lines.add(TransferNodeItem.rateLine(
                menu.kind(),
                upgrades.overclockCount(),
                upgrades.stackCount(),
                upgrades.fluidCapacityCount()
        ));
        return lines;
    }

    private void tintGhostSlot(GuiGraphicsExtractor graphics, Slot slot) {
        if (!slot.hasItem()) {
            return;
        }
        int x = leftPos + slot.x;
        int y = topPos + slot.y;
        graphics.fill(x, y, x + 16, y + 16, 0x40FFFFFF);
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
        if (overBody) {
            for (int i = children().size() - 1; i >= 0; i--) {
                GuiEventListener child = children().get(i);
                if (child.mouseClicked(event, doubleClick)) {
                    setFocused(child);
                    clearFocus();
                    return true;
                }
            }
            for (int i = children().size() - 1; i >= 0; i--) {
                GuiEventListener child = children().get(i);
                if (child instanceof AbstractWidget widget && widget.visible && !widget.active
                        && isOverWidget(widget, event.x(), event.y())) {
                    return true;
                }
            }
            Slot slotUnder = findActiveSlotAt(event.x(), event.y());
            if (slotUnder != null && menu.isUpgradeSlotIndex(slotUnder.index)) {
                return super.mouseClicked(event, doubleClick);
            }
            if (panelHost.mouseClicked(event.x(), event.y())) {
                return true;
            }
            return panelHost.handleTabClick(event.x(), event.y(), leftPos, topPos, imageWidth);
        }
        if (overTab) {
            return panelHost.handleTabClick(event.x(), event.y(), leftPos, topPos, imageWidth);
        }
        boolean handled = super.mouseClicked(event, doubleClick);
        if (handled) {
            clearFocus();
            return true;
        }
        return panelHost.handleTabClick(event.x(), event.y(), leftPos, topPos, imageWidth);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (panelHost.mouseDragged(event.x(), event.y())) {
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        boolean panelHandled = panelHost.mouseReleased();
        boolean handled = super.mouseReleased(event);
        return panelHandled || handled;
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
