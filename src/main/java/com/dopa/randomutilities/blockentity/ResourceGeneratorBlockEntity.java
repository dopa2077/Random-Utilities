package com.dopa.randomutilities.blockentity;

import com.dopa.randomutilities.block.ResourceGeneratorBlock;
import com.dopa.randomutilities.config.GeneratorOutputMode;
import com.dopa.randomutilities.config.GeneratorRecipe;
import com.dopa.randomutilities.config.GeneratorRecipeConfig;
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
    private static final int EFFECT_INTERVAL = 20;
    private static final int REMATCH_INTERVAL = 4;
    private static final int MAX_NEARBY_ITEMS = 24;
    private static final int SAVE_INTERVAL = 20;

    private int tickProgress;
    private int effectTimer;
    private int rematchCooldown;
    private int saveTimer;
    private boolean hasActiveMatch;
    private boolean outputBlocked;
    private String activeRecipeId = "";
    private String displayResultId = "";
    private final BlockPos[] resourcePositions = new BlockPos[GeneratorRecipe.SIDE_COUNT];
    private final boolean[] hasResource = new boolean[GeneratorRecipe.SIDE_COUNT];
    @Nullable
    private GeneratorRecipe.Match cachedMatch;

    public ResourceGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RESOURCE_GENERATOR.get(), pos, state);
        Arrays.fill(resourcePositions, BlockPos.ZERO);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ResourceGeneratorBlockEntity be) {
        if (!level.isClientSide()) {
            be.tick((ServerLevel) level);
        }
    }

    private GeneratorType type() {
        return getBlockState().getBlock() instanceof ResourceGeneratorBlock block
                ? block.generatorType()
                : GeneratorType.BASIC_STONE;
    }

    private void tick(ServerLevel level) {
        GeneratorType type = type();
        GeneratorRecipe.Match match = resolveMatch(level, type);
        if (match == null) {
            if (hasActiveMatch || tickProgress > 0) {
                resetProgress();
                setChanged();
            }
            return;
        }

        if (outputBlocked) {
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
        tickEffects(level);

        if (++saveTimer >= SAVE_INTERVAL) {
            saveTimer = 0;
            setChanged();
        }

        if (tickProgress < match.recipe().ticks()) {
            return;
        }

        int produced;
        if (match.recipe().isFluidResult()) {
            Fluid fluid = match.recipe().resultFluid();
            produced = insertFluid(level, fluid, match.recipe().amount(), false);
            if (fluid == null || produced <= 0) {
                outputBlocked = true;
                resetProgress();
                setChanged();
                return;
            }
            setDisplayResult(displayBlockForFluid(fluid));
        } else {
            Block result = resolveResult(level, type, match.recipe());
            produced = outputBlocks(level, result, match.recipe().amount(), match.recipe().outputMode(), false);
            if (result == null || produced <= 0) {
                outputBlocked = true;
                resetProgress();
                setChanged();
                return;
            }
            setDisplayResult(result);
        }

        if (produced >= match.recipe().amount()) {
            consumeResources(level, match);
        }
        tickProgress = 0;
        setChanged();
    }

    @Nullable
    private GeneratorRecipe.Match resolveMatch(ServerLevel level, GeneratorType type) {
        if (cachedMatch != null && rematchCooldown-- > 0) {
            return cachedMatch;
        }
        cachedMatch = GeneratorRecipe.findBestMatch(level, worldPosition, GeneratorRecipeConfig.getRecipes(type))
                .orElse(null);
        rematchCooldown = REMATCH_INTERVAL;
        return cachedMatch;
    }

    @Nullable
    private Block resolveResult(ServerLevel level, GeneratorType type, GeneratorRecipe recipe) {
        if (!recipe.isRandomResult()) {
            return recipe.result();
        }
        List<Block> pool = poolFor(type);
        return pool.isEmpty() ? null : pool.get(level.getRandom().nextInt(pool.size()));
    }

    private List<Block> poolFor(GeneratorType type) {
        return switch (type.mode()) {
            case RANDOM_ORE -> GeneratorRecipeConfig.ores();
            case METAL_BLOCK -> GeneratorRecipeConfig.metalBlocks();
            case RECIPE -> List.of();
        };
    }

    private void setDisplayResult(@Nullable Block result) {
        String newId = result == null ? "" : Optional.ofNullable(BuiltInRegistries.BLOCK.getKey(result))
                .map(Identifier::toString).orElse("");
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

    private boolean canStartOutput(ServerLevel level, GeneratorType type, GeneratorRecipe recipe) {
        if (recipe.isFluidResult()) {
            return insertFluid(level, recipe.resultFluid(), recipe.amount(), true) > 0;
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
        return outputBlocks(level, recipe.result(), recipe.amount(), recipe.outputMode(), true) > 0;
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

    private int outputBlocks(
            ServerLevel level,
            @Nullable Block result,
            int amount,
            GeneratorOutputMode mode,
            boolean simulate
    ) {
        if (result == null || amount <= 0) {
            return 0;
        }
        return switch (mode) {
            case INSERT -> insertItems(level, result, amount, simulate);
            case DROP -> simulate || dropItems(level, result, amount) ? amount : 0;
            case PLACE -> simulate ? (canAcceptPlace(level) ? 1 : 0) : (placeBlock(level, result) ? 1 : 0);
        };
    }

    private int insertItems(ServerLevel level, Block result, int amount, boolean simulate) {
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

    private int insertFluid(ServerLevel level, @Nullable Fluid fluid, int amount, boolean simulate) {
        ResourceHandler<FluidResource> handler = getFluidOutputHandler(level);
        if (handler == null || fluid == null || fluid.isSame(Fluids.EMPTY) || amount <= 0) {
            return 0;
        }
        FluidResource resource = FluidResource.of(fluid);
        if (resource.isEmpty()) {
            return 0;
        }
        long millibuckets = Math.min((long) amount * FluidType.BUCKET_VOLUME, Integer.MAX_VALUE);
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
        return level.getBlockState(above).canBeReplaced()
                && level.getEntitiesOfClass(ItemEntity.class, new AABB(above).inflate(1.5)).size() < MAX_NEARBY_ITEMS;
    }

    private boolean placeBlock(ServerLevel level, Block result) {
        BlockPos above = worldPosition.above();
        return canAcceptPlace(level)
                && level.setBlock(above, result.defaultBlockState(), Block.UPDATE_ALL);
    }

    private boolean dropItems(ServerLevel level, Block result, int amount) {
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
        for (int remaining = amount; remaining > 0; ) {
            int batch = Math.min(remaining, maxStack);
            ItemEntity entity = new ItemEntity(level, x, y, z, new ItemStack(result.asItem(), batch));
            entity.setDefaultPickUpDelay();
            level.addFreshEntity(entity);
            remaining -= batch;
        }
        return true;
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
