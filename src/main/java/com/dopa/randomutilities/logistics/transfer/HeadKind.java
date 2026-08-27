package com.dopa.randomutilities.logistics.transfer;

import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

public enum HeadKind implements StringRepresentable {
    ITEM("item"),
    FLUID("fluid"),
    ENERGY("energy");

    public static final Codec<HeadKind> CODEC = StringRepresentable.fromEnum(HeadKind::values);
    public static final StreamCodec<ByteBuf, HeadKind> STREAM_CODEC =
            ByteBufCodecs.idMapper(HeadKind::byOrdinal, HeadKind::ordinal);

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
