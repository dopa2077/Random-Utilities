package com.dopa.randomutilities.trashcan;

import com.dopa.randomutilities.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/** Single-slot trash can: stacks normally, voids overflow and previous stack on type change. */
public class TrashCanBlockEntity extends BlockEntity {
    public static final int SLOT_COUNT = 1;

    private final ItemStacksResourceHandler itemHandler = new ItemStacksResourceHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int index, ItemStack previousContents) {
            setChanged();
        }

        @Override
        protected int getCapacity(int index, ItemResource resource) {
            if (resource.isEmpty()) {
                ItemResource current = getResource(index);
                return current.isEmpty() ? 64 : Math.min(64, current.getMaxStackSize());
            }
            return Math.min(64, resource.getMaxStackSize());
        }

        /**
         * Hoppers bail out via {@code ResourceHandlerUtil.isFull} when amount >= capacity for the
         * current resource. Always report at least one free unit so automation keeps inserting;
         * {@link #insert} still only stores up to the real stack limit and voids the rest / old type.
         */
        @Override
        public long getCapacityAsLong(int index, ItemResource resource) {
            long real = super.getCapacityAsLong(index, resource);
            long amount = getAmountAsLong(index);
            return Math.max(real, amount + 1L);
        }

        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
            ItemResource current = getResource(index);
            int currentAmount = getAmountAsInt(index);

            if (!current.isEmpty() && !current.equals(resource)) {
                extract(index, current, currentAmount, transaction);
            }

            int capacity = getCapacity(index, resource);
            int storedNow = getAmountAsInt(index);
            int room = Math.max(0, capacity - storedNow);
            if (room > 0) {
                super.insert(index, resource, Math.min(amount, room), transaction);
            }
            // Always consume the full offered amount: leftover is voided.
            return amount;
        }
    };

    public TrashCanBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TRASH_CAN.get(), pos, state);
    }

    public ItemStacksResourceHandler itemHandler() {
        return itemHandler;
    }

    public Component getDisplayName() {
        return Component.translatable("container.dopasrandomutilities.trash_can");
    }

    public ItemStack storedStack() {
        ItemResource resource = itemHandler.getResource(0);
        if (resource.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return resource.toStack(itemHandler.getAmountAsInt(0));
    }

    public boolean isEmpty() {
        return itemHandler.getResource(0).isEmpty();
    }

    public void dropContents(Level level, BlockPos pos) {
        ItemStack stack = storedStack();
        if (!stack.isEmpty()) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
        }
        itemHandler.set(0, ItemResource.EMPTY, 0);
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
        ItemStack stack = input.read("Item", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        if (stack.isEmpty()) {
            itemHandler.set(0, ItemResource.EMPTY, 0);
        } else {
            itemHandler.set(0, ItemResource.of(stack), stack.getCount());
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ItemStack stack = storedStack();
        if (!stack.isEmpty()) {
            output.store("Item", ItemStack.CODEC, stack);
        }
    }
}
