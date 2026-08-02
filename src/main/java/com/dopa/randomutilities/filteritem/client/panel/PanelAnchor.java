package com.dopa.randomutilities.filteritem.client.panel;

/**
 * Where an {@link AttachedPanel} tab sits relative to the main inventory window.
 */
public enum PanelAnchor {
    LEFT_TOP,
    LEFT_BELOW,
    RIGHT_TOP,
    RIGHT_BELOW;

    public boolean isLeft() {
        return this == LEFT_TOP || this == LEFT_BELOW;
    }

    public boolean isBelowSibling() {
        return this == LEFT_BELOW || this == RIGHT_BELOW;
    }
}
