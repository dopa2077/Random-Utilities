package com.dopa.randomutilities.machine.network;

import com.dopa.randomutilities.dOPasRandomUtilities;
import com.dopa.randomutilities.fishnet.menu.FishnetMenu;
import com.dopa.randomutilities.machine.RedstoneMode;
import com.dopa.randomutilities.generator.menu.ResourceGeneratorMenu;
import com.dopa.randomutilities.solarfurnace.menu.SolarFurnaceMenu;
import com.dopa.randomutilities.transfer.menu.TransferEnergyMenu;
import com.dopa.randomutilities.transfer.menu.TransferNodeMenu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record MachineSettingPayload(byte kind, int value) implements CustomPacketPayload {
    public static final byte KIND_LOCK_OUTPUT = 0;
    public static final byte KIND_REDSTONE = 1;

    public static final Type<MachineSettingPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(dOPasRandomUtilities.MOD_ID, "machine_setting"));

    public static final StreamCodec<FriendlyByteBuf, MachineSettingPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE, MachineSettingPayload::kind,
            ByteBufCodecs.VAR_INT, MachineSettingPayload::value,
            MachineSettingPayload::new
    );

    public static MachineSettingPayload lockOutput(boolean locked) {
        return new MachineSettingPayload(KIND_LOCK_OUTPUT, locked ? 1 : 0);
    }

    public static MachineSettingPayload redstone(RedstoneMode mode) {
        return new MachineSettingPayload(KIND_REDSTONE, mode.ordinal());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MachineSettingPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player.containerMenu instanceof ResourceGeneratorMenu menu) {
                if (payload.kind() == KIND_LOCK_OUTPUT) {
                    menu.setOutputLocked(payload.value() != 0);
                } else if (payload.kind() == KIND_REDSTONE) {
                    menu.setRedstoneMode(RedstoneMode.byOrdinal(payload.value()));
                }
            } else if (player.containerMenu instanceof SolarFurnaceMenu menu) {
                if (payload.kind() == KIND_REDSTONE) {
                    menu.setRedstoneMode(RedstoneMode.byOrdinal(payload.value()));
                }
            } else if (player.containerMenu instanceof FishnetMenu menu) {
                if (payload.kind() == KIND_REDSTONE) {
                    menu.setRedstoneMode(RedstoneMode.byOrdinal(payload.value()));
                }
            } else if (player.containerMenu instanceof TransferNodeMenu menu) {
                if (payload.kind() == KIND_REDSTONE) {
                    menu.setRedstoneMode(RedstoneMode.byOrdinal(payload.value()));
                }
            } else if (player.containerMenu instanceof TransferEnergyMenu energyMenu) {
                if (payload.kind() == KIND_REDSTONE) {
                    energyMenu.setRedstoneMode(RedstoneMode.byOrdinal(payload.value()));
                }
            }
        });
    }
}
