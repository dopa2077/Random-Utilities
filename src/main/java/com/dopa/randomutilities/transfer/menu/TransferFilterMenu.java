package com.dopa.randomutilities.transfer.menu;

import com.dopa.randomutilities.filter.menu.GhostFilterHandler;
import com.dopa.randomutilities.filter.menu.GhostFilterSlot;
import com.dopa.randomutilities.registry.ModMenus;
import com.dopa.randomutilities.transfer.TransferFilterContents;
import com.dopa.randomutilities.transfer.TransferFilterItem;

import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;

public class TransferFilterMenu extends AbstractContainerMenu {
    public static final int SLOT_COUNT = TransferFilterContents.SIZE;
    public static final int GRID = 4;
    /** 16×16 wells (1px smaller than a vanilla 18×18 slot), pitched 16. */
    public static final int SLOT = 16;
    /** Aligned to filter.png ghost wells (one slot left of the previous layout). */
    public static final int GRID_X = 7;
    public static final int GRID_Y = 21;
    /** Baked button panel on filter.png (right of the 4×4 grid). */
    public static final int BUTTON_PANEL_X = 89;
    public static final int BUTTON_PANEL_Y = 21;
    public static final int BUTTON_PANEL_WIDTH = 79;
    public static final int BUTTON_PANEL_HEIGHT = 62;
    public static final int BUTTON_WIDTH = 77;
    public static final int BUTTON_HEIGHT = 18;
    public static final int BUTTON_GAP = 4;
    public static final int BUTTON_X = BUTTON_PANEL_X + (BUTTON_PANEL_WIDTH - BUTTON_WIDTH) / 2;
    public static final int PLAYER_INV_Y = 102;
    public static final int IMAGE_WIDTH = 176;
    public static final int IMAGE_HEIGHT = 184;

    public static final int BTN_NBT = 0;
    public static final int BTN_META = 1;
    public static final int BTN_ORE_DICT = 2;

    public static final int DATA_NBT = 0;
    public static final int DATA_META = 1;
    public static final int DATA_ORE_DICT = 2;
    public static final int DATA_SIZE = 3;

    private final Player player;
    private final InteractionHand hand;
    private final GhostFilterHandler handler;
    private final ContainerData data;
    private final int playerInvStart;

    public TransferFilterMenu(int containerId, Inventory playerInv, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInv, buf.readEnum(InteractionHand.class));
    }

    public TransferFilterMenu(int containerId, Inventory playerInv, InteractionHand hand) {
        super(ModMenus.TRANSFER_FILTER.get(), containerId);
        this.player = playerInv.player;
        this.hand = hand;

        TransferFilterContents contents = TransferFilterContents.get(host());
        NonNullList<ItemStack> stacks = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        for (int i = 0; i < SLOT_COUNT; i++) {
            stacks.set(i, contents.slot(i));
        }
        this.handler = new GhostFilterHandler(stacks);
        this.handler.setOnChanged(this::saveContents);

        for (int row = 0; row < GRID; row++) {
            for (int col = 0; col < GRID; col++) {
                int index = row * GRID + col;
                this.addSlot(new GhostFilterSlot(
                        handler,
                        index,
                        GRID_X + col * SLOT,
                        GRID_Y + row * SLOT,
                        stack -> TransferFilterContents.canPlaceIn(host(), stack)
                ));
            }
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

    private void saveContents() {
        TransferFilterContents contents = TransferFilterContents.get(host());
        for (int i = 0; i < SLOT_COUNT; i++) {
            contents = contents.withSlot(i, handler.getResource(i).toStack(handler.getAmountAsInt(i)));
        }
        TransferFilterContents.set(host(), contents);
        syncData();
    }

    private void syncData() {
        TransferFilterContents contents = TransferFilterContents.get(host());
        data.set(DATA_NBT, contents.matchNbt() ? 1 : 0);
        data.set(DATA_META, contents.matchMeta() ? 1 : 0);
        data.set(DATA_ORE_DICT, contents.matchOreDict() ? 1 : 0);
    }

    @Override
    public void broadcastChanges() {
        syncData();
        super.broadcastChanges();
    }

    public boolean matchNbt() {
        return data.get(DATA_NBT) != 0;
    }

    public boolean matchMeta() {
        return data.get(DATA_META) != 0;
    }

    public boolean matchOreDict() {
        return data.get(DATA_ORE_DICT) != 0;
    }

    public void setFilterSlot(int index, ItemStack stack) {
        if (index < 0 || index >= SLOT_COUNT || stack.isEmpty()) {
            return;
        }
        Slot slot = this.slots.get(index);
        if (!slot.mayPlace(stack)) {
            return;
        }
        slot.safeInsert(stack.copyWithCount(1), 1);
        broadcastChanges();
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        TransferFilterContents contents = TransferFilterContents.get(host());
        contents = switch (buttonId) {
            case BTN_NBT -> contents.withMatchNbt(!contents.matchNbt());
            case BTN_META -> contents.withMatchMeta(!contents.matchMeta());
            case BTN_ORE_DICT -> contents.withMatchOreDict(!contents.matchOreDict());
            default -> contents;
        };
        TransferFilterContents.set(host(), contents);
        syncData();
        broadcastChanges();
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getItemInHand(hand).getItem() instanceof TransferFilterItem;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        if (index < playerInvStart) {
            slot.setByPlayer(ItemStack.EMPTY);
            return ItemStack.EMPTY;
        }
        for (int i = 0; i < playerInvStart; i++) {
            Slot filterSlot = this.slots.get(i);
            if (filterSlot.getItem().isEmpty() && filterSlot.mayPlace(stack)) {
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
        if (slot.isFake()) {
            return false;
        }
        return super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        saveContents();
    }
}
