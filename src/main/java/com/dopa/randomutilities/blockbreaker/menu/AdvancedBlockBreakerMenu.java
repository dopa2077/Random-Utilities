package com.dopa.randomutilities.blockbreaker.menu;

import com.dopa.randomutilities.blockbreaker.AdvancedBlockBreakerBlockEntity;
import com.dopa.randomutilities.filter.menu.GhostFilterHandler;
import com.dopa.randomutilities.filter.menu.GhostFilterMenu;
import com.dopa.randomutilities.filter.menu.GhostFilterSlot;
import com.dopa.randomutilities.gui.machine.VolumeMachineMenu;
import com.dopa.randomutilities.machine.AdvancedVolumeMachineMenuSupport;
import com.dopa.randomutilities.machine.RedstoneMode;
import com.dopa.randomutilities.machine.UpgradeInventory;
import com.dopa.randomutilities.machine.config.UpgradeConfig;
import com.dopa.randomutilities.machine.menu.MachineUpgradeSlot;
import com.dopa.randomutilities.registry.ModMenus;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.dopa.randomutilities.machine.AdvancedVolumeMachineMenuSupport.DATA_ENERGY_CAPACITY;
import static com.dopa.randomutilities.machine.AdvancedVolumeMachineMenuSupport.DATA_ENERGY_MAX_RECEIVE;
import static com.dopa.randomutilities.machine.AdvancedVolumeMachineMenuSupport.DATA_ENERGY_STORED;
import static com.dopa.randomutilities.machine.AdvancedVolumeMachineMenuSupport.DATA_ENERGY_USAGE;
import static com.dopa.randomutilities.machine.AdvancedVolumeMachineMenuSupport.DATA_MAX_RANGE;
import static com.dopa.randomutilities.machine.AdvancedVolumeMachineMenuSupport.DATA_MUTE;
import static com.dopa.randomutilities.machine.AdvancedVolumeMachineMenuSupport.DATA_OFFSET_X;
import static com.dopa.randomutilities.machine.AdvancedVolumeMachineMenuSupport.DATA_OFFSET_Y;
import static com.dopa.randomutilities.machine.AdvancedVolumeMachineMenuSupport.DATA_OFFSET_Z;
import static com.dopa.randomutilities.machine.AdvancedVolumeMachineMenuSupport.DATA_OVERLAY_COLOR;
import static com.dopa.randomutilities.machine.AdvancedVolumeMachineMenuSupport.DATA_RANGE_X;
import static com.dopa.randomutilities.machine.AdvancedVolumeMachineMenuSupport.DATA_RANGE_Y;
import static com.dopa.randomutilities.machine.AdvancedVolumeMachineMenuSupport.DATA_RANGE_Z;
import static com.dopa.randomutilities.machine.AdvancedVolumeMachineMenuSupport.DATA_REDSTONE;
import static com.dopa.randomutilities.machine.AdvancedVolumeMachineMenuSupport.DATA_SIZE;
import static com.dopa.randomutilities.machine.AdvancedVolumeMachineMenuSupport.DATA_WHITELIST;

public class AdvancedBlockBreakerMenu extends AbstractContainerMenu implements GhostFilterMenu, VolumeMachineMenu {
    public static final int ENERGY_BAR_X = 10;
    public static final int ENERGY_BAR_Y = 7;
    public static final int ENERGY_BAR_W = 11;
    public static final int ENERGY_BAR_H = 62;

    public static final int PICKAXE_X = 80;
    public static final int PICKAXE_Y = 34;
    public static final int FILTER_SLOT_COUNT = AdvancedBlockBreakerBlockEntity.FILTER_SLOTS;
    public static final int ICON_X = 8;
    public static final int FILTER_SLOT_X = 26;
    public static final int FILTER_SLOT_Y = ENERGY_BAR_Y + ENERGY_BAR_H + 3;
    public static final int PLAYER_INV_Y = 107;
    public static final int IMAGE_HEIGHT = PLAYER_INV_Y + 82;

    private final AdvancedBlockBreakerBlockEntity be;
    private final ContainerLevelAccess access;
    private final GhostFilterHandler filterHandler;
    private final ContainerData data;
    private final List<MachineUpgradeSlot> upgradeSlots;
    private final int pickaxeSlotIndex;
    private final int filterStart;
    private final int playerInvStart;

