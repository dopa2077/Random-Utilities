package com.dopa.randomutilities.filteritem;

import java.util.function.Consumer;

import com.dopa.randomutilities.filteritem.menu.FilterMenu;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Filter/void item — behaviour driven entirely by {@link FilterProfile}. Register new variants via {@link FilterRegistry}. */
public class FilterItem extends Item {
    private static final String INFINITY = "\u221E";

    private final FilterProfile profile;

    public FilterItem(Properties properties, FilterProfile profile) {
        super(properties);
        this.profile = profile;
    }

    public FilterProfile profile() {
        return profile;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack host = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            openGui(player, hand);
            return InteractionResult.SUCCESS;
        }

        ItemStack selected = FilterStorage.getSelectedStack(host);
        if (selected.isEmpty()) {
            openGui(player, hand);
            return InteractionResult.SUCCESS;
        }

        if (selected.get(DataComponents.CONSUMABLE) != null) {
            if (!level.isClientSide() && !FilterEvents.isUsing(player)) {
                FilterEvents.beginUse(player, hand, host);
                player.setItemInHand(hand, selected.copy());
            }
            return player.getItemInHand(hand).use(level, player, hand);
        }

        player.setItemInHand(hand, selected);
        return restoreHost(player, hand, host, selected.use(level, player, hand));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }

        InteractionHand hand = context.getHand();
        ItemStack host = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            openGui(player, hand);
            return InteractionResult.SUCCESS;
        }

        ItemStack selected = FilterStorage.getSelectedStack(host);
        if (selected.isEmpty()) {
            openGui(player, hand);
            return InteractionResult.SUCCESS;
        }

        BlockHitResult hit = new BlockHitResult(
                context.getClickLocation(),
                context.getClickedFace(),
                context.getClickedPos(),
                context.isInside()
        );
        player.setItemInHand(hand, selected);
        return restoreHost(player, hand, host, selected.useOn(new UseOnContext(player, hand, hit)));
    }

    private static InteractionResult restoreHost(
            Player player,
            InteractionHand hand,
            ItemStack host,
            InteractionResult result
    ) {
        ItemStack after = player.getItemInHand(hand);
        if (result instanceof InteractionResult.Success success
                && success.itemContext().heldItemTransformedTo() != null) {
            after = success.itemContext().heldItemTransformedTo();
            FilterStorage.setSelectedStack(host, after);
            player.setItemInHand(hand, host);
            return success.heldItemTransformedTo(host);
        }
        FilterStorage.setSelectedStack(host, after);
        player.setItemInHand(hand, host);
        return result;
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        ItemStack selected = FilterStorage.getSelectedStack(stack);
        return selected.isEmpty() ? 1.0F : selected.getDestroySpeed(state);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity owner) {
        ItemStack selected = FilterStorage.getSelectedStack(stack);
        if (selected.isEmpty()) {
            return false;
        }
        boolean result = selected.getItem().mineBlock(selected, level, state, pos, owner);
        FilterStorage.setSelectedStack(stack, selected);
        return result;
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        ItemStack selected = FilterStorage.getSelectedStack(stack);
        return !selected.isEmpty() && selected.isCorrectToolForDrops(state);
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        ItemStack selected = FilterStorage.getSelectedStack(stack);
        if (!selected.isEmpty()) {
            selected.hurtEnemy(target, attacker);
            FilterStorage.setSelectedStack(stack, selected);
        }
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        ItemStack selected = FilterStorage.getSelectedStack(stack);
        if (!selected.isEmpty()) {
            selected.postHurtEnemy(target, attacker);
            FilterStorage.setSelectedStack(stack, selected);
        }
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        ItemStack selected = FilterStorage.getSelectedStack(stack);
        if (selected.isEmpty()) {
            return InteractionResult.PASS;
        }
        player.setItemInHand(hand, selected);
        return restoreHost(player, hand, stack, selected.interactLivingEntity(player, target, hand));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        ItemStack selected = FilterStorage.getSelectedStack(stack);
        return !selected.isEmpty() && selected.getItem().isBarVisible(selected);
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        ItemStack selected = FilterStorage.getSelectedStack(stack);
        return selected.isEmpty() ? 0 : selected.getItem().getBarWidth(selected);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        ItemStack selected = FilterStorage.getSelectedStack(stack);
        return selected.isEmpty() ? 0xFFFFFF : selected.getItem().getBarColor(selected);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> tooltip,
            TooltipFlag flag
    ) {
        ItemStack preview = FilterStorage.getPreviewStack(stack);
        if (!preview.isEmpty()) {
            tooltip.accept(Component.literal(preview.getCount() + " × ").append(preview.getHoverName()));
        } else {
            tooltip.accept(Component.translatable(profile.emptyTooltipKey()).withStyle(ChatFormatting.GRAY));
        }
        if (profile.slotsTooltipKey() != null) {
            FilterContents contents = FilterStorage.get(stack);
            int max = contents.maxStackSize();
            tooltip.accept(Component.translatable(
                    profile.slotsTooltipKey(),
                    contents.slotCount(),
                    max == Integer.MAX_VALUE ? INFINITY : Integer.toString(max)
            ).withStyle(ChatFormatting.GRAY));
        }
    }

    public static void openGui(Player player, InteractionHand hand) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        ItemStack host = player.getItemInHand(hand);
        FilterProfile profile = FilterRegistry.profile(host);
        if (profile == null) {
            return;
        }
        FilterContents contents = FilterStorage.get(host);
        serverPlayer.openMenu(
                new SimpleMenuProvider(
                        (id, inv, p) -> new FilterMenu(id, inv, hand),
                        profile.containerTitle()
                ),
                buf -> {
                    buf.writeEnum(hand);
                    if (!profile.isBasic()) {
                        FilterContents.STREAM_CODEC.encode(buf, contents);
                    }
                }
        );
    }
}
