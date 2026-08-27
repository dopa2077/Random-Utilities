package com.dopa.randomutilities.block.cardboardbox;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.function.Consumer;

public final class CardboardBoxTooltip {
    private static final int VALUE_GRAY = 0xAAAAAA;
    private static final int YES_GREEN = 0x55FF55;

    private CardboardBoxTooltip() {}

    public static void append(
            ItemStack stack,
            net.minecraft.core.HolderLookup.Provider registries,
            Consumer<Component> tooltip
    ) {
        CardboardBoxContents contents = CardboardBoxContents.get(stack);
        boolean hasData = contents != null && contents.hasData();
        tooltip.accept(labelValue(
                "item.dopasrandomutilities.cardboard_box.block_data",
                hasData
                        ? Component.translatable("item.dopasrandomutilities.cardboard_box.yes").withColor(YES_GREEN)
                        : Component.translatable("item.dopasrandomutilities.cardboard_box.no").withStyle(ChatFormatting.GRAY)
        ));
        if (!hasData) {
            return;
        }
        BlockState state = contents.blockState(registries);
        if (state != null) {
            tooltip.accept(labelValue(
                    "item.dopasrandomutilities.cardboard_box.block",
                    state.getBlock().getName().withColor(VALUE_GRAY)
            ));
        }
        contents.blockEntityTag().ifPresent(tag -> {
            Identifier beId = blockEntityId(tag);
            if (beId != null) {
                tooltip.accept(labelValue(
                        "item.dopasrandomutilities.cardboard_box.block_entity",
                        Component.literal(beId.toString()).withColor(VALUE_GRAY)
                ));
            }
            spawnerEntityName(tag).ifPresent(name -> tooltip.accept(labelValue(
                    "item.dopasrandomutilities.cardboard_box.spawns",
                    Component.literal(name).withColor(VALUE_GRAY)
            )));
        });
    }

    private static Component labelValue(String labelKey, Component value) {
        return Component.translatable(labelKey, value)
                .withStyle(style -> style.withColor(ChatFormatting.AQUA));
    }

    private static @Nullable Identifier blockEntityId(CompoundTag tag) {
        if (tag.contains("id")) {
            return tag.read("id", Identifier.CODEC).orElse(null);
        }
        return null;
    }

    private static Optional<String> spawnerEntityName(net.minecraft.nbt.CompoundTag tag) {
        Optional<String> entityId = readSpawnEntityId(tag);
        if (entityId.isEmpty()) {
            return Optional.empty();
        }
        Identifier id = Identifier.tryParse(entityId.get());
        if (id == null) {
            return Optional.of(entityId.get());
        }
        var type = BuiltInRegistries.ENTITY_TYPE.getValue(id);
        if (type == null) {
            return Optional.of(id.toString());
        }
        return Optional.of(type.getDescription().getString());
    }

    private static Optional<String> readSpawnEntityId(net.minecraft.nbt.CompoundTag tag) {
        if (tag.contains("SpawnData")) {
            var spawnData = tag.getCompound("SpawnData").orElse(null);
            if (spawnData != null) {
                if (spawnData.contains("entity")) {
                    var entity = spawnData.getCompound("entity").orElse(null);
                    if (entity != null && entity.contains("id")) {
                        return entity.read("id", Identifier.CODEC).map(Identifier::toString);
                    }
                }
                if (spawnData.contains("id")) {
                    return spawnData.read("id", Identifier.CODEC).map(Identifier::toString);
                }
            }
        }
        if (tag.contains("EntityId")) {
            return tag.getString("EntityId");
        }
        return Optional.empty();
    }
}
