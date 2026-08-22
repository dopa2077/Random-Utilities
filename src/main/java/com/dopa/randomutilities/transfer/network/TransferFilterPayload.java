package com.dopa.randomutilities.transfer.network;

import com.dopa.randomutilities.dOPasRandomUtilities;
import com.dopa.randomutilities.transfer.menu.TransferFilterMenu;
import com.dopa.randomutilities.util.GhostFilterPayloads;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record TransferFilterPayload(int slotIndex, ItemStack filterItem) implements CustomPacketPayload {
    public static final Type<TransferFilterPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(dOPasRandomUtilities.MOD_ID, "transfer_filter"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TransferFilterPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TransferFilterPayload::slotIndex,
            ItemStack.STREAM_CODEC, TransferFilterPayload::filterItem,
            TransferFilterPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TransferFilterPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (!(player.containerMenu instanceof TransferFilterMenu menu) || !menu.stillValid(player)) {
                return;
            }
            if (payload.filterItem().isEmpty()) {
                return;
            }
            menu.setFilterSlot(payload.slotIndex(), GhostFilterPayloads.sanitizeGhost(payload.filterItem()));
        });
    }
}
