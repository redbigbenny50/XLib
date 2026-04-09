package com.whatxe.xlib.ability;

import com.whatxe.xlib.XLib;
import com.whatxe.xlib.XLibRegistryGuard;
import com.whatxe.xlib.attachment.ModAttachments;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public final class StateFlagApi {
    public static final ResourceLocation COMMAND_SOURCE =
            ResourceLocation.fromNamespaceAndPath(XLib.MODID, "command_state_flag");

    private static final Set<ResourceLocation> STATE_FLAGS = new LinkedHashSet<>();

    private StateFlagApi() {}

    public static void bootstrap() {}

    public static ResourceLocation registerStateFlag(ResourceLocation flagId) {
        XLibRegistryGuard.ensureMutable("state_flags");
        ResourceLocation resolvedFlagId = java.util.Objects.requireNonNull(flagId, "flagId");
        if (!STATE_FLAGS.add(resolvedFlagId)) {
            throw new IllegalStateException("Duplicate state flag registration: " + resolvedFlagId);
        }
        return resolvedFlagId;
    }

    public static Optional<ResourceLocation> unregisterStateFlag(ResourceLocation flagId) {
        XLibRegistryGuard.ensureMutable("state_flags");
        ResourceLocation resolvedFlagId = java.util.Objects.requireNonNull(flagId, "flagId");
        return STATE_FLAGS.remove(resolvedFlagId) ? Optional.of(resolvedFlagId) : Optional.empty();
    }

    public static Optional<ResourceLocation> findStateFlag(ResourceLocation flagId) {
        ResourceLocation resolvedFlagId = java.util.Objects.requireNonNull(flagId, "flagId");
        return STATE_FLAGS.contains(resolvedFlagId) ? Optional.of(resolvedFlagId) : Optional.empty();
    }

    public static Collection<ResourceLocation> allStateFlags() {
        return List.copyOf(STATE_FLAGS);
    }

    public static boolean hasActiveFlag(Player player, ResourceLocation flagId) {
        return hasActiveFlag(ModAttachments.get(player), flagId);
    }

    public static boolean hasActiveFlag(AbilityData data, ResourceLocation flagId) {
        return data.hasStateFlag(flagId) && findStateFlag(flagId).isPresent();
    }

    public static Set<ResourceLocation> activeFlags(AbilityData data) {
        return data.activeStateFlags().stream()
                .filter(STATE_FLAGS::contains)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    public static Set<ResourceLocation> flagSources(Player player, ResourceLocation flagId) {
        return Set.copyOf(ModAttachments.get(player).stateFlagSourcesFor(flagId));
    }

    public static void grant(Player player, ResourceLocation flagId) {
        grant(player, flagId, COMMAND_SOURCE);
    }

    public static void grant(Player player, ResourceLocation flagId, ResourceLocation sourceId) {
        update(player, AbilityGrantApi.sanitize(AbilityApi.sanitizeData(
                ModAttachments.get(player).withStateFlagSource(flagId, sourceId, true)
        )));
    }

    public static void grant(Player player, Collection<ResourceLocation> flagIds, ResourceLocation sourceId) {
        AbilityData data = ModAttachments.get(player);
        for (ResourceLocation flagId : new LinkedHashSet<>(flagIds)) {
            data = data.withStateFlagSource(flagId, sourceId, true);
        }
        update(player, AbilityGrantApi.sanitize(AbilityApi.sanitizeData(data)));
    }

    public static void revoke(Player player, ResourceLocation flagId) {
        revoke(player, flagId, COMMAND_SOURCE);
    }

    public static void revoke(Player player, ResourceLocation flagId, ResourceLocation sourceId) {
        update(player, AbilityGrantApi.sanitize(AbilityApi.sanitizeData(
                ModAttachments.get(player).withStateFlagSource(flagId, sourceId, false)
        )));
    }

    public static void clearFlags(Player player) {
        update(player, AbilityGrantApi.sanitize(AbilityApi.sanitizeData(
                ModAttachments.get(player).clearStateFlagSources()
        )));
    }

    public static void syncSourceFlags(Player player, ResourceLocation sourceId, Collection<ResourceLocation> flagIds) {
        AbilityData data = ModAttachments.get(player).withManagedGrantSource(sourceId, true);
        Set<ResourceLocation> desiredFlags = new LinkedHashSet<>(flagIds);
        for (ResourceLocation flagId : Set.copyOf(data.activeStateFlags())) {
            if (data.stateFlagSourcesFor(flagId).contains(sourceId) && !desiredFlags.contains(flagId)) {
                data = data.withStateFlagSource(flagId, sourceId, false);
            }
        }
        for (ResourceLocation flagId : desiredFlags) {
            data = data.withStateFlagSource(flagId, sourceId, true);
        }
        update(player, AbilityGrantApi.sanitize(AbilityApi.sanitizeData(data)));
    }

    public static AbilityData revokeSourceFlags(AbilityData data, ResourceLocation sourceId) {
        return data.clearStateFlagSource(sourceId);
    }

    private static void update(Player player, AbilityData updatedData) {
        if (!updatedData.equals(ModAttachments.get(player))) {
            ModAttachments.set(player, updatedData);
        }
    }
}
