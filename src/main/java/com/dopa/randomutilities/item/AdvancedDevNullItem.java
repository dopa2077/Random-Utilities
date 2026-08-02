package com.dopa.randomutilities.item;

import com.dopa.randomutilities.filteritem.FilterItem;

/** Advanced /dev/null — expandable slots, color tint, settings panel. Limits come from {@code devnull.json}. */
public class AdvancedDevNullItem extends FilterItem {
    public AdvancedDevNullItem(Properties properties) {
        super(properties, com.dopa.randomutilities.config.DevNullConfig.advancedProfile());
    }
}
