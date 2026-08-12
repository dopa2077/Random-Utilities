package com.dopa.randomutilities.fishnet.client;

import com.dopa.randomutilities.fishnet.FishnetBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class FishnetRenderer implements BlockEntityRenderer<FishnetBlockEntity, FishnetRenderer.State> {
    private final EntityRenderDispatcher entityRenderer;

    public FishnetRenderer(BlockEntityRendererProvider.Context context) {
        this.entityRenderer = context.entityRenderer();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public AABB getRenderBoundingBox(FishnetBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(4.0);
    }

    @Override
    public void extractRenderState(
            FishnetBlockEntity blockEntity,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        state.fishState = null;
        state.fishOffset = Vec3.ZERO;

        FishnetCatchEffects.ActiveCatch active = FishnetCatchEffects.activeAt(blockEntity.getBlockPos());
        if (active == null) {
            return;
        }
        AbstractFish fish = active.fish();
        Vec3 pos = active.position(partialTicks);
        fish.setPos(pos.x, pos.y, pos.z);

        EntityRenderState fishState = entityRenderer.extractEntity(fish, partialTicks);
        fishState.lightCoords = state.lightCoords;
        state.fishState = fishState;
        state.fishOffset = pos.subtract(Vec3.atLowerCornerOf(blockEntity.getBlockPos()));
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.fishState == null) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(state.fishOffset.x, state.fishOffset.y, state.fishOffset.z);
        entityRenderer.submit(state.fishState, camera, 0.0, 0.0, 0.0, poseStack, collector);
        poseStack.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        @Nullable
        public EntityRenderState fishState;
        public Vec3 fishOffset = Vec3.ZERO;
    }
}
