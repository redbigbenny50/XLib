package com.whatxe.xlib.ability;

import com.whatxe.xlib.XLib;
import com.whatxe.xlib.progression.UpgradeApi;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;

public final class GrantOwnershipApi {
    private static final String MODE_SOURCE_PATH_PREFIX = "mode/";
    private static final String UPGRADE_NODE_SOURCE_PATH_PREFIX = "upgrade_node/";
    private static final String BUNDLE_SOURCE_PATH_PREFIX = "bundle/";
    private static final String SUPPORT_PACKAGE_SOURCE_PATH_PREFIX = "support_package/";
    private static final String ARTIFACT_ACTIVE_SOURCE_PATH_PREFIX = "artifact_active/";
    private static final String ARTIFACT_UNLOCK_SOURCE_PATH_PREFIX = "artifact_unlock/";
    private static final String RECIPE_ADVANCEMENT_SOURCE_PATH_PREFIX = "recipe_advancement/";

    private GrantOwnershipApi() {}

    public static Map<ResourceLocation, GrantSourceDescriptor> describeSources(AbilityData data) {
        Map<ResourceLocation, GrantSourceDescriptor> descriptors = new LinkedHashMap<>();
        for (ResourceLocation sourceId : sourceIds(data)) {
            descriptors.put(sourceId, describeSource(data, sourceId));
        }
        return descriptors;
    }

