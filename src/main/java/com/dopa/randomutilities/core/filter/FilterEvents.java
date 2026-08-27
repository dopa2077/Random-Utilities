package com.dopa.randomutilities.core.filter;

import com.dopa.randomutilities.dOPasRandomUtilities;

import net.minecraft.util.TriState;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = dOPasRandomUtilities.MOD_ID)
public final class FilterEvents {
    private FilterEvents() {}

    @SubscribeEvent
    public static void onItemPickupPre(ItemEntityPickupEvent.Pre event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        ItemEntity itemEntity = event.getItemEntity();
        ItemStack picked = itemEntity.getItem();
        if (picked.isEmpty() || player.level().isClientSide() || itemEntity.hasPickUpDelay()) {
            return;
        }
        if (!FilterStorage.tryVoidPickup(player, picked)) {
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
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        FilterItem.clearGuiSuppress(event.getEntity().getUUID());
    }
}
