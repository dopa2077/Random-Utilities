package com.dopa.randomutilities.filteritem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.dopa.randomutilities.dOPasRandomUtilities;

import net.minecraft.util.TriState;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = dOPasRandomUtilities.MOD_ID)
public final class FilterEvents {
    private static final Map<UUID, FilterSlotCache> FILTER_SLOTS = new ConcurrentHashMap<>();

    private FilterEvents() {}

    static int[] getFilterSlotIndices(Player player) {
        return FILTER_SLOTS.computeIfAbsent(player.getUUID(), uuid -> buildFilterSlotCache(player)).filterSlots();
    }

    static void invalidateFilterCache(Player player) {
        FILTER_SLOTS.remove(player.getUUID());
    }

    private static boolean playerHasFilters(Player player) {
        return getFilterSlotIndices(player).length > 0;
    }

    private static FilterSlotCache buildFilterSlotCache(Player player) {
        Inventory inventory = player.getInventory();
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (FilterRegistry.isFilterItem(inventory.getItem(i))) {
                slots.add(i);
            }
        }
        int[] indices = new int[slots.size()];
        for (int i = 0; i < slots.size(); i++) {
            indices[i] = slots.get(i);
        }
        return new FilterSlotCache(indices);
    }

    @SubscribeEvent
    public static void onItemPickupPre(ItemEntityPickupEvent.Pre event) {
        Player player = event.getPlayer();
        ItemEntity itemEntity = event.getItemEntity();
        ItemStack picked = itemEntity.getItem();
        if (picked.isEmpty() || player.level().isClientSide() || itemEntity.hasPickUpDelay()) {
            return;
        }
        if (!playerHasFilters(player)) {
            return;
        }
        if (!FilterStorage.tryVoidPickup(player, getFilterSlotIndices(player), picked)) {
            return;
        }

        player.level().playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.ITEM_PICKUP,
                SoundSource.PLAYERS,
                0.2F,
                ((player.getRandom().nextFloat() - player.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F
        );

        picked.setCount(0);
        event.setCanPickup(TriState.FALSE);
        if (itemEntity.getItem().isEmpty()) {
            itemEntity.discard();
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        FILTER_SLOTS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        invalidateFilterCache(event.getEntity());
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        invalidateFilterCache(event.getEntity());
    }

    private record FilterSlotCache(int[] filterSlots) {}
}
