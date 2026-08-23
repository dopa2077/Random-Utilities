package com.dopa.randomutilities.filter.config;

import com.dopa.randomutilities.config.ConfigPack;
import com.dopa.randomutilities.filter.FilterContents;
import com.dopa.randomutilities.filter.FilterProfile;
import com.google.gson.JsonObject;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;

/** Limits for /dev/null items ({@code items/devnull.json}). */
public final class DevNullConfig {
    private static final String RELATIVE = "items/devnull.json";
    private static final String DEFAULT_RESOURCE = "/default/dopas_random_utilities/items/devnull.json";

    private static int basicMaxStackSize = 64;
    private static boolean basicAllowOverstacking = false;
    private static boolean basicCanPlaceBlocks = false;
    private static int advancedMinSlots = 27;
    private static int advancedMaxSlots = 81;
    private static int advancedMaxStackSize = 1000;
    private static int advancedMaxPages = 2;
    private static boolean advancedAllowOverstacking = true;
    private static boolean advancedCanPlaceBlocks = true;

    static {
        loadDefaultsFromJar();
    }

    private DevNullConfig() {}

    public static void load() {
        ConfigPack.loadJson(RELATIVE, DEFAULT_RESOURCE, DevNullConfig::applyJson, DevNullConfig::loadDefaultsFromJar);
    }

    public static void reload() {
        load();
    }

    public static int basicMaxStackSize() {
        return basicMaxStackSize;
    }

    public static int advancedMinSlots() {
        return advancedMinSlots;
    }

    public static int advancedMaxSlots() {
        return advancedMaxSlots;
    }

    public static int advancedMaxStackSize() {
        return advancedMaxStackSize;
    }

    public static int advancedMaxPages() {
        return advancedMaxPages;
    }

    public static boolean basicAllowOverstacking() {
        return basicAllowOverstacking;
    }

    public static boolean advancedAllowOverstacking() {
        return advancedAllowOverstacking;
    }

    public static boolean basicCanPlaceBlocks() {
        return basicCanPlaceBlocks;
    }

    public static boolean advancedCanPlaceBlocks() {
        return advancedCanPlaceBlocks;
    }

    public static boolean canPlaceBlocks(boolean basic) {
        return basic ? basicCanPlaceBlocks : advancedCanPlaceBlocks;
    }

    public static boolean allowOverstacking(boolean basic) {
        return basic ? basicAllowOverstacking : advancedAllowOverstacking;
    }

    /**
     * Per-slot fill limit for a given item. When overstacking is disabled, vanilla-unstackable
     * items (max stack size 1) are capped at 1; other items still use the filter max.
     */
    public static int effectiveSlotCapacity(ItemStack stack, int filterMax, boolean basic) {
        if (allowOverstacking(basic) || stack.isEmpty()) {
            return Math.max(1, filterMax);
        }
        int vanilla = stack.getMaxStackSize();
        if (vanilla <= 1) {
            return 1;
        }
        return Math.max(1, filterMax);
    }

    public static int effectiveSlotCapacity(ItemResource resource, int filterMax, boolean basic) {
        if (resource.isEmpty()) {
            return Math.max(1, filterMax);
        }
        return effectiveSlotCapacity(resource.toStack(1), filterMax, basic);
    }

    public static FilterProfile basicProfile() {
        return new FilterProfile(
                1, 1, basicMaxStackSize,
                false, true, false, false,
                true, false,
                false, false,
                "item.dopasrandomutilities.dev_null.empty",
                "container.dopasrandomutilities.dev_null",
                null
        );
    }

    public static FilterProfile advancedProfile() {
        return new FilterProfile(
                advancedMinSlots,
                advancedMaxSlots,
                0,
                true, true, true, true,
                true, true,
                true, true,
                "item.dopasrandomutilities.advanced_dev_null.empty",
                "container.dopasrandomutilities.advanced_dev_null",
                "item.dopasrandomutilities.advanced_dev_null.slots"
        );
    }

    public static int clampAdvancedMaxStack(int value) {
        return Math.max(1, Math.min(advancedMaxStackSize, value));
    }

    public static int clampAdvancedSlotCount(int count) {
        return Math.max(advancedMinSlots, Math.min(advancedMaxSlots, count));
    }

    public static int clampAdvancedPage(int page, int slotCount) {
        int pages = Math.min(pageCountForSlots(slotCount), advancedMaxPages);
        return Math.max(0, Math.min(pages - 1, page));
    }

    public static int pageCountForSlots(int slotCount) {
        return Math.max(1, (Math.max(1, slotCount) + FilterContents.SLOTS_PER_PAGE - 1) / FilterContents.SLOTS_PER_PAGE);
    }

    public static int effectivePageCount(int slotCount) {
        return Math.min(pageCountForSlots(slotCount), advancedMaxPages);
    }

    private static void loadDefaultsFromJar() {
        ConfigPack.loadJarJson(DEFAULT_RESOURCE, DevNullConfig::applyJson, DevNullConfig::applyBuiltInDefaults);
    }

    private static void applyBuiltInDefaults() {
        basicMaxStackSize = 64;
        basicAllowOverstacking = false;
        basicCanPlaceBlocks = false;
        advancedMinSlots = 27;
        advancedMaxSlots = 81;
        advancedMaxStackSize = 1000;
        advancedMaxPages = 2;
        advancedAllowOverstacking = true;
        advancedCanPlaceBlocks = true;
    }

    private static void applyJson(JsonObject root) {
        JsonObject basic = root.getAsJsonObject("basic");
        JsonObject advanced = root.getAsJsonObject("advanced");
        if (basic == null || advanced == null) {
            throw new IllegalStateException("devnull.json must contain basic and advanced sections");
        }

        basicMaxStackSize = Math.max(1, basic.get("max_stack_size").getAsInt());
        basicAllowOverstacking = !basic.has("allow_overstacking") || basic.get("allow_overstacking").getAsBoolean();
        basicCanPlaceBlocks = basic.has("can_place_blocks") && basic.get("can_place_blocks").getAsBoolean();
        advancedMinSlots = Math.max(1, advanced.get("min_slots").getAsInt());
        advancedMaxSlots = Math.max(advancedMinSlots, advanced.get("max_slots").getAsInt());
        advancedMaxStackSize = Math.max(1, advanced.get("max_stack_size").getAsInt());
        advancedMaxPages = Math.max(1, advanced.get("max_pages").getAsInt());
        advancedAllowOverstacking = !advanced.has("allow_overstacking") || advanced.get("allow_overstacking").getAsBoolean();
        advancedCanPlaceBlocks = !advanced.has("can_place_blocks") || advanced.get("can_place_blocks").getAsBoolean();

        int slotsForPages = Math.min(advancedMaxSlots, advancedMaxPages * FilterContents.SLOTS_PER_PAGE);
        if (slotsForPages < advancedMaxSlots) {
            advancedMaxSlots = Math.max(advancedMinSlots, slotsForPages);
        }
    }
}
