package com.dopa.randomutilities.filter;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import com.dopa.randomutilities.filter.config.DevNullConfig;
import com.dopa.randomutilities.util.CompactCountFormat;
import com.dopa.randomutilities.filter.menu.FilterMenu;

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
import net.minecraft.world.item.BlockItem;
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
import net.neoforged.neoforge.transfer.item.ItemResource;

/** Filter/void item — behaviour driven entirely by {@link FilterProfile}. Register new variants via {@link FilterRegistry}. */
public class FilterItem extends Item {
    private static final int GUI_SUPPRESS_TICKS = 15;
    /** Expiry in level game time (not player tickCount — survives reconnect). */
    private static final Map<UUID, Long> SUPPRESS_GUI_UNTIL = new ConcurrentHashMap<>();

    private final FilterProfile profile;

    public FilterItem(Properties properties, FilterProfile profile) {
        super(properties);
        this.profile = profile;
    }

    public FilterProfile profile() {
        return profile;
    }

    static void clearGuiSuppress(UUID playerId) {
        SUPPRESS_GUI_UNTIL.remove(playerId);
    }

    private static boolean isGuiSuppressed(Player player) {
        Long until = SUPPRESS_GUI_UNTIL.get(player.getUUID());
        if (until == null) {
            return false;
        }
        long now = player.level().getGameTime();
        if (now >= until) {
            SUPPRESS_GUI_UNTIL.remove(player.getUUID(), until);
            return false;
        }
        return true;
    }

    private static void suppressGui(Player player) {
        SUPPRESS_GUI_UNTIL.put(player.getUUID(), player.level().getGameTime() + GUI_SUPPRESS_TICKS);
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
            if (isGuiSuppressed(player)) {
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

        // Instant hand-swap proxy is unsafe: items like backpacks open a menu bound to the
        // temporary hand stack; even closing that menu leaves in-flight open-screen packets
        // that desync/wipe the player inventory. Consumables use startUsingItem on the host instead.
        return InteractionResult.PASS;
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
            if (isGuiSuppressed(player)) {
                return InteractionResult.PASS;
            }
            openGui(player, hand);
            return InteractionResult.SUCCESS;
        }

        InteractionResult placed = tryPlaceSelectedBlock(context, player, host);
        if (placed != InteractionResult.PASS) {
            return placed;
        }

        return InteractionResult.PASS;
    }

    /**
     * Places the selected slot's block without swapping the /dev/null out of the hand.
     * Only {@link BlockItem}s are forwarded; other items keep the existing consume/proxy paths.
     */
    private InteractionResult tryPlaceSelectedBlock(UseOnContext context, Player player, ItemStack host) {
        FilterProfile profile = profile();
        if (!DevNullConfig.canPlaceBlocks(profile.isBasic())) {
            return InteractionResult.PASS;
        }

        FilterContents contents = FilterStorage.get(host);
        int slotIndex = contents.selectedSlot();
        FilterContents.Slot slot = contents.slot(slotIndex);
        if (slot.isEmpty()) {
            return InteractionResult.PASS;
        }

        ItemStack toPlace = slot.resource().toStack(1);
        if (!(toPlace.getItem() instanceof BlockItem blockItem)) {
            return InteractionResult.PASS;
        }

        BlockHitResult hit = new BlockHitResult(
                context.getClickLocation(),
                context.getClickedFace(),
                context.getClickedPos(),
                context.isInside()
        );
        UseOnContext placeContext = new UseOnContext(
                context.getLevel(),
                player,
                context.getHand(),
                toPlace,
                hit
        );
        InteractionResult result = blockItem.useOn(placeContext);
        if (result.consumesAction() && toPlace.isEmpty() && !player.hasInfiniteMaterials()) {
            int remaining = slot.count() - 1;
            FilterStorage.set(host, contents.withSlot(
                    slotIndex,
                    remaining <= 0 ? ItemResource.EMPTY : slot.resource(),
                    Math.max(0, remaining)
            ));
            if (remaining <= 0) {
                suppressGui(player);
            }
        }
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
            ItemStack beforeUse = single.copy();
            single.getItem().finishUsingItem(single, level, entity);

            UseCooldown useCooldown = selected.get(DataComponents.USE_COOLDOWN);
            if (useCooldown != null && entity instanceof Player player) {
                useCooldown.apply(selected, player);
            }

            if (!infiniteMaterials) {
                ItemStack remainder = FilterStorage.resolveUseRemainder(beforeUse, single, 1, false);
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
                    suppressGui(player);
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
        return InteractionResult.PASS;
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
        openGui(player, hand, false);
    }

    public static void openGui(Player player, InteractionHand hand, boolean restoreConfig) {
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
