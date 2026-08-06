package com.dopa.randomutilities.machine.generator;

import com.dopa.randomutilities.machine.generator.config.GeneratorRecipe;
import com.dopa.randomutilities.machine.generator.config.GeneratorRecipeConfig;
import com.dopa.randomutilities.machine.generator.config.GeneratorRecipePresence;
import com.dopa.randomutilities.machine.generator.config.GeneratorResource;
import com.dopa.randomutilities.machine.generator.config.GeneratorType;
import com.dopa.randomutilities.machine.config.UpgradeConfig;
import com.dopa.randomutilities.machine.RedstoneControl;
import com.dopa.randomutilities.machine.RedstoneMode;
import com.dopa.randomutilities.machine.UpgradeInventory;
import com.dopa.randomutilities.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class ResourceGeneratorBlockEntity extends BlockEntity implements RedstoneControl {
    private static final int EFFECT_INTERVAL = 20;
    private static final int REMATCH_INTERVAL = 4;
    private static final int SAVE_INTERVAL = 20;

    private int tickProgress;
    private int effectTimer;
    private int rematchCooldown;
    private int saveTimer;
    private boolean hasActiveMatch;
    private boolean outputBlocked;
    private String activeRecipeId = "";
    private int activeRecipeTicks;
    private int productivityBonusBank;
    private String displayResultId = "";
    private boolean outputLocked;
    private String lockedResultId = "";
    private String lockedRecipeId = "";
    private String lastNonFreeRecipeId = "";
    private String lastNonFreeResultId = "";
    private RedstoneMode redstoneMode = RedstoneMode.IGNORE;
    private final BlockPos[] resourcePositions = new BlockPos[GeneratorRecipe.SIDE_COUNT];
    private final boolean[] hasResource = new boolean[GeneratorRecipe.SIDE_COUNT];
    @Nullable
    private GeneratorRecipe.Match cachedMatch;

    private final UpgradeInventory upgrades =
            new UpgradeInventory(UpgradeConfig.UPGRADE_SLOT_COUNT, () -> UpgradeConfig.maxPerType(type()));

    public ResourceGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RESOURCE_GENERATOR.get(), pos, state);
        Arrays.fill(resourcePositions, BlockPos.ZERO);
        upgrades.setOnChanged(this::setChanged);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ResourceGeneratorBlockEntity be) {
        if (!level.isClientSide()) {
            be.tick((ServerLevel) level);
        }
    }

    public GeneratorType type() {
        return getBlockState().getBlock() instanceof ResourceGeneratorBlock block
                ? block.generatorType()
                : GeneratorType.BASIC_STONE;
    }

    public boolean supportsLockOutput() {
        return type().mode() == GeneratorType.Mode.RECIPE;
    }

    public UpgradeInventory upgrades() {
        return upgrades;
    }

    public int tickProgress() {
        return tickProgress;
    }

    public int effectiveTicks() {
        if (activeRecipeTicks <= 0) {
            return 0;
        }
        return UpgradeConfig.effectiveTicks(activeRecipeTicks, upgrades.overclockCount());
    }

    public boolean hasActiveMatch() {
        return hasActiveMatch;
    }

    public boolean isOutputLocked() {
        return outputLocked;
    }

    @Override
    public RedstoneMode redstoneMode() {
        return redstoneMode;
    }

    public String displayResultId() {
        return displayResultId;
    }

    @Override
    public void setRedstoneMode(RedstoneMode mode) {
        if (mode == null || mode == redstoneMode) {
            return;
        }
        redstoneMode = mode;
        setChanged();
    }

    public void setOutputLocked(boolean locked) {
        if (!supportsLockOutput()) {
            return;
        }
        if (locked == outputLocked) {
            return;
        }
        outputLocked = locked;
        cachedMatch = null;
        rematchCooldown = 0;
        if (!locked) {
            lockedResultId = "";
            lockedRecipeId = "";
        } else {
            lockedRecipeId = "";
            lockedResultId = "";
            GeneratorRecipe current = findRecipeById(activeRecipeId);
            if (current != null && !current.isFreeRecipe()) {
                lockedRecipeId = current.id();
                // Fluid recipes are pinned by recipe id only — display uses ice/magma proxies, not real outputs.
                if (current.result() != null) {
                    lockedResultId = blockId(current.result());
                } else if (!current.isFluidResult() && !displayResultId.isEmpty()) {
                    lockedResultId = displayResultId;
                }
            } else if (!lastNonFreeRecipeId.isEmpty()) {
                lockedRecipeId = lastNonFreeRecipeId;
                GeneratorRecipe last = findRecipeById(lastNonFreeRecipeId);
                if (last != null && last.isFluidResult()) {
                    lockedResultId = "";
                    setDisplayResult(ResourceGeneratorOutput.displayBlockForFluid(last.resultFluid()));
                } else {
                    lockedResultId = lastNonFreeResultId;
                    if (!lockedResultId.isEmpty()) {
                        displayResultId = lockedResultId;
                    }
                }
            }
        }
        setChanged();
        syncDisplay();
    }

    @Nullable
    public GeneratorRecipe displayRecipe() {
        if (outputLocked && !lockedRecipeId.isEmpty()) {
            return findRecipeById(lockedRecipeId);
        }
        if (outputLocked && !lastNonFreeRecipeId.isEmpty()) {
            return findRecipeById(lastNonFreeRecipeId);
        }
        if (cachedMatch != null) {
            return cachedMatch.recipe();
        }
        if (!activeRecipeId.isEmpty()) {
            return findRecipeById(activeRecipeId);
        }
        return null;
    }

    public int missingInputFlags(Level level) {
        GeneratorRecipe recipe = displayRecipe();
        if (recipe == null) {
            return 0;
        }
        return GeneratorRecipePresence.missingFlags(level, worldPosition, recipe);
    }

    public ItemStack ghostSideStack(int orderedIndex) {
        GeneratorRecipe recipe = displayRecipe();
        if (recipe == null) {
            return ItemStack.EMPTY;
        }
        List<GeneratorResource> ordered = GeneratorRecipePresence.orderedSideResources(recipe);
        if (orderedIndex < 0 || orderedIndex >= ordered.size()) {
            return ItemStack.EMPTY;
        }
        return GeneratorRecipePresence.ghostStack(ordered.get(orderedIndex));
    }

    public ItemStack ghostBelowStack() {
        GeneratorRecipe recipe = displayRecipe();
        return recipe == null ? ItemStack.EMPTY : GeneratorRecipePresence.belowGhostStack(recipe);
    }

    public ItemStack displayResultStack() {
        return getDisplayResultBlock()
                .map(block -> new ItemStack(block.asItem()))
                .orElse(ItemStack.EMPTY);
    }

    public void dropUpgrades(Level level, BlockPos pos) {
        for (int i = 0; i < upgrades.size(); i++) {
            ItemStack stack = upgrades.stackInSlot(i);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
            }
        }
        upgrades.clearContents();
    }

    /** Shift-right-click insert: consumes from {@code stack} into upgrade slots. */
    public int insertUpgrade(ItemStack stack) {
        if (!UpgradeConfig.upgradesEnabled(type())) {
            return 0;
        }
        return upgrades.insertFrom(stack);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null && !level.isClientSide()) {
            dropUpgrades(level, pos);
        }
        super.preRemoveSideEffects(pos, state);
    }

    private void tick(ServerLevel level) {
        GeneratorType type = type();
        if (!redstoneMode.allowsOperation(level.getBestNeighborSignal(worldPosition))) {
            return;
        }

        GeneratorRecipe.Match match = resolveMatch(level, type);
        if (match == null) {
            if (hasActiveMatch || tickProgress > 0) {
                resetProgress();
                setChanged();
            }
            return;
        }

        int baseAmount = match.recipe().amount();
        int produceAmount = UpgradeConfig.peekBoostedAmount(baseAmount, upgrades.productivityCount(), productivityBonusBank);
        Block outputOverride = lockedOutputBlock(match.recipe());

        if (outputBlocked) {
            if (!ResourceGeneratorOutput.canStart(level, worldPosition, type, match.recipe(), produceAmount, outputOverride)) {
                return;
            }
            outputBlocked = false;
        } else if (!ResourceGeneratorOutput.canStart(level, worldPosition, type, match.recipe(), produceAmount, outputOverride)) {
            outputBlocked = true;
            if (hasActiveMatch || tickProgress > 0) {
                resetProgress();
                setChanged();
            }
            return;
        }

        if (!sameMatch(match)) {
            applyMatch(match);
            tickProgress = 0;
            if (!outputLocked || lockedResultId.isEmpty()) {
                if (match.recipe().isFluidResult()) {
                    setDisplayResult(ResourceGeneratorOutput.displayBlockForFluid(match.recipe().resultFluid()));
                } else if (!match.recipe().isRandomResult()) {
                    setDisplayResult(match.recipe().result());
                }
            }
            setChanged();
        }

        tickProgress++;
        tickEffects(level);

        if (++saveTimer >= SAVE_INTERVAL) {
            saveTimer = 0;
            setChanged();
        }

        int neededTicks = UpgradeConfig.effectiveTicks(match.recipe().ticks(), upgrades.overclockCount());
        if (tickProgress < neededTicks) {
            return;
        }

        int produced;
        if (match.recipe().isFluidResult()) {
            Fluid fluid = match.recipe().resultFluid();
            produced = ResourceGeneratorOutput.insertFluid(level, worldPosition, fluid, produceAmount, false);
            if (fluid == null || produced <= 0) {
                outputBlocked = true;
                resetProgress();
                setChanged();
                return;
            }
            setDisplayResult(ResourceGeneratorOutput.displayBlockForFluid(fluid));
            rememberNonFree(match.recipe(), null);
            pinLockFromCurrent(match.recipe());
        } else {
            Block result = outputOverride != null
                    ? outputOverride
                    : ResourceGeneratorOutput.resolveResult(level, type, match.recipe());
            produced = ResourceGeneratorOutput.outputBlocks(level, worldPosition, result, produceAmount, match.recipe().outputMode(), false);
            if (result == null || produced <= 0) {
                outputBlocked = true;
                resetProgress();
                setChanged();
                return;
            }
            setDisplayResult(result);
            rememberNonFree(match.recipe(), result);
            pinLockFromCurrent(match.recipe());
        }

        if (produced >= produceAmount) {
            consumeResources(level, match);
            productivityBonusBank = UpgradeConfig.advanceProductivityBank(
                    baseAmount, upgrades.productivityCount(), productivityBonusBank);
        }
        tickProgress = 0;
        setChanged();
    }

    private void pinLockFromCurrent(GeneratorRecipe recipe) {
        if (!outputLocked || !supportsLockOutput() || recipe.isFreeRecipe()) {
            return;
        }
        if (lockedRecipeId.isEmpty()) {
            lockedRecipeId = recipe.id();
        }
        if (recipe.isFluidResult()) {
            // Keep lock on the recipe; never pin ice/magma display proxies as the output block.
            return;
        }
        if (lockedResultId.isEmpty() && !displayResultId.isEmpty()) {
            lockedResultId = displayResultId;
        }
    }

    private void rememberNonFree(GeneratorRecipe recipe, @Nullable Block result) {
        if (recipe.isFreeRecipe()) {
            return;
        }
        lastNonFreeRecipeId = recipe.id();
        if (recipe.isFluidResult()) {
            lastNonFreeResultId = "";
            return;
        }
        if (result != null) {
            String id = blockId(result);
            if (!id.isEmpty()) {
                lastNonFreeResultId = id;
            }
        }
    }

    private static String blockId(Block block) {
        return Optional.ofNullable(BuiltInRegistries.BLOCK.getKey(block))
                .map(Identifier::toString)
                .orElse("");
    }

    @Nullable
    private Block lockedOutputBlock(GeneratorRecipe matchedRecipe) {
        if (matchedRecipe.isFluidResult()) {
            return null;
        }
        if (!outputLocked || !supportsLockOutput() || lockedResultId.isEmpty()) {
            return null;
        }
        Identifier id = Identifier.tryParse(lockedResultId);
        if (id == null || !BuiltInRegistries.BLOCK.containsKey(id)) {
            return null;
        }
        return BuiltInRegistries.BLOCK.getValue(id);
    }

    @Nullable
    private GeneratorRecipe findRecipeById(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        for (GeneratorRecipe recipe : GeneratorRecipeConfig.getRecipes(type())) {
            if (recipe.id().equals(id)) {
                return recipe;
            }
        }
        return null;
    }

    @Nullable
    private GeneratorRecipe.Match resolveMatch(ServerLevel level, GeneratorType type) {
        if (cachedMatch != null && rematchCooldown-- > 0) {
            if (!outputLocked
                    || lockedRecipeId.isEmpty()
                    || cachedMatch.recipe().id().equals(lockedRecipeId)) {
                return cachedMatch;
            }
        }

        List<GeneratorRecipe> recipes = GeneratorRecipeConfig.getRecipes(type);
        if (outputLocked && supportsLockOutput()) {
            if (!lockedRecipeId.isEmpty()) {
                GeneratorRecipe locked = findRecipeById(lockedRecipeId);
                if (locked != null && locked.isFreeRecipe()) {
                    lockedRecipeId = "";
                    lockedResultId = "";
                    cachedMatch = GeneratorRecipe.findBestMatch(level, worldPosition, recipes, true).orElse(null);
                    if (cachedMatch != null) {
                        GeneratorRecipe recipe = cachedMatch.recipe();
                        lockedRecipeId = recipe.id();
                        if (recipe.isFluidResult()) {
                            lockedResultId = "";
                            setDisplayResult(ResourceGeneratorOutput.displayBlockForFluid(recipe.resultFluid()));
                            rememberNonFree(recipe, null);
                        } else if (recipe.result() != null) {
                            lockedResultId = blockId(recipe.result());
                            setDisplayResult(recipe.result());
                            rememberNonFree(recipe, recipe.result());
                        }
                        syncDisplay();
                    }
                } else {
                    cachedMatch = locked == null
                            ? null
                            : GeneratorRecipe.tryMatchAt(level, worldPosition, locked).orElse(null);
                }
            } else {
                cachedMatch = GeneratorRecipe.findBestMatch(level, worldPosition, recipes, true).orElse(null);
                if (cachedMatch != null) {
                    GeneratorRecipe recipe = cachedMatch.recipe();
                    lockedRecipeId = recipe.id();
                    if (recipe.isFluidResult()) {
                        lockedResultId = "";
                        setDisplayResult(ResourceGeneratorOutput.displayBlockForFluid(recipe.resultFluid()));
                        rememberNonFree(recipe, null);
                    } else if (recipe.result() != null) {
                        lockedResultId = blockId(recipe.result());
                        setDisplayResult(recipe.result());
                        rememberNonFree(recipe, recipe.result());
                    }
                    syncDisplay();
                }
            }
        } else {
            cachedMatch = GeneratorRecipe.findBestMatch(level, worldPosition, recipes).orElse(null);
        }
        rematchCooldown = REMATCH_INTERVAL;
        return cachedMatch;
    }


    private void setDisplayResult(@Nullable Block result) {
        String newId = result == null ? "" : Optional.ofNullable(BuiltInRegistries.BLOCK.getKey(result))
                .map(Identifier::toString).orElse("");
        if (newId.equals(displayResultId)) {
            return;
        }
        displayResultId = newId;
        setChanged();
        syncDisplay();
    }

    private void syncDisplay() {
        if (level instanceof ServerLevel serverLevel) {
            BlockState state = getBlockState();
            serverLevel.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    public Optional<Block> getDisplayResultBlock() {
        if (displayResultId.isEmpty()) {
            return Optional.empty();
        }
        Identifier id = Identifier.tryParse(displayResultId);
        return id != null && BuiltInRegistries.BLOCK.containsKey(id)
                ? Optional.of(BuiltInRegistries.BLOCK.getValue(id))
                : Optional.empty();
    }

    public Block defaultDisplayBlock() {
        return switch (type().mode()) {
            case RECIPE -> Blocks.COBBLESTONE;
            case RANDOM_ORE -> Blocks.DIAMOND_ORE;
            case METAL_BLOCK -> Blocks.IRON_BLOCK;
        };
    }


    private boolean sameMatch(GeneratorRecipe.Match match) {
        if (!match.recipe().id().equals(activeRecipeId) || !hasActiveMatch) {
            return false;
        }
        for (int i = 0; i < GeneratorRecipe.SIDE_COUNT; i++) {
            BlockPos matched = match.resourcePositions()[i];
            boolean present = matched != null;
            if (present != hasResource[i] || (present && !matched.equals(resourcePositions[i]))) {
                return false;
            }
        }
        return true;
    }

    private void applyMatch(GeneratorRecipe.Match match) {
        activeRecipeId = match.recipe().id();
        activeRecipeTicks = match.recipe().ticks();
        hasActiveMatch = true;
        for (int i = 0; i < GeneratorRecipe.SIDE_COUNT; i++) {
            BlockPos matched = match.resourcePositions()[i];
            hasResource[i] = matched != null;
            resourcePositions[i] = matched == null ? BlockPos.ZERO : matched.immutable();
        }
        syncDisplay();
    }


    private void consumeResources(ServerLevel level, GeneratorRecipe.Match match) {
        boolean[] consume = match.recipe().consume();
        BlockPos[] positions = match.resourcePositions();
        for (int i = 0; i < GeneratorRecipe.SIDE_COUNT; i++) {
            if (consume[i] && positions[i] != null) {
                level.setBlock(positions[i], Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        cachedMatch = null;
        rematchCooldown = 0;
    }

    private void tickEffects(ServerLevel level) {
        if (++effectTimer < EFFECT_INTERVAL) {
            return;
        }
        effectTimer = 0;
        double x = worldPosition.getX() + 0.5;
        double y = worldPosition.getY() + 1.05;
        double z = worldPosition.getZ() + 0.5;
        level.sendParticles(ParticleTypes.SMOKE, x, y, z, 2, 0.12, 0.05, 0.12, 0.005);
        level.sendParticles(ParticleTypes.FLAME, x, y, z, 1, 0.08, 0.02, 0.08, 0.0);
        level.playSound(null, worldPosition, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS,
                0.18F, 0.85F + level.getRandom().nextFloat() * 0.2F);
    }

    private void resetProgress() {
        tickProgress = 0;
        effectTimer = 0;
        saveTimer = 0;
        hasActiveMatch = false;
        activeRecipeId = "";
        activeRecipeTicks = 0;
        cachedMatch = null;
        rematchCooldown = 0;
        Arrays.fill(hasResource, false);
        Arrays.fill(resourcePositions, BlockPos.ZERO);
        syncDisplay();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        tickProgress = input.getIntOr("TickProgress", 0);
        displayResultId = input.getStringOr("DisplayResultId", "");
        outputLocked = input.getBooleanOr("OutputLocked", false);
        lockedResultId = input.getStringOr("LockedResultId", "");
        lockedRecipeId = input.getStringOr("LockedRecipeId", "");
        lastNonFreeRecipeId = input.getStringOr("LastNonFreeRecipeId", "");
        lastNonFreeResultId = input.getStringOr("LastNonFreeResultId", "");
        redstoneMode = RedstoneMode.byOrdinal(input.getIntOr("RedstoneMode", 0));
        activeRecipeId = input.getStringOr("ActiveRecipeId", "");
        activeRecipeTicks = input.getIntOr("ActiveRecipeTicks", 0);
        productivityBonusBank = Math.max(0, input.getIntOr(
                "ProductivityBonusBank",
                input.getIntOr("CapacityBonusBank", 0)
        ));
        for (int i = 0; i < upgrades.size(); i++) {
            ItemStack stack = input.read("Upgrade" + i, ItemStack.CODEC).orElse(ItemStack.EMPTY);
            if (stack.isEmpty()) {
                upgrades.set(i, ItemResource.EMPTY, 0);
            } else {
                upgrades.set(i, ItemResource.of(stack), stack.getCount());
            }
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("TickProgress", tickProgress);
        if (!displayResultId.isEmpty()) {
            output.putString("DisplayResultId", displayResultId);
        }
        output.putBoolean("OutputLocked", outputLocked);
        if (!lockedResultId.isEmpty()) {
            output.putString("LockedResultId", lockedResultId);
        }
        if (!lockedRecipeId.isEmpty()) {
            output.putString("LockedRecipeId", lockedRecipeId);
        }
        if (!lastNonFreeRecipeId.isEmpty()) {
            output.putString("LastNonFreeRecipeId", lastNonFreeRecipeId);
        }
        if (!lastNonFreeResultId.isEmpty()) {
            output.putString("LastNonFreeResultId", lastNonFreeResultId);
        }
        output.putInt("RedstoneMode", redstoneMode.ordinal());
        if (!activeRecipeId.isEmpty()) {
            output.putString("ActiveRecipeId", activeRecipeId);
        }
        if (activeRecipeTicks > 0) {
            output.putInt("ActiveRecipeTicks", activeRecipeTicks);
        }
        if (productivityBonusBank > 0) {
            output.putInt("ProductivityBonusBank", productivityBonusBank);
        }
        for (int i = 0; i < upgrades.size(); i++) {
            ItemStack stack = upgrades.stackInSlot(i);
            if (!stack.isEmpty()) {
                output.store("Upgrade" + i, ItemStack.CODEC, stack);
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        if (!displayResultId.isEmpty()) {
            tag.putString("DisplayResultId", displayResultId);
        }
        tag.putBoolean("OutputLocked", outputLocked);
        if (!lockedRecipeId.isEmpty()) {
            tag.putString("LockedRecipeId", lockedRecipeId);
        }
        if (!lockedResultId.isEmpty()) {
            tag.putString("LockedResultId", lockedResultId);
        }
        if (!lastNonFreeRecipeId.isEmpty()) {
            tag.putString("LastNonFreeRecipeId", lastNonFreeRecipeId);
        }
        if (!lastNonFreeResultId.isEmpty()) {
            tag.putString("LastNonFreeResultId", lastNonFreeResultId);
        }
        if (!activeRecipeId.isEmpty()) {
            tag.putString("ActiveRecipeId", activeRecipeId);
        }
        return tag;
    }

    @Override
    public void handleUpdateTag(ValueInput input) {
        applyClientSync(input);
    }

    /**
     * NeoForge's default {@code onDataPacket} calls {@code loadWithComponents}, which would run
     * {@link #loadAdditional} with the sparse update tag and wipe client-side upgrade stacks
     * (menu slots read those stacks via {@code StackCopySlot}).
     */
    @Override
    public void onDataPacket(Connection net, ValueInput valueInput) {
        applyClientSync(valueInput);
    }

    private void applyClientSync(ValueInput input) {
        displayResultId = input.getStringOr("DisplayResultId", "");
        outputLocked = input.getBooleanOr("OutputLocked", false);
        lockedRecipeId = input.getStringOr("LockedRecipeId", "");
        lockedResultId = input.getStringOr("LockedResultId", "");
        lastNonFreeRecipeId = input.getStringOr("LastNonFreeRecipeId", "");
        lastNonFreeResultId = input.getStringOr("LastNonFreeResultId", "");
        activeRecipeId = input.getStringOr("ActiveRecipeId", "");
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
