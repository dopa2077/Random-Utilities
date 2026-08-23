package com.dopa.randomutilities.filter.menu;

import com.dopa.randomutilities.filter.config.DevNullConfig;
import com.dopa.randomutilities.filter.FilterContents;
import com.dopa.randomutilities.filter.FilterItem;
import com.dopa.randomutilities.filter.FilterNesting;
import com.dopa.randomutilities.filter.FilterProfile;
import com.dopa.randomutilities.filter.FilterRegistry;
import com.dopa.randomutilities.filter.FilterStorage;
import com.dopa.randomutilities.registry.ModMenus;
import com.dopa.randomutilities.registry.ModTriggers;

import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.world.inventory.StackCopySlot;

import java.util.Optional;

public class FilterMenu extends AbstractContainerMenu {
    public static final int BTN_ADD_SLOT = 0;
    public static final int BTN_REMOVE_SLOT = 1;
    public static final int BTN_PREV_PAGE = 2;
    public static final int BTN_NEXT_PAGE = 3;
    public static final int BTN_ADD_ROW = 4;
    public static final int BTN_REMOVE_ROW = 5;
    public static final int BTN_GATHER = 6;
    public static final int DATA_SIZE = 11;

    public static final int BASIC_SLOT_X = 80;
    public static final int BASIC_SLOT_Y = 20;
    public static final int BASIC_PLAYER_INV_Y = 51;

    private final Player player;
    private final InteractionHand hand;
    private final ItemStack openedHost;
    private final FilterProfile profile;
    private final FilterStacksHandler handler;
    private final ContainerData data;
    private final int pageSlotCount;
    private final int rows;
    private int displayPage;
    private final int playerInvStart;
    private final boolean restoreConfigPanel;
    private boolean gatherDirty = true;
    private boolean gatherWouldChange;

