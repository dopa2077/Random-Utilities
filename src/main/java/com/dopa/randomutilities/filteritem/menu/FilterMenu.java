package com.dopa.randomutilities.filteritem.menu;

import com.dopa.randomutilities.filteritem.FilterContents;
import com.dopa.randomutilities.filteritem.FilterItem;
import com.dopa.randomutilities.filteritem.FilterProfile;
import com.dopa.randomutilities.filteritem.FilterRegistry;
import com.dopa.randomutilities.filteritem.FilterStorage;
import com.dopa.randomutilities.registry.ModMenus;

import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

public class FilterMenu extends AbstractContainerMenu {
    public static final int BTN_ADD_SLOT = 0;
    public static final int BTN_REMOVE_SLOT = 1;
    public static final int BTN_PREV_PAGE = 2;
    public static final int BTN_NEXT_PAGE = 3;
    public static final int DATA_SIZE = 9;

    public static final int BASIC_SLOT_X = 80;
    public static final int BASIC_SLOT_Y = 18;
    public static final int BASIC_PLAYER_INV_Y = 49;

    private final Player player;
    private final InteractionHand hand;
    private final FilterProfile profile;
    private final FilterStacksHandler handler;
    private final ContainerData data;
    private final int pageSlotCount;
    private final int rows;
    private final int displayPage;

