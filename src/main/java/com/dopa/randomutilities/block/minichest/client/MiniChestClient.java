package com.dopa.randomutilities.block.minichest.client;

import com.dopa.randomutilities.dOPasRandomUtilities;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;

import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;
import org.joml.Vector3fc;

import java.util.function.Consumer;

@EventBusSubscriber(modid = dOPasRandomUtilities.MOD_ID, value = Dist.CLIENT)
public final class MiniChestClient {
    private MiniChestClient() {}

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(MiniChestModel.LAYER_LOCATION, MiniChestModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerSpecialRenderers(RegisterSpecialModelRendererEvent event) {
        event.register(
                Identifier.fromNamespaceAndPath(dOPasRandomUtilities.MOD_ID, "mini_chest"),
                MiniChestSpecialRenderer.Unbaked.MAP_CODEC
        );
    }

    public static final class MiniChestSpecialRenderer implements NoDataSpecialModelRenderer {
        private final MiniChestModel model;

        public MiniChestSpecialRenderer(MiniChestModel model) {
            this.model = model;
        }

        @Override
        public void submit(
                PoseStack poseStack,
                SubmitNodeCollector submitNodeCollector,
                int lightCoords,
                int overlayCoords,
                boolean hasFoil,
                int outlineColor
        ) {
            submitNodeCollector.submitModel(
                    this.model,
                    0.0F,
                    poseStack,
                    RenderTypes.entityCutout(MiniChestRenderer.TEXTURE),
                    lightCoords,
                    overlayCoords,
                    -1,
                    null,
                    outlineColor,
                    null
            );
        }

        @Override
        public void getExtents(Consumer<Vector3fc> output) {
            PoseStack poseStack = new PoseStack();
            this.model.setupAnim(0.0F);
            this.model.root().getExtentsForGui(poseStack, output);
        }

        public record Unbaked() implements NoDataSpecialModelRenderer.Unbaked {
            public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

            @Override
            public MapCodec<Unbaked> type() {
                return MAP_CODEC;
            }

            @Override
            public MiniChestSpecialRenderer bake(SpecialModelRenderer.BakingContext context) {
                EntityModelSet models = context.entityModelSet();
                return new MiniChestSpecialRenderer(new MiniChestModel(models.bakeLayer(MiniChestModel.LAYER_LOCATION)));
            }
        }
    }
}
