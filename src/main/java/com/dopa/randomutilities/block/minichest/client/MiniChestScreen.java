package com.dopa.randomutilities.block.minichest.client;

import com.dopa.randomutilities.block.minichest.MiniChestMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class MiniChestScreen extends AbstractContainerScreen<MiniChestMenu> {
    private static final Identifier BACKGROUND =
            Identifier.fromNamespaceAndPath("dopasrandomutilities", "textures/gui/special/mini_chest.png");
    private static final int TEXTURE_SIZE = 256;
    private static final int IMAGE_HEIGHT = 133;

    public MiniChestScreen(MiniChestMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title, 176, IMAGE_HEIGHT);
        this.inventoryLabelY = this.imageHeight - 94;
        this.titleLabelY = 6;
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
    }
}
