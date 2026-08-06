package com.dopa.randomutilities.compat.jei;

import com.dopa.randomutilities.itemcollector.client.ItemCollectorJeiDragState;
import com.dopa.randomutilities.itemcollector.client.ItemCollectorScreen;
import com.dopa.randomutilities.itemcollector.menu.ItemCollectorMenu;
import com.dopa.randomutilities.itemcollector.network.ItemCollectorFilterPayload;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.List;

public final class ItemCollectorJeiHandler implements IGhostIngredientHandler<ItemCollectorScreen> {
    @Override
    public <I> List<Target<I>> getTargetsTyped(ItemCollectorScreen gui, ITypedIngredient<I> ingredient, boolean doStart) {
        if (!ingredient.getType().equals(VanillaTypes.ITEM_STACK)) {
            return List.of();
        }
        I raw = ingredient.getIngredient();
        if (!(raw instanceof ItemStack stack) || stack.isEmpty()) {
            return List.of();
        }
        if (doStart) {
            ItemCollectorJeiDragState.beginDrag();
        }
        int slotCount = gui.getMenu().collectorType().filterSlotCount();
        int slotX = ItemCollectorMenu.filterSlotX(gui.getMenu().collectorType());
        List<Target<I>> targets = new ArrayList<>(slotCount);
        for (int i = 0; i < slotCount; i++) {
            int slotIndex = i;
            int x = gui.leftPos() + slotX + i * 18;
            int y = gui.topPos() + ItemCollectorMenu.FILTER_SLOT_Y;
            targets.add(new Target<>() {
                @Override
                public Rect2i getArea() {
                    return new Rect2i(x, y, 16, 16);
                }

                @Override
                public void accept(I ingredient) {
                    if (ingredient instanceof ItemStack itemStack && !itemStack.isEmpty()) {
                        ClientPacketDistributor.sendToServer(new ItemCollectorFilterPayload(
                                slotIndex,
                                itemStack.copyWithCount(1)
                        ));
                    }
                }
            });
        }
        return targets;
    }

    @Override
    public void onComplete() {
        ItemCollectorJeiDragState.endDrag();
    }
}
