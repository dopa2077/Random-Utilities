package com.dopa.randomutilities.trashcan.client;

import com.dopa.randomutilities.client.gui.JeiGhostDragState;
import com.dopa.randomutilities.trashcan.TrashCanMenu;
import com.dopa.randomutilities.trashcan.network.TrashCanSettingPayload;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public class TrashCanScreen extends AbstractContainerScreen<TrashCanMenu> {
    private static final Identifier CHEST_BACKGROUND =
            Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");
    private static final Identifier BLACKLIST_ICON =
            Identifier.fromNamespaceAndPath("dopasrandomutilities", "textures/gui/blacklist_icon.png");
    private static final Identifier WHITELIST_ICON =
            Identifier.fromNamespaceAndPath("dopasrandomutilities", "textures/gui/whitelist_icon.png");
    private static final int TEXTURE_SIZE = 256;
    private static final int BODY_COLOR = 0xFFC6C6C6;
    private static final int FOOTER_Y = 57;
    private static final int PLAYER_INV_HEIGHT = 96;
    private static final int IMAGE_HEIGHT = FOOTER_Y + PLAYER_INV_HEIGHT + 1;
    private static final int SLOT = 18;
    private static final int ICON_SIZE = 16;

    private Button modeButton;

    public TrashCanScreen(TrashCanMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title, 176, IMAGE_HEIGHT);
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
        modeButton = Button.builder(Component.empty(), b -> toggleFilterMode())
                .bounds(leftPos + TrashCanMenu.FILTER_ICON_X, topPos + TrashCanMenu.FILTER_SLOT_Y, ICON_SIZE, ICON_SIZE)
                .tooltip(Tooltip.create(Component.translatable(
                        menu.isWhitelistMode()
                                ? "gui.dopasrandomutilities.item_collector.whitelist"
                                : "gui.dopasrandomutilities.item_collector.blacklist")))
                .build();
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
            modeButton.setTooltip(Tooltip.create(Component.translatable(
                    menu.isWhitelistMode()
                            ? "gui.dopasrandomutilities.item_collector.whitelist"
                            : "gui.dopasrandomutilities.item_collector.blacklist")));
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
        int xo = this.leftPos;
        int yo = this.topPos;

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                CHEST_BACKGROUND,
                xo,
                yo,
                0.0F,
                0.0F,
                this.imageWidth,
                FOOTER_Y,
                TEXTURE_SIZE,
                TEXTURE_SIZE
        );
        graphics.fill(xo + 7, yo + 17, xo + this.imageWidth - 7, yo + FOOTER_Y, BODY_COLOR);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                CHEST_BACKGROUND,
                xo,
                yo + FOOTER_Y,
                0.0F,
                126.0F,
                this.imageWidth,
                PLAYER_INV_HEIGHT,
                TEXTURE_SIZE,
                TEXTURE_SIZE
        );

        // Trash slot frame
        int frameX = xo + TrashCanMenu.CHEST_SLOT_X + 8 - SLOT / 2;
        int frameY = yo + TrashCanMenu.CHEST_SLOT_Y + 8 - SLOT / 2;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, frameX, frameY, SLOT, SLOT);

        // Filter slot frames (advanced collector layout)
        for (int i = 0; i < TrashCanMenu.FILTER_SLOT_COUNT; i++) {
            Slot slot = menu.slots.get(1 + i);
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, xo + slot.x, yo + slot.y, 16, 16);
        }
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractContents(graphics, mouseX, mouseY, partialTick);
        renderFilterModeIcon(graphics);
        renderGhostSlotTints(graphics);
        JeiGhostDragState.renderLine(graphics, mouseX, mouseY);
    }

    private void renderFilterModeIcon(GuiGraphicsExtractor graphics) {
        Identifier icon = menu.isWhitelistMode() ? WHITELIST_ICON : BLACKLIST_ICON;
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                icon,
                leftPos + TrashCanMenu.FILTER_ICON_X,
                topPos + TrashCanMenu.FILTER_SLOT_Y,
                0.0F,
                0.0F,
                ICON_SIZE,
                ICON_SIZE,
                ICON_SIZE,
                ICON_SIZE
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
