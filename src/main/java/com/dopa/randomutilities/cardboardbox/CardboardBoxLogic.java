package com.dopa.randomutilities.cardboardbox;

import com.dopa.randomutilities.registry.ModBlocks;

import com.dopa.randomutilities.machine.ClaimActionGate;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.Set;

public final class CardboardBoxLogic {
    private static final int POOF_COUNT = 14;
    private static final double POOF_SPREAD = 0.28;
    private static final double POOF_HEIGHT = 0.4;

    private static final Component CANNOT_WRAP = Component.translatable(
            "item.dopasrandomutilities.cardboard_box.failure.cannot_wrap"
    ).withStyle(ChatFormatting.RED);

    private static final Set<Block> BLACKLIST = Set.of(
            Blocks.AIR,
            Blocks.BEDROCK,
            Blocks.BARRIER,
            Blocks.END_PORTAL,
            Blocks.END_PORTAL_FRAME,
            Blocks.NETHER_PORTAL,
            Blocks.COMMAND_BLOCK,
            Blocks.CHAIN_COMMAND_BLOCK,
            Blocks.REPEATING_COMMAND_BLOCK,
            Blocks.STRUCTURE_BLOCK,
            Blocks.STRUCTURE_VOID,
            Blocks.JIGSAW,
            Blocks.MOVING_PISTON,
            Blocks.REINFORCED_DEEPSLATE
    );

    private CardboardBoxLogic() {}

    private static boolean canWrap(Level level, BlockPos pos, @Nullable Player player, BlockState state) {
        if (state.isAir()) {
            return false;
        }
        if (state.getBlock() == ModBlocks.CARDBOARD_BOX.get()) {
            return false;
        }
        if (BLACKLIST.contains(state.getBlock())) {
            return false;
        }
        if (state.getBlock() instanceof DoorBlock || state.getBlock() instanceof BedBlock) {
            return false;
        }
        if (state.getBlock() instanceof DoublePlantBlock) {
            return false;
        }
        if (player != null && !level.mayInteract(player, pos)) {
            return false;
        }
        if (player != null && level instanceof ServerLevel serverLevel && !ClaimActionGate.canBreak(serverLevel, player, pos)) {
            return false;
        }
        return true;
    }

    public static Optional<Component> tryWrapFromItem(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return Optional.empty();
        }
        ServerPlayer player = context.getPlayer() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!canWrap(level, pos, player, state)) {
            return Optional.of(CANNOT_WRAP);
        }
        Direction boxFacing = context.getHorizontalDirection().getOpposite();
        CardboardBoxContents contents = capture(serverLevel, pos, state, boxFacing);
        if (!placeBox(serverLevel, pos, contents, boxFacing)) {
            restoreCapturedBlock(serverLevel, pos, contents);
            return Optional.of(CANNOT_WRAP);
        }
        ItemStack stack = context.getItemInHand();
        stack.shrink(1);
        return Optional.empty();
    }

    public static boolean tryUnwrap(ServerLevel level, BlockPos pos, @Nullable Player player) {
        if (!(level.getBlockEntity(pos) instanceof CardboardBoxBlockEntity box) || !box.hasContents()) {
            return false;
        }
        CardboardBoxContents contents = box.contents();
        if (contents == null || !contents.hasData()) {
            return false;
        }
        BlockState boxState = level.getBlockState(pos);
        Direction boxFacing = boxState.getValue(CardboardBoxBlock.FACING);
        BlockState restored = contents.blockStateForUnwrap(level.registryAccess(), boxFacing);
        if (restored == null) {
            return false;
        }
        if (player != null && !level.mayInteract(player, pos)) {
            return false;
        }
        if (!restored.canSurvive(level, pos)) {
            return false;
        }
        level.setBlock(pos, restored, Block.UPDATE_ALL);
        restoreBlockEntityTag(level, pos, contents);
        playUnwrap(level, pos);
        return true;
    }

    private static CardboardBoxContents capture(
            ServerLevel level,
            BlockPos pos,
            BlockState state,
            Direction captureFacing
    ) {
        HolderLookup.Provider registries = level.registryAccess();
        BlockEntity source = level.getBlockEntity(pos);
        net.minecraft.nbt.@Nullable CompoundTag blockEntityTag = null;
        if (source != null) {
            TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
            source.saveWithFullMetadata(output);
            blockEntityTag = output.buildResult();
        }
        return CardboardBoxContents.fromBlockState(registries, state, blockEntityTag, captureFacing);
    }

    private static boolean placeBox(
            ServerLevel level,
            BlockPos pos,
            CardboardBoxContents contents,
            Direction facing
    ) {
        BlockEntity existing = level.getBlockEntity(pos);
        if (existing instanceof Container container) {
            container.clearContent();
        }
        level.removeBlockEntity(pos);
        BlockState boxState = ModBlocks.CARDBOARD_BOX.get().defaultBlockState()
                .setValue(CardboardBoxBlock.FACING, CardboardBoxContents.horizontalFacing(facing));
        if (!level.setBlock(pos, boxState, Block.UPDATE_ALL)) {
            return false;
        }
        if (level.getBlockEntity(pos) instanceof CardboardBoxBlockEntity box) {
            box.setContents(contents);
            box.setChanged();
        }
        return true;
    }

    private static void applyStoredBlockEntity(BlockEntity be, ServerLevel level, net.minecraft.nbt.CompoundTag tag) {
        ValueInput input = TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag);
        be.loadWithComponents(input);
    }

    private static void restoreBlockEntityTag(ServerLevel level, BlockPos pos, CardboardBoxContents contents) {
        contents.blockEntityTag().ifPresent(tag -> {
            BlockEntity be = level.getBlockEntity(pos);
            if (be != null) {
                applyStoredBlockEntity(be, level, tag);
                be.setChanged();
            }
        });
    }

    private static void restoreCapturedBlock(ServerLevel level, BlockPos pos, CardboardBoxContents contents) {
        BlockState restored = contents.blockState(level.registryAccess());
        if (restored == null) {
            return;
        }
        level.setBlock(pos, restored, Block.UPDATE_ALL);
        restoreBlockEntityTag(level, pos, contents);
    }

    private static void playUnwrap(ServerLevel level, BlockPos pos) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + POOF_HEIGHT;
        double z = pos.getZ() + 0.5;
        level.sendParticles(ParticleTypes.POOF, x, y, z, POOF_COUNT, POOF_SPREAD, 0.08, POOF_SPREAD, 0.02);
        level.playSound(null, pos, SoundEvents.BAMBOO_WOOD_BREAK, SoundSource.BLOCKS, 0.6F, 1.1F);
    }
}
