package com.dopa.randomutilities.core.filter.client;

import com.dopa.randomutilities.core.filter.FilterContents;
import com.dopa.randomutilities.core.filter.FilterRegistry;
import com.dopa.randomutilities.core.filter.FilterStorage;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.ARGB;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public record FilterTintSource(int defaultColor) implements ItemTintSource {
    public static final MapCodec<FilterTintSource> MAP_CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    ExtraCodecs.RGB_COLOR_CODEC.optionalFieldOf("default", FilterContents.DEFAULT_COLOR)
                            .forGetter(FilterTintSource::defaultColor)
            ).apply(instance, FilterTintSource::new)
    );

    public FilterTintSource {
        defaultColor = ARGB.opaque(defaultColor);
    }

    @Override
    public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
        if (!FilterRegistry.isFilterItem(stack)) {
            return defaultColor;
        }
        return ARGB.opaque(FilterStorage.get(stack).color());
    }

    @Override
    public MapCodec<? extends ItemTintSource> type() {
        return MAP_CODEC;
    }
}
