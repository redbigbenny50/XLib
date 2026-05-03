package com.whatxe.xlib.ability;

import com.whatxe.xlib.XLib;
import com.whatxe.xlib.XLibRegistryGuard;
import com.whatxe.xlib.api.event.XLibProfileOnboardingCompletedEvent;
import com.whatxe.xlib.api.event.XLibProfileResetEvent;
import com.whatxe.xlib.api.event.XLibProfileSelectionClaimEvent;
import com.whatxe.xlib.attachment.ModAttachments;
import com.whatxe.xlib.menu.AbilityMenuAccessApi;
import com.whatxe.xlib.menu.AbilityMenuAccessPolicy;
import com.whatxe.xlib.menu.MenuAccessDecision;
import com.whatxe.xlib.menu.MenuAccessRequirement;
import com.whatxe.xlib.menu.ProgressionMenuAccessApi;
import com.whatxe.xlib.menu.ProgressionMenuAccessPolicy;
import com.whatxe.xlib.progression.UpgradeApi;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

public final class ProfileApi {
    private static final String PROFILE_SOURCE_PATH_PREFIX = "profile/";
    private static final ResourceLocation ABILITY_MENU_POLICY_SOURCE =
            ResourceLocation.fromNamespaceAndPath(XLib.MODID, "profile_ability_menu_access");
    private static final ResourceLocation PROGRESSION_MENU_POLICY_SOURCE =
            ResourceLocation.fromNamespaceAndPath(XLib.MODID, "profile_progression_menu_access");

    private static final Map<ResourceLocation, ProfileGroupDefinition> GROUPS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, ProfileDefinition> PROFILES = new LinkedHashMap<>();
    private static boolean bootstrapped;

    private ProfileApi() {}

    public static void bootstrap() {
        if (bootstrapped) {
            return;
        }
        bootstrapped = true;
        AbilityMenuAccessApi.registerPolicy(
                ABILITY_MENU_POLICY_SOURCE,
                AbilityMenuAccessPolicy.builder()
                        .availableWhen(MenuAccessRequirement.of(
                                Component.translatable("message.xlib.profile_selection_required_menu", Component.literal("-")),
                                (player, data) -> firstAbilityMenuFailure(player)
                        ))
                        .build()
        );
        ProgressionMenuAccessApi.registerPolicy(
                PROGRESSION_MENU_POLICY_SOURCE,
                ProgressionMenuAccessPolicy.builder()
                        .availableWhen(MenuAccessRequirement.of(
                                Component.translatable("message.xlib.profile_selection_required_progression", Component.literal("-")),
                                (player, data) -> firstProgressionFailure(player)
                        ))
                        .build()
        );
    }

    public static ProfileGroupDefinition registerGroup(ProfileGroupDefinition group) {
        XLibRegistryGuard.ensureMutable("profile_groups");
        ProfileGroupDefinition previous = GROUPS.putIfAbsent(group.id(), group);
        if (previous != null) {
            throw new IllegalStateException("Duplicate profile group registration: " + group.id());
        }
        return group;
    }

    public static Optional<ProfileGroupDefinition> unregisterGroup(ResourceLocation groupId) {
        XLibRegistryGuard.ensureMutable("profile_groups");
        return Optional.ofNullable(GROUPS.remove(groupId));
    }

    public static Optional<ProfileGroupDefinition> findGroup(ResourceLocation groupId) {
        return Optional.ofNullable(GROUPS.get(groupId));
    }

    public static Collection<ProfileGroupDefinition> allGroups() {
        return List.copyOf(GROUPS.values());
    }

    public static ProfileDefinition registerProfile(ProfileDefinition profile) {
        XLibRegistryGuard.ensureMutable("profiles");
        ProfileDefinition previous = PROFILES.putIfAbsent(profile.id(), profile);
        if (previous != null) {
            throw new IllegalStateException("Duplicate profile registration: " + profile.id());
        }
        return profile;
    }

    public static Optional<ProfileDefinition> unregisterProfile(ResourceLocation profileId) {
        XLibRegistryGuard.ensureMutable("profiles");
        return Optional.ofNullable(PROFILES.remove(profileId));
    }

    public static Optional<ProfileDefinition> findProfile(ResourceLocation profileId) {
        return Optional.ofNullable(PROFILES.get(profileId));
    }

    public static Collection<ProfileDefinition> allProfiles() {
        return List.copyOf(PROFILES.values());
    }

    public static ProfileSelectionData createDefaultData() {
        return ProfileSelectionData.empty();
    }

