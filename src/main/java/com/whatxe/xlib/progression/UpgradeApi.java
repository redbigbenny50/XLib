package com.whatxe.xlib.progression;

import com.whatxe.xlib.XLib;
import com.whatxe.xlib.XLibRegistryGuard;
import com.whatxe.xlib.api.event.XLibUpgradeRewardProjectionEvent;
import com.whatxe.xlib.ability.AbilityCombatTracker;
import com.whatxe.xlib.ability.AbilityData;
import com.whatxe.xlib.ability.AbilityGrantApi;
import com.whatxe.xlib.ability.GrantedItemGrantApi;
import com.whatxe.xlib.ability.IdentityApi;
import com.whatxe.xlib.ability.PassiveGrantApi;
import com.whatxe.xlib.ability.RecipePermissionApi;
import com.whatxe.xlib.attachment.ModAttachments;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

public final class UpgradeApi {
    private static final Map<ResourceLocation, UpgradePointType> POINT_TYPES = new LinkedHashMap<>();
    private static final Map<ResourceLocation, UpgradeTrackDefinition> TRACKS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, UpgradeNodeDefinition> NODES = new LinkedHashMap<>();
    private static final Map<ResourceLocation, UpgradeConsumeRule> CONSUME_RULES = new LinkedHashMap<>();
    private static final Map<ResourceLocation, UpgradeKillRule> KILL_RULES = new LinkedHashMap<>();

    private UpgradeApi() {}

    public static void bootstrap() {}

    public static UpgradePointType registerPointType(UpgradePointType pointType) {
        XLibRegistryGuard.ensureMutable("upgrade_point_types");
        UpgradePointType previous = POINT_TYPES.putIfAbsent(pointType.id(), pointType);
        if (previous != null) {
            throw new IllegalStateException("Duplicate upgrade point type registration: " + pointType.id());
        }
        return pointType;
    }

    public static Optional<UpgradePointType> unregisterPointType(ResourceLocation id) {
        XLibRegistryGuard.ensureMutable("upgrade_point_types");
        return Optional.ofNullable(POINT_TYPES.remove(id));
    }

    public static Optional<UpgradePointType> findPointType(ResourceLocation id) {
        return Optional.ofNullable(POINT_TYPES.get(id));
    }

    public static Collection<UpgradePointType> allPointTypes() {
        return List.copyOf(POINT_TYPES.values());
    }

    public static UpgradeTrackDefinition registerTrack(UpgradeTrackDefinition track) {
        XLibRegistryGuard.ensureMutable("upgrade_tracks");
        UpgradeTrackDefinition previous = TRACKS.putIfAbsent(track.id(), track);
        if (previous != null) {
            throw new IllegalStateException("Duplicate upgrade track registration: " + track.id());
        }
        return track;
    }

    public static Optional<UpgradeTrackDefinition> unregisterTrack(ResourceLocation id) {
        XLibRegistryGuard.ensureMutable("upgrade_tracks");
        return Optional.ofNullable(TRACKS.remove(id));
    }

    public static Optional<UpgradeTrackDefinition> findTrack(ResourceLocation id) {
        return Optional.ofNullable(TRACKS.get(id));
    }

    public static Collection<UpgradeTrackDefinition> allTracks() {
        return List.copyOf(TRACKS.values());
    }

    public static Collection<UpgradeTrackDefinition> tracksInFamily(ResourceLocation familyId) {
        ResourceLocation resolvedFamilyId = java.util.Objects.requireNonNull(familyId, "familyId");
        return filterTracks(track -> track.familyId().filter(resolvedFamilyId::equals).isPresent());
    }

    public static Collection<UpgradeTrackDefinition> tracksInGroup(ResourceLocation groupId) {
        ResourceLocation resolvedGroupId = java.util.Objects.requireNonNull(groupId, "groupId");
        return filterTracks(track -> track.groupId().filter(resolvedGroupId::equals).isPresent());
    }

    public static Collection<UpgradeTrackDefinition> tracksOnPage(ResourceLocation pageId) {
        ResourceLocation resolvedPageId = java.util.Objects.requireNonNull(pageId, "pageId");
        return filterTracks(track -> track.pageId().filter(resolvedPageId::equals).isPresent());
    }

