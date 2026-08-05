package com.dopa.randomutilities.filter.item;

import com.dopa.randomutilities.filter.FilterItem;

/** Basic /dev/null — one filter slot, fixed stack size. Limits come from {@code devnull.json}. */
public class DevNullItem extends FilterItem {
    public DevNullItem(Properties properties) {
        super(properties, com.dopa.randomutilities.filter.config.DevNullConfig.basicProfile());
    }
}
