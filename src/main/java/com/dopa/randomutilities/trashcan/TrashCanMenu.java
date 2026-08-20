package com.dopa.randomutilities.trashcan;

import com.dopa.randomutilities.filter.menu.GhostFilterHandler;
import com.dopa.randomutilities.filter.menu.GhostFilterSlot;
import com.dopa.randomutilities.registry.ModBlocks;
import com.dopa.randomutilities.registry.ModMenus;
import com.dopa.randomutilities.util.GhostItemFilter;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.transfer.item.ItemResource;

public class TrashCanMenu extends AbstractContainerMenu {
    public static final int DATA_WHITELIST_MODE = 0;
    public static final int DATA_SIZE = 1;

    public static final int IMAGE_WIDTH = 176;
    public static final int IMAGE_HEIGHT = 155;
    public static final int CHEST_SLOT_X = 80;
    public static final int CHEST_SLOT_Y = 20;
    /** Advanced collector layout: icon @ 8, filters @ 26, 5px under the trash slot. */
    public static final int FILTER_ICON_X = 8;
    public static final int FILTER_SLOT_X = 26;
    public static final int FILTER_SLOT_Y = 43;
    public static final int FILTER_SLOT_COUNT = TrashCanBlockEntity.FILTER_SLOT_COUNT;
    public static final int PLAYER_INV_Y = 74;

    private final TrashCanBlockEntity trashCan;
    private final ContainerLevelAccess access;
    private final GhostFilterHandler filterHandler;
    private final ContainerData data;

