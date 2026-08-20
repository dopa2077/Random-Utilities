package com.dopa.randomutilities.trashcan.client;

import com.dopa.randomutilities.gui.widget.FilterModeButton;
import com.dopa.randomutilities.gui.widget.FilterModeIcon;
import com.dopa.randomutilities.gui.widget.FilterRow;
import com.dopa.randomutilities.gui.widget.JeiGhostDragState;
import com.dopa.randomutilities.trashcan.TrashCanMenu;
import com.dopa.randomutilities.trashcan.network.TrashCanSettingPayload;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public class TrashCanScreen extends AbstractContainerScreen<TrashCanMenu> {
    private static final Identifier BACKGROUND =
            Identifier.fromNamespaceAndPath("dopasrandomutilities", "textures/gui/special/trash_can.png");
    private static final int TEXTURE_SIZE = 256;

    private FilterModeButton modeButton;

    public TrashCanScreen(TrashCanMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title, TrashCanMenu.IMAGE_WIDTH, TrashCanMenu.IMAGE_HEIGHT);
        this.inventoryLabelY = this.imageHeight - 94;
        this.titleLabelY = 6;
    }

    public int leftPos() {
        return leftPos;
    }

    public int topPos() {
        return topPos;
    }

    @Override
    protected void init() {
        super.init();
        modeButton = new FilterModeButton(
                leftPos + TrashCanMenu.FILTER_ICON_X,
                topPos + TrashCanMenu.FILTER_SLOT_Y,
                filterModeTooltip(),
                this::toggleFilterMode
        );
        addRenderableWidget(modeButton);
    }

    private void toggleFilterMode() {
        boolean next = !menu.isWhitelistMode();
        ClientPacketDistributor.sendToServer(new TrashCanSettingPayload(next));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
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
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                BACKGROUND,
                this.leftPos,
                this.topPos,
                0.0F,
                0.0F,
                this.imageWidth,
                this.imageHeight,
                TEXTURE_SIZE,
                TEXTURE_SIZE
        );
        FilterRow.blit(
                graphics,
                this.leftPos + TrashCanMenu.FILTER_ICON_X,
                this.topPos + TrashCanMenu.FILTER_SLOT_Y,
                TrashCanMenu.FILTER_SLOT_COUNT,
                i -> this.menu.slots.get(1 + i).hasItem()
        );
    }

    private Component filterModeTooltip() {
        return Component.translatable(menu.isWhitelistMode()
                ? "gui.dopasrandomutilities.item_collector.whitelist"
                : "gui.dopasrandomutilities.item_collector.blacklist");
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractContents(graphics, mouseX, mouseY, partialTick);
        renderFilterModeIcon(graphics);
        renderGhostSlotTints(graphics);
        JeiGhostDragState.renderLine(graphics, mouseX, mouseY);
        if (hoveredSlot != null && menu.isInputSlot(hoveredSlot) && !hoveredSlot.hasItem()) {
            graphics.setTooltipForNextFrame(
                    font,
                    Component.translatable("gui.dopasrandomutilities.trash_can.input"),
                    mouseX,
                    mouseY
            );
            return;
        }
        FilterRow.applyEmptyHover(graphics, font, mouseX, mouseY, hoveredSlot);
    }

    private void renderFilterModeIcon(GuiGraphicsExtractor graphics) {
        FilterModeIcon.render(
                graphics,
                menu.isWhitelistMode(),
                leftPos + TrashCanMenu.FILTER_ICON_X,
                topPos + TrashCanMenu.FILTER_SLOT_Y,
                modeButton != null && modeButton.isHovered()
        );
    }

    private void renderGhostSlotTints(GuiGraphicsExtractor graphics) {
        for (int i = 0; i < TrashCanMenu.FILTER_SLOT_COUNT; i++) {
            Slot slot = menu.slots.get(1 + i);
            if (!slot.hasItem()) {
                continue;
            }
            int x = leftPos + slot.x;
            int y = topPos + slot.y;
            graphics.fill(x, y, x + 16, y + 16, 0x40FFFFFF);
        }
    }
}
