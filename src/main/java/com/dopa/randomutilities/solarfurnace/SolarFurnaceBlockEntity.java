package com.dopa.randomutilities.solarfurnace;

import com.dopa.randomutilities.machine.RedstoneControl;
import com.dopa.randomutilities.machine.RedstoneMode;
import com.dopa.randomutilities.machine.config.UpgradeConfig;
import com.dopa.randomutilities.registry.ModBlockEntities;
import com.dopa.randomutilities.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.RangedResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import org.jetbrains.annotations.Nullable;

public class SolarFurnaceBlockEntity extends BlockEntity implements RedstoneControl {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int SLOT_COUNT = 2;
    private static final int SAVE_INTERVAL = 20;

    private final ItemStacksResourceHandler items = new ItemStacksResourceHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int index, ItemStack previousContents) {
            if (!loading && index == SLOT_INPUT) {
                cookingProgress = 0.0F;
                refreshCookTotal();
            }
            if (!loading) {
                setChanged();
            }
        }

        /** Blocks hoppers / pipes from inserting into output; smelting uses {@link #set}. */
        @Override
        public boolean isValid(int index, ItemResource resource) {
            if (index == SLOT_OUTPUT) {
                return false;
            }
            return super.isValid(index, resource);
        }
    };

    private final ResourceHandler<ItemResource> inputHandler =
            RangedResourceHandler.ofSingleIndex(items, SLOT_INPUT);
    private final ResourceHandler<ItemResource> outputHandler =
            RangedResourceHandler.ofSingleIndex(items, SLOT_OUTPUT);

    private final OverclockUpgradeInventory upgrades =
            new OverclockUpgradeInventory(UpgradeConfig.UPGRADE_SLOT_COUNT, UpgradeConfig::maxOverclockSolarFurnace);

    private final RecipeManager.CachedCheck<SingleRecipeInput, ? extends AbstractCookingRecipe> quickCheck =
            RecipeManager.createCheck(RecipeType.SMELTING);

    private boolean loading;
    private float cookingProgress;
    private int cookingTotalTime;
    private int saveTimer;
    private float storedExperience;
    private RedstoneMode redstoneMode = RedstoneMode.IGNORE;
    private SolarPower.Snapshot solarSnapshot = new SolarPower.Snapshot(0.0F, SolarPower.Status.NO_SUN);

    public SolarFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SOLAR_FURNACE.get(), pos, state);
        upgrades.setOnChanged(this::setChanged);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SolarFurnaceBlockEntity be) {
        if (!level.isClientSide()) {
            be.tick((ServerLevel) level, pos, state);
        }
    }

    public ItemStacksResourceHandler items() {
        return items;
    }

    public OverclockUpgradeInventory upgrades() {
        return upgrades;
    }

    public ResourceHandler<ItemResource> itemHandler(@Nullable Direction side) {
        if (side == null) {
            return items;
        }
        if (side == Direction.DOWN) {
            return outputHandler;
        }
        return inputHandler;
    }

    /** Tenths of a tick so fractional solar progress still moves the UI arrow. */
    public int cookingProgressSynced() {
        return Math.max(0, Math.round(cookingProgress * 10.0F));
    }

    public int cookingTotalSynced() {
        return Math.max(0, cookingTotalTime * 10);
    }

    public SolarPower.Snapshot solarSnapshot() {
        return solarSnapshot;
    }

    @Override
    public RedstoneMode redstoneMode() {
        return redstoneMode;
    }

    @Override
    public void setRedstoneMode(RedstoneMode mode) {
        if (this.redstoneMode != mode) {
            this.redstoneMode = mode;
            setChanged();
        }
    }

    public int insertUpgrade(ItemStack stack) {
        if (!stack.is(ModItems.OVERCLOCK_UPGRADE.get())) {
            return 0;
        }
        return upgrades.insertFrom(stack);
    }

    public ItemStack stackInSlot(int slot) {
        ItemResource resource = items.getResource(slot);
        if (resource.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return resource.toStack(items.getAmountAsInt(slot));
    }

    private void tick(ServerLevel level, BlockPos pos, BlockState state) {
        boolean changed = false;
        boolean wasLit = state.getValue(SolarFurnaceBlock.LIT);
        boolean lit = false;

        if (!redstoneMode.allowsOperation(level.getBestNeighborSignal(pos))) {
            if (wasLit) {
                level.setBlock(pos, state.setValue(SolarFurnaceBlock.LIT, false), 3);
                setChanged();
            }
            solarSnapshot = SolarPower.evaluate(level, pos);
            return;
        }

        solarSnapshot = SolarPower.evaluate(level, pos);

        ItemStack input = stackInSlot(SLOT_INPUT);
        if (input.isEmpty()) {
            if (cookingProgress > 0.0F || cookingTotalTime > 0) {
                cookingProgress = 0.0F;
                cookingTotalTime = 0;
                saveTimer = 0;
                changed = true;
            }
            if (wasLit) {
                level.setBlock(pos, state.setValue(SolarFurnaceBlock.LIT, false), 3);
                changed = true;
            }
            if (changed) {
                setChanged();
            }
            return;
        }

        SingleRecipeInput recipeInput = new SingleRecipeInput(input);
        RecipeHolder<? extends AbstractCookingRecipe> recipe =
                quickCheck.getRecipeFor(recipeInput, level).orElse(null);
        if (recipe != null) {
            ItemStack result = recipe.value().assemble(recipeInput);
            int needed = recipe.value().cookingTime();
            if (cookingTotalTime != needed) {
                cookingTotalTime = needed;
                changed = true;
            }
            if (!result.isEmpty() && canAcceptResult(result)) {
                float factor = solarSnapshot.factor()
                        * UpgradeConfig.solarPeakFactor(upgrades.overclockCount());
                if (factor > 0.0F) {
                    cookingProgress += factor;
                    lit = true;
                    if (cookingProgress >= cookingTotalTime) {
                        finishCook(input, result, recipe);
                        cookingProgress = 0.0F;
                        cookingTotalTime = recipe.value().cookingTime();
                        saveTimer = 0;
                        changed = true;
                    } else if (++saveTimer >= SAVE_INTERVAL) {
                        saveTimer = 0;
                        changed = true;
                    }
                }
            } else if (cookingProgress > 0.0F) {
                cookingProgress = 0.0F;
                saveTimer = 0;
                changed = true;
            }
        } else if (cookingProgress > 0.0F || cookingTotalTime > 0) {
            cookingProgress = 0.0F;
            cookingTotalTime = 0;
            saveTimer = 0;
            changed = true;
        }

        if (wasLit != lit) {
            level.setBlock(pos, state.setValue(SolarFurnaceBlock.LIT, lit), 3);
            changed = true;
        }
        if (changed) {
            setChanged();
        }
    }

    private void refreshCookTotal() {
        if (!(level instanceof ServerLevel serverLevel)) {
            cookingTotalTime = 0;
            return;
        }
        ItemStack input = stackInSlot(SLOT_INPUT);
        if (input.isEmpty()) {
            cookingTotalTime = 0;
            return;
        }
        SingleRecipeInput recipeInput = new SingleRecipeInput(input);
        cookingTotalTime = quickCheck.getRecipeFor(recipeInput, serverLevel)
                .map(holder -> holder.value().cookingTime())
                .orElse(0);
    }

    private boolean canAcceptResult(ItemStack result) {
        if (result.isEmpty()) {
            return false;
        }
        ItemStack output = stackInSlot(SLOT_OUTPUT);
        if (output.isEmpty()) {
            return true;
        }
        if (!ItemStack.isSameItemSameComponents(output, result)) {
            return false;
        }
        // Output isValid is false (hopper lock); getCapacityAsInt would be 0.
        return output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    private void finishCook(
            ItemStack input,
            ItemStack result,
            RecipeHolder<? extends AbstractCookingRecipe> recipe
    ) {
        ItemStack output = stackInSlot(SLOT_OUTPUT);
        if (output.isEmpty()) {
            items.set(SLOT_OUTPUT, ItemResource.of(result), result.getCount());
        } else {
            items.set(SLOT_OUTPUT, ItemResource.of(output), output.getCount() + result.getCount());
        }
        ItemStack remaining = input.copy();
        remaining.shrink(1);
        if (remaining.isEmpty()) {
            items.set(SLOT_INPUT, ItemResource.EMPTY, 0);
        } else {
            items.set(SLOT_INPUT, ItemResource.of(remaining), remaining.getCount());
        }
        storedExperience += recipe.value().experience();
    }

    /** Same as a vanilla furnace: XP is held until a player takes the output or the block breaks. */
    public void awardExperience(Player player) {
        if (storedExperience <= 0.0F || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        popExperience(serverLevel, serverPlayer.position());
    }

    private void popExperience(ServerLevel level, Vec3 pos) {
        if (storedExperience <= 0.0F) {
            return;
        }
        int orbs = Mth.floor(storedExperience);
        float fraction = Mth.frac(storedExperience);
        if (fraction > 0.0F && level.getRandom().nextFloat() < fraction) {
            orbs++;
        }
        ExperienceOrb.award(level, pos, orbs);
        storedExperience = 0.0F;
        setChanged();
    }

    public void dropContents(Level level, BlockPos pos) {
        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack stack = stackInSlot(i);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
            }
        }
        items.set(SLOT_INPUT, ItemResource.EMPTY, 0);
        items.set(SLOT_OUTPUT, ItemResource.EMPTY, 0);
        for (int i = 0; i < upgrades.size(); i++) {
            ItemStack stack = upgrades.stackInSlot(i);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
            }
        }
        upgrades.clearContents();
        if (level instanceof ServerLevel serverLevel) {
            popExperience(serverLevel, Vec3.atCenterOf(pos));
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
        loading = true;
        try {
            for (int i = 0; i < SLOT_COUNT; i++) {
                ItemStack stack = input.read("Item" + i, ItemStack.CODEC).orElse(ItemStack.EMPTY);
                if (stack.isEmpty()) {
                    items.set(i, ItemResource.EMPTY, 0);
                } else {
                    items.set(i, ItemResource.of(stack), stack.getCount());
                }
            }
            for (int i = 0; i < upgrades.size(); i++) {
                ItemStack stack = input.read("Upgrade" + i, ItemStack.CODEC).orElse(ItemStack.EMPTY);
                if (stack.isEmpty()) {
                    upgrades.set(i, ItemResource.EMPTY, 0);
                } else {
                    upgrades.set(i, ItemResource.of(stack), stack.getCount());
                }
            }
        } finally {
            loading = false;
        }
        cookingProgress = input.getFloatOr("CookingProgress", 0.0F);
        cookingTotalTime = input.getIntOr("CookingTotalTime", 0);
        storedExperience = Math.max(0.0F, input.getFloatOr("StoredExperience", 0.0F));
        redstoneMode = RedstoneMode.byOrdinal(input.getIntOr("RedstoneMode", 0));
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
        if (cookingProgress > 0.0F) {
            output.putFloat("CookingProgress", cookingProgress);
        }
        if (cookingTotalTime > 0) {
            output.putInt("CookingTotalTime", cookingTotalTime);
        }
        if (storedExperience > 0.0F) {
            output.putFloat("StoredExperience", storedExperience);
        }
        output.putInt("RedstoneMode", redstoneMode.ordinal());
        for (int i = 0; i < upgrades.size(); i++) {
            ItemStack stack = upgrades.stackInSlot(i);
            if (!stack.isEmpty()) {
                output.store("Upgrade" + i, ItemStack.CODEC, stack);
            }
        }
    }
}
