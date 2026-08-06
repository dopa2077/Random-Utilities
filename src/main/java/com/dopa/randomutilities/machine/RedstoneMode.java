package com.dopa.randomutilities.machine;

public enum RedstoneMode {
    IGNORE,
    LOW,
    HIGH;

    public static RedstoneMode byOrdinal(int ordinal) {
        RedstoneMode[] values = values();
        if (ordinal < 0 || ordinal >= values.length) {
            return IGNORE;
        }
        return values[ordinal];
    }

    public boolean allowsOperation(int signal) {
        return switch (this) {
            case IGNORE -> true;
            case LOW -> signal <= 0;
            case HIGH -> signal > 0;
        };
    }
}
