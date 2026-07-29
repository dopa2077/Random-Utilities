package com.dopa.randomutilities.client;

import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import org.jspecify.annotations.Nullable;

public class ResourceGeneratorRenderState extends BlockEntityRenderState {
    public @Nullable MovingBlockRenderState insideBlock;
}
