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
import java.util.stream.Collectors;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class ArtifactApi {
    private static final Map<ResourceLocation, ArtifactDefinition> ARTIFACTS = new LinkedHashMap<>();
    private static final String ACTIVE_SOURCE_PATH_PREFIX = "artifact_active/";
    private static final String UNLOCK_SOURCE_PATH_PREFIX = "artifact_unlock/";

    private ArtifactApi() {}

    public static void bootstrap() {}

    public static ArtifactDefinition registerArtifact(ArtifactDefinition artifact) {
        XLibRegistryGuard.ensureMutable("artifacts");
        ArtifactDefinition previous = ARTIFACTS.putIfAbsent(artifact.id(), artifact);
        if (previous != null) {
            throw new IllegalStateException("Duplicate artifact registration: " + artifact.id());
        }
        return artifact;
    }

    public static Optional<ArtifactDefinition> unregisterArtifact(ResourceLocation artifactId) {
        XLibRegistryGuard.ensureMutable("artifacts");
        return Optional.ofNullable(ARTIFACTS.remove(artifactId));
    }

    public static Optional<ArtifactDefinition> findArtifact(ResourceLocation artifactId) {
        return Optional.ofNullable(ARTIFACTS.get(artifactId));
    }

    public static Collection<ArtifactDefinition> allArtifacts() {
        return List.copyOf(ARTIFACTS.values());
    }

    public static ResourceLocation activeSourceIdFor(ResourceLocation artifactId) {
        return nestedSourceId(ACTIVE_SOURCE_PATH_PREFIX, artifactId);
    }

    public static ResourceLocation unlockSourceIdFor(ResourceLocation artifactId) {
        return nestedSourceId(UNLOCK_SOURCE_PATH_PREFIX, artifactId);
    }

    public static Optional<ResourceLocation> parseActiveSource(ResourceLocation sourceId) {
        return parseSource(sourceId, ACTIVE_SOURCE_PATH_PREFIX);
    }

    public static Optional<ResourceLocation> parseUnlockSource(ResourceLocation sourceId) {
        return parseSource(sourceId, UNLOCK_SOURCE_PATH_PREFIX);
    }

    public static Set<ResourceLocation> unlockedArtifacts(AbilityData data) {
        return data.unlockedArtifacts().stream()
                .filter(ARTIFACTS::containsKey)
                .collect(Collectors.toUnmodifiableSet());
    }

    public static boolean isUnlocked(AbilityData data, ResourceLocation artifactId) {
        return ARTIFACTS.containsKey(artifactId) && data.hasUnlockedArtifact(artifactId);
    }

    public static boolean isUnlocked(Player player, ResourceLocation artifactId) {
        return isUnlocked(ModAttachments.get(player), artifactId);
    }

    public static Set<ResourceLocation> unlockSources(AbilityData data, ResourceLocation artifactId) {
        return Set.copyOf(data.artifactUnlockSourcesFor(artifactId));
    }

    public static Set<ResourceLocation> activeUnlockSources(AbilityData data) {
        Set<ResourceLocation> sourceIds = new LinkedHashSet<>();
        data.artifactUnlockSources().values().forEach(sourceIds::addAll);
        return Set.copyOf(sourceIds);
    }

    public static boolean isActive(Player player, ResourceLocation artifactId) {
        ArtifactDefinition artifact = ARTIFACTS.get(artifactId);
        if (artifact == null) {
            return false;
        }
        return matchesPresence(player, artifact, ModAttachments.get(player));
    }

    public static Map<ResourceLocation, Set<ResourceLocation>> activeBundlesBySource(Player player, AbilityData data) {
        Map<ResourceLocation, Set<ResourceLocation>> bundlesBySource = new LinkedHashMap<>();
        for (ArtifactDefinition artifact : ARTIFACTS.values()) {
            if (artifact.equippedBundles().isEmpty() || !matchesPresence(player, artifact, data)) {
                continue;
            }
            bundlesBySource.put(activeSourceIdFor(artifact.id()), Set.copyOf(artifact.equippedBundles()));
        }
        return Map.copyOf(bundlesBySource);
    }

    public static void unlock(ServerPlayer player, ResourceLocation artifactId) {
        unlock(player, artifactId, unlockSourceIdFor(artifactId));
    }

    public static void unlock(ServerPlayer player, ResourceLocation artifactId, ResourceLocation sourceId) {
        ArtifactDefinition artifact = ARTIFACTS.get(artifactId);
        if (artifact == null) {
            return;
        }
        AbilityData currentData = ModAttachments.get(player);
        AbilityData updatedData = currentData.withArtifactUnlockSource(artifactId, sourceId, true)
                .withManagedGrantSource(sourceId, true);
        if (!updatedData.equals(currentData)) {
            ModAttachments.set(player, updatedData);
        }
        GrantBundleApi.syncSourceBundles(player, sourceId, artifact.unlockedBundles());
    }

    public static void revokeUnlock(Player player, ResourceLocation artifactId) {
        revokeUnlock(player, artifactId, unlockSourceIdFor(artifactId));
    }

    public static void revokeUnlock(Player player, ResourceLocation artifactId, ResourceLocation sourceId) {
        ArtifactDefinition artifact = ARTIFACTS.get(artifactId);
        if (artifact == null) {
            return;
        }
        AbilityData currentData = ModAttachments.get(player);
        AbilityData updatedData = currentData.withArtifactUnlockSource(artifactId, sourceId, false);
        if (!updatedData.equals(currentData)) {
            ModAttachments.set(player, updatedData);
        }
        GrantBundleApi.clearManagedSourceBundles(player, sourceId);
    }

    public static void syncUnlockSourceArtifacts(Player player, ResourceLocation sourceId, Collection<ResourceLocation> artifactIds) {
        AbilityData currentData = ModAttachments.get(player);
        Set<ResourceLocation> desiredArtifactIds = artifactIds.stream()
                .filter(java.util.Objects::nonNull)
                .filter(ARTIFACTS::containsKey)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        AbilityData updatedData = currentData;
        for (ResourceLocation artifactId : Set.copyOf(currentData.unlockedArtifacts())) {
            if (currentData.artifactUnlockSourcesFor(artifactId).contains(sourceId) && !desiredArtifactIds.contains(artifactId)) {
                updatedData = updatedData.withArtifactUnlockSource(artifactId, sourceId, false);
            }
        }
        for (ResourceLocation artifactId : desiredArtifactIds) {
            updatedData = updatedData.withArtifactUnlockSource(artifactId, sourceId, true)
                    .withManagedGrantSource(sourceId, true);
        }
        if (!updatedData.equals(currentData)) {
            ModAttachments.set(player, updatedData);
        }

        Set<ResourceLocation> unlockedBundles = new LinkedHashSet<>();
        for (ResourceLocation artifactId : desiredArtifactIds) {
            ArtifactDefinition artifact = ARTIFACTS.get(artifactId);
            if (artifact != null) {
                unlockedBundles.addAll(artifact.unlockedBundles());
            }
        }
        GrantBundleApi.syncSourceBundles(player, sourceId, unlockedBundles);
    }

    static boolean matchesPresence(Player player, ArtifactDefinition artifact, AbilityData data) {
        if (AbilityRequirements.firstFailure(player, data, artifact.requirements()).isPresent()) {
            return false;
        }
        for (ArtifactPresenceMode presenceMode : artifact.presenceModes()) {
            if (matchesPresence(player, artifact, presenceMode)) {
                return true;
            }
        }
        return false;
    }

    static boolean matchesPresence(Player player, ArtifactDefinition artifact, ArtifactPresenceMode presenceMode) {
        return switch (presenceMode) {
            case INVENTORY -> anyMatching(player.getInventory().items, artifact)
                    || anyMatching(player.getInventory().armor, artifact)
                    || anyMatching(player.getInventory().offhand, artifact);
            case HOTBAR -> anyMatching(player.getInventory().items.subList(0, Math.min(9, player.getInventory().items.size())), artifact);
            case MAIN_HAND -> matchesItemId(player.getMainHandItem(), artifact);
            case OFF_HAND -> matchesItemId(player.getOffhandItem(), artifact);
            case ARMOR -> anyMatching(player.getInventory().armor, artifact);
            case EQUIPPED -> matchesItemId(player.getMainHandItem(), artifact)
                    || matchesItemId(player.getOffhandItem(), artifact)
                    || anyMatching(player.getInventory().armor, artifact);
        };
    }

    public static Collection<ArtifactDefinition> matchingArtifacts(ItemStack stack) {
        return itemId(stack).map(ArtifactApi::matchingArtifacts).orElse(List.of());
    }

    public static Collection<ArtifactDefinition> matchingArtifacts(ResourceLocation itemId) {
        if (itemId == null) {
            return List.of();
        }
        return ARTIFACTS.values().stream()
                .filter(artifact -> artifact.matchesItem(itemId))
                .toList();
    }

    private static boolean anyMatching(Iterable<ItemStack> stacks, ArtifactDefinition artifact) {
        for (ItemStack stack : stacks) {
            if (matchesItemId(stack, artifact)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesItemId(ItemStack stack, ArtifactDefinition artifact) {
        return itemId(stack).filter(artifact::matchesItem).isPresent();
    }

    private static Optional<ResourceLocation> itemId(ItemStack stack) {
        return stack == null || stack.isEmpty()
                ? Optional.empty()
                : Optional.of(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    private static ResourceLocation nestedSourceId(String prefix, ResourceLocation artifactId) {
        return ResourceLocation.fromNamespaceAndPath(
                XLib.MODID,
                prefix + artifactId.getNamespace() + "/" + artifactId.getPath()
        );
    }

    private static Optional<ResourceLocation> parseSource(ResourceLocation sourceId, String prefix) {
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
}
