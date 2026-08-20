package com.dopa.randomutilities.transfer.client;

import com.dopa.randomutilities.gui.widget.FilterRow;
import com.dopa.randomutilities.gui.widget.JeiGhostDragState;
import com.dopa.randomutilities.transfer.menu.TransferFilterMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class TransferFilterScreen extends AbstractContainerScreen<TransferFilterMenu> {
    private static final Identifier BACKGROUND =
            Identifier.fromNamespaceAndPath("dopasrandomutilities", "textures/gui/special/filter.png");
    private static final int TEXTURE_SIZE = 256;
    private static final int LABEL_COLOR = 0xFF404040;
    /** Inner 16×16 of the packed red-guide well (plain slot, no paper). */
    private static final int OCCUPIED_U = 177;
    private static final int OCCUPIED_V = 1;
    private static final int BUTTON_X = 96;
    private static final int BUTTON_WIDTH = 72;
    /** Three buttons + two gaps fill the 64px 4×4 grid height. */
    private static final int BUTTON_HEIGHT = 18;
    private static final int BUTTON_GAP = 5;

    private Button nbtButton;
    private Button metaButton;
    private Button oreDictButton;

    public TransferFilterScreen(TransferFilterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, TransferFilterMenu.IMAGE_WIDTH, TransferFilterMenu.IMAGE_HEIGHT);
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
        int x = leftPos + BUTTON_X;
        int y = topPos + TransferFilterMenu.GRID_Y;
        int step = BUTTON_HEIGHT + BUTTON_GAP;
        nbtButton = toggleButton(x, y, nbtLabel(), TransferFilterMenu.BTN_NBT);
        metaButton = toggleButton(x, y + step, metaLabel(), TransferFilterMenu.BTN_META);
        oreDictButton = toggleButton(x, y + step * 2, oreDictLabel(), TransferFilterMenu.BTN_ORE_DICT);
    }

    private Button toggleButton(int x, int y, Component label, int buttonId) {
        Button button = Button.builder(label, b -> click(buttonId))
                .bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();
        // Hover/press only — do not keep the highlighted sprite while focused after click.
        button.setOverrideRenderHighlightedSprite(button::isHovered);
        return addRenderableWidget(button);
    }

    private void click(int buttonId) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (nbtButton != null) {
            nbtButton.setMessage(nbtLabel());
        }
        if (metaButton != null) {
            metaButton.setMessage(metaLabel());
        }
        if (oreDictButton != null) {
            oreDictButton.setMessage(oreDictLabel());
        }
    }

    private Component nbtLabel() {
        return Component.translatable(menu.matchNbt()
                ? "gui.dopasrandomutilities.filter.match_nbt"
                : "gui.dopasrandomutilities.filter.ignore_nbt");
    }

    private Component metaLabel() {
        return Component.translatable(menu.matchMeta()
                ? "gui.dopasrandomutilities.filter.match_meta"
                : "gui.dopasrandomutilities.filter.ignore_meta");
    }

    private Component oreDictLabel() {
        return Component.translatable(menu.matchOreDict()
                ? "gui.dopasrandomutilities.filter.match_ore_dict"
                : "gui.dopasrandomutilities.filter.ignore_ore_dict");
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
                leftPos,
                topPos,
                0.0F,
                0.0F,
                imageWidth,
                imageHeight,
                TEXTURE_SIZE,
                TEXTURE_SIZE
        );
        blitOccupiedWells(graphics);
    }

    private void blitOccupiedWells(GuiGraphicsExtractor graphics) {
        for (int i = 0; i < TransferFilterMenu.SLOT_COUNT; i++) {
            Slot slot = menu.slots.get(i);
            if (!slot.hasItem()) {
                continue;
            }
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    BACKGROUND,
                    leftPos + slot.x,
                    topPos + slot.y,
                    (float) OCCUPIED_U,
                    (float) OCCUPIED_V,
                    TransferFilterMenu.SLOT,
                    TransferFilterMenu.SLOT,
                    TEXTURE_SIZE,
                    TEXTURE_SIZE
            );
        }
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractContents(graphics, mouseX, mouseY, partialTick);
        for (int i = 0; i < TransferFilterMenu.SLOT_COUNT; i++) {
            FilterRow.tintGhostItem(graphics, leftPos, topPos, menu.slots.get(i));
        }
        JeiGhostDragState.renderLine(graphics, mouseX, mouseY);
        FilterRow.applyEmptyHover(graphics, font, mouseX, mouseY, hoveredSlot);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, (imageWidth - font.width(title)) / 2, titleLabelY, LABEL_COLOR, false);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, LABEL_COLOR, false);
    }
}
