package com.dopa.randomutilities.blockbreaker;

import com.dopa.randomutilities.machine.ClaimActionGate;
import com.dopa.randomutilities.machine.EnergyMachineUpgradeInventory;
import com.dopa.randomutilities.machine.MachineActors;
import com.dopa.randomutilities.machine.MachineEnergy;
import com.dopa.randomutilities.machine.MachineOwnerProfiles;
import com.dopa.randomutilities.machine.OwnableMachine;
import com.dopa.randomutilities.machine.OwnerRequiredFeedback;
import com.dopa.randomutilities.machine.RedstoneControl;
import com.dopa.randomutilities.machine.RedstoneMode;
import com.dopa.randomutilities.machine.AdvancedVolumeMachineHost;
import com.dopa.randomutilities.machine.AdvancedVolumeMachineSupport;
import com.dopa.randomutilities.machine.config.UpgradeConfig;
import com.dopa.randomutilities.registry.ModBlockEntities;
import com.dopa.randomutilities.util.BlockOrientations;
import com.dopa.randomutilities.util.GhostItemFilter;
import com.dopa.randomutilities.util.WorkingVolume;
import com.dopa.randomutilities.util.WorkingVolumeSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.transfer.item.ContainerOrHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemUtil;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class AdvancedBlockBreakerBlockEntity extends BlockEntity
        implements OwnableMachine, RedstoneControl, WorkingVolumeSource, AdvancedVolumeMachineHost {
    public static final int FILTER_SLOTS = 8;
    public static final int OVERLAY_COLOR = 0xE64D33;

    private static final int TRIGGERED_TICKS = 8;
    private static final int DISPENSE_ACCURACY = 6;
    private static final double DISPENSE_OFFSET = 0.7;
    /** Skip volume scans for a few ticks after finding no harvestable cell. */
    private static final int EMPTY_VOLUME_BACKOFF = AdvancedVolumeMachineSupport.EMPTY_VOLUME_BACKOFF;
    /** Max box cells inspected per random-target attempt. */
    private static final int VOLUME_SCAN_BUDGET = AdvancedVolumeMachineSupport.VOLUME_SCAN_BUDGET;

    private final ItemStacksResourceHandler pickaxe = new ItemStacksResourceHandler(1) {
        @Override
        protected int getCapacity(int index, ItemResource resource) {
            return 1;
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            return resource.toStack().is(ItemTags.PICKAXES);
        }

        @Override
        protected void onContentsChanged(int index, ItemStack previousContents) {
            setChanged();
        }
    };
    private final NonNullList<ItemStack> filterSlots = NonNullList.withSize(FILTER_SLOTS, ItemStack.EMPTY);
    private final WorkingVolume volume = new WorkingVolume();
    private final EnergyMachineUpgradeInventory upgrades =
            new EnergyMachineUpgradeInventory(UpgradeConfig.UPGRADE_SLOT_COUNT);
    private final MachineEnergy energy = new MachineEnergy();
    private final BreakerMining mining = new BreakerMining();
    private boolean whitelistMode;
    private boolean mute = true;
    private int overlayColor = OVERLAY_COLOR;
    private RedstoneMode redstoneMode = RedstoneMode.IGNORE;
    private int triggeredTicks;
    private int emptyScanBackoff;
    private int ownerFeedbackCooldown;
    private int volumeScanCursor;
    private boolean filtersConfigured;
    @Nullable
    private UUID ownerUuid;

    public AdvancedBlockBreakerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ADVANCED_BLOCK_BREAKER.get(), pos, state);
        upgrades.setOnChanged(this::onUpgradesChanged);
        if (state.hasProperty(BlockOrientations.ORIENTATION)) {
            volume.setOffsetToFacing(BlockOrientations.front(state));
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AdvancedBlockBreakerBlockEntity be) {
        if (!level.isClientSide()) {
            be.tick((ServerLevel) level, pos, state);
        }
    }

    public ItemStacksResourceHandler pickaxeHandler() {
        return pickaxe;
    }

    public EnergyMachineUpgradeInventory upgrades() {
        return upgrades;
    }

    public MachineEnergy energy() {
        return energy;
    }

    public int insertUpgrade(ItemStack stack) {
        return upgrades.insertFrom(stack);
    }

    public int maxRange() {
        return volume.maxRange();
    }

    private void onUpgradesChanged() {
        energy.applyEnergyUpgrades(upgrades.energyCount());
        volume.setMaxRange(WorkingVolume.MAX_RANGE + UpgradeConfig.extraRange(upgrades.rangeCount()));
        wakeVolumeScan();
        syncToClient();
    }

    private void wakeVolumeScan() {
        emptyScanBackoff = 0;
    }

    public Component getDisplayName() {
        return Component.translatable("container.dopasrandomutilities.advanced_block_breaker");
    }

    @Override
    public WorkingVolume workingVolume() {
        return volume;
    }

    @Override
    public int overlayColor() {
        return overlayColor & 0xFFFFFF;
    }

    public void setOverlayColor(int overlayColor) {
        this.overlayColor = overlayColor & 0xFFFFFF;
        syncToClient();
    }

    public NonNullList<ItemStack> filterSlots() {
        return filterSlots;
    }

    public boolean whitelistMode() {
        return whitelistMode;
    }

    public void setWhitelistMode(boolean whitelistMode) {
        this.whitelistMode = whitelistMode;
        wakeVolumeScan();
        setChanged();
    }

    public boolean isMuted() {
        return mute;
    }

    public void setMuted(boolean mute) {
        this.mute = mute;
        setChanged();
    }

    @Override
    public RedstoneMode redstoneMode() {
        return redstoneMode;
    }

    @Override
    public void setRedstoneMode(RedstoneMode mode) {
        this.redstoneMode = mode == null ? RedstoneMode.IGNORE : mode;
        setChanged();
    }

    public void setRangeX(int value) {
        volume.setRangeX(value);
        wakeVolumeScan();
        syncToClient();
    }

    public void setRangeY(int value) {
        volume.setRangeY(value);
        wakeVolumeScan();
        syncToClient();
    }

    public void setRangeZ(int value) {
        volume.setRangeZ(value);
        wakeVolumeScan();
        syncToClient();
    }

    public void setOffsetX(int value) {
        volume.setOffsetX(value);
        wakeVolumeScan();
        syncToClient();
    }

    public void setOffsetY(int value) {
        volume.setOffsetY(value);
        wakeVolumeScan();
        syncToClient();
    }

    public void setOffsetZ(int value) {
        volume.setOffsetZ(value);
        wakeVolumeScan();
        syncToClient();
    }

    public void setFilterSlot(int index, ItemStack stack) {
        if (index < 0 || index >= FILTER_SLOTS) {
            return;
        }
        filterSlots.set(index, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
        refreshFiltersConfigured();
        wakeVolumeScan();
        setChanged();
    }

    private void refreshFiltersConfigured() {
        for (ItemStack slot : filterSlots) {
            if (!slot.isEmpty()) {
                filtersConfigured = true;
                return;
            }
        }
        filtersConfigured = false;
    }

    private boolean allowsBlock(BlockState cellState) {
        if (!filtersConfigured) {
            return true;
        }
        return GhostItemFilter.allows(
                ItemResource.of(cellState.getBlock().asItem()),
                filterSlots,
                whitelistMode
        );
    }

    public ItemStack pickaxeStack() {
        ItemResource resource = pickaxe.getResource(0);
        if (resource.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return resource.toStack(pickaxe.getAmountAsInt(0));
    }

    private void tick(ServerLevel level, BlockPos pos, BlockState state) {
        energy.beginTick();
        ownerFeedbackCooldown = OwnerRequiredFeedback.tickOwnerFeedbackCooldown(ownerFeedbackCooldown);
        if (emptyScanBackoff > 0) {
            emptyScanBackoff--;
        }
        boolean powered = redstoneMode.allowsOperation(level.getBestNeighborSignal(pos));
        if (OwnerRequiredFeedback.blockWhilePowered(level, pos, this, powered, ownerFeedbackCooldown)) {
            if (!hasOwner() && ownerFeedbackCooldown <= 0) {
                ownerFeedbackCooldown = OwnerRequiredFeedback.cooldownAfterPoweredBlock();
            }
            if (triggeredTicks > 0) {
                triggeredTicks--;
                if (triggeredTicks == 0) {
                    setTriggered(level, pos, false);
                    setChanged();
                }
            }
            return;
        }
        if (powered) {
            boolean brokeBlock = false;
            if (emptyScanBackoff <= 0) {
                boolean[] broke = {false};
                energy.runReadyCycles(upgrades.overclockCount(), () -> {
                    int outcome = tryBreak(level, pos, state);
                    if (outcome == 0) {
                        emptyScanBackoff = emptyVolumeBackoff();
                        return false;
                    }
                    emptyScanBackoff = 0;
                    if (outcome == 2) {
                        broke[0] = true;
                    }
                    return true;
                });
                brokeBlock = broke[0];
            }
            if (brokeBlock) {
                setTriggered(level, pos, true);
                triggeredTicks = TRIGGERED_TICKS;
                setChanged();
            }
        }
        if (triggeredTicks > 0) {
            triggeredTicks--;
            if (triggeredTicks == 0) {
                setTriggered(level, pos, false);
                setChanged();
            }
        }
    }

    /** @return 0 failed, 1 mining progress, 2 block broken */
    private int tryBreak(ServerLevel level, BlockPos pos, BlockState state) {
        Direction facing = BlockOrientations.front(state);
        ItemStack stored = pickaxeStack();
        boolean virtualTool = stored.isEmpty();
        ItemStack tool = virtualTool ? new ItemStack(Items.DIAMOND_PICKAXE) : stored.copy();
        FakePlayer breaker = MachineActors.actor(level, ownerUuid, pos, facing).orElse(null);
        if (breaker == null) {
            return 0;
        }
        breaker.setItemInHand(InteractionHand.MAIN_HAND, tool);

        BlockPos target = selectTarget(level, breaker, virtualTool);
        if (target == null) {
            mining.clear(level, destroyId());
            breaker.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            return 0;
        }
        BlockState targetState = level.getBlockState(target);
        float hardness = targetState.getDestroySpeed(level, target);
        int efficiency = virtualTool ? 0 : BreakerMining.efficiency(level, tool);
        int hitsNeeded = BreakerMining.hitsNeeded(hardness, efficiency);
        // Charge hardness-scaled FE once on break — not again on every intermediate hit.
        int cost = energy.operationCost(pos, target, upgrades.efficiencyCount(), upgrades.overclockCount())
                * BreakerMining.energyMultiplier(hardness);
        if (energy.stored() < cost) {
            breaker.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            return 0;
        }

        if (!mining.completeAfterHit(level, destroyId(), target, hitsNeeded)) {
            if (!mute) {
                BreakerMining.playHit(level, target, targetState);
            }
            breaker.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            return 1;
        }
        energy.tryConsume(cost);

        // Always break via gameMode so claims/protection apply; mute only suppresses FX.
        List<ItemStack> drops = new ArrayList<>();
        AABB capture = new AABB(target).inflate(0.5);
        Set<UUID> existing = itemEntityIds(level, capture);
        boolean broken = breaker.gameMode.destroyBlock(target);
        if (broken) {
            BreakerDropCapture.collectFresh(level, capture, existing, drops);
        }
        if (!virtualTool) {
            tool = breaker.getItemInHand(InteractionHand.MAIN_HAND);
            writePickaxeBack(tool);
        }
        breaker.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        if (broken) {
            ejectFromBack(level, pos, facing.getOpposite(), drops, mute);
            return 2;
        }
        return 0;
    }

    private BlockPos selectTarget(ServerLevel level, FakePlayer breaker, boolean virtualTool) {
        BlockPos current = mining.target();
        if (current != null && stillValidTarget(level, current, breaker, virtualTool)) {
            return current;
        }
        mining.clear(level, destroyId());
        return pickRandomCell(level, breaker, virtualTool);
    }

    private boolean stillValidTarget(ServerLevel level, BlockPos cell, FakePlayer breaker, boolean virtualTool) {
        if (!volume.contains(worldPosition, cell)) {
            return false;
        }
        BlockState cellState = level.getBlockState(cell);
        if (!canHarvest(level, cell, cellState, breaker, virtualTool)) {
            return false;
        }
        if (!allowsBlock(cellState)) {
            return false;
        }
        return ClaimActionGate.canBreak(level, breaker, cell);
    }

    private int destroyId() {
        return Long.hashCode(worldPosition.asLong());
    }

    private BlockPos pickRandomCell(ServerLevel level, FakePlayer breaker, boolean virtualTool) {
        BlockPos chosen = AdvancedVolumeMachineSupport.pickRandomCell(
                this,
                volume,
                volumeScanCursor,
                level.getRandom(),
                cell -> {
                    BlockState cellState = level.getBlockState(cell);
                    if (!canHarvest(level, cell, cellState, breaker, virtualTool)) {
                        return false;
                    }
                    if (!allowsBlock(cellState)) {
                        return false;
                    }
                    return ClaimActionGate.canBreak(level, breaker, cell);
                }
        );
        volumeScanCursor = AdvancedVolumeMachineSupport.advanceScanCursor(volumeScanCursor, volume, worldPosition);
        return chosen;
    }

    private int emptyVolumeBackoff() {
        return AdvancedVolumeMachineSupport.emptyVolumeBackoff(this, volume);
    }

    private static boolean canHarvest(
            ServerLevel level,
            BlockPos pos,
            BlockState state,
            FakePlayer breaker,
            boolean virtualTool
    ) {
        if (state.isAir() || state.getBlock() instanceof LiquidBlock) {
            return false;
        }
        if (state.getDestroySpeed(level, pos) < 0.0F) {
            return false;
        }
        if (virtualTool) {
            return !state.is(BlockTags.INCORRECT_FOR_DIAMOND_TOOL);
        }
        return !state.requiresCorrectToolForDrops() || breaker.hasCorrectToolForDrops(state);
    }

    private void writePickaxeBack(ItemStack tool) {
        if (tool.isEmpty()) {
            pickaxe.set(0, ItemResource.EMPTY, 0);
        } else {
            pickaxe.set(0, ItemResource.of(tool), tool.getCount());
        }
    }

    private void ejectFromBack(ServerLevel level, BlockPos pos, Direction output, List<ItemStack> drops, boolean muteSounds) {
        ContainerOrHandler into = HopperBlockEntity.getContainerOrHandlerAt(
                level,
                pos.relative(output),
                output.getOpposite()
        );
        Vec3 dispensePos = Vec3.atCenterOf(pos).add(
                output.getStepX() * DISPENSE_OFFSET,
                output.getStepY() * DISPENSE_OFFSET,
                output.getStepZ() * DISPENSE_OFFSET
        );
        boolean shot = false;
        for (ItemStack drop : drops) {
            if (drop.isEmpty()) {
                continue;
            }
            ItemStack remaining = insertLikeDropper(into, output, drop.copy());
            if (!remaining.isEmpty()) {
                DefaultDispenseItemBehavior.spawnItem(level, remaining, DISPENSE_ACCURACY, output, dispensePos);
                shot = true;
            }
        }
        if (shot && !muteSounds) {
            level.levelEvent(LevelEvent.SOUND_DISPENSER_DISPENSE, pos, 0);
            level.levelEvent(LevelEvent.PARTICLES_SHOOT_SMOKE, pos, output.get3DDataValue());
        }
    }

    private static ItemStack insertLikeDropper(ContainerOrHandler into, Direction output, ItemStack stack) {
        if (into.isEmpty()) {
            return stack;
        }
        if (into.container() != null) {
            return HopperBlockEntity.addItem(null, into.container(), stack, output.getOpposite());
        }
        return ItemUtil.insertItemReturnRemaining(into.itemHandler(), stack, false, null);
    }

    private static Set<UUID> itemEntityIds(ServerLevel level, AABB area) {
        Set<UUID> ids = new HashSet<>();
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, area)) {
            ids.add(entity.getUUID());
        }
        return ids;
    }

    @Override
    @Nullable
    public UUID ownerUuid() {
        return ownerUuid;
    }

    @Override
    public void setOwnerUuid(@Nullable UUID uuid) {
        this.ownerUuid = uuid;
        setChanged();
    }

    private static void setTriggered(ServerLevel level, BlockPos pos, boolean triggered) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof AdvancedBlockBreakerBlock
                && state.getValue(AdvancedBlockBreakerBlock.TRIGGERED) != triggered) {
            level.setBlock(pos, state.setValue(AdvancedBlockBreakerBlock.TRIGGERED, triggered), Block.UPDATE_CLIENTS);
        }
    }

    public void dropContents(Level level, BlockPos pos) {
        ItemStack tool = pickaxeStack();
        if (!tool.isEmpty()) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), tool);
            pickaxe.set(0, ItemResource.EMPTY, 0);
        }
        upgrades.dropAt(level, pos);
        for (int i = 0; i < FILTER_SLOTS; i++) {
            filterSlots.set(i, ItemStack.EMPTY);
        }
    }

    private void syncToClient() {
        AdvancedVolumeMachineSupport.syncToClient(this);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel server) {
            mining.clear(server, destroyId());
            dropContents(server, pos);
        }
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ItemStack tool = input.read("Pickaxe", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        if (tool.isEmpty()) {
            pickaxe.set(0, ItemResource.EMPTY, 0);
        } else {
            pickaxe.set(0, ItemResource.of(tool), tool.getCount());
        }
        for (int i = 0; i < FILTER_SLOTS; i++) {
            filterSlots.set(i, input.read("Filter" + i, ItemStack.CODEC).orElse(ItemStack.EMPTY));
        }
        refreshFiltersConfigured();
        whitelistMode = input.getBooleanOr("WhitelistMode", false);
        mute = input.getBooleanOr("Mute", true);
        overlayColor = input.getIntOr("OverlayColor", OVERLAY_COLOR) & 0xFFFFFF;
        redstoneMode = RedstoneMode.byOrdinal(input.getIntOr("RedstoneMode", 0));
        upgrades.loadSlots(input);
        upgrades.trimInstalledCaps();
        onUpgradesChanged();
        energy.load(input);
        volume.load(input);
        mining.load(input);
        triggeredTicks = Math.max(0, input.getIntOr("TriggeredTicks", 0));
        ownerUuid = MachineOwnerProfiles.load(input);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ItemStack tool = pickaxeStack();
        if (!tool.isEmpty()) {
            output.store("Pickaxe", ItemStack.CODEC, tool);
        }
        for (int i = 0; i < FILTER_SLOTS; i++) {
            ItemStack stack = filterSlots.get(i);
            if (!stack.isEmpty()) {
                output.store("Filter" + i, ItemStack.CODEC, stack);
            }
        }
        output.putBoolean("WhitelistMode", whitelistMode);
        output.putBoolean("Mute", mute);
        output.putInt("OverlayColor", overlayColor & 0xFFFFFF);
        output.putInt("RedstoneMode", redstoneMode.ordinal());
        upgrades.saveSlots(output);
        energy.save(output);
        volume.save(output);
        mining.save(output);
        if (triggeredTicks > 0) {
            output.putInt("TriggeredTicks", triggeredTicks);
        }
        if (ownerUuid != null) {
            MachineOwnerProfiles.save(output, ownerUuid);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return AdvancedVolumeMachineSupport.createUpdateTag(volume, overlayColor);
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
        AdvancedVolumeMachineSupport.applyClientSync(input, volume, value -> overlayColor = value, overlayColor);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
