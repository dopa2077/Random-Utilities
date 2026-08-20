package com.dopa.randomutilities.blockbreaker.network;

import com.dopa.randomutilities.blockbreaker.menu.AdvancedBlockBreakerMenu;
import com.dopa.randomutilities.dOPasRandomUtilities;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record AdvancedBlockBreakerFilterPayload(int slotIndex, ItemStack filterItem) implements CustomPacketPayload {
    public static final Type<AdvancedBlockBreakerFilterPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(dOPasRandomUtilities.MOD_ID, "advanced_block_breaker_filter"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AdvancedBlockBreakerFilterPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, AdvancedBlockBreakerFilterPayload::slotIndex,
                    ItemStack.STREAM_CODEC, AdvancedBlockBreakerFilterPayload::filterItem,
                    AdvancedBlockBreakerFilterPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AdvancedBlockBreakerFilterPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (!(player.containerMenu instanceof AdvancedBlockBreakerMenu menu)) {
                return;
            }
            if (payload.filterItem().isEmpty()) {
                return;
            }
            if (payload.slotIndex() < 0 || payload.slotIndex() >= AdvancedBlockBreakerMenu.FILTER_SLOT_COUNT) {
                return;
            }
            menu.setFilterSlot(payload.slotIndex(), payload.filterItem().copyWithCount(1));
        });
    }
}
