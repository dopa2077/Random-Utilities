package com.dopa.randomutilities.lasso;

import com.dopa.randomutilities.registry.ModDataComponents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public record LassoCapture(Identifier entityId, CompoundTag entityData, float health, float maxHealth) {
    private static final CustomModelData CAPTURED_MODEL_MARKER =
            new CustomModelData(List.of(), List.of(true), List.of(), List.of());

    public static final Codec<LassoCapture> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("entity_id").forGetter(LassoCapture::entityId),
            CompoundTag.CODEC.fieldOf("entity_data").forGetter(LassoCapture::entityData),
            Codec.FLOAT.fieldOf("health").forGetter(LassoCapture::health),
            Codec.FLOAT.fieldOf("max_health").forGetter(LassoCapture::maxHealth)
    ).apply(instance, LassoCapture::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, LassoCapture> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC,
            LassoCapture::entityId,
            ByteBufCodecs.COMPOUND_TAG,
            LassoCapture::entityData,
            ByteBufCodecs.FLOAT,
            LassoCapture::health,
            ByteBufCodecs.FLOAT,
            LassoCapture::maxHealth,
            LassoCapture::new
    );

    public static boolean has(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof LassoItem && stack.has(ModDataComponents.LASSO_CAPTURE.get());
    }

    public static @Nullable LassoCapture get(ItemStack stack) {
        if (!has(stack)) {
            return null;
        }
        return stack.get(ModDataComponents.LASSO_CAPTURE.get());
    }

    public static void set(ItemStack stack, LassoCapture capture) {
        if (stack.isEmpty() || !(stack.getItem() instanceof LassoItem)) {
            return;
        }
        stack.set(ModDataComponents.LASSO_CAPTURE.get(), capture);
        stack.set(DataComponents.CUSTOM_MODEL_DATA, CAPTURED_MODEL_MARKER);
    }

    public static void clear(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof LassoItem)) {
            return;
        }
        stack.remove(ModDataComponents.LASSO_CAPTURE.get());
        stack.remove(DataComponents.CUSTOM_MODEL_DATA);
    }

    public @Nullable EntityType<?> entityType() {
        return BuiltInRegistries.ENTITY_TYPE.getValue(entityId);
    }

    public Component mobName() {
        EntityType<?> type = entityType();
        Component fallback = type != null ? type.getDescription() : Component.literal(entityId.toString());
        if (!entityData.contains("CustomName")) {
            return fallback;
        }
        Optional<String> customName = entityData.getString("CustomName");
        if (customName.isPresent() && !customName.get().isEmpty()) {
            return Component.literal(customName.get());
        }
        return fallback;
    }
}
