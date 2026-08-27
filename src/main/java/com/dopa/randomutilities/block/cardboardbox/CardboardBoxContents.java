package com.dopa.randomutilities.block.cardboardbox;

import com.dopa.randomutilities.registry.ModDataComponents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public record CardboardBoxContents(CompoundTag blockStateTag, Optional<CompoundTag> blockEntityTag, Direction captureFacing) {
    private static final CustomModelData FILLED_MODEL_MARKER =
            new CustomModelData(List.of(), List.of(true), List.of(), List.of());

    public CardboardBoxContents {
        captureFacing = horizontalFacing(captureFacing);
    }

    public static final Codec<CardboardBoxContents> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CompoundTag.CODEC.fieldOf("block_state").forGetter(CardboardBoxContents::blockStateTag),
            CompoundTag.CODEC.optionalFieldOf("block_entity").forGetter(CardboardBoxContents::blockEntityTag),
            Direction.CODEC.optionalFieldOf("capture_facing", Direction.NORTH).forGetter(CardboardBoxContents::captureFacing)
    ).apply(instance, CardboardBoxContents::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CardboardBoxContents> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.COMPOUND_TAG,
            CardboardBoxContents::blockStateTag,
            ByteBufCodecs.optional(ByteBufCodecs.COMPOUND_TAG),
            CardboardBoxContents::blockEntityTag,
            Direction.STREAM_CODEC,
            CardboardBoxContents::captureFacing,
            CardboardBoxContents::new
    );

    public boolean hasData() {
        return !blockStateTag.isEmpty();
    }

    public @Nullable BlockState blockState(HolderLookup.Provider registries) {
        if (!hasData()) {
            return null;
        }
        return NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK), blockStateTag);
    }

    public static boolean has(ItemStack stack) {
        return !stack.isEmpty()
                && stack.getItem() instanceof CardboardBoxItem
                && stack.has(ModDataComponents.CARDBOARD_BOX_CONTENTS.get());
    }

    public static @Nullable CardboardBoxContents get(ItemStack stack) {
        if (!has(stack)) {
            return null;
        }
        return stack.get(ModDataComponents.CARDBOARD_BOX_CONTENTS.get());
    }

    public static void set(ItemStack stack, CardboardBoxContents contents) {
        if (stack.isEmpty() || !(stack.getItem() instanceof CardboardBoxItem)) {
            return;
        }
        stack.set(ModDataComponents.CARDBOARD_BOX_CONTENTS.get(), contents);
        stack.set(DataComponents.CUSTOM_MODEL_DATA, FILLED_MODEL_MARKER);
    }

    public static void clear(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof CardboardBoxItem)) {
            return;
        }
        stack.remove(ModDataComponents.CARDBOARD_BOX_CONTENTS.get());
        stack.remove(DataComponents.CUSTOM_MODEL_DATA);
    }

    public static CardboardBoxContents fromBlockState(
            HolderLookup.Provider registries,
            BlockState state,
            @Nullable CompoundTag blockEntityTag,
            Direction captureFacing
    ) {
        return new CardboardBoxContents(
                NbtUtils.writeBlockState(state),
                Optional.ofNullable(blockEntityTag),
                captureFacing
        );
    }

    public BlockState blockStateForUnwrap(HolderLookup.Provider registries, Direction currentBoxFacing) {
        BlockState state = blockState(registries);
        if (state == null) {
            return null;
        }
        Rotation rotation = between(captureFacing, horizontalFacing(currentBoxFacing));
        return rotation == Rotation.NONE ? state : state.rotate(rotation);
    }

    static Direction horizontalFacing(Direction direction) {
        return direction.getAxis().isHorizontal() ? direction : Direction.NORTH;
    }

    private static Rotation between(Direction from, Direction to) {
        int steps = (to.get2DDataValue() - from.get2DDataValue() + 4) & 3;
        return switch (steps) {
            case 1 -> Rotation.CLOCKWISE_90;
            case 2 -> Rotation.CLOCKWISE_180;
            case 3 -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }
}
