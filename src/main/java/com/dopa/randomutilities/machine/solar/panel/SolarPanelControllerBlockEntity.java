package com.dopa.randomutilities.machine.solar.panel;

import com.dopa.randomutilities.core.machine.AdvancedVolumeMachineSupport;
import com.dopa.randomutilities.core.machine.GeneratorEnergy;
import com.dopa.randomutilities.core.machine.MachineEnergy;
import com.dopa.randomutilities.core.machine.UpgradeInventory;
import com.dopa.randomutilities.core.machine.RedstoneControl;
import com.dopa.randomutilities.core.machine.RedstoneMode;
import com.dopa.randomutilities.core.machine.config.UpgradeConfig;
import com.dopa.randomutilities.registry.ModBlockEntities;
import com.dopa.randomutilities.machine.solar.furnace.SolarPower;
import com.dopa.randomutilities.machine.solar.panel.config.SolarPanelConfig;
import com.dopa.randomutilities.core.util.WorkingVolume;
import com.dopa.randomutilities.core.util.WorkingVolumeSource;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
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
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

public class SolarPanelControllerBlockEntity extends BlockEntity
        implements RedstoneControl, WorkingVolumeSource {
    public static final int OVERLAY_COLOR = 0xF2C94C;

    private final UpgradeInventory upgrades =
            UpgradeInventory.withCaps(UpgradeConfig.UPGRADE_SLOT_COUNT, UpgradeConfig::capSolarPanel);
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
    private final List<BlockPos> claimedPanels = new ArrayList<>();

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

    public UpgradeInventory upgrades() {
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

    public boolean isFormed() {
        return !claimedPanels.isEmpty();
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

    /**
     * Sneak-wrench recast. Replaces the cached blob from the panel on Y+1.
     * @return empty on success, otherwise a status message
     */
    public Optional<Component> tryRecast() {
        if (level == null || level.isClientSide()) {
            return Optional.of(Component.translatable("item.dopasrandomutilities.wrench.form.no_panel"));
        }
        BlockPos seed = SolarArray.seedPos(worldPosition);
        if (!(level.getBlockState(seed).getBlock() instanceof SolarPanelBlock)) {
            return Optional.of(Component.translatable("item.dopasrandomutilities.wrench.form.no_panel"));
        }
        if (!(level.getBlockEntity(seed) instanceof SolarPanelBlockEntity seedBe)
                || !seedBe.isFreeOrOwnedBy(level, worldPosition)) {
            return Optional.of(Component.translatable("item.dopasrandomutilities.wrench.form.claimed"));
        }
        List<BlockPos> blob = SolarArray.collect(level, worldPosition);
        if (blob.isEmpty()) {
            return Optional.of(Component.translatable("item.dopasrandomutilities.wrench.form.no_panel"));
        }
        unform(null);
        HashSet<BlockPos> set = new HashSet<>(blob);
        for (BlockPos pos : blob) {
            if (level.getBlockEntity(pos) instanceof SolarPanelBlockEntity panelBe) {
                panelBe.claim(worldPosition);
            }
        }
        claimedPanels.clear();
        claimedPanels.addAll(blob);
        SolarArray.applyFormed(level, set);
        SolarArray.setControllerFormed(level, worldPosition, true);
        if (level instanceof ServerLevel serverLevel) {
            SolarArray.spawnFormParticles(serverLevel, worldPosition, blob);
        }
        lastComputedFe = evaluateCached(level);
        solarEvalCooldown = SolarPanelConfig.evalInterval();
        setChanged();
        return Optional.empty();
    }

    public void onClaimedPanelRemoved(BlockPos panelPos) {
        if (level == null || level.isClientSide()) {
            return;
        }
        BlockPos immutable = panelPos.immutable();
        if (!claimedPanels.contains(immutable)) {
            return;
        }
        unform(immutable);
    }

    public void unform(@Nullable BlockPos skipPanel) {
        if (level == null) {
            return;
        }
        SolarArray.clearFormed(level, claimedPanels, skipPanel);
        for (BlockPos pos : claimedPanels) {
            if (skipPanel != null && skipPanel.equals(pos)) {
                continue;
            }
            if (level.getBlockEntity(pos) instanceof SolarPanelBlockEntity panelBe) {
                panelBe.clearOwnerIf(worldPosition);
            }
        }
        claimedPanels.clear();
        linkedPanels = 0;
        peakFePerTick = 0;
        lastSunFactor = 0.0F;
        lastStatus = SolarPower.Status.NO_SKY;
        lastComputedFe = 0;
        if (level.getBlockState(worldPosition).getBlock() instanceof SolarPanelControllerBlock) {
            SolarArray.setControllerFormed(level, worldPosition, false);
        }
        setChanged();
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
            lastComputedFe = evaluateCached(level);
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

    /** Sun-eval the cached snapshot only. Unformed controllers generate 0 FE. */
    private int evaluateCached(Level level) {
        if (claimedPanels.isEmpty()) {
            linkedPanels = 0;
            peakFePerTick = 0;
            lastSunFactor = 0.0F;
            lastStatus = SolarPower.Status.NO_SKY;
            return 0;
        }

        for (BlockPos current : claimedPanels) {
            if (!level.isLoaded(current)) {
                return lastComputedFe;
            }
        }

        int panels = 0;
        int peak = 0;
        double feSum = 0.0;
        SolarPower.Status status = SolarPower.Status.NO_SUN;
        int efficiency = upgrades.efficiencyCount();

        for (BlockPos current : claimedPanels) {
            BlockState state = level.getBlockState(current);
            if (!(state.getBlock() instanceof SolarPanelBlock panel)
                    || !(level.getBlockEntity(current) instanceof SolarPanelBlockEntity panelBe)
                    || !panelBe.isOwnedBy(worldPosition)) {
                unform(null);
                return 0;
            }
            panels++;
            int tierFe = panel.tier().fePerTick();
            peak += tierFe;
            SolarPower.Snapshot snap = SolarPower.evaluate(level, current);
            float factor = UpgradeConfig.solarEfficiencyFactor(snap.factor(), efficiency);
            feSum += tierFe * factor;
            if (snap.status() == SolarPower.Status.WORKING) {
                status = SolarPower.Status.WORKING;
            } else if (status != SolarPower.Status.WORKING && snap.status() == SolarPower.Status.NO_SKY) {
                status = SolarPower.Status.NO_SKY;
            }
        }

        linkedPanels = panels;
        peakFePerTick = peak;
        lastSunFactor = peak <= 0 ? 0.0F : (float) (feSum / (double) peak);
        lastStatus = status;
        return Math.max(0, (int) Math.round(feSum));
    }

    public void dropContents(Level level, BlockPos pos) {
        upgrades.dropAt(level, pos);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null && !level.isClientSide()) {
            unform(null);
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
        claimedPanels.clear();
        input.read("Claimed", BlockPos.CODEC.listOf()).ifPresent(claimedPanels::addAll);
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
        if (!claimedPanels.isEmpty()) {
            output.store("Claimed", BlockPos.CODEC.listOf(), List.copyOf(claimedPanels));
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