    public static Collection<UpgradeTrackDefinition> tracksWithTag(ResourceLocation tagId) {
        ResourceLocation resolvedTagId = java.util.Objects.requireNonNull(tagId, "tagId");
        return filterTracks(track -> track.hasTag(resolvedTagId));
    }

    public static List<UpgradeNodeDefinition> nodesInTrack(ResourceLocation trackId) {
        List<UpgradeNodeDefinition> nodes = new ArrayList<>();
        for (UpgradeNodeDefinition node : allNodes()) {
            if (trackId.equals(node.trackId())) {
                nodes.add(node);
            }
        }
        return List.copyOf(nodes);
    }

    public static List<UpgradeTrackDefinition> visibleTracks(UpgradeProgressData data) {
        List<UpgradeTrackDefinition> visibleTracks = new ArrayList<>();
        for (UpgradeTrackDefinition track : allTracks()) {
            if (trackHasUnlockedNodes(data, track.id()) || !isTrackBlocked(data, track.id())) {
                visibleTracks.add(track);
            }
        }
        return List.copyOf(visibleTracks);
    }

    public static UpgradeNodeDefinition registerNode(UpgradeNodeDefinition node) {
        XLibRegistryGuard.ensureMutable("upgrade_nodes");
        UpgradeNodeDefinition previous = NODES.putIfAbsent(node.id(), node);
        if (previous != null) {
            throw new IllegalStateException("Duplicate upgrade node registration: " + node.id());
        }
        return node;
    }

    public static Optional<UpgradeNodeDefinition> unregisterNode(ResourceLocation id) {
        XLibRegistryGuard.ensureMutable("upgrade_nodes");
        return Optional.ofNullable(NODES.remove(id));
    }

    public static Optional<UpgradeNodeDefinition> findNode(ResourceLocation id) {
        return Optional.ofNullable(NODES.get(id));
    }

    public static Collection<UpgradeNodeDefinition> allNodes() {
        return List.copyOf(NODES.values());
    }

    public static Collection<UpgradeNodeDefinition> nodesInFamily(ResourceLocation familyId) {
        ResourceLocation resolvedFamilyId = java.util.Objects.requireNonNull(familyId, "familyId");
        return filterNodes(node -> node.familyId().filter(resolvedFamilyId::equals).isPresent());
    }

    public static Collection<UpgradeNodeDefinition> nodesInGroup(ResourceLocation groupId) {
        ResourceLocation resolvedGroupId = java.util.Objects.requireNonNull(groupId, "groupId");
        return filterNodes(node -> node.groupId().filter(resolvedGroupId::equals).isPresent());
    }

    public static Collection<UpgradeNodeDefinition> nodesOnPage(ResourceLocation pageId) {
        ResourceLocation resolvedPageId = java.util.Objects.requireNonNull(pageId, "pageId");
        return filterNodes(node -> node.pageId().filter(resolvedPageId::equals).isPresent());
    }

    public static Collection<UpgradeNodeDefinition> nodesWithTag(ResourceLocation tagId) {
        ResourceLocation resolvedTagId = java.util.Objects.requireNonNull(tagId, "tagId");
        return filterNodes(node -> node.hasTag(resolvedTagId));
    }

    public static Collection<UpgradeNodeDefinition> nodesInChoiceGroup(ResourceLocation choiceGroupId) {
        ResourceLocation resolvedChoiceGroupId = java.util.Objects.requireNonNull(choiceGroupId, "choiceGroupId");
        return filterNodes(node -> node.choiceGroupId().filter(resolvedChoiceGroupId::equals).isPresent());
    }

    public static UpgradeConsumeRule registerConsumeRule(UpgradeConsumeRule rule) {
        XLibRegistryGuard.ensureMutable("upgrade_consume_rules");
        UpgradeConsumeRule previous = CONSUME_RULES.putIfAbsent(rule.id(), rule);
        if (previous != null) {
            throw new IllegalStateException("Duplicate upgrade consume rule registration: " + rule.id());
        }
        return rule;
    }

