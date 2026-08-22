package com.dopa.randomutilities.itemcollector;

import com.dopa.randomutilities.machine.RedstoneControl;
import com.dopa.randomutilities.machine.RedstoneMode;
import com.dopa.randomutilities.machine.config.UpgradeConfig;
import com.dopa.randomutilities.registry.ModBlockEntities;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

public class ItemCollectorBlockEntity extends BlockEntity implements RedstoneControl {
    public static final int MAX_FILTER_SLOTS = ItemCollectorType.ADVANCED.filterSlotCount();
    public static final int DEFAULT_OVERLAY_COLOR = 0x33E6E6;

    int tickCounter;

    private final NonNullList<ItemStack> filterSlots = NonNullList.withSize(MAX_FILTER_SLOTS, ItemStack.EMPTY);
    private final CollectorUpgradeInventory upgrades =
            new CollectorUpgradeInventory(UpgradeConfig.UPGRADE_SLOT_COUNT, () -> collectorType().maxRangeUpgrades());
    private boolean whitelistMode;
    private int rangeX = 3;
    private int rangeY = 3;
    private int rangeZ = 3;
    private int pickupDelay = 60;
    private int pickupBatch = 1;
    private RedstoneMode redstoneMode = RedstoneMode.IGNORE;
    private int overlayColor = DEFAULT_OVERLAY_COLOR;
    private boolean particlesEnabled = true;
    private int emptySweepBackoff;
    @Nullable
    private AABB cachedScanBox;

