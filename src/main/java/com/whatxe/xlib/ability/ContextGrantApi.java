package com.whatxe.xlib.ability;

import com.whatxe.xlib.XLib;
import com.whatxe.xlib.XLibRegistryGuard;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class ContextGrantApi {
    private static final Map<ResourceLocation, ContextGrantProvider> PROVIDERS = new LinkedHashMap<>();

    private ContextGrantApi() {}

    public static void bootstrap() {}

    public static ContextGrantProvider registerProvider(ContextGrantProvider provider) {
        XLibRegistryGuard.ensureMutable("context_grant_providers");
        ContextGrantProvider previous = PROVIDERS.putIfAbsent(provider.id(), provider);
        if (previous != null) {
            throw new IllegalStateException("Duplicate context grant provider registration: " + provider.id());
        }
        return provider;
    }

    public static Optional<ContextGrantProvider> unregisterProvider(ResourceLocation providerId) {
        XLibRegistryGuard.ensureMutable("context_grant_providers");
        return Optional.ofNullable(PROVIDERS.remove(providerId));
    }

    public static Collection<ContextGrantProvider> allProviders() {
        return List.copyOf(PROVIDERS.values());
    }

    public static Collection<ContextGrantSnapshot> collectSnapshots(ServerPlayer player, AbilityData currentData) {
        Map<ResourceLocation, ContextGrantSnapshot> mergedSnapshots = new LinkedHashMap<>();
        for (ContextGrantProvider provider : PROVIDERS.values()) {
            try {
                for (ContextGrantSnapshot snapshot : provider.collect(player, currentData)) {
                    if (snapshot == null || snapshot.isEmpty()) {
                        continue;
                    }
                    mergedSnapshots.merge(snapshot.sourceId(), snapshot, ContextGrantSnapshot::merge);
                }
            } catch (RuntimeException exception) {
                XLib.LOGGER.error("Context grant provider {} failed for player {}", provider.id(), player.getGameProfile().getName(), exception);
            }
        }
        return List.copyOf(mergedSnapshots.values());
    }
}