    public static Optional<UpgradeConsumeRule> unregisterConsumeRule(ResourceLocation id) {
        XLibRegistryGuard.ensureMutable("upgrade_consume_rules");
        return Optional.ofNullable(CONSUME_RULES.remove(id));
    }

    public static Collection<UpgradeConsumeRule> allConsumeRules() {
        return List.copyOf(CONSUME_RULES.values());
    }

    public static UpgradeKillRule registerKillRule(UpgradeKillRule rule) {
        XLibRegistryGuard.ensureMutable("upgrade_kill_rules");
        UpgradeKillRule previous = KILL_RULES.putIfAbsent(rule.id(), rule);
        if (previous != null) {
            throw new IllegalStateException("Duplicate upgrade kill rule registration: " + rule.id());
        }
        return rule;
    }

    public static Optional<UpgradeKillRule> unregisterKillRule(ResourceLocation id) {
        XLibRegistryGuard.ensureMutable("upgrade_kill_rules");
        return Optional.ofNullable(KILL_RULES.remove(id));
    }

    public static Collection<UpgradeKillRule> allKillRules() {
        return List.copyOf(KILL_RULES.values());
    }

    public static UpgradeProgressData createDefaultData() {
        return UpgradeProgressData.empty();
    }

    public static UpgradeProgressData sanitizeData(UpgradeProgressData data) {
        UpgradeProgressData sanitized = data;
        boolean changed;
        do {
            changed = false;
            for (ResourceLocation nodeId : Set.copyOf(sanitized.unlockedNodes())) {
                UpgradeNodeDefinition node = findNode(nodeId).orElse(null);
                if (node == null || !sanitized.unlockedNodes().containsAll(node.requiredNodes())) {
                    sanitized = sanitized.withUnlockedNode(nodeId, false);
                    for (ResourceLocation sourceId : Set.copyOf(sanitized.unlockSourcesFor(nodeId))) {
                        sanitized = sanitized.withUnlockedNodeSource(nodeId, sourceId, false);
                    }
                    changed = true;
                }
            }
        } while (changed);
        return sanitized;
    }

    public static UpgradeProgressData get(Player player) {
        return ModAttachments.getProgression(player);
    }

    public static void set(Player player, UpgradeProgressData data) {
        ModAttachments.setProgression(player, sanitizeData(data));
    }

    public static Map<ResourceLocation, Integer> pointBalances(Player player) {
        return Map.copyOf(get(player).pointBalances());
    }

    public static Map<ResourceLocation, Integer> counters(Player player) {
        return Map.copyOf(get(player).counters());
    }

    public static Set<ResourceLocation> activeRewardSources(Player player) {
        return activeRewardSources(get(player));
    }

    public static Set<ResourceLocation> activeRewardSources(UpgradeProgressData data) {
        Set<ResourceLocation> activeSources = new LinkedHashSet<>();
        for (ResourceLocation nodeId : data.unlockedNodes()) {
            activeSources.add(sourceIdFor(nodeId));
        }
        return Set.copyOf(activeSources);
    }

    public static Set<ResourceLocation> activeManagedUnlockSources(UpgradeProgressData data) {
        Set<ResourceLocation> activeSources = new LinkedHashSet<>();
        data.managedUnlockSources().values().forEach(activeSources::addAll);
        return Set.copyOf(activeSources);
    }

    public static Set<ResourceLocation> unlockedNodes(Player player) {
        return Set.copyOf(get(player).unlockedNodes());
    }

    public static int points(Player player, ResourceLocation pointTypeId) {
        return get(player).points(pointTypeId);
    }

    public static int counter(Player player, ResourceLocation counterId) {
        return get(player).counter(counterId);
    }

    public static void addPoints(Player player, ResourceLocation pointTypeId, int amount) {
        if (amount == 0) {
            return;
        }
        set(player, get(player).addPoints(pointTypeId, amount));
    }

    public static void addCounter(Player player, ResourceLocation counterId, int amount) {
        if (amount == 0) {
            return;
        }
        set(player, get(player).addCounter(counterId, amount));
    }

