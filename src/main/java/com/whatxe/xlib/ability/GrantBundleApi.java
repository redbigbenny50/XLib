package com.whatxe.xlib.ability;

import com.whatxe.xlib.XLib;
import com.whatxe.xlib.XLibRegistryGuard;
import com.whatxe.xlib.attachment.ModAttachments;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class GrantBundleApi {
    private static final Map<ResourceLocation, GrantBundleDefinition> BUNDLES = new LinkedHashMap<>();

    private GrantBundleApi() {}

    public static void bootstrap() {}

    public static GrantBundleDefinition registerBundle(GrantBundleDefinition bundle) {
        XLibRegistryGuard.ensureMutable("grant_bundles");
        GrantBundleDefinition previous = BUNDLES.putIfAbsent(bundle.id(), bundle);
        if (previous != null) {
            throw new IllegalStateException("Duplicate grant bundle registration: " + bundle.id());
        }
        return bundle;
    }

    public static Optional<GrantBundleDefinition> unregisterBundle(ResourceLocation bundleId) {
        XLibRegistryGuard.ensureMutable("grant_bundles");
        return Optional.ofNullable(BUNDLES.remove(bundleId));
    }

    public static Optional<GrantBundleDefinition> findBundle(ResourceLocation bundleId) {
        return Optional.ofNullable(BUNDLES.get(bundleId));
    }

    public static Collection<GrantBundleDefinition> allBundles() {
        return List.copyOf(BUNDLES.values());
    }

    public static ResourceLocation sourceIdFor(ResourceLocation bundleId) {
        return ResourceLocation.fromNamespaceAndPath(
                XLib.MODID,
                "bundle/" + bundleId.getNamespace() + "/" + bundleId.getPath()
        );
    }

    public static Set<ResourceLocation> activeBundles(AbilityData data) {
        return data.activeGrantBundles().stream()
                .filter(BUNDLES::containsKey)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    public static Set<ResourceLocation> bundleSources(Player player, ResourceLocation bundleId) {
        return Set.copyOf(ModAttachments.get(player).grantBundleSourcesFor(bundleId));
    }

    public static Set<ResourceLocation> bundlesForSource(AbilityData data, ResourceLocation sourceId) {
        Set<ResourceLocation> bundleIds = new LinkedHashSet<>();
        for (ResourceLocation bundleId : data.activeGrantBundles()) {
            if (data.grantBundleSourcesFor(bundleId).contains(sourceId) && BUNDLES.containsKey(bundleId)) {
                bundleIds.add(bundleId);
            }
        }
        return Set.copyOf(bundleIds);
    }

    public static void grantBundle(Player player, ResourceLocation bundleId) {
        grantBundle(player, bundleId, sourceIdFor(bundleId));
    }

    public static void grantBundle(Player player, ResourceLocation bundleId, ResourceLocation sourceId) {
        Set<ResourceLocation> desiredBundles = new LinkedHashSet<>(bundlesForSource(ModAttachments.get(player), sourceId));
        desiredBundles.add(bundleId);
        setSourceBundles(player, sourceId, desiredBundles, false);
    }

    public static void revokeBundle(Player player, ResourceLocation bundleId) {
        revokeBundle(player, bundleId, sourceIdFor(bundleId));
    }

    public static void revokeBundle(Player player, ResourceLocation bundleId, ResourceLocation sourceId) {
        Set<ResourceLocation> desiredBundles = new LinkedHashSet<>(bundlesForSource(ModAttachments.get(player), sourceId));
        desiredBundles.remove(bundleId);
        setSourceBundles(player, sourceId, desiredBundles, false);
    }

    public static void syncSourceBundles(Player player, ResourceLocation sourceId, Collection<ResourceLocation> bundleIds) {
        setSourceBundles(player, sourceId, new LinkedHashSet<>(bundleIds), true);
    }

    public static void clearSourceBundles(Player player, ResourceLocation sourceId) {
        setSourceBundles(player, sourceId, Set.of(), false);
    }

    public static void clearManagedSourceBundles(Player player, ResourceLocation sourceId) {
        setSourceBundles(player, sourceId, Set.of(), true);
    }

    private static void setSourceBundles(
            Player player,
            ResourceLocation sourceId,
            Collection<ResourceLocation> desiredBundleIds,
            boolean managed
    ) {
        AbilityData currentData = ModAttachments.get(player);
        Set<ResourceLocation> resolvedBundleIds = desiredBundleIds.stream()
                .filter(java.util.Objects::nonNull)
                .filter(BUNDLES::containsKey)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        AbilityData updatedData = currentData;
        for (ResourceLocation bundleId : Set.copyOf(currentData.activeGrantBundles())) {
            if (currentData.grantBundleSourcesFor(bundleId).contains(sourceId) && !resolvedBundleIds.contains(bundleId)) {
                updatedData = updatedData.withGrantBundleSource(bundleId, sourceId, false);
            }
        }
        for (ResourceLocation bundleId : resolvedBundleIds) {
            updatedData = updatedData.withGrantBundleSource(bundleId, sourceId, true);
        }
        if (managed) {
            updatedData = updatedData.withManagedGrantSource(sourceId, true);
        }
        if (!updatedData.equals(currentData)) {
            ModAttachments.set(player, updatedData);
        }

        Set<ResourceLocation> abilities = new LinkedHashSet<>();
        Set<ResourceLocation> passives = new LinkedHashSet<>();
        Set<ResourceLocation> grantedItems = new LinkedHashSet<>();
        Set<ResourceLocation> recipePermissions = new LinkedHashSet<>();
        Set<ResourceLocation> blockedAbilities = new LinkedHashSet<>();
        Set<ResourceLocation> statePolicies = new LinkedHashSet<>();
        Set<ResourceLocation> stateFlags = new LinkedHashSet<>();
        for (ResourceLocation bundleId : resolvedBundleIds) {
            GrantBundleDefinition bundle = BUNDLES.get(bundleId);
            if (bundle == null) {
                continue;
            }
            abilities.addAll(bundle.abilities());
            passives.addAll(bundle.passives());
            grantedItems.addAll(bundle.grantedItems());
            recipePermissions.addAll(bundle.recipePermissions());
            blockedAbilities.addAll(bundle.blockedAbilities());
            statePolicies.addAll(bundle.statePolicies());
            stateFlags.addAll(bundle.stateFlags());
        }

        AbilityGrantApi.syncSourceAbilities(player, sourceId, abilities);
        PassiveGrantApi.syncSourcePassives(player, sourceId, passives);
        GrantedItemGrantApi.syncSourceItems(player, sourceId, grantedItems);
        RecipePermissionApi.syncSourcePermissions(player, sourceId, recipePermissions);
        AbilityGrantApi.syncActivationBlocks(player, sourceId, blockedAbilities);
        StatePolicyApi.syncSourcePolicies(player, sourceId, statePolicies);
        StateFlagApi.syncSourceFlags(player, sourceId, stateFlags);

        AbilityData finalData = ModAttachments.get(player);
        if (!managed && !usesSource(finalData, sourceId)) {
            finalData = finalData.withManagedGrantSource(sourceId, false);
            if (!finalData.equals(ModAttachments.get(player))) {
                ModAttachments.set(player, finalData);
            }
        }
        if (managed && resolvedBundleIds.isEmpty() && !usesSource(finalData, sourceId)) {
            AbilityData clearedData = finalData.withManagedGrantSource(sourceId, false);
            if (!clearedData.equals(finalData)) {
                ModAttachments.set(player, clearedData);
            }
        }
        if (player instanceof ServerPlayer serverPlayer) {
            GrantedItemRuntime.restoreMissingUndroppableItems(serverPlayer, ModAttachments.get(serverPlayer));
            GrantedItemRuntime.reconcile(serverPlayer);
        }
    }

    private static boolean usesSource(AbilityData data, ResourceLocation sourceId) {
        return data.abilityGrantSources().values().stream().anyMatch(sources -> sources.contains(sourceId))
                || data.passiveGrantSources().values().stream().anyMatch(sources -> sources.contains(sourceId))
                || data.grantedItemSources().values().stream().anyMatch(sources -> sources.contains(sourceId))
                || data.recipePermissionSources().values().stream().anyMatch(sources -> sources.contains(sourceId))
                || data.abilityActivationBlockSources().values().stream().anyMatch(sources -> sources.contains(sourceId))
                || data.statePolicySources().values().stream().anyMatch(sources -> sources.contains(sourceId))
                || data.stateFlagSources().values().stream().anyMatch(sources -> sources.contains(sourceId))
                || data.grantBundleSources().values().stream().anyMatch(sources -> sources.contains(sourceId));
    }
}
