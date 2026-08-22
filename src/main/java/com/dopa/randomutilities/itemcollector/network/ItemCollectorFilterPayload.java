package com.dopa.randomutilities.itemcollector.network;

import com.dopa.randomutilities.dOPasRandomUtilities;
import com.dopa.randomutilities.itemcollector.menu.ItemCollectorMenu;
import com.dopa.randomutilities.util.GhostFilterPayloads;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ItemCollectorFilterPayload(int slotIndex, ItemStack filterItem) implements CustomPacketPayload {
    public static final Type<ItemCollectorFilterPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(dOPasRandomUtilities.MOD_ID, "item_collector_filter"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ItemCollectorFilterPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ItemCollectorFilterPayload::slotIndex,
            ItemStack.STREAM_CODEC, ItemCollectorFilterPayload::filterItem,
            ItemCollectorFilterPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ItemCollectorFilterPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (!(player.containerMenu instanceof ItemCollectorMenu menu) || !menu.stillValid(player)) {
                return;
            }
            if (payload.filterItem().isEmpty()) {
                return;
            }
            int slots = menu.collectorType().filterSlotCount();
            if (payload.slotIndex() < 0 || payload.slotIndex() >= slots) {
                return;
            }
            menu.setFilterSlot(payload.slotIndex(), GhostFilterPayloads.sanitizeGhost(payload.filterItem()));
        });
    }
}