    public FilterMenu(int containerId, Inventory playerInv, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInv, buf.readEnum(InteractionHand.class), null, buf);
    }

    public FilterMenu(int containerId, Inventory playerInv, InteractionHand hand) {
        this(containerId, playerInv, hand, FilterStorage.get(playerInv.player.getItemInHand(hand)), null);
    }

    private FilterMenu(
            int containerId,
            Inventory playerInv,
            InteractionHand hand,
            FilterContents presetContents,
            RegistryFriendlyByteBuf buf
    ) {
        super(ModMenus.FILTER.get(), containerId);
        this.player = playerInv.player;
        this.hand = hand;
        ItemStack host = player.getItemInHand(hand);
        this.profile = FilterRegistry.profile(host);
        this.data = profile != null && !profile.isBasic() ? new SimpleContainerData(DATA_SIZE) : null;

        FilterContents contents = presetContents;
        if (contents == null && profile != null && !profile.isBasic() && buf != null) {
            contents = FilterContents.STREAM_CODEC.decode(buf);
        }
        if (contents == null) {
            contents = FilterStorage.get(host);
        }

        if (profile == null || profile.isBasic()) {
            this.pageSlotCount = 1;
            this.rows = 1;
            this.displayPage = 0;
            NonNullList<ItemStack> stacks = NonNullList.withSize(1, ItemStack.EMPTY);
            stacks.set(0, contents.stackInSlot(0));
            int maxStack = profile != null && profile.fixedMaxStack() > 0 ? profile.fixedMaxStack() : 64;
            this.handler = new FilterStacksHandler(stacks, () -> maxStack);
            this.handler.setOnChanged(this::saveBasic);
            this.addSlot(new ResourceHandlerSlot(handler, handler::set, 0, BASIC_SLOT_X, BASIC_SLOT_Y));
            this.addStandardInventorySlots(playerInv, 8, BASIC_PLAYER_INV_Y);
            return;
        }

        int page = contents.clampedPage();
        this.displayPage = page;
        int start = page * FilterContents.SLOTS_PER_PAGE;
        int end = Math.min(contents.slotCount(), start + FilterContents.SLOTS_PER_PAGE);
        this.pageSlotCount = Math.max(1, end - start);
        this.rows = FilterContents.rowsForSlotCount(pageSlotCount);

        NonNullList<ItemStack> stacks = NonNullList.withSize(pageSlotCount, ItemStack.EMPTY);
        for (int i = 0; i < pageSlotCount; i++) {
            stacks.set(i, contents.stackInSlot(start + i));
        }

        this.handler = new FilterStacksHandler(stacks, () -> FilterStorage.get(player.getItemInHand(hand)).maxStackSize());
        this.handler.setOnChanged(this::savePage);
        for (int i = 0; i < pageSlotCount; i++) {
            int col = i % 9;
            int row = i / 9;
            this.addSlot(new ResourceHandlerSlot(handler, handler::set, i, 8 + col * 18, 18 + row * 18));
        }
        this.addStandardInventorySlots(playerInv, 8, playerInvY());
        syncData();
        this.addDataSlots(data);
    }

    public FilterProfile profile() {
        return profile;
    }

    public boolean isBasic() {
        return profile == null || profile.isBasic();
    }

    public int getRows() {
        return rows;
    }

    public int playerInvY() {
        return isBasic() ? BASIC_PLAYER_INV_Y : 18 + rows * 18 + 13;
    }

    public int getPageSlotCount() {
        return pageSlotCount;
    }

    private ItemStack host() {
        return player.getItemInHand(hand);
    }

    private void saveBasic() {
        ItemStack host = host();
        if (!FilterRegistry.isFilterItem(host)) {
            return;
        }
        FilterContents contents = FilterStorage.get(host);
        ItemStack stack = handler.getResource(0).toStack(handler.getAmountAsInt(0));
        FilterStorage.set(host, contents.withSlotStack(0, stack));
    }

    private void savePage() {
        ItemStack host = host();
        if (profile == null || profile.isBasic()) {
            return;
        }
        FilterContents contents = FilterStorage.get(host);
        int start = displayPage * FilterContents.SLOTS_PER_PAGE;
        int slotsToSave = Math.min(pageSlotCount, Math.max(0, contents.slotCount() - start));
        for (int i = 0; i < slotsToSave; i++) {
            ItemStack stack = handler.getResource(i).toStack(handler.getAmountAsInt(i));
            contents = contents.withSlotStack(start + i, stack);
        }
        FilterStorage.set(host, contents);
        syncData();
    }

    private void syncData() {
        if (data == null) {
            return;
        }
        FilterContents contents = FilterStorage.get(host());
        data.set(0, contents.maxStackSize());
        data.set(1, contents.slotCount());
        data.set(2, contents.clampedPage());
        data.set(3, contents.pageCount());
        data.set(4, contents.color());
        data.set(5, contents.selectedSlot());
        data.set(6, pageSlotCount);
        data.set(7, contents.slotCount() > profile.minSlots() ? 1 : 0);
        int lastIndex = contents.slotCount() - 1;
        data.set(8, lastIndex >= 0 && !contents.slot(lastIndex).isEmpty() ? 1 : 0);
    }

    public int getMaxStackSizeSetting() {
        return data != null ? data.get(0) : 64;
    }

    public int getSlotCountSetting() {
        return data != null ? data.get(1) : 1;
    }

    public int getPage() {
        return data != null ? data.get(2) : 0;
    }

    public int getPageCount() {
        return data != null ? data.get(3) : 1;
    }

    public int getColor() {
        return data != null ? data.get(4) : FilterContents.DEFAULT_COLOR;
    }

    public boolean canRemoveSlot() {
        return data != null && data.get(7) != 0;
    }

    public boolean isLastSlotOccupied() {
        return data != null && data.get(8) != 0;
    }

    public void setMaxStackSizeSetting(int value) {
        if (profile == null || !profile.configurableMaxStack()) {
            return;
        }
        savePage();
        FilterContents contents = FilterStorage.get(host()).withMaxStackSize(value);
        FilterStorage.set(host(), contents);
        for (int i = 0; i < pageSlotCount; i++) {
            ItemStack stack = handler.getResource(i).toStack(handler.getAmountAsInt(i));
            if (!stack.isEmpty() && stack.getCount() > contents.maxStackSize()) {
                handler.set(i, handler.getResource(i), contents.maxStackSize());
            }
        }
        syncData();
        broadcastChanges();
    }

    public void setColorSetting(int rgb) {
        if (profile == null || !profile.colorable()) {
            return;
        }
        FilterStorage.set(host(), FilterStorage.get(host()).withColor(rgb));
        syncData();
        broadcastChanges();
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (profile == null || !profile.expandable()) {
            return false;
        }
        savePage();
        FilterContents contents = FilterStorage.get(host());
        boolean changed = false;

        switch (buttonId) {
            case BTN_ADD_SLOT -> {
                if (contents.slotCount() < profile.maxSlots()) {
                    contents = contents.withSlotCount(contents.slotCount() + 1, profile.minSlots());
                    if (contents.slotCount() > (contents.clampedPage() + 1) * FilterContents.SLOTS_PER_PAGE) {
                        contents = contents.withPage(contents.pageCount() - 1);
                    }
                    changed = true;
                }
            }
            case BTN_REMOVE_SLOT -> {
                if (contents.slotCount() > profile.minSlots()) {
                    int lastIndex = contents.slotCount() - 1;
                    int pageStart = displayPage * FilterContents.SLOTS_PER_PAGE;
                    if (lastIndex >= pageStart && lastIndex < pageStart + pageSlotCount) {
                        int local = lastIndex - pageStart;
                        ItemStack stack = handler.getResource(local).toStack(handler.getAmountAsInt(local));
                        contents = contents.withSlotStack(lastIndex, stack);
                    }
                    contents = contents.withSlotStack(lastIndex, ItemStack.EMPTY);
                    contents = contents.withSlotCount(contents.slotCount() - 1, profile.minSlots());
                    changed = true;
                }
            }
            case BTN_PREV_PAGE -> {
                int next = Math.max(0, contents.clampedPage() - 1);
                if (next != contents.clampedPage()) {
                    contents = contents.withPage(next);
                    changed = true;
                }
            }
            case BTN_NEXT_PAGE -> {
                int next = Math.min(contents.pageCount() - 1, contents.clampedPage() + 1);
                if (next != contents.clampedPage()) {
                    contents = contents.withPage(next);
                    changed = true;
                }
            }
            default -> {
                return false;
            }
        }

        if (!changed) {
            return true;
        }

        FilterStorage.set(host(), contents);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.closeContainer();
            FilterItem.openGui(serverPlayer, hand);
        }
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack raw = slot.getItem();
            result = raw.copy();
            if (index < pageSlotCount) {
                if (!this.moveItemStackTo(raw, pageSlotCount, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(raw, 0, pageSlotCount, false)) {
                return ItemStack.EMPTY;
            }
            if (raw.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return FilterRegistry.isFilterItem(player.getItemInHand(hand));
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (isBasic()) {
            saveBasic();
        } else {
            savePage();
        }
    }
}
