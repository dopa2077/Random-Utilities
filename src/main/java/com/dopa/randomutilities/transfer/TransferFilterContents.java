package com.dopa.randomutilities.transfer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import com.dopa.randomutilities.filter.FilterNesting;
import com.dopa.randomutilities.registry.ModDataComponents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public record TransferFilterContents(
        List<ItemStack> slots,
        boolean matchNbt,
        boolean matchMeta,
        boolean matchOreDict
) {
    public static final int SIZE = 16;

    public static final TransferFilterContents EMPTY = new TransferFilterContents(
            emptySlots(),
            false,
            false,
            false
    );

    public static final Codec<TransferFilterContents> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemStack.OPTIONAL_CODEC.listOf().fieldOf("slots").forGetter(TransferFilterContents::slots),
            Codec.BOOL.optionalFieldOf("match_nbt", false).forGetter(TransferFilterContents::matchNbt),
            Codec.BOOL.optionalFieldOf("match_meta", false).forGetter(TransferFilterContents::matchMeta),
            Codec.BOOL.optionalFieldOf("match_ore_dict", false).forGetter(TransferFilterContents::matchOreDict)
    ).apply(instance, TransferFilterContents::create));

    public static final StreamCodec<RegistryFriendlyByteBuf, TransferFilterContents> STREAM_CODEC = StreamCodec.composite(
            ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()),
            TransferFilterContents::slots,
            ByteBufCodecs.BOOL,
            TransferFilterContents::matchNbt,
            ByteBufCodecs.BOOL,
            TransferFilterContents::matchMeta,
            ByteBufCodecs.BOOL,
            TransferFilterContents::matchOreDict,
            TransferFilterContents::create
    );

    public TransferFilterContents {
        slots = normalize(slots);
    }

    public static TransferFilterContents create(
            List<ItemStack> slots,
            boolean matchNbt,
            boolean matchMeta,
            boolean matchOreDict
    ) {
        return new TransferFilterContents(slots, matchNbt, matchMeta, matchOreDict);
    }

    public static TransferFilterContents get(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof TransferFilterItem)) {
            return EMPTY;
        }
        return stack.getOrDefault(ModDataComponents.TRANSFER_FILTER.get(), EMPTY);
    }

    public static void set(ItemStack stack, TransferFilterContents contents) {
        if (stack.isEmpty() || !(stack.getItem() instanceof TransferFilterItem)) {
            return;
        }
        stack.set(ModDataComponents.TRANSFER_FILTER.get(), contents);
    }

    public static boolean isFilter(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof TransferFilterItem;
    }

    public static int nestingDepth(ItemStack stack) {
        if (!isFilter(stack)) {
            return 0;
        }
        int innerMax = 0;
        for (ItemStack slot : get(stack).slots()) {
            if (isFilter(slot)) {
                innerMax = Math.max(innerMax, nestingDepth(slot));
            }
        }
        return 1 + innerMax;
    }

    public static boolean canPlaceIn(ItemStack host, ItemStack candidate) {
        if (candidate.isEmpty()) {
            return false;
        }
        if (!isFilter(candidate)) {
            return true;
        }
        return 1 + nestingDepth(candidate) <= FilterNesting.MAX_DEPTH;
    }

    public static boolean allows(ItemStack candidate, ItemStack filter, int depth) {
        if (candidate.isEmpty() || !isFilter(filter) || depth >= FilterNesting.MAX_DEPTH) {
            return false;
        }
        TransferFilterContents contents = get(filter);
        for (ItemStack slot : contents.slots()) {
            if (slot.isEmpty()) {
                continue;
            }
            if (isFilter(slot)) {
                if (allows(candidate, slot, depth + 1)) {
                    return true;
                }
            } else if (matchesGhost(candidate, slot, contents)) {
                return true;
            }
        }
        return false;
    }

    static boolean matchesGhost(ItemStack candidate, ItemStack ghost, TransferFilterContents flags) {
        if (candidate.isEmpty() || ghost.isEmpty()) {
            return false;
        }
        boolean itemOrTag = candidate.is(ghost.getItem())
                || (flags.matchOreDict() && sharesAnyTag(candidate, ghost));
        if (!itemOrTag) {
            return false;
        }
        if (flags.matchNbt()) {
            return ItemStack.isSameItemSameComponents(candidate, ghost);
        }
        if (flags.matchMeta()) {
            return sameMeta(candidate, ghost);
        }
        return true;
    }

    private static boolean sharesAnyTag(ItemStack candidate, ItemStack ghost) {
        return ghost.getItem().builtInRegistryHolder().tags().anyMatch(candidate::is);
    }

    private static boolean sameMeta(ItemStack candidate, ItemStack ghost) {
        if (candidate.getDamageValue() != ghost.getDamageValue()) {
            return false;
        }
        return Objects.equals(candidate.get(DataComponents.BLOCK_STATE), ghost.get(DataComponents.BLOCK_STATE));
    }

    public ItemStack slot(int index) {
        if (index < 0 || index >= SIZE) {
            return ItemStack.EMPTY;
        }
        return slots.get(index);
    }

    public TransferFilterContents withSlot(int index, ItemStack stack) {
        if (index < 0 || index >= SIZE) {
            return this;
        }
        List<ItemStack> next = new ArrayList<>(slots);
        next.set(index, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
        return new TransferFilterContents(next, matchNbt, matchMeta, matchOreDict);
    }

    public TransferFilterContents withMatchNbt(boolean value) {
        return new TransferFilterContents(slots, value, matchMeta, matchOreDict);
    }

    public TransferFilterContents withMatchMeta(boolean value) {
        return new TransferFilterContents(slots, matchNbt, value, matchOreDict);
    }

    public TransferFilterContents withMatchOreDict(boolean value) {
        return new TransferFilterContents(slots, matchNbt, matchMeta, value);
    }

    public void appendHoverText(Consumer<Component> tooltip) {
        tooltip.accept(flagLine("nbt", matchNbt));
        tooltip.accept(flagLine("meta", matchMeta));
        tooltip.accept(flagLine("ore_dict", matchOreDict));
        appendTree(tooltip, slots, 0);
    }

    private static Component flagLine(String id, boolean match) {
        String key = match
                ? "item.dopasrandomutilities.filter.match." + id
                : "item.dopasrandomutilities.filter.ignore." + id;
        return Component.translatable(key).withStyle(ChatFormatting.GRAY);
    }

    private static void appendTree(Consumer<Component> tooltip, List<ItemStack> entries, int depth) {
        if (depth >= FilterNesting.MAX_DEPTH) {
            return;
        }
        String indent = "    ".repeat(depth);
        int number = 0;
        for (ItemStack slot : entries) {
            if (slot.isEmpty()) {
                continue;
            }
            number++;
            tooltip.accept(Component.literal(indent + number + " -- ").append(slot.getHoverName()));
            if (isFilter(slot)) {
                appendTree(tooltip, get(slot).slots(), depth + 1);
            }
        }
    }

    private static List<ItemStack> emptySlots() {
        List<ItemStack> list = new ArrayList<>(SIZE);
        for (int i = 0; i < SIZE; i++) {
            list.add(ItemStack.EMPTY);
        }
        return List.copyOf(list);
    }

    private static List<ItemStack> normalize(List<ItemStack> slots) {
        List<ItemStack> list = new ArrayList<>(SIZE);
        for (int i = 0; i < SIZE; i++) {
            ItemStack stack = i < slots.size() ? slots.get(i) : ItemStack.EMPTY;
            list.add(stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
        }
        return List.copyOf(list);
    }
}
