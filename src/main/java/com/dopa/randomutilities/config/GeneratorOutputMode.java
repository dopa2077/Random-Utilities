package com.dopa.randomutilities.config;

/**
 * How a generator delivers its result.
 * <ul>
 *   <li>{@link #INSERT} — insert items into an inventory above, or fluids into a tank above</li>
 *   <li>{@link #DROP} — spawn item entities above the generator</li>
 *   <li>{@link #PLACE} — place a single block in the world above the generator</li>
 * </ul>
 */
public enum GeneratorOutputMode {
    INSERT,
    DROP,
    PLACE;

    public static GeneratorOutputMode parse(String value) {
        if (value == null || value.isBlank()) {
            return INSERT;
        }
        return switch (value.trim().toLowerCase()) {
            case "drop" -> DROP;
            case "place" -> PLACE;
            case "insert" -> INSERT;
            default -> INSERT;
        };
    }

    public String id() {
        return name().toLowerCase();
    }
}
