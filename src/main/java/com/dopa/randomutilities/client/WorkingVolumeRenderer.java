package com.dopa.randomutilities.client;

import com.dopa.randomutilities.dOPasRandomUtilities;
import com.dopa.randomutilities.util.WorkingVolumeSource;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

import java.util.Iterator;
import java.util.Set;

@EventBusSubscriber(modid = dOPasRandomUtilities.MOD_ID, value = Dist.CLIENT)
public final class WorkingVolumeRenderer {
    private static final int FACE_ALPHA = 0x55;
    private static final float Z_FIGHT_EPS = 0.0025F;

    private WorkingVolumeRenderer() {}

    @SubscribeEvent
    public static void onSubmitCustomGeometry(SubmitCustomGeometryEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null) {
            return;
        }

        Set<BlockPos> enabled = WorkingVolumeOverlay.enabledPositions(level.dimension());
        if (enabled.isEmpty()) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        Iterator<BlockPos> it = enabled.iterator();
        while (it.hasNext()) {
            BlockPos pos = it.next();
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof WorkingVolumeSource source)) {
                it.remove();
                continue;
            }

            AABB worldBox = source.workingVolume().overlayBox(pos);
            float minX = (float) (worldBox.minX - pos.getX()) - Z_FIGHT_EPS;
            float minY = (float) (worldBox.minY - pos.getY()) - Z_FIGHT_EPS;
            float minZ = (float) (worldBox.minZ - pos.getZ()) - Z_FIGHT_EPS;
            float maxX = (float) (worldBox.maxX - pos.getX()) + Z_FIGHT_EPS;
            float maxY = (float) (worldBox.maxY - pos.getY()) + Z_FIGHT_EPS;
            float maxZ = (float) (worldBox.maxZ - pos.getZ()) + Z_FIGHT_EPS;
            int color = ARGB.color(FACE_ALPHA, source.overlayColor());

            poseStack.pushPose();
            Vec3 offset = Vec3.atLowerCornerOf(pos).subtract(camera);
            poseStack.translate(offset.x, offset.y, offset.z);
            event.getSubmitNodeCollector().submitCustomGeometry(
                    poseStack,
                    RenderTypes.debugQuads(),
                    (pose, buffer) -> drawHollowCube(pose, buffer, minX, minY, minZ, maxX, maxY, maxZ, color)
            );
            poseStack.popPose();
        }
        WorkingVolumeOverlay.dropIfEmpty(level.dimension());
    }

    private static void drawHollowCube(
            PoseStack.Pose pose,
            VertexConsumer buffer,
            float minX,
            float minY,
            float minZ,
            float maxX,
            float maxY,
            float maxZ,
            int color
    ) {
        quad(pose, buffer, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, color);
        quad(pose, buffer, maxX, minY, maxZ, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, color);
        quad(pose, buffer, minX, minY, maxZ, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, color);
        quad(pose, buffer, minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, color);
        quad(pose, buffer, maxX, minY, minZ, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, color);
        quad(pose, buffer, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, color);
    }

    private static void quad(
            PoseStack.Pose pose,
            VertexConsumer buffer,
            float x0, float y0, float z0,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            int color
    ) {
        buffer.addVertex(pose, x0, y0, z0).setColor(color);
        buffer.addVertex(pose, x1, y1, z1).setColor(color);
        buffer.addVertex(pose, x2, y2, z2).setColor(color);
        buffer.addVertex(pose, x3, y3, z3).setColor(color);
    }
}
