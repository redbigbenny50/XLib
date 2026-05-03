package com.whatxe.xlib;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.SharedConstants;

public final class XLibRegistryGuard {
    private static final Set<String> WARNED_REGISTRIES = ConcurrentHashMap.newKeySet();
    private static volatile boolean frozen;
    private static volatile String freezeReason = "unspecified";

    private XLibRegistryGuard() {}

    public static void freeze(String reason) {
        if (!frozen) {
            freezeReason = reason;
            frozen = true;
            XLib.LOGGER.debug("Freezing XLib registries: {}", reason);
        }
    }

    public static boolean isFrozen() {
        return frozen;
    }

    public static void ensureMutable(String registryName) {
        if (!frozen) {
            return;
        }

        String message = "XLib registry '" + registryName + "' is frozen after " + freezeReason;
        if (SharedConstants.IS_RUNNING_IN_IDE) {
            if (WARNED_REGISTRIES.add(registryName)) {
                XLib.LOGGER.warn("{}; allowing the mutation because the game is running in the IDE", message);
            }
            return;
        }

        throw new IllegalStateException(message);
    }
}
