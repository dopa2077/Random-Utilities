package com.dopa.randomutilities.filter.client;

import com.dopa.randomutilities.filter.menu.GhostFilterSlot;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;

/** Blocks vanilla drag-fill on ghost filter wells (left quick-craft and right-click drag). */
public final class GhostFilterClicks {
    private static boolean rightDragActive;

    private GhostFilterClicks() {}

    public static void onMouseDragged(MouseButtonEvent event) {
        if (event.buttonInfo().button() == InputConstants.MOUSE_BUTTON_RIGHT) {
            rightDragActive = true;
        }
    }

    public static void clearRightDrag() {
        rightDragActive = false;
    }

    public static void reset() {
        clearRightDrag();
    }

    public static boolean blockDrag(Slot slot, int mouseButton, ContainerInput type) {
        if (!(slot instanceof GhostFilterSlot)) {
            return false;
        }
        if (type == ContainerInput.QUICK_CRAFT) {
            return true;
        }
        return rightDragActive && mouseButton == 1 && type == ContainerInput.PICKUP;
    }
}
