package com.dopa.randomutilities.item.magnet.client;

import com.dopa.randomutilities.item.magnet.MagnetContents;
import com.dopa.randomutilities.item.magnet.MagnetStorage;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.ARGB;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public record MagnetTintSource(int defaultColor) implements ItemTintSource {
    public static final MapCodec<MagnetTintSource> MAP_CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    ExtraCodecs.RGB_COLOR_CODEC.optionalFieldOf("default", MagnetContents.DEFAULT_COLOR)
                            .forGetter(MagnetTintSource::defaultColor)
            ).apply(instance, MagnetTintSource::new)
    );

    public MagnetTintSource {
        defaultColor = ARGB.opaque(defaultColor);
    }

    @Override
    public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
        if (!MagnetStorage.isMagnet(stack)) {
            return defaultColor;
        }
        return ARGB.opaque(MagnetStorage.get(stack).color());
    }

    @Override
    public MapCodec<? extends ItemTintSource> type() {
        return MAP_CODEC;
    }
}
