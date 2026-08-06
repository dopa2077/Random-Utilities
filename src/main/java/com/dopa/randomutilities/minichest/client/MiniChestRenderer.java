package com.dopa.randomutilities.minichest.client;

import com.dopa.randomutilities.minichest.MiniChestBlock;
import com.dopa.randomutilities.minichest.MiniChestBlockEntity;
import com.dopa.randomutilities.dOPasRandomUtilities;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class MiniChestRenderer implements BlockEntityRenderer<MiniChestBlockEntity, MiniChestRenderer.State> {
    public static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(dOPasRandomUtilities.MOD_ID, "textures/entity/mini_chest.png");

    private final MiniChestModel model;

    public MiniChestRenderer(BlockEntityRendererProvider.Context context) {
        this.model = new MiniChestModel(context.bakeLayer(MiniChestModel.LAYER_LOCATION));
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            MiniChestBlockEntity blockEntity,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        BlockState blockState = blockEntity.getBlockState();
        state.facing = blockState.hasProperty(MiniChestBlock.FACING)
                ? blockState.getValue(MiniChestBlock.FACING)
                : Direction.SOUTH;
        float open = blockEntity.getOpenNess(partialTicks);
        open = 1.0F - open;
        state.open = 1.0F - open * open * open;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.facing.toYRot()));
        poseStack.translate(-0.5F, 0.0F, -0.5F);
        submitNodeCollector.submitModel(
                this.model,
                state.open,
                poseStack,
                RenderTypes.entityCutout(TEXTURE),
                state.lightCoords,
                OverlayTexture.NO_OVERLAY,
                -1,
                null,
                0,
                state.breakProgress
        );
        poseStack.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public Direction facing = Direction.SOUTH;
        public float open;
    }
}
