package com.dopa.randomutilities.logistics.transfer;

import net.minecraft.util.StringRepresentable;

public enum TransferNodeFace implements StringRepresentable {
    NONE("none"),
    PIPE("pipe"),
    INVENTORY("inventory"),
    DISABLED("disabled");

    private final String name;

    TransferNodeFace(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public boolean hasArm() {
        return this == PIPE || this == INVENTORY;
    }
}