    public static Optional<Component> firstUnlockFailure(
            @Nullable ServerPlayer player,
            UpgradeProgressData data,
            UpgradeNodeDefinition node
    ) {
        Optional<Component> structuralFailure = firstStructuralUnlockFailure(data, node);
        if (structuralFailure.isPresent()) {
            return structuralFailure;
        }

        return UpgradeRequirements.firstFailure(player, data, node.requirements());
    }

    public static Optional<Component> firstStructuralUnlockFailure(
            UpgradeProgressData data,
            UpgradeNodeDefinition node
    ) {
        if (data.hasUnlockedNode(node.id())) {
            return Optional.of(Component.translatable("message.xlib.upgrade.node_already_unlocked", node.displayName()));
        }

        Optional<NodeConflict> blockingNode = blockingNode(data, node);
        if (blockingNode.isPresent()) {
            Component blockingNodeName = UpgradeRequirements.displayNodeName(blockingNode.get().nodeId());
            return switch (blockingNode.get().kind()) {
                case CHOICE_GROUP -> Optional.of(Component.translatable(
                        "message.xlib.upgrade.choice_locked",
                        node.displayName(),
                        blockingNodeName
                ));
                case NODE_LOCK -> Optional.of(Component.translatable(
                        "message.xlib.upgrade.node_locked_by_node",
                        node.displayName(),
                        blockingNodeName
                ));
                case TRACK_LOCK -> Optional.of(Component.translatable(
                        "message.xlib.upgrade.track_locked_by_node",
                        node.displayName(),
                        blockingNodeName
                ));
            };
        }

        if (node.trackId() != null) {
            Optional<ResourceLocation> blockingTrackId = blockingTrack(data, node.trackId());
            if (blockingTrackId.isPresent()) {
                return Optional.of(Component.translatable(
                        "message.xlib.upgrade.track_locked",
                        node.displayName(),
                        displayTrackName(blockingTrackId.get())
                ));
            }
        }

        for (ResourceLocation requiredNodeId : node.requiredNodes()) {
            if (!data.hasUnlockedNode(requiredNodeId)) {
                return Optional.of(Component.translatable(
                        "message.xlib.upgrade.node_missing_prerequisite",
                        node.displayName(),
                        UpgradeRequirements.displayNodeName(requiredNodeId)
                ));
            }
        }

        for (Map.Entry<ResourceLocation, Integer> entry : node.pointCosts().entrySet()) {
            if (data.points(entry.getKey()) < entry.getValue()) {
                Component pointName = findPointType(entry.getKey())
                        .map(UpgradePointType::displayName)
                        .orElse(Component.literal(entry.getKey().toString()));
                return Optional.of(Component.translatable(
                        "message.xlib.upgrade.node_missing_points",
                        node.displayName(),
                        entry.getValue(),
                        pointName
                ));
            }
        }

        return Optional.empty();
    }

    public static boolean unlockNode(ServerPlayer player, ResourceLocation nodeId) {
        UpgradeNodeDefinition node = findNode(nodeId).orElse(null);
        if (node == null) {
            return false;
        }

        UpgradeProgressData currentData = get(player);
        Optional<Component> failure = firstUnlockFailure(player, currentData, node);
        if (failure.isPresent()) {
            player.displayClientMessage(failure.get(), true);
            return false;
        }

        UpgradeProgressData updatedData = currentData;
        for (Map.Entry<ResourceLocation, Integer> entry : node.pointCosts().entrySet()) {
            updatedData = updatedData.addPoints(entry.getKey(), -entry.getValue());
        }
        updatedData = updatedData.withUnlockedNode(node.id(), true);
        set(player, updatedData);
        syncServerState(player, currentData);
        player.displayClientMessage(Component.translatable("message.xlib.upgrade.node_unlocked", node.displayName()), true);
        return true;
    }

    public static boolean revokeNode(ServerPlayer player, ResourceLocation nodeId) {
        UpgradeProgressData currentData = get(player);
        if (!currentData.hasUnlockedNode(nodeId)) {
            return false;
        }

        set(player, currentData.withUnlockedNode(nodeId, false));
        syncServerState(player, currentData);
        return true;
    }

