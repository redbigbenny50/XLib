package com.whatxe.xlib.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.whatxe.xlib.ability.AbilityData;
import com.whatxe.xlib.ability.AbilityGrantApi;
import com.whatxe.xlib.ability.GrantedItemGrantApi;
import com.whatxe.xlib.ability.PassiveGrantApi;
import com.whatxe.xlib.ability.ProfileApi;
import com.whatxe.xlib.ability.ProfileSelectionData;
import com.whatxe.xlib.ability.RecipePermissionApi;
import com.whatxe.xlib.attachment.ModAttachments;
import com.whatxe.xlib.progression.UpgradeApi;
import com.whatxe.xlib.progression.UpgradeProgressData;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

final class DebugAdminCommands {
    private static final DynamicCommandExceptionType DEBUG_EXPORT_FAILED =
            new DynamicCommandExceptionType(value -> Component.literal("Failed to write XLib debug export: " + value));
    private static final DynamicCommandExceptionType DEBUG_DIFF_FAILED =
            new DynamicCommandExceptionType(value -> Component.literal("Failed to diff XLib debug snapshot: " + value));
    private static final Gson DEBUG_GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final DateTimeFormatter DEBUG_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.ROOT);

    private DebugAdminCommands() {}

    static int counters(CommandSourceStack source, ServerPlayer target) {
        XLibDebugCounters counters = XLibDebugCounters.collect(target);
        source.sendSuccess(() -> Component.literal(target.getGameProfile().getName() + " | debug_counters"), false);
        source.sendSuccess(() -> Component.literal(counters.summary()), false);
        return 1;
    }

    static int state(CommandSourceStack source, ServerPlayer target) {
        AbilityData data = ModAttachments.get(target);
        ProfileSelectionData profileData = ProfileApi.get(target);
        source.sendSuccess(() -> Component.literal(target.getGameProfile().getName() + " | state"), false);
        source.sendSuccess(() -> Component.literal("active_modes=" + XLibCommandSupport.joinIds(data.activeModes())), false);
        source.sendSuccess(() -> Component.literal("mode_cycle_history=" + XLibCommandSupport.formatModeCycleHistory(data)), false);
        Collection<String> modeLines = XLibCommandSupport.buildActiveModeStateLines(data);
        if (modeLines.isEmpty()) {
            source.sendSuccess(() -> Component.literal("active_mode_states=-"), false);
        } else {
            for (String line : modeLines) {
                source.sendSuccess(() -> Component.literal("active_mode_state=" + line), false);
            }
        }

        source.sendSuccess(() -> Component.literal("active_detectors=" + XLibCommandSupport.joinIds(com.whatxe.xlib.ability.AbilityDetectorApi.activeDetectors(data))), false);
        Collection<String> detectorLines = XLibCommandSupport.buildDetectorStateLines(data);
        if (detectorLines.isEmpty()) {
            source.sendSuccess(() -> Component.literal("detector_states=-"), false);
        } else {
            for (String line : detectorLines) {
                source.sendSuccess(() -> Component.literal("detector_state=" + line), false);
            }
        }

        source.sendSuccess(() -> Component.literal("identities=" + XLibCommandSupport.joinIds(com.whatxe.xlib.ability.IdentityApi.activeIdentities(data))), false);
        Collection<String> identityLines = XLibCommandSupport.buildIdentityStateLines(data);
        if (identityLines.isEmpty()) {
            source.sendSuccess(() -> Component.literal("identity_states=-"), false);
        } else {
            for (String line : identityLines) {
                source.sendSuccess(() -> Component.literal("identity_state=" + line), false);
            }
        }

        source.sendSuccess(() -> Component.literal("active_artifacts=" + XLibCommandSupport.joinIds(
                com.whatxe.xlib.ability.ArtifactApi.allArtifacts().stream()
                        .filter(artifact -> com.whatxe.xlib.ability.ArtifactApi.isActive(target, artifact.id()))
                        .map(com.whatxe.xlib.ability.ArtifactDefinition::id)
                        .toList()
        )), false);
        source.sendSuccess(() -> Component.literal("unlocked_artifacts=" + XLibCommandSupport.joinIds(com.whatxe.xlib.ability.ArtifactApi.unlockedArtifacts(data))), false);
        Collection<String> artifactLines = XLibCommandSupport.buildArtifactStateLines(target, data);
        if (artifactLines.isEmpty()) {
            source.sendSuccess(() -> Component.literal("artifact_states=-"), false);
        } else {
            for (String line : artifactLines) {
                source.sendSuccess(() -> Component.literal("artifact_state=" + line), false);
            }
        }

        source.sendSuccess(() -> Component.literal("grant_bundles=" + XLibCommandSupport.joinIds(com.whatxe.xlib.ability.GrantBundleApi.activeBundles(data))), false);
        Collection<String> grantBundleLines = XLibCommandSupport.buildGrantBundleStateLines(data);
        if (grantBundleLines.isEmpty()) {
            source.sendSuccess(() -> Component.literal("grant_bundle_states=-"), false);
        } else {
            for (String line : grantBundleLines) {
                source.sendSuccess(() -> Component.literal("grant_bundle_state=" + line), false);
            }
        }

        source.sendSuccess(() -> Component.literal("state_policies=" + XLibCommandSupport.joinIds(data.activeStatePolicies())), false);
        Collection<String> statePolicyLines = XLibCommandSupport.buildStatePolicyLines(data);
        if (statePolicyLines.isEmpty()) {
            source.sendSuccess(() -> Component.literal("state_policy_states=-"), false);
        } else {
            for (String line : statePolicyLines) {
                source.sendSuccess(() -> Component.literal("state_policy_state=" + line), false);
            }
        }

        source.sendSuccess(() -> Component.literal("state_flags=" + XLibCommandSupport.joinIds(
                com.whatxe.xlib.ability.StateFlagApi.activeFlags(data).stream()
                        .filter(flagId -> !com.whatxe.xlib.ability.IdentityApi.isIdentity(flagId))
                        .toList()
        )), false);
        Collection<String> stateFlagLines = XLibCommandSupport.buildStateFlagLines(data);
        if (stateFlagLines.isEmpty()) {
            source.sendSuccess(() -> Component.literal("state_flag_states=-"), false);
        } else {
            for (String line : stateFlagLines) {
                source.sendSuccess(() -> Component.literal("state_flag_state=" + line), false);
            }
        }

        source.sendSuccess(() -> Component.literal("passives=" + XLibCommandSupport.joinIds(PassiveGrantApi.grantedPassives(target))), false);
        Collection<String> passiveLines = XLibCommandSupport.buildPassiveStateLines(target, data);
        if (passiveLines.isEmpty()) {
            source.sendSuccess(() -> Component.literal("passive_states=-"), false);
        } else {
            for (String line : passiveLines) {
                source.sendSuccess(() -> Component.literal("passive_state=" + line), false);
            }
        }

        source.sendSuccess(() -> Component.literal("selected_profiles=" + XLibCommandSupport.joinIds(profileData.selectedProfileIds())), false);
        Collection<String> profileLines = XLibCommandSupport.buildProfileSelectionLines(profileData);
        if (profileLines.isEmpty()) {
            source.sendSuccess(() -> Component.literal("profile_selection_states=-"), false);
        } else {
            for (String line : profileLines) {
                source.sendSuccess(() -> Component.literal("profile_selection_state=" + line), false);
            }
        }

        source.sendSuccess(() -> Component.literal("pending_profile_groups=" + XLibCommandSupport.joinIds(profileData.pendingGroupIds())), false);
        Collection<String> pendingProfileLines = XLibCommandSupport.buildPendingProfileGroupLines(profileData);
        if (pendingProfileLines.isEmpty()) {
            source.sendSuccess(() -> Component.literal("pending_profile_group_states=-"), false);
        } else {
            for (String line : pendingProfileLines) {
                source.sendSuccess(() -> Component.literal("pending_profile_group_state=" + line), false);
            }
        }
        return 1;
    }

    static int dump(CommandSourceStack source, ServerPlayer target) {
        AbilityData data = ModAttachments.get(target);
        UpgradeProgressData progressionData = UpgradeApi.get(target);
        ProfileSelectionData profileData = ProfileApi.get(target);
        XLibDebugCounters counters = XLibDebugCounters.collect(target);
        source.sendSuccess(() -> Component.literal(target.getGameProfile().getName() + " | uuid=" + target.getStringUUID()), false);
        source.sendSuccess(() -> Component.literal("debug_counters=" + counters.summary()), false);
        source.sendSuccess(() -> Component.literal("restricted=" + data.abilityAccessRestricted()
                + " | managed_sources=" + XLibCommandSupport.joinIds(data.managedGrantSources())), false);
        source.sendSuccess(() -> Component.literal("granted_abilities=" + XLibCommandSupport.joinIds(AbilityGrantApi.grantedAbilities(target))), false);
        source.sendSuccess(() -> Component.literal("blocked_abilities=" + XLibCommandSupport.joinIds(AbilityGrantApi.blockedAbilities(target))), false);
        source.sendSuccess(() -> Component.literal("active_detectors=" + XLibCommandSupport.joinIds(com.whatxe.xlib.ability.AbilityDetectorApi.activeDetectors(data))), false);
        source.sendSuccess(() -> Component.literal("identities=" + XLibCommandSupport.joinIds(com.whatxe.xlib.ability.IdentityApi.activeIdentities(data))), false);
        source.sendSuccess(() -> Component.literal("active_artifacts=" + XLibCommandSupport.joinIds(
                com.whatxe.xlib.ability.ArtifactApi.allArtifacts().stream()
                        .filter(artifact -> com.whatxe.xlib.ability.ArtifactApi.isActive(target, artifact.id()))
                        .map(com.whatxe.xlib.ability.ArtifactDefinition::id)
                        .toList()
        )), false);
        source.sendSuccess(() -> Component.literal("unlocked_artifacts=" + XLibCommandSupport.joinIds(com.whatxe.xlib.ability.ArtifactApi.unlockedArtifacts(data))), false);
        source.sendSuccess(() -> Component.literal("grant_bundles=" + XLibCommandSupport.joinIds(com.whatxe.xlib.ability.GrantBundleApi.activeBundles(data))), false);
        source.sendSuccess(() -> Component.literal("state_policies=" + XLibCommandSupport.joinIds(data.activeStatePolicies())), false);
        source.sendSuccess(() -> Component.literal("state_flags=" + XLibCommandSupport.joinIds(
                com.whatxe.xlib.ability.StateFlagApi.activeFlags(data).stream()
                        .filter(flagId -> !com.whatxe.xlib.ability.IdentityApi.isIdentity(flagId))
                        .toList()
        )), false);
        source.sendSuccess(() -> Component.literal("passives=" + XLibCommandSupport.joinIds(PassiveGrantApi.grantedPassives(target))), false);
        source.sendSuccess(() -> Component.literal("granted_items=" + XLibCommandSupport.joinIds(GrantedItemGrantApi.grantedItems(target))), false);
        source.sendSuccess(() -> Component.literal("recipe_permissions=" + XLibCommandSupport.joinIds(RecipePermissionApi.permissions(target))), false);
        source.sendSuccess(() -> Component.literal("locked_recipes=" + XLibCommandSupport.joinIds(RecipePermissionApi.lockedRecipes(target))), false);
        source.sendSuccess(() -> Component.literal("point_balances=" + XLibCommandSupport.formatNumericMap(progressionData.pointBalances())), false);
        source.sendSuccess(() -> Component.literal("counters=" + XLibCommandSupport.formatNumericMap(progressionData.counters())), false);
        source.sendSuccess(() -> Component.literal("unlocked_nodes=" + XLibCommandSupport.joinIds(progressionData.unlockedNodes())), false);
        source.sendSuccess(() -> Component.literal("managed_unlock_sources=" + XLibCommandSupport.formatSourceMap(progressionData.managedUnlockSources())), false);
        source.sendSuccess(() -> Component.literal("visible_tracks=" + XLibCommandSupport.formatTrackIds(UpgradeApi.visibleTracks(progressionData))), false);
        source.sendSuccess(() -> Component.literal("selected_profiles=" + XLibCommandSupport.joinIds(profileData.selectedProfileIds())), false);
        source.sendSuccess(() -> Component.literal("pending_profile_groups=" + XLibCommandSupport.joinIds(profileData.pendingGroupIds())), false);
        source.sendSuccess(() -> Component.literal("profile_reset_counts=" + XLibCommandSupport.formatNumericMap(profileData.resetCounts())), false);
        source.sendSuccess(() -> Component.literal("profile_last_reset_reasons=" + XLibCommandSupport.formatStringMap(profileData.lastResetReasons())), false);
        source.sendSuccess(() -> Component.literal("slots=" + XLibCommandSupport.formatSlots(data)), false);
        source.sendSuccess(() -> Component.literal("resolved_slots=" + XLibCommandSupport.formatResolvedSlots(data)), false);
        source.sendSuccess(() -> Component.literal("mode_loadouts=" + XLibCommandSupport.formatModeLoadouts(data)), false);
        source.sendSuccess(() -> Component.literal("mode_overlays=" + XLibCommandSupport.formatModeOverlays(data)), false);
        source.sendSuccess(() -> Component.literal("mode_cycle_history=" + XLibCommandSupport.formatModeCycleHistory(data)), false);
        source.sendSuccess(() -> Component.literal("active=" + XLibCommandSupport.joinIds(data.activeModes())), false);
        source.sendSuccess(() -> Component.literal("detector_windows=" + XLibCommandSupport.formatNumericMap(data.detectorWindows())), false);
        source.sendSuccess(() -> Component.literal("combo_windows=" + XLibCommandSupport.formatComboWindows(data)), false);
        source.sendSuccess(() -> Component.literal("combo_overrides=" + XLibCommandSupport.formatComboOverrides(data)), false);
        source.sendSuccess(() -> Component.literal("cooldowns=" + XLibCommandSupport.formatCooldowns(data)), false);
        source.sendSuccess(() -> Component.literal("charges=" + XLibCommandSupport.formatCharges(data)), false);
        source.sendSuccess(() -> Component.literal("resources=" + XLibCommandSupport.formatResources(data)), false);
        source.sendSuccess(() -> Component.literal("ability_sources=" + XLibCommandSupport.formatSourceMap(data.abilityGrantSources())), false);
        source.sendSuccess(() -> Component.literal("activation_block_sources=" + XLibCommandSupport.formatSourceMap(data.abilityActivationBlockSources())), false);
        source.sendSuccess(() -> Component.literal("state_policy_sources=" + XLibCommandSupport.formatSourceMap(data.statePolicySources())), false);
        source.sendSuccess(() -> Component.literal("state_flag_sources=" + XLibCommandSupport.formatSourceMap(
                data.stateFlagSources().entrySet().stream()
                        .filter(entry -> !com.whatxe.xlib.ability.IdentityApi.isIdentity(entry.getKey()))
                        .collect(java.util.stream.Collectors.toMap(
                                java.util.Map.Entry::getKey,
                                entry -> java.util.Set.copyOf(entry.getValue()),
                                (left, right) -> left,
                                java.util.LinkedHashMap::new
                        ))
        )), false);
        source.sendSuccess(() -> Component.literal("grant_bundle_sources=" + XLibCommandSupport.formatSourceMap(data.grantBundleSources())), false);
        source.sendSuccess(() -> Component.literal("artifact_unlock_sources=" + XLibCommandSupport.formatSourceMap(data.artifactUnlockSources())), false);
        source.sendSuccess(() -> Component.literal("passive_sources=" + XLibCommandSupport.formatSourceMap(data.passiveGrantSources())), false);
        source.sendSuccess(() -> Component.literal("granted_item_sources=" + XLibCommandSupport.formatSourceMap(data.grantedItemSources())), false);
        source.sendSuccess(() -> Component.literal("recipe_permission_sources=" + XLibCommandSupport.formatSourceMap(data.recipePermissionSources())), false);
        source.sendSuccess(() -> Component.literal("source_groups=" + XLibCommandSupport.formatSourceGroups(data)), false);
        for (String line : XLibCommandSupport.buildSourceDescriptors(data).values().stream()
                .map(XLibCommandSupport::formatSourceDescriptor)
                .sorted()
                .toList()) {
            source.sendSuccess(() -> Component.literal("source_descriptor=" + line), false);
        }
        for (String line : XLibCommandSupport.buildIdentityStateLines(data)) {
            source.sendSuccess(() -> Component.literal("identity_state=" + line), false);
        }
        for (String line : XLibCommandSupport.buildGrantBundleStateLines(data)) {
            source.sendSuccess(() -> Component.literal("grant_bundle_state=" + line), false);
        }
        for (String line : XLibCommandSupport.buildActiveModeStateLines(data)) {
            source.sendSuccess(() -> Component.literal("active_mode_state=" + line), false);
        }
        for (String line : XLibCommandSupport.buildDetectorStateLines(data)) {
            source.sendSuccess(() -> Component.literal("detector_state=" + line), false);
        }
        for (String line : XLibCommandSupport.buildArtifactStateLines(target, data)) {
            source.sendSuccess(() -> Component.literal("artifact_state=" + line), false);
        }
        for (String line : XLibCommandSupport.buildStatePolicyLines(data)) {
            source.sendSuccess(() -> Component.literal("state_policy_state=" + line), false);
        }
        for (String line : XLibCommandSupport.buildStateFlagLines(data)) {
            source.sendSuccess(() -> Component.literal("state_flag_state=" + line), false);
        }
        for (String line : XLibCommandSupport.buildPassiveStateLines(target, data)) {
            source.sendSuccess(() -> Component.literal("passive_state=" + line), false);
        }
        for (String line : XLibCommandSupport.buildProfileSelectionLines(profileData)) {
            source.sendSuccess(() -> Component.literal("profile_selection_state=" + line), false);
        }
        for (String line : XLibCommandSupport.buildPendingProfileGroupLines(profileData)) {
            source.sendSuccess(() -> Component.literal("pending_profile_group_state=" + line), false);
        }
        return 1;
    }

    static int export(CommandSourceStack source, ServerPlayer target) throws CommandSyntaxException {
        Path debugDirectory = source.getServer().getFile("debug/xlib");
        String fileName = target.getGameProfile().getName() + "-" + DEBUG_TIMESTAMP.format(LocalDateTime.now()) + ".json";
        Path exportPath = debugDirectory.resolve(fileName);

        try {
            Files.createDirectories(debugDirectory);
            Files.writeString(
                    exportPath,
                    DEBUG_GSON.toJson(XLibCommandSupport.buildDebugJson(target)),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE
            );
        } catch (IOException exception) {
            throw DEBUG_EXPORT_FAILED.create(exception.getMessage());
        }

        source.sendSuccess(() -> Component.literal("Wrote XLib debug export: " + exportPath.toAbsolutePath()), false);
        return 1;
    }

    static int source(CommandSourceStack source, ServerPlayer target, ResourceLocation sourceId) {
        AbilityData data = ModAttachments.get(target);
        XLibCommandSupport.SourceGroup group = XLibCommandSupport.buildSourceGroups(data).get(sourceId);
        com.whatxe.xlib.ability.GrantSourceDescriptor descriptor = XLibCommandSupport.buildSourceDescriptors(data).get(sourceId);
        if (group == null && descriptor == null) {
            source.sendSuccess(() -> Component.literal("No XLib state currently tracked for source " + sourceId), false);
            return 0;
        }

        source.sendSuccess(() -> Component.literal(target.getGameProfile().getName() + " | source=" + sourceId), false);
        if (descriptor != null) {
            source.sendSuccess(() -> Component.literal("descriptor=" + XLibCommandSupport.formatSourceDescriptor(descriptor)), false);
        }
        if (group == null) {
            return 1;
        }
        source.sendSuccess(() -> Component.literal("abilities=" + XLibCommandSupport.joinIds(group.abilities())), false);
        source.sendSuccess(() -> Component.literal("blocked_abilities=" + XLibCommandSupport.joinIds(group.blockedAbilities())), false);
        source.sendSuccess(() -> Component.literal("state_policies=" + XLibCommandSupport.joinIds(group.statePolicies())), false);
        source.sendSuccess(() -> Component.literal("state_flags=" + XLibCommandSupport.joinIds(group.stateFlags())), false);
        source.sendSuccess(() -> Component.literal("grant_bundles=" + XLibCommandSupport.joinIds(group.grantBundles())), false);
        source.sendSuccess(() -> Component.literal("identities=" + XLibCommandSupport.joinIds(group.identities())), false);
        source.sendSuccess(() -> Component.literal("unlocked_artifacts=" + XLibCommandSupport.joinIds(group.unlockedArtifacts())), false);
        source.sendSuccess(() -> Component.literal("passives=" + XLibCommandSupport.joinIds(group.passives())), false);
        source.sendSuccess(() -> Component.literal("granted_items=" + XLibCommandSupport.joinIds(group.grantedItems())), false);
        source.sendSuccess(() -> Component.literal("recipe_permissions=" + XLibCommandSupport.joinIds(group.recipePermissions())), false);
        return 1;
    }

    static int diff(CommandSourceStack source, ServerPlayer target, String snapshotName) throws CommandSyntaxException {
        Path debugDirectory = source.getServer().getFile("debug/xlib");
        String safeFileName = Paths.get(snapshotName).getFileName().toString();
        Path snapshotPath = debugDirectory.resolve(safeFileName);
        JsonObject previousSnapshot;
        try {
            previousSnapshot = JsonParser.parseString(Files.readString(snapshotPath, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (RuntimeException | IOException exception) {
            throw DEBUG_DIFF_FAILED.create(exception.getMessage());
        }

        JsonObject currentSnapshot = XLibCommandSupport.buildDebugJson(target);
        source.sendSuccess(
                () -> Component.literal("Comparing current state for " + target.getGameProfile().getName() + " against " + snapshotPath.toAbsolutePath()),
                false
        );
        XLibCommandSupport.sendArrayDiff(source, "granted_abilities", previousSnapshot, currentSnapshot);
        XLibCommandSupport.sendArrayDiff(source, "blocked_abilities", previousSnapshot, currentSnapshot);
        XLibCommandSupport.sendArrayDiff(source, "active_detectors", previousSnapshot, currentSnapshot);
        XLibCommandSupport.sendArrayDiff(source, "identities", previousSnapshot, currentSnapshot);
        XLibCommandSupport.sendArrayDiff(source, "active_artifacts", previousSnapshot, currentSnapshot);
        XLibCommandSupport.sendArrayDiff(source, "unlocked_artifacts", previousSnapshot, currentSnapshot);
        XLibCommandSupport.sendArrayDiff(source, "grant_bundles", previousSnapshot, currentSnapshot);
        XLibCommandSupport.sendArrayDiff(source, "state_policies", previousSnapshot, currentSnapshot);
        XLibCommandSupport.sendArrayDiff(source, "state_flags", previousSnapshot, currentSnapshot);
        XLibCommandSupport.sendArrayDiff(source, "passives", previousSnapshot, currentSnapshot);
        XLibCommandSupport.sendArrayDiff(source, "granted_items", previousSnapshot, currentSnapshot);
        XLibCommandSupport.sendArrayDiff(source, "recipe_permissions", previousSnapshot, currentSnapshot);
        XLibCommandSupport.sendArrayDiff(source, "locked_recipes", previousSnapshot, currentSnapshot);
        XLibCommandSupport.sendNumericMapDiff(source, "detector_windows", previousSnapshot, currentSnapshot);
        XLibCommandSupport.sendNumericMapDiff(source, "point_balances", previousSnapshot, currentSnapshot);
        XLibCommandSupport.sendNumericMapDiff(source, "counters", previousSnapshot, currentSnapshot);
        XLibCommandSupport.sendNumericMapDiff(source, "profile_reset_counts", previousSnapshot, currentSnapshot);
        XLibCommandSupport.sendArrayDiff(source, "unlocked_nodes", previousSnapshot, currentSnapshot);
        XLibCommandSupport.sendArrayDiff(source, "visible_tracks", previousSnapshot, currentSnapshot);
        XLibCommandSupport.sendArrayDiff(source, "selected_profiles", previousSnapshot, currentSnapshot);
        XLibCommandSupport.sendArrayDiff(source, "pending_profile_groups", previousSnapshot, currentSnapshot);
        return 1;
    }
}
