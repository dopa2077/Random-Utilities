package com.dopa.randomutilities.filter.network;

import com.dopa.randomutilities.dOPasRandomUtilities;
import com.dopa.randomutilities.filter.menu.GhostFilterMenu;
import com.dopa.randomutilities.util.GhostFilterPayloads;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record GhostFilterPayload(int slotIndex, ItemStack filterItem) implements CustomPacketPayload {
    public static final Type<GhostFilterPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(dOPasRandomUtilities.MOD_ID, "ghost_filter"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GhostFilterPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, GhostFilterPayload::slotIndex,
            ItemStack.STREAM_CODEC, GhostFilterPayload::filterItem,
            GhostFilterPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(GhostFilterPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (!(player.containerMenu instanceof GhostFilterMenu menu)
                    || !(player.containerMenu instanceof AbstractContainerMenu container)
                    || !container.stillValid(player)) {
                return;
            }
            if (payload.filterItem().isEmpty()) {
                return;
            }
            if (payload.slotIndex() < 0 || payload.slotIndex() >= menu.filterSlotCount()) {
                return;
            }
            menu.setFilterSlot(payload.slotIndex(), GhostFilterPayloads.sanitizeGhost(payload.filterItem()));
        });
    }
}
