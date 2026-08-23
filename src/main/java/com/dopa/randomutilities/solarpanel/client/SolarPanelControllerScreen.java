package com.dopa.randomutilities.solarpanel.client;

import com.dopa.randomutilities.client.WorkingVolumeOverlay;
import com.dopa.randomutilities.gui.machine.EnergyUpgradeRemoveConfirm;
import com.dopa.randomutilities.gui.machine.MachineEnergyBar;
import com.dopa.randomutilities.gui.machine.MachineEnergyPanel;
import com.dopa.randomutilities.gui.machine.MachineRedstonePanel;
import com.dopa.randomutilities.gui.machine.MachineUpgradePanel;
import com.dopa.randomutilities.gui.machine.UpgradeSlotTooltips;
import com.dopa.randomutilities.gui.panel.AttachedPanel;
import com.dopa.randomutilities.gui.panel.PanelAnchor;
import com.dopa.randomutilities.gui.panel.PanelHost;
import com.dopa.randomutilities.gui.panel.ScrollingInfoPanel;
import com.dopa.randomutilities.gui.widget.IconButton;
import com.dopa.randomutilities.machine.RedstoneMode;
import com.dopa.randomutilities.solarpanel.menu.SolarPanelControllerMenu;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class SolarPanelControllerScreen extends AbstractContainerScreen<SolarPanelControllerMenu>
        implements MachineRedstonePanel.Host, MachineEnergyPanel.Host {
    private static final Identifier BACKGROUND =
            Identifier.fromNamespaceAndPath("dopasrandomutilities", "textures/gui/machine/basic_generator.png");
    private static final Identifier RANGE_OVERLAY_ICON =
            Identifier.fromNamespaceAndPath("dopasrandomutilities", "textures/gui/widget/hitbox.png");
    private static final int OVERLAY_BUTTON_SIZE = 13;
    private static final int TEXTURE_SIZE = 256;
    private static final int LABEL_COLOR = 0xFF404040;

    private final PanelHost panelHost = new PanelHost();
    private final int tabYBias = SolarPanelControllerMenu.TAB_Y_BIAS;
    private final EnergyUpgradeRemoveConfirm energyRemoveConfirm = new EnergyUpgradeRemoveConfirm();

    @Nullable
    private MachineRedstonePanel redstonePanel;
    @Nullable
    private IconButton rangeOverlayButton;

    public SolarPanelControllerScreen(SolarPanelControllerMenu menu, Inventory inventory, Component title) {
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

    private boolean isRangeOverlayEnabled() {
        var level = menu.machineLevel();
        if (level == null) {
            return false;
        }
        return WorkingVolumeOverlay.isEnabled(level.dimension(), menu.machinePos());
    }

    @Override
    protected void init() {
        super.init();
        this.panelHost.clear();
        this.redstonePanel = null;

        this.panelHost.add(new ScrollingInfoPanel(
                tabYBias,
                "gui.dopasrandomutilities.panel.info.solar_panel_controller.intro",
                "gui.dopasrandomutilities.panel.info.solar_panel_controller.panels"
        ));
        this.panelHost.add(new SolarStatusPanel(tabYBias));
        this.panelHost.add(new MachineEnergyPanel(this));
        this.panelHost.add(new MachineUpgradePanel(
                this.menu.getUpgradeSlots(), PanelAnchor.RIGHT_BELOW, tabYBias));
        this.redstonePanel = new MachineRedstonePanel(this, PanelAnchor.RIGHT_LOW, tabYBias);
        this.panelHost.add(this.redstonePanel);
        this.redstonePanel.initWidgets();
        this.panelHost.layoutWidgets(this.leftPos, this.topPos, this.imageWidth);

        rangeOverlayButton = new IconButton(
                leftPos + imageWidth - OVERLAY_BUTTON_SIZE - 4,
                topPos + 4,
                OVERLAY_BUTTON_SIZE,
                RANGE_OVERLAY_ICON,
                rangeOverlayTooltip(),
                this::toggleRangeOverlay
        );
        addRenderableWidget(rangeOverlayButton);
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

    private void toggleRangeOverlay() {
        var level = menu.machineLevel();
        if (level == null) {
            return;
        }
        WorkingVolumeOverlay.toggle(level.dimension(), menu.machinePos());
        if (rangeOverlayButton != null) {
            rangeOverlayButton.updateTooltip(rangeOverlayTooltip());
        }
    }

    @Override
    public void containerTick() {
        super.containerTick();
        this.panelHost.layoutWidgets(this.leftPos, this.topPos, this.imageWidth);
        if (rangeOverlayButton != null) {
            rangeOverlayButton.updateTooltip(rangeOverlayTooltip());
        }
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
                xo + SolarPanelControllerMenu.ENERGY_BAR_X,
                yo + SolarPanelControllerMenu.ENERGY_BAR_Y,
                SolarPanelControllerMenu.ENERGY_BAR_W,
                SolarPanelControllerMenu.ENERGY_BAR_H,
                menu.energyStored(),
                menu.energyCapacity()
        );
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
                leftPos + SolarPanelControllerMenu.ENERGY_BAR_X,
                topPos + SolarPanelControllerMenu.ENERGY_BAR_Y,
                SolarPanelControllerMenu.ENERGY_BAR_W,
                SolarPanelControllerMenu.ENERGY_BAR_H
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

    /** Read-only status: linked panels, max range, sun strength. */
    private final class SolarStatusPanel extends AttachedPanel {
        private static final int BG = 0xFF1A4548;
        private static final ItemStack TAB_ICON = new ItemStack(Items.DAYLIGHT_DETECTOR);

        private final int tabYBias;

        private SolarStatusPanel(int tabYBias) {
            super(
                    PanelAnchor.LEFT_BELOW,
                    136,
                    136,
                    BG,
                    Component.translatable("gui.dopasrandomutilities.panel.status")
            );
            this.tabYBias = tabYBias;
        }

        @Override
        public int tabOffsetY() {
            return super.tabOffsetY() + tabYBias;
        }

        @Override
        protected void renderIcon(GuiGraphicsExtractor graphics, Font font, int centerX, int centerY) {
            graphics.item(TAB_ICON, centerX - 8, centerY - 8, centerX ^ centerY);
        }

        @Override
        protected void renderContents(
                GuiGraphicsExtractor graphics,
                Font font,
                int bodyX,
                int bodyY,
                int mouseX,
                int mouseY,
                float partialTick
        ) {
            renderTitleRow(graphics, font, bodyX, bodyY);
            drawLabel(graphics, font, Component.translatable("gui.dopasrandomutilities.solar_panel.linked"), bodyX, bodyY + 22);
            drawValue(graphics, font, Component.literal(String.valueOf(menu.linkedPanels())), bodyX, bodyY + 34);
            drawLabel(graphics, font, Component.translatable("gui.dopasrandomutilities.solar_panel.max_range"), bodyX, bodyY + 48);
            drawValue(graphics, font, Component.literal(String.valueOf(menu.maxRange())), bodyX, bodyY + 60);
            drawLabel(graphics, font, Component.translatable("gui.dopasrandomutilities.solar_panel.peak_fe"), bodyX, bodyY + 74);
            drawValue(
                    graphics,
                    font,
                    Component.translatable("gui.dopasrandomutilities.energy.fe_tick", menu.peakFePerTick()),
                    bodyX,
                    bodyY + 86
            );
            drawLabel(graphics, font, Component.translatable("gui.dopasrandomutilities.solar_panel.sun"), bodyX, bodyY + 100);
            drawValue(graphics, font, sunStatusLine(), bodyX, bodyY + 112);
        }

        private Component sunStatusLine() {
            return switch (menu.solarStatus()) {
                case WORKING -> Component.translatable(
                        "gui.dopasrandomutilities.solar_furnace.solar.working",
                        Math.round(menu.solarStrengthFraction() * 100.0F)
                );
                case NO_SKY -> Component.translatable("gui.dopasrandomutilities.solar_furnace.solar.no_sky");
                case NO_SUN -> Component.translatable("gui.dopasrandomutilities.solar_furnace.solar.no_sun");
            };
        }
    }
}
