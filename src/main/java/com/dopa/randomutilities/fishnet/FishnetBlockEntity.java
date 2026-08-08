package com.dopa.randomutilities.fishnet;

import com.dopa.randomutilities.fishnet.network.FishnetCatchPayload;
import com.dopa.randomutilities.machine.RedstoneControl;
import com.dopa.randomutilities.machine.RedstoneMode;
import com.dopa.randomutilities.machine.config.UpgradeConfig;
import com.dopa.randomutilities.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FishnetBlockEntity extends BlockEntity implements RedstoneControl {
    public static final int SLOT_COUNT = 9;
    public static final int BASE_CATCH_TICKS = 600;

    private final ItemStacksResourceHandler items = new ItemStacksResourceHandler(SLOT_COUNT) {
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
    private int productivityBonusBank;
    private boolean underwater;

    public FishnetBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FISHNET.get(), pos, state);
        upgrades.setOnChanged(this::setChanged);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FishnetBlockEntity be) {
        if (!level.isClientSide()) {
            be.tick((ServerLevel) level, pos);
        }
    }

    public ItemStacksResourceHandler items() {
        return items;
    }

    public FishnetUpgradeInventory upgrades() {
        return upgrades;
    }

    public ResourceHandler<ItemResource> itemHandler(@Nullable Direction side) {
        return items;
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

    public ItemStack stackInSlot(int slot) {
        ItemResource resource = items.getResource(slot);
        if (resource.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return resource.toStack(items.getAmountAsInt(slot));
    }

    public static boolean hasWaterSourcesOnAllSides(Level level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (!level.getFluidState(pos.relative(direction)).is(FluidTags.WATER)
                    || !level.getFluidState(pos.relative(direction)).isSource()) {
                return false;
            }
        }
        return true;
    }

    private void tick(ServerLevel level, BlockPos pos) {
        underwater = hasWaterSourcesOnAllSides(level, pos);
        int needed = UpgradeConfig.effectiveTicks(BASE_CATCH_TICKS, upgrades.overclockCount());
        if (catchTotal != needed) {
            catchTotal = needed;
            catchProgress = Math.min(catchProgress, catchTotal);
            setChanged();
        }

        if (!underwater || !redstoneMode.allowsOperation(level.getBestNeighborSignal(pos))) {
            if (catchProgress > 0) {
                catchProgress = 0;
                setChanged();
            }
            return;
        }

        catchProgress++;
        if (catchProgress < catchTotal) {
            setChanged();
            return;
        }
        catchProgress = 0;
        attemptCatch(level, pos);
        setChanged();
    }

    private void attemptCatch(ServerLevel level, BlockPos pos) {
        Vec3 origin = Vec3.atCenterOf(pos);
        Player player = level.getNearestPlayer(origin.x, origin.y, origin.z, 32.0, false);
        if (player == null) {
            player = FakePlayerFactory.getMinecraft(level);
        }

        ItemStack rod = new ItemStack(Items.FISHING_ROD);
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, origin)
                .withParameter(LootContextParams.TOOL, rod)
                .withParameter(LootContextParams.THIS_ENTITY, player)
                .withParameter(LootContextParams.ATTACKING_ENTITY, player)
                .withLuck(player.getLuck())
                .create(LootContextParamSets.FISHING);

        LootTable lootTable = level.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.FISHING);
        List<ItemStack> drops = lootTable.getRandomItems(params);
        if (drops.isEmpty()) {
            return;
        }

        ItemStack display = ItemStack.EMPTY;
        boolean anyInserted = false;
        for (ItemStack drop : drops) {
            if (drop.isEmpty()) {
                continue;
            }
            ItemStack toInsert = applyProductivity(drop);
            if (tryStore(toInsert)) {
                anyInserted = true;
                if (display.isEmpty()) {
                    display = toInsert.copy();
                }
            }
        }
        if (anyInserted) {
            PacketDistributor.sendToPlayersNear(
                    level,
                    null,
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5,
                    48.0,
                    new FishnetCatchPayload(pos, display.isEmpty() ? Items.COD.getDefaultInstance() : display)
            );
            if (player instanceof ServerPlayer serverPlayer && !(player instanceof FakePlayer)) {
                serverPlayer.awardStat(net.minecraft.stats.Stats.FISH_CAUGHT, 1);
            }
        }
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

    /** Stores as much as possible into catch slots. Returns true if any amount was stored. */
    private boolean tryStore(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ItemStack remaining = stack.copy();
        // Merge into existing stacks first.
        for (int i = 0; i < SLOT_COUNT && !remaining.isEmpty(); i++) {
            ItemStack existing = stackInSlot(i);
            if (existing.isEmpty() || !ItemStack.isSameItemSameComponents(existing, remaining)) {
                continue;
            }
            int max = Math.min(existing.getMaxStackSize(), items.getCapacityAsInt(i, ItemResource.of(remaining)));
            int room = max - existing.getCount();
            if (room <= 0) {
                continue;
            }
            int move = Math.min(room, remaining.getCount());
            items.set(i, ItemResource.of(existing), existing.getCount() + move);
            remaining.shrink(move);
        }
        for (int i = 0; i < SLOT_COUNT && !remaining.isEmpty(); i++) {
            if (!stackInSlot(i).isEmpty()) {
                continue;
            }
            int max = Math.min(remaining.getMaxStackSize(), items.getCapacityAsInt(i, ItemResource.of(remaining)));
            int move = Math.min(max, remaining.getCount());
            if (move <= 0) {
                continue;
            }
            ItemStack placed = remaining.copy();
            placed.setCount(move);
            items.set(i, ItemResource.of(placed), move);
            remaining.shrink(move);
        }
        return remaining.getCount() < stack.getCount();
    }

    public void dropContents(Level level, BlockPos pos) {
        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack stack = stackInSlot(i);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
            }
            items.set(i, ItemResource.EMPTY, 0);
        }
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
        catchProgress = input.getIntOr("CatchProgress", 0);
        catchTotal = input.getIntOr("CatchTotal", BASE_CATCH_TICKS);
        redstoneMode = RedstoneMode.byOrdinal(input.getIntOr("RedstoneMode", 0));
        productivityBonusBank = Math.max(0, input.getIntOr("ProductivityBonusBank", 0));
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
        for (int i = 0; i < upgrades.size(); i++) {
            ItemStack stack = upgrades.stackInSlot(i);
            if (!stack.isEmpty()) {
                output.store("Upgrade" + i, ItemStack.CODEC, stack);
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
    }
}