    public FilterMenu(int containerId, Inventory playerInv, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInv, FilterOpenSource.decode(buf));
    }

    public FilterMenu(int containerId, Inventory playerInv, InteractionHand hand) {
        this(containerId, playerInv, FilterOpenSource.forHand(hand, null, false));
    }

    private FilterMenu(int containerId, Inventory playerInv, FilterOpenSource source) {
        super(ModMenus.FILTER.get(), containerId);
        this.player = playerInv.player;
        this.hand = source.hand();
        ItemStack host = host();
        this.openedHost = host;
        this.profile = FilterRegistry.profile(host);
        this.data = profile != null && (!profile.isBasic() || profile.colorable())
                ? new SimpleContainerData(DATA_SIZE)
                : null;

        FilterContents contents = source.presetContents();
        if (contents == null) {
            contents = FilterStorage.get(host);
        }
        boolean restoreConfig = source.restoreConfig();

        if (profile == null || profile.isBasic()) {
            this.restoreConfigPanel = false;
            this.pageSlotCount = 1;
            this.rows = 1;
            this.displayPage = 0;
            this.playerInvStart = 1;
            NonNullList<ItemStack> stacks = NonNullList.withSize(1, ItemStack.EMPTY);
            stacks.set(0, contents.stackInSlot(0));
            int maxStack = DevNullConfig.basicMaxStackSize();
            this.handler = new FilterStacksHandler(stacks, DevNullConfig::basicMaxStackSize, true, this::host);
            this.handler.setOnChanged(this::saveBasic);
            this.addSlot(new FilterSlot(handler, 0, BASIC_SLOT_X, BASIC_SLOT_Y, true, maxStack));
            this.addStandardInventorySlots(playerInv, 8, BASIC_PLAYER_INV_Y);
            if (data != null) {
                syncData();
                this.addDataSlots(data);
            }
            return;
        }

        this.restoreConfigPanel = restoreConfig;

        int page = contents.clampedPage();
        this.displayPage = page;
        int start = page * FilterContents.SLOTS_PER_PAGE;
        this.pageSlotCount = FilterPageLayout.pageSlotCount(contents, page);
        this.rows = FilterContents.rowsForSlotCount(pageSlotCount);

        NonNullList<ItemStack> stacks = NonNullList.withSize(pageSlotCount, ItemStack.EMPTY);
        for (int i = 0; i < pageSlotCount; i++) {
            stacks.set(i, contents.stackInSlot(start + i));
        }

        this.handler = new FilterStacksHandler(stacks, () -> FilterStorage.get(host()).maxStackSize(), false, this::host);
        this.handler.setOnChanged(this::savePage);
        for (int i = 0; i < pageSlotCount; i++) {
            this.addSlot(new FilterSlot(handler, i, FilterPageLayout.slotX(i), FilterPageLayout.slotY(i), false, 0));
        }

        this.playerInvStart = pageSlotCount;

        this.addStandardInventorySlots(playerInv, 8, playerInvY());
        syncData();
        this.addDataSlots(data);
    }

    public boolean shouldRestoreConfigPanel() {
        return restoreConfigPanel;
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
        return isBasic() ? BASIC_PLAYER_INV_Y : 19 + rows * 18 + 13;
    }

    public int getPageSlotCount() {
        return pageSlotCount;
    }

    public int getPlayerInvStart() {
        return playerInvStart;
    }

    private ItemStack host() {
        return player.getItemInHand(hand);
    }

    private void saveBasic() {
        ItemStack host = host();
        if (host != openedHost || !FilterRegistry.isFilterItem(host)) {
            return;
        }
        FilterContents contents = FilterStorage.get(host);
        ItemStack stack = handler.getResource(0).toStack(handler.getAmountAsInt(0));
        FilterStorage.set(host, contents.withSlotStack(0, stack));
        gatherDirty = true;
        syncData();
        maybeAwardNestAdvancement(host);
    }

    private void maybeAwardNestAdvancement(ItemStack host) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (FilterNesting.nestingDepth(host) < FilterNesting.ADVANCEMENT_DEPTH) {
            return;
        }
        ModTriggers.DEV_NULL_NEST.get().trigger(serverPlayer);
    }

    private void savePage() {
        ItemStack host = host();
        if (host != openedHost || !FilterRegistry.isFilterItem(host) || profile == null || profile.isBasic()) {
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
        gatherDirty = true;
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
        data.set(3, DevNullConfig.effectivePageCount(contents.slotCount()));
        data.set(4, contents.color());
        data.set(5, contents.selectedSlot());
        data.set(6, pageSlotCount);
        data.set(7, contents.slotCount() > DevNullConfig.advancedMinSlots() ? 1 : 0);
        int lastIndex = contents.slotCount() - 1;
        data.set(8, lastIndex >= 0 && !contents.slot(lastIndex).isEmpty() ? 1 : 0);
        if (gatherDirty) {
            gatherWouldChange = FilterStorage.wouldGatherChange(contents);
            gatherDirty = false;
        }
        data.set(9, gatherWouldChange ? 1 : 0);
        data.set(10, contents.highlightMatchColor() ? 1 : 0);
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

    public boolean isHighlightMatchColor() {
        return data != null && data.get(10) != 0;
    }

    public int getSelectedSlot() {
        return data != null ? data.get(5) : 0;
    }

    public boolean canRemoveSlot() {
        return data != null && data.get(7) != 0;
    }

    public boolean isLastSlotOccupied() {
        return data != null && data.get(8) != 0;
    }

    public boolean wouldGatherChange() {
        return data != null && data.get(9) != 0;
    }

    public boolean wouldGatherVoidItems() {
        if (profile == null || profile.isBasic()) {
            return false;
        }
        return FilterStorage.wouldGatherVoidItems(FilterStorage.get(host()));
    }

    public boolean wouldSingleRemoveVoidItems() {
        return isLastSlotOccupied();
    }

    public boolean wouldBulkRemoveVoidItems() {
        if (profile == null || profile.isBasic()) {
            return false;
        }
        FilterContents contents = FilterStorage.get(host());
        int toRemove = slotsToRemoveForBulk(contents.slotCount());
        for (int i = contents.slotCount() - toRemove; i < contents.slotCount(); i++) {
            if (!contents.slot(i).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public boolean wouldRemoveVoidItems(boolean bulk) {
        return bulk ? wouldBulkRemoveVoidItems() : wouldSingleRemoveVoidItems();
    }

    public void setMaxStackSizeSetting(int value) {
        if (profile == null || !profile.configurableMaxStack()) {
            return;
        }
        savePage();
        FilterContents contents = FilterStorage.get(host()).withMaxStackSize(DevNullConfig.clampAdvancedMaxStack(value));
        FilterStorage.set(host(), contents);
        gatherDirty = true;
        syncData();
        broadcastChanges();
    }

    public boolean isFilterSlotOverCap(int localIndex) {
        if (isBasic() || localIndex < 0 || localIndex >= pageSlotCount || localIndex >= slots.size()) {
            return false;
        }
        ItemStack stack = slots.get(localIndex).getItem();
        return !stack.isEmpty() && stack.getCount() > getMaxStackSizeSetting();
    }

    public void setColorSetting(int rgb) {
        if (profile == null || !profile.colorable()) {
            return;
        }
        FilterStorage.set(host(), FilterStorage.get(host()).withColor(rgb));
        syncData();
        broadcastChanges();
    }

    public void setHighlightMatchColorSetting(boolean match) {
        if (profile == null || !profile.colorable() || !profile.showCosmeticHighlight()) {
            return;
        }
        FilterStorage.set(host(), FilterStorage.get(host()).withHighlightMatchColor(match));
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
                if (contents.slotCount() < DevNullConfig.advancedMaxSlots()) {
                    contents = contents.withSlotCount(
                            contents.slotCount() + 1,
                            DevNullConfig.advancedMinSlots(),
                            DevNullConfig.advancedMaxSlots()
                    );
                    if (contents.slotCount() > (contents.clampedPage() + 1) * FilterContents.SLOTS_PER_PAGE) {
                        contents = contents.withPage(contents.pageCount() - 1);
                    }
                    changed = true;
                }
            }
            case BTN_REMOVE_SLOT -> {
                if (contents.slotCount() > DevNullConfig.advancedMinSlots()) {
                    contents = removeLastSlot(contents);
                    changed = true;
                }
            }
            case BTN_ADD_ROW -> {
                int toAdd = slotsToAddForBulk(contents.slotCount());
                if (contents.slotCount() + toAdd <= DevNullConfig.advancedMaxSlots()) {
                    contents = contents.withSlotCount(
                            contents.slotCount() + toAdd,
                            DevNullConfig.advancedMinSlots(),
                            DevNullConfig.advancedMaxSlots()
                    );
                    if (contents.slotCount() > (contents.clampedPage() + 1) * FilterContents.SLOTS_PER_PAGE) {
                        contents = contents.withPage(contents.pageCount() - 1);
                    }
                    changed = true;
                }
            }
            case BTN_REMOVE_ROW -> {
                int toRemove = slotsToRemoveForBulk(contents.slotCount());
                if (contents.slotCount() - toRemove >= DevNullConfig.advancedMinSlots()) {
                    for (int i = 0; i < toRemove; i++) {
                        contents = removeLastSlot(contents);
                    }
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
                int maxPage = DevNullConfig.effectivePageCount(contents.slotCount()) - 1;
                int next = Math.min(maxPage, contents.clampedPage() + 1);
                if (next != contents.clampedPage()) {
                    contents = contents.withPage(next);
                    changed = true;
                }
            }
            case BTN_GATHER -> {
                contents = FilterStorage.gather(contents);
                changed = true;
            }
            default -> {
                return false;
            }
        }

        if (!changed) {
            return true;
        }

        FilterStorage.set(host(), contents);
        gatherDirty = true;
        if (buttonId == BTN_GATHER) {
            reloadPageFromContents(contents);
            syncData();
            broadcastChanges();
            return true;
        }

        int newPage = contents.clampedPage();
        int start = newPage * FilterContents.SLOTS_PER_PAGE;
        int end = Math.min(contents.slotCount(), start + FilterContents.SLOTS_PER_PAGE);
        int newPageSlotCount = Math.max(1, end - start);
        int newRows = FilterContents.rowsForSlotCount(newPageSlotCount);
        if (newRows == this.rows && newPageSlotCount == this.pageSlotCount) {
            this.displayPage = newPage;
            reloadPageFromContents(contents);
            syncData();
            broadcastChanges();
            return true;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.closeContainer();
            FilterItem.openGui(serverPlayer, hand, true);
        }
        return true;
    }

    private void reloadPageFromContents(FilterContents contents) {
        int start = displayPage * FilterContents.SLOTS_PER_PAGE;
        int slotsToLoad = Math.min(pageSlotCount, Math.max(0, contents.slotCount() - start));
        for (int i = 0; i < pageSlotCount; i++) {
            if (i < slotsToLoad) {
                ItemStack stack = contents.stackInSlot(start + i);
                handler.set(i, ItemResource.of(stack), stack.getCount());
            } else {
                handler.set(i, ItemResource.EMPTY, 0);
            }
        }
    }

    private FilterContents removeLastSlot(FilterContents contents) {
        int lastIndex = contents.slotCount() - 1;
        int pageStart = displayPage * FilterContents.SLOTS_PER_PAGE;
        if (lastIndex >= pageStart && lastIndex < pageStart + pageSlotCount) {
            int local = lastIndex - pageStart;
            ItemStack stack = handler.getResource(local).toStack(handler.getAmountAsInt(local));
            contents = contents.withSlotStack(lastIndex, stack);
        }
        contents = contents.withSlotStack(lastIndex, ItemStack.EMPTY);
        return contents.withSlotCount(
                contents.slotCount() - 1,
                DevNullConfig.advancedMinSlots(),
                DevNullConfig.advancedMaxSlots()
        );
    }

    static int slotsToAddForBulk(int slotCount) {
        return FilterPageLayout.slotsToAddForBulk(slotCount);
    }

    static int slotsToRemoveForBulk(int slotCount) {
        return FilterPageLayout.slotsToRemoveForBulk(slotCount);
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput containerInput, Player player) {
        if (isBasic() && slotId == 0 && containerInput == ContainerInput.PICKUP && tryBasicTrashClick(button)) {
            return;
        }
        super.clicked(slotId, button, containerInput, player);
    }

    private boolean tryBasicTrashClick(int button) {
        if (button != 0 && button != 1) {
            return false;
        }
        Slot slot = this.slots.getFirst();
        ItemStack carried = this.getCarried();
        ItemStack slotStack = slot.getItem();
        if (carried.isEmpty()) {
            return false;
        }
        if (!slotStack.isEmpty() && !ItemStack.isSameItemSameComponents(slotStack, carried)) {
            return false;
        }
        if (!slot.mayPlace(carried)) {
            return false;
        }
        int amount = button == 0 ? carried.getCount() : 1;
        this.setCarried(slot.safeInsert(carried, amount));
        slot.setChanged();
        return true;
    }

    @Override
    protected boolean moveItemStackTo(ItemStack stack, int startIndex, int endIndex, boolean reverseDirection) {
        if (startIndex == 0 && endIndex == pageSlotCount && !stack.isEmpty()) {
            return moveIntoFilterSlots(stack, reverseDirection);
        }
        return super.moveItemStackTo(stack, startIndex, endIndex, reverseDirection);
    }

    private boolean moveIntoFilterSlots(ItemStack stack, boolean reverseDirection) {
        boolean changed = false;
        int startSlot = reverseDirection ? pageSlotCount - 1 : 0;
        int endSlot = reverseDirection ? -1 : pageSlotCount;
        int step = reverseDirection ? -1 : 1;

        for (int i = startSlot; i != endSlot; i += step) {
            Slot slot = this.slots.get(i);
            if (!slot.getItem().isEmpty() && ItemStack.isSameItemSameComponents(stack, slot.getItem())) {
                int before = stack.getCount();
                slot.safeInsert(stack, before);
                if (stack.getCount() < before) {
                    changed = true;
                }
            }
        }

        if (!stack.isEmpty()) {
            for (int i = startSlot; i != endSlot; i += step) {
                Slot slot = this.slots.get(i);
                if (slot.getItem().isEmpty() && slot.mayPlace(stack)) {
                    int before = stack.getCount();
                    slot.safeInsert(stack, before);
                    if (stack.getCount() < before) {
                        changed = true;
                        break;
                    }
                }
            }
        }

        return changed;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack raw = slot.getItem();
            result = raw.copy();
            if (index < pageSlotCount) {
                int moveCount = Math.min(raw.getCount(), raw.getMaxStackSize());
                ItemStack toMove = raw.copyWithCount(moveCount);
                int before = toMove.getCount();
                if (!this.moveItemStackTo(toMove, playerInvStart, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
                int moved = before - toMove.getCount();
                if (moved <= 0) {
                    return ItemStack.EMPTY;
                }
                raw.shrink(moved);
                result = toMove;
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
        return player.getItemInHand(hand) == openedHost
                && FilterRegistry.isFilterItem(openedHost);
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

    private static final class FilterSlot extends StackCopySlot {
        private final FilterStacksHandler handler;
        private final int handlerIndex;
        private final boolean trashMode;
        private final int trashDisplayCount;

        FilterSlot(
                FilterStacksHandler handler,
                int handlerIndex,
                int xPosition,
                int yPosition,
                boolean trashMode,
                int trashDisplayCount
        ) {
            super(handlerIndex, xPosition, yPosition);
            this.handler = handler;
            this.handlerIndex = handlerIndex;
            this.trashMode = trashMode;
            this.trashDisplayCount = trashDisplayCount;
        }

        @Override
        protected ItemStack getStackCopy() {
            return handler.getResource(handlerIndex).toStack(handler.getAmountAsInt(handlerIndex));
        }

        @Override
        protected void setStackCopy(ItemStack stack) {
            if (trashMode && !stack.isEmpty()) {
                int limit = trashSlotLimit(stack);
                stack = stack.copyWithCount(Math.min(stack.getCount(), limit));
            }
            handler.set(handlerIndex, ItemResource.of(stack), stack.getCount());
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return !stack.isEmpty() && handler.isValid(handlerIndex, ItemResource.of(stack));
        }

        @Override
        public boolean mayPickup(Player player) {
            return !getItem().isEmpty();
        }

        private int softCap() {
            return handler.getCapacityAsInt(handlerIndex, ItemResource.EMPTY);
        }

        private boolean isAtOrOverSoftCap() {
            return handler.getAmountAsInt(handlerIndex) >= softCap();
        }

        /** Filter display max, further limited by {@link DevNullConfig#effectiveSlotCapacity}. */
        private int trashSlotLimit(ItemStack forItem) {
            int effective = handler.getCapacityAsInt(handlerIndex, ItemResource.of(forItem));
            return Math.min(trashDisplayCount, Math.max(1, effective));
        }

        private int trashInsertCapacity(ItemStack incoming) {
            ItemStack current = getStackCopy();
            int limit = trashSlotLimit(incoming.isEmpty() ? current : incoming);
            if (current.isEmpty()) {
                return limit;
            }
            return Math.max(0, limit - current.getCount());
        }

        private int trashItemMaxStackSize(ItemStack stack) {
            if (!stack.isEmpty()) {
                return stack.getMaxStackSize();
            }
            ItemStack current = getStackCopy();
            return current.isEmpty() ? trashDisplayCount : current.getMaxStackSize();
        }

        private int trashReportedMaxStackSize(ItemStack stack) {
            int limit = trashSlotLimit(stack.isEmpty() ? getStackCopy() : stack);
            int current = getStackCopy().getCount();
            if (current < limit) {
                return limit;
            }
            // At limit: still report room so inserts can void extras into /dev/null.
            return current + trashItemMaxStackSize(stack);
        }

        @Override
        public int getMaxStackSize() {
            if (trashMode) {
                return trashReportedMaxStackSize(ItemStack.EMPTY);
            }
            if (isAtOrOverSoftCap()) {
                return 0;
            }
            return softCap();
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            if (trashMode) {
                return trashReportedMaxStackSize(stack);
            }
            if (isAtOrOverSoftCap()) {
                return 0;
            }
            return handler.getCapacityAsInt(handlerIndex, ItemResource.of(stack));
        }

        @Override
        public Optional<ItemStack> tryRemove(int amount, int maxAmount, Player player) {
            ItemStack current = getItem();
            if (current.isEmpty()) {
                return Optional.empty();
            }
            int vanillaStack = current.getMaxStackSize();
            int capped = capVanillaPickupAmount(amount, current.getCount(), vanillaStack);
            return super.tryRemove(capped, maxAmount, player);
        }

        @Override
        public ItemStack safeInsert(ItemStack inputStack, int inputAmount) {
            if (!trashMode) {
                if (isAtOrOverSoftCap()) {
                    return inputStack;
                }
                return super.safeInsert(inputStack, inputAmount);
            }
            if (inputStack.isEmpty() || !mayPlace(inputStack)) {
                return inputStack;
            }
            ItemStack slotStack = getStackCopy();
            if (!slotStack.isEmpty() && !ItemStack.isSameItemSameComponents(slotStack, inputStack)) {
                return inputStack;
            }
            int consume = Math.min(inputAmount, inputStack.getCount());
            if (consume <= 0) {
                return inputStack;
            }
            int capacity = trashInsertCapacity(inputStack);
            if (capacity <= 0) {
                ItemStack filterItem = inputStack.copy();
                inputStack.shrink(consume);
                if (slotStack.isEmpty()) {
                    set(filterItem.copyWithCount(Math.min(consume, trashSlotLimit(filterItem))));
                }
                return inputStack;
            }
            consume = Math.min(consume, capacity);
            ItemStack filterItem = inputStack.copy();
            inputStack.shrink(consume);
            int newCount = slotStack.isEmpty() ? consume : slotStack.getCount() + consume;
            set(filterItem.copyWithCount(newCount));
            return inputStack;
        }

        private static int capVanillaPickupAmount(int requested, int slotCount, int vanillaStackSize) {
            if (requested <= 0 || slotCount <= 0) {
                return 0;
            }
            int halfOfSlot = (slotCount + 1) / 2;
            if (requested == halfOfSlot) {
                return slotCount > vanillaStackSize
                        ? Math.min(halfOfSlot, (vanillaStackSize + 1) / 2)
                        : requested;
            }
            if (requested >= slotCount || requested >= vanillaStackSize) {
                return Math.min(slotCount, vanillaStackSize);
            }
            return Math.min(requested, vanillaStackSize);
        }
    }
}
