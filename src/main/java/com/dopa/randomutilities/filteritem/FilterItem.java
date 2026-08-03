package com.dopa.randomutilities.filteritem;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import com.dopa.randomutilities.filteritem.client.CompactCountFormat;
import com.dopa.randomutilities.filteritem.menu.FilterMenu;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.UseCooldown;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Filter/void item — behaviour driven entirely by {@link FilterProfile}. Register new variants via {@link FilterRegistry}. */
public class FilterItem extends Item {
    private static final int GUI_SUPPRESS_TICKS = 15;
    private static final Map<UUID, Integer> SUPPRESS_GUI_UNTIL = new ConcurrentHashMap<>();

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
            if (player.tickCount < SUPPRESS_GUI_UNTIL.getOrDefault(player.getUUID(), 0)) {
                return InteractionResult.PASS;
            }
            openGui(player, hand);
            return InteractionResult.SUCCESS;
        }

        if (selected.get(DataComponents.CONSUMABLE) != null || selected.getUseDuration(player) > 0) {
            Consumable consumable = selected.get(DataComponents.CONSUMABLE);
            if (consumable != null) {
                consumable.emitParticlesAndSounds(player.getRandom(), player, selected, selected.getUseDuration(player));
            }
            player.startUsingItem(hand);
            return InteractionResult.CONSUME;
        }

        // Instant actions: swap to proxy, use, restore filter in the same call.
        player.setItemInHand(hand, selected);
        InteractionResult result;
        try {
            result = selected.use(level, player, hand);
        } catch (RuntimeException exception) {
            player.setItemInHand(hand, host);
            throw exception;
        }
        return restoreHost(player, hand, host, result);
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
            if (player.tickCount < SUPPRESS_GUI_UNTIL.getOrDefault(player.getUUID(), 0)) {
                return InteractionResult.PASS;
            }
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
        InteractionResult result;
        try {
            result = selected.useOn(new UseOnContext(player, hand, hit));
        } catch (RuntimeException exception) {
            player.setItemInHand(hand, host);
            throw exception;
        }
        return restoreHost(player, hand, host, result);
    }

    private static InteractionResult restoreHost(
            Player player,
            InteractionHand hand,
            ItemStack host,
            InteractionResult result
    ) {
        if (result == InteractionResult.CONSUME) {
            player.setItemInHand(hand, host);
            return result;
        }

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
    public ItemUseAnimation getUseAnimation(ItemStack host) {
        ItemStack selected = FilterStorage.getSelectedStack(host);
        return selected.isEmpty() ? super.getUseAnimation(host) : selected.getUseAnimation();
    }

    @Override
    public int getUseDuration(ItemStack host, LivingEntity user) {
        ItemStack selected = FilterStorage.getSelectedStack(host);
        return selected.isEmpty() ? 0 : selected.getUseDuration(user);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack host, int remainingUseDuration) {
        ItemStack selected = FilterStorage.getSelectedStack(host);
        if (!selected.isEmpty()) {
            selected.onUseTick(level, entity, remainingUseDuration);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack host, Level level, LivingEntity entity) {
        ItemStack selected = FilterStorage.getSelectedStack(host);
        if (selected.isEmpty()) {
            return host;
        }

        if (selected.get(DataComponents.CONSUMABLE) == null) {
            if (selected.getUseDuration(entity) > 0 && !level.isClientSide()) {
                ItemStack result = selected.finishUsingItem(level, entity);
                FilterStorage.setSelectedStack(host, result);
            }
            return host;
        }

        if (!level.isClientSide()) {
            FilterContents contents = FilterStorage.get(host);
            int slotIndex = contents.selectedSlot();
            int countBefore = selected.getCount();
            boolean infiniteMaterials = entity instanceof Player player && player.hasInfiniteMaterials();

            ItemStack single = selected.copyWithCount(1);
            single.getItem().finishUsingItem(single, level, entity);

            UseCooldown useCooldown = selected.get(DataComponents.USE_COOLDOWN);
            if (useCooldown != null && entity instanceof Player player) {
                useCooldown.apply(selected, player);
            }

            if (!infiniteMaterials) {
                ItemStack remainder = FilterStorage.resolveUseRemainder(single, 1, false);
                ItemStack remaining = countBefore <= 1
                        ? ItemStack.EMPTY
                        : selected.copyWithCount(countBefore - 1);
                FilterStorage.setSelectedStack(host, remaining);

                if (!remainder.isEmpty()) {
                    if (countBefore <= 1) {
                        FilterStorage.setSlotStack(host, slotIndex, remainder);
                    } else if (entity instanceof Player player) {
                        FilterStorage.insertRemainderOrDrop(host, slotIndex, remainder, player);
                    }
                }

                if (remaining.isEmpty() && entity instanceof Player player) {
                    SUPPRESS_GUI_UNTIL.put(player.getUUID(), player.tickCount + GUI_SUPPRESS_TICKS);
                }
            }
        }
        return host;
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
        Item usedItem = selected.getItem();
        boolean result = usedItem.mineBlock(selected, level, state, pos, owner);
        if (result && owner instanceof Player player) {
            player.awardStat(Stats.ITEM_USED.get(usedItem));
        }
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
        // Instant swap + restore in the same call (non-consumables).
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
            tooltip.accept(Component.literal(CompactCountFormat.format(preview.getCount()) + " × ")
                    .append(preview.getHoverName()));
        } else {
            tooltip.accept(Component.translatable(profile.emptyTooltipKey()).withStyle(ChatFormatting.GRAY));
        }
        if (profile.slotsTooltipKey() != null) {
            FilterContents contents = FilterStorage.get(stack);
            tooltip.accept(Component.translatable(
                    profile.slotsTooltipKey(),
                    contents.slotCount(),
                    CompactCountFormat.format(contents.maxStackSize())
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
        boolean restoreConfig = FilterMenu.restoreConfigOnNextOpen;
        FilterMenu.restoreConfigOnNextOpen = false;
        serverPlayer.openMenu(
                new SimpleMenuProvider(
                        (id, inv, p) -> new FilterMenu(id, inv, hand),
                        profile.containerTitle()
                ),
                buf -> {
                    buf.writeBoolean(false);
                    buf.writeEnum(hand);
                    if (!profile.isBasic()) {
                        buf.writeBoolean(true);
                        FilterContents.STREAM_CODEC.encode(buf, contents);
                        buf.writeBoolean(restoreConfig);
                    } else {
                        buf.writeBoolean(false);
                    }
                }
        );
    }
}
