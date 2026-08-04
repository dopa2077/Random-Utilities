package com.dopa.randomutilities.config;

import com.dopa.randomutilities.dOPasRandomUtilities;
import com.dopa.randomutilities.filteritem.FilterContents;
import com.dopa.randomutilities.filteritem.FilterProfile;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Limits for /dev/null items. Loaded from {@code config/dopas_random_utilities/items/devnull.json}. */
public final class DevNullConfig {
    private static final String CONFIG_RELATIVE = "dopas_random_utilities/items/devnull.json";
    private static final String DEFAULT_RESOURCE = "/default/dopas_random_utilities/items/devnull.json";

    private static int basicMaxStackSize = 64;
    private static boolean basicAllowOverstacking = true;
    private static int advancedMinSlots = FilterContents.MIN_ADVANCED_SLOTS;
    private static int advancedMaxSlots = FilterContents.SLOTS_PER_PAGE;
    private static int advancedMaxStackSize = 1_000_000;
    private static int advancedMaxPages = 1;
    private static boolean advancedAllowOverstacking = true;

    static {
        loadDefaultsFromJar();
    }

    private DevNullConfig() {}

    public static void load() {
        Path configFile = FMLPaths.CONFIGDIR.get().resolve(CONFIG_RELATIVE);
        try {
            Files.createDirectories(configFile.getParent());
            if (Files.notExists(configFile)) {
                copyDefaultConfig(configFile);
            }
            try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
                applyJson(JsonParser.parseReader(reader).getAsJsonObject());
            }
        } catch (IOException | JsonSyntaxException | IllegalStateException exception) {
            dOPasRandomUtilities.LOGGER.error("Failed to load dev/null config from {}, using defaults", configFile, exception);
            loadDefaultsFromJar();
        }
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
                false, false, false, false, false,
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
                true, false, false, false, true,
                "item.dopasrandomutilities.advanced_dev_null.empty",
                "container.dopasrandomutilities.advanced_dev_null",
                "item.dopasrandomutilities.advanced_dev_null.slots"
        );
    }

    /** Full AttachedPanel set for creative UI testers. */
    public static FilterProfile uiTestProfile() {
        return new FilterProfile(
                advancedMinSlots,
                advancedMaxSlots,
                0,
                true, true, true, true,
                true, true,
                true, true, true, true, true,
                "item.dopasrandomutilities.ui_test_item.empty",
                "container.dopasrandomutilities.ui_test",
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
        try (InputStream input = DevNullConfig.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (input == null) {
                applyBuiltInDefaults();
                return;
            }
            applyJson(JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject());
        } catch (IOException | JsonSyntaxException | IllegalStateException exception) {
            dOPasRandomUtilities.LOGGER.error("Failed to load bundled dev/null config defaults", exception);
            applyBuiltInDefaults();
        }
    }

    private static void copyDefaultConfig(Path configFile) throws IOException {
        try (InputStream input = DevNullConfig.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (input == null) {
                throw new IOException("Missing bundled default config at " + DEFAULT_RESOURCE);
            }
            Files.copy(input, configFile, StandardCopyOption.REPLACE_EXISTING);
            dOPasRandomUtilities.LOGGER.info("Wrote dev/null config at {}", configFile);
        }
    }

    private static void applyBuiltInDefaults() {
        basicMaxStackSize = 64;
        basicAllowOverstacking = true;
        advancedMinSlots = FilterContents.MIN_ADVANCED_SLOTS;
        advancedMaxSlots = FilterContents.SLOTS_PER_PAGE;
        advancedMaxStackSize = 1_000_000;
        advancedMaxPages = 1;
        advancedAllowOverstacking = true;
    }

    private static void applyJson(JsonObject root) {
        JsonObject basic = root.getAsJsonObject("basic");
        JsonObject advanced = root.getAsJsonObject("advanced");
        if (basic == null || advanced == null) {
            throw new IllegalStateException("devnull.json must contain basic and advanced sections");
        }

        basicMaxStackSize = Math.max(1, basic.get("max_stack_size").getAsInt());
        basicAllowOverstacking = !basic.has("allow_overstacking") || basic.get("allow_overstacking").getAsBoolean();
        advancedMinSlots = Math.max(1, advanced.get("min_slots").getAsInt());
        advancedMaxSlots = Math.max(advancedMinSlots, advanced.get("max_slots").getAsInt());
        advancedMaxStackSize = Math.max(1, advanced.get("max_stack_size").getAsInt());
        advancedMaxPages = Math.max(1, advanced.get("max_pages").getAsInt());
        advancedAllowOverstacking = !advanced.has("allow_overstacking") || advanced.get("allow_overstacking").getAsBoolean();

        int slotsForPages = Math.min(advancedMaxSlots, advancedMaxPages * FilterContents.SLOTS_PER_PAGE);
        if (slotsForPages < advancedMaxSlots) {
            advancedMaxSlots = Math.max(advancedMinSlots, slotsForPages);
        }
    }
}
