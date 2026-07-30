package com.dopa.randomutilities.blockentity;

import com.dopa.randomutilities.block.ResourceGeneratorBlock;
import com.dopa.randomutilities.config.GeneratedBlockLists;
import com.dopa.randomutilities.config.GeneratorOutputMode;
import com.dopa.randomutilities.config.GeneratorRecipe;
import com.dopa.randomutilities.config.GeneratorRecipeConfig;
import com.dopa.randomutilities.config.GeneratorRecipeMatcher;
import com.dopa.randomutilities.config.GeneratorType;
import com.dopa.randomutilities.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class ResourceGeneratorBlockEntity extends BlockEntity {
    private static final int EFFECT_INTERVAL_TICKS = 20;
    private static final int REMATCH_INTERVAL_TICKS = 4;
    private static final int MAX_NEARBY_ITEM_ENTITIES = 24;
    private static final int SAVE_PROGRESS_INTERVAL = 20;

    private int tickProgress;
    private int effectTimer;
    private int rematchCooldown;
    private int saveTimer;
    private boolean hasActiveMatch;
    private boolean outputBlocked;
    private String activeRecipeId = "";
    /** ResourceLocation string of the block shown in the inside cube. */
    private String displayResultId = "";
    private final BlockPos[] resourcePositions = new BlockPos[GeneratorRecipe.SIDE_COUNT];
    private final boolean[] hasResource = new boolean[GeneratorRecipe.SIDE_COUNT];
    @Nullable
    private GeneratorRecipeMatcher.Match cachedMatch;

    public ResourceGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RESOURCE_GENERATOR.get(), pos, state);
        Arrays.fill(resourcePositions, BlockPos.ZERO);
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            ResourceGeneratorBlockEntity blockEntity
    ) {
        if (level.isClientSide()) {
            return;
        }
        blockEntity.tick((ServerLevel) level);
    }

    private GeneratorType generatorType() {
        if (getBlockState().getBlock() instanceof ResourceGeneratorBlock block) {
            return block.generatorType();
        }
        return GeneratorType.BASIC_STONE;
    }

    private void tick(ServerLevel level) {
        GeneratorType type = generatorType();
        GeneratorRecipeMatcher.Match match = resolveMatch(level, type);

        if (match == null) {
            if (hasActiveMatch || tickProgress > 0) {
                resetProgress();
                setChanged();
            }
            return;
        }

        if (outputBlocked) {
            // Cheap re-check: only resume when output space looks available again.
            if (!canStartOutput(level, type, match.recipe())) {
                return;
            }
            outputBlocked = false;
        } else if (!canStartOutput(level, type, match.recipe())) {
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
            if (match.recipe().isFluidResult()) {
                setDisplayResult(displayBlockForFluid(match.recipe().resultFluid()));
            } else if (!match.recipe().isRandomResult()) {
                setDisplayResult(match.recipe().result());
            }
            setChanged();
        }

        tickProgress++;
        tickRunningEffects(level);

        // Avoid dirtying the chunk every tick; checkpoint progress periodically for crash safety.
        saveTimer++;
        if (saveTimer >= SAVE_PROGRESS_INTERVAL) {
            saveTimer = 0;
            setChanged();
        }

        if (tickProgress < match.recipe().ticks()) {
            return;
        }

        int produced;
        if (match.recipe().isFluidResult()) {
            Fluid fluid = match.recipe().resultFluid();
            produced = tryOutputFluid(level, fluid, match.recipe().amount());
            if (fluid == null || produced <= 0) {
                outputBlocked = true;
                resetProgress();
                setChanged();
                return;
            }
            setDisplayResult(displayBlockForFluid(fluid));
        } else {
            Block result = resolveResult(level, type, match.recipe());
            produced = tryOutput(level, result, match.recipe().amount(), match.recipe().outputMode());
            if (result == null || produced <= 0) {
                outputBlocked = true;
                resetProgress();
                setChanged();
                return;
            }
            setDisplayResult(result);
        }

        // Only consume side resources when the full recipe amount was delivered.
        if (produced >= match.recipe().amount()) {
            consumeResources(level, match);
        }

        tickProgress = 0;
        setChanged();
    }

    @Nullable
    private GeneratorRecipeMatcher.Match resolveMatch(ServerLevel level, GeneratorType type) {
        if (cachedMatch != null && rematchCooldown > 0) {
            rematchCooldown--;
            return cachedMatch;
        }

        GeneratorRecipeMatcher.Match match = GeneratorRecipeMatcher
                .findBestMatch(level, worldPosition, GeneratorRecipeConfig.getRecipes(type))
                .orElse(null);
        cachedMatch = match;
        rematchCooldown = REMATCH_INTERVAL_TICKS;
        return match;
    }

    @Nullable
    private Block resolveResult(ServerLevel level, GeneratorType type, GeneratorRecipe recipe) {
        if (!recipe.isRandomResult()) {
            return recipe.result();
        }

        List<Block> pool = poolFor(type);
        if (pool.isEmpty()) {
            return null;
        }
        return pool.get(level.getRandom().nextInt(pool.size()));
    }

    private void setDisplayResult(@Nullable Block result) {
        String newId = "";
        if (result != null) {
            Identifier key = BuiltInRegistries.BLOCK.getKey(result);
            if (key != null) {
                newId = key.toString();
            }
        }
        if (newId.equals(displayResultId)) {
            return;
        }
        displayResultId = newId;
        setChanged();
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
        if (id == null || !BuiltInRegistries.BLOCK.containsKey(id)) {
            return Optional.empty();
        }
        return Optional.of(BuiltInRegistries.BLOCK.getValue(id));
    }

    public Block defaultDisplayBlock() {
        return switch (generatorType().mode()) {
            case RECIPE -> Blocks.COBBLESTONE;
            case RANDOM_ORE -> Blocks.DIAMOND_ORE;
            case METAL_BLOCK -> Blocks.IRON_BLOCK;
        };
    }

    private List<Block> poolFor(GeneratorType type) {
        return switch (type.mode()) {
            case RANDOM_ORE -> GeneratedBlockLists.ores();
            case METAL_BLOCK -> GeneratedBlockLists.metalBlocks();
            case RECIPE -> List.of();
        };
    }

    private boolean canStartOutput(ServerLevel level, GeneratorType type, GeneratorRecipe recipe) {
        if (recipe.isFluidResult()) {
            return canOutputFluid(level, recipe.resultFluid(), recipe.amount());
        }
        if (recipe.isRandomResult()) {
            if (poolFor(type).isEmpty()) {
                return false;
            }
            return switch (recipe.outputMode()) {
                case INSERT -> getItemOutputHandler(level) != null;
                case DROP -> canAcceptItemDrop(level);
                case PLACE -> canAcceptPlace(level);
            };
        }
        return canOutput(level, recipe.result(), recipe.amount(), recipe.outputMode());
    }

    private boolean sameMatch(GeneratorRecipeMatcher.Match match) {
        if (!match.recipe().id().equals(activeRecipeId) || !hasActiveMatch) {
            return false;
        }
        for (int i = 0; i < GeneratorRecipe.SIDE_COUNT; i++) {
            BlockPos matched = match.resourcePositions()[i];
            boolean present = matched != null;
            if (present != hasResource[i]) {
                return false;
            }
            if (present && !matched.equals(resourcePositions[i])) {
                return false;
            }
        }
        return true;
    }

    private void applyMatch(GeneratorRecipeMatcher.Match match) {
        activeRecipeId = match.recipe().id();
        hasActiveMatch = true;
        for (int i = 0; i < GeneratorRecipe.SIDE_COUNT; i++) {
            BlockPos matched = match.resourcePositions()[i];
            hasResource[i] = matched != null;
            resourcePositions[i] = matched == null ? BlockPos.ZERO : matched.immutable();
        }
    }

    @Nullable
    private ResourceHandler<ItemResource> getItemOutputHandler(ServerLevel level) {
        return level.getCapability(Capabilities.Item.BLOCK, worldPosition.above(), Direction.DOWN);
    }

    @Nullable
    private ResourceHandler<FluidResource> getFluidOutputHandler(ServerLevel level) {
        return level.getCapability(Capabilities.Fluid.BLOCK, worldPosition.above(), Direction.DOWN);
    }

    private boolean canOutput(ServerLevel level, Block result, int amount, GeneratorOutputMode mode) {
        return switch (mode) {
            case INSERT -> insertAmount(level, result, amount, true) > 0;
            case DROP -> canAcceptItemDrop(level);
            case PLACE -> canAcceptPlace(level);
        };
    }

    private boolean canOutputFluid(ServerLevel level, @Nullable Fluid fluid, int amount) {
        if (fluid == null || fluid.isSame(Fluids.EMPTY)) {
            return false;
        }
        // Fluid recipes always insert into a tank above the generator.
        return insertFluidAmount(level, fluid, amount, true) > 0;
    }

    /**
     * @return number of items/blocks actually produced (0 = failed)
     */
    private int tryOutput(ServerLevel level, @Nullable Block result, int amount, GeneratorOutputMode mode) {
        if (result == null || amount <= 0) {
            return 0;
        }
        return switch (mode) {
            case INSERT -> insertAmount(level, result, amount, false);
            case DROP -> tryDropItems(level, result, amount) ? amount : 0;
            // Place always sets exactly one block above; recipes should use amount 1.
            case PLACE -> tryPlaceBlock(level, result) ? 1 : 0;
        };
    }

    /**
     * @return number of buckets actually produced (0 = failed)
     */
    private int tryOutputFluid(ServerLevel level, @Nullable Fluid fluid, int amount) {
        if (fluid == null || amount <= 0) {
            return 0;
        }
        return insertFluidAmount(level, fluid, amount, false);
    }

    private int insertAmount(ServerLevel level, Block result, int amount, boolean simulate) {
        ResourceHandler<ItemResource> handler = getItemOutputHandler(level);
        if (handler == null || amount <= 0) {
            return 0;
        }

        ItemResource resource = ItemResource.of(result.asItem());
        if (resource.isEmpty()) {
            return 0;
        }

        try (Transaction tx = Transaction.open(null)) {
            int inserted = handler.insert(resource, amount, tx);
            if (inserted > 0 && !simulate) {
                tx.commit();
            }
            return Math.max(0, inserted);
        }
    }

    /**
     * Inserts {@code amount} buckets into a fluid handler above the generator.
     *
     * @return number of whole buckets inserted
     */
    private int insertFluidAmount(ServerLevel level, Fluid fluid, int amount, boolean simulate) {
        ResourceHandler<FluidResource> handler = getFluidOutputHandler(level);
        if (handler == null || amount <= 0) {
            return 0;
        }

        FluidResource resource = FluidResource.of(fluid);
        if (resource.isEmpty()) {
            return 0;
        }

        long millibuckets = (long) amount * FluidType.BUCKET_VOLUME;
        if (millibuckets > Integer.MAX_VALUE) {
            millibuckets = Integer.MAX_VALUE;
        }

        try (Transaction tx = Transaction.open(null)) {
            int insertedMb = handler.insert(resource, (int) millibuckets, tx);
            if (insertedMb > 0 && !simulate) {
                tx.commit();
            }
            return Math.max(0, insertedMb / FluidType.BUCKET_VOLUME);
        }
    }

    private static Block displayBlockForFluid(@Nullable Fluid fluid) {
        if (fluid == null || fluid.isSame(Fluids.EMPTY)) {
            return Blocks.GLASS;
        }
        if (fluid.isSame(Fluids.WATER)) {
            return Blocks.ICE;
        }
        if (fluid.isSame(Fluids.LAVA)) {
            return Blocks.MAGMA_BLOCK;
        }
        return Blocks.GLASS;
    }

    private boolean canAcceptPlace(ServerLevel level) {
        return level.getBlockState(worldPosition.above()).canBeReplaced();
    }

    private boolean canAcceptItemDrop(ServerLevel level) {
        BlockPos above = worldPosition.above();
        if (!level.getBlockState(above).canBeReplaced()) {
            return false;
        }
        AABB area = new AABB(above).inflate(1.5);
        return level.getEntitiesOfClass(ItemEntity.class, area).size() < MAX_NEARBY_ITEM_ENTITIES;
    }

    private boolean tryPlaceBlock(ServerLevel level, Block result) {
        BlockPos above = worldPosition.above();
        if (!level.getBlockState(above).canBeReplaced()) {
            return false;
        }
        return level.setBlock(above, result.defaultBlockState(), Block.UPDATE_ALL);
    }

    private boolean tryDropItems(ServerLevel level, Block result, int amount) {
        if (!canAcceptItemDrop(level)) {
            return false;
        }

        ItemStack probe = new ItemStack(result.asItem(), 1);
        if (probe.isEmpty()) {
            return false;
        }

        BlockPos above = worldPosition.above();
        double x = above.getX() + 0.5;
        double y = above.getY() + 0.15;
        double z = above.getZ() + 0.5;
        int maxStack = Math.max(1, probe.getMaxStackSize());
        int remaining = amount;

        while (remaining > 0) {
            int batch = Math.min(remaining, maxStack);
            ItemStack stack = new ItemStack(result.asItem(), batch);
            ItemEntity entity = new ItemEntity(level, x, y, z, stack);
            entity.setDefaultPickUpDelay();
            level.addFreshEntity(entity);
            remaining -= batch;
        }
        return true;
    }

    private void consumeResources(ServerLevel level, GeneratorRecipeMatcher.Match match) {
        boolean[] consume = match.recipe().consume();
        BlockPos[] positions = match.resourcePositions();
        for (int i = 0; i < GeneratorRecipe.SIDE_COUNT; i++) {
            if (!consume[i] || positions[i] == null) {
                continue;
            }
            level.setBlock(positions[i], Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        // Side blocks changed — force a rematch next tick.
        cachedMatch = null;
        rematchCooldown = 0;
    }

    private void tickRunningEffects(ServerLevel level) {
        effectTimer++;
        if (effectTimer < EFFECT_INTERVAL_TICKS) {
            return;
        }
        effectTimer = 0;

        double x = worldPosition.getX() + 0.5;
        double y = worldPosition.getY() + 1.05;
        double z = worldPosition.getZ() + 0.5;

        level.sendParticles(ParticleTypes.SMOKE, x, y, z, 2, 0.12, 0.05, 0.12, 0.005);
        level.sendParticles(ParticleTypes.FLAME, x, y, z, 1, 0.08, 0.02, 0.08, 0.0);
        level.playSound(
                null,
                worldPosition,
                SoundEvents.FURNACE_FIRE_CRACKLE,
                SoundSource.BLOCKS,
                0.18F,
                0.85F + level.getRandom().nextFloat() * 0.2F
        );
    }

    private void resetProgress() {
        tickProgress = 0;
        effectTimer = 0;
        saveTimer = 0;
        hasActiveMatch = false;
        activeRecipeId = "";
        cachedMatch = null;
        rematchCooldown = 0;
        Arrays.fill(hasResource, false);
        Arrays.fill(resourcePositions, BlockPos.ZERO);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        tickProgress = input.getIntOr("TickProgress", 0);
        displayResultId = input.getStringOr("DisplayResultId", "");
        // Match caches are rebuilt from the world; do not restore them.
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("TickProgress", tickProgress);
        if (!displayResultId.isEmpty()) {
            output.putString("DisplayResultId", displayResultId);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        if (!displayResultId.isEmpty()) {
            tag.putString("DisplayResultId", displayResultId);
        }
        return tag;
    }

    @Override
    public void handleUpdateTag(ValueInput input) {
        displayResultId = input.getStringOr("DisplayResultId", "");
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
