package com.dopa.randomutilities.integration.jei;

import com.dopa.randomutilities.core.gui.widget.FilterModeIcon;
import com.dopa.randomutilities.core.gui.widget.FilterRow;
import com.dopa.randomutilities.core.gui.widget.JeiGhostDragState;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.ToIntFunction;

/**
 * JEI ghost-ingredient drop onto filter wells.
 * Slot X/Y functions return the screen-space origin of slot 0.
 * Default layout is a horizontal row at {@link FilterRow} pitch; pass columns/pitch for a grid.
 */
public final class FilterGhostJeiHandler<T extends AbstractContainerScreen<?>> implements IGhostIngredientHandler<T> {
    private final ToIntFunction<T> slotCount;
    private final ToIntFunction<T> slotX;
    private final ToIntFunction<T> slotY;
    private final BiConsumer<Integer, ItemStack> send;
    private final int columns;
    private final int pitch;

    public FilterGhostJeiHandler(
            ToIntFunction<T> slotCount,
            ToIntFunction<T> slotX,
            ToIntFunction<T> slotY,
            BiConsumer<Integer, ItemStack> send
    ) {
        this(slotCount, slotX, slotY, send, 0, FilterRow.WELL);
    }

    public FilterGhostJeiHandler(
            ToIntFunction<T> slotCount,
            ToIntFunction<T> slotX,
            ToIntFunction<T> slotY,
            BiConsumer<Integer, ItemStack> send,
            int columns,
            int pitch
    ) {
        this.slotCount = slotCount;
        this.slotX = slotX;
        this.slotY = slotY;
        this.send = send;
        this.columns = columns;
        this.pitch = pitch;
    }

    @Override
    public <I> List<Target<I>> getTargetsTyped(T gui, ITypedIngredient<I> ingredient, boolean doStart) {
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
        int count = slotCount.applyAsInt(gui);
        int cols = columns > 0 ? columns : Math.max(1, count);
        int originX = slotX.applyAsInt(gui);
        int originY = slotY.applyAsInt(gui);
        List<Target<I>> targets = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int slotIndex = i;
            int x = originX + (i % cols) * pitch;
            int y = originY + (i / cols) * pitch;
            targets.add(new Target<>() {
                @Override
                public Rect2i getArea() {
                    return new Rect2i(x, y, FilterModeIcon.SIZE, FilterModeIcon.SIZE);
                }

                @Override
                public void accept(I ingredient) {
                    if (ingredient instanceof ItemStack itemStack && !itemStack.isEmpty()) {
                        send.accept(slotIndex, itemStack.copyWithCount(1));
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
