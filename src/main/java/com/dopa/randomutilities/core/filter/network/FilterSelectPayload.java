package com.dopa.randomutilities.core.filter.network;

import com.dopa.randomutilities.dOPasRandomUtilities;
import com.dopa.randomutilities.core.filter.FilterContents;
import com.dopa.randomutilities.core.filter.FilterProfile;
import com.dopa.randomutilities.core.filter.FilterRegistry;
import com.dopa.randomutilities.core.filter.FilterStorage;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record FilterSelectPayload(byte mode, Identifier blockId) implements CustomPacketPayload {
    public static final byte MODE_MATCH_BLOCK = 0;
    public static final byte MODE_CYCLE_NEXT = 1;
    public static final byte MODE_CYCLE_PREV = 2;

    public static final Type<FilterSelectPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(dOPasRandomUtilities.MOD_ID, "filter_select"));

    public static final StreamCodec<FriendlyByteBuf, FilterSelectPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE, FilterSelectPayload::mode,
            Identifier.STREAM_CODEC, FilterSelectPayload::blockId,
            FilterSelectPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(FilterSelectPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            ItemStack host = findCyclingHost(player);
            if (host == null || !FilterRegistry.isFilterItem(host)) {
                return;
            }

            if (payload.mode() == MODE_CYCLE_NEXT || payload.mode() == MODE_CYCLE_PREV) {
                int direction = payload.mode() == MODE_CYCLE_NEXT ? 1 : -1;
                int selected = FilterStorage.cycleNonEmptySlot(host, direction);
                player.sendOverlayMessage(
                        net.minecraft.network.chat.Component.translatable(
                                "message.dopasrandomutilities.dev_null.selected",
                                selected + 1
                        )
                );
                return;
            }

            Block block = BuiltInRegistries.BLOCK.getValue(payload.blockId());
            if (block == null) {
                return;
            }
            Item item = block.asItem();
            ItemStack filter = new ItemStack(item);
            if (filter.isEmpty()) {
                return;
            }
            int slot = FilterStorage.findMatchingSlot(host, filter);
            if (slot >= 0) {
                var contents = FilterStorage.get(host).withSelectedSlot(slot);
                contents = contents.withPage(slot / FilterContents.SLOTS_PER_PAGE);
                FilterStorage.set(host, contents);
                player.sendOverlayMessage(
                        net.minecraft.network.chat.Component.translatable(
                                "message.dopasrandomutilities.dev_null.selected",
                                slot + 1
                        )
                );
            }
        });
    }

    private static ItemStack findCyclingHost(Player player) {
        ItemStack host = player.getItemInHand(InteractionHand.MAIN_HAND);
        FilterProfile profile = FilterRegistry.profile(host);
        if (profile != null && profile.slotCycling()) {
            return host;
        }
        host = player.getItemInHand(InteractionHand.OFF_HAND);
        profile = FilterRegistry.profile(host);
        return profile != null && profile.slotCycling() ? host : null;
    }
}