    public static void syncSourceNodes(ServerPlayer player, ResourceLocation sourceId, Collection<ResourceLocation> nodeIds) {
        UpgradeProgressData currentData = get(player);
        Set<ResourceLocation> desiredNodeIds = nodeIds.stream()
                .filter(java.util.Objects::nonNull)
                .filter(nodeId -> findNode(nodeId).isPresent())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        UpgradeProgressData updatedData = currentData;
        for (ResourceLocation nodeId : currentData.unlockedNodes()) {
            if (currentData.unlockSourcesFor(nodeId).contains(sourceId) && !desiredNodeIds.contains(nodeId)) {
                updatedData = updatedData.withUnlockedNodeSource(nodeId, sourceId, false);
            }
        }
        for (ResourceLocation nodeId : desiredNodeIds) {
            updatedData = updatedData.withUnlockedNodeSource(nodeId, sourceId, true);
        }
        updatedData = sanitizeData(updatedData);
        if (!updatedData.equals(currentData)) {
            set(player, updatedData);
            syncServerState(player, currentData);
        }
    }

    public static boolean revokeNodesGrantingAbility(ServerPlayer player, ResourceLocation abilityId) {
        UpgradeProgressData currentData = get(player);
        UpgradeProgressData updatedData = revokeAbilityRewardNodes(currentData, AbilityGrantApi.grantSources(player, abilityId), abilityId);
        if (updatedData.equals(currentData)) {
            return false;
        }

        set(player, updatedData);
        syncServerState(player, currentData);
        return true;
    }

    public static boolean revokeTrack(ServerPlayer player, ResourceLocation trackId) {
        if (findTrack(trackId).isEmpty()) {
            return false;
        }

        UpgradeProgressData currentData = get(player);
        UpgradeProgressData updatedData = revokeTrackNodes(currentData, trackId);
        if (updatedData.equals(currentData)) {
            return false;
        }

        set(player, updatedData);
        syncServerState(player, currentData);
        return true;
    }

    public static boolean clearProgress(ServerPlayer player) {
        UpgradeProgressData currentData = get(player);
        if (currentData.equals(UpgradeProgressData.empty())) {
            return false;
        }

        set(player, UpgradeProgressData.empty());
        syncServerState(player, currentData);
        return true;
    }

    public static void syncServerState(ServerPlayer player) {
        syncServerState(player, get(player));
    }

    public static void syncServerState(ServerPlayer player, UpgradeProgressData previousData) {
        UpgradeProgressData currentData = sanitizeData(get(player));
        if (!currentData.equals(get(player))) {
            ModAttachments.setProgression(player, currentData);
        }

        for (UpgradeNodeDefinition node : allNodes()) {
            if (currentData.hasUnlockedNode(node.id())) {
                syncNodeRewards(player, node);
            } else {
                clearNodeRewards(player, node, node.sourceId());
            }
        }

        for (ResourceLocation previousNodeId : previousData.unlockedNodes()) {
            if (!currentData.hasUnlockedNode(previousNodeId)) {
                UpgradeNodeDefinition node = findNode(previousNodeId).orElse(null);
                if (node != null) {
                    clearNodeRewards(player, node, sourceIdFor(previousNodeId));
                }
            }
        }
        pruneUnusedManagedSources(player, currentData);
    }

    public static void onItemConsumed(ServerPlayer player, ItemStack usedStack) {
        AbilityData abilityData = ModAttachments.get(player);
        UpgradeProgressData currentData = get(player);
        UpgradeProgressData updatedData = currentData;
        for (UpgradeConsumeRule rule : allConsumeRules()) {
            if (rule.matches(player, abilityData, usedStack)) {
                updatedData = applyRuleRewards(updatedData, rule.pointRewards(), rule.counterRewards());
            }
        }
        if (!updatedData.equals(currentData)) {
            set(player, updatedData);
        }
    }

    public static void onKill(ServerPlayer player, LivingEntity target) {
        Optional<ResourceLocation> attributedAbilityId = AbilityCombatTracker.recentKillingAbility(player, target);
        UpgradeProgressData currentData = get(player);
        UpgradeProgressData updatedData = currentData;
        for (UpgradeKillRule rule : allKillRules()) {
            if (rule.matches(player, target, attributedAbilityId)) {
                updatedData = applyRuleRewards(updatedData, rule.pointRewards(), rule.counterRewards());
            }
        }
        AbilityCombatTracker.clearTarget(target);
        if (!updatedData.equals(currentData)) {
            set(player, updatedData);
        }
    }

