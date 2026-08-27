package com.dopa.randomutilities.logistics.collector.network;

import com.dopa.randomutilities.dOPasRandomUtilities;
import com.dopa.randomutilities.logistics.collector.menu.ItemCollectorMenu;
import com.dopa.randomutilities.core.machine.RedstoneMode;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ItemCollectorSettingPayload(byte kind, int value) implements CustomPacketPayload {
    public static final byte KIND_RANGE_X = 0;
    public static final byte KIND_RANGE_Y = 1;
    public static final byte KIND_RANGE_Z = 2;
    public static final byte KIND_PICKUP_DELAY = 3;
    public static final byte KIND_PICKUP_BATCH = 4;
    public static final byte KIND_FILTER_MODE = 5;
    public static final byte KIND_REDSTONE = 7;
    public static final byte KIND_COLOR = 8;
    public static final byte KIND_PARTICLES = 9;

    public static final Type<ItemCollectorSettingPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(dOPasRandomUtilities.MOD_ID, "item_collector_setting"));

    public static final StreamCodec<FriendlyByteBuf, ItemCollectorSettingPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE, ItemCollectorSettingPayload::kind,
            ByteBufCodecs.VAR_INT, ItemCollectorSettingPayload::value,
            ItemCollectorSettingPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ItemCollectorSettingPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (!(player.containerMenu instanceof ItemCollectorMenu menu) || !menu.stillValid(player)) {
                return;
            }
            switch (payload.kind()) {
                case KIND_RANGE_X -> menu.setRangeX(payload.value());
                case KIND_RANGE_Y -> menu.setRangeY(payload.value());
                case KIND_RANGE_Z -> menu.setRangeZ(payload.value());
                case KIND_PICKUP_DELAY -> menu.setPickupDelay(payload.value());
                case KIND_PICKUP_BATCH -> menu.setPickupBatch(payload.value());
                case KIND_FILTER_MODE -> menu.setWhitelistMode(payload.value() != 0);
                case KIND_REDSTONE -> menu.setRedstoneMode(RedstoneMode.byOrdinal(payload.value()));
                case KIND_COLOR -> menu.setOverlayColor(payload.value());
                case KIND_PARTICLES -> menu.setParticlesEnabled(payload.value() != 0);
                default -> {
                }
            }
        });
    }
}
