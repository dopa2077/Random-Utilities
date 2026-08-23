package com.dopa.randomutilities.config;

import com.dopa.randomutilities.dOPasRandomUtilities;
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
import java.util.function.Consumer;

/** Shared helpers for {@code config/dopas_random_utilities/}. */
public final class ConfigPack {
    public static final String ROOT = "dopas_random_utilities";
    private static final String README_RESOURCE = "/default/dopas_random_utilities/README.txt";
    private static volatile boolean rootReady;

    private ConfigPack() {}

    public static Path root() {
        return FMLPaths.CONFIGDIR.get().resolve(ROOT);
    }

    public static Path resolve(String relativeUnderRoot) {
        return root().resolve(relativeUnderRoot);
    }

    /** Ensures the config root exists and copies README.txt once if missing. */
    public static void ensureRoot() {
        if (rootReady) {
            return;
        }
        try {
            Files.createDirectories(root());
            Path readme = root().resolve("README.txt");
            if (Files.notExists(readme)) {
                copyResource(README_RESOURCE, readme);
            }
            rootReady = true;
        } catch (IOException exception) {
            dOPasRandomUtilities.LOGGER.error("Failed to prepare config folder {}", root(), exception);
        }
    }

    /**
     * Copies a jar default to {@code config/dopas_random_utilities/<relative>} when missing.
     * @return the disk path
     */
    public static Path ensureFile(String relativeUnderRoot, String jarResourcePath) throws IOException {
        ensureRoot();
        Path configFile = resolve(relativeUnderRoot);
        Files.createDirectories(configFile.getParent());
        if (Files.notExists(configFile)) {
            copyResource(jarResourcePath, configFile);
            dOPasRandomUtilities.LOGGER.info("Wrote config at {}", configFile);
        }
        return configFile;
    }

    /**
     * Ensure disk file, parse JSON object, apply. On failure runs {@code onFailure} (typically jar defaults).
     */
    public static void loadJson(
            String relativeUnderRoot,
            String jarResourcePath,
            Consumer<JsonObject> apply,
            Runnable onFailure
    ) {
        try {
            Path configFile = ensureFile(relativeUnderRoot, jarResourcePath);
            try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
                apply.accept(JsonParser.parseReader(reader).getAsJsonObject());
            }
        } catch (IOException | JsonSyntaxException | IllegalStateException exception) {
            dOPasRandomUtilities.LOGGER.error(
                    "Failed to load config {}, using defaults",
                    relativeUnderRoot,
                    exception
            );
            onFailure.run();
        }
    }

    /** Parse a bundled jar JSON object, or run {@code onMissing} if the resource is absent/invalid. */
    public static void loadJarJson(String jarResourcePath, Consumer<JsonObject> apply, Runnable onMissing) {
        try (InputStream input = ConfigPack.class.getResourceAsStream(jarResourcePath)) {
            if (input == null) {
                onMissing.run();
                return;
            }
            apply.accept(JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject());
        } catch (IOException | JsonSyntaxException | IllegalStateException exception) {
            dOPasRandomUtilities.LOGGER.error("Failed to load bundled {}", jarResourcePath, exception);
            onMissing.run();
        }
    }

    public static int intOr(JsonObject object, String key, int fallback) {
        if (object == null || !object.has(key) || !object.get(key).isJsonPrimitive()) {
            return fallback;
        }
        return Math.max(0, object.get(key).getAsInt());
    }

    public static double doubleOr(JsonObject object, String key, double fallback) {
        if (object == null || !object.has(key) || !object.get(key).isJsonPrimitive()) {
            return fallback;
        }
        return Math.max(0.0, object.get(key).getAsDouble());
    }

    private static void copyResource(String jarResourcePath, Path target) throws IOException {
        try (InputStream input = ConfigPack.class.getResourceAsStream(jarResourcePath)) {
            if (input == null) {
                throw new IOException("Missing bundled default at " + jarResourcePath);
            }
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
