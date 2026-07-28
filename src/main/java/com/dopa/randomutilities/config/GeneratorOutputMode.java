package com.dopa.randomutilities.config;

/**
 * How a generator delivers its result.
 * <ul>
 *   <li>{@link #INSERT} — insert items into a container above the generator</li>
 *   <li>{@link #DROP} — place the block above the generator (or drop items if amount &gt; 1)</li>
 * </ul>
 */
public enum GeneratorOutputMode {
    INSERT,
    DROP;

    public static GeneratorOutputMode parse(String value) {
        if (value == null || value.isBlank()) {
            return INSERT;
        }
        return switch (value.trim().toLowerCase()) {
            case "drop" -> DROP;
            case "insert" -> INSERT;
            default -> INSERT;
        };
    }

    public String id() {
        return name().toLowerCase();
    }
}
