package com.dopa.randomutilities.logistics.transfer.client;

import com.dopa.randomutilities.logistics.transfer.TransferPipeBlock;
import com.mojang.serialization.MapCodec;

import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public record TransferPipeItemTintSource() implements ItemTintSource {
    public static final TransferPipeItemTintSource INSTANCE = new TransferPipeItemTintSource();
    public static final MapCodec<TransferPipeItemTintSource> MAP_CODEC = MapCodec.unit(INSTANCE);

    @Override
    public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
        if (stack.getItem() instanceof BlockItem item && item.getBlock() instanceof TransferPipeBlock pipe) {
            return pipe.channel().tint();
        }
        return 0xFFFFFFFF;
    }

    @Override
    public MapCodec<? extends ItemTintSource> type() {
        return MAP_CODEC;
    }
}
