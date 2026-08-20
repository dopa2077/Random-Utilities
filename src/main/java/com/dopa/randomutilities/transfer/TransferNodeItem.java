package com.dopa.randomutilities.transfer;

import com.dopa.randomutilities.machine.config.UpgradeConfig;
import com.dopa.randomutilities.util.DescribedBlockItem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Consumer;

public class TransferNodeItem extends DescribedBlockItem {
    private final HeadKind kind;

    public TransferNodeItem(Block block, Properties properties, String tooltipKey) {
        this(block, properties, tooltipKey, HeadKind.ITEM);
    }

    public TransferNodeItem(Block block, Properties properties, String tooltipKey, HeadKind kind) {
        super(block, properties, tooltipKey);
        this.kind = kind;
    }

    public HeadKind kind() {
        return kind;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> tooltip,
            TooltipFlag flag
    ) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.empty());
        int ticks = UpgradeConfig.transferNodeBaseTicks(kind);
        Component interval = Component.literal(Integer.toString(ticks)).withColor(MUSTARD_GOLD);
        tooltip.accept(switch (kind) {
            case ITEM -> Component.translatable(
                    "item.dopasrandomutilities.transfer_node.rate",
                    Component.literal("1").withColor(0x55FF55),
                    interval
            );
            case FLUID -> Component.translatable(
                    "item.dopasrandomutilities.transfer_node_fluid.rate",
                    Component.literal(Integer.toString(UpgradeConfig.transferNodeBaseMb())).withColor(WATER_BLUE),
                    interval
            );
            case ENERGY -> Component.translatable(
                    "item.dopasrandomutilities.transfer_node_energy.rate",
                    Component.literal(Integer.toString(UpgradeConfig.transferNodeBaseFe())).withColor(0xFF3333),
                    interval
            );
        });
    }

    private static final int MUSTARD_GOLD = 0xC5A000;
    private static final int WATER_BLUE = 0x3F76E4;

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        Direction face = context.getClickedFace();
        BlockState clicked = level.getBlockState(clickedPos);
        if (clicked.getBlock() instanceof TransferNodeBlock
                && !TransferNodeBlock.hasHead(level, clickedPos, face)) {
            return TransferNodeBlock.addHead(
                    level, clickedPos, clicked, face, context.getPlayer(), context.getItemInHand());
        }
        if (clicked.getBlock() instanceof TransferPipeBlock) {
            return TransferNodeBlock.convertPipe(
                    level,
                    clickedPos,
                    clicked,
                    face,
                    context.getPlayer(),
                    context.getItemInHand(),
                    this.getBlock().defaultBlockState()
            );
        }
        BlockPos placePos = clickedPos.relative(face);
        BlockState atPlace = level.getBlockState(placePos);
        Direction head = face.getOpposite();
        if (atPlace.getBlock() instanceof TransferNodeBlock) {
            if (!TransferNodeBlock.hasHead(level, placePos, head)) {
                return TransferNodeBlock.addHead(
                        level, placePos, atPlace, head, context.getPlayer(), context.getItemInHand());
            }
            // Destination face already headed — let vanilla placement fail/replace rather than soft-fail.
            return InteractionResult.FAIL;
        }
        if (atPlace.getBlock() instanceof TransferPipeBlock) {
            return TransferNodeBlock.convertPipe(
                    level,
                    placePos,
                    atPlace,
                    head,
                    context.getPlayer(),
                    context.getItemInHand(),
                    this.getBlock().defaultBlockState()
            );
        }
        return super.useOn(context);
    }

    @Override
    protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
        if (!super.placeBlock(context, state)) {
            return false;
        }
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (level.getBlockEntity(pos) instanceof TransferNodeBlockEntity be) {
            Direction head = context.getClickedFace().getOpposite();
            be.setHead(head, kind);
            BlockState connected = TransferNodeBlock.withConnections(level.getBlockState(pos), level, pos);
            if (connected != state) {
                level.setBlock(pos, connected, Block.UPDATE_ALL);
            }
        }
        return true;
    }
}