    public TrashCanMenu(int containerId, Inventory playerInv, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInv, resolveBlockEntity(playerInv, buf.readBlockPos()));
    }

    public TrashCanMenu(int containerId, Inventory playerInv, TrashCanBlockEntity trashCan) {
        super(ModMenus.TRASH_CAN.get(), containerId);
        this.trashCan = trashCan;
        this.access = ContainerLevelAccess.create(trashCan.getLevel(), trashCan.getBlockPos());

        NonNullList<ItemStack> stacks = NonNullList.withSize(FILTER_SLOT_COUNT, ItemStack.EMPTY);
        for (int i = 0; i < FILTER_SLOT_COUNT; i++) {
            stacks.set(i, trashCan.filterSlots().get(i));
        }
        this.filterHandler = new GhostFilterHandler(stacks);
        this.filterHandler.setOnChanged(() -> {
            saveFilters();
            trashCan.setChanged();
        });

        this.data = new SimpleContainerData(DATA_SIZE);
        syncData();

        // Use menu-synced filters/mode so client mayPlace matches the server (BE filters are not synced).
        this.addSlot(new TrashCanSlot(trashCan, CHEST_SLOT_X, CHEST_SLOT_Y, this::allowsInsert));
        for (int i = 0; i < FILTER_SLOT_COUNT; i++) {
            this.addSlot(new GhostFilterSlot(filterHandler, i, FILTER_SLOT_X + i * 18, FILTER_SLOT_Y));
        }

        this.addStandardInventorySlots(playerInv, 8, PLAYER_INV_Y);
        this.addDataSlots(data);
    }

    private static TrashCanBlockEntity resolveBlockEntity(Inventory playerInv, BlockPos pos) {
        BlockEntity be = playerInv.player.level().getBlockEntity(pos);
        if (be instanceof TrashCanBlockEntity trashCan) {
            return trashCan;
        }
        throw new IllegalStateException("Missing trash can at " + pos);
    }

    private void saveFilters() {
        for (int i = 0; i < FILTER_SLOT_COUNT; i++) {
            trashCan.setFilterSlot(i, filterHandler.getResource(i).toStack(filterHandler.getAmountAsInt(i)));
        }
    }

    private void syncData() {
        data.set(DATA_WHITELIST_MODE, trashCan.whitelistMode() ? 1 : 0);
    }

    /** Menu-local filter check (ghost slots + ContainerData), valid on client and server. */
    boolean allowsInsert(ItemStack stack) {
        NonNullList<ItemStack> filters = NonNullList.withSize(FILTER_SLOT_COUNT, ItemStack.EMPTY);
        for (int i = 0; i < FILTER_SLOT_COUNT; i++) {
            ItemResource resource = filterHandler.getResource(i);
            if (!resource.isEmpty()) {
                filters.set(i, resource.toStack(1));
            }
        }
        return GhostItemFilter.allows(stack, filters, isWhitelistMode());
    }

    public TrashCanBlockEntity blockEntity() {
        return trashCan;
    }

    public boolean isWhitelistMode() {
        return data.get(DATA_WHITELIST_MODE) != 0;
    }

    public boolean isInputSlot(Slot slot) {
        return slot != null && slot.index == 0;
    }

    public void setWhitelistMode(boolean whitelist) {
        trashCan.setWhitelistMode(whitelist);
        syncData();
        broadcastChanges();
    }

    public void setFilterSlot(int index, ItemStack stack) {
        trashCan.setFilterSlot(index, stack);
        if (index >= 0 && index < FILTER_SLOT_COUNT) {
            if (stack.isEmpty()) {
                filterHandler.set(index, ItemResource.EMPTY, 0);
            } else {
                filterHandler.set(index, ItemResource.of(stack), 1);
            }
        }
        broadcastChanges();
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput containerInput, Player player) {
        if (slotId == 0
                && containerInput == ContainerInput.PICKUP
                && (button == 0 || button == 1)
                && tryVoidReplaceClick(button)) {
            return;
        }
        super.clicked(slotId, button, containerInput, player);
    }

    /** Different item on occupied slot: void previous, then insert from cursor (no swap). */
    private boolean tryVoidReplaceClick(int button) {
        Slot slot = this.slots.getFirst();
        ItemStack carried = this.getCarried();
        ItemStack slotStack = slot.getItem();
        if (carried.isEmpty() || slotStack.isEmpty()) {
            return false;
        }
        if (ItemStack.isSameItemSameComponents(slotStack, carried)) {
            return false;
        }
        if (!slot.mayPlace(carried)) {
            return false;
        }
        slot.setByPlayer(ItemStack.EMPTY);
        int amount = button == 0 ? carried.getCount() : 1;
        this.setCarried(slot.safeInsert(carried, amount));
        slot.setChanged();
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, ModBlocks.TRASH_CAN.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return result;
        }
        ItemStack stack = slot.getItem();
        result = stack.copy();
        int filterEnd = 1 + FILTER_SLOT_COUNT;
        if (index == 0) {
            if (!this.moveItemStackTo(stack, filterEnd, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (index >= 1 && index < filterEnd) {
            // Ghost clear — do not move into player inventory.
            slot.setByPlayer(ItemStack.EMPTY);
            return ItemStack.EMPTY;
        } else {
            // Shift-click always targets the trash slot; filters are set manually / via JEI.
            ItemStack toInsert = stack.copy();
            Slot trashSlot = this.slots.getFirst();
            if (!trashSlot.mayPlace(toInsert)) {
                return ItemStack.EMPTY;
            }
            ItemStack before = trashSlot.getItem().copy();
            int countBefore = toInsert.getCount();
            trashSlot.safeInsert(toInsert, toInsert.getCount());
            int moved = countBefore - toInsert.getCount();
            if (moved <= 0 && ItemStack.matches(before, trashSlot.getItem())) {
                if (!before.isEmpty() && !ItemStack.isSameItemSameComponents(before, stack)) {
                    trashSlot.setByPlayer(ItemStack.EMPTY);
                    trashSlot.safeInsert(toInsert, toInsert.getCount());
                    moved = countBefore - toInsert.getCount();
                }
            }
            if (moved <= 0 && toInsert.getCount() == countBefore) {
                return ItemStack.EMPTY;
            }
            stack.setCount(toInsert.getCount());
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return result;
    }

    @Override
    public boolean canDragTo(Slot slot) {
        return !slot.isFake();
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        // Ghost filter slots must not participate in double-click gather.
        if (slot.isFake()) {
            return false;
        }
        return super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        saveFilters();
    }
}
