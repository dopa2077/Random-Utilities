package com.dopa.randomutilities.item;

import com.dopa.randomutilities.filteritem.FilterContents;
import com.dopa.randomutilities.filteritem.FilterItem;
import com.dopa.randomutilities.filteritem.FilterProfile;

/** Advanced /dev/null — expandable slots, color tint, settings panel. Edit behaviour here; core logic lives in {@link FilterItem}. */
public class AdvancedDevNullItem extends FilterItem {
    public static final FilterProfile PROFILE = new FilterProfile(
            FilterContents.MIN_ADVANCED_SLOTS,
            FilterContents.MAX_TOTAL_SLOTS,
            0,
            true, true, true, true,
            "item.dopasrandomutilities.advanced_dev_null.empty",
            "container.dopasrandomutilities.advanced_dev_null",
            "item.dopasrandomutilities.advanced_dev_null.slots"
    );

    public AdvancedDevNullItem(Properties properties) {
        super(properties, PROFILE);
    }
}