    public static GrantSourceDescriptor describeSource(AbilityData data, ResourceLocation sourceId) {
        boolean managed = data.managedGrantSources().contains(sourceId);
        Set<ResourceLocation> identities = IdentityApi.identitiesForSource(data, sourceId);
        Set<ResourceLocation> directBundles = GrantBundleApi.bundlesForSource(data, sourceId);

        if (isCommandSource(sourceId)) {
            return GrantSourceDescriptor.of(
                    sourceId,
                    GrantSourceKind.COMMAND,
                    null,
                    null,
                    Set.of(),
                    directBundles,
                    null,
                    managed,
                    "Applied directly through an XLib command source.",
                    "Remains until the command-owned source is revoked or the stored state is cleared."
            );
        }

        Optional<DelegatedGrantApi.DelegatedSource> delegatedSource = DelegatedGrantApi.parseSource(sourceId);
        if (delegatedSource.isPresent()) {
            DelegatedGrantApi.DelegatedSource resolved = delegatedSource.get();
            return GrantSourceDescriptor.of(
                    sourceId,
                    GrantSourceKind.DELEGATION,
                    resolved.bundleId(),
                    null,
                    Set.of(),
                    directBundles.isEmpty() ? Set.of(resolved.bundleId()) : directBundles,
                    resolved.grantorId(),
                    managed,
                    "Delegated by player " + resolved.grantorId() + " using bundle " + resolved.bundleId() + ".",
                    "Removed when the grantor revokes the delegation or the managed delegation source is cleared."
            );
        }

        Optional<SupportPackageApi.SupportSource> supportSource = SupportPackageApi.parseSource(sourceId);
        if (supportSource.isPresent()) {
            SupportPackageApi.SupportSource resolved = supportSource.get();
            Set<ResourceLocation> packageBundles = SupportPackageApi.findSupportPackage(resolved.supportPackageId())
                    .map(SupportPackageDefinition::grantBundles)
                    .orElse(Set.of());
            return GrantSourceDescriptor.of(
                    sourceId,
                    GrantSourceKind.SUPPORT_PACKAGE,
                    resolved.supportPackageId(),
                    null,
                    Set.of(),
                    directBundles.isEmpty() ? packageBundles : directBundles,
                    resolved.supporterId(),
                    managed,
                    "Shared by player " + resolved.supporterId() + " using support package " + resolved.supportPackageId() + ".",
                    "Removed when the source package is revoked or the managed support source is cleared."
            );
        }

        Optional<ResourceLocation> identityBackingSource = IdentityApi.parseProjectionSourceId(sourceId);
        if (identityBackingSource.isPresent()) {
            ResourceLocation backingSourceId = identityBackingSource.get();
            Set<ResourceLocation> backingIdentities = IdentityApi.identitiesForSource(data, backingSourceId);
            Set<ResourceLocation> projectedBundles = GrantBundleApi.bundlesForSource(data, sourceId);
            return GrantSourceDescriptor.of(
                    sourceId,
                    GrantSourceKind.IDENTITY_PROJECTION,
                    projectedBundles.size() == 1 ? projectedBundles.iterator().next() : null,
                    backingSourceId,
                    backingIdentities,
                    projectedBundles,
                    null,
                    managed,
                    "Projects bundles from identity source " + backingSourceId + ".",
                    "Updates whenever identities on the backing source change and disappears when no bundles remain projected."
            );
        }

        Optional<ResourceLocation> bundleId = parseNestedSourceId(sourceId, BUNDLE_SOURCE_PATH_PREFIX);
        if (bundleId.isPresent()) {
            return GrantSourceDescriptor.of(
                    sourceId,
                    GrantSourceKind.BUNDLE,
                    bundleId.get(),
                    null,
                    Set.of(),
                    directBundles.isEmpty() ? Set.of(bundleId.get()) : directBundles,
                    null,
                    managed,
                    "Granted by bundle " + bundleId.get() + ".",
                    "Remains until that bundle source is revoked or cleared."
            );
        }

        Optional<ResourceLocation> artifactId = parseNestedSourceId(sourceId, ARTIFACT_ACTIVE_SOURCE_PATH_PREFIX);
        if (artifactId.isPresent()) {
            Set<ResourceLocation> artifactBundles = ArtifactApi.findArtifact(artifactId.get())
                    .map(ArtifactDefinition::equippedBundles)
                    .orElse(Set.of());
            return GrantSourceDescriptor.of(
                    sourceId,
                    GrantSourceKind.ARTIFACT,
                    artifactId.get(),
                    null,
                    Set.of(),
                    directBundles.isEmpty() ? artifactBundles : directBundles,
                    null,
                    managed,
                    "Projected by active artifact " + artifactId.get() + ".",
                    "Ends when the backing item no longer satisfies the artifact presence rules or the managed source is pruned."
            );
        }

        Optional<ResourceLocation> unlockedArtifactId = parseNestedSourceId(sourceId, ARTIFACT_UNLOCK_SOURCE_PATH_PREFIX);
        if (unlockedArtifactId.isPresent()) {
            Set<ResourceLocation> artifactBundles = ArtifactApi.findArtifact(unlockedArtifactId.get())
                    .map(ArtifactDefinition::unlockedBundles)
                    .orElse(Set.of());
            return GrantSourceDescriptor.of(
                    sourceId,
                    GrantSourceKind.ARTIFACT_UNLOCK,
                    unlockedArtifactId.get(),
                    null,
                    Set.of(),
                    directBundles.isEmpty() ? artifactBundles : directBundles,
                    null,
                    managed,
                    "Granted by unlocked artifact " + unlockedArtifactId.get() + ".",
                    "Remains until that artifact unlock source is revoked or the stored unlock state is cleared."
            );
        }

        Optional<ResourceLocation> modeId = parseNestedSourceId(sourceId, MODE_SOURCE_PATH_PREFIX);
        if (modeId.isPresent()) {
            return GrantSourceDescriptor.of(
                    sourceId,
                    GrantSourceKind.MODE,
                    modeId.get(),
                    null,
                    Set.of(),
                    directBundles,
                    null,
                    managed,
                    "Projected by active mode " + modeId.get() + ".",
                    "Ends when the mode deactivates or its managed source is pruned."
            );
        }

        Optional<ResourceLocation> nodeId = parseNestedSourceId(sourceId, UPGRADE_NODE_SOURCE_PATH_PREFIX);
        if (nodeId.isPresent()) {
            return GrantSourceDescriptor.of(
                    sourceId,
                    GrantSourceKind.UPGRADE_NODE,
                    nodeId.get(),
                    null,
                    Set.of(),
                    directBundles,
                    null,
                    managed,
                    "Projected by upgrade node " + nodeId.get() + ".",
                    "Changes when the node is refunded, invalidated, or its rewards are rebuilt."
            );
        }

        Optional<ResourceLocation> recipeId = parseNestedSourceId(sourceId, RECIPE_ADVANCEMENT_SOURCE_PATH_PREFIX);
        if (recipeId.isPresent()) {
            return GrantSourceDescriptor.of(
                    sourceId,
                    GrantSourceKind.RECIPE_ADVANCEMENT,
                    recipeId.get(),
                    null,
                    Set.of(),
                    directBundles,
                    null,
                    managed,
                    "Granted by the recipe advancement for " + recipeId.get() + ".",
                    "Revokes when the advancement-backed permission is removed or the restriction changes."
            );
        }

        Optional<ResourceLocation> profileId = ProfileApi.parseSourceId(sourceId);
        if (profileId.isPresent()) {
            ProfileDefinition profile = ProfileApi.findProfile(profileId.get()).orElse(null);
            return GrantSourceDescriptor.of(
                    sourceId,
                    GrantSourceKind.PROFILE,
                    profileId.get(),
                    null,
                    identities,
                    directBundles,
                    null,
                    managed,
                    "Projected by selected profile " + profileId.get() + ".",
                    "Remains until the backing profile selection is cleared, reset, or rebuilt."
            );
        }

        if (!identities.isEmpty()) {
            Set<ResourceLocation> identityBundles = IdentityApi.grantBundlesForSource(data, sourceId);
            return GrantSourceDescriptor.of(
                    sourceId,
                    GrantSourceKind.IDENTITY,
                    identities.size() == 1 ? identities.iterator().next() : null,
                    null,
                    identities,
                    identityBundles,
                    null,
                    managed,
                    "Carries identity state for " + joinIds(identities) + ".",
                    "Remains until the identity is revoked from this source."
            );
        }

        return GrantSourceDescriptor.of(
                sourceId,
                managed ? GrantSourceKind.CUSTOM_MANAGED : GrantSourceKind.CUSTOM,
                null,
                null,
                Set.of(),
                directBundles,
                null,
                managed,
                managed
                        ? "Managed runtime source with no specialized XLib classifier."
                        : "Custom source with no specialized XLib classifier.",
                managed
                        ? "Removed when the runtime sync no longer reports this source."
                        : "Remains until the source is explicitly revoked."
        );
    }

