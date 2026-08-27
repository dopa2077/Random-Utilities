package com.dopa.randomutilities.core.machine;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

import java.util.UUID;

public final class OwnerDisplayNames {
    private static final String UNKNOWN = "Unknown";

    private OwnerDisplayNames() {}

    public static Component ownerLabel(UUID ownerUuid) {
        return Component.translatable("probe.dopasrandomutilities.owner", resolveName(ownerUuid));
    }

    public static Component noOwnerLabel() {
        return Component.translatable("probe.dopasrandomutilities.no_owner");
    }

    public static String resolveName(UUID ownerUuid) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() != null) {
            PlayerInfo info = minecraft.getConnection().getPlayerInfo(ownerUuid);
            if (info != null) {
                return info.getProfile().name();
            }
        }
        return UNKNOWN;
    }
}