    public static ProfileSelectionData sanitizeData(ProfileSelectionData data) {
        if (data == null) {
            return ProfileSelectionData.empty();
        }

        Map<ResourceLocation, ProfileSelectionEntry> sanitizedSelections = new LinkedHashMap<>();
        Map<ResourceLocation, Integer> selectionsPerGroup = new LinkedHashMap<>();
        for (ResourceLocation profileId : data.selectedProfileIds()) {
            ProfileDefinition profile = PROFILES.get(profileId);
            ProfileSelectionEntry entry = data.selection(profileId).orElse(null);
            if (profile == null || entry == null || !profile.groupId().equals(entry.groupId())) {
                continue;
            }
            if (selectionConflictsWithExisting(sanitizedSelections.keySet(), profileId)) {
                continue;
            }
            ProfileGroupDefinition group = GROUPS.get(entry.groupId());
            if (group == null) {
                continue;
            }
            int count = selectionsPerGroup.getOrDefault(entry.groupId(), 0);
            if (count >= group.selectionLimit()) {
                continue;
            }
            sanitizedSelections.put(profileId, entry);
            selectionsPerGroup.put(entry.groupId(), count + 1);
        }

        Map<ResourceLocation, ProfilePendingSelection> sanitizedPending = new LinkedHashMap<>();
        for (ResourceLocation groupId : GROUPS.keySet()) {
            ProfileGroupDefinition group = GROUPS.get(groupId);
            ProfilePendingSelection pending = data.pendingGroup(groupId).orElse(null);
            if (group == null || pending == null || !group.requiredOnboarding()) {
                continue;
            }
            if (selectedProfilesInGroup(sanitizedSelections.keySet(), groupId).size() < group.selectionLimit()) {
                sanitizedPending.put(groupId, pending);
            }
        }

        return new ProfileSelectionData(
                sanitizedSelections,
                sanitizedPending,
                data.resetCounts(),
                data.lastResetReasons(),
                data.firstLoginHandled()
        );
    }

    public static ProfileSelectionData get(Player player) {
        return ModAttachments.getProfiles(player);
    }

    public static void set(Player player, ProfileSelectionData data) {
        ModAttachments.setProfiles(player, sanitizeData(data));
    }

    public static List<ResourceLocation> selectedProfilesInGroup(ProfileSelectionData data, ResourceLocation groupId) {
        return selectedProfilesInGroup(data.selectedProfileIds(), groupId);
    }

    public static Set<ResourceLocation> pendingGroups(ProfileSelectionData data) {
        return data.pendingGroupIds();
    }

    public static Optional<ResourceLocation> firstPendingGroupId(ProfileSelectionData data) {
        for (ResourceLocation groupId : GROUPS.keySet()) {
            if (data.hasPendingGroup(groupId)) {
                return Optional.of(groupId);
            }
        }
        return Optional.empty();
    }

    public static Optional<ResourceLocation> firstAutoOpenPendingGroupId(ProfileSelectionData data) {
        for (ResourceLocation groupId : GROUPS.keySet()) {
            if (!data.hasPendingGroup(groupId)) {
                continue;
            }
            ProfileGroupDefinition group = GROUPS.get(groupId);
            if (group != null && group.autoOpenMenu()) {
                return Optional.of(groupId);
            }
        }
        return Optional.empty();
    }

    public static ResourceLocation sourceIdFor(ResourceLocation profileId) {
        return ResourceLocation.fromNamespaceAndPath(
                XLib.MODID,
                PROFILE_SOURCE_PATH_PREFIX + profileId.getNamespace() + "/" + profileId.getPath()
        );
    }

