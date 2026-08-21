package com.dopa.randomutilities.transfer.client;

import com.dopa.randomutilities.gui.machine.MachineRedstonePanel;
import com.dopa.randomutilities.gui.machine.MachineUpgradePanel;
import com.dopa.randomutilities.gui.machine.UpgradeSlotTooltips;
import com.dopa.randomutilities.gui.panel.PanelAnchor;
import com.dopa.randomutilities.gui.panel.PanelHost;
import com.dopa.randomutilities.gui.panel.ScrollingInfoPanel;
import com.dopa.randomutilities.machine.RedstoneMode;
import com.dopa.randomutilities.machine.config.UpgradeConfig;
import com.dopa.randomutilities.transfer.HeadKind;
import com.dopa.randomutilities.transfer.menu.TransferEnergyMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class TransferEnergyScreen extends AbstractContainerScreen<TransferEnergyMenu>
        implements MachineRedstonePanel.Host {
    private static final Identifier BACKGROUND =
            Identifier.fromNamespaceAndPath("dopasrandomutilities", "textures/gui/special/trash_can.png");
    private static final int TEXTURE_SIZE = 256;
    private static final int OVERLAY_X = 20;
    private static final int OVERLAY_Y = 18;
    private static final int OVERLAY_W = 136;
    private static final int OVERLAY_H = 36;
    private static final int OVERLAY_INNER = 0xFF8B8B8B;
    private static final int OVERLAY_SHADOW = 0xFF373737;
    private static final int OVERLAY_HIGHLIGHT = 0xFFFFFFFF;
    private static final int LABEL_COLOR = 0xFF404040;
    private static final int LINE_GAP = 13;

    private final PanelHost panelHost = new PanelHost();
    @Nullable
    private MachineRedstonePanel redstonePanel;

    public TransferEnergyScreen(TransferEnergyMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, TransferEnergyMenu.IMAGE_WIDTH, TransferEnergyMenu.IMAGE_HEIGHT);
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
        panelHost.add(new ScrollingInfoPanel(
                "gui.dopasrandomutilities.panel.info.transfer_node_energy.intro",
                "gui.dopasrandomutilities.panel.info.transfer_node_energy.speed",
                "gui.dopasrandomutilities.panel.info.transfer_node_energy.upgrades"
        ));
        panelHost.add(new MachineUpgradePanel(menu.getUpgradeSlots(), PanelAnchor.RIGHT_TOP, TransferEnergyMenu.TAB_Y_BIAS));
        redstonePanel = new MachineRedstonePanel(this, PanelAnchor.RIGHT_BELOW, TransferEnergyMenu.TAB_Y_BIAS);
        panelHost.add(redstonePanel);
        redstonePanel.initWidgets();
        panelHost.layoutWidgets(leftPos, topPos, imageWidth);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        panelHost.layoutWidgets(leftPos, topPos, imageWidth);
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
        renderTextOverlay(graphics);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractContents(graphics, mouseX, mouseY, partialTick);
        Component lastPulled = Component.translatable(
                "gui.dopasrandomutilities.transfer_node_energy.last_pulled",
                menu.lastEnergyPulled()
        );
        Component extraction = Component.translatable(
                "gui.dopasrandomutilities.transfer_node_energy.extraction",
                menu.energyPullRate(),
                UpgradeConfig.transferNodeInterval(HeadKind.ENERGY, menu.upgrades().overclockCount())
        );
        int overlayLeft = leftPos + OVERLAY_X;
        int overlayTop = topPos + OVERLAY_Y;
        int textY = overlayTop + 6;
        graphics.text(font, lastPulled, overlayLeft + (OVERLAY_W - font.width(lastPulled)) / 2, textY, LABEL_COLOR, false);
        graphics.text(
                font,
                extraction,
                overlayLeft + (OVERLAY_W - font.width(extraction)) / 2,
                textY + LINE_GAP,
                LABEL_COLOR,
                false
        );
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
                menu.upgrades()
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

    private void renderTextOverlay(GuiGraphicsExtractor graphics) {
        int x = leftPos + OVERLAY_X;
        int y = topPos + OVERLAY_Y;
        int x2 = x + OVERLAY_W;
        int y2 = y + OVERLAY_H;
        graphics.fill(x, y, x2, y2, OVERLAY_INNER);
        graphics.fill(x, y, x2, y + 1, OVERLAY_SHADOW);
        graphics.fill(x, y, x + 1, y2, OVERLAY_SHADOW);
        graphics.fill(x2 - 1, y, x2, y2, OVERLAY_HIGHLIGHT);
        graphics.fill(x, y2 - 1, x2, y2, OVERLAY_HIGHLIGHT);
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
