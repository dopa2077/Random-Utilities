package com.dopa.randomutilities.client;

import com.dopa.randomutilities.blockentity.ResourceGeneratorBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class ResourceGeneratorRenderer
        implements BlockEntityRenderer<ResourceGeneratorBlockEntity, ResourceGeneratorRenderer.State> {
    private static final float INSIDE_MIN = 5.0F / 16.0F;
    private static final float INSIDE_Y = 2.0F / 16.0F;
    private static final float INSIDE_SCALE = 6.0F / 16.0F;

    public ResourceGeneratorRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            ResourceGeneratorBlockEntity blockEntity,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        state.insideBlock = null;
        if (!(blockEntity.getLevel() instanceof ClientLevel level)) {
            return;
        }

        Block display = blockEntity.getDisplayResultBlock().orElseGet(blockEntity::defaultDisplayBlock);
        BlockState blockState = display.defaultBlockState();
        if (blockState.getRenderShape() != RenderShape.MODEL) {
            blockState = blockEntity.defaultDisplayBlock().defaultBlockState();
            if (blockState.getRenderShape() != RenderShape.MODEL) {
                return;
            }
        }

        BlockPos pos = blockEntity.getBlockPos();
        state.insideBlock = createMovingBlock(pos, blockState, level.getBiome(pos), level);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        if (state.insideBlock == null) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(INSIDE_MIN, INSIDE_Y, INSIDE_MIN);
        poseStack.scale(INSIDE_SCALE, INSIDE_SCALE, INSIDE_SCALE);
        submitNodeCollector.submitMovingBlock(poseStack, state.insideBlock, 0);
        poseStack.popPose();
    }

    private static MovingBlockRenderState createMovingBlock(
            BlockPos pos,
            BlockState blockState,
            Holder<Biome> biome,
            ClientLevel level
    ) {
        MovingBlockRenderState moving = new MovingBlockRenderState();
        moving.randomSeedPos = pos;
        moving.blockPos = pos;
        moving.blockState = blockState;
        moving.biome = biome;
        moving.cardinalLighting = level.cardinalLighting();
        moving.lightEngine = level.getLightEngine();
        return moving;
    }

    public static final class State extends BlockEntityRenderState {
        public @Nullable MovingBlockRenderState insideBlock;
    }
}
