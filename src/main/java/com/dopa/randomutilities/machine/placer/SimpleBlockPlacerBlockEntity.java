package com.dopa.randomutilities.machine.placer;

import com.dopa.randomutilities.core.machine.ClaimActionGate;
import com.dopa.randomutilities.core.machine.MachineOwnerProfiles;
import com.dopa.randomutilities.core.machine.OwnableMachine;
import com.dopa.randomutilities.core.machine.OwnerRequiredFeedback;
import com.dopa.randomutilities.registry.ModBlockEntities;
import com.dopa.randomutilities.core.util.ActionCooldownFeedback;
import com.dopa.randomutilities.core.util.BlockOrientations;
import com.dopa.randomutilities.core.util.RedstonePulse;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class SimpleBlockPlacerBlockEntity extends BlockEntity implements OwnableMachine {
    public static final int SLOT_COUNT = 9;

    private final ItemStacksResourceHandler items = new ItemStacksResourceHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int index, ItemStack previousContents) {
            setChanged();
        }
    };

    private final RedstonePulse pulse = new RedstonePulse();
    private int actionCooldown;
    @Nullable
    private UUID ownerUuid;

    public SimpleBlockPlacerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SIMPLE_BLOCK_PLACER.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SimpleBlockPlacerBlockEntity be) {
        if (!level.isClientSide()) {
            be.tick((ServerLevel) level, pos, state);
        }
    }

    public ItemStacksResourceHandler itemHandler() {
        return items;
    }

    public Component getDisplayName() {
        return Component.translatable("container.dopasrandomutilities.simple_block_placer");
    }

    public ItemStack stackInSlot(int slot) {
        ItemResource resource = items.getResource(slot);
        if (resource.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return resource.toStack(items.getAmountAsInt(slot));
    }

    public int getAnalogOutput() {
        return BlockPlacerActions.analogOutput(SLOT_COUNT, this::stackInSlot);
    }

    private void tick(ServerLevel level, BlockPos pos, BlockState state) {
        if (actionCooldown > 0) {
            actionCooldown--;
        }
        boolean powered = level.getBestNeighborSignal(pos) > 0;
        if (!pulse.risingEdge(powered)) {
            return;
        }
        if (OwnerRequiredFeedback.blockRisingEdge(level, pos, this, actionCooldown)) {
            if (!hasOwner()) {
                actionCooldown = OwnerRequiredFeedback.cooldownAfterNoOwnerBlock(actionCooldown);
            }
            return;
        }
        if (tryPlace(level, pos, state)) {
            actionCooldown = ActionCooldownFeedback.DEFAULT_COOLDOWN_TICKS;
        }
    }

    private boolean tryPlace(ServerLevel level, BlockPos pos, BlockState state) {
        Direction facing = BlockOrientations.front(state);
        BlockPos front = pos.relative(facing);
        if (!level.getBlockState(front).canBeReplaced()) {
            return false;
        }
        int slot = BlockPlacerActions.pickRandomBlockSlot(SLOT_COUNT, level.getRandom(), this::stackInSlot, stack -> true);
        if (slot < 0) {
            return false;
        }
        boolean placed = BlockPlacerActions.placeFromSlot(
                level,
                pos,
                state,
                front,
                slot,
                items,
                this::stackInSlot,
                ownerUuid,
                (placer, stack) -> ClaimActionGate.canPlace(level, placer, front, stack, facing.getOpposite())
        );
        if (placed) {
            setChanged();
        }
        return placed;
    }

    @Override
    @Nullable
    public UUID ownerUuid() {
        return ownerUuid;
    }

    @Override
    public void setOwnerUuid(@Nullable UUID uuid) {
        this.ownerUuid = uuid;
        setChanged();
    }

    public void dropContents(Level level, BlockPos pos) {
        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack stack = stackInSlot(i);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
            }
            items.set(i, ItemResource.EMPTY, 0);
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null && !level.isClientSide()) {
            dropContents(level, pos);
        }
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack stack = input.read("Item" + i, ItemStack.CODEC).orElse(ItemStack.EMPTY);
            if (stack.isEmpty()) {
                items.set(i, ItemResource.EMPTY, 0);
            } else {
                items.set(i, ItemResource.of(stack), stack.getCount());
            }
        }
        pulse.setWasPowered(input.getBooleanOr("WasPowered", false));
        actionCooldown = Math.max(0, input.getIntOr("ActionCooldown", 0));
        ownerUuid = MachineOwnerProfiles.load(input);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack stack = stackInSlot(i);
            if (!stack.isEmpty()) {
                output.store("Item" + i, ItemStack.CODEC, stack);
            }
        }
        if (pulse.wasPowered()) {
            output.putBoolean("WasPowered", true);
        }
        if (actionCooldown > 0) {
            output.putInt("ActionCooldown", actionCooldown);
        }
        if (ownerUuid != null) {
            MachineOwnerProfiles.save(output, ownerUuid);
        }
    }
}
