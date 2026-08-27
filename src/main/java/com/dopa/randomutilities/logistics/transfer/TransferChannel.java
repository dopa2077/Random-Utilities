package com.dopa.randomutilities.logistics.transfer;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.Nullable;

/** AE2-style pipe color. Uncolored connects to every color; dyed pipes only to the same dye or uncolored. */
public enum TransferChannel implements StringRepresentable {
    NONE(null, MapColor.STONE, 0xFF9A9A9A),
    WHITE(DyeColor.WHITE),
    ORANGE(DyeColor.ORANGE),
    MAGENTA(DyeColor.MAGENTA),
    LIGHT_BLUE(DyeColor.LIGHT_BLUE),
    YELLOW(DyeColor.YELLOW),
    LIME(DyeColor.LIME),
    PINK(DyeColor.PINK),
    GRAY(DyeColor.GRAY),
    LIGHT_GRAY(DyeColor.LIGHT_GRAY),
    CYAN(DyeColor.CYAN),
    PURPLE(DyeColor.PURPLE),
    BLUE(DyeColor.BLUE),
    BROWN(DyeColor.BROWN),
    GREEN(DyeColor.GREEN),
    RED(DyeColor.RED),
    BLACK(DyeColor.BLACK);

    private static final TransferChannel[] DYED;

    static {
        TransferChannel[] all = values();
        DYED = new TransferChannel[all.length - 1];
        System.arraycopy(all, 1, DYED, 0, DYED.length);
    }

    @Nullable
    private final DyeColor dye;
    private final MapColor mapColor;
    private final int tint;
    private final String name;

    TransferChannel(@Nullable DyeColor dye, MapColor mapColor, int tint) {
        this.dye = dye;
        this.mapColor = mapColor;
        this.tint = tint;
        this.name = dye == null ? "none" : dye.getSerializedName();
    }

    TransferChannel(DyeColor dye) {
        this(dye, dye.getMapColor(), overlayTint(dye));
    }

    /** Dye multiply color, lifted toward white so pipes read closer to concrete. */
    private static int overlayTint(DyeColor dye) {
        int color = dye.getTextureDiffuseColor() | 0xFF000000;
        float lift = switch (dye) {
            case BLACK, GRAY -> 0.08F;
            case WHITE, LIGHT_GRAY -> 0.12F;
            default -> 0.30F;
        };
        return lerpChannel(color, 0xFFFFFFFF, lift);
    }

    private static int lerpChannel(int from, int to, float t) {
        int r = lerpByte((from >> 16) & 0xFF, (to >> 16) & 0xFF, t);
        int g = lerpByte((from >> 8) & 0xFF, (to >> 8) & 0xFF, t);
        int b = lerpByte(from & 0xFF, to & 0xFF, t);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static int lerpByte(int from, int to, float t) {
        return from + Math.round((to - from) * t);
    }

    public static TransferChannel[] dyed() {
        return DYED;
    }

    public static TransferChannel fromDye(DyeColor dye) {
        for (TransferChannel channel : DYED) {
            if (channel.dye == dye) {
                return channel;
            }
        }
        return NONE;
    }

    public boolean connectsTo(TransferChannel other) {
        return this == NONE || other == NONE || this == other;
    }

    @Nullable
    public DyeColor dye() {
        return dye;
    }

    public MapColor mapColor() {
        return mapColor;
    }

    public int tint() {
        return tint;
    }

    public String blockId() {
        return this == NONE ? "transfer_pipe" : "transfer_pipe_" + name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
