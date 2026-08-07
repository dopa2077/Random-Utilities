package com.dopa.randomutilities.compat.jei;

import com.dopa.randomutilities.client.gui.JeiGhostDragState;
import com.dopa.randomutilities.trashcan.TrashCanMenu;
import com.dopa.randomutilities.trashcan.client.TrashCanScreen;
import com.dopa.randomutilities.trashcan.network.TrashCanFilterPayload;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.List;

public final class TrashCanJeiHandler implements IGhostIngredientHandler<TrashCanScreen> {
    @Override
    public <I> List<Target<I>> getTargetsTyped(TrashCanScreen gui, ITypedIngredient<I> ingredient, boolean doStart) {
        if (!ingredient.getType().equals(VanillaTypes.ITEM_STACK)) {
            return List.of();
        }
        I raw = ingredient.getIngredient();
        if (!(raw instanceof ItemStack stack) || stack.isEmpty()) {
            return List.of();
        }
        if (doStart) {
            JeiGhostDragState.beginDrag();
        }
        List<Target<I>> targets = new ArrayList<>(TrashCanMenu.FILTER_SLOT_COUNT);
        for (int i = 0; i < TrashCanMenu.FILTER_SLOT_COUNT; i++) {
            int slotIndex = i;
            int x = gui.leftPos() + TrashCanMenu.FILTER_SLOT_X + i * 18;
            int y = gui.topPos() + TrashCanMenu.FILTER_SLOT_Y;
            targets.add(new Target<>() {
                @Override
                public Rect2i getArea() {
                    return new Rect2i(x, y, 16, 16);
                }

                @Override
                public void accept(I ingredient) {
                    if (ingredient instanceof ItemStack itemStack && !itemStack.isEmpty()) {
                        ClientPacketDistributor.sendToServer(new TrashCanFilterPayload(
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
        JeiGhostDragState.endDrag();
    }
}