    public AdvancedBlockBreakerMenu(int containerId, Inventory playerInv, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInv, resolveBlockEntity(playerInv, buf.readBlockPos()));
    }

    public AdvancedBlockBreakerMenu(int containerId, Inventory playerInv, AdvancedBlockBreakerBlockEntity be) {
        super(ModMenus.ADVANCED_BLOCK_BREAKER.get(), containerId);
        this.be = be;
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());

        List<MachineUpgradeSlot> upgrades = new ArrayList<>();
        UpgradeInventory handler = be.upgrades();
        for (int i = 0; i < UpgradeConfig.UPGRADE_SLOT_COUNT; i++) {
            MachineUpgradeSlot slot = new MachineUpgradeSlot(handler, i, 0);
            this.addSlot(slot);
            upgrades.add(slot);
        }
        this.upgradeSlots = Collections.unmodifiableList(upgrades);

        this.pickaxeSlotIndex = this.slots.size();
        this.addSlot(new ResourceHandlerSlot(
                be.pickaxeHandler(),
                be.pickaxeHandler()::set,
                0,
                PICKAXE_X,
                PICKAXE_Y
        ) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ItemTags.PICKAXES);
            }
        });

        this.filterStart = this.slots.size();
        NonNullList<ItemStack> stacks = NonNullList.withSize(FILTER_SLOT_COUNT, ItemStack.EMPTY);
        for (int i = 0; i < FILTER_SLOT_COUNT; i++) {
            stacks.set(i, be.filterSlots().get(i));
        }
        this.filterHandler = new GhostFilterHandler(stacks);
        this.filterHandler.setOnChanged(() -> {
            saveFilters();
            be.setChanged();
        });
        for (int i = 0; i < FILTER_SLOT_COUNT; i++) {
            this.addSlot(new GhostFilterSlot(filterHandler, i, FILTER_SLOT_X + i * 18, FILTER_SLOT_Y));
        }

        this.playerInvStart = this.slots.size();
        this.addStandardInventorySlots(playerInv, 8, PLAYER_INV_Y);

        this.data = new SimpleContainerData(DATA_SIZE);
        syncData();
        this.addDataSlots(data);
    }

    private static AdvancedBlockBreakerBlockEntity resolveBlockEntity(Inventory playerInv, BlockPos pos) {
        BlockEntity be = playerInv.player.level().getBlockEntity(pos);
        if (be instanceof AdvancedBlockBreakerBlockEntity breaker) {
            return breaker;
        }
        throw new IllegalStateException("Missing advanced block breaker at " + pos);
    }

    private void saveFilters() {
        for (int i = 0; i < FILTER_SLOT_COUNT; i++) {
            be.setFilterSlot(i, filterHandler.getResource(i).toStack(filterHandler.getAmountAsInt(i)));
        }
    }

    private void syncData() {
        AdvancedVolumeMachineMenuSupport.syncData(data, be);
    }

    @Override
    public void broadcastChanges() {
        syncData();
        super.broadcastChanges();
    }

    public List<MachineUpgradeSlot> getUpgradeSlots() {
        return upgradeSlots;
    }

    public boolean isUpgradeSlotIndex(int index) {
        return index >= 0 && index < pickaxeSlotIndex;
    }

    public int pickaxeSlotIndex() {
        return pickaxeSlotIndex;
    }

    public boolean isPickaxeSlotIndex(int index) {
        return index == pickaxeSlotIndex;
    }

    public int filterSlotStart() {
        return filterStart;
    }

    public AdvancedBlockBreakerBlockEntity blockEntity() {
        return be;
    }

    public int getRangeX() {
        return data.get(DATA_RANGE_X);
    }

    public int getRangeY() {
        return data.get(DATA_RANGE_Y);
    }

    public int getRangeZ() {
        return data.get(DATA_RANGE_Z);
    }

    public int getOffsetX() {
        return data.get(DATA_OFFSET_X);
    }

    public int getOffsetY() {
        return data.get(DATA_OFFSET_Y);
    }

    public int getOffsetZ() {
        return data.get(DATA_OFFSET_Z);
    }

    public boolean isWhitelistMode() {
        return data.get(DATA_WHITELIST) != 0;
    }

    public boolean isMuted() {
        return data.get(DATA_MUTE) != 0;
    }

    public RedstoneMode redstoneMode() {
        return RedstoneMode.byOrdinal(data.get(DATA_REDSTONE));
    }

    public int energyStored() {
        return data.get(DATA_ENERGY_STORED);
    }

    public int energyCapacity() {
        return data.get(DATA_ENERGY_CAPACITY);
    }

    public int energyUsage() {
        return data.get(DATA_ENERGY_USAGE);
    }

    public int energyMaxReceive() {
        return data.get(DATA_ENERGY_MAX_RECEIVE);
    }

    public int maxRange() {
        return data.get(DATA_MAX_RANGE);
    }

    public int getOverlayColor() {
        return data.get(DATA_OVERLAY_COLOR) & 0xFFFFFF;
    }

    public void setRangeX(int value) {
        be.setRangeX(value);
        data.set(DATA_RANGE_X, be.workingVolume().rangeX());
    }

    public void setRangeY(int value) {
        be.setRangeY(value);
        data.set(DATA_RANGE_Y, be.workingVolume().rangeY());
    }

    public void setRangeZ(int value) {
        be.setRangeZ(value);
        data.set(DATA_RANGE_Z, be.workingVolume().rangeZ());
    }

    public void setOffsetX(int value) {
        be.setOffsetX(value);
        data.set(DATA_OFFSET_X, be.workingVolume().offsetX());
    }

    public void setOffsetY(int value) {
        be.setOffsetY(value);
        data.set(DATA_OFFSET_Y, be.workingVolume().offsetY());
    }

    public void setOffsetZ(int value) {
        be.setOffsetZ(value);
        data.set(DATA_OFFSET_Z, be.workingVolume().offsetZ());
    }

    public void setWhitelistMode(boolean whitelist) {
        be.setWhitelistMode(whitelist);
        data.set(DATA_WHITELIST, be.whitelistMode() ? 1 : 0);
    }

    public void setMuted(boolean mute) {
        be.setMuted(mute);
        data.set(DATA_MUTE, be.isMuted() ? 1 : 0);
    }

    public void setOverlayColor(int color) {
        be.setOverlayColor(color);
        data.set(DATA_OVERLAY_COLOR, be.overlayColor());
    }

    public void setRedstoneMode(RedstoneMode mode) {
        be.setRedstoneMode(mode);
        data.set(DATA_REDSTONE, be.redstoneMode().ordinal());
    }

    public void applySetting(byte kind, int value) {
        AdvancedVolumeMachineMenuSupport.applySetting(data, be, kind, value);
    }

    public void setFilterSlot(int index, ItemStack stack) {
        be.setFilterSlot(index, stack);
        if (index >= 0 && index < FILTER_SLOT_COUNT) {
            if (stack.isEmpty()) {
                filterHandler.set(index, ItemResource.EMPTY, 0);
            } else {
                filterHandler.set(index, ItemResource.of(stack), 1);
            }
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, be.getBlockState().getBlock());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        if (index >= filterStart && index < playerInvStart) {
            slot.setByPlayer(ItemStack.EMPTY);
            return ItemStack.EMPTY;
        }
        ItemStack remaining = stack.copy();
        if (index < filterStart) {
            if (!this.moveItemStackTo(stack, playerInvStart, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (UpgradeInventory.isEnergyMachineUpgrade(stack)
                && this.moveItemStackTo(stack, 0, pickaxeSlotIndex, false)) {
            // upgrades first while the upgrade panel is the intended destination
        } else if (stack.is(ItemTags.PICKAXES)
                && this.moveItemStackTo(stack, pickaxeSlotIndex, pickaxeSlotIndex + 1, false)) {
            // pickaxe into the tool slot
        } else if (UpgradeInventory.isEnergyMachineUpgrade(stack)) {
            return ItemStack.EMPTY;
        } else {
            boolean ghosted = false;
            for (int i = filterStart; i < playerInvStart; i++) {
                Slot filterSlot = this.slots.get(i);
                if (filterSlot.getItem().isEmpty()) {
                    filterSlot.safeInsert(stack, 1);
                    ghosted = true;
                    break;
                }
            }
            if (!ghosted) {
                return ItemStack.EMPTY;
            }
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return remaining;
    }

    @Override
    public boolean canDragTo(Slot slot) {
        return !slot.isFake();
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
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

    @Override
    public int filterSlotCount() {
        return FILTER_SLOT_COUNT;
    }

    @Override
    public int iconX() {
        return ICON_X;
    }

    @Override
    public int filterSlotY() {
        return FILTER_SLOT_Y;
    }

    @Override
    public int energyBarX() {
        return ENERGY_BAR_X;
    }

    @Override
    public int energyBarY() {
        return ENERGY_BAR_Y;
    }

    @Override
    public int energyBarW() {
        return ENERGY_BAR_W;
    }

    @Override
    public int energyBarH() {
        return ENERGY_BAR_H;
    }

    @Override
    public com.dopa.randomutilities.machine.EnergyMachineUpgradeInventory upgrades() {
        return be.upgrades();
    }

    @Override
    public BlockPos machinePos() {
        return be.getBlockPos();
    }

    @Override
    public net.minecraft.world.level.Level machineLevel() {
        return be.getLevel();
    }
}