    public ItemCollectorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ITEM_COLLECTOR.get(), pos, state);
        upgrades.setOnChanged(this::onUpgradesChanged);
        applyInitialDefaults(ItemCollectorType.fromBlock(state.getBlock()));
    }

    public ItemCollectorType collectorType() {
        return ItemCollectorType.fromBlock(getBlockState().getBlock());
    }

    private void applyInitialDefaults(ItemCollectorType type) {
        rangeX = Math.min(3, type.maxRange());
        rangeY = Math.min(3, type.maxRange());
        rangeZ = Math.min(3, type.maxRange());
        pickupDelay = type.minPickupDelay();
        pickupBatch = 1;
        whitelistMode = false;
        redstoneMode = RedstoneMode.IGNORE;
        overlayColor = DEFAULT_OVERLAY_COLOR;
        particlesEnabled = true;
    }

    /** Pickup / overlay AABB centered on the collector block. */
    public static AABB scanBox(BlockPos pos, int rangeX, int rangeY, int rangeZ) {
        return new AABB(pos).inflate(rangeX, rangeY, rangeZ);
    }

    public AABB scanBox() {
        if (cachedScanBox == null) {
            cachedScanBox = scanBox(worldPosition, rangeX, rangeY, rangeZ);
        }
        return cachedScanBox;
    }

    private void invalidateScanBox() {
        cachedScanBox = null;
    }

    @Override
    public RedstoneMode redstoneMode() {
        return redstoneMode;
    }

    @Override
    public void setRedstoneMode(RedstoneMode mode) {
        this.redstoneMode = mode == null ? RedstoneMode.IGNORE : mode;
        syncToClient();
    }

    public CollectorUpgradeInventory upgrades() {
        return upgrades;
    }

    public NonNullList<ItemStack> filterSlots() {
        return filterSlots;
    }

    public int insertUpgrade(ItemStack stack) {
        return upgrades.insertFrom(stack);
    }

    public int maxRange() {
        return collectorType().maxRange() + UpgradeConfig.extraRange(upgrades.rangeCount());
    }

    public int clampRange(int value) {
        return Math.max(0, Math.min(maxRange(), value));
    }

    private void onUpgradesChanged() {
        rangeX = clampRange(rangeX);
        rangeY = clampRange(rangeY);
        rangeZ = clampRange(rangeZ);
        invalidateScanBox();
        syncToClient();
    }

    public int activeFilterSlotCount() {
        return collectorType().filterSlotCount();
    }

    public boolean whitelistMode() {
        return whitelistMode;
    }

    public void setWhitelistMode(boolean whitelistMode) {
        this.whitelistMode = whitelistMode;
        setChanged();
    }

    public int rangeX() {
        return rangeX;
    }

    public int rangeY() {
        return rangeY;
    }

    public int rangeZ() {
        return rangeZ;
    }

    public int pickupDelay() {
        return pickupDelay;
    }

    public int pickupBatch() {
        return pickupBatch;
    }

    int emptySweepBackoff() {
        return emptySweepBackoff;
    }

    void tickEmptySweepBackoff() {
        if (emptySweepBackoff > 0) {
            emptySweepBackoff--;
        }
    }

    void onEmptySweep() {
        // Extra delay before the next AABB entity query when nothing was picked up.
        emptySweepBackoff = Math.min(4, Math.max(1, pickupDelay / 20));
    }

    void onSuccessfulSweep() {
        emptySweepBackoff = 0;
    }

    public int overlayColor() {
        return overlayColor & 0xFFFFFF;
    }

    public boolean particlesEnabled() {
        return particlesEnabled;
    }

    public void setRangeX(int rangeX) {
        this.rangeX = clampRange(rangeX);
        invalidateScanBox();
        syncToClient();
    }

    public void setRangeY(int rangeY) {
        this.rangeY = clampRange(rangeY);
        invalidateScanBox();
        syncToClient();
    }

    public void setRangeZ(int rangeZ) {
        this.rangeZ = clampRange(rangeZ);
        invalidateScanBox();
        syncToClient();
    }

    public void setPickupDelay(int pickupDelay) {
        this.pickupDelay = collectorType().clampPickupDelay(pickupDelay);
        tickCounter = 0;
        setChanged();
    }

    public void setPickupBatch(int pickupBatch) {
        this.pickupBatch = collectorType().clampPickupBatch(pickupBatch);
        setChanged();
    }

    public void setOverlayColor(int overlayColor) {
        this.overlayColor = overlayColor & 0xFFFFFF;
        syncToClient();
    }

    public void setParticlesEnabled(boolean particlesEnabled) {
        this.particlesEnabled = particlesEnabled;
        syncToClient();
    }

    public void setFilterSlot(int index, ItemStack stack) {
        if (index < 0 || index >= activeFilterSlotCount()) {
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
        return Component.translatable(switch (collectorType()) {
            case BASIC -> "container.dopasrandomutilities.basic_item_collector";
            case ADVANCED -> "container.dopasrandomutilities.advanced_item_collector";
        });
    }

    public void dropFilters(Level level, BlockPos pos) {
        upgrades.dropAt(level, pos);
        for (int i = 0; i < activeFilterSlotCount(); i++) {
            filterSlots.set(i, ItemStack.EMPTY);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ItemCollectorBlockEntity be) {
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            ItemCollectorLogic.tick(serverLevel, pos, state, be);
        }
    }

    private void syncToClient() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null) {
            dropFilters(level, pos);
        }
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        for (int i = 0; i < MAX_FILTER_SLOTS; i++) {
            filterSlots.set(i, input.read("Filter" + i, ItemStack.CODEC).orElse(ItemStack.EMPTY));
        }
        whitelistMode = input.getBooleanOr("WhitelistMode", false);
        rangeX = input.getIntOr("RangeX", 3);
        rangeY = input.getIntOr("RangeY", 3);
        rangeZ = input.getIntOr("RangeZ", 3);
        pickupDelay = input.getIntOr("PickupDelay", 20);
        pickupBatch = input.getIntOr("PickupBatch", 1);
        redstoneMode = RedstoneMode.byOrdinal(input.getIntOr("RedstoneMode", 0));
        tickCounter = input.getIntOr("TickCounter", 0);
        overlayColor = input.getIntOr("OverlayColor", DEFAULT_OVERLAY_COLOR) & 0xFFFFFF;
        particlesEnabled = input.getBooleanOr("ParticlesEnabled", true);
        upgrades.loadSlots(input);
        upgrades.trimToCap();
        clampToType(collectorType());
    }

    private void clampToType(ItemCollectorType type) {
        rangeX = clampRange(rangeX);
        rangeY = clampRange(rangeY);
        rangeZ = clampRange(rangeZ);
        pickupDelay = type.clampPickupDelay(pickupDelay);
        pickupBatch = type.clampPickupBatch(pickupBatch);
        invalidateScanBox();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        for (int i = 0; i < activeFilterSlotCount(); i++) {
            ItemStack stack = filterSlots.get(i);
            if (!stack.isEmpty()) {
                output.store("Filter" + i, ItemStack.CODEC, stack);
            }
        }
        output.putBoolean("WhitelistMode", whitelistMode);
        output.putInt("RangeX", rangeX);
        output.putInt("RangeY", rangeY);
        output.putInt("RangeZ", rangeZ);
        output.putInt("PickupDelay", pickupDelay);
        output.putInt("PickupBatch", pickupBatch);
        output.putInt("RedstoneMode", redstoneMode.ordinal());
        output.putInt("TickCounter", tickCounter);
        output.putInt("OverlayColor", overlayColor & 0xFFFFFF);
        output.putBoolean("ParticlesEnabled", particlesEnabled);
        upgrades.saveSlots(output);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("RangeX", rangeX);
        tag.putInt("RangeY", rangeY);
        tag.putInt("RangeZ", rangeZ);
        tag.putInt("OverlayColor", overlayColor & 0xFFFFFF);
        tag.putBoolean("ParticlesEnabled", particlesEnabled);
        return tag;
    }

    @Override
    public void handleUpdateTag(ValueInput input) {
        applyClientSync(input);
    }

    /**
     * Default {@code onDataPacket} runs {@code loadAdditional} with the sparse update tag and
     * wipes client upgrade stacks (menu slots read those via {@code StackCopySlot}).
     */
    @Override
    public void onDataPacket(Connection net, ValueInput valueInput) {
        applyClientSync(valueInput);
    }

    private void applyClientSync(ValueInput input) {
        rangeX = input.getIntOr("RangeX", rangeX);
        rangeY = input.getIntOr("RangeY", rangeY);
        rangeZ = input.getIntOr("RangeZ", rangeZ);
        overlayColor = input.getIntOr("OverlayColor", overlayColor) & 0xFFFFFF;
        particlesEnabled = input.getBooleanOr("ParticlesEnabled", particlesEnabled);
        invalidateScanBox();
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
