package com.dopa.randomutilities.blockbreaker;

import com.dopa.randomutilities.dOPasRandomUtilities;
import com.dopa.randomutilities.registry.ModBlockEntities;
import com.dopa.randomutilities.util.ActionCooldownFeedback;
import com.dopa.randomutilities.util.BlockOrientations;
import com.dopa.randomutilities.util.RedstonePulse;
import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.transfer.item.ContainerOrHandler;
import net.neoforged.neoforge.transfer.item.ItemUtil;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class SimpleBlockBreakerBlockEntity extends BlockEntity {
    private static final int TRIGGERED_TICKS = 8;
    private static final int DISPENSE_ACCURACY = 6;
    private static final double DISPENSE_OFFSET = 0.7;

    private static final GameProfile BREAKER_PROFILE = new GameProfile(
            UUID.nameUUIDFromBytes((dOPasRandomUtilities.MOD_ID + ":simple_block_breaker").getBytes(StandardCharsets.UTF_8)),
            "[Block Breaker]"
    );

    private final RedstonePulse pulse = new RedstonePulse();
    private final BreakerMining mining = new BreakerMining();
    private int triggeredTicks;
    private int actionCooldown;

    public SimpleBlockBreakerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SIMPLE_BLOCK_BREAKER.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SimpleBlockBreakerBlockEntity be) {
        if (!level.isClientSide()) {
            be.tick((ServerLevel) level, pos, state);
        }
    }

    private void tick(ServerLevel level, BlockPos pos, BlockState state) {
        if (actionCooldown > 0) {
            actionCooldown--;
        }
        boolean powered = level.getBestNeighborSignal(pos) > 0;
        if (pulse.risingEdge(powered)) {
            if (actionCooldown > 0) {
                ActionCooldownFeedback.smoke(level, pos);
            } else if (tryBreak(level, pos, state)) {
                actionCooldown = ActionCooldownFeedback.DEFAULT_COOLDOWN_TICKS;
                setTriggered(level, pos, true);
                triggeredTicks = TRIGGERED_TICKS;
                setChanged();
            }
        }
        if (triggeredTicks > 0) {
            triggeredTicks--;
            if (triggeredTicks == 0) {
                setTriggered(level, pos, false);
                setChanged();
            }
        }
    }

    private boolean tryBreak(ServerLevel level, BlockPos pos, BlockState state) {
        Direction facing = BlockOrientations.front(state);
        BlockPos target = pos.relative(facing);
        BlockState targetState = level.getBlockState(target);
        if (!canHarvest(level, target, targetState)) {
            mining.clear(level, destroyId());
            return false;
        }
        float hardness = targetState.getDestroySpeed(level, target);
        int hitsNeeded = BreakerMining.hitsNeeded(hardness, 0);
        if (!mining.completeAfterHit(level, destroyId(), target, hitsNeeded)) {
            BreakerMining.playHit(level, target, targetState);
            return true;
        }
        FakePlayer breaker = breaker(level);
        positionPlayer(breaker, pos, facing);
        ItemStack tool = new ItemStack(Items.IRON_PICKAXE);
        breaker.setItemInHand(InteractionHand.MAIN_HAND, tool);
        BlockEntity targetBe = level.getBlockEntity(target);
        List<ItemStack> drops = new ArrayList<>(Block.getDrops(targetState, level, target, targetBe, breaker, tool));
        AABB capture = new AABB(target).inflate(0.5);
        Set<UUID> existing = itemEntityIds(level, capture);
        boolean broken = level.destroyBlock(target, false);
        if (broken) {
            targetState.spawnAfterBreak(level, target, tool, true);
            collectNewItemEntities(level, capture, existing, drops);
            ejectFromBack(level, pos, facing.getOpposite(), drops);
        }
        breaker.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        return broken;
    }

    private int destroyId() {
        return Long.hashCode(worldPosition.asLong());
    }

    private static void ejectFromBack(ServerLevel level, BlockPos pos, Direction output, List<ItemStack> drops) {
        ContainerOrHandler into = HopperBlockEntity.getContainerOrHandlerAt(
                level,
                pos.relative(output),
                output.getOpposite()
        );
        Vec3 dispensePos = Vec3.atCenterOf(pos).add(
                output.getStepX() * DISPENSE_OFFSET,
                output.getStepY() * DISPENSE_OFFSET,
                output.getStepZ() * DISPENSE_OFFSET
        );
        boolean shot = false;
        for (ItemStack drop : drops) {
            if (drop.isEmpty()) {
                continue;
            }
            ItemStack remaining = insertLikeDropper(into, output, drop.copy());
            if (!remaining.isEmpty()) {
                DefaultDispenseItemBehavior.spawnItem(level, remaining, DISPENSE_ACCURACY, output, dispensePos);
                shot = true;
            }
        }
        if (shot) {
            level.levelEvent(LevelEvent.SOUND_DISPENSER_DISPENSE, pos, 0);
            level.levelEvent(LevelEvent.PARTICLES_SHOOT_SMOKE, pos, output.get3DDataValue());
        }
    }

    private static ItemStack insertLikeDropper(ContainerOrHandler into, Direction output, ItemStack stack) {
        if (into.isEmpty()) {
            return stack;
        }
        if (into.container() != null) {
            return HopperBlockEntity.addItem(null, into.container(), stack, output.getOpposite());
        }
        return ItemUtil.insertItemReturnRemaining(into.itemHandler(), stack, false, null);
    }

    private static Set<UUID> itemEntityIds(ServerLevel level, AABB area) {
        Set<UUID> ids = new HashSet<>();
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, area)) {
            ids.add(entity.getUUID());
        }
        return ids;
    }

    private static void collectNewItemEntities(
            ServerLevel level,
            AABB area,
            Set<UUID> existing,
            List<ItemStack> drops
    ) {
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, area)) {
            if (existing.contains(entity.getUUID())) {
                continue;
            }
            ItemStack stack = entity.getItem();
            if (!stack.isEmpty()) {
                drops.add(stack.copy());
            }
            entity.discard();
        }
    }

    private static boolean canHarvest(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.isAir() || state.getBlock() instanceof LiquidBlock) {
            return false;
        }
        if (state.getDestroySpeed(level, pos) < 0.0F) {
            return false;
        }
        return !state.is(BlockTags.INCORRECT_FOR_IRON_TOOL);
    }

    private static void setTriggered(ServerLevel level, BlockPos pos, boolean triggered) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof SimpleBlockBreakerBlock
                && state.getValue(SimpleBlockBreakerBlock.TRIGGERED) != triggered) {
            level.setBlock(pos, state.setValue(SimpleBlockBreakerBlock.TRIGGERED, triggered), Block.UPDATE_CLIENTS);
        }
    }

    private FakePlayer breaker(ServerLevel level) {
        return FakePlayerFactory.get(level, BREAKER_PROFILE);
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

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel server) {
            mining.clear(server, destroyId());
        }
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        pulse.setWasPowered(input.getBooleanOr("WasPowered", false));
        triggeredTicks = Math.max(0, input.getIntOr("TriggeredTicks", 0));
        actionCooldown = Math.max(0, input.getIntOr("ActionCooldown", 0));
        mining.load(input);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (pulse.wasPowered()) {
            output.putBoolean("WasPowered", true);
        }
        if (triggeredTicks > 0) {
            output.putInt("TriggeredTicks", triggeredTicks);
        }
        if (actionCooldown > 0) {
            output.putInt("ActionCooldown", actionCooldown);
        }
        mining.save(output);
    }
}
