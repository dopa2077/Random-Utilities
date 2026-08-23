package com.dopa.randomutilities.solarpanel;

import com.dopa.randomutilities.machine.AdvancedVolumeMachineSupport;
import com.dopa.randomutilities.machine.GeneratorEnergy;
import com.dopa.randomutilities.machine.GeneratorUpgradeInventory;
import com.dopa.randomutilities.machine.MachineEnergy;
import com.dopa.randomutilities.machine.RedstoneControl;
import com.dopa.randomutilities.machine.RedstoneMode;
import com.dopa.randomutilities.machine.config.UpgradeConfig;
import com.dopa.randomutilities.registry.ModBlockEntities;
import com.dopa.randomutilities.solarfurnace.SolarPower;
import com.dopa.randomutilities.solarpanel.config.SolarPanelConfig;
import com.dopa.randomutilities.util.WorkingVolume;
import com.dopa.randomutilities.util.WorkingVolumeSource;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

public class SolarPanelControllerBlockEntity extends BlockEntity
        implements RedstoneControl, WorkingVolumeSource {
    public static final int OVERLAY_COLOR = 0xF2C94C;
    private static final Direction[] HORIZONTAL = {
            Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
    };

    private final GeneratorUpgradeInventory upgrades =
            GeneratorUpgradeInventory.solarPanel(UpgradeConfig.UPGRADE_SLOT_COUNT);
    private final MachineEnergy energy = new MachineEnergy();
    private final WorkingVolume volume = new WorkingVolume();

    private RedstoneMode redstoneMode = RedstoneMode.IGNORE;
    private int solarEvalCooldown;
    private int linkedPanels;
    private int peakFePerTick;
    private float lastSunFactor;
    private SolarPower.Status lastStatus = SolarPower.Status.NO_SUN;
    private int overlayColor = OVERLAY_COLOR;
    private int lastComputedFe;
    /** In-memory claim set; rebuilt each eval, used to release panels on break/rescan. */
    private final Set<BlockPos> claimedPanels = new HashSet<>();

    public SolarPanelControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SOLAR_PANEL_CONTROLLER.get(), pos, state);
        upgrades.setOnChanged(this::onUpgradesChanged);
        energy.applyGeneratorEnergyUpgrades(0);
        refreshFootprint();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide()) {
            syncFootprint();
        }
    }

    private void onUpgradesChanged() {
        energy.applyGeneratorEnergyUpgrades(upgrades.energyCount());
        setChanged();
    }

    private void refreshFootprint() {
        int range = SolarPanelConfig.maxRange();
        volume.setMaxRange(Math.max(WorkingVolume.MAX_RANGE, range));
        volume.setOffsetX(0);
        volume.setOffsetY(1);
        volume.setOffsetZ(0);
        volume.setRangeX(range);
        volume.setRangeY(0);
        volume.setRangeZ(range);
    }

    private void syncFootprint() {
        refreshFootprint();
        AdvancedVolumeMachineSupport.syncToClient(this);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SolarPanelControllerBlockEntity be) {
        if (!level.isClientSide()) {
            be.tick((ServerLevel) level, pos, state);
        }
    }

    public GeneratorUpgradeInventory upgrades() {
        return upgrades;
    }

    public MachineEnergy energy() {
        return energy;
    }

    public int linkedPanels() {
        return linkedPanels;
    }

    public int peakFePerTick() {
        return peakFePerTick;
    }

    public float lastSunFactor() {
        return lastSunFactor;
    }

    public SolarPower.Status lastStatus() {
        return lastStatus;
    }

    public int solarPermille() {
        return Mth.clamp(Math.round(lastSunFactor * 1000.0F), 0, 1000);
    }

    @Override
    public WorkingVolume workingVolume() {
        return volume;
    }

    @Override
    public int overlayColor() {
        return overlayColor;
    }

    @Override
    public RedstoneMode redstoneMode() {
        return redstoneMode;
    }

    @Override
    public void setRedstoneMode(RedstoneMode mode) {
        if (this.redstoneMode != mode) {
            this.redstoneMode = mode == null ? RedstoneMode.IGNORE : mode;
            setChanged();
        }
    }

    public int insertUpgrade(ItemStack stack) {
        return upgrades.insertFrom(stack);
    }

    private void tick(ServerLevel level, BlockPos pos, BlockState state) {
        energy.beginTick();
        int energyBefore = energy.stored();
        boolean powered = redstoneMode == RedstoneMode.IGNORE
                || redstoneMode.allowsOperation(level.getBestNeighborSignal(pos));
        boolean panelsChanged = false;

        if (--solarEvalCooldown <= 0) {
            solarEvalCooldown = SolarPanelConfig.evalInterval();
            int prevLinked = linkedPanels;
            int prevPeak = peakFePerTick;
            SolarPower.Status prevStatus = lastStatus;
            lastComputedFe = scanAndComputeGeneration(level, pos);
            panelsChanged = prevLinked != linkedPanels
                    || prevPeak != peakFePerTick
                    || prevStatus != lastStatus;
        }

        if (powered && lastComputedFe > 0) {
            energy.tryGenerate(lastComputedFe);
        }
        GeneratorEnergy.pushToNeighbors(level, pos, energy);

        boolean energyChanged = energy.stored() != energyBefore;
        if (panelsChanged || energyChanged) {
            setChanged();
        }
    }

    /**
     * Counts solar panels on Y+1 that form an orthogonally connected structure
     * rooted at the panel directly above this controller (within max range).
     * Panels already claimed by another valid controller are a hard boundary.
     */
    private int scanAndComputeGeneration(Level level, BlockPos origin) {
        int panels = 0;
        int peak = 0;
        double feSum = 0.0;
        float bestFactor = 0.0F;
        SolarPower.Status status = SolarPower.Status.NO_SUN;
        int efficiency = upgrades.efficiencyCount();
        int range = SolarPanelConfig.maxRange();
        Set<BlockPos> visited = new HashSet<>();

        BlockPos seed = origin.above();
        if (!(level.getBlockState(seed).getBlock() instanceof SolarPanelBlock)
                || !(level.getBlockEntity(seed) instanceof SolarPanelBlockEntity seedBe)
                || !seedBe.isFreeOrOwnedBy(level, origin)) {
            releaseUnvisitedClaims(level, origin, visited);
            linkedPanels = 0;
            peakFePerTick = 0;
            lastSunFactor = 0.0F;
            lastStatus = SolarPower.Status.NO_SKY;
            return 0;
        }

        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        BlockPos seedImmutable = seed.immutable();
        queue.add(seedImmutable);
        visited.add(seedImmutable);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        while (!queue.isEmpty()) {
            BlockPos current = queue.removeFirst();
            BlockState state = level.getBlockState(current);
            if (!(state.getBlock() instanceof SolarPanelBlock panel)) {
                continue;
            }
            if (!(level.getBlockEntity(current) instanceof SolarPanelBlockEntity panelBe)) {
                continue;
            }
            if (!panelBe.isFreeOrOwnedBy(level, origin)) {
                // Owned by another valid controller — do not expand through it.
                continue;
            }
            panelBe.claim(origin);
            claimedPanels.add(current);

            panels++;
            int tierFe = panel.tier().fePerTick();
            peak += tierFe;
            SolarPower.Snapshot snap = SolarPower.evaluate(level, current);
            float factor = UpgradeConfig.solarEfficiencyFactor(snap.factor(), efficiency);
            feSum += tierFe * factor;
            if (snap.factor() > bestFactor) {
                bestFactor = snap.factor();
            }
            if (snap.status() == SolarPower.Status.WORKING) {
                status = SolarPower.Status.WORKING;
            } else if (status != SolarPower.Status.WORKING && snap.status() == SolarPower.Status.NO_SKY) {
                status = SolarPower.Status.NO_SKY;
            }

            for (Direction dir : HORIZONTAL) {
                cursor.setWithOffset(current, dir);
                if (chebyshevHorizontal(origin, cursor) > range) {
                    continue;
                }
                if (!(level.getBlockState(cursor).getBlock() instanceof SolarPanelBlock)) {
                    continue;
                }
                BlockPos next = cursor.immutable();
                if (visited.add(next)) {
                    queue.add(next);
                }
            }
        }

        releaseUnvisitedClaims(level, origin, visited);
        linkedPanels = panels;
        peakFePerTick = peak;
        lastSunFactor = bestFactor;
        lastStatus = panels <= 0 ? SolarPower.Status.NO_SKY : status;
        return Math.max(0, (int) Math.round(feSum));
    }

    private void releaseUnvisitedClaims(Level level, BlockPos origin, Set<BlockPos> visited) {
        claimedPanels.removeIf(pos -> {
            if (visited.contains(pos)) {
                return false;
            }
            if (level.getBlockEntity(pos) instanceof SolarPanelBlockEntity panelBe) {
                panelBe.clearOwnerIf(origin);
            }
            return true;
        });
    }

    private void clearAllClaims(Level level) {
        BlockPos origin = worldPosition;
        for (BlockPos pos : claimedPanels) {
            if (level.getBlockEntity(pos) instanceof SolarPanelBlockEntity panelBe) {
                panelBe.clearOwnerIf(origin);
            }
        }
        claimedPanels.clear();
    }

    private static int chebyshevHorizontal(BlockPos origin, BlockPos cell) {
        return Math.max(Math.abs(cell.getX() - origin.getX()), Math.abs(cell.getZ() - origin.getZ()));
    }

    public void dropContents(Level level, BlockPos pos) {
        upgrades.dropAt(level, pos);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null && !level.isClientSide()) {
            clearAllClaims(level);
            dropContents(level, pos);
        }
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        upgrades.loadSlots(input);
        upgrades.trimInstalledCaps();
        energy.load(input);
        energy.applyGeneratorEnergyUpgrades(upgrades.energyCount());
        redstoneMode = RedstoneMode.byOrdinal(input.getIntOr("RedstoneMode", 0));
        linkedPanels = Math.max(0, input.getIntOr("LinkedPanels", 0));
        peakFePerTick = Math.max(0, input.getIntOr("PeakFe", 0));
        lastSunFactor = Math.max(0.0F, input.getFloatOr("SunFactor", 0.0F));
        int statusOrd = input.getIntOr("SunStatus", SolarPower.Status.NO_SUN.ordinal());
        SolarPower.Status[] values = SolarPower.Status.values();
        lastStatus = statusOrd >= 0 && statusOrd < values.length ? values[statusOrd] : SolarPower.Status.NO_SUN;
        overlayColor = input.getIntOr("OverlayColor", OVERLAY_COLOR) & 0xFFFFFF;
        refreshFootprint();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        upgrades.saveSlots(output);
        energy.save(output);
        if (redstoneMode != RedstoneMode.IGNORE) {
            output.putInt("RedstoneMode", redstoneMode.ordinal());
        }
        if (linkedPanels > 0) {
            output.putInt("LinkedPanels", linkedPanels);
        }
        if (peakFePerTick > 0) {
            output.putInt("PeakFe", peakFePerTick);
        }
        if (lastSunFactor > 0.0F) {
            output.putFloat("SunFactor", lastSunFactor);
        }
        output.putInt("SunStatus", lastStatus.ordinal());
        output.putInt("OverlayColor", overlayColor & 0xFFFFFF);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return AdvancedVolumeMachineSupport.createUpdateTag(volume, overlayColor);
    }

    @Override
    public void handleUpdateTag(ValueInput input) {
        applyClientSync(input);
    }

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
