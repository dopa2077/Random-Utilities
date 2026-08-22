package com.dopa.randomutilities.fishnet;

import com.dopa.randomutilities.fishnet.config.FishnetConfig;
import com.dopa.randomutilities.fishnet.config.TreasureLootConfig;
import com.dopa.randomutilities.fishnet.network.FishnetApproachPayload;
import com.dopa.randomutilities.machine.MachineActors;
import com.dopa.randomutilities.machine.MachineOwnerProfiles;
import com.dopa.randomutilities.machine.OwnableMachine;
import com.dopa.randomutilities.machine.OwnerRequiredFeedback;
import com.dopa.randomutilities.machine.RedstoneControl;
import com.dopa.randomutilities.machine.RedstoneMode;
import com.dopa.randomutilities.machine.config.UpgradeConfig;
import com.dopa.randomutilities.registry.ModBlockEntities;
import com.dopa.randomutilities.registry.ModItemTags;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FishnetBlockEntity extends BlockEntity implements OwnableMachine, RedstoneControl {
    public static final int CATCH_SLOT_COUNT = 9;
    public static final int BASE_CATCH_TICKS = 600;
    private static final int SAVE_INTERVAL = 20;

    public static final int DATA_PROGRESS = 0;
    public static final int DATA_TOTAL = 1;
    public static final int DATA_REDSTONE = 2;
    public static final int DATA_UNDERWATER = 3;
    public static final int DATA_HAS_ROD = 4;
    public static final int DATA_PARTICLES = 5;
    public static final int DATA_SOUND = 6;
    public static final int DATA_COUNT = 7;

    /** Vanilla hooked approach lasts ~20–80 ticks; only then are fishing ripples shown. */
    private static final int HOOKED_PHASE_TICKS = 60;

    private final ItemStacksResourceHandler rod = new ItemStacksResourceHandler(1) {
        @Override
        protected void onContentsChanged(int index, ItemStack previousContents) {
            setChanged();
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            return isFishingRod(resource.toStack(1));
        }
    };

    private final ItemStacksResourceHandler items = new ItemStacksResourceHandler(CATCH_SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int index, ItemStack previousContents) {
            setChanged();
        }

        /** Blocks hoppers / pipes from inserting; internal catch uses {@link #set}. */
        @Override
        public boolean isValid(int index, ItemResource resource) {
            return false;
        }
    };

    private final FishnetUpgradeInventory upgrades =
            new FishnetUpgradeInventory(UpgradeConfig.UPGRADE_SLOT_COUNT);

    private RedstoneMode redstoneMode = RedstoneMode.IGNORE;
    private int catchProgress;
    private int catchTotal = BASE_CATCH_TICKS;
    private int saveTimer;
    private int productivityBonusBank;
    private boolean underwater;
    private float fishAngle;
    private boolean particlesEnabled = true;
    private boolean soundEnabled = true;
    /** True after the approach VFX packet was sent for the current catch cycle. */
    private boolean approachSent;
    /** Catch that could not fit; fishing stays paused until these can be stored. */
    private final List<ItemStack> pendingCatch = new ArrayList<>();
    private int ownerFeedbackCooldown;
    @Nullable
    private UUID ownerUuid;

    public FishnetBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FISHNET.get(), pos, state);
        upgrades.setOnChanged(this::setChanged);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FishnetBlockEntity be) {
        if (!level.isClientSide()) {
            be.tick((ServerLevel) level, pos);
        }
    }

    public static boolean isFishingRod(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ModItemTags.FISHING_RODS);
    }

    public ItemStacksResourceHandler rod() {
        return rod;
    }

    public ItemStacksResourceHandler items() {
        return items;
    }

    public FishnetUpgradeInventory upgrades() {
        return upgrades;
    }

    /**
     * Automation IO: top/sides expose the fishing-rod slot (insert rods);
     * bottom exposes catch slots (extract loot; inserts blocked via {@code isValid}).
     */
    public ResourceHandler<ItemResource> itemHandler(@Nullable Direction side) {
        if (side == Direction.DOWN) {
            return items;
        }
        return rod;
    }

    public ItemStack rodStack() {
        ItemResource resource = rod.getResource(0);
        if (resource.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return resource.toStack(rod.getAmountAsInt(0));
    }

    public void setRodStack(ItemStack stack) {
        if (stack.isEmpty()) {
            rod.set(0, ItemResource.EMPTY, 0);
        } else {
            rod.set(0, ItemResource.of(stack), stack.getCount());
        }
    }

    public int catchProgress() {
        return catchProgress;
    }

    public int catchTotal() {
        return Math.max(1, catchTotal);
    }

    public boolean isUnderwater() {
        return underwater;
    }

    public boolean hasRod() {
        return isFishingRod(rodStack());
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

    public boolean particlesEnabled() {
        return particlesEnabled;
    }

    public void setParticlesEnabled(boolean enabled) {
        if (this.particlesEnabled != enabled) {
            this.particlesEnabled = enabled;
            setChanged();
        }
    }

    public boolean soundEnabled() {
        return soundEnabled;
    }

    public void setSoundEnabled(boolean enabled) {
        if (this.soundEnabled != enabled) {
            this.soundEnabled = enabled;
            setChanged();
        }
    }

    public int insertUpgrade(ItemStack stack) {
        return upgrades.insertFrom(stack);
    }

    public ItemStack stackInSlot(int slot) {
        ItemResource resource = items.getResource(slot);
        if (resource.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return resource.toStack(items.getAmountAsInt(slot));
    }

    public static boolean canCatchHere(Level level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof FishnetBlock) || !state.getValue(FishnetBlock.WATERLOGGED)) {
            return false;
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            FluidState fluid = level.getFluidState(pos.relative(direction));
            if (!fluid.is(FluidTags.WATER) || !fluid.isSource()) {
                return false;
            }
        }
        return true;
    }

    private void tick(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        underwater = canCatchHere(level, pos, state);
        ownerFeedbackCooldown = OwnerRequiredFeedback.tickOwnerFeedbackCooldown(ownerFeedbackCooldown);

        ItemStack rodStack = rodStack();
        boolean powered = redstoneMode.allowsOperation(level.getBestNeighborSignal(pos));
        boolean wouldRun = underwater && isFishingRod(rodStack) && powered;
        if (OwnerRequiredFeedback.blockWhilePowered(level, pos, this, wouldRun, ownerFeedbackCooldown)) {
            if (!hasOwner() && ownerFeedbackCooldown <= 0) {
                ownerFeedbackCooldown = OwnerRequiredFeedback.cooldownAfterPoweredBlock();
            }
            stopApproachVfx(level, pos);
            return;
        }

        FakePlayer fisher = MachineActors.actor(level, ownerUuid, pos, Direction.NORTH).orElse(null);
        if (fisher == null) {
            return;
        }

        int lureBonus = 0;
        if (isFishingRod(rodStack)) {
            lureBonus = (int) (EnchantmentHelper.getFishingTimeReduction(level, rodStack, fisher) * 20.0F);
        }
        int lureWait = Math.max(1, BASE_CATCH_TICKS - lureBonus);
        int needed = Math.max(1, UpgradeConfig.effectiveTicks(lureWait, upgrades.overclockCount()));
        if (catchTotal != needed) {
            catchTotal = needed;
            catchProgress = Math.min(catchProgress, catchTotal);
            setChanged();
        }

        // Wait for room for the held catch — do not start another fishing cycle.
        if (!pendingCatch.isEmpty()) {
            if (catchProgress > 0) {
                catchProgress = 0;
                saveTimer = 0;
                setChanged();
            }
            stopApproachVfx(level, pos);
            if (underwater && isFishingRod(rodStack) && powered) {
                tryFlushPendingCatch(level, pos, fisher, rodStack);
            }
            return;
        }

        boolean canRun = underwater && isFishingRod(rodStack) && powered;

        if (!canRun) {
            stopApproachVfx(level, pos);
            return;
        }

        catchProgress++;
        int remaining = catchTotal - catchProgress;
        int hooked = hookedPhaseTicks();
        if (remaining > 0 && remaining <= hooked) {
            if (!approachSent && particlesEnabled) {
                sendApproachVfx(level, pos, remaining);
                approachSent = true;
            }
            spawnHookedApproachParticles(level, pos, remaining);
        } else if (remaining > hooked) {
            spawnLureTeaseSplash(level, pos);
        }
        if (catchProgress < catchTotal) {
            markProgressDirty();
            return;
        }

        catchProgress = 0;
        saveTimer = 0;
        approachSent = false;
        completeCatch(level, pos, fisher, rodStack);
        setChanged();
    }

    private void markProgressDirty() {
        if (++saveTimer >= SAVE_INTERVAL) {
            saveTimer = 0;
            setChanged();
        }
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

    private int hookedPhaseTicks() {
        return Math.min(HOOKED_PHASE_TICKS, Math.max(1, catchTotal));
    }

    private void sendApproachVfx(ServerLevel level, BlockPos pos, int durationTicks) {
        PacketDistributor.sendToPlayersNear(
                level,
                null,
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                48.0,
                new FishnetApproachPayload(pos, durationTicks)
        );
    }

    private void stopApproachVfx(ServerLevel level, BlockPos pos) {
        if (!approachSent) {
            return;
        }
        approachSent = false;
        sendApproachVfx(level, pos, 0);
    }

    /** Occasional distant splash while waiting — same idea as vanilla lure teases. */
    private void spawnLureTeaseSplash(ServerLevel level, BlockPos pos) {
        if (!particlesEnabled) {
            return;
        }
        float teaseChance = 0.015F;
        int remaining = catchTotal - catchProgress;
        if (remaining < catchTotal / 4) {
            teaseChance += 0.02F;
        }
        if (level.getRandom().nextFloat() >= teaseChance) {
            return;
        }
        float angle = Mth.nextFloat(level.getRandom(), 0.0F, 360.0F) * ((float) Math.PI / 180.0F);
        float dist = Mth.nextFloat(level.getRandom(), 2.5F, 6.0F);
        double fishX = pos.getX() + 0.5 + Mth.sin(angle) * dist;
        double fishY = pos.getY() + 1.0;
        double fishZ = pos.getZ() + 0.5 + Mth.cos(angle) * dist;
        BlockPos splashPos = BlockPos.containing(fishX, fishY - 1.0, fishZ);
        if (level.getBlockState(splashPos).is(Blocks.WATER) || level.getFluidState(splashPos).is(FluidTags.WATER)) {
            level.sendParticles(ParticleTypes.SPLASH, fishX, fishY, fishZ, 2 + level.getRandom().nextInt(2), 0.1F, 0.0, 0.1F, 0.0);
        }
    }

    /**
     * Same particle math as {@code FishingHook} while {@code timeUntilHooked} counts down:
     * ripples start far out and close in during the final catch window.
     */
    private void spawnHookedApproachParticles(ServerLevel level, BlockPos pos, int timeUntilHooked) {
        if (!particlesEnabled) {
            return;
        }
        fishAngle += (float) level.getRandom().triangle(0.0, 9.188);
        float angle = fishAngle * ((float) Math.PI / 180.0F);
        float angleSin = Mth.sin(angle);
        float angleCos = Mth.cos(angle);
        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + 1.0;
        double cz = pos.getZ() + 0.5;
        double fishX = cx + angleSin * timeUntilHooked * 0.1F;
        double fishZ = cz + angleCos * timeUntilHooked * 0.1F;
        BlockPos splashPos = BlockPos.containing(fishX, cy - 1.0, fishZ);
        if (!level.getBlockState(splashPos).is(Blocks.WATER)
                && !level.getFluidState(splashPos).is(FluidTags.WATER)) {
            return;
        }

        if (level.getRandom().nextFloat() < 0.15F) {
            level.sendParticles(ParticleTypes.BUBBLE, fishX, cy - 0.1F, fishZ, 1, angleSin, 0.1, angleCos, 0.0);
        }
        float particleXMovement = angleSin * 0.04F;
        float particleZMovement = angleCos * 0.04F;
        level.sendParticles(
                ParticleTypes.FISHING, fishX, cy, fishZ, 0, particleZMovement, 0.01, -particleXMovement, 1.0);
        level.sendParticles(
                ParticleTypes.FISHING, fishX, cy, fishZ, 0, -particleZMovement, 0.01, particleXMovement, 1.0);
    }

    private void completeCatch(ServerLevel level, BlockPos pos, FakePlayer fisher, ItemStack rodStack) {
        int luck = EnchantmentHelper.getFishingLuckBonus(level, rodStack, fisher);
        Vec3 origin = Vec3.atCenterOf(pos);
        boolean openWater = FishnetOpenWater.calculateOpenWater(level, pos);
        boolean treasureAllowed = openWater
                && !FishnetConfig.preventRareLoot()
                && upgrades.fortuneMeshCount() > 0;
        // Treasure pool requires THIS_ENTITY to be a FishingHook with in_open_water
        // matching vanilla FishingHook#calculateOpenWater (not a small 3×3 pond).
        FishingHook bobber = new FishingHook(fisher, level, luck, 0);
        bobber.setPos(origin.x, origin.y, origin.z);
        FishnetOpenWater.applyToBobber(bobber, treasureAllowed);
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, origin)
                .withParameter(LootContextParams.TOOL, rodStack)
                .withParameter(LootContextParams.THIS_ENTITY, bobber)
                .withParameter(LootContextParams.ATTACKING_ENTITY, fisher)
                .withLuck(luck + fisher.getLuck())
                .create(LootContextParamSets.FISHING);

        List<ItemStack> drops = rollCatchLoot(level, params, treasureAllowed);
        bobber.discard();

        List<ItemStack> toStore = new ArrayList<>();
        for (ItemStack drop : drops) {
            if (drop.isEmpty()) {
                continue;
            }
            toStore.add(applyProductivity(drop));
        }
        if (toStore.isEmpty()) {
            return;
        }

        if (!canFullyStoreAll(toStore)) {
            pendingCatch.clear();
            for (ItemStack stack : toStore) {
                pendingCatch.add(stack.copy());
            }
            setChanged();
            return;
        }

        commitCatch(level, pos, fisher, rodStack, toStore);
    }

    /**
     * Treasure Mesh replaces the catch with {@code treasure_loot.json}.
     * Otherwise Fortune Mesh may force vanilla fishing treasure in open water.
     */
    private List<ItemStack> rollCatchLoot(ServerLevel level, LootParams params, boolean treasureAllowed) {
        if (upgrades.treasureMeshCount() > 0 && FishnetUpgradeInventory.maxTreasureMesh() > 0) {
            ItemStack custom = TreasureLootConfig.roll(level.getRandom());
            return custom.isEmpty() ? List.of() : List.of(custom);
        }
        return rollFishingLoot(level, params, treasureAllowed);
    }

    /**
     * Fortune Mesh unlocks vanilla treasure in open water and rolls a capped chance to force it.
     * Never guaranteed ({@link UpgradeConfig#FORTUNE_MESH_MAX_CHANCE_PERCENT}).
     */
    private List<ItemStack> rollFishingLoot(ServerLevel level, LootParams params, boolean treasureAllowed) {
        int chancePercent = UpgradeConfig.fortuneMeshChancePercent(upgrades.fortuneMeshCount());
        float forceChance = chancePercent / 100.0F;
        boolean forceTreasure = treasureAllowed
                && chancePercent > 0
                && level.getRandom().nextFloat() < forceChance;
        var tableKey = forceTreasure ? BuiltInLootTables.FISHING_TREASURE : BuiltInLootTables.FISHING;
        LootTable lootTable = level.getServer().reloadableRegistries().getLootTable(tableKey);
        return lootTable.getRandomItems(params);
    }

    private void tryFlushPendingCatch(ServerLevel level, BlockPos pos, FakePlayer fisher, ItemStack rodStack) {
        if (pendingCatch.isEmpty() || !canFullyStoreAll(pendingCatch)) {
            return;
        }
        List<ItemStack> toStore = new ArrayList<>(pendingCatch.size());
        for (ItemStack stack : pendingCatch) {
            toStore.add(stack.copy());
        }
        pendingCatch.clear();
        commitCatch(level, pos, fisher, rodStack, toStore);
        setChanged();
    }

    private void commitCatch(
            ServerLevel level,
            BlockPos pos,
            FakePlayer fisher,
            ItemStack rodStack,
            List<ItemStack> toStore
    ) {
        double y = pos.getY() + 0.5;
        if (soundEnabled) {
            level.playSound(
                    null,
                    pos,
                    SoundEvents.FISHING_BOBBER_SPLASH,
                    SoundSource.BLOCKS,
                    0.35F,
                    1.0F + (level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.4F
            );
        }
        if (particlesEnabled) {
            level.sendParticles(
                    ParticleTypes.BUBBLE,
                    pos.getX() + 0.5,
                    y,
                    pos.getZ() + 0.5,
                    12,
                    0.35,
                    0.0,
                    0.35,
                    0.2
            );
            level.sendParticles(
                    ParticleTypes.FISHING,
                    pos.getX() + 0.5,
                    y,
                    pos.getZ() + 0.5,
                    12,
                    0.35,
                    0.0,
                    0.35,
                    0.2
            );
        }

        for (ItemStack stack : toStore) {
            storeFully(stack);
        }

        ItemStack damagedRod = rodStack.copy();
        damagedRod.hurtAndBreak(1, level, fisher, broken -> {});
        setRodStack(damagedRod);
    }

    private ItemStack applyProductivity(ItemStack drop) {
        ItemStack result = drop.copy();
        int base = result.getCount();
        int boosted = UpgradeConfig.peekBoostedAmount(base, upgrades.productivityCount(), productivityBonusBank);
        productivityBonusBank = UpgradeConfig.advanceProductivityBank(
                base, upgrades.productivityCount(), productivityBonusBank);
        if (boosted > result.getMaxStackSize()) {
            boosted = result.getMaxStackSize();
        }
        result.setCount(Math.max(1, boosted));
        return result;
    }

    private boolean canFullyStoreAll(List<ItemStack> stacks) {
        ItemStack[] simulated = new ItemStack[CATCH_SLOT_COUNT];
        for (int i = 0; i < CATCH_SLOT_COUNT; i++) {
            simulated[i] = stackInSlot(i).copy();
        }
        for (ItemStack stack : stacks) {
            if (!simulateStore(simulated, stack)) {
                return false;
            }
        }
        return true;
    }

    private static boolean simulateStore(ItemStack[] slots, ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        int remaining = stack.getCount();
        for (int i = 0; i < slots.length && remaining > 0; i++) {
            ItemStack existing = slots[i];
            if (existing.isEmpty() || !ItemStack.isSameItemSameComponents(existing, stack)) {
                continue;
            }
            int room = existing.getMaxStackSize() - existing.getCount();
            if (room <= 0) {
                continue;
            }
            int move = Math.min(room, remaining);
            existing.grow(move);
            remaining -= move;
        }
        for (int i = 0; i < slots.length && remaining > 0; i++) {
            if (!slots[i].isEmpty()) {
                continue;
            }
            int move = Math.min(stack.getMaxStackSize(), remaining);
            ItemStack placed = stack.copy();
            placed.setCount(move);
            slots[i] = placed;
            remaining -= move;
        }
        return remaining <= 0;
    }

    /** Caller must only invoke after {@link #canFullyStoreAll}. */
    private void storeFully(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ItemStack remaining = stack.copy();
        for (int i = 0; i < CATCH_SLOT_COUNT && !remaining.isEmpty(); i++) {
            ItemStack existing = stackInSlot(i);
            if (existing.isEmpty() || !ItemStack.isSameItemSameComponents(existing, remaining)) {
                continue;
            }
            int room = existing.getMaxStackSize() - existing.getCount();
            if (room <= 0) {
                continue;
            }
            int move = Math.min(room, remaining.getCount());
            items.set(i, ItemResource.of(existing), existing.getCount() + move);
            remaining.shrink(move);
        }
        for (int i = 0; i < CATCH_SLOT_COUNT && !remaining.isEmpty(); i++) {
            if (!stackInSlot(i).isEmpty()) {
                continue;
            }
            int move = Math.min(remaining.getMaxStackSize(), remaining.getCount());
            ItemStack placed = remaining.copy();
            placed.setCount(move);
            items.set(i, ItemResource.of(placed), move);
            remaining.shrink(move);
        }
    }

    public void dropContents(Level level, BlockPos pos) {
        ItemStack rodStack = rodStack();
        if (!rodStack.isEmpty()) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), rodStack);
            setRodStack(ItemStack.EMPTY);
        }
        for (int i = 0; i < CATCH_SLOT_COUNT; i++) {
            ItemStack stack = stackInSlot(i);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
            }
            items.set(i, ItemResource.EMPTY, 0);
        }
        for (ItemStack stack : pendingCatch) {
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
            }
        }
        pendingCatch.clear();
        for (int i = 0; i < upgrades.size(); i++) {
            ItemStack stack = upgrades.stackInSlot(i);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
            }
        }
        upgrades.clearContents();
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
        setRodStack(input.read("Rod", ItemStack.CODEC).orElse(ItemStack.EMPTY));
        for (int i = 0; i < CATCH_SLOT_COUNT; i++) {
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
        pendingCatch.clear();
        int pendingCount = Math.max(0, input.getIntOr("PendingCount", 0));
        for (int i = 0; i < pendingCount; i++) {
            ItemStack stack = input.read("Pending" + i, ItemStack.CODEC).orElse(ItemStack.EMPTY);
            if (!stack.isEmpty()) {
                pendingCatch.add(stack);
            }
        }
        catchProgress = input.getIntOr("CatchProgress", 0);
        catchTotal = input.getIntOr("CatchTotal", BASE_CATCH_TICKS);
        redstoneMode = RedstoneMode.byOrdinal(input.getIntOr("RedstoneMode", 0));
        productivityBonusBank = Math.max(0, input.getIntOr("ProductivityBonusBank", 0));
        particlesEnabled = input.getBooleanOr("ParticlesEnabled", true);
        soundEnabled = input.getBooleanOr("SoundEnabled", true);
        ownerUuid = MachineOwnerProfiles.load(input);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ItemStack rodStack = rodStack();
        if (!rodStack.isEmpty()) {
            output.store("Rod", ItemStack.CODEC, rodStack);
        }
        for (int i = 0; i < CATCH_SLOT_COUNT; i++) {
            ItemStack stack = stackInSlot(i);
            if (!stack.isEmpty()) {
                output.store("Item" + i, ItemStack.CODEC, stack);
            }
        }
        for (int i = 0; i < upgrades.size(); i++) {
            ItemStack stack = upgrades.stackInSlot(i);
            if (!stack.isEmpty()) {
                output.store("Upgrade" + i, ItemStack.CODEC, stack);
            }
        }
        if (!pendingCatch.isEmpty()) {
            output.putInt("PendingCount", pendingCatch.size());
            for (int i = 0; i < pendingCatch.size(); i++) {
                ItemStack stack = pendingCatch.get(i);
                if (!stack.isEmpty()) {
                    output.store("Pending" + i, ItemStack.CODEC, stack);
                }
            }
        }
        if (catchProgress > 0) {
            output.putInt("CatchProgress", catchProgress);
        }
        output.putInt("CatchTotal", catchTotal);
        output.putInt("RedstoneMode", redstoneMode.ordinal());
        if (productivityBonusBank > 0) {
            output.putInt("ProductivityBonusBank", productivityBonusBank);
        }
        if (!particlesEnabled) {
            output.putBoolean("ParticlesEnabled", false);
        }
        if (!soundEnabled) {
            output.putBoolean("SoundEnabled", false);
        }
        if (ownerUuid != null) {
            MachineOwnerProfiles.save(output, ownerUuid);
        }
    }
}
