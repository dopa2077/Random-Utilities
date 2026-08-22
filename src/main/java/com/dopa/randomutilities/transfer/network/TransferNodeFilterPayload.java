package com.dopa.randomutilities.transfer.network;

import com.dopa.randomutilities.dOPasRandomUtilities;
import com.dopa.randomutilities.transfer.menu.TransferNodeMenu;
import com.dopa.randomutilities.util.GhostFilterPayloads;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record TransferNodeFilterPayload(int slotIndex, ItemStack filterItem) implements CustomPacketPayload {
    public static final Type<TransferNodeFilterPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(dOPasRandomUtilities.MOD_ID, "transfer_node_filter"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TransferNodeFilterPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TransferNodeFilterPayload::slotIndex,
            ItemStack.STREAM_CODEC, TransferNodeFilterPayload::filterItem,
            TransferNodeFilterPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TransferNodeFilterPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (!(player.containerMenu instanceof TransferNodeMenu menu) || !menu.stillValid(player)) {
                return;
            }
            if (payload.slotIndex() < 0 || payload.slotIndex() >= TransferNodeMenu.FILTER_SLOT_COUNT) {
                return;
            }
            if (payload.filterItem().isEmpty()) {
                menu.setFilterSlot(payload.slotIndex(), ItemStack.EMPTY);
                return;
            }
            menu.setFilterSlot(payload.slotIndex(), GhostFilterPayloads.sanitizeGhost(payload.filterItem()));
        });
    }
}
