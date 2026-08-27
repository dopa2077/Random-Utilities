package com.dopa.randomutilities.item.devnull;

import com.dopa.randomutilities.core.filter.FilterItem;

/** Advanced /dev/null — expandable slots, color tint, settings panel. Limits come from {@code devnull.json}. */
public class AdvancedDevNullItem extends FilterItem {
    public AdvancedDevNullItem(Properties properties) {
        super(properties, com.dopa.randomutilities.core.filter.config.DevNullConfig.advancedProfile());
    }
}
