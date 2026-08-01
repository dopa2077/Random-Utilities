package com.dopa.randomutilities.filteritem;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.dopa.randomutilities.dOPasRandomUtilities;

import net.minecraft.util.TriState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

@EventBusSubscriber(modid = dOPasRandomUtilities.MOD_ID)
public final class FilterEvents {
    private static final Map<UUID, PendingUse> PENDING = new ConcurrentHashMap<>();

    private FilterEvents() {}

    public static void beginUse(Player player, InteractionHand hand, ItemStack host) {
        PENDING.put(player.getUUID(), new PendingUse(hand, host.copy()));
    }

    public static boolean isUsing(Player player) {
        return PENDING.containsKey(player.getUUID());
    }

    @SubscribeEvent
    public static void onItemPickupPre(ItemEntityPickupEvent.Pre event) {
        Player player = event.getPlayer();
        ItemEntity itemEntity = event.getItemEntity();
        ItemStack picked = itemEntity.getItem();
        if (picked.isEmpty() || player.level().isClientSide() || itemEntity.hasPickUpDelay()) {
            return;
        }
        if (!FilterStorage.hasMatchingFilter(player, picked)) {
            return;
        }

        Inventory inventory = player.getInventory();
        int remaining = picked.getCount();
        for (int i = 0; i < inventory.getContainerSize() && remaining > 0; i++) {
            ItemStack host = inventory.getItem(i);
            if (!FilterRegistry.isFilterItem(host)) {
                continue;
            }
            ItemStack probe = picked.copyWithCount(remaining);
            int absorbed = probe.getCount() - FilterStorage.absorb(host, probe);
            if (absorbed > 0) {
                inventory.setItem(i, host);
                remaining -= absorbed;
            }
        }

        picked.setCount(0);
        event.setCanPickup(TriState.FALSE);
        if (itemEntity.getItem().isEmpty()) {
            itemEntity.discard();
        }
    }

    @SubscribeEvent
    public static void onUseStop(LivingEntityUseItemEvent.Stop event) {
        if (PENDING.containsKey(event.getEntity().getUUID())) {
            finishUse(event.getEntity(), event.getItem());
        }
    }

    @SubscribeEvent
    public static void onUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (PENDING.containsKey(event.getEntity().getUUID())) {
            finishUse(event.getEntity(), event.getResultStack());
            event.setResultStack(ItemStack.EMPTY);
        }
    }

    private static void finishUse(LivingEntity entity, ItemStack remaining) {
        if (!(entity instanceof Player player)) {
            return;
        }
        PendingUse pending = PENDING.remove(player.getUUID());
        if (pending == null) {
            return;
        }
        ItemStack host = pending.host();
        FilterStorage.setSelectedStack(host, remaining);
        player.setItemInHand(pending.hand(), host);
    }

    private record PendingUse(InteractionHand hand, ItemStack host) {}
}
