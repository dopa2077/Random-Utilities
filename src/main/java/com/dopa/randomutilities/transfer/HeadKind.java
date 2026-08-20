package com.dopa.randomutilities.transfer;

import net.minecraft.util.StringRepresentable;

public enum HeadKind implements StringRepresentable {
    ITEM("item"),
    FLUID("fluid"),
    ENERGY("energy");

    private final String name;

    HeadKind(String name) {
        this.name = name;
    }

    public static HeadKind byOrdinal(int ordinal) {
        HeadKind[] values = values();
        if (ordinal < 0 || ordinal >= values.length) {
            return ITEM;
        }
        return values[ordinal];
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
