package com.dopa.randomutilities.block.tinytnt;

import com.dopa.randomutilities.registry.ModSounds;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class TinyTntBlock extends Block {
    public static final MapCodec<TinyTntBlock> CODEC = simpleCodec(TinyTntBlock::new);
    public static final BooleanProperty UNSTABLE = BlockStateProperties.UNSTABLE;
    private static final VoxelShape SHAPE = Block.box(4.0, 0.0, 4.0, 12.0, 8.0, 12.0);

    public TinyTntBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(UNSTABLE, false));
    }

    @Override
    public MapCodec<TinyTntBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!oldState.is(state.getBlock()) && level.hasNeighborSignal(pos) && onCaughtFire(state, level, pos, null, null)) {
            level.removeBlock(pos, false);
        }
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block block,
            @Nullable Orientation orientation,
            boolean movedByPiston
    ) {
        if (level.hasNeighborSignal(pos) && onCaughtFire(state, level, pos, null, null)) {
            level.removeBlock(pos, false);
        }
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && !player.getAbilities().instabuild && state.getValue(UNSTABLE)) {
            onCaughtFire(state, level, pos, null, null);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void wasExploded(ServerLevel level, BlockPos pos, Explosion explosion) {
        if (level.getGameRules().get(GameRules.TNT_EXPLODES)) {
            PrimedTinyTnt primed = new PrimedTinyTnt(
                    level,
                    pos.getX() + 0.5,
                    pos.getY(),
                    pos.getZ() + 0.5,
                    explosion.getIndirectSourceEntity()
            );
            primed.setFuse(PrimedTinyTnt.getRandomShortFuse(primed.getFuse(), level.getRandom()));
            level.addFreshEntity(primed);
        }
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack itemStack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        if (!itemStack.is(Items.FLINT_AND_STEEL) && !itemStack.is(Items.FIRE_CHARGE)) {
            return super.useItemOn(itemStack, state, level, pos, player, hand, hitResult);
        }

        if (onCaughtFire(state, level, pos, hitResult.getDirection(), player)) {
            level.removeBlock(pos, false);
            Item item = itemStack.getItem();
            if (itemStack.is(Items.FLINT_AND_STEEL)) {
                itemStack.hurtAndBreak(1, player, hand.asEquipmentSlot());
            } else {
                itemStack.consume(1, player);
            }
            player.awardStat(Stats.ITEM_USED.get(item));
        } else if (level instanceof ServerLevel serverLevel && !serverLevel.getGameRules().get(GameRules.TNT_EXPLODES)) {
            player.sendOverlayMessage(Component.translatable("block.minecraft.tnt.disabled"));
            return InteractionResult.PASS;
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onProjectileHit(Level level, BlockState state, BlockHitResult blockHit, Projectile projectile) {
        if (level instanceof ServerLevel serverLevel) {
            BlockPos pos = blockHit.getBlockPos();
            Entity owner = projectile.getOwner();
            if (projectile.isOnFire()
                    && projectile.mayInteract(serverLevel, pos)
                    && onCaughtFire(state, level, pos, null, owner instanceof LivingEntity living ? living : null)) {
                level.removeBlock(pos, false);
            }
        }
    }

    @Override
    public boolean dropFromExplosion(Explosion explosion) {
        return false;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(UNSTABLE);
    }

    @Override
    public boolean onCaughtFire(
            BlockState state,
            Level level,
            BlockPos pos,
            @Nullable Direction face,
            @Nullable LivingEntity igniter
    ) {
        return prime(level, pos, igniter);
    }

    static boolean prime(Level level, BlockPos pos, @Nullable LivingEntity source) {
        if (level instanceof ServerLevel serverLevel && serverLevel.getGameRules().get(GameRules.TNT_EXPLODES)) {
            PrimedTinyTnt tnt = new PrimedTinyTnt(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, source);
            level.addFreshEntity(tnt);
            level.playSound(
                    null,
                    tnt.getX(),
                    tnt.getY(),
                    tnt.getZ(),
                    ModSounds.TINY_TNT_PRIMED.get(),
                    SoundSource.BLOCKS,
                    1.0F,
                    1.5F
            );
            level.gameEvent(source, GameEvent.PRIME_FUSE, pos);
            return true;
        }
        return false;
    }
}
