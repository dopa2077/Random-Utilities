package com.dopa.randomutilities.logistics.transfer;

import net.minecraft.util.StringRepresentable;

public enum TransferPipeFace implements StringRepresentable {
    NONE("none"),
    PIPE("pipe"),
    INVENTORY("inventory"),
    DISABLED("disabled");

    private final String name;

    TransferPipeFace(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public boolean hasArm() {
        return this == PIPE || this == INVENTORY;
    }

    public boolean shortened() {
        return this == INVENTORY;
    }
}
