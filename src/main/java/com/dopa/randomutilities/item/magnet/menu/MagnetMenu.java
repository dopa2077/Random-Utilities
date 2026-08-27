package com.dopa.randomutilities.item.magnet.menu;

import com.dopa.randomutilities.core.filter.menu.GhostFilterHandler;
import com.dopa.randomutilities.core.filter.menu.GhostFilterMenu;
import com.dopa.randomutilities.core.filter.menu.GhostFilterSlot;
import com.dopa.randomutilities.core.gui.panel.PanelLayout;
import com.dopa.randomutilities.core.machine.UpgradeInventory;
import com.dopa.randomutilities.core.machine.config.UpgradeConfig;
import com.dopa.randomutilities.core.machine.menu.MachineUpgradeSlot;
import com.dopa.randomutilities.item.magnet.MagnetContents;
import com.dopa.randomutilities.item.magnet.MagnetItem;
import com.dopa.randomutilities.item.magnet.MagnetStorage;
import com.dopa.randomutilities.item.magnet.config.MagnetConfig;
import com.dopa.randomutilities.registry.ModMenus;

import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MagnetMenu extends AbstractContainerMenu implements GhostFilterMenu {
    public static final int DATA_RANGE = 0;
    public static final int DATA_WHITELIST = 1;
    public static final int DATA_COLLECT = 2;
    public static final int DATA_IGNORE_DELAY = 3;
    public static final int DATA_PAUSE_SNEAK = 4;
    public static final int DATA_PULL_XP = 5;
    public static final int DATA_PARTICLES = 6;
    public static final int DATA_COLOR = 7;
    public static final int DATA_MAX_RANGE = 8;
    public static final int DATA_SIZE = 9;
    public static final int IMAGE_WIDTH = 176;
    public static final int IMAGE_HEIGHT = 133;
    public static final int FILTER_SLOT_COUNT = MagnetContents.FILTER_SLOTS;
    public static final int FILTER_SLOT_Y = 20;
    public static final int PLAYER_INV_Y = 51;
    public static final int ICON_X = 8;
    public static final int FILTER_SLOT_X = 26;

    private final Player player;
    private final InteractionHand hand;
    private final ItemStack openedHost;
    private final UpgradeInventory upgrades;
    private final GhostFilterHandler filterHandler;
    private final ContainerData data;
    private final List<MachineUpgradeSlot> upgradeSlots;
    private final int filterStart;
    private final int playerInvStart;

    public MagnetMenu(int containerId, Inventory playerInv, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInv, buf.readEnum(InteractionHand.class));
    }

    public MagnetMenu(int containerId, Inventory playerInv, InteractionHand hand) {
        super(ModMenus.ITEM_MAGNET.get(), containerId);
        this.player = playerInv.player;
        this.hand = hand;
        this.openedHost = player.getItemInHand(hand);
        MagnetContents contents = MagnetStorage.get(openedHost);

        this.upgrades = UpgradeInventory.withCaps(UpgradeConfig.UPGRADE_SLOT_COUNT, MagnetConfig::capFor);
        this.upgrades.loadStacks(contents.upgrades());
        this.upgrades.setOnChanged(this::saveUpgrades);

        List<MachineUpgradeSlot> upgradeSlotList = new ArrayList<>();
        int upgradeSlotYBias = -PanelLayout.TAB_SIZE;
        for (int i = 0; i < UpgradeConfig.UPGRADE_SLOT_COUNT; i++) {
            MachineUpgradeSlot slot = new MachineUpgradeSlot(upgrades, i, upgradeSlotYBias);
            this.addSlot(slot);
            upgradeSlotList.add(slot);
        }
        this.upgradeSlots = Collections.unmodifiableList(upgradeSlotList);

        this.filterStart = this.slots.size();
        NonNullList<ItemStack> filters = contents.filterSlots();
        this.filterHandler = new GhostFilterHandler(filters);
        this.filterHandler.setOnChanged(this::saveFilters);
        for (int i = 0; i < FILTER_SLOT_COUNT; i++) {
            this.addSlot(new GhostFilterSlot(filterHandler, i, FILTER_SLOT_X + i * 18, FILTER_SLOT_Y));
        }

        this.playerInvStart = this.slots.size();
        this.addStandardInventorySlots(playerInv, 8, PLAYER_INV_Y);

        this.data = new SimpleContainerData(DATA_SIZE);
        syncData();
        this.addDataSlots(data);
    }

    private ItemStack host() {
        return player.getItemInHand(hand);
    }

    private boolean hostValid() {
        ItemStack host = host();
        return host == openedHost && host.getItem() instanceof MagnetItem;
    }

    private void saveFilters() {
        if (!hostValid()) {
            return;
        }
        MagnetContents contents = MagnetStorage.get(openedHost);
        for (int i = 0; i < FILTER_SLOT_COUNT; i++) {
            contents = contents.withFilter(i, filterHandler.getResource(i).toStack(filterHandler.getAmountAsInt(i)));
        }
        MagnetStorage.set(openedHost, contents);
    }

    private void saveUpgrades() {
        if (!hostValid()) {
            return;
        }
        MagnetContents contents = MagnetStorage.get(openedHost).withUpgrades(upgrades.snapshot());
        contents = contents.withRange(MagnetStorage.clampRange(contents, contents.range()));
        MagnetStorage.set(openedHost, contents);
        data.set(DATA_RANGE, contents.range());
        data.set(DATA_MAX_RANGE, MagnetStorage.maxRange(contents));
    }

    private void update(MagnetContents contents) {
        MagnetStorage.set(openedHost, contents);
        syncData();
    }

    private void syncData() {
        MagnetContents contents = MagnetStorage.get(openedHost);
        data.set(DATA_RANGE, contents.range());
        data.set(DATA_WHITELIST, contents.whitelist() ? 1 : 0);
        data.set(DATA_COLLECT, contents.collectMode() ? 1 : 0);
        data.set(DATA_IGNORE_DELAY, contents.ignorePickupDelay() ? 1 : 0);
        data.set(DATA_PAUSE_SNEAK, contents.pauseOnSneak() ? 1 : 0);
        data.set(DATA_PULL_XP, contents.pullXp() ? 1 : 0);
        data.set(DATA_PARTICLES, contents.particles() ? 1 : 0);
        data.set(DATA_COLOR, contents.color());
        data.set(DATA_MAX_RANGE, MagnetStorage.maxRange(contents));
    }

    @Override
    public void broadcastChanges() {
        if (hostValid()) {
            syncData();
        }
        super.broadcastChanges();
    }

    public List<MachineUpgradeSlot> getUpgradeSlots() {
        return upgradeSlots;
    }

    public UpgradeInventory upgrades() {
        return upgrades;
    }

    public boolean isUpgradeSlotIndex(int index) {
        return index >= 0 && index < filterStart;
    }

    public int filterSlotStart() {
        return filterStart;
    }

    public int getRange() {
        return data.get(DATA_RANGE);
    }

    public boolean isWhitelistMode() {
        return data.get(DATA_WHITELIST) != 0;
    }

    public boolean isCollectMode() {
        return data.get(DATA_COLLECT) != 0;
    }

    public boolean isIgnorePickupDelay() {
        return data.get(DATA_IGNORE_DELAY) != 0;
    }

    public boolean isPauseOnSneak() {
        return data.get(DATA_PAUSE_SNEAK) != 0;
    }

    public boolean isPullXp() {
        return data.get(DATA_PULL_XP) != 0;
    }

    public boolean isParticlesEnabled() {
        return data.get(DATA_PARTICLES) != 0;
    }

    public int getOverlayColor() {
        return data.get(DATA_COLOR) & 0xFFFFFF;
    }

    public int maxRange() {
        return data.get(DATA_MAX_RANGE);
    }

    public void setRange(int value) {
        if (!hostValid()) {
            return;
        }
        MagnetContents contents = MagnetStorage.get(openedHost);
        update(contents.withRange(MagnetStorage.clampRange(contents, value)));
    }

    public void setWhitelistMode(boolean whitelist) {
        if (!hostValid()) {
            return;
        }
        update(MagnetStorage.get(openedHost).withWhitelist(whitelist));
    }

    public void setCollectMode(boolean collect) {
        if (!hostValid()) {
            return;
        }
        update(MagnetStorage.get(openedHost).withCollectMode(collect));
    }

    public void setIgnorePickupDelay(boolean ignore) {
        if (!hostValid()) {
            return;
        }
        update(MagnetStorage.get(openedHost).withIgnorePickupDelay(ignore));
    }

    public void setPauseOnSneak(boolean pause) {
        if (!hostValid()) {
            return;
        }
        update(MagnetStorage.get(openedHost).withPauseOnSneak(pause));
    }

    public void setPullXp(boolean pullXp) {
        if (!hostValid()) {
            return;
        }
        update(MagnetStorage.get(openedHost).withPullXp(pullXp));
    }

    public void setParticlesEnabled(boolean enabled) {
        if (!hostValid()) {
            return;
        }
        update(MagnetStorage.get(openedHost).withParticles(enabled));
    }

    public void setOverlayColor(int color) {
        if (!hostValid()) {
            return;
        }
        update(MagnetStorage.get(openedHost).withColor(color));
    }

    public void setFilterSlot(int index, ItemStack stack) {
        if (!hostValid() || index < 0 || index >= FILTER_SLOT_COUNT) {
            return;
        }
        if (stack.isEmpty()) {
            filterHandler.set(index, ItemResource.EMPTY, 0);
        } else {
            filterHandler.set(index, ItemResource.of(stack), 1);
        }
        saveFilters();
    }

    @Override
    public boolean stillValid(Player player) {
        return player == this.player && hostValid();
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput containerInput, Player player) {
        if (slotId >= 0 && slotId < slots.size() && slots.get(slotId).getItem() == openedHost) {
            return;
        }
        if (containerInput == ContainerInput.SWAP) {
            ItemStack hotbar = player.getInventory().getItem(button);
            if (hotbar == openedHost) {
                return;
            }
        }
        super.clicked(slotId, button, containerInput, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        if (stack == openedHost) {
            return ItemStack.EMPTY;
        }
        if (index < filterStart) {
            if (!this.moveItemStackTo(stack, playerInvStart, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            return stack.copy();
        }
        if (index >= filterStart && index < playerInvStart) {
            slot.setByPlayer(ItemStack.EMPTY);
            return ItemStack.EMPTY;
        }
        if (upgrades.accepts(stack)
                && this.moveItemStackTo(stack, 0, filterStart, false)) {
            ItemStack remaining = stack.copy();
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            return remaining;
        }
        if (upgrades.accepts(stack)) {
            return ItemStack.EMPTY;
        }
        for (int i = 0; i < FILTER_SLOT_COUNT; i++) {
            Slot filterSlot = this.slots.get(filterStart + i);
            if (filterSlot.getItem().isEmpty()) {
                filterSlot.safeInsert(stack, 1);
                return ItemStack.EMPTY;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canDragTo(Slot slot) {
        return !slot.isFake();
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        if (slot.isFake() || slot.getItem() == openedHost) {
            return false;
        }
        return super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        saveFilters();
        saveUpgrades();
    }

    @Override
    public int filterSlotCount() {
        return FILTER_SLOT_COUNT;
    }
}
