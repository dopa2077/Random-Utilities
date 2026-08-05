package com.dopa.randomutilities.filter.item;

import com.dopa.randomutilities.filter.FilterItem;

/** Advanced /dev/null — expandable slots, color tint, settings panel. Limits come from {@code devnull.json}. */
public class AdvancedDevNullItem extends FilterItem {
    public AdvancedDevNullItem(Properties properties) {
        super(properties, com.dopa.randomutilities.filter.config.DevNullConfig.advancedProfile());
    }
}
