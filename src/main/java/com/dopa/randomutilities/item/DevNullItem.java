package com.dopa.randomutilities.item;

import com.dopa.randomutilities.filteritem.FilterItem;
import com.dopa.randomutilities.filteritem.FilterProfile;

/** Basic /dev/null — one filter slot, fixed stack size. Edit behaviour here; core logic lives in {@link FilterItem}. */
public class DevNullItem extends FilterItem {
    public static final FilterProfile PROFILE = new FilterProfile(
            1, 1, 64,
            false, false, false, false,
            "item.dopasrandomutilities.dev_null.empty",
            "container.dopasrandomutilities.dev_null",
            null
    );

    public DevNullItem(Properties properties) {
        super(properties, PROFILE);
    }
}
