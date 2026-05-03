package com.whatxe.xlib.ability;

import com.whatxe.xlib.XLib;
import com.whatxe.xlib.XLibRegistryGuard;
import com.whatxe.xlib.attachment.ModAttachments;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public final class IdentityApi {
    private static final String IDENTITY_SOURCE_PATH_PREFIX = "identity/";
    private static final String IDENTITY_PROJECTION_PATH_PREFIX = "identity_projection/";

    private static final Map<ResourceLocation, IdentityDefinition> IDENTITIES = new LinkedHashMap<>();

    private IdentityApi() {}

    public static void bootstrap() {}

    public static IdentityDefinition registerIdentity(IdentityDefinition identity) {
        XLibRegistryGuard.ensureMutable("identities");
        IdentityDefinition previous = IDENTITIES.putIfAbsent(identity.id(), identity);
        if (previous != null) {
            throw new IllegalStateException("Duplicate identity registration: " + identity.id());
        }
        try {
            StateFlagApi.registerStateFlag(identity.id());
        } catch (RuntimeException exception) {
            IDENTITIES.remove(identity.id());
            throw exception;
        }
        return identity;
    }

    public static Optional<IdentityDefinition> unregisterIdentity(ResourceLocation identityId) {
        XLibRegistryGuard.ensureMutable("identities");
        IdentityDefinition removed = IDENTITIES.remove(identityId);
        if (removed == null) {
            return Optional.empty();
        }
        StateFlagApi.unregisterStateFlag(identityId);
        return Optional.of(removed);
    }

    public static Optional<IdentityDefinition> findIdentity(ResourceLocation identityId) {
        return Optional.ofNullable(IDENTITIES.get(identityId));
    }

    public static Collection<IdentityDefinition> allIdentities() {
        return List.copyOf(IDENTITIES.values());
    }

    public static boolean isIdentity(ResourceLocation identityId) {
        return IDENTITIES.containsKey(identityId);
    }

    public static ResourceLocation sourceIdFor(ResourceLocation identityId) {
        return ResourceLocation.fromNamespaceAndPath(
                XLib.MODID,
                IDENTITY_SOURCE_PATH_PREFIX + identityId.getNamespace() + "/" + identityId.getPath()
        );
    }

    public static ResourceLocation projectionSourceIdFor(ResourceLocation sourceId) {
        return ResourceLocation.fromNamespaceAndPath(
                XLib.MODID,
                IDENTITY_PROJECTION_PATH_PREFIX + sourceId.getNamespace() + "/" + sourceId.getPath()
        );
    }

    public static Optional<ResourceLocation> parseProjectionSourceId(ResourceLocation sourceId) {
        if (!XLib.MODID.equals(sourceId.getNamespace()) || !sourceId.getPath().startsWith(IDENTITY_PROJECTION_PATH_PREFIX)) {
            return Optional.empty();
        }
        String suffix = sourceId.getPath().substring(IDENTITY_PROJECTION_PATH_PREFIX.length());
        String[] parts = suffix.split("/", 2);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(ResourceLocation.fromNamespaceAndPath(parts[0], parts[1]));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    public static boolean hasIdentity(Player player, ResourceLocation identityId) {
        return hasIdentity(ModAttachments.get(player), identityId);
    }

    public static boolean hasIdentity(AbilityData data, ResourceLocation identityId) {
        return data.hasStateFlag(identityId) && IDENTITIES.containsKey(identityId);
    }

    public static Set<ResourceLocation> activeIdentities(AbilityData data) {
        return data.activeStateFlags().stream()
                .filter(IDENTITIES::containsKey)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    public static Set<ResourceLocation> identitySources(Player player, ResourceLocation identityId) {
        return identitySources(ModAttachments.get(player), identityId);
    }

    public static Set<ResourceLocation> identitySources(AbilityData data, ResourceLocation identityId) {
        return Set.copyOf(data.stateFlagSourcesFor(identityId));
    }

    public static Set<ResourceLocation> identitiesForSource(AbilityData data, ResourceLocation sourceId) {
        Set<ResourceLocation> identityIds = new LinkedHashSet<>();
        for (ResourceLocation identityId : activeIdentities(data)) {
            if (data.stateFlagSourcesFor(identityId).contains(sourceId)) {
                identityIds.add(identityId);
            }
        }
        return Set.copyOf(identityIds);
    }

    public static Set<ResourceLocation> resolvedIdentities(ResourceLocation identityId) {
        LinkedHashSet<ResourceLocation> resolved = new LinkedHashSet<>();
        resolveIdentityClosure(identityId, resolved, new LinkedHashSet<>());
        return Set.copyOf(resolved);
    }

    public static Set<ResourceLocation> resolvedGrantBundles(ResourceLocation identityId) {
        LinkedHashSet<ResourceLocation> bundleIds = new LinkedHashSet<>();
        for (ResourceLocation resolvedIdentityId : resolvedIdentities(identityId)) {
            IdentityDefinition identity = IDENTITIES.get(resolvedIdentityId);
            if (identity != null) {
                bundleIds.addAll(identity.grantBundles().stream()
                        .filter(bundleId -> GrantBundleApi.findBundle(bundleId).isPresent())
                        .toList());
            }
        }
        return Set.copyOf(bundleIds);
    }

    public static Set<ResourceLocation> grantBundlesForSource(AbilityData data, ResourceLocation sourceId) {
        LinkedHashSet<ResourceLocation> bundleIds = new LinkedHashSet<>();
        for (ResourceLocation identityId : identitiesForSource(data, sourceId)) {
            bundleIds.addAll(resolvedGrantBundles(identityId));
        }
        return Set.copyOf(bundleIds);
    }

    public static void grantIdentity(Player player, ResourceLocation identityId) {
        grantIdentity(player, identityId, sourceIdFor(identityId));
    }

    public static void grantIdentity(Player player, ResourceLocation identityId, ResourceLocation sourceId) {
        if (findIdentity(identityId).isEmpty()) {
            return;
        }
        StateFlagApi.grant(player, identityId, sourceId);
        syncProjectionBundles(player, sourceId);
    }

    public static void grantIdentities(Player player, Collection<ResourceLocation> identityIds, ResourceLocation sourceId) {
        LinkedHashSet<ResourceLocation> resolvedIdentityIds = new LinkedHashSet<>(identityIds);
        for (ResourceLocation identityId : resolvedIdentityIds) {
            if (findIdentity(identityId).isPresent()) {
                StateFlagApi.grant(player, identityId, sourceId);
            }
        }
        syncProjectionBundles(player, sourceId);
    }

    public static void syncSourceIdentities(Player player, ResourceLocation sourceId, Collection<ResourceLocation> identityIds) {
        AbilityData currentData = ModAttachments.get(player);
        Set<ResourceLocation> desiredIdentityIds = identityIds.stream()
                .filter(Objects::nonNull)
                .filter(identityId -> findIdentity(identityId).isPresent())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        AbilityData updatedData = currentData;
        for (ResourceLocation identityId : activeIdentities(currentData)) {
            if (currentData.stateFlagSourcesFor(identityId).contains(sourceId) && !desiredIdentityIds.contains(identityId)) {
                updatedData = updatedData.withStateFlagSource(identityId, sourceId, false);
            }
        }
        for (ResourceLocation identityId : desiredIdentityIds) {
            updatedData = updatedData.withStateFlagSource(identityId, sourceId, true);
        }
        if (!updatedData.equals(currentData)) {
            ModAttachments.set(player, AbilityGrantApi.sanitize(AbilityApi.sanitizeData(updatedData)));
        }
        syncProjectionBundles(player, sourceId);
    }

    public static void revokeIdentity(Player player, ResourceLocation identityId) {
        revokeIdentity(player, identityId, sourceIdFor(identityId));
    }

    public static void revokeIdentity(Player player, ResourceLocation identityId, ResourceLocation sourceId) {
        if (findIdentity(identityId).isEmpty()) {
            return;
        }
        StateFlagApi.revoke(player, identityId, sourceId);
        syncProjectionBundles(player, sourceId);
    }

    public static void clearSourceIdentities(Player player, ResourceLocation sourceId) {
        AbilityData data = ModAttachments.get(player);
        AbilityData updatedData = data;
        for (ResourceLocation identityId : identitiesForSource(data, sourceId)) {
            updatedData = updatedData.withStateFlagSource(identityId, sourceId, false);
        }
        if (!updatedData.equals(data)) {
            ModAttachments.set(player, AbilityGrantApi.sanitize(AbilityApi.sanitizeData(updatedData)));
        }
        syncProjectionBundles(player, sourceId);
    }

    private static void syncProjectionBundles(Player player, ResourceLocation identitySourceId) {
        GrantBundleApi.syncSourceBundles(player, projectionSourceIdFor(identitySourceId), grantBundlesForSource(ModAttachments.get(player), identitySourceId));
    }

    private static void resolveIdentityClosure(
            ResourceLocation identityId,
            Set<ResourceLocation> resolved,
            Set<ResourceLocation> visiting
    ) {
        if (!visiting.add(identityId)) {
            throw new IllegalStateException("Identity inheritance cycle detected at " + identityId);
        }

        IdentityDefinition identity = IDENTITIES.get(identityId);
        if (identity == null) {
            visiting.remove(identityId);
            return;
        }

        resolved.add(identityId);
        for (ResourceLocation inheritedIdentityId : identity.inheritedIdentities()) {
            resolveIdentityClosure(inheritedIdentityId, resolved, visiting);
        }
        visiting.remove(identityId);
    }
}
