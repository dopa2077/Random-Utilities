package com.dopa.randomutilities.filtersystem;

import java.util.ArrayList;
import java.util.List;

import com.dopa.randomutilities.config.DevNullConfig;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;

public record FilterContents(
        List<Slot> slots,
        int maxStackSize,
        int selectedSlot,
        int page,
        int color,
        boolean highlightMatchColor
) {
    public static final int SLOTS_PER_PAGE = 54;
    public static final int SLOTS_PER_ROW = 9;
    public static final int MIN_ADVANCED_SLOTS = 9;
    public static final int MAX_TOTAL_SLOTS = SLOTS_PER_PAGE * 64;
    public static final int DEFAULT_COLOR = 0xFFFFFF;
    /** GUI selected-slot highlight when {@link #highlightMatchColor} is false. */
    public static final int DEFAULT_HIGHLIGHT_COLOR = 0x555555;

    public static FilterContents basicDefault() {
        return new FilterContents(List.of(Slot.EMPTY), 64, 0, 0, DEFAULT_COLOR, false);
    }

    public static FilterContents advancedDefault(int minSlots) {
        return new FilterContents(emptySlots(minSlots), 64, 0, 0, DEFAULT_COLOR, false);
    }

    public static final Codec<FilterContents> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Slot.CODEC.listOf().fieldOf("slots").forGetter(FilterContents::slots),
            Codec.INT.fieldOf("max_stack_size").forGetter(FilterContents::maxStackSize),
            Codec.INT.fieldOf("selected_slot").forGetter(FilterContents::selectedSlot),
            Codec.INT.fieldOf("page").forGetter(FilterContents::page),
            Codec.INT.fieldOf("color").forGetter(FilterContents::color),
            Codec.BOOL.optionalFieldOf("highlight_match", false).forGetter(FilterContents::highlightMatchColor)
    ).apply(instance, FilterContents::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, FilterContents> STREAM_CODEC = StreamCodec.composite(
            Slot.STREAM_CODEC.apply(ByteBufCodecs.list()),
            FilterContents::slots,
            ByteBufCodecs.VAR_INT,
            FilterContents::maxStackSize,
            ByteBufCodecs.VAR_INT,
            FilterContents::selectedSlot,
            ByteBufCodecs.VAR_INT,
            FilterContents::page,
            ByteBufCodecs.INT,
            FilterContents::color,
            ByteBufCodecs.BOOL,
            FilterContents::highlightMatchColor,
            FilterContents::new
    );

    public FilterContents {
        slots = List.copyOf(slots);
        maxStackSize = Math.max(1, maxStackSize);
        selectedSlot = slots.isEmpty() ? 0 : Math.floorMod(selectedSlot, slots.size());
        page = Math.max(0, page);
        color = color & 0xFFFFFF;
    }

    private static List<Slot> emptySlots(int count) {
        List<Slot> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(Slot.EMPTY);
        }
        return list;
    }

    public int slotCount() {
        return slots.size();
    }

    public int pageCount() {
        return Math.max(1, (slotCount() + SLOTS_PER_PAGE - 1) / SLOTS_PER_PAGE);
    }

    public int clampedPage() {
        return Math.min(page, pageCount() - 1);
    }

    public static int rowsForSlotCount(int slotsOnPage) {
        return Math.max(1, (Math.max(1, slotsOnPage) + 8) / 9);
    }

    public Slot slot(int index) {
        return index < 0 || index >= slots.size() ? Slot.EMPTY : slots.get(index);
    }

    public ItemStack stackInSlot(int index) {
        return slot(index).toStack();
    }

    public FilterContents withSlot(int index, ItemResource resource, int count) {
        List<Slot> next = new ArrayList<>(slots);
        while (next.size() <= index) {
            next.add(Slot.EMPTY);
        }
        int capped = Math.max(0, count);
        next.set(index, Slot.of(resource, capped));
        return new FilterContents(next, maxStackSize, selectedSlot, page, color, highlightMatchColor);
    }

    public FilterContents withSlotStack(int index, ItemStack stack) {
        return stack.isEmpty()
                ? withSlot(index, ItemResource.EMPTY, 0)
                : withSlot(index, ItemResource.of(stack), stack.getCount());
    }

    public FilterContents withMaxStackSize(int value) {
        return new FilterContents(slots, Math.max(1, value), selectedSlot, page, color, highlightMatchColor);
    }

    public FilterContents withSelectedSlot(int index) {
        return new FilterContents(slots, maxStackSize, index, page, color, highlightMatchColor);
    }

    public FilterContents withPage(int newPage) {
        return new FilterContents(slots, maxStackSize, selectedSlot, newPage, color, highlightMatchColor);
    }

    public FilterContents withColor(int rgb) {
        return new FilterContents(slots, maxStackSize, selectedSlot, page, rgb, highlightMatchColor);
    }

    public FilterContents withHighlightMatchColor(boolean match) {
        return new FilterContents(slots, maxStackSize, selectedSlot, page, color, match);
    }

    public FilterContents withSlotCount(int count, int minSlots, int maxSlots) {
        int target = Math.max(minSlots, Math.min(maxSlots, count));
        List<Slot> next = new ArrayList<>(target);
        for (int i = 0; i < target; i++) {
            next.add(i < slots.size() ? slots.get(i) : Slot.EMPTY);
        }
        int newSelected = Math.min(selectedSlot, target - 1);
        int newPage = Math.min(page, Math.max(0, (target + SLOTS_PER_PAGE - 1) / SLOTS_PER_PAGE - 1));
        return new FilterContents(next, maxStackSize, newSelected, newPage, color, highlightMatchColor);
    }

    public FilterContents ensureMinimum(int minSlots) {
        return slotCount() < minSlots
                ? withSlotCount(minSlots, minSlots, DevNullConfig.advancedMaxSlots())
                : this;
    }

    public record Slot(ItemResource resource, int count) {
        public static final Slot EMPTY = new Slot(ItemResource.EMPTY, 0);

        public static final Codec<Slot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ItemResource.OPTIONAL_CODEC.fieldOf("resource").forGetter(Slot::resource),
                Codec.INT.fieldOf("count").forGetter(Slot::count)
        ).apply(instance, Slot::of));

        public static final StreamCodec<RegistryFriendlyByteBuf, Slot> STREAM_CODEC = StreamCodec.composite(
                ItemResource.STREAM_CODEC,
                Slot::resource,
                ByteBufCodecs.VAR_INT,
                Slot::count,
                Slot::of
        );

        public static Slot of(ItemResource resource, int count) {
            return resource == null || resource.isEmpty() || count <= 0 ? EMPTY : new Slot(resource, count);
        }

        public boolean isEmpty() {
            return resource.isEmpty() || count <= 0;
        }

        public ItemStack toStack() {
            return isEmpty() ? ItemStack.EMPTY : resource.toStack(count);
        }

        public boolean matches(ItemStack stack) {
            return !isEmpty() && !stack.isEmpty() && resource.matches(stack);
        }
    }
}
