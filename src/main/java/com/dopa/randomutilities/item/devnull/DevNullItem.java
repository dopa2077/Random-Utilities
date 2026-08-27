package com.dopa.randomutilities.item.devnull;

import com.dopa.randomutilities.core.filter.FilterItem;

/** Basic /dev/null — one filter slot, fixed stack size. Limits come from {@code devnull.json}. */
public class DevNullItem extends FilterItem {
    public DevNullItem(Properties properties) {
        super(properties, com.dopa.randomutilities.core.filter.config.DevNullConfig.basicProfile());
    }
}
