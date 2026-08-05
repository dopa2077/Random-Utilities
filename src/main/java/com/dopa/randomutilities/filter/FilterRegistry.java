package com.dopa.randomutilities.filter;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.IdentityHashMap;
import java.util.Map;

/** Maps registered filter items to their profiles. All generic code queries this instead of hard-coded item ids. */
public final class FilterRegistry {
    private static final Map<Item, FilterProfile> BY_ITEM = new IdentityHashMap<>();
    private static Item[] allItems = new Item[0];

    private FilterRegistry() {}

    public static void register(Item item, FilterProfile profile) {
        BY_ITEM.put(item, profile);
        allItems = BY_ITEM.keySet().toArray(Item[]::new);
    }

    public static FilterProfile profile(ItemStack stack) {
        return stack == null || stack.isEmpty() ? null : BY_ITEM.get(stack.getItem());
    }

    public static FilterProfile profile(Item item) {
        return BY_ITEM.get(item);
    }

    public static boolean isFilterItem(ItemStack stack) {
        return profile(stack) != null;
    }

    public static boolean isFilterItem(Item item) {
        return BY_ITEM.containsKey(item);
    }

    public static Item[] allItems() {
        return allItems;
    }
}
