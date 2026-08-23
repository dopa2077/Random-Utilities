package com.dopa.randomutilities.solarpanel;

import com.dopa.randomutilities.solarpanel.config.SolarPanelConfig;

/** Solar panel tier and peak FE/t contribution. */
public enum SolarPanelTier {
    TIER1(1),
    TIER2(2),
    TIER3(3);

    private final int id;

    SolarPanelTier(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }

    public int fePerTick() {
        return switch (this) {
            case TIER1 -> SolarPanelConfig.tier1Fe();
            case TIER2 -> SolarPanelConfig.tier2Fe();
            case TIER3 -> SolarPanelConfig.tier3Fe();
        };
    }
}
