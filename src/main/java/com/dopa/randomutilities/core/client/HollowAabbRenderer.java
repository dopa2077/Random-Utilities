package com.dopa.randomutilities.core.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Shared hollow AABB overlay draw used by working-volume and collector range renderers. */
public final class HollowAabbRenderer {
    private static final float Z_FIGHT_EPS = 0.0025F;

    private HollowAabbRenderer() {}

    public static void submit(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            Vec3 camera,
            BlockPos anchor,
            AABB worldBox,
            int color
    ) {
        float minX = (float) (worldBox.minX - anchor.getX()) - Z_FIGHT_EPS;
        float minY = (float) (worldBox.minY - anchor.getY()) - Z_FIGHT_EPS;
        float minZ = (float) (worldBox.minZ - anchor.getZ()) - Z_FIGHT_EPS;
        float maxX = (float) (worldBox.maxX - anchor.getX()) + Z_FIGHT_EPS;
        float maxY = (float) (worldBox.maxY - anchor.getY()) + Z_FIGHT_EPS;
        float maxZ = (float) (worldBox.maxZ - anchor.getZ()) + Z_FIGHT_EPS;

        poseStack.pushPose();
        Vec3 offset = Vec3.atLowerCornerOf(anchor).subtract(camera);
        poseStack.translate(offset.x, offset.y, offset.z);
        collector.submitCustomGeometry(
                poseStack,
                RenderTypes.debugQuads(),
                (pose, buffer) -> drawHollowCube(pose, buffer, minX, minY, minZ, maxX, maxY, maxZ, color)
        );
        poseStack.popPose();
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
