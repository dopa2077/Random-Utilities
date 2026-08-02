package com.dopa.randomutilities.filteritem.client.panel;

/**
 * Where an {@link AttachedPanel} tab sits relative to the main inventory window.
 * Stack index 0 = top, 1 = below, 2 = low (third in the column).
 */
public enum PanelAnchor {
    LEFT_TOP(0, true),
    LEFT_BELOW(1, true),
    RIGHT_TOP(0, false),
    RIGHT_BELOW(1, false),
    RIGHT_LOW(2, false);

    private final int stackIndex;
    private final boolean left;

    PanelAnchor(int stackIndex, boolean left) {
        this.stackIndex = stackIndex;
        this.left = left;
    }

    public int stackIndex() {
        return stackIndex;
    }

    public boolean isLeft() {
        return left;
    }
}
