package com.dopa.randomutilities.registry;

import com.dopa.randomutilities.dOPasRandomUtilities;
import com.dopa.randomutilities.filter.FilterContents;
import com.dopa.randomutilities.lasso.LassoCapture;
import com.dopa.randomutilities.cardboardbox.CardboardBoxContents;
import com.dopa.randomutilities.magnet.MagnetContents;
import com.dopa.randomutilities.filter.TransferFilterContents;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, dOPasRandomUtilities.MOD_ID);

    /** Persistent id kept for world-save compatibility with earlier builds. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<FilterContents>> FILTER_CONTENTS =
            DATA_COMPONENTS.registerComponentType(
                    "dev_null_contents",
                    builder -> builder
                            .persistent(FilterContents.CODEC)
                            .networkSynchronized(FilterContents.STREAM_CODEC)
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<TransferFilterContents>> TRANSFER_FILTER =
            DATA_COMPONENTS.registerComponentType(
                    "transfer_filter_contents",
                    builder -> builder
                            .persistent(TransferFilterContents.CODEC)
                            .networkSynchronized(TransferFilterContents.STREAM_CODEC)
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<LassoCapture>> LASSO_CAPTURE =
            DATA_COMPONENTS.registerComponentType(
                    "lasso_capture",
                    builder -> builder
                            .persistent(LassoCapture.CODEC)
                            .networkSynchronized(LassoCapture.STREAM_CODEC)
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<MagnetContents>> MAGNET_CONTENTS =
            DATA_COMPONENTS.registerComponentType(
                    "magnet_contents",
                    builder -> builder
                            .persistent(MagnetContents.CODEC)
                            .networkSynchronized(MagnetContents.STREAM_CODEC)
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CardboardBoxContents>> CARDBOARD_BOX_CONTENTS =
            DATA_COMPONENTS.registerComponentType(
                    "cardboard_box_contents",
                    builder -> builder
                            .persistent(CardboardBoxContents.CODEC)
                            .networkSynchronized(CardboardBoxContents.STREAM_CODEC)
            );

    private ModDataComponents() {}
}
