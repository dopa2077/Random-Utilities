package com.dopa.randomutilities.logistics.transfer;

import com.dopa.randomutilities.core.machine.config.UpgradeConfig;
import com.dopa.randomutilities.registry.ModDataComponents;
import com.dopa.randomutilities.registry.ModItems;
import com.dopa.randomutilities.core.util.DescribedBlockItem;

import net.minecraft.ChatFormatting;
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
    private static final int MUSTARD_GOLD = 0xC5A000;
    private static final int WATER_BLUE = 0x3F76E4;
    private static final int ITEM_GREEN = 0x55FF55;
    private static final int ENERGY_RED = 0xFF3333;

    public TransferNodeItem(Block block, Properties properties) {
        super(block, properties, "block.dopasrandomutilities.transfer_node.tooltip");
    }

    public static ItemStack create(HeadKind kind) {
        return create(kind, 1);
    }

    public static ItemStack create(HeadKind kind, int count) {
        if (ModItems.TRANSFER_NODE == null) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(ModItems.TRANSFER_NODE.get(), count);
        setKind(stack, kind);
        return stack;
    }

    public static HeadKind kind(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof TransferNodeItem)) {
            return HeadKind.ITEM;
        }
        return stack.getOrDefault(ModDataComponents.TRANSFER_NODE_KIND.get(), HeadKind.ITEM);
    }

    public static void setKind(ItemStack stack, HeadKind kind) {
        if (stack.isEmpty() || !(stack.getItem() instanceof TransferNodeItem)) {
            return;
        }
        stack.set(ModDataComponents.TRANSFER_NODE_KIND.get(), kind);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(nameKey(kind(stack)));
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> tooltip,
            TooltipFlag flag
    ) {
        HeadKind kind = kind(stack);
        tooltip.accept(Component.translatable(tooltipKey(kind)).withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.empty());
        tooltip.accept(rateLine(kind, 0, 0, 0, 0));
    }

    private static String nameKey(HeadKind kind) {
        return switch (kind) {
            case ITEM -> "block.dopasrandomutilities.transfer_node";
            case FLUID -> "item.dopasrandomutilities.transfer_node_fluid";
            case ENERGY -> "item.dopasrandomutilities.transfer_node_energy";
        };
    }

    private static String tooltipKey(HeadKind kind) {
        return switch (kind) {
            case ITEM -> "block.dopasrandomutilities.transfer_node.tooltip";
            case FLUID -> "block.dopasrandomutilities.transfer_node_fluid.tooltip";
            case ENERGY -> "block.dopasrandomutilities.transfer_node_energy.tooltip";
        };
    }

    /** Colored pull/interval line for item tooltip and GUI display-slot hover. */
    public static Component rateLine(
            HeadKind kind,
            int overclockCount,
            int stackCount,
            int fluidCapacityCount,
            int energyCount
    ) {
        int ticks = UpgradeConfig.transferNodeInterval(kind, overclockCount);
        Component interval = Component.literal(Integer.toString(ticks)).withColor(MUSTARD_GOLD);
        return switch (kind) {
            case ITEM -> Component.translatable(
                    "item.dopasrandomutilities.transfer_node.rate",
                    Component.literal(Integer.toString(UpgradeConfig.stackTransferTotal(stackCount)))
                            .withColor(ITEM_GREEN),
                    interval
            );
            case FLUID -> Component.translatable(
                    "item.dopasrandomutilities.transfer_node_fluid.rate",
                    Component.literal(Integer.toString(UpgradeConfig.transferNodeFluidAmount(fluidCapacityCount)))
                            .withColor(WATER_BLUE),
                    interval
            );
            case ENERGY -> Component.translatable(
                    "item.dopasrandomutilities.transfer_node_energy.rate",
                    Component.literal(Integer.toString(UpgradeConfig.transferNodeEnergyAmount(energyCount)))
                            .withColor(ENERGY_RED),
                    interval
            );
        };
    }

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
            be.setHead(head, kind(context.getItemInHand()));
            BlockState connected = TransferNodeBlock.withConnections(level.getBlockState(pos), level, pos);
            if (connected != state) {
                level.setBlock(pos, connected, Block.UPDATE_ALL);
            }
        }
        return true;
    }
}
