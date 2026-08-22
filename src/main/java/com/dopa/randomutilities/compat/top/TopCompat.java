package com.dopa.randomutilities.compat.top;

import com.dopa.randomutilities.dOPasRandomUtilities;
import com.dopa.randomutilities.machine.OwnableMachine;
import com.dopa.randomutilities.machine.OwnerDisplayNames;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.function.Function;

/**
 * Reflection-based TOP hook so we do not compile against TOP API (mapping/version drift on MC 26.2).
 */
public final class TopCompat {
    private TopCompat() {}

    public static Function<?, ?> createPlugin() {
        return (Function<Object, Void>) TopCompat::register;
    }

    private static Void register(Object theOneProbe) {
        try {
            Class<?> providerType = Class.forName("mcjty.theoneprobe.api.IProbeInfoProvider");
            Object provider = Proxy.newProxyInstance(
                    providerType.getClassLoader(),
                    new Class<?>[] { providerType },
                    new ProbeHandler()
            );
            theOneProbe.getClass().getMethod("registerProvider", providerType).invoke(theOneProbe, provider);
        } catch (ReflectiveOperationException e) {
            dOPasRandomUtilities.LOGGER.warn("Failed to register The One Probe provider", e);
        }
        return null;
    }

    private static final class ProbeHandler implements InvocationHandler {
        private static final Identifier ID = Identifier.fromNamespaceAndPath(
                dOPasRandomUtilities.MOD_ID,
                "ownable_machine"
        );

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return switch (method.getName()) {
                case "getID" -> ID;
                case "addProbeInfo" -> {
                    addProbeInfo(args[1], args[4]);
                    yield null;
                }
                default -> defaultMethod(proxy, method, args);
            };
        }

        private static void addProbeInfo(Object probeInfo, Object hitData) throws ReflectiveOperationException {
            Object level = hitData.getClass().getMethod("getWorld").invoke(hitData);
            Object pos = hitData.getClass().getMethod("getPos").invoke(hitData);
            BlockEntity be = ((net.minecraft.world.level.Level) level).getBlockEntity((net.minecraft.core.BlockPos) pos);
            if (!(be instanceof OwnableMachine ownable)) {
                return;
            }
            Component text = ownable.hasOwner()
                    ? OwnerDisplayNames.ownerLabel(ownable.ownerUuid())
                    : OwnerDisplayNames.noOwnerLabel();
            probeInfo.getClass().getMethod("mcText", Component.class).invoke(probeInfo, text);
        }

        private static Object defaultMethod(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.isDefault()) {
                return InvocationHandler.invokeDefault(proxy, method, args);
            }
            return null;
        }
    }
}
