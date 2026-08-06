package com.dopa.randomutilities.filter.network;

import com.dopa.randomutilities.filter.config.DevNullConfig;
import com.dopa.randomutilities.dOPasRandomUtilities;
import com.dopa.randomutilities.filter.menu.FilterMenu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record FilterSettingPayload(byte kind, int value) implements CustomPacketPayload {
    public static final byte KIND_COLOR = 0;
    public static final byte KIND_MAX_STACK = 1;
    public static final byte KIND_HIGHLIGHT_MATCH = 2;

    public static final Type<FilterSettingPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(dOPasRandomUtilities.MOD_ID, "filter_setting"));

    public static final StreamCodec<FriendlyByteBuf, FilterSettingPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE, FilterSettingPayload::kind,
            ByteBufCodecs.VAR_INT, FilterSettingPayload::value,
            FilterSettingPayload::new
    );

    public static FilterSettingPayload color(int rgb) {
        return new FilterSettingPayload(KIND_COLOR, rgb);
    }

    public static FilterSettingPayload maxStack(int size) {
        return new FilterSettingPayload(KIND_MAX_STACK, size);
    }

    public static FilterSettingPayload highlightMatch(boolean match) {
        return new FilterSettingPayload(KIND_HIGHLIGHT_MATCH, match ? 1 : 0);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(FilterSettingPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player.containerMenu instanceof FilterMenu menu) {
                if (payload.kind() == KIND_COLOR) {
                    menu.setColorSetting(payload.value() & 0xFFFFFF);
                } else if (payload.kind() == KIND_MAX_STACK) {
                    menu.setMaxStackSizeSetting(DevNullConfig.clampAdvancedMaxStack(payload.value()));
                } else if (payload.kind() == KIND_HIGHLIGHT_MATCH) {
                    menu.setHighlightMatchColorSetting(payload.value() != 0);
                }
            }
        });
    }
}
