package com.dopa.randomutilities.itemcollector.client;

import com.dopa.randomutilities.client.HollowAabbRenderer;
import com.dopa.randomutilities.dOPasRandomUtilities;
import com.dopa.randomutilities.itemcollector.ItemCollectorBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
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
public final class ItemCollectorRangeRenderer {
    private static final int FACE_ALPHA = 0x55;

    private ItemCollectorRangeRenderer() {}

    @SubscribeEvent
    public static void onSubmitCustomGeometry(SubmitCustomGeometryEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null) {
            return;
        }

        Set<BlockPos> enabled = ItemCollectorClientOverlay.enabledPositions(level.dimension());
        if (enabled.isEmpty()) {
            return;
        }

        ItemCollectorClientOverlay.pruneRemoved(level, level.dimension());
        enabled = ItemCollectorClientOverlay.enabledPositions(level.dimension());
        if (enabled.isEmpty()) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        Iterator<BlockPos> it = enabled.iterator();
        while (it.hasNext()) {
            BlockPos pos = it.next();
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof ItemCollectorBlockEntity collector)) {
                continue;
            }

            AABB worldBox = collector.scanBox();
            HollowAabbRenderer.submit(
                    poseStack,
                    event.getSubmitNodeCollector(),
                    camera,
                    pos,
                    worldBox,
                    ARGB.color(FACE_ALPHA, collector.overlayColor())
            );
        }
        ItemCollectorClientOverlay.dropIfEmpty(level.dimension());
    }
}
