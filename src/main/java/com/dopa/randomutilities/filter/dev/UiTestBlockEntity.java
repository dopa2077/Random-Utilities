package com.dopa.randomutilities.filter.dev;

import com.dopa.randomutilities.registry.ModBlockEntities;
import com.dopa.randomutilities.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Stores a mutable UI-test filter host stack for the placeable tester block. */
public class UiTestBlockEntity extends BlockEntity {
    private ItemStack hostStack = ItemStack.EMPTY;

    public UiTestBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.UI_TEST.get(), pos, state);
    }

    public ItemStack hostStack() {
        if (hostStack.isEmpty()) {
            hostStack = new ItemStack(ModItems.UI_TEST_ITEM.get());
        }
        return hostStack;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hostStack = input.read("Host", ItemStack.CODEC).orElse(ItemStack.EMPTY);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!hostStack.isEmpty()) {
            output.store("Host", ItemStack.CODEC, hostStack);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return new CompoundTag();
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
