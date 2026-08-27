package com.dopa.randomutilities.core.machine;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class MachineOwnerProfiles {
    private static final String UNKNOWN_NAME = "Unknown";

    private MachineOwnerProfiles() {}

    public static GameProfile profileFor(ServerLevel level, UUID ownerUuid) {
        ServerPlayer online = level.getServer().getPlayerList().getPlayer(ownerUuid);
        if (online != null) {
            return online.getGameProfile();
        }
        return new GameProfile(ownerUuid, UNKNOWN_NAME);
    }

    @Nullable
    public static UUID load(ValueInput input) {
        String raw = input.getStringOr("OwnerUuid", "");
        if (raw.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static void save(ValueOutput output, @Nullable UUID ownerUuid) {
        if (ownerUuid != null) {
            output.putString("OwnerUuid", ownerUuid.toString());
        }
    }
}