    private static UpgradeProgressData applyRuleRewards(
            UpgradeProgressData currentData,
            Map<ResourceLocation, Integer> pointRewards,
            Map<ResourceLocation, Integer> counterRewards
    ) {
        UpgradeProgressData updatedData = currentData;
        for (Map.Entry<ResourceLocation, Integer> entry : pointRewards.entrySet()) {
            updatedData = updatedData.addPoints(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<ResourceLocation, Integer> entry : counterRewards.entrySet()) {
            updatedData = updatedData.addCounter(entry.getKey(), entry.getValue());
        }
        return updatedData;
    }

    private static void syncNodeRewards(ServerPlayer player, UpgradeNodeDefinition node) {
        UpgradeRewardBundle rewards = node.rewards();
        AbilityGrantApi.syncSourceAbilities(player, node.sourceId(), rewards.abilities());
        PassiveGrantApi.syncSourcePassives(player, node.sourceId(), rewards.passives());
        GrantedItemGrantApi.syncSourceItems(player, node.sourceId(), rewards.grantedItems());
        RecipePermissionApi.syncSourcePermissions(player, node.sourceId(), rewards.recipePermissions());
        IdentityApi.grantIdentities(player, rewards.identities(), node.sourceId());
        NeoForge.EVENT_BUS.post(new XLibUpgradeRewardProjectionEvent.Projected(player, node, node.sourceId(), rewards));
    }

    private static void clearNodeRewards(ServerPlayer player, UpgradeNodeDefinition node, ResourceLocation sourceId) {
        AbilityGrantApi.syncSourceAbilities(player, sourceId, List.of());
        PassiveGrantApi.syncSourcePassives(player, sourceId, List.of());
        GrantedItemGrantApi.syncSourceItems(player, sourceId, List.of());
        RecipePermissionApi.syncSourcePermissions(player, sourceId, List.of());
        IdentityApi.clearSourceIdentities(player, sourceId);
        NeoForge.EVENT_BUS.post(new XLibUpgradeRewardProjectionEvent.Cleared(player, node, sourceId, node.rewards()));
    }

    private static ResourceLocation sourceIdFor(ResourceLocation nodeId) {
        return ResourceLocation.fromNamespaceAndPath(
                XLib.MODID,
                "upgrade_node/" + nodeId.getNamespace() + "/" + nodeId.getPath()
        );
    }

    private static void pruneUnusedManagedSources(ServerPlayer player, UpgradeProgressData currentData) {
        Set<ResourceLocation> activeSources = activeRewardSources(currentData);
        AbilityData currentAbilityData = ModAttachments.get(player);
        AbilityData updatedAbilityData = currentAbilityData;
        for (ResourceLocation sourceId : Set.copyOf(currentAbilityData.managedGrantSources())) {
            if (!sourceId.getNamespace().equals(XLib.MODID) || !sourceId.getPath().startsWith("upgrade_node/")) {
                continue;
            }
            if (!activeSources.contains(sourceId)) {
                updatedAbilityData = updatedAbilityData.withManagedGrantSource(sourceId, false);
            }
        }
        if (!updatedAbilityData.equals(currentAbilityData)) {
            ModAttachments.set(player, updatedAbilityData);
        }
    }

    public static boolean isTrackBlocked(UpgradeProgressData data, ResourceLocation trackId) {
        return blockingTrack(data, trackId).isPresent()
                || allNodes().stream().anyMatch(node -> data.hasUnlockedNode(node.id()) && node.lockedTracks().contains(trackId));
    }

    public static boolean trackHasUnlockedNodes(UpgradeProgressData data, ResourceLocation trackId) {
        for (UpgradeNodeDefinition node : allNodes()) {
            if (trackId.equals(node.trackId()) && data.hasUnlockedNode(node.id())) {
                return true;
            }
        }
        return false;
    }

    public static boolean trackCompleted(UpgradeProgressData data, ResourceLocation trackId) {
        List<UpgradeNodeDefinition> nodes = nodesInTrack(trackId);
        return !nodes.isEmpty() && nodes.stream().allMatch(node -> data.hasUnlockedNode(node.id()));
    }

    static UpgradeProgressData revokeTrackNodes(UpgradeProgressData data, ResourceLocation trackId) {
        UpgradeProgressData updatedData = data;
        boolean changed = false;
        for (UpgradeNodeDefinition node : allNodes()) {
            if (trackId.equals(node.trackId()) && updatedData.hasUnlockedNode(node.id())) {
                updatedData = updatedData.withUnlockedNode(node.id(), false);
                changed = true;
            }
        }
        return changed ? sanitizeData(updatedData) : data;
    }

    static UpgradeProgressData revokeAbilityRewardNodes(
            UpgradeProgressData data,
            Collection<ResourceLocation> activeGrantSources,
            ResourceLocation abilityId
    ) {
        Set<ResourceLocation> activeSourceSet = Set.copyOf(activeGrantSources);
        if (activeSourceSet.isEmpty()) {
            return data;
        }

        UpgradeProgressData updatedData = data;
        boolean changed = false;
        for (UpgradeNodeDefinition node : allNodes()) {
            if (!updatedData.hasUnlockedNode(node.id())) {
                continue;
            }
            if (!activeSourceSet.contains(node.sourceId())) {
                continue;
            }
            if (!node.rewards().abilities().contains(abilityId)) {
                continue;
            }
            updatedData = updatedData.withUnlockedNode(node.id(), false);
            changed = true;
        }
        return changed ? sanitizeData(updatedData) : data;
    }

    private static Optional<ResourceLocation> blockingTrack(UpgradeProgressData data, ResourceLocation trackId) {
        UpgradeTrackDefinition track = findTrack(trackId).orElse(null);
        if (track == null) {
            return Optional.empty();
        }

        for (ResourceLocation exclusiveTrackId : track.exclusiveTracks()) {
            if (trackHasUnlockedNodes(data, exclusiveTrackId)) {
                return Optional.of(exclusiveTrackId);
            }
        }
        return Optional.empty();
    }

    public static Component displayTrackName(ResourceLocation trackId) {
        return findTrack(trackId)
                .map(UpgradeTrackDefinition::displayName)
                .orElse(Component.literal(trackId.toString()));
    }

    private static Collection<UpgradeTrackDefinition> filterTracks(Predicate<UpgradeTrackDefinition> predicate) {
        return TRACKS.values().stream().filter(predicate).toList();
    }

    private static Collection<UpgradeNodeDefinition> filterNodes(Predicate<UpgradeNodeDefinition> predicate) {
        return NODES.values().stream().filter(predicate).toList();
    }

    private static Optional<NodeConflict> blockingNode(UpgradeProgressData data, UpgradeNodeDefinition node) {
        for (UpgradeNodeDefinition unlockedNode : allNodes()) {
            if (unlockedNode.id().equals(node.id()) || !data.hasUnlockedNode(unlockedNode.id())) {
                continue;
            }
            if (node.choiceGroupId().isPresent()
                    && unlockedNode.choiceGroupId().filter(node.choiceGroupId().get()::equals).isPresent()) {
                return Optional.of(new NodeConflict(unlockedNode.id(), NodeConflictKind.CHOICE_GROUP));
            }
            if (unlockedNode.lockedNodes().contains(node.id()) || node.lockedNodes().contains(unlockedNode.id())) {
                return Optional.of(new NodeConflict(unlockedNode.id(), NodeConflictKind.NODE_LOCK));
            }
            if ((node.trackId() != null && unlockedNode.lockedTracks().contains(node.trackId()))
                    || (unlockedNode.trackId() != null && node.lockedTracks().contains(unlockedNode.trackId()))) {
                return Optional.of(new NodeConflict(unlockedNode.id(), NodeConflictKind.TRACK_LOCK));
            }
        }
        return Optional.empty();
    }

    private enum NodeConflictKind {
        CHOICE_GROUP,
        NODE_LOCK,
        TRACK_LOCK
    }

    private record NodeConflict(ResourceLocation nodeId, NodeConflictKind kind) {}
}
