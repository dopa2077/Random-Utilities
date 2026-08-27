package com.dopa.randomutilities.core.util;

import java.util.Locale;

/** Compact stack counts for filter slot overlays (1K, 10K, 1M, …). */
public final class CompactCountFormat {
    private static final String[] SUFFIXES = {"K", "M", "B", "T"};

    private CompactCountFormat() {}

    public static String format(int count) {
        return format(count & 0xFFFFFFFFL);
    }

    public static String format(long count) {
        if (count < 1_000L) {
            return Long.toString(count);
        }

        int tier = 0;
        double scaled = count;
        while (scaled >= 1_000L && tier < SUFFIXES.length) {
            scaled /= 1_000.0D;
            tier++;
        }
        tier = Math.max(1, tier);

        if (scaled >= 100.0D) {
            return String.format(Locale.ROOT, "%.0f%s", scaled, SUFFIXES[tier - 1]);
        }
        if (scaled >= 10.0D) {
            return String.format(Locale.ROOT, "%.0f%s", scaled, SUFFIXES[tier - 1]);
        }
        if (Math.rint(scaled) == scaled) {
            return String.format(Locale.ROOT, "%.0f%s", scaled, SUFFIXES[tier - 1]);
        }
        return String.format(Locale.ROOT, "%.1f%s", scaled, SUFFIXES[tier - 1]);
    }
}
