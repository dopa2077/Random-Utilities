package com.dopa.randomutilities.magnet.client;

import com.dopa.randomutilities.filter.client.GhostFilterClicks;
import com.dopa.randomutilities.gui.machine.MachineUpgradePanel;
import com.dopa.randomutilities.gui.machine.UpgradeSlotTooltips;
import com.dopa.randomutilities.gui.panel.PanelAnchor;
import com.dopa.randomutilities.gui.panel.PanelHost;
import com.dopa.randomutilities.gui.panel.ScrollingInfoPanel;
import com.dopa.randomutilities.gui.widget.FilterModeButton;
import com.dopa.randomutilities.gui.widget.FilterModeIcon;
import com.dopa.randomutilities.gui.widget.FilterRow;
import com.dopa.randomutilities.gui.widget.JeiGhostDragState;
import com.dopa.randomutilities.magnet.client.panel.MagnetConfigPanel;
import com.dopa.randomutilities.magnet.client.panel.MagnetCosmeticPanel;
import com.dopa.randomutilities.magnet.menu.MagnetMenu;
import com.dopa.randomutilities.magnet.network.MagnetSettingPayload;

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

public class MagnetScreen extends AbstractContainerScreen<MagnetMenu> {
    private static final Identifier BACKGROUND =
            Identifier.fromNamespaceAndPath("dopasrandomutilities", "textures/gui/special/mini_chest.png");
    private static final int TEXTURE_SIZE = 256;
    /** Mini chest center well — unused; covered so only the filter row reads as slots. */
    private static final int CENTER_SLOT_X = 80;
    private static final int CENTER_SLOT_Y = 20;
    private static final int PANEL_BODY_COLOR = 0xFFC6C6C6;

    private final PanelHost panelHost = new PanelHost();
    @Nullable
    private MagnetConfigPanel configPanel;
    @Nullable
    private MagnetCosmeticPanel cosmeticPanel;
    @Nullable
    private FilterModeButton modeButton;
    private boolean lastWhitelistMode;

    public MagnetScreen(MagnetMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, MagnetMenu.IMAGE_WIDTH, MagnetMenu.IMAGE_HEIGHT);
        this.inventoryLabelY = this.imageHeight - 94;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
    }

    public MagnetMenu getMenu() {
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

    public <T extends GuiEventListener & Renderable & NarratableEntry> T addOverlayWidget(T widget) {
        return addRenderableWidget(widget);
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

    @Override
    protected void init() {
        super.init();
        panelHost.clear();
        configPanel = null;
        cosmeticPanel = null;

        panelHost.add(new ScrollingInfoPanel(
                "gui.dopasrandomutilities.panel.info.item_magnet.intro",
                "gui.dopasrandomutilities.panel.info.item_magnet.usage"
        ));

        configPanel = new MagnetConfigPanel(this);
        panelHost.add(configPanel);
        configPanel.initWidgets();

        cosmeticPanel = new MagnetCosmeticPanel(this);
        panelHost.add(cosmeticPanel);
        cosmeticPanel.initWidgets();

        panelHost.add(new MachineUpgradePanel(menu.getUpgradeSlots(), PanelAnchor.RIGHT_TOP, 0));
        panelHost.layoutWidgets(leftPos, topPos, imageWidth);

        lastWhitelistMode = menu.isWhitelistMode();
        modeButton = new FilterModeButton(
                leftPos + MagnetMenu.ICON_X,
                topPos + MagnetMenu.FILTER_SLOT_Y,
                filterModeTooltip(),
                this::toggleFilterMode
        );
        addRenderableWidget(modeButton);
    }

    private void toggleFilterMode() {
        boolean next = !menu.isWhitelistMode();
        ClientPacketDistributor.sendToServer(new MagnetSettingPayload(
                MagnetSettingPayload.KIND_FILTER_MODE,
                next ? 1 : 0
        ));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        panelHost.layoutWidgets(leftPos, topPos, imageWidth);
        if (modeButton != null) {
            boolean whitelist = menu.isWhitelistMode();
            if (whitelist != lastWhitelistMode) {
                lastWhitelistMode = whitelist;
                modeButton.updateTooltip(filterModeTooltip());
            }
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
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (panelHost.mouseDragged(event.x(), event.y())) {
            return true;
        }
        GhostFilterClicks.onMouseDragged(event);
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public void onClose() {
        GhostFilterClicks.reset();
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
        graphics.fill(
                leftPos + CENTER_SLOT_X - 1,
                topPos + CENTER_SLOT_Y - 1,
                leftPos + CENTER_SLOT_X + 17,
                topPos + CENTER_SLOT_Y + 17,
                PANEL_BODY_COLOR
        );

        int filterStart = menu.filterSlotStart();
        FilterRow.blit(
                graphics,
                leftPos + MagnetMenu.ICON_X,
                topPos + MagnetMenu.FILTER_SLOT_Y,
                MagnetMenu.FILTER_SLOT_COUNT,
                i -> menu.slots.get(filterStart + i).hasItem()
        );
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractContents(graphics, mouseX, mouseY, partialTick);
        int filterStart = menu.filterSlotStart();
        for (int i = 0; i < MagnetMenu.FILTER_SLOT_COUNT; i++) {
            FilterRow.tintGhostItem(graphics, leftPos, topPos, menu.slots.get(filterStart + i));
        }
        renderFilterModeIcon(graphics);
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
                menu.upgrades()
        );
        FilterRow.applyEmptyHover(graphics, font, mouseX, mouseY, hoveredSlot);
    }

    private void renderFilterModeIcon(GuiGraphicsExtractor graphics) {
        boolean hovered = modeButton != null && modeButton.isHovered();
        FilterModeIcon.render(
                graphics,
                menu.isWhitelistMode(),
                leftPos + MagnetMenu.ICON_X,
                topPos + MagnetMenu.FILTER_SLOT_Y,
                hovered
        );
    }

    private Component filterModeTooltip() {
        return Component.translatable(menu.isWhitelistMode()
                ? "gui.dopasrandomutilities.item_magnet.whitelist"
                : "gui.dopasrandomutilities.item_magnet.blacklist");
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
        boolean overCosmeticControl = cosmeticPanel != null
                && cosmeticPanel.contentsInteractive()
                && cosmeticPanel.isMouseOverInteractiveWidget(event.x(), event.y());
        if (overBody || overConfigControl || overCosmeticControl) {
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
