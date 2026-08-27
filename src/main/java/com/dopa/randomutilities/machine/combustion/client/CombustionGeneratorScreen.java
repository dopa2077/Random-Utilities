package com.dopa.randomutilities.machine.combustion.client;

import com.dopa.randomutilities.machine.combustion.menu.CombustionGeneratorMenu;
import com.dopa.randomutilities.core.gui.machine.EnergyUpgradeRemoveConfirm;
import com.dopa.randomutilities.core.gui.machine.MachineEnergyBar;
import com.dopa.randomutilities.core.gui.machine.MachineEnergyPanel;
import com.dopa.randomutilities.core.gui.machine.MachineRedstonePanel;
import com.dopa.randomutilities.core.gui.machine.MachineUpgradePanel;
import com.dopa.randomutilities.core.gui.machine.UpgradeSlotTooltips;
import com.dopa.randomutilities.core.gui.panel.PanelAnchor;
import com.dopa.randomutilities.core.gui.panel.PanelHost;
import com.dopa.randomutilities.core.gui.panel.ScrollingInfoPanel;
import com.dopa.randomutilities.core.machine.RedstoneMode;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class CombustionGeneratorScreen extends AbstractContainerScreen<CombustionGeneratorMenu>
        implements MachineRedstonePanel.Host, MachineEnergyPanel.Host {
    private static final Identifier BACKGROUND =
            Identifier.fromNamespaceAndPath("dopasrandomutilities", "textures/gui/machine/basic_generator.png");
    private static final int TEXTURE_SIZE = 256;
    private static final int LABEL_COLOR = 0xFF404040;

    private final PanelHost panelHost = new PanelHost();
    private final int tabYBias = CombustionGeneratorMenu.TAB_Y_BIAS;
    private final EnergyUpgradeRemoveConfirm energyRemoveConfirm = new EnergyUpgradeRemoveConfirm();

    @Nullable
    private MachineRedstonePanel redstonePanel;

    public CombustionGeneratorScreen(CombustionGeneratorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        this.inventoryLabelY = this.imageHeight - 94;
        this.titleLabelY = 6;
    }

    public PanelHost getPanelHost() {
        return this.panelHost;
    }

    public int leftPos() {
        return this.leftPos;
    }

    public int topPos() {
        return this.topPos;
    }

    public int imageWidth() {
        return this.imageWidth;
    }

    @Override
    public <T extends GuiEventListener & net.minecraft.client.gui.components.Renderable
            & net.minecraft.client.gui.narration.NarratableEntry> T addOverlayWidget(T widget) {
        return this.addRenderableWidget(widget);
    }

    @Override
    public Supplier<RedstoneMode> redstoneMode() {
        return () -> this.menu.redstoneMode();
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
        return menu.energyMaxOut();
    }

    @Override
    protected void init() {
        super.init();
        this.panelHost.clear();
        this.redstonePanel = null;

        this.panelHost.add(new ScrollingInfoPanel(
                tabYBias,
                "gui.dopasrandomutilities.panel.info.combustion_generator.intro",
                "gui.dopasrandomutilities.panel.info.combustion_generator.fuel"
        ));
        this.panelHost.add(new MachineEnergyPanel(this));
        this.panelHost.add(new MachineUpgradePanel(
                this.menu.getUpgradeSlots(), PanelAnchor.RIGHT_BELOW, tabYBias));
        this.redstonePanel = new MachineRedstonePanel(this, PanelAnchor.RIGHT_LOW, tabYBias);
        this.panelHost.add(this.redstonePanel);
        this.redstonePanel.initWidgets();
        this.panelHost.layoutWidgets(this.leftPos, this.topPos, this.imageWidth);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        this.panelHost.layoutWidgets(this.leftPos, this.topPos, this.imageWidth);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        this.panelHost.render(graphics, this.font, this.leftPos, this.topPos, this.imageWidth,
                mouseX, mouseY, partialTick);

        int xo = this.leftPos;
        int yo = this.topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, xo, yo, 0.0F, 0.0F,
                this.imageWidth, this.imageHeight, TEXTURE_SIZE, TEXTURE_SIZE);

        MachineEnergyBar.render(
                graphics,
                xo + CombustionGeneratorMenu.ENERGY_BAR_X,
                yo + CombustionGeneratorMenu.ENERGY_BAR_Y,
                CombustionGeneratorMenu.ENERGY_BAR_W,
                CombustionGeneratorMenu.ENERGY_BAR_H,
                menu.energyStored(),
                menu.energyCapacity()
        );

        float burn = menu.burnFraction();
        if (burn > 0.0F) {
            int h = CombustionGeneratorMenu.BURN_H;
            int fillH = Math.max(1, Mth.ceil(burn * h));
            int v = h - fillH;
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    BACKGROUND,
                    xo + CombustionGeneratorMenu.BURN_X,
                    yo + CombustionGeneratorMenu.BURN_Y + v,
                    (float) CombustionGeneratorMenu.BURN_TEX_U,
                    (float) (CombustionGeneratorMenu.BURN_TEX_V + v),
                    CombustionGeneratorMenu.BURN_W,
                    fillH,
                    TEXTURE_SIZE,
                    TEXTURE_SIZE
            );
        }
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractContents(graphics, mouseX, mouseY, partialTick);
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
                menu.upgrades(),
                energyRemoveConfirm.pendingFor(hoveredSlot, menu.upgrades(), menu.energyStored())
        )) {
            return;
        }
        if (MachineEnergyBar.isHover(
                mouseX,
                mouseY,
                leftPos + CombustionGeneratorMenu.ENERGY_BAR_X,
                topPos + CombustionGeneratorMenu.ENERGY_BAR_Y,
                CombustionGeneratorMenu.ENERGY_BAR_W,
                CombustionGeneratorMenu.ENERGY_BAR_H
        )) {
            MachineEnergyBar.renderHoverTooltip(
                    graphics, font, mouseX, mouseY, menu.energyStored(), menu.energyCapacity());
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, (imageWidth - font.width(title)) / 2, titleLabelY, LABEL_COLOR, false);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, LABEL_COLOR, false);
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
    protected void slotClicked(Slot slot, int slotIndex, int mouseButton, ContainerInput type) {
        if (energyRemoveConfirm.block(
                slot,
                mouseButton,
                type,
                menu.getCarried(),
                menu.upgrades(),
                menu.energyStored()
        )) {
            return;
        }
        super.slotClicked(slot, slotIndex, mouseButton, type);
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
