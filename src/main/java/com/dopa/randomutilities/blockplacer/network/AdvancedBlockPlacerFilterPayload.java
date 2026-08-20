package com.dopa.randomutilities.blockplacer.network;

import com.dopa.randomutilities.blockplacer.menu.AdvancedBlockPlacerMenu;
import com.dopa.randomutilities.dOPasRandomUtilities;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record AdvancedBlockPlacerFilterPayload(int slotIndex, ItemStack filterItem) implements CustomPacketPayload {
    public static final Type<AdvancedBlockPlacerFilterPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(dOPasRandomUtilities.MOD_ID, "advanced_block_placer_filter"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AdvancedBlockPlacerFilterPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, AdvancedBlockPlacerFilterPayload::slotIndex,
                    ItemStack.STREAM_CODEC, AdvancedBlockPlacerFilterPayload::filterItem,
                    AdvancedBlockPlacerFilterPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AdvancedBlockPlacerFilterPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (!(player.containerMenu instanceof AdvancedBlockPlacerMenu menu)) {
                return;
            }
            if (payload.filterItem().isEmpty()) {
                return;
            }
            if (payload.slotIndex() < 0 || payload.slotIndex() >= AdvancedBlockPlacerMenu.FILTER_SLOT_COUNT) {
                return;
            }
            menu.setFilterSlot(payload.slotIndex(), payload.filterItem().copyWithCount(1));
        });
    }
}
