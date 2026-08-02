package com.dopa.randomutilities.item;

import com.dopa.randomutilities.filteritem.FilterItem;

/** Basic /dev/null — one filter slot, fixed stack size. Limits come from {@code devnull.json}. */
public class DevNullItem extends FilterItem {
    public DevNullItem(Properties properties) {
        super(properties, com.dopa.randomutilities.config.DevNullConfig.basicProfile());
    }
}
