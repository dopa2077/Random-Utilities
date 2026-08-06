package com.dopa.randomutilities.minichest.client;

import com.dopa.randomutilities.minichest.MiniChestMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class MiniChestScreen extends AbstractContainerScreen<MiniChestMenu> {
    private static final Identifier CHEST_BACKGROUND =
            Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");
    private static final int TEXTURE_SIZE = 256;
    private static final int BODY_COLOR = 0xFFC6C6C6;
    private static final int FOOTER_Y = 35;
    private static final int PLAYER_INV_HEIGHT = 96;
    private static final int IMAGE_HEIGHT = 114 + 18;
    private static final int SLOT = 18;

    public MiniChestScreen(MiniChestMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title, 176, IMAGE_HEIGHT);
        this.inventoryLabelY = this.imageHeight - 94;
        this.titleLabelY = 6;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int xo = this.leftPos;
        int yo = this.topPos;

        // Title strip from chest GUI (avoid baking in a full 9-slot row).
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

        int frameX = xo + MiniChestMenu.CHEST_SLOT_X + 8 - SLOT / 2;
        int frameY = yo + MiniChestMenu.CHEST_SLOT_Y + 8 - SLOT / 2;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, frameX, frameY, SLOT, SLOT);
    }
}
