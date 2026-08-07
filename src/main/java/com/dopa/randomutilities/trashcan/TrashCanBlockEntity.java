package com.dopa.randomutilities.trashcan;

import com.dopa.randomutilities.registry.ModBlockEntities;
import com.dopa.randomutilities.util.GhostItemFilter;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
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
    /** Same count/layout as advanced item collector filter row. */
    public static final int FILTER_SLOT_COUNT = 8;

    private final NonNullList<ItemStack> filterSlots = NonNullList.withSize(FILTER_SLOT_COUNT, ItemStack.EMPTY);
    private boolean whitelistMode;

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
         * <p>
         * Forbidden (filtered-out) non-empty resources report capacity == amount so hoppers skip them.
         * Empty resource probes must not use that path — otherwise an empty can looks full.
         */
        @Override
        public long getCapacityAsLong(int index, ItemResource resource) {
            if (!resource.isEmpty() && !allows(resource)) {
                return getAmountAsLong(index);
            }
            long real = super.getCapacityAsLong(index, resource);
            long amount = getAmountAsLong(index);
            return Math.max(real, amount + 1L);
        }

        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
            if (!allows(resource)) {
                return 0;
            }
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

    private boolean allows(ItemResource resource) {
        if (resource.isEmpty()) {
            return false;
        }
        return GhostItemFilter.allows(resource.toStack(), filterSlots, whitelistMode);
    }

    public boolean allowsItem(ItemStack stack) {
        return GhostItemFilter.allows(stack, filterSlots, whitelistMode);
    }

    public ItemStacksResourceHandler itemHandler() {
        return itemHandler;
    }

    public NonNullList<ItemStack> filterSlots() {
        return filterSlots;
    }

    public boolean whitelistMode() {
        return whitelistMode;
    }

    public void setWhitelistMode(boolean whitelistMode) {
        this.whitelistMode = whitelistMode;
        setChanged();
    }

    public void setFilterSlot(int index, ItemStack stack) {
        if (index < 0 || index >= FILTER_SLOT_COUNT) {
            return;
        }
        if (stack.isEmpty()) {
            filterSlots.set(index, ItemStack.EMPTY);
        } else {
            filterSlots.set(index, stack.copyWithCount(1));
        }
        setChanged();
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
        for (int i = 0; i < FILTER_SLOT_COUNT; i++) {
            filterSlots.set(i, ItemStack.EMPTY);
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
        ItemStack stack = input.read("Item", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        if (stack.isEmpty()) {
            itemHandler.set(0, ItemResource.EMPTY, 0);
        } else {
            itemHandler.set(0, ItemResource.of(stack), stack.getCount());
        }
        for (int i = 0; i < FILTER_SLOT_COUNT; i++) {
            filterSlots.set(i, input.read("Filter" + i, ItemStack.CODEC).orElse(ItemStack.EMPTY));
        }
        whitelistMode = input.getBooleanOr("WhitelistMode", false);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ItemStack stack = storedStack();
        if (!stack.isEmpty()) {
            output.store("Item", ItemStack.CODEC, stack);
        }
        for (int i = 0; i < FILTER_SLOT_COUNT; i++) {
            ItemStack filter = filterSlots.get(i);
            if (!filter.isEmpty()) {
                output.store("Filter" + i, ItemStack.CODEC, filter);
            }
        }
        output.putBoolean("WhitelistMode", whitelistMode);
    }
}
