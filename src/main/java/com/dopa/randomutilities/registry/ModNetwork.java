package com.dopa.randomutilities.registry;

import com.dopa.randomutilities.core.filter.network.FilterSelectPayload;
import com.dopa.randomutilities.core.filter.network.FilterSettingPayload;
import com.dopa.randomutilities.core.filter.network.GhostFilterPayload;
import com.dopa.randomutilities.machine.fishnet.network.FishnetApproachPayload;
import com.dopa.randomutilities.machine.fishnet.network.FishnetSettingPayload;
import com.dopa.randomutilities.core.gui.machine.network.VolumeMachineSettingPayload;
import com.dopa.randomutilities.logistics.collector.network.ItemCollectorSettingPayload;
import com.dopa.randomutilities.core.machine.network.MachineSettingPayload;
import com.dopa.randomutilities.item.magnet.network.MagnetSettingPayload;
import com.dopa.randomutilities.block.redstoneclock.network.RedstoneClockSettingPayload;
import com.dopa.randomutilities.block.trashcan.network.TrashCanSettingPayload;
import com.dopa.randomutilities.logistics.transfer.network.TransferNodeSettingPayload;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetwork {
    private ModNetwork() {}

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(FilterSettingPayload.TYPE, FilterSettingPayload.STREAM_CODEC, FilterSettingPayload::handle);
        registrar.playToServer(FilterSelectPayload.TYPE, FilterSelectPayload.STREAM_CODEC, FilterSelectPayload::handle);
        registrar.playToServer(GhostFilterPayload.TYPE, GhostFilterPayload.STREAM_CODEC, GhostFilterPayload::handle);
        registrar.playToServer(
                VolumeMachineSettingPayload.TYPE,
                VolumeMachineSettingPayload.STREAM_CODEC,
                VolumeMachineSettingPayload::handle
        );
        registrar.playToServer(MachineSettingPayload.TYPE, MachineSettingPayload.STREAM_CODEC, MachineSettingPayload::handle);
        registrar.playToServer(
                ItemCollectorSettingPayload.TYPE,
                ItemCollectorSettingPayload.STREAM_CODEC,
                ItemCollectorSettingPayload::handle
        );
        registrar.playToServer(MagnetSettingPayload.TYPE, MagnetSettingPayload.STREAM_CODEC, MagnetSettingPayload::handle);
        registrar.playToServer(
                TransferNodeSettingPayload.TYPE,
                TransferNodeSettingPayload.STREAM_CODEC,
                TransferNodeSettingPayload::handle
        );
        registrar.playToServer(TrashCanSettingPayload.TYPE, TrashCanSettingPayload.STREAM_CODEC, TrashCanSettingPayload::handle);
        registrar.playToServer(
                RedstoneClockSettingPayload.TYPE,
                RedstoneClockSettingPayload.STREAM_CODEC,
                RedstoneClockSettingPayload::handle
        );
        registrar.playToServer(FishnetSettingPayload.TYPE, FishnetSettingPayload.STREAM_CODEC, FishnetSettingPayload::handle);
        registrar.playToClient(
                FishnetApproachPayload.TYPE,
                FishnetApproachPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> FishnetApproachPayload.clientHandler.accept(payload))
        );
    }
}
