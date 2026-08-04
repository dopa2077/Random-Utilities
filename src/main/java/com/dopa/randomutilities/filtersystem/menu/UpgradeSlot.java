package com.dopa.randomutilities.filtersystem.menu;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Placeholder upgrade slot. Rejects all items until real upgrades exist.
 */
public class UpgradeSlot extends Slot {
    public static final int COUNT = 6;
    public static final int COLS = 3;
    public static final int ROWS = 2;

    /** Relative to GUI origin when the upgrade panel is fully open (right side). */
    public static final int TITLE_GAP = 18;

    private boolean active;

    public UpgradeSlot(Container container, int index, int x, int y) {
        super(container, index, x, y);
    }

    public static int panelWidth() {
        return Math.max(AttachedPanelLayout.CONTENT_PAD * 2 + COLS * 18, 100);
    }

    public static int gridWidth() {
        return COLS * 18;
    }

    /** Body left edge relative to the inventory GUI (matches AttachedPanel bodyXOpen nudge). */
    public static int bodyOriginX() {
        return AttachedPanelLayout.GUI_WIDTH - 1;
    }

    public static int gridOriginX() {
        return bodyOriginX() + (panelWidth() - gridWidth()) / 2;
    }

    public static int gridOriginY() {
        return AttachedPanelLayout.BELOW_TAB_Y + AttachedPanelLayout.CONTENT_PAD + TITLE_GAP;
    }

    public static int slotX(int index) {
        return gridOriginX() + (index % COLS) * 18;
    }

    public static int slotY(int index) {
        return gridOriginY() + (index / COLS) * 18;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    @Override
    public boolean mayPickup(Player player) {
        return false;
    }

    /**
     * Layout constants shared with client panels without pulling client code into the menu.
     */
    public static final class AttachedPanelLayout {
        public static final int GUI_WIDTH = 176;
        public static final int TAB_SIZE = 22;
        public static final int TAB_GAP = 0;
        public static final int TOP_INSET = 4;
        public static final int CONTENT_PAD = 8;
        public static final int BELOW_TAB_Y = TOP_INSET + TAB_SIZE + TAB_GAP;

        private AttachedPanelLayout() {}
    }
}