    private static Set<ResourceLocation> sourceIds(AbilityData data) {
        Set<ResourceLocation> sourceIds = new LinkedHashSet<>(data.managedGrantSources());
        collectSourceIds(sourceIds, data.abilityGrantSources());
        collectSourceIds(sourceIds, data.passiveGrantSources());
        collectSourceIds(sourceIds, data.grantedItemSources());
        collectSourceIds(sourceIds, data.recipePermissionSources());
        collectSourceIds(sourceIds, data.abilityActivationBlockSources());
        collectSourceIds(sourceIds, data.statePolicySources());
        collectSourceIds(sourceIds, data.stateFlagSources());
        collectSourceIds(sourceIds, data.grantBundleSources());
        collectSourceIds(sourceIds, data.artifactUnlockSources());
        return Set.copyOf(sourceIds);
    }

    private static void collectSourceIds(
            Set<ResourceLocation> sourceIds,
            Map<ResourceLocation, Set<ResourceLocation>> sourceMap
    ) {
        for (Set<ResourceLocation> ids : sourceMap.values()) {
            sourceIds.addAll(ids);
        }
    }

    private static boolean isCommandSource(ResourceLocation sourceId) {
        return sourceId.equals(AbilityGrantApi.COMMAND_SOURCE)
                || sourceId.equals(AbilityGrantApi.COMMAND_ACTIVATION_BLOCK_SOURCE)
                || sourceId.equals(PassiveGrantApi.COMMAND_SOURCE)
                || sourceId.equals(GrantedItemGrantApi.COMMAND_SOURCE)
                || sourceId.equals(RecipePermissionApi.COMMAND_SOURCE)
                || sourceId.equals(StatePolicyApi.COMMAND_SOURCE)
                || sourceId.equals(StateFlagApi.COMMAND_SOURCE);
    }

    private static Optional<ResourceLocation> parseNestedSourceId(ResourceLocation sourceId, String prefix) {
        if (!XLib.MODID.equals(sourceId.getNamespace()) || !sourceId.getPath().startsWith(prefix)) {
            return Optional.empty();
        }
        String suffix = sourceId.getPath().substring(prefix.length());
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

    private static String joinIds(Set<ResourceLocation> ids) {
        return ids.isEmpty()
                ? "-"
                : ids.stream().map(ResourceLocation::toString).sorted().reduce((left, right) -> left + ", " + right).orElse("-");
    }
}
