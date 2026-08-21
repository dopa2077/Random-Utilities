package com.dopa.randomutilities.blockplacer;

import com.dopa.randomutilities.dOPasRandomUtilities;
import com.dopa.randomutilities.machine.EnergyMachineUpgradeInventory;
import com.dopa.randomutilities.machine.MachineEnergy;
import com.dopa.randomutilities.machine.RedstoneControl;
import com.dopa.randomutilities.machine.RedstoneMode;
import com.dopa.randomutilities.machine.config.UpgradeConfig;
import com.dopa.randomutilities.registry.ModBlockEntities;
import com.dopa.randomutilities.util.BlockOrientations;
import com.dopa.randomutilities.util.GhostItemFilter;
import com.dopa.randomutilities.util.WorkingVolume;
import com.dopa.randomutilities.util.WorkingVolumeSource;
import com.mojang.authlib.GameProfile;

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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class AdvancedBlockPlacerBlockEntity extends BlockEntity implements RedstoneControl, WorkingVolumeSource {
    public static final int SLOT_COUNT = 9;
    public static final int FILTER_SLOTS = 8;
    public static final int OVERLAY_COLOR = 0x33E6E6;
    private static final int EMPTY_VOLUME_BACKOFF = 10;

    private static final GameProfile PLACER_PROFILE = new GameProfile(
            UUID.nameUUIDFromBytes((dOPasRandomUtilities.MOD_ID + ":advanced_block_placer").getBytes(StandardCharsets.UTF_8)),
            "[Advanced Block Placer]"
    );

    private final ItemStacksResourceHandler items = new ItemStacksResourceHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int index, ItemStack previousContents) {
            wakeVolumeScan();
            setChanged();
        }
    };
    private final NonNullList<ItemStack> filterSlots = NonNullList.withSize(FILTER_SLOTS, ItemStack.EMPTY);
    private final WorkingVolume volume = new WorkingVolume();
    private final EnergyMachineUpgradeInventory upgrades =
            new EnergyMachineUpgradeInventory(UpgradeConfig.UPGRADE_SLOT_COUNT);
    private final MachineEnergy energy = new MachineEnergy();
    private boolean whitelistMode;
    private boolean mute = true;
    private int overlayColor = OVERLAY_COLOR;
    private RedstoneMode redstoneMode = RedstoneMode.IGNORE;
    private int emptyScanBackoff;

    public AdvancedBlockPlacerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ADVANCED_BLOCK_PLACER.get(), pos, state);
        upgrades.setOnChanged(this::onUpgradesChanged);
        if (state.hasProperty(BlockOrientations.ORIENTATION)) {
            volume.setOffsetToFacing(BlockOrientations.front(state));
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AdvancedBlockPlacerBlockEntity be) {
        if (!level.isClientSide()) {
            be.tick((ServerLevel) level, pos, state);
        }
    }

    public ItemStacksResourceHandler itemHandler() {
        return items;
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
        return Component.translatable("container.dopasrandomutilities.advanced_block_placer");
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
        wakeVolumeScan();
        setChanged();
    }

    public ItemStack stackInSlot(int slot) {
        ItemResource resource = items.getResource(slot);
        if (resource.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return resource.toStack(items.getAmountAsInt(slot));
    }

    public int getAnalogOutput() {
        float fill = 0.0F;
        boolean any = false;
        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack stack = stackInSlot(i);
            if (!stack.isEmpty()) {
                any = true;
                fill += (float) stack.getCount() / (float) Math.max(1, stack.getMaxStackSize());
            }
        }
        return any ? Mth.floor(fill / SLOT_COUNT * 14.0F) + 1 : 0;
    }

    private void tick(ServerLevel level, BlockPos pos, BlockState state) {
        energy.beginTick();
        if (emptyScanBackoff > 0) {
            emptyScanBackoff--;
        }
        if (!redstoneMode.allowsOperation(level.getBestNeighborSignal(pos))) {
            return;
        }
        if (emptyScanBackoff > 0) {
            return;
        }
        energy.runReadyCycles(upgrades.overclockCount(), () -> {
            if (!tryPlace(level, pos, state)) {
                emptyScanBackoff = EMPTY_VOLUME_BACKOFF;
                return false;
            }
            emptyScanBackoff = 0;
            return true;
        });
    }

    private boolean tryPlace(ServerLevel level, BlockPos pos, BlockState state) {
        Direction facing = BlockOrientations.front(state);
        BlockPos target = pickRandomCell(level.getRandom(), cell -> level.getBlockState(cell).canBeReplaced());
        if (target == null) {
            return false;
        }
        int slot = pickRandomBlockSlot(level.getRandom());
        if (slot < 0) {
            return false;
        }
        int cost = energy.operationCost(pos, target, upgrades.efficiencyCount(), upgrades.overclockCount());
        if (energy.stored() < cost) {
            return false;
        }
        ItemStack stack = stackInSlot(slot);
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return false;
        }
        FakePlayer placer = placer(level);
        positionPlayer(placer, pos, facing);
        ItemStack held = stack.copy();
        placer.setItemInHand(InteractionHand.MAIN_HAND, held);
        Vec3 hitLoc = Vec3.atCenterOf(target).subtract(
                facing.getStepX() * 0.5,
                facing.getStepY() * 0.5,
                facing.getStepZ() * 0.5
        );
        BlockHitResult hit = new BlockHitResult(hitLoc, facing.getOpposite(), target, false);
        BlockPlaceContext context = new BlockPlaceContext(placer, InteractionHand.MAIN_HAND, held, hit);
        boolean placed = placeBlock(level, target, blockItem, context, held);
        ItemStack remaining = placer.getItemInHand(InteractionHand.MAIN_HAND);
        if (remaining.isEmpty()) {
            items.set(slot, ItemResource.EMPTY, 0);
        } else {
            items.set(slot, ItemResource.of(remaining), remaining.getCount());
        }
        placer.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        if (placed) {
            energy.tryConsume(cost);
            setChanged();
        }
        return placed;
    }

    private boolean placeBlock(
            ServerLevel level,
            BlockPos target,
            BlockItem blockItem,
            BlockPlaceContext context,
            ItemStack held
    ) {
        if (!context.canPlace()) {
            return false;
        }
        BlockState toPlace = blockItem.getBlock().getStateForPlacement(context);
        if (toPlace == null || !toPlace.canSurvive(level, target)) {
            return false;
        }
        if (!level.setBlock(target, toPlace, Block.UPDATE_ALL)) {
            return false;
        }
        BlockState placed = level.getBlockState(target);
        blockItem.getBlock().setPlacedBy(level, target, placed, context.getPlayer(), held);
        level.gameEvent(GameEvent.BLOCK_PLACE, target, GameEvent.Context.of(context.getPlayer(), placed));
        if (!mute) {
            SoundType sound = placed.getSoundType();
            level.playSound(
                    null,
                    target,
                    sound.getPlaceSound(),
                    SoundSource.BLOCKS,
                    (sound.getVolume() + 1.0F) / 2.0F,
                    sound.getPitch() * 0.8F
            );
        }
        held.shrink(1);
        return true;
    }

    private BlockPos pickRandomCell(RandomSource random, java.util.function.Predicate<BlockPos> valid) {
        int[] seen = {0};
        BlockPos[] chosen = {null};
        volume.forEachCell(worldPosition, cell -> {
            if (!valid.test(cell)) {
                return;
            }
            seen[0]++;
            if (random.nextInt(seen[0]) == 0) {
                chosen[0] = cell.immutable();
            }
        });
        return chosen[0];
    }

    private int pickRandomBlockSlot(RandomSource random) {
        int chosen = -1;
        int seen = 0;
        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack stack = stackInSlot(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) {
                continue;
            }
            if (!GhostItemFilter.allows(stack, filterSlots, whitelistMode)) {
                continue;
            }
            seen++;
            if (random.nextInt(seen) == 0) {
                chosen = i;
            }
        }
        return chosen;
    }

    private FakePlayer placer(ServerLevel level) {
        return FakePlayerFactory.get(level, PLACER_PROFILE);
    }

    private static void positionPlayer(Player player, BlockPos pos, Direction facing) {
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        player.setYRot(facing.toYRot());
        player.setXRot(pitchFor(facing));
    }

    private static float pitchFor(Direction facing) {
        return switch (facing) {
            case UP -> -90.0F;
            case DOWN -> 90.0F;
            default -> 0.0F;
        };
    }

    public void dropContents(Level level, BlockPos pos) {
        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack stack = stackInSlot(i);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
            }
            items.set(i, ItemResource.EMPTY, 0);
        }
        upgrades.dropAt(level, pos);
        for (int i = 0; i < FILTER_SLOTS; i++) {
            filterSlots.set(i, ItemStack.EMPTY);
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
        if (level != null && !level.isClientSide()) {
            dropContents(level, pos);
        }
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack stack = input.read("Item" + i, ItemStack.CODEC).orElse(ItemStack.EMPTY);
            if (stack.isEmpty()) {
                items.set(i, ItemResource.EMPTY, 0);
            } else {
                items.set(i, ItemResource.of(stack), stack.getCount());
            }
        }
        for (int i = 0; i < FILTER_SLOTS; i++) {
            filterSlots.set(i, input.read("Filter" + i, ItemStack.CODEC).orElse(ItemStack.EMPTY));
        }
        whitelistMode = input.getBooleanOr("WhitelistMode", false);
        mute = input.getBooleanOr("Mute", true);
        overlayColor = input.getIntOr("OverlayColor", OVERLAY_COLOR) & 0xFFFFFF;
        redstoneMode = RedstoneMode.byOrdinal(input.getIntOr("RedstoneMode", 0));
        upgrades.loadSlots(input);
        upgrades.trimInstalledCaps();
        onUpgradesChanged();
        energy.load(input);
        volume.load(input);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack stack = stackInSlot(i);
            if (!stack.isEmpty()) {
                output.store("Item" + i, ItemStack.CODEC, stack);
            }
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
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("MaxRange", volume.maxRange());
        tag.putInt("RangeX", volume.rangeX());
        tag.putInt("RangeY", volume.rangeY());
        tag.putInt("RangeZ", volume.rangeZ());
        tag.putInt("OffsetX", volume.offsetX());
        tag.putInt("OffsetY", volume.offsetY());
        tag.putInt("OffsetZ", volume.offsetZ());
        tag.putInt("OverlayColor", overlayColor & 0xFFFFFF);
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
        volume.load(input);
        overlayColor = input.getIntOr("OverlayColor", overlayColor) & 0xFFFFFF;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
