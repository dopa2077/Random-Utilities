package com.dopa.randomutilities.filteritem.client;

import com.dopa.randomutilities.filteritem.FilterStorage;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.IItemDecorator;

public final class FilterDecorator implements IItemDecorator {
    public static final FilterDecorator INSTANCE = new FilterDecorator();

    private static final int INSET = 3;

    private FilterDecorator() {}

    @Override
    public boolean render(GuiGraphicsExtractor graphics, Font font, ItemStack stack, int xOffset, int yOffset) {
        ItemStack preview = FilterStorage.getPreviewIconStack(stack);
        if (preview.isEmpty()) {
            return false;
        }
        graphics.pose().pushMatrix();
        graphics.pose().translate(xOffset + INSET, yOffset + INSET);
        float scale = (16 - INSET * 2) / 16.0F;
        graphics.pose().scale(scale, scale);
        graphics.item(preview, 0, 0, xOffset ^ yOffset);
        graphics.pose().popMatrix();
        return true;
    }
}
