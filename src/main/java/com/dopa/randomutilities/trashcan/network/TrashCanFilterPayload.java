package com.dopa.randomutilities.trashcan.network;

import com.dopa.randomutilities.dOPasRandomUtilities;
import com.dopa.randomutilities.trashcan.TrashCanMenu;
import com.dopa.randomutilities.util.GhostFilterPayloads;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record TrashCanFilterPayload(int slotIndex, ItemStack filterItem) implements CustomPacketPayload {
    public static final Type<TrashCanFilterPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(dOPasRandomUtilities.MOD_ID, "trash_can_filter"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TrashCanFilterPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TrashCanFilterPayload::slotIndex,
            ItemStack.STREAM_CODEC, TrashCanFilterPayload::filterItem,
            TrashCanFilterPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TrashCanFilterPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (!(player.containerMenu instanceof TrashCanMenu menu) || !menu.stillValid(player)) {
                return;
            }
            if (payload.filterItem().isEmpty()) {
                return;
            }
            if (payload.slotIndex() < 0 || payload.slotIndex() >= TrashCanMenu.FILTER_SLOT_COUNT) {
                return;
            }
            menu.setFilterSlot(payload.slotIndex(), GhostFilterPayloads.sanitizeGhost(payload.filterItem()));
        });
    }
}
