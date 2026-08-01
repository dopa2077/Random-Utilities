package com.dopa.randomutilities.config;

import com.dopa.randomutilities.dOPasRandomUtilities;
import com.dopa.randomutilities.filteritem.FilterContents;
import com.dopa.randomutilities.filteritem.FilterProfile;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import net.neoforged.fml.loading.FMLPaths;

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
    private static int advancedMinSlots = FilterContents.MIN_ADVANCED_SLOTS;
    private static int advancedMaxSlots = FilterContents.SLOTS_PER_PAGE;
    private static int advancedMaxStackSize = 1_000_000;
    private static int advancedMaxPages = 1;

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

    public static FilterProfile basicProfile() {
        return new FilterProfile(
                1, 1, basicMaxStackSize,
                false, false, false, false,
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
        advancedMinSlots = FilterContents.MIN_ADVANCED_SLOTS;
        advancedMaxSlots = FilterContents.SLOTS_PER_PAGE;
        advancedMaxStackSize = 1_000_000;
        advancedMaxPages = 1;
    }

    private static void applyJson(JsonObject root) {
        JsonObject basic = root.getAsJsonObject("basic");
        JsonObject advanced = root.getAsJsonObject("advanced");
        if (basic == null || advanced == null) {
            throw new IllegalStateException("devnull.json must contain basic and advanced sections");
        }

        basicMaxStackSize = Math.max(1, basic.get("max_stack_size").getAsInt());
        advancedMinSlots = Math.max(1, advanced.get("min_slots").getAsInt());
        advancedMaxSlots = Math.max(advancedMinSlots, advanced.get("max_slots").getAsInt());
        advancedMaxStackSize = Math.max(1, advanced.get("max_stack_size").getAsInt());
        advancedMaxPages = Math.max(1, advanced.get("max_pages").getAsInt());

        int slotsForPages = Math.min(advancedMaxSlots, advancedMaxPages * FilterContents.SLOTS_PER_PAGE);
        if (slotsForPages < advancedMaxSlots) {
            advancedMaxSlots = Math.max(advancedMinSlots, slotsForPages);
        }
    }
}
