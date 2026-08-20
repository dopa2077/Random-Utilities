package com.dopa.randomutilities.transfer;

import com.dopa.randomutilities.machine.RedstoneMode;
import com.dopa.randomutilities.machine.config.UpgradeConfig;
import com.dopa.randomutilities.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.model.data.ModelData;
import net.neoforged.neoforge.model.data.ModelProperty;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class TransferNodeBlockEntity extends BlockEntity {
    public static final int FILTER_SLOT_COUNT = 8;
    public static final ModelProperty<Integer> HEADS = new ModelProperty<>();
    public static final ModelProperty<Integer> KINDS = new ModelProperty<>();
    public static final ModelProperty<Integer> PIPE_CHANNEL = new ModelProperty<>();
    static final int TRANSFERS_PER_SAVE = 20;

    private final Head[] heads = new Head[Direction.values().length];
    private int headMask;
    private TransferChannel pipeChannel = TransferChannel.NONE;
    private int unsavedTransfers;

    public TransferNodeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TRANSFER_NODE.get(), pos, state);
        for (int i = 0; i < heads.length; i++) {
            heads[i] = new Head();
            heads[i].upgrades.setOnChanged(this::setChanged);
        }
    }

    public Head head(Direction direction) {
        return heads[direction.ordinal()];
    }

    public int headMask() {
        return headMask;
    }

    public static boolean hasHead(int mask, Direction direction) {
        return (mask & (1 << direction.ordinal())) != 0;
    }

    public boolean hasHead(Direction direction) {
        return hasHead(headMask, direction);
    }

    public boolean setHead(Direction direction, boolean present) {
        return setHead(direction, present, HeadKind.ITEM);
    }

    public boolean setHead(Direction direction, HeadKind kind) {
        return setHead(direction, true, kind);
    }

    public boolean setHead(Direction direction, boolean present, HeadKind kind) {
        int bit = 1 << direction.ordinal();
        int next = present ? headMask | bit : headMask & ~bit;
        HeadKind nextKind = present ? (kind == null ? HeadKind.ITEM : kind) : head(direction).kind;
        boolean kindChanged = present && head(direction).kind != nextKind;
        if (next == headMask && !kindChanged) {
            return false;
        }
        headMask = next;
        if (present) {
            head(direction).kind = nextKind;
            head(direction).networkDirty = true;
        } else {
            head(direction).setDestinations(List.of());
        }
        setChanged();
        syncHeads();
        return true;
    }

    public int headCount() {
        return Integer.bitCount(headMask);
    }

    public TransferChannel pipeChannel() {
        return pipeChannel;
    }

    public void setPipeChannel(TransferChannel channel) {
        TransferChannel next = channel == null ? TransferChannel.NONE : channel;
        if (pipeChannel == next) {
            return;
        }
        pipeChannel = next;
        setChanged();
        syncHeads();
    }

    public int kindMask() {
        int packed = 0;
        for (Direction direction : Direction.values()) {
            if (!hasHead(direction)) {
                continue;
            }
            packed |= head(direction).kind.ordinal() << (direction.ordinal() * 2);
        }
        return packed;
    }

    @Override
    public ModelData getModelData() {
        return ModelData.builder()
                .with(HEADS, headMask)
                .with(KINDS, kindMask())
                .with(PIPE_CHANNEL, pipeChannel.ordinal())
                .build();
    }

    private void refreshHeadRender() {
        requestModelDataUpdate();
        if (level != null && level.isClientSide()) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
        }
    }

    private void syncHeads() {
        if (level == null) {
            return;
        }
        if (level.isClientSide()) {
            refreshHeadRender();
            return;
        }
        BlockState state = getBlockState();
        level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
        level.updateNeighborsAt(worldPosition, state.getBlock());
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = worldPosition.relative(direction);
            BlockState neighbor = level.getBlockState(neighborPos);
            if (neighbor.getBlock() instanceof TransferPipeBlock) {
                level.sendBlockUpdated(neighborPos, neighbor, neighbor, Block.UPDATE_CLIENTS);
            }
        }
    }

    public Component getDisplayName() {
        return Component.translatable("container.dopasrandomutilities.transfer_node");
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TransferNodeBlockEntity be) {
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            TransferNodeLogic.tick(serverLevel, pos, state, be);
        }
    }

    void refreshNetwork(net.minecraft.server.level.ServerLevel level, Direction facing) {
        Head head = head(facing);
        if (!head.networkDirty) {
            return;
        }
        TransferNetworks.rebuildNow(level, worldPosition);
    }

    void receiveIsland(TransferNetworks.Island island) {
        for (Direction direction : Direction.values()) {
            Head current = head(direction);
            if (!hasHead(direction)) {
                current.setDestinations(List.of());
                continue;
            }
            List<TransferNetworks.Destination> pool = switch (current.kind()) {
                case ITEM -> island.items();
                case FLUID -> island.fluids();
                case ENERGY -> island.energy();
            };
            BlockPos sourcePos = worldPosition.relative(direction);
            List<TransferNetworks.Destination> filtered = new ArrayList<>(pool.size());
            for (TransferNetworks.Destination dest : pool) {
                if (!dest.inventoryPos().equals(sourcePos)) {
                    filtered.add(dest);
                }
            }
            current.setDestinations(filtered);
        }
    }

    void noteTransfer() {
        if (++unsavedTransfers >= TRANSFERS_PER_SAVE) {
            unsavedTransfers = 0;
            setChanged();
        }
    }

    public int insertUpgrade(Direction face, ItemStack stack) {
        Head head = head(face);
        if (!TransferNodeUpgradeInventory.isNodeUpgrade(head.kind(), stack)) {
            return 0;
        }
        return head.upgrades.insertFrom(stack);
    }

    public void dropUpgrades() {
        if (level == null) {
            return;
        }
        for (Head head : heads) {
            head.upgrades.dropAt(level, worldPosition);
        }
    }

    public void dropUpgrades(Direction face) {
        if (level == null) {
            return;
        }
        head(face).upgrades.dropAt(level, worldPosition);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null && !level.isClientSide()) {
            dropUpgrades();
        }
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        boolean migrated = false;
        for (Direction direction : Direction.values()) {
            Head head = head(direction);
            String prefix = direction.getSerializedName();
            boolean found = false;
            for (int i = 0; i < FILTER_SLOT_COUNT; i++) {
                ItemStack stack = input.read(prefix + "Filter" + i, ItemStack.CODEC).orElse(ItemStack.EMPTY);
                head.filterSlots.set(i, stack);
                found = found || !stack.isEmpty();
            }
            head.destCursor = input.getIntOr(prefix + "DestCursor", 0);
            head.backoff = input.getIntOr(prefix + "Backoff", 0);
            head.redstoneMode = RedstoneMode.byOrdinal(input.getIntOr(prefix + "RedstoneMode", 0));
            head.upgrades.loadSlots(input, prefix + "Upgrade");
            if (input.getBooleanOr(prefix + "HasWhitelist", false) || found) {
                head.whitelistMode = input.getBooleanOr(prefix + "WhitelistMode", true);
                migrated = true;
            }
            head.networkDirty = true;
        }
        headMask = input.getIntOr("Heads", 0);
        pipeChannel = TransferChannel.values()[Math.clamp(input.getIntOr("PipeChannel", 0), 0, TransferChannel.values().length - 1)];
        for (Direction direction : Direction.values()) {
            Head current = head(direction);
            String prefix = direction.getSerializedName();
            current.kind = HeadKind.byOrdinal(input.getIntOr(prefix + "Kind", HeadKind.ITEM.ordinal()));
            current.lastEnergyPulled = input.getIntOr(prefix + "LastEnergy", 0);
        }
        if (!migrated) {
            Head legacy = head(legacyFacing());
            for (int i = 0; i < FILTER_SLOT_COUNT; i++) {
                legacy.filterSlots.set(i, input.read("Filter" + i, ItemStack.CODEC).orElse(ItemStack.EMPTY));
            }
            legacy.destCursor = input.getIntOr("DestCursor", 0);
            legacy.backoff = input.getIntOr("Backoff", 0);
            legacy.whitelistMode = input.getBooleanOr("WhitelistMode", true);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        for (Direction direction : Direction.values()) {
            Head head = head(direction);
            String prefix = direction.getSerializedName();
            for (int i = 0; i < FILTER_SLOT_COUNT; i++) {
                ItemStack stack = head.filterSlots.get(i);
                if (!stack.isEmpty()) {
                    output.store(prefix + "Filter" + i, ItemStack.CODEC, stack);
                }
            }
            output.putInt(prefix + "DestCursor", head.destCursor);
            output.putInt(prefix + "Backoff", head.backoff);
            output.putInt(prefix + "RedstoneMode", head.redstoneMode.ordinal());
            output.putBoolean(prefix + "WhitelistMode", head.whitelistMode);
            output.putBoolean(prefix + "HasWhitelist", true);
            output.putInt(prefix + "Kind", head.kind.ordinal());
            output.putInt(prefix + "LastEnergy", head.lastEnergyPulled);
            head.upgrades.saveSlots(output, prefix + "Upgrade");
        }
        output.putInt("Heads", headMask);
        output.putInt("PipeChannel", pipeChannel.ordinal());
    }

    private Direction legacyFacing() {
        for (Direction direction : Direction.values()) {
            if (hasHead(direction)) {
                return direction;
            }
        }
        return Direction.NORTH;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Heads", headMask);
        tag.putInt("Kinds", kindMask());
        tag.putInt("PipeChannel", pipeChannel.ordinal());
        return tag;
    }

    @Override
    public void handleUpdateTag(ValueInput input) {
        applyHeadSync(input);
    }

    @Override
    public void onDataPacket(Connection net, ValueInput valueInput) {
        applyHeadSync(valueInput);
    }

    private void applyHeadSync(ValueInput input) {
        int next = input.getIntOr("Heads", headMask);
        int kinds = input.getIntOr("Kinds", kindMask());
        TransferChannel nextChannel = TransferChannel.values()[Math.clamp(
                input.getIntOr("PipeChannel", pipeChannel.ordinal()),
                0,
                TransferChannel.values().length - 1
        )];
        boolean changed = next != headMask || kinds != kindMask() || nextChannel != pipeChannel;
        headMask = next;
        pipeChannel = nextChannel;
        for (Direction direction : Direction.values()) {
            head(direction).kind = HeadKind.byOrdinal((kinds >> (direction.ordinal() * 2)) & 3);
        }
        if (changed) {
            refreshHeadRender();
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public static final class Head {
        private HeadKind kind = HeadKind.ITEM;
        private final NonNullList<ItemStack> filterSlots = NonNullList.withSize(FILTER_SLOT_COUNT, ItemStack.EMPTY);
        private final TransferNodeUpgradeInventory upgrades = new TransferNodeUpgradeInventory(() -> kind);
        private List<TransferNetworks.Destination> destinations = List.of();
        @Nullable
        private ResourceHandler<ItemResource>[] destHandlers;
        @Nullable
        private BlockEntity[] destBlockEntities;
        @Nullable
        private BlockState[] destBlockStates;
        private boolean networkDirty = true;
        private boolean whitelistMode = true;
        private RedstoneMode redstoneMode = RedstoneMode.IGNORE;
        private int destCursor;
        private int backoff;
        private int lastEnergyPulled;
        private ItemStack transferredDisplay = ItemStack.EMPTY;

        public HeadKind kind() {
            return kind;
        }

        public NonNullList<ItemStack> filterSlots() {
            return filterSlots;
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
        }

        public boolean whitelistMode() {
            return whitelistMode;
        }

        public void setWhitelistMode(boolean whitelistMode) {
            this.whitelistMode = whitelistMode;
        }

        public TransferNodeUpgradeInventory upgrades() {
            return upgrades;
        }

        public RedstoneMode redstoneMode() {
            return redstoneMode;
        }

        public void setRedstoneMode(RedstoneMode mode) {
            this.redstoneMode = mode == null ? RedstoneMode.IGNORE : mode;
        }

        int transferInterval() {
            return UpgradeConfig.transferNodeInterval(kind, upgrades.overclockCount());
        }

        int transferBudget() {
            return upgrades.stackCount() > 0 ? TransferNodeLogic.STACK_TRANSFER_ITEMS : 1;
        }

        int energyBudget() {
            return UpgradeConfig.transferNodeEnergyAmount(upgrades.energyCount());
        }

        int fluidBudget() {
            return UpgradeConfig.transferNodeFluidAmount(upgrades.fluidCapacityCount());
        }

        public int lastEnergyPulled() {
            return lastEnergyPulled;
        }

        public int energyPullRate() {
            return energyBudget();
        }

        void setLastEnergyPulled(int amount) {
            lastEnergyPulled = Math.max(0, amount);
        }

        @SuppressWarnings("unchecked")
        void setDestinations(List<TransferNetworks.Destination> next) {
            destinations = List.copyOf(next);
            int size = next.size();
            destHandlers = size == 0 ? null : (ResourceHandler<ItemResource>[]) new ResourceHandler<?>[size];
            destBlockEntities = size == 0 ? null : new BlockEntity[size];
            destBlockStates = size == 0 ? null : new BlockState[size];
            destCursor = size == 0 ? 0 : Math.floorMod(destCursor, size);
            networkDirty = false;
        }

        List<TransferNetworks.Destination> destinations() {
            return destinations;
        }

        @Nullable
        ResourceHandler<ItemResource> handlerAt(Level level, int index) {
            List<TransferNetworks.Destination> dests = destinations;
            if (index < 0 || index >= dests.size()) {
                return null;
            }
            TransferNetworks.Destination dest = dests.get(index);
            BlockPos pos = dest.inventoryPos();
            BlockEntity be = level.getBlockEntity(pos);
            ResourceHandler<ItemResource>[] handlers = destHandlers;
            BlockEntity[] cachedBes = destBlockEntities;
            BlockState[] cachedStates = destBlockStates;
            if (handlers != null && handlers[index] != null && cachedBes != null && cachedStates != null) {
                if (be != null) {
                    if (be == cachedBes[index] && !be.isRemoved()) {
                        return handlers[index];
                    }
                } else if (cachedBes[index] == null && cachedStates[index] == level.getBlockState(pos)) {
                    return handlers[index];
                }
            }
            ResourceHandler<ItemResource> handler = level.getCapability(
                    Capabilities.Item.BLOCK,
                    pos,
                    dest.insertFace()
            );
            if (handlers == null || cachedBes == null || cachedStates == null) {
                return handler;
            }
            if (handler == null) {
                handlers[index] = null;
                if (cachedBes != null) {
                    cachedBes[index] = null;
                }
                if (cachedStates != null) {
                    cachedStates[index] = null;
                }
                return null;
            }
            handlers[index] = handler;
            if (be != null && !be.isRemoved()) {
                cachedBes[index] = be;
                cachedStates[index] = null;
            } else {
                cachedBes[index] = null;
                cachedStates[index] = level.getBlockState(pos);
            }
            return handler;
        }

        int destCursor() {
            return destCursor;
        }

        void setDestCursor(int cursor) {
            this.destCursor = cursor;
        }

        int backoff() {
            return backoff;
        }

        void setBackoff(int backoff) {
            this.backoff = Math.max(0, backoff);
        }

        public ItemStack transferredDisplay() {
            return transferredDisplay;
        }

        void setTransferredDisplay(ItemStack stack) {
            ItemStack next = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
            if (ItemStack.isSameItemSameComponents(transferredDisplay, next)
                    && transferredDisplay.getCount() == next.getCount()) {
                return;
            }
            transferredDisplay = next;
        }
    }
}
