package com.dopa.randomutilities.compat.ftbchunks;

import com.dopa.randomutilities.dOPasRandomUtilities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;

/**
 * Reflection-based FTB Chunks hook so we do not load FTB API classes when the mod is absent
 * (FTB is compileOnly in build.gradle).
 */
public final class FtbChunksCompat {
    private static final String FTB_MOD_ID = "ftbchunks";

    private static Boolean available;
    private static Object apiInstance;
    private static Method getManager;
    private static Method shouldPreventInteraction;
    private static Object editBlockProtection;

    private FtbChunksCompat() {}

    public static boolean isAvailable() {
        if (available == null) {
            available = probe();
        }
        return available;
    }

    private static boolean probe() {
        if (!ModList.get().isLoaded(FTB_MOD_ID)) {
            return false;
        }
        try {
            Class<?> apiClass = Class.forName("dev.ftb.mods.ftbchunks.api.FTBChunksAPI");
            apiInstance = apiClass.getMethod("api").invoke(null);
            if (apiInstance == null || !(boolean) apiClass.getMethod("isManagerLoaded").invoke(apiInstance)) {
                return false;
            }
            getManager = apiInstance.getClass().getMethod("getManager");
            Object manager = getManager.invoke(apiInstance);
            Class<?> protectionClass = Class.forName("dev.ftb.mods.ftbchunks.api.Protection");
            editBlockProtection = protectionClass.getField("EDIT_BLOCK").get(null);
            for (Method method : manager.getClass().getMethods()) {
                if ("shouldPreventInteraction".equals(method.getName()) && method.getParameterCount() == 5) {
                    shouldPreventInteraction = method;
                    break;
                }
            }
            return shouldPreventInteraction != null;
        } catch (ReflectiveOperationException e) {
            dOPasRandomUtilities.LOGGER.warn("FTB Chunks present but API probe failed; claim checks disabled", e);
            return false;
        }
    }

    public static boolean canEditBlock(ServerLevel level, Player actor, BlockPos pos) {
        if (!isAvailable()) {
            return true;
        }
        if (!(actor instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        try {
            Object manager = getManager.invoke(apiInstance);
            boolean prevented = (boolean) shouldPreventInteraction.invoke(
                    manager,
                    serverPlayer,
                    InteractionHand.MAIN_HAND,
                    pos,
                    editBlockProtection,
                    null
            );
            return !prevented;
        } catch (ReflectiveOperationException e) {
            dOPasRandomUtilities.LOGGER.warn("FTB Chunks claim check failed", e);
            return true;
        }
    }
}