    public static Optional<ResourceLocation> parseSourceId(ResourceLocation sourceId) {
        if (!XLib.MODID.equals(sourceId.getNamespace()) || !sourceId.getPath().startsWith(PROFILE_SOURCE_PATH_PREFIX)) {
            return Optional.empty();
        }
        String suffix = sourceId.getPath().substring(PROFILE_SOURCE_PATH_PREFIX.length());
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

    public static Optional<Component> firstAbilityUseFailure(@Nullable Player player) {
        return firstPendingFailure(player, ProfileGroupDefinition::blocksAbilityUse, "message.xlib.profile_selection_required_use");
    }

    public static Optional<Component> firstAbilityMenuFailure(@Nullable Player player) {
        return firstPendingFailure(player, ProfileGroupDefinition::blocksAbilityMenu, "message.xlib.profile_selection_required_menu");
    }

    public static Optional<Component> firstProgressionFailure(@Nullable Player player) {
        return firstPendingFailure(player, ProfileGroupDefinition::blocksProgression, "message.xlib.profile_selection_required_progression");
    }

    public static boolean evaluateOnboarding(ServerPlayer player, ProfileOnboardingTrigger trigger) {
        return evaluateOnboarding(player, trigger, "");
    }

    public static boolean evaluateOnboarding(ServerPlayer player, ProfileOnboardingTrigger trigger, String reason) {
        ProfileSelectionData currentData = sanitizeData(get(player));
        ProfileSelectionData updatedData = currentData;
        for (ProfileGroupDefinition group : GROUPS.values()) {
            if (!group.requiredOnboarding() || !group.onboardingTriggers().contains(trigger)) {
                continue;
            }
            if (selectedProfilesInGroup(updatedData, group.id()).size() >= group.selectionLimit()) {
                updatedData = updatedData.clearPendingGroup(group.id());
                continue;
            }
            updatedData = updatedData.withPendingGroup(group.id(), new ProfilePendingSelection(trigger, reason));
        }
        if (trigger == ProfileOnboardingTrigger.FIRST_LOGIN) {
            updatedData = updatedData.withFirstLoginHandled(true);
        }
        if (!updatedData.equals(currentData)) {
            set(player, updatedData);
            return true;
        }
        return false;
    }

    public static void markPendingGroup(ServerPlayer player, ResourceLocation groupId, ProfileOnboardingTrigger trigger, String reason) {
        ProfileGroupDefinition group = GROUPS.get(groupId);
        if (group == null || selectedProfilesInGroup(get(player), groupId).size() >= group.selectionLimit()) {
            return;
        }
        set(player, get(player).withPendingGroup(groupId, new ProfilePendingSelection(trigger, reason)));
    }

    public static void clearPendingGroup(ServerPlayer player, ResourceLocation groupId) {
        set(player, get(player).clearPendingGroup(groupId));
    }

    public static Optional<Component> claimPendingProfile(ServerPlayer player, ResourceLocation profileId) {
        return claimProfile(player, profileId, ProfileSelectionOrigin.PLAYER, false, "", true);
    }

    public static Optional<Component> claimProfile(
            ServerPlayer player,
            ResourceLocation profileId,
            ProfileSelectionOrigin origin,
            boolean locked,
            String reason
    ) {
        return claimProfile(player, profileId, origin, locked, reason, false);
    }

    public static Optional<Component> resetGroup(ServerPlayer player, ResourceLocation groupId, boolean admin, String reason) {
        ProfileGroupDefinition group = GROUPS.get(groupId);
        if (group == null) {
            return Optional.of(Component.translatable("command.xlib.profiles.group_invalid", groupId.toString()));
        }

        ProfileSelectionData currentData = sanitizeData(get(player));
        List<ResourceLocation> selectedProfiles = selectedProfilesInGroup(currentData, groupId);
        if (selectedProfiles.isEmpty()) {
            return Optional.of(Component.translatable("message.xlib.profile_group_empty", group.displayName()));
        }
        if (!admin && !group.playerCanReset()) {
            return Optional.of(Component.translatable("message.xlib.profile_reset_forbidden", group.displayName()));
        }
        if (admin && !group.adminCanReset()) {
            return Optional.of(Component.translatable("message.xlib.profile_admin_reset_forbidden", group.displayName()));
        }
        if (!admin && selectedProfiles.stream()
                .map(currentData::selection)
                .flatMap(Optional::stream)
                .anyMatch(ProfileSelectionEntry::locked)) {
            return Optional.of(Component.translatable("message.xlib.profile_reset_locked", group.displayName()));
        }

        ProfileSelectionData updatedData = currentData.clearGroupSelections(groupId)
                .clearPendingGroup(groupId)
                .withResetCount(groupId, currentData.resetCount(groupId) + 1)
                .withLastResetReason(groupId, reason);
        boolean onboardingReopened = false;
        if (group.requiredOnboarding() && group.reopenOnReset()) {
            updatedData = updatedData.withPendingGroup(groupId, new ProfilePendingSelection(ProfileOnboardingTrigger.COMMAND, reason));
            onboardingReopened = true;
        }
        set(player, updatedData);
        rebuild(player);
        NeoForge.EVENT_BUS.post(new XLibProfileResetEvent(
                player,
                groupId,
                admin,
                reason,
                updatedData.resetCount(groupId),
                onboardingReopened
        ));
        return Optional.empty();
    }

    public static void rebuild(ServerPlayer player) {
        ProfileSelectionData sanitizedData = sanitizeData(get(player));
        if (!sanitizedData.equals(get(player))) {
            set(player, sanitizedData);
        }

        Set<ResourceLocation> profileSourceIds = new LinkedHashSet<>(activeProfileSources(ModAttachments.get(player)));
        profileSourceIds.addAll(UpgradeApi.activeManagedUnlockSources(UpgradeApi.get(player)).stream()
                .filter(sourceId -> parseSourceId(sourceId).isPresent())
                .toList());
        for (ResourceLocation profileId : sanitizedData.selectedProfileIds()) {
            profileSourceIds.add(sourceIdFor(profileId));
        }

        for (ResourceLocation sourceId : profileSourceIds) {
            clearProfileSource(player, sourceId);
        }
        for (ResourceLocation profileId : sanitizedData.selectedProfileIds()) {
            applyProfileSource(player, profileId);
        }
    }

    private static Optional<Component> claimProfile(
            ServerPlayer player,
            ResourceLocation profileId,
            ProfileSelectionOrigin origin,
            boolean locked,
            String reason,
            boolean requirePending
    ) {
        ProfileDefinition profile = PROFILES.get(profileId);
        if (profile == null) {
            return Optional.of(Component.translatable("command.xlib.profiles.invalid", profileId.toString()));
        }
        ProfileGroupDefinition group = GROUPS.get(profile.groupId());
        if (group == null) {
            return Optional.of(Component.translatable("command.xlib.profiles.group_invalid", profile.groupId().toString()));
        }

        ProfileSelectionData currentData = sanitizeData(get(player));
        ProfilePendingSelection pendingSelection = currentData.pendingGroup(group.id()).orElse(null);
        if (requirePending && !currentData.hasPendingGroup(group.id())) {
            return Optional.of(Component.translatable("message.xlib.profile_pending_missing", group.displayName()));
        }

        List<ResourceLocation> selectedInGroup = selectedProfilesInGroup(currentData, group.id());
        if (selectedInGroup.contains(profileId)) {
            if (selectedInGroup.size() >= group.selectionLimit()) {
                set(player, currentData.clearPendingGroup(group.id()));
            }
            return Optional.empty();
        }
        if (selectedInGroup.size() >= group.selectionLimit()) {
            return Optional.of(Component.translatable("message.xlib.profile_group_full", group.displayName()));
        }
        for (ResourceLocation selectedProfileId : currentData.selectedProfileIds()) {
            ProfileDefinition selectedProfile = PROFILES.get(selectedProfileId);
            if (selectedProfile == null) {
                continue;
            }
            if (profile.incompatibleWith(selectedProfileId) || selectedProfile.incompatibleWith(profileId)) {
                return Optional.of(Component.translatable(
                        "message.xlib.profile_incompatible",
                        profile.displayName(),
                        selectedProfile.displayName()
                ));
            }
        }

        ProfileSelectionData updatedData = currentData.withSelection(
                profileId,
                new ProfileSelectionEntry(group.id(), origin, locked, reason)
        );
        if (selectedProfilesInGroup(updatedData, group.id()).size() >= group.selectionLimit()) {
            updatedData = updatedData.clearPendingGroup(group.id());
        }
        set(player, updatedData);
        applyProfileSource(player, profileId);
        NeoForge.EVENT_BUS.post(new XLibProfileSelectionClaimEvent(
                player,
                group.id(),
                profileId,
                origin,
                locked,
                reason,
                requirePending
        ));
        if (pendingSelection != null && !updatedData.hasPendingGroup(group.id())) {
            NeoForge.EVENT_BUS.post(new XLibProfileOnboardingCompletedEvent(
                    player,
                    group.id(),
                    profileId,
                    pendingSelection.trigger(),
                    pendingSelection.reason()
            ));
        }
        return Optional.empty();
    }

    private static Optional<Component> firstPendingFailure(
            @Nullable Player player,
            java.util.function.Predicate<ProfileGroupDefinition> blockPredicate,
            String translationKey
    ) {
        if (player == null) {
            return Optional.empty();
        }
        ProfileSelectionData data = sanitizeData(get(player));
        for (ResourceLocation groupId : GROUPS.keySet()) {
            if (!data.hasPendingGroup(groupId)) {
                continue;
            }
            ProfileGroupDefinition group = GROUPS.get(groupId);
            if (group != null && blockPredicate.test(group)) {
                return Optional.of(Component.translatable(translationKey, group.displayName()));
            }
        }
        return Optional.empty();
    }

    private static void applyProfileSource(ServerPlayer player, ResourceLocation profileId) {
        ProfileDefinition profile = PROFILES.get(profileId);
        if (profile == null) {
            return;
        }
        ResourceLocation sourceId = sourceIdFor(profileId);
        Set<ResourceLocation> grantedAbilities = new LinkedHashSet<>(profile.abilities());
        grantedAbilities.addAll(profile.modes());
        AbilityGrantApi.syncSourceAbilities(player, sourceId, grantedAbilities);
        PassiveGrantApi.syncSourcePassives(player, sourceId, profile.passives());
        GrantedItemGrantApi.syncSourceItems(player, sourceId, profile.grantedItems());
        RecipePermissionApi.syncSourcePermissions(player, sourceId, profile.recipePermissions());
        StateFlagApi.syncSourceFlags(player, sourceId, profile.stateFlags());
        GrantBundleApi.syncSourceBundles(player, sourceId, profile.grantBundles());
        IdentityApi.syncSourceIdentities(player, sourceId, profile.identities());
        ArtifactApi.syncUnlockSourceArtifacts(player, sourceId, profile.unlockedArtifacts());
        UpgradeApi.syncSourceNodes(player, sourceId, profile.startingNodes());
    }

    private static void clearProfileSource(ServerPlayer player, ResourceLocation sourceId) {
        AbilityGrantApi.syncSourceAbilities(player, sourceId, List.of());
        PassiveGrantApi.syncSourcePassives(player, sourceId, List.of());
        GrantedItemGrantApi.syncSourceItems(player, sourceId, List.of());
        RecipePermissionApi.syncSourcePermissions(player, sourceId, List.of());
        StateFlagApi.syncSourceFlags(player, sourceId, List.of());
        GrantBundleApi.syncSourceBundles(player, sourceId, List.of());
        IdentityApi.syncSourceIdentities(player, sourceId, List.of());
        ArtifactApi.syncUnlockSourceArtifacts(player, sourceId, List.of());
        UpgradeApi.syncSourceNodes(player, sourceId, List.of());
    }

    private static Set<ResourceLocation> activeProfileSources(AbilityData data) {
        Set<ResourceLocation> sources = new LinkedHashSet<>(data.managedGrantSources());
        collectProfileSources(sources, data.abilityGrantSources());
        collectProfileSources(sources, data.passiveGrantSources());
        collectProfileSources(sources, data.grantedItemSources());
        collectProfileSources(sources, data.recipePermissionSources());
        collectProfileSources(sources, data.stateFlagSources());
        collectProfileSources(sources, data.grantBundleSources());
        collectProfileSources(sources, data.artifactUnlockSources());
        for (ResourceLocation identityId : IdentityApi.activeIdentities(data)) {
            for (ResourceLocation sourceId : data.stateFlagSourcesFor(identityId)) {
                if (parseSourceId(sourceId).isPresent()) {
                    sources.add(sourceId);
                }
            }
        }
        return Set.copyOf(sources);
    }

    private static void collectProfileSources(
            Set<ResourceLocation> sources,
            Map<ResourceLocation, Set<ResourceLocation>> sourceMap
    ) {
        for (Set<ResourceLocation> sourceIds : sourceMap.values()) {
            for (ResourceLocation sourceId : sourceIds) {
                if (parseSourceId(sourceId).isPresent()) {
                    sources.add(sourceId);
                }
            }
        }
    }

    private static List<ResourceLocation> selectedProfilesInGroup(Set<ResourceLocation> selectedProfileIds, ResourceLocation groupId) {
        List<ResourceLocation> selected = new ArrayList<>();
        for (ResourceLocation profileId : PROFILES.keySet()) {
            ProfileDefinition profile = PROFILES.get(profileId);
            if (profile != null && profile.groupId().equals(groupId) && selectedProfileIds.contains(profileId)) {
                selected.add(profileId);
            }
        }
        return List.copyOf(selected);
    }

    private static boolean selectionConflictsWithExisting(Set<ResourceLocation> existingProfileIds, ResourceLocation candidateProfileId) {
        ProfileDefinition candidate = PROFILES.get(candidateProfileId);
        if (candidate == null) {
            return true;
        }
        for (ResourceLocation existingProfileId : existingProfileIds) {
            ProfileDefinition existing = PROFILES.get(existingProfileId);
            if (existing == null) {
                continue;
            }
            if (candidate.incompatibleWith(existingProfileId) || existing.incompatibleWith(candidateProfileId)) {
                return true;
            }
        }
        return false;
    }
}
