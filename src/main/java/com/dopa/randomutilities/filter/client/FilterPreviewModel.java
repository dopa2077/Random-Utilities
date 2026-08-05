package com.dopa.randomutilities.filter.client;

import com.dopa.randomutilities.filter.FilterStorage;
import com.mojang.serialization.MapCodec;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Field;

public final class FilterPreviewModel implements ItemModel {
    private static final FilterPreviewModel INSTANCE = new FilterPreviewModel();
    private static final float PREVIEW_SCALE = 0.35F;

    private FilterPreviewModel() {}

    @Override
    public void update(
            ItemStackRenderState output,
            ItemStack item,
            ItemModelResolver resolver,
            ItemDisplayContext displayContext,
            @Nullable ClientLevel level,
            @Nullable ItemOwner owner,
            int seed
    ) {
        ItemStack preview = FilterStorage.getPreviewIconStack(item);
        if (preview.isEmpty() || Layers.getActiveLayerCount(output) == 0) {
            return;
        }
        ItemTransform frameTransform = Layers.getItemTransform(Layers.getLayer(output, 0));
        output.appendModelIdentityElement(this);
        output.appendModelIdentityElement(preview.getItem());
        output.appendModelIdentityElement(displayContext);
        int layerStart = Layers.getActiveLayerCount(output);
        resolver.appendItemLayers(output, preview, ItemDisplayContext.GUI, level, owner, seed);
        Layers.applyPreviewLayerTransforms(output, layerStart, frameTransform, previewLocalTransform(displayContext));
    }

    private static Matrix4fc previewLocalTransform(ItemDisplayContext displayContext) {
        Matrix4f orientation = new Matrix4f().translate(0.5F, 0.5F, 0.5F);
        if (displayContext.firstPerson()) {
            float yRotation = displayContext.leftHand() ? (float) (-Math.PI / 2.0) : (float) (Math.PI / 2.0);
            orientation.rotateY(yRotation);
        } else {
            orientation.rotateX((float) (-Math.PI / 2.0));
        }
        orientation.translate(-0.5F, -0.5F, -0.5F);
        return orientation.mul(new Matrix4f()
                .translate(0.5F, 0.5F, 0.5F)
                .scale(PREVIEW_SCALE)
                .translate(-0.5F, -0.5F, -0.5F));
    }

    public record Unbaked() implements ItemModel.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
            return INSTANCE;
        }

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {}
    }

    private static final class Layers {
        private static final Field ACTIVE_LAYER_COUNT;
        private static final Field LAYERS;
        private static final Field ITEM_TRANSFORM;

        static {
            try {
                ACTIVE_LAYER_COUNT = ItemStackRenderState.class.getDeclaredField("activeLayerCount");
                ACTIVE_LAYER_COUNT.setAccessible(true);
                LAYERS = ItemStackRenderState.class.getDeclaredField("layers");
                LAYERS.setAccessible(true);
                ITEM_TRANSFORM = ItemStackRenderState.LayerRenderState.class.getDeclaredField("itemTransform");
                ITEM_TRANSFORM.setAccessible(true);
            } catch (NoSuchFieldException exception) {
                throw new ExceptionInInitializerError(exception);
            }
        }

        static int getActiveLayerCount(ItemStackRenderState state) {
            try {
                return ACTIVE_LAYER_COUNT.getInt(state);
            } catch (IllegalAccessException exception) {
                throw new IllegalStateException(exception);
            }
        }

        static ItemStackRenderState.LayerRenderState getLayer(ItemStackRenderState state, int index) {
            try {
                return ((ItemStackRenderState.LayerRenderState[]) LAYERS.get(state))[index];
            } catch (IllegalAccessException exception) {
                throw new IllegalStateException(exception);
            }
        }

        static ItemTransform getItemTransform(ItemStackRenderState.LayerRenderState layer) {
            try {
                return (ItemTransform) ITEM_TRANSFORM.get(layer);
            } catch (IllegalAccessException exception) {
                throw new IllegalStateException(exception);
            }
        }

        static void applyPreviewLayerTransforms(
                ItemStackRenderState state,
                int fromIndex,
                ItemTransform frameTransform,
                Matrix4fc localTransform
        ) {
            try {
                ItemStackRenderState.LayerRenderState[] layers =
                        (ItemStackRenderState.LayerRenderState[]) LAYERS.get(state);
                int count = ACTIVE_LAYER_COUNT.getInt(state);
                for (int index = fromIndex; index < count; index++) {
                    ItemStackRenderState.LayerRenderState layer = layers[index];
                    layer.setItemTransform(frameTransform);
                    layer.setLocalTransform(localTransform);
                }
            } catch (IllegalAccessException exception) {
                throw new IllegalStateException(exception);
            }
        }
    }
}
