package com.dopa.randomutilities.blockplacer;

import com.dopa.randomutilities.dOPasRandomUtilities;
import com.dopa.randomutilities.registry.ModBlockEntities;
import com.dopa.randomutilities.util.ActionCooldownFeedback;
import com.dopa.randomutilities.util.BlockOrientations;
import com.dopa.randomutilities.util.RedstonePulse;
import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
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

public class SimpleBlockPlacerBlockEntity extends BlockEntity {
    public static final int SLOT_COUNT = 9;

    private static final GameProfile PLACER_PROFILE = new GameProfile(
            UUID.nameUUIDFromBytes((dOPasRandomUtilities.MOD_ID + ":simple_block_placer").getBytes(StandardCharsets.UTF_8)),
            "[Block Placer]"
    );

    private final ItemStacksResourceHandler items = new ItemStacksResourceHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int index, ItemStack previousContents) {
            setChanged();
        }
    };

    private final RedstonePulse pulse = new RedstonePulse();
    private int actionCooldown;

    public SimpleBlockPlacerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SIMPLE_BLOCK_PLACER.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SimpleBlockPlacerBlockEntity be) {
        if (!level.isClientSide()) {
            be.tick((ServerLevel) level, pos, state);
        }
    }

    public ItemStacksResourceHandler itemHandler() {
        return items;
    }

    public Component getDisplayName() {
        return Component.translatable("container.dopasrandomutilities.simple_block_placer");
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
        if (actionCooldown > 0) {
            actionCooldown--;
        }
        boolean powered = level.getBestNeighborSignal(pos) > 0;
        if (!pulse.risingEdge(powered)) {
            return;
        }
        if (actionCooldown > 0) {
            ActionCooldownFeedback.smoke(level, pos);
            return;
        }
        if (tryPlace(level, pos, state)) {
            actionCooldown = ActionCooldownFeedback.DEFAULT_COOLDOWN_TICKS;
        }
    }

    private boolean tryPlace(ServerLevel level, BlockPos pos, BlockState state) {
        Direction facing = BlockOrientations.front(state);
        BlockPos front = pos.relative(facing);
        if (!level.getBlockState(front).canBeReplaced()) {
            return false;
        }
        int slot = pickRandomBlockSlot(level.getRandom());
        if (slot < 0) {
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
        Vec3 hitLoc = Vec3.atCenterOf(front).subtract(
                facing.getStepX() * 0.5,
                facing.getStepY() * 0.5,
                facing.getStepZ() * 0.5
        );
        BlockHitResult hit = new BlockHitResult(hitLoc, facing.getOpposite(), front, false);
        BlockPlaceContext context = new BlockPlaceContext(placer, InteractionHand.MAIN_HAND, held, hit);
        InteractionResult result = blockItem.place(context);
        ItemStack remaining = placer.getItemInHand(InteractionHand.MAIN_HAND);
        if (remaining.isEmpty()) {
            items.set(slot, ItemResource.EMPTY, 0);
        } else {
            items.set(slot, ItemResource.of(remaining), remaining.getCount());
        }
        placer.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        if (result.consumesAction()) {
            setChanged();
            return true;
        }
        return false;
    }

    /** Reservoir sample among slots that hold a placeable block. */
    private int pickRandomBlockSlot(RandomSource random) {
        int chosen = -1;
        int seen = 0;
        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack stack = stackInSlot(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) {
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
        pulse.setWasPowered(input.getBooleanOr("WasPowered", false));
        actionCooldown = Math.max(0, input.getIntOr("ActionCooldown", 0));
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
        if (pulse.wasPowered()) {
            output.putBoolean("WasPowered", true);
        }
        if (actionCooldown > 0) {
            output.putInt("ActionCooldown", actionCooldown);
        }
    }
}
