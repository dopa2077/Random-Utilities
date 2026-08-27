package com.dopa.randomutilities.machine.combustion;

import com.dopa.randomutilities.machine.combustion.config.CombustionGeneratorConfig;
import com.dopa.randomutilities.core.machine.GeneratorEnergy;
import com.dopa.randomutilities.core.machine.MachineEnergy;
import com.dopa.randomutilities.core.machine.UpgradeInventory;
import com.dopa.randomutilities.core.machine.RedstoneControl;
import com.dopa.randomutilities.core.machine.RedstoneMode;
import com.dopa.randomutilities.core.machine.config.UpgradeConfig;
import com.dopa.randomutilities.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.FuelValues;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

public class CombustionGeneratorBlockEntity extends BlockEntity implements RedstoneControl {
    public static final int SLOT_FUEL = 0;
    public static final int SLOT_COUNT = 1;
    private static final int SAVE_INTERVAL = 20;

    private final ItemStacksResourceHandler items = new ItemStacksResourceHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int index, ItemStack previousContents) {
            if (!loading) {
                setChanged();
            }
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            if (resource.isEmpty()) {
                return true;
            }
            Level level = getLevel();
            if (level == null) {
                return false;
            }
            return burnDuration(resource.toStack(1), level) > 0;
        }
    };

    private final UpgradeInventory upgrades =
            UpgradeInventory.withCaps(UpgradeConfig.UPGRADE_SLOT_COUNT, UpgradeConfig::capCombustion);
    private final MachineEnergy energy = new MachineEnergy();

    private boolean loading;
    private float burnRemaining;
    private float burnTotal;
    private RedstoneMode redstoneMode = RedstoneMode.IGNORE;
    private int saveTimer;

    public CombustionGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COMBUSTION_GENERATOR.get(), pos, state);
        upgrades.setOnChanged(this::onUpgradesChanged);
        refreshEnergyUpgrades();
    }

    private void onUpgradesChanged() {
        refreshEnergyUpgrades();
        setChanged();
    }

    private void refreshEnergyUpgrades() {
        energy.applyGeneratorEnergyUpgrades(
                upgrades.energyCount(),
                CombustionGeneratorConfig.baseMaxExtract()
        );
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CombustionGeneratorBlockEntity be) {
        if (!level.isClientSide()) {
            be.tick((ServerLevel) level, pos, state);
        }
    }

    public ItemStacksResourceHandler items() {
        return items;
    }

    public UpgradeInventory upgrades() {
        return upgrades;
    }

    public MachineEnergy energy() {
        return energy;
    }

    public float burnRemaining() {
        return burnRemaining;
    }

    public float burnTotal() {
        return burnTotal;
    }

    /** 0–1 remaining fuel fraction for the burn progress UI. */
    public float burnFraction() {
        if (burnTotal <= 0.0F) {
            return 0.0F;
        }
        return Mth.clamp(burnRemaining / burnTotal, 0.0F, 1.0F);
    }

    public int burnProgressSynced() {
        return Math.max(0, Math.round(burnFraction() * 1000.0F));
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

    public ItemStack fuelStack() {
        ItemResource resource = items.getResource(SLOT_FUEL);
        if (resource.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return resource.toStack(items.getAmountAsInt(SLOT_FUEL));
    }

    private void tick(ServerLevel level, BlockPos pos, BlockState state) {
        energy.beginTick();
        int energyBefore = energy.stored();
        boolean powered = redstoneMode == RedstoneMode.IGNORE
                || redstoneMode.allowsOperation(level.getBestNeighborSignal(pos));
        boolean wasLit = state.getValue(CombustionGeneratorBlock.LIT);
        boolean lit = burnRemaining > 0.0F;

        if (powered) {
            if (burnRemaining <= 0.0F) {
                tryIgnite(level);
                lit = burnRemaining > 0.0F;
            }
            if (burnRemaining > 0.0F) {
                int room = energy.capacity() - energy.stored();
                int want = fePerTick();
                if (room > 0 && want > 0) {
                    int generated = energy.tryGenerate(Math.min(want, room));
                    if (generated > 0) {
                        burnRemaining -= fuelDrainPerTick();
                        if (burnRemaining < 0.0F) {
                            burnRemaining = 0.0F;
                        }
                    }
                }
                lit = burnRemaining > 0.0F || canIgnite(level);
            }
        }
        GeneratorEnergy.pushToNeighbors(level, pos, energy);

        boolean litChanged = wasLit != lit;
        if (litChanged) {
            level.setBlock(pos, state.setValue(CombustionGeneratorBlock.LIT, lit), 3);
        }
        boolean energyChanged = energy.stored() != energyBefore;
        if (litChanged || energyChanged) {
            saveTimer = 0;
            setChanged();
        } else if (lit && ++saveTimer >= SAVE_INTERVAL) {
            saveTimer = 0;
            setChanged();
        }
    }

    /**
     * Overclock raises FE/t and burn rate together so total FE per fuel stays fixed
     * (efficiency alone multiplies total FE). Usage panel shows this boosted rate.
     */
    private int fePerTick() {
        int base = CombustionGeneratorConfig.baseFePerTick();
        if (base <= 0) {
            return 0;
        }
        return Math.max(1, Math.round(base * burnSpeedRatio()));
    }

    private float fuelDrainPerTick() {
        return (float) (CombustionGeneratorConfig.burnSpeedFactor() * burnSpeedRatio() / efficiencyMultiplier());
    }

    private float burnSpeedRatio() {
        float burnTotalSafe = Math.max(burnTotal, 1.0F);
        int baseTicks = Math.max(1, Math.round(burnTotalSafe / (float) CombustionGeneratorConfig.burnSpeedFactor()));
        int ticks = UpgradeConfig.effectiveTicks(baseTicks, upgrades.overclockCount());
        return baseTicks / (float) Math.max(1, ticks);
    }

    private double efficiencyMultiplier() {
        return 1.0
                + (UpgradeConfig.efficiencyBonusPercent() / 100.0) * upgrades.efficiencyCount();
    }

    private void tryIgnite(Level level) {
        ItemStack fuel = fuelStack();
        int duration = burnDuration(fuel, level);
        if (duration <= 0) {
            burnRemaining = 0.0F;
            burnTotal = 0.0F;
            return;
        }
        ItemResource resource = items.getResource(SLOT_FUEL);
        int count = items.getAmountAsInt(SLOT_FUEL);
        if (count <= 0 || resource.isEmpty()) {
            return;
        }
        ItemStackTemplate remainder = fuel.getCraftingRemainder();
        items.set(SLOT_FUEL, resource, count - 1);
        if (items.getAmountAsInt(SLOT_FUEL) <= 0) {
            ItemStack rem = remainder != null ? remainder.create() : ItemStack.EMPTY;
            if (!rem.isEmpty()) {
                items.set(SLOT_FUEL, ItemResource.of(rem), rem.getCount());
            }
        }
        burnTotal = duration;
        burnRemaining = duration;
    }

    private boolean canIgnite(Level level) {
        return burnDuration(fuelStack(), level) > 0;
    }

    public static int burnDuration(ItemStack stack, Level level) {
        if (stack.isEmpty()) {
            return 0;
        }
        FuelValues fuels = level.fuelValues();
        return Math.max(0, stack.getBurnTime(RecipeType.SMELTING, fuels));
    }

    public void dropContents(Level level, BlockPos pos) {
        ItemStack fuel = fuelStack();
        if (!fuel.isEmpty()) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), fuel);
        }
        items.set(SLOT_FUEL, ItemResource.EMPTY, 0);
        upgrades.dropAt(level, pos);
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
        loading = true;
        try {
            ItemStack stack = input.read("Item0", ItemStack.CODEC).orElse(ItemStack.EMPTY);
            if (stack.isEmpty()) {
                items.set(SLOT_FUEL, ItemResource.EMPTY, 0);
            } else {
                items.set(SLOT_FUEL, ItemResource.of(stack), stack.getCount());
            }
            upgrades.loadSlots(input);
            upgrades.trimInstalledCaps();
        } finally {
            loading = false;
        }
        energy.load(input);
        refreshEnergyUpgrades();
        burnRemaining = Math.max(0.0F, input.getFloatOr("BurnRemaining", 0.0F));
        burnTotal = Math.max(0.0F, input.getFloatOr("BurnTotal", 0.0F));
        redstoneMode = RedstoneMode.byOrdinal(input.getIntOr("RedstoneMode", 0));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ItemStack fuel = fuelStack();
        if (!fuel.isEmpty()) {
            output.store("Item0", ItemStack.CODEC, fuel);
        }
        upgrades.saveSlots(output);
        energy.save(output);
        if (burnRemaining > 0.0F) {
            output.putFloat("BurnRemaining", burnRemaining);
        }
        if (burnTotal > 0.0F) {
            output.putFloat("BurnTotal", burnTotal);
        }
        if (redstoneMode != RedstoneMode.IGNORE) {
            output.putInt("RedstoneMode", redstoneMode.ordinal());
        }
    }
}
