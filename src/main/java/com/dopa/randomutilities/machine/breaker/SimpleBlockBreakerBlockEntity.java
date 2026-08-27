package com.dopa.randomutilities.machine.breaker;

import com.dopa.randomutilities.core.machine.ClaimActionGate;
import com.dopa.randomutilities.core.machine.MachineActors;
import com.dopa.randomutilities.core.machine.MachineOwnerProfiles;
import com.dopa.randomutilities.core.machine.OrientedEntityBlock;
import com.dopa.randomutilities.core.machine.OwnableMachine;
import com.dopa.randomutilities.core.machine.OwnerRequiredFeedback;
import com.dopa.randomutilities.registry.ModBlockEntities;
import com.dopa.randomutilities.core.util.ActionCooldownFeedback;
import com.dopa.randomutilities.core.util.BlockOrientations;
import com.dopa.randomutilities.core.util.RedstonePulse;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayer;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class SimpleBlockBreakerBlockEntity extends BlockEntity implements OwnableMachine {
    private static final int TRIGGERED_TICKS = 8;

    private final RedstonePulse pulse = new RedstonePulse();
    private final BreakerMining mining = new BreakerMining();
    private int triggeredTicks;
    private int actionCooldown;
    @Nullable
    private UUID ownerUuid;

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
            if (OwnerRequiredFeedback.blockRisingEdge(level, pos, this, actionCooldown)) {
                if (!hasOwner()) {
                    actionCooldown = OwnerRequiredFeedback.cooldownAfterNoOwnerBlock(actionCooldown);
                }
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
        FakePlayer breaker = MachineActors.actor(level, ownerUuid, pos, facing).orElse(null);
        if (breaker == null) {
            return false;
        }
        if (!ClaimActionGate.canBreak(level, breaker, target)) {
            mining.clear(level, destroyId());
            return false;
        }
        float hardness = targetState.getDestroySpeed(level, target);
        int hitsNeeded = BreakerMining.hitsNeeded(hardness, 0);
        if (!mining.completeAfterHit(level, destroyId(), target, hitsNeeded)) {
            BreakerMining.playHit(level, target, targetState);
            return true;
        }
        ItemStack tool = new ItemStack(Items.IRON_PICKAXE);
        breaker.setItemInHand(InteractionHand.MAIN_HAND, tool);
        List<ItemStack> drops = new ArrayList<>();
        AABB capture = new AABB(target).inflate(0.5);
        Set<UUID> existing = itemEntityIds(level, capture);
        boolean broken = breaker.gameMode.destroyBlock(target);
        if (broken) {
            BreakerDropCapture.collectFresh(level, capture, existing, drops);
            BreakerDrops.ejectFromBack(level, pos, facing.getOpposite(), drops, false);
        }
        breaker.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        return broken;
    }

    private int destroyId() {
        return Long.hashCode(worldPosition.asLong());
    }

    private static Set<UUID> itemEntityIds(ServerLevel level, AABB area) {
        Set<UUID> ids = new HashSet<>();
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, area)) {
            ids.add(entity.getUUID());
        }
        return ids;
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
                && state.getValue(OrientedEntityBlock.TRIGGERED) != triggered) {
            level.setBlock(pos, state.setValue(OrientedEntityBlock.TRIGGERED, triggered), Block.UPDATE_CLIENTS);
        }
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
        ownerUuid = MachineOwnerProfiles.load(input);
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
        if (ownerUuid != null) {
            MachineOwnerProfiles.save(output, ownerUuid);
        }
        mining.save(output);
    }
}
