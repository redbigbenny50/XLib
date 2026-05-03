package com.whatxe.xlib.command;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.serialization.JsonOps;
import com.whatxe.xlib.ability.AbilityRequirement;
import com.whatxe.xlib.ability.AbilityApi;
import com.whatxe.xlib.ability.AbilityData;
import com.whatxe.xlib.ability.AbilityDetectorApi;
import com.whatxe.xlib.ability.AbilityDetectorDefinition;
import com.whatxe.xlib.ability.AbilityDefinition;
import com.whatxe.xlib.ability.AbilityGrantApi;
import com.whatxe.xlib.ability.AbilityLoadoutApi;
import com.whatxe.xlib.ability.AbilityResourceDefinition;
import com.whatxe.xlib.ability.AbilitySlotContainerApi;
import com.whatxe.xlib.ability.AbilitySlotReference;
import com.whatxe.xlib.ability.ArtifactApi;
import com.whatxe.xlib.ability.ArtifactDefinition;
import com.whatxe.xlib.ability.ComboChainApi;
import com.whatxe.xlib.ability.GrantBundleApi;
import com.whatxe.xlib.ability.GrantBundleDefinition;
import com.whatxe.xlib.ability.GrantedItemApi;
import com.whatxe.xlib.ability.GrantedItemGrantApi;
import com.whatxe.xlib.ability.GrantOwnershipApi;
import com.whatxe.xlib.ability.GrantSourceDescriptor;
import com.whatxe.xlib.ability.IdentityDefinition;
import com.whatxe.xlib.ability.IdentityApi;
import com.whatxe.xlib.ability.ModeApi;
import com.whatxe.xlib.ability.ModeDefinition;
import com.whatxe.xlib.ability.PassiveApi;
import com.whatxe.xlib.ability.PassiveDefinition;
import com.whatxe.xlib.ability.PassiveGrantApi;
import com.whatxe.xlib.ability.PassiveSoundTrigger;
import com.whatxe.xlib.ability.ProfileApi;
import com.whatxe.xlib.ability.ProfileDefinition;
import com.whatxe.xlib.ability.ProfileGroupDefinition;
import com.whatxe.xlib.ability.ProfilePendingSelection;
import com.whatxe.xlib.ability.ProfileSelectionData;
import com.whatxe.xlib.ability.ProfileSelectionEntry;
import com.whatxe.xlib.ability.RecipePermissionApi;
import com.whatxe.xlib.ability.StateFlagApi;
import com.whatxe.xlib.ability.StatePolicyApi;
import com.whatxe.xlib.ability.StatePolicyDefinition;
import com.whatxe.xlib.attachment.ModAttachments;
import com.whatxe.xlib.binding.EntityBindingApi;
import com.whatxe.xlib.binding.EntityBindingState;
import com.whatxe.xlib.body.BodyTransitionApi;
import com.whatxe.xlib.body.BodyTransitionState;
import com.whatxe.xlib.capability.CapabilityPolicyApi;
import com.whatxe.xlib.capability.CapabilityPolicyData;
import com.whatxe.xlib.classification.EntityClassificationApi;
import com.whatxe.xlib.form.VisualFormApi;
import com.whatxe.xlib.form.VisualFormData;
import com.whatxe.xlib.lifecycle.LifecycleStageApi;
import com.whatxe.xlib.lifecycle.LifecycleStageState;
import com.whatxe.xlib.network.ModPayloads;
import com.whatxe.xlib.progression.UpgradeApi;
import com.whatxe.xlib.progression.UpgradeProgressData;
import com.whatxe.xlib.progression.UpgradeTrackDefinition;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.RecipeHolder;

final class XLibCommandSupport {
    private static final DynamicCommandExceptionType UNKNOWN_ABILITY =
            new DynamicCommandExceptionType(value -> Component.translatable("command.xlib.abilities.invalid", value));
    private static final DynamicCommandExceptionType UNKNOWN_PASSIVE =
            new DynamicCommandExceptionType(value -> Component.translatable("command.xlib.passives.invalid", value));
    private static final DynamicCommandExceptionType UNKNOWN_GRANTED_ITEM =
            new DynamicCommandExceptionType(value -> Component.translatable("command.xlib.items.invalid", value));
    private static final DynamicCommandExceptionType UNKNOWN_RECIPE =
            new DynamicCommandExceptionType(value -> Component.translatable("command.xlib.recipes.invalid", value));
    private static final DynamicCommandExceptionType UNKNOWN_UPGRADE_NODE =
            new DynamicCommandExceptionType(value -> Component.translatable("command.xlib.progression.node_invalid", value));
    private static final DynamicCommandExceptionType UNKNOWN_UPGRADE_TRACK =
            new DynamicCommandExceptionType(value -> Component.translatable("command.xlib.progression.track_invalid", value));
    private static final DynamicCommandExceptionType UNKNOWN_PROFILE =
            new DynamicCommandExceptionType(value -> Component.translatable("command.xlib.profiles.invalid", value));
    private static final DynamicCommandExceptionType UNKNOWN_PROFILE_GROUP =
            new DynamicCommandExceptionType(value -> Component.translatable("command.xlib.profiles.group_invalid", value));

    private XLibCommandSupport() {}

    static CompletableFuture<Suggestions> suggestRecipeIds(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder
    ) {
        LinkedHashSet<ResourceLocation> recipeIds = new LinkedHashSet<>(RecipePermissionApi.restrictedRecipes());
        context.getSource().getServer().getRecipeManager().getRecipes().stream()
                .map(RecipeHolder::id)
                .forEach(recipeIds::add);
        return SharedSuggestionProvider.suggestResource(recipeIds, builder);
    }

    static void validateAbility(ResourceLocation abilityId) throws CommandSyntaxException {
        if (AbilityApi.findAbility(abilityId).isEmpty()) {
            throw UNKNOWN_ABILITY.create(abilityId.toString());
        }
    }

    static void validatePassive(ResourceLocation passiveId) throws CommandSyntaxException {
        if (PassiveApi.findPassive(passiveId).isEmpty()) {
            throw UNKNOWN_PASSIVE.create(passiveId.toString());
        }
    }

    static void validateGrantedItem(ResourceLocation grantedItemId) throws CommandSyntaxException {
        if (GrantedItemApi.findGrantedItem(grantedItemId).isEmpty()) {
            throw UNKNOWN_GRANTED_ITEM.create(grantedItemId.toString());
        }
    }

    static void validateRecipe(CommandSourceStack source, ResourceLocation recipeId) throws CommandSyntaxException {
        if (!RecipePermissionApi.isRestricted(recipeId)
                && source.getServer().getRecipeManager().byKey(recipeId).isEmpty()) {
            throw UNKNOWN_RECIPE.create(recipeId.toString());
        }
    }

    static void validateUpgradeNode(ResourceLocation nodeId) throws CommandSyntaxException {
        if (UpgradeApi.findNode(nodeId).isEmpty()) {
            throw UNKNOWN_UPGRADE_NODE.create(nodeId.toString());
        }
    }

    static void validateUpgradeTrack(ResourceLocation trackId) throws CommandSyntaxException {
        if (UpgradeApi.findTrack(trackId).isEmpty()) {
            throw UNKNOWN_UPGRADE_TRACK.create(trackId.toString());
        }
    }

    static void validateProfile(ResourceLocation profileId) throws CommandSyntaxException {
        if (ProfileApi.findProfile(profileId).isEmpty()) {
            throw UNKNOWN_PROFILE.create(profileId.toString());
        }
    }

    static void validateProfileGroup(ResourceLocation groupId) throws CommandSyntaxException {
        if (ProfileApi.findGroup(groupId).isEmpty()) {
            throw UNKNOWN_PROFILE_GROUP.create(groupId.toString());
        }
    }

    static String formatSlots(AbilityData data) {
        return formatContainerSlots(data, false);
    }

    static String formatResolvedSlots(AbilityData data) {
        return formatContainerSlots(data, true);
    }

    private static String formatContainerSlots(AbilityData data, boolean resolved) {
        StringBuilder builder = new StringBuilder();
        boolean wroteContainer = false;
        for (ResourceLocation containerId : orderedContainerIds(data)) {
            int pageCount = Math.max(1, AbilitySlotContainerApi.resolvedPageCount(data, containerId));
            for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
                if (wroteContainer) {
                    builder.append(" ; ");
                }
                builder.append(containerId).append("[").append(pageIndex + 1).append("]=");
                int slotCount = Math.max(1, AbilitySlotContainerApi.resolvedSlotsPerPage(data, containerId));
                for (int slot = 0; slot < slotCount; slot++) {
                    if (slot > 0) {
                        builder.append(" | ");
                    }
                    AbilitySlotReference slotReference = new AbilitySlotReference(containerId, pageIndex, slot);
                    builder.append(slot + 1).append(":")
                            .append((resolved ? AbilityLoadoutApi.resolvedAbilityId(data, slotReference) : data.abilityInSlot(slotReference))
                                    .map(ResourceLocation::toString)
                                    .orElse("-"));
                }
                wroteContainer = true;
            }
        }
        return wroteContainer ? builder.toString() : "-";
    }

    static String formatCooldowns(AbilityData data) {
        if (data.cooldowns().isEmpty()) {
            return "-";
        }
        return data.cooldowns().entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .map(entry -> entry.getKey() + "=" + String.format(Locale.ROOT, "%.1fs", entry.getValue() / 20.0F))
                .reduce((left, right) -> left + ", " + right)
                .orElse("-");
    }

    static String formatCharges(AbilityData data) {
        if (data.charges().isEmpty()) {
            return "-";
        }
        return data.charges().entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .map(entry -> entry.getKey() + "=" + entry.getValue() + " (recharge=" + data.chargeRechargeFor(entry.getKey()) + ")")
                .reduce((left, right) -> left + ", " + right)
                .orElse("-");
    }

    static String formatResources(AbilityData data) {
        if (data.resources().isEmpty()) {
            return "-";
        }
        return AbilityApi.allResources().stream()
                .sorted(Comparator.comparing(resource -> resource.id().toString()))
                .map(AbilityResourceDefinition::id)
                .map(resourceId -> resourceId + "=" + formatExactAmount(data.resourceAmountExact(resourceId))
                        + " (regen=" + data.resourceRegenDelay(resourceId)
                        + ", decay=" + data.resourceDecayDelay(resourceId) + ")")
                .reduce((left, right) -> left + ", " + right)
                .orElse("-");
    }

    private static String formatExactAmount(double amount) {
        int whole = (int) Math.round(amount);
        if (Math.abs(amount - whole) < 0.001D) {
            return Integer.toString(whole);
        }
        return String.format(java.util.Locale.ROOT, "%.2f", amount);
    }

    static String formatComboWindows(AbilityData data) {
        if (data.comboWindows().isEmpty()) {
            return "-";
        }
        return data.comboWindows().entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .reduce((left, right) -> left + ", " + right)
                .orElse("-");
    }

    static String formatComboOverrides(AbilityData data) {
        StringBuilder builder = new StringBuilder();
        boolean wrote = false;
        for (ResourceLocation containerId : orderedContainerIds(data)) {
            int pageCount = Math.max(1, AbilitySlotContainerApi.resolvedPageCount(data, containerId));
            int slotCount = Math.max(1, AbilitySlotContainerApi.resolvedSlotsPerPage(data, containerId));
            for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
                for (int slot = 0; slot < slotCount; slot++) {
                    AbilitySlotReference slotReference = new AbilitySlotReference(containerId, pageIndex, slot);
                    Optional<ResourceLocation> abilityId = data.comboOverrideInSlot(slotReference);
                    if (abilityId.isEmpty()) {
                        continue;
                    }
                    if (wrote) {
                        builder.append(", ");
                    }
                    builder.append(containerId).append("[").append(pageIndex + 1).append("]:")
                            .append(slot + 1).append(":").append(abilityId.get())
                            .append(" (").append(data.comboOverrideDurationForSlot(slotReference)).append(")");
                    wrote = true;
                }
            }
        }
        return wrote ? builder.toString() : "-";
    }

    static String formatModeOverlays(AbilityData data) {
        StringBuilder builder = new StringBuilder();
        boolean wrote = false;
        for (ResourceLocation containerId : orderedContainerIds(data)) {
            int pageCount = Math.max(1, AbilitySlotContainerApi.resolvedPageCount(data, containerId));
            int slotCount = Math.max(1, AbilitySlotContainerApi.resolvedSlotsPerPage(data, containerId));
            for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
                for (int slot = 0; slot < slotCount; slot++) {
                    AbilitySlotReference slotReference = new AbilitySlotReference(containerId, pageIndex, slot);
                    Optional<ResourceLocation> overlayAbility = ModeApi.resolveOverlayAbility(data, slotReference);
                    if (overlayAbility.isEmpty()) {
                        continue;
                    }
                    if (wrote) {
                        builder.append(", ");
                    }
                    builder.append(containerId).append("[").append(pageIndex + 1).append("]:")
                            .append(slot + 1).append(":").append(overlayAbility.get());
                    wrote = true;
                }
            }
        }
        return wrote ? builder.toString() : "-";
    }

    static String formatModeLoadouts(AbilityData data) {
        if (data.containerState().containerIds().isEmpty()) {
            return "-";
        }
        List<String> modeEntries = new ArrayList<>();
        for (ResourceLocation containerId : orderedContainerIds(data)) {
            data.containerState().modeLoadouts(containerId).entrySet().stream()
                    .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                    .forEach(entry -> modeEntries.add(entry.getKey() + "@" + containerId + "=" + formatModeLoadoutSlots(data, entry.getKey(), containerId)));
        }
        return modeEntries.isEmpty() ? "-" : String.join("; ", modeEntries);
    }

    private static String formatModeLoadoutSlots(AbilityData data, ResourceLocation modeId, ResourceLocation containerId) {
        StringBuilder builder = new StringBuilder();
        int pageCount = Math.max(1, AbilitySlotContainerApi.resolvedPageCount(data, containerId));
        int slotCount = Math.max(1, AbilitySlotContainerApi.resolvedSlotsPerPage(data, containerId));
        for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
            if (pageIndex > 0) {
                builder.append(" ; ");
            }
            builder.append("page ").append(pageIndex + 1).append(": ");
            for (int slot = 0; slot < slotCount; slot++) {
                if (slot > 0) {
                    builder.append(" | ");
                }
                builder.append(slot + 1).append(":")
                        .append(data.modeAbilityInSlot(modeId, new AbilitySlotReference(containerId, pageIndex, slot))
                                .map(ResourceLocation::toString)
                                .orElse("-"));
            }
        }
        return builder.toString();
    }

    private static List<ResourceLocation> orderedContainerIds(AbilityData data) {
        if (data.containerState().containerIds().isEmpty()) {
            return List.of(AbilitySlotContainerApi.PRIMARY_CONTAINER_ID);
        }
        return data.containerState().containerIds().stream()
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .toList();
    }

    static String formatSourceMap(Map<ResourceLocation, Set<ResourceLocation>> sourceMap) {
        if (sourceMap.isEmpty()) {
            return "-";
        }
        return sourceMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .map(entry -> entry.getKey() + "=" + joinIds(entry.getValue()))
                .reduce((left, right) -> left + "; " + right)
                .orElse("-");
    }

    static String formatNumericMap(Map<ResourceLocation, Integer> values) {
        if (values.isEmpty()) {
            return "-";
        }
        return values.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .reduce((left, right) -> left + ", " + right)
                .orElse("-");
    }

    static String formatTrackIds(Collection<UpgradeTrackDefinition> tracks) {
        if (tracks.isEmpty()) {
            return "-";
        }
        return tracks.stream()
                .map(UpgradeTrackDefinition::id)
                .map(ResourceLocation::toString)
                .sorted()
                .reduce((left, right) -> left + ", " + right)
                .orElse("-");
    }

    static String joinIds(Collection<ResourceLocation> ids) {
        return ids.isEmpty()
                ? "-"
                : ids.stream().map(ResourceLocation::toString).sorted().reduce((left, right) -> left + ", " + right).orElse("-");
    }

    static String formatMetadata(
            Optional<ResourceLocation> familyId,
            Optional<ResourceLocation> groupId,
            Optional<ResourceLocation> pageId,
            Collection<ResourceLocation> tags
    ) {
        return "family=" + familyId.map(ResourceLocation::toString).orElse("-")
                + " | group=" + groupId.map(ResourceLocation::toString).orElse("-")
                + " | page=" + pageId.map(ResourceLocation::toString).orElse("-")
                + " | tags=" + joinIds(tags);
    }

    static String formatMetadataBlock(
            Optional<ResourceLocation> familyId,
            Optional<ResourceLocation> groupId,
            Optional<ResourceLocation> pageId,
            Collection<ResourceLocation> tags
    ) {
        return "{" + formatMetadata(familyId, groupId, pageId, tags) + "}";
    }

    static String formatRequirementDescriptions(Collection<AbilityRequirement> requirements) {
        if (requirements.isEmpty()) {
            return "-";
        }
        return requirements.stream()
                .map(AbilityRequirement::description)
                .map(Component::getString)
                .sorted()
                .reduce((left, right) -> left + " | " + right)
                .orElse("-");
    }

    static Collection<String> buildProfileSelectionLines(ProfileSelectionData data) {
        Collection<String> lines = new ArrayList<>();
        for (ResourceLocation profileId : data.selectedProfileIds().stream()
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .toList()) {
            data.selection(profileId).ifPresent(entry -> lines.add(formatProfileSelectionLine(profileId, entry)));
        }
        return List.copyOf(lines);
    }

    static String formatProfileSelectionLine(ResourceLocation profileId, ProfileSelectionEntry entry) {
        ProfileDefinition profile = ProfileApi.findProfile(profileId).orElse(null);
        ProfileGroupDefinition group = ProfileApi.findGroup(entry.groupId()).orElse(null);
        String displayName = profile == null ? profileId.toString() : profile.displayName().getString();
        String groupName = group == null ? entry.groupId().toString() : group.displayName().getString();
        return profileId
                + " | display=" + displayName
                + " | group=" + groupName
                + " | origin=" + entry.origin().name().toLowerCase(Locale.ROOT)
                + " | locked=" + entry.locked()
                + " | admin_set=" + entry.adminSet()
                + " | delegated=" + entry.delegated()
                + " | temporary=" + entry.temporary()
                + " | reason=" + (entry.reason().isBlank() ? "-" : entry.reason());
    }

    static Collection<String> buildPendingProfileGroupLines(ProfileSelectionData data) {
        Collection<String> lines = new ArrayList<>();
        for (ResourceLocation groupId : data.pendingGroupIds().stream()
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .toList()) {
            data.pendingGroup(groupId).ifPresent(entry -> lines.add(formatPendingProfileGroupLine(groupId, entry)));
        }
        return List.copyOf(lines);
    }

    static String formatPendingProfileGroupLine(ResourceLocation groupId, ProfilePendingSelection pendingSelection) {
        ProfileGroupDefinition group = ProfileApi.findGroup(groupId).orElse(null);
        String displayName = group == null ? groupId.toString() : group.displayName().getString();
        return groupId
                + " | display=" + displayName
                + " | trigger=" + pendingSelection.trigger().name().toLowerCase(Locale.ROOT)
                + " | reason=" + (pendingSelection.reason().isBlank() ? "-" : pendingSelection.reason());
    }

    static String formatStringMap(Map<ResourceLocation, String> values) {
        if (values.isEmpty()) {
            return "-";
        }
        return values.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .reduce((left, right) -> left + "; " + right)
                .orElse("-");
    }

    static String formatPassiveHooks(PassiveDefinition passive) {
        return joinStrings(passive.authoredHooks().stream()
                .map(hook -> hook.name().toLowerCase(Locale.ROOT))
                .toList());
    }

    static String formatPassiveSoundTriggers(PassiveDefinition passive) {
        return joinStrings(passive.soundTriggers().stream()
                .map(PassiveSoundTrigger::name)
                .map(name -> name.toLowerCase(Locale.ROOT))
                .toList());
    }

    static Collection<String> buildPassiveStateLines(Player player, AbilityData data) {
        Collection<String> lines = new ArrayList<>();
        data.grantedPassives().stream()
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .forEach(passiveId -> PassiveApi.findPassive(passiveId).ifPresent(passive -> lines.add(
                        formatPassiveStateLine(player, data, passive, data.passiveGrantSourcesFor(passiveId))
                )));
        return List.copyOf(lines);
    }

    static String formatPassiveStateLine(
            Player player,
            AbilityData data,
            PassiveDefinition passive,
            Collection<ResourceLocation> sources
    ) {
        String grant = passive.firstFailedGrantRequirement(player, data).map(Component::getString).orElse("ok");
        String active = passive.firstFailedActiveRequirement(player, data).map(Component::getString).orElse("ok");
        return passive.id()
                + " | metadata=" + formatMetadataBlock(passive.familyId(), passive.groupId(), passive.pageId(), passive.tags())
                + " | grant=" + grant
                + " | active=" + active
                + " | cooldown_multiplier=" + formatDecimal(passive.cooldownTickRateMultiplier())
                + " | hooks=" + formatPassiveHooks(passive)
                + " | sound_triggers=" + formatPassiveSoundTriggers(passive)
                + " | sources=" + joinIds(sources);
    }

    static Collection<String> buildActiveModeStateLines(AbilityData data) {
        Collection<String> lines = new ArrayList<>();
        for (ModeDefinition mode : ModeApi.activeModes(data)) {
            lines.add(formatModeStateLine(data, mode));
        }
        return List.copyOf(lines);
    }

    static Collection<String> buildStatePolicyLines(AbilityData data) {
        Collection<String> lines = new ArrayList<>();
        for (StatePolicyDefinition policy : StatePolicyApi.activePolicies(data)) {
            lines.add(formatStatePolicyLine(data, policy));
        }
        return List.copyOf(lines);
    }

    static Collection<String> buildDetectorStateLines(AbilityData data) {
        return AbilityDetectorApi.activeDetectors(data).stream()
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .map(detectorId -> formatDetectorStateLine(data, detectorId))
                .toList();
    }

    static Collection<String> buildIdentityStateLines(AbilityData data) {
        return IdentityApi.activeIdentities(data).stream()
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .map(identityId -> formatIdentityStateLine(data, identityId))
                .toList();
    }

    static Collection<String> buildGrantBundleStateLines(AbilityData data) {
        Collection<String> lines = new ArrayList<>();
        for (ResourceLocation bundleId : GrantBundleApi.activeBundles(data).stream()
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .toList()) {
            GrantBundleApi.findBundle(bundleId).ifPresent(bundle -> lines.add(formatGrantBundleStateLine(data, bundle)));
        }
        return List.copyOf(lines);
    }

    static Collection<String> buildStateFlagLines(AbilityData data) {
        return nonIdentityStateFlags(data).stream()
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .map(flagId -> formatStateFlagLine(data, flagId))
                .toList();
    }

    static Collection<String> buildArtifactStateLines(Player player, AbilityData data) {
        return ArtifactApi.allArtifacts().stream()
                .filter(artifact -> ArtifactApi.isUnlocked(data, artifact.id()) || player != null && ArtifactApi.isActive(player, artifact.id()))
                .sorted(Comparator.comparing(artifact -> artifact.id().toString()))
                .map(artifact -> formatArtifactStateLine(player, data, artifact))
                .toList();
    }

    static String formatModeStateLine(AbilityData data, ModeDefinition mode) {
        String cycleSummary = mode.cycleGroupId() == null
                ? "-"
                : mode.cycleOrder() > 0 ? mode.cycleGroupId() + "#" + mode.cycleOrder() : mode.cycleGroupId().toString();
        return mode.abilityId()
                + " | duration=" + formatDuration(data.activeDurationFor(mode.abilityId()))
                + " | metadata=" + formatMetadataBlock(mode.familyId(), mode.groupId(), mode.pageId(), mode.tags())
                + " | priority=" + mode.priority()
                + " | stackable=" + mode.stackable()
                + " | cooldown_multiplier=" + formatDecimal(mode.cooldownTickRateMultiplier())
                + " | cycle=" + cycleSummary
                + " | cycle_resets=" + joinIds(mode.resetCycleGroupsOnActivate())
                + " | overlays=" + formatOverlaySlots(mode.overlayAbilities())
                + " | upkeep=" + formatModeUpkeep(mode)
                + " | grants=" + formatModeGrantBundle(mode)
                + " | state_policies=" + joinIds(mode.statePolicies())
                + " | state_flags=" + joinIds(mode.stateFlags())
                + " | blocks=" + joinIds(mode.blockedAbilities())
                + " | blocked_by=" + joinIds(mode.blockedByModes())
                + " | exclusive=" + joinIds(mode.exclusiveModes())
                + " | transforms_from=" + joinIds(mode.transformsFrom());
    }

    static String formatDetectorStateLine(AbilityData data, ResourceLocation detectorId) {
        AbilityDetectorDefinition detector = AbilityDetectorApi.findDetector(detectorId).orElse(null);
        if (detector == null) {
            return detectorId + " | remaining_ticks=" + data.detectorWindowFor(detectorId);
        }
        return detector.id()
                + " | remaining_ticks=" + data.detectorWindowFor(detector.id())
                + " | duration_ticks=" + detector.durationTicks()
                + " | events=" + formatReactiveEvents(detector.events());
    }

    static String formatStatePolicyLine(AbilityData data, StatePolicyDefinition policy) {
        return policy.id()
                + " | cooldown_multiplier=" + formatDecimal(policy.cooldownTickRateMultiplier())
                + " | locked=" + joinIds(StatePolicyApi.lockedAbilities(AbilityData.empty().withStatePolicySource(policy.id(), policy.id(), true)))
                + " | silenced=" + joinIds(StatePolicyApi.silencedAbilities(AbilityData.empty().withStatePolicySource(policy.id(), policy.id(), true)))
                + " | suppressed=" + joinIds(StatePolicyApi.suppressedAbilities(AbilityData.empty().withStatePolicySource(policy.id(), policy.id(), true)))
                + " | sources=" + joinIds(data.statePolicySourcesFor(policy.id()));
    }

    static String formatIdentityStateLine(AbilityData data, ResourceLocation identityId) {
        IdentityDefinition identity = IdentityApi.findIdentity(identityId).orElse(null);
        if (identity == null) {
            return identityId + " | sources=" + joinIds(data.stateFlagSourcesFor(identityId));
        }
        return identityId
                + " | inherits=" + joinIds(identity.inheritedIdentities())
                + " | bundles=" + joinIds(IdentityApi.resolvedGrantBundles(identityId))
                + " | sources=" + joinIds(IdentityApi.identitySources(data, identityId));
    }

    static String formatGrantBundleStateLine(AbilityData data, GrantBundleDefinition bundle) {
        return bundle.id()
                + " | abilities=" + joinIds(bundle.abilities())
                + " | passives=" + joinIds(bundle.passives())
                + " | granted_items=" + joinIds(bundle.grantedItems())
                + " | recipe_permissions=" + joinIds(bundle.recipePermissions())
                + " | blocked_abilities=" + joinIds(bundle.blockedAbilities())
                + " | state_policies=" + joinIds(bundle.statePolicies())
                + " | state_flags=" + joinIds(bundle.stateFlags())
                + " | sources=" + joinIds(data.grantBundleSourcesFor(bundle.id()));
    }

    static String formatStateFlagLine(AbilityData data, ResourceLocation flagId) {
        return flagId + " | sources=" + joinIds(data.stateFlagSourcesFor(flagId));
    }

    static String formatArtifactStateLine(Player player, AbilityData data, ArtifactDefinition artifact) {
        boolean active = player != null && ArtifactApi.isActive(player, artifact.id());
        return artifact.id()
                + " | active=" + active
                + " | unlocked=" + ArtifactApi.isUnlocked(data, artifact.id())
                + " | items=" + joinIds(artifact.itemIds())
                + " | presence=" + joinStrings(artifact.presenceModes().stream()
                        .map(mode -> mode.name().toLowerCase(Locale.ROOT))
                        .toList())
                + " | equipped_bundles=" + joinIds(artifact.equippedBundles())
                + " | unlocked_bundles=" + joinIds(artifact.unlockedBundles())
                + " | requirements=" + formatRequirementDescriptions(artifact.requirements())
                + " | unlock_on_consume=" + artifact.unlockOnConsume()
                + " | unlock_sources=" + joinIds(data.artifactUnlockSourcesFor(artifact.id()));
    }

    static String formatSourceDescriptor(GrantSourceDescriptor descriptor) {
        return descriptor.sourceId()
                + " | kind=" + descriptor.kind().name().toLowerCase(Locale.ROOT)
                + " | primary=" + descriptor.primaryId().map(ResourceLocation::toString).orElse("-")
                + " | backing_source=" + descriptor.backingSourceId().map(ResourceLocation::toString).orElse("-")
                + " | identities=" + joinIds(descriptor.identities())
                + " | grant_bundles=" + joinIds(descriptor.grantBundles())
                + " | grantor=" + descriptor.grantorId().map(Object::toString).orElse("-")
                + " | managed=" + descriptor.managed()
                + " | reason=" + descriptor.reason()
                + " | disappears=" + descriptor.disappearsWhen();
    }

    static Map<ResourceLocation, GrantSourceDescriptor> buildSourceDescriptors(AbilityData data) {
        return GrantOwnershipApi.describeSources(data);
    }

    static String formatModeCycleHistory(AbilityData data) {
        if (data.modeCycleHistory().isEmpty()) {
            return "-";
        }
        return data.modeCycleHistory().entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .map(entry -> entry.getKey() + "=" + joinIds(entry.getValue()))
                .reduce((left, right) -> left + "; " + right)
                .orElse("-");
    }

    static String formatSourceGroups(AbilityData data) {
        Map<ResourceLocation, SourceGroup> groups = buildSourceGroups(data);
        if (groups.isEmpty()) {
            return "-";
        }
        return groups.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .map(entry -> entry.getKey() + "[" + joinIds(entry.getValue().abilities())
                        + "|" + joinIds(entry.getValue().passives())
                        + "|" + joinIds(entry.getValue().grantedItems())
                        + "|" + joinIds(entry.getValue().recipePermissions())
                        + "|" + joinIds(entry.getValue().blockedAbilities())
                        + "|" + joinIds(entry.getValue().statePolicies())
                        + "|" + joinIds(entry.getValue().stateFlags())
                        + "|" + joinIds(entry.getValue().grantBundles())
                        + "|" + joinIds(entry.getValue().identities())
                        + "|" + joinIds(entry.getValue().unlockedArtifacts()) + "]")
                .reduce((left, right) -> left + "; " + right)
                .orElse("-");
    }

    static JsonObject buildDebugJson(ServerPlayer target) {
        AbilityData data = ModAttachments.get(target);
        UpgradeProgressData progressionData = UpgradeApi.get(target);
        ProfileSelectionData profileData = ProfileApi.get(target);
        XLibDebugCounters counters = XLibDebugCounters.collect(target);
        JsonObject root = new JsonObject();
        root.addProperty("player_name", target.getGameProfile().getName());
        root.addProperty("player_uuid", target.getStringUUID());
        root.addProperty("play_protocol_version", ModPayloads.PLAY_PROTOCOL_VERSION);
        root.addProperty("play_protocol_series", ModPayloads.PLAY_PROTOCOL.series());
        root.addProperty("play_protocol_revision", ModPayloads.PLAY_PROTOCOL.revision());
        root.addProperty("ability_access_restricted", data.abilityAccessRestricted());
        root.add("managed_sources", idsToJson(data.managedGrantSources()));
        root.add("debug_counters", counters.toJson());
        root.add("granted_abilities", idsToJson(AbilityGrantApi.grantedAbilities(target)));
        root.add("blocked_abilities", idsToJson(AbilityGrantApi.blockedAbilities(target)));
        root.add("active_detectors", idsToJson(AbilityDetectorApi.activeDetectors(data)));
        root.add("identities", idsToJson(IdentityApi.activeIdentities(data)));
        root.add("active_artifacts", idsToJson(activeArtifactIds(target, data)));
        root.add("unlocked_artifacts", idsToJson(ArtifactApi.unlockedArtifacts(data)));
        root.add("grant_bundles", idsToJson(GrantBundleApi.activeBundles(data)));
        root.add("state_policies", idsToJson(data.activeStatePolicies()));
        root.add("state_flags", idsToJson(nonIdentityStateFlags(data)));
        root.add("passives", idsToJson(PassiveGrantApi.grantedPassives(target)));
        root.add("granted_items", idsToJson(GrantedItemGrantApi.grantedItems(target)));
        root.add("recipe_permissions", idsToJson(RecipePermissionApi.permissions(target)));
        root.add("locked_recipes", idsToJson(RecipePermissionApi.lockedRecipes(target)));
        root.add("point_balances", intMapToJson(progressionData.pointBalances()));
        root.add("counters", intMapToJson(progressionData.counters()));
        root.add("unlocked_nodes", idsToJson(progressionData.unlockedNodes()));
        root.add("managed_unlock_sources", sourceMapToJson(progressionData.managedUnlockSources()));
        root.add("visible_tracks", trackIdsToJson(UpgradeApi.visibleTracks(progressionData)));
        root.add("selected_profiles", idsToJson(profileData.selectedProfileIds()));
        root.add("pending_profile_groups", idsToJson(profileData.pendingGroupIds()));
        root.add("profile_reset_counts", intMapToJson(profileData.resetCounts()));
        root.add("profile_last_reset_reasons", stringMapToJson(profileData.lastResetReasons()));
        root.add("synthetic_entity_types", idsToJson(EntityClassificationApi.syntheticEntityTypes(target)));
        root.add("synthetic_direct_tags", idsToJson(EntityClassificationApi.directSyntheticTags(target)));
        root.add("synthetic_inherited_tags", idsToJson(EntityClassificationApi.inheritedSyntheticTags(target)));
        root.add("synthetic_tags", idsToJson(EntityClassificationApi.syntheticTags(target)));
        root.add("synthetic_entity_type_sources", sourceMapToJson(EntityClassificationApi.syntheticEntityTypeSources(target)));
        root.add("synthetic_tag_sources", sourceMapToJson(EntityClassificationApi.syntheticTagSources(target)));
        root.addProperty("resolved_slots", formatResolvedSlots(data));
        root.addProperty("mode_loadouts", formatModeLoadouts(data));
        root.addProperty("mode_overlays", formatModeOverlays(data));
        root.addProperty("mode_cycle_history", formatModeCycleHistory(data));
        root.add("detector_windows", intMapToJson(data.detectorWindows()));
        root.addProperty("combo_windows_summary", formatComboWindows(data));
        root.addProperty("combo_overrides_summary", formatComboOverrides(data));
        root.add("active_mode_states", activeModeStatesToJson(data));
        root.add("detector_states", detectorStatesToJson(data));
        root.add("identity_states", identityStatesToJson(data));
        root.add("artifact_states", artifactStatesToJson(target, data));
        root.add("grant_bundle_states", grantBundleStatesToJson(data));
        root.add("state_policy_states", statePolicyStatesToJson(data));
        root.add("state_flag_states", stateFlagStatesToJson(data));
        root.add("passive_states", passiveStatesToJson(target, data));
        root.add("profile_selection_states", profileSelectionStatesToJson(profileData));
        root.add("pending_profile_group_states", pendingProfileGroupStatesToJson(profileData));
        root.add("ability_sources", sourceMapToJson(data.abilityGrantSources()));
        root.add("activation_block_sources", sourceMapToJson(data.abilityActivationBlockSources()));
        root.add("state_policy_sources", sourceMapToJson(data.statePolicySources()));
        root.add("state_flag_sources", sourceMapToJson(nonIdentityStateFlagSources(data)));
        root.add("grant_bundle_sources", sourceMapToJson(data.grantBundleSources()));
        root.add("artifact_unlock_sources", sourceMapToJson(data.artifactUnlockSources()));
        root.add("passive_sources", sourceMapToJson(data.passiveGrantSources()));
        root.add("granted_item_sources", sourceMapToJson(data.grantedItemSources()));
        root.add("recipe_permission_sources", sourceMapToJson(data.recipePermissionSources()));
        root.add("source_groups", sourceGroupsToJson(buildSourceGroups(data)));
        root.add("source_descriptors", sourceDescriptorsToJson(buildSourceDescriptors(data)));
        JsonElement rawData = AbilityData.CODEC.encodeStart(JsonOps.INSTANCE, data).getOrThrow(IllegalStateException::new);
        root.add("ability_data", rawData);
        JsonElement rawProgressionData = UpgradeProgressData.CODEC.encodeStart(JsonOps.INSTANCE, progressionData)
                .getOrThrow(IllegalStateException::new);
        root.add("progression_data", rawProgressionData);
        JsonElement rawProfileData = ProfileSelectionData.CODEC.encodeStart(JsonOps.INSTANCE, profileData)
                .getOrThrow(IllegalStateException::new);
        root.add("profile_selection_data", rawProfileData);
        root.add("capability_policies", capabilityPoliciesToJson(target));
        root.add("entity_bindings", entityBindingsToJson(target));
        root.add("lifecycle_stage", lifecycleStageToJson(target));
        root.add("visual_forms", visualFormsToJson(target));
        root.add("body_transition", bodyTransitionToJson(target));
        return root;
    }

    static Map<ResourceLocation, SourceGroup> buildSourceGroups(AbilityData data) {
        Map<ResourceLocation, SourceGroupBuilder> builders = new LinkedHashMap<>();
        mergeSourceGroups(builders, data.abilityGrantSources(), SourceGroupField.ABILITY);
        mergeSourceGroups(builders, data.abilityActivationBlockSources(), SourceGroupField.BLOCKED_ABILITY);
        mergeSourceGroups(builders, data.statePolicySources(), SourceGroupField.STATE_POLICY);
        mergeSourceGroups(builders, nonIdentityStateFlagSources(data), SourceGroupField.STATE_FLAG);
        mergeSourceGroups(builders, data.grantBundleSources(), SourceGroupField.GRANT_BUNDLE);
        mergeSourceGroups(builders, data.artifactUnlockSources(), SourceGroupField.UNLOCKED_ARTIFACT);
        mergeSourceGroups(builders, data.passiveGrantSources(), SourceGroupField.PASSIVE);
        mergeSourceGroups(builders, data.grantedItemSources(), SourceGroupField.GRANTED_ITEM);
        mergeSourceGroups(builders, data.recipePermissionSources(), SourceGroupField.RECIPE_PERMISSION);
        for (ResourceLocation identityId : IdentityApi.activeIdentities(data)) {
            for (ResourceLocation sourceId : data.stateFlagSourcesFor(identityId)) {
                builders.computeIfAbsent(sourceId, ignored -> new SourceGroupBuilder()).add(SourceGroupField.IDENTITY, identityId);
            }
        }
        Map<ResourceLocation, SourceGroup> groups = new LinkedHashMap<>();
        builders.forEach((sourceId, builder) -> groups.put(sourceId, builder.build()));
        return groups;
    }

    private static void mergeSourceGroups(
            Map<ResourceLocation, SourceGroupBuilder> builders,
            Map<ResourceLocation, Set<ResourceLocation>> sourceMap,
            SourceGroupField field
    ) {
        for (Map.Entry<ResourceLocation, Set<ResourceLocation>> entry : sourceMap.entrySet()) {
            for (ResourceLocation sourceId : entry.getValue()) {
                builders.computeIfAbsent(sourceId, ignored -> new SourceGroupBuilder()).add(field, entry.getKey());
            }
        }
    }

    static void sendArrayDiff(
            CommandSourceStack source,
            String fieldName,
            JsonObject previousSnapshot,
            JsonObject currentSnapshot
    ) {
        Set<String> previousValues = jsonArrayToSet(previousSnapshot.getAsJsonArray(fieldName));
        Set<String> currentValues = jsonArrayToSet(currentSnapshot.getAsJsonArray(fieldName));
        Set<String> added = new LinkedHashSet<>(currentValues);
        added.removeAll(previousValues);
        Set<String> removed = new LinkedHashSet<>(previousValues);
        removed.removeAll(currentValues);
        source.sendSuccess(() -> Component.literal(fieldName + " | +" + joinStrings(added) + " | -" + joinStrings(removed)), false);
    }

    static void sendNumericMapDiff(
            CommandSourceStack source,
            String fieldName,
            JsonObject previousSnapshot,
            JsonObject currentSnapshot
    ) {
        Map<String, Integer> previousValues = jsonObjectToIntMap(previousSnapshot.getAsJsonObject(fieldName));
        Map<String, Integer> currentValues = jsonObjectToIntMap(currentSnapshot.getAsJsonObject(fieldName));
        Set<String> keys = new LinkedHashSet<>(previousValues.keySet());
        keys.addAll(currentValues.keySet());
        Collection<String> added = new ArrayList<>();
        Collection<String> changed = new ArrayList<>();
        Collection<String> removed = new ArrayList<>();
        keys.stream().sorted().forEach(key -> {
            Integer previousValue = previousValues.get(key);
            Integer currentValue = currentValues.get(key);
            if (previousValue == null && currentValue != null) {
                added.add(key + "=" + currentValue);
            } else if (previousValue != null && currentValue == null) {
                removed.add(key + "=" + previousValue);
            } else if (previousValue != null && currentValue != null && !previousValue.equals(currentValue)) {
                changed.add(key + ":" + previousValue + "->" + currentValue);
            }
        });
        source.sendSuccess(
                () -> Component.literal(fieldName + " | +" + joinStrings(added) + " | ~" + joinStrings(changed) + " | -" + joinStrings(removed)),
                false
        );
    }

    private static JsonArray idsToJson(Collection<ResourceLocation> ids) {
        JsonArray array = new JsonArray();
        ids.stream().map(ResourceLocation::toString).sorted().forEach(array::add);
        return array;
    }

    private static JsonObject sourceMapToJson(Map<ResourceLocation, Set<ResourceLocation>> sourceMap) {
        JsonObject object = new JsonObject();
        sourceMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> object.add(entry.getKey().toString(), idsToJson(entry.getValue())));
        return object;
    }

    private static JsonArray activeModeStatesToJson(AbilityData data) {
        JsonArray array = new JsonArray();
        for (ModeDefinition mode : ModeApi.activeModes(data)) {
            array.add(activeModeStateToJson(data, mode));
        }
        return array;
    }

    private static JsonArray statePolicyStatesToJson(AbilityData data) {
        JsonArray array = new JsonArray();
        for (StatePolicyDefinition policy : StatePolicyApi.activePolicies(data)) {
            JsonObject object = new JsonObject();
            object.addProperty("id", policy.id().toString());
            object.addProperty("summary", formatStatePolicyLine(data, policy));
            object.addProperty("cooldown_tick_rate_multiplier", policy.cooldownTickRateMultiplier());
            object.add("locked_abilities", idsToJson(StatePolicyApi.lockedAbilities(AbilityData.empty().withStatePolicySource(policy.id(), policy.id(), true))));
            object.add("silenced_abilities", idsToJson(StatePolicyApi.silencedAbilities(AbilityData.empty().withStatePolicySource(policy.id(), policy.id(), true))));
            object.add("suppressed_abilities", idsToJson(StatePolicyApi.suppressedAbilities(AbilityData.empty().withStatePolicySource(policy.id(), policy.id(), true))));
            object.add("sources", idsToJson(data.statePolicySourcesFor(policy.id())));
            array.add(object);
        }
        return array;
    }

    private static JsonArray detectorStatesToJson(AbilityData data) {
        JsonArray array = new JsonArray();
        for (ResourceLocation detectorId : AbilityDetectorApi.activeDetectors(data).stream()
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .toList()) {
            JsonObject object = new JsonObject();
            object.addProperty("id", detectorId.toString());
            object.addProperty("summary", formatDetectorStateLine(data, detectorId));
            object.addProperty("remaining_ticks", data.detectorWindowFor(detectorId));
            AbilityDetectorApi.findDetector(detectorId).ifPresent(detector -> {
                object.addProperty("duration_ticks", detector.durationTicks());
                object.add("events", stringArrayToJson(detector.events().stream()
                        .map(eventType -> eventType.name().toLowerCase(Locale.ROOT))
                        .toList()));
            });
            array.add(object);
        }
        return array;
    }

    private static JsonArray identityStatesToJson(AbilityData data) {
        JsonArray array = new JsonArray();
        for (ResourceLocation identityId : IdentityApi.activeIdentities(data).stream()
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .toList()) {
            JsonObject object = new JsonObject();
            object.addProperty("id", identityId.toString());
            object.addProperty("summary", formatIdentityStateLine(data, identityId));
            object.add("sources", idsToJson(IdentityApi.identitySources(data, identityId)));
            object.add("resolved_bundles", idsToJson(IdentityApi.resolvedGrantBundles(identityId)));
            IdentityApi.findIdentity(identityId).ifPresent(identity ->
                    object.add("inherited_identities", idsToJson(identity.inheritedIdentities())));
            array.add(object);
        }
        return array;
    }

    private static JsonArray artifactStatesToJson(Player player, AbilityData data) {
        JsonArray array = new JsonArray();
        for (ArtifactDefinition artifact : ArtifactApi.allArtifacts().stream()
                .filter(entry -> ArtifactApi.isUnlocked(data, entry.id()) || ArtifactApi.isActive(player, entry.id()))
                .sorted(Comparator.comparing(entry -> entry.id().toString()))
                .toList()) {
            array.add(artifactStateToJson(player, data, artifact));
        }
        return array;
    }

    private static JsonArray grantBundleStatesToJson(AbilityData data) {
        JsonArray array = new JsonArray();
        for (ResourceLocation bundleId : GrantBundleApi.activeBundles(data).stream()
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .toList()) {
            GrantBundleApi.findBundle(bundleId).ifPresent(bundle -> {
                JsonObject object = new JsonObject();
                object.addProperty("id", bundle.id().toString());
                object.addProperty("summary", formatGrantBundleStateLine(data, bundle));
                object.add("abilities", idsToJson(bundle.abilities()));
                object.add("passives", idsToJson(bundle.passives()));
                object.add("granted_items", idsToJson(bundle.grantedItems()));
                object.add("recipe_permissions", idsToJson(bundle.recipePermissions()));
                object.add("blocked_abilities", idsToJson(bundle.blockedAbilities()));
                object.add("state_policies", idsToJson(bundle.statePolicies()));
                object.add("state_flags", idsToJson(bundle.stateFlags()));
                object.add("sources", idsToJson(data.grantBundleSourcesFor(bundle.id())));
                array.add(object);
            });
        }
        return array;
    }

    private static JsonArray stateFlagStatesToJson(AbilityData data) {
        JsonArray array = new JsonArray();
        for (ResourceLocation flagId : nonIdentityStateFlags(data).stream()
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .toList()) {
            JsonObject object = new JsonObject();
            object.addProperty("id", flagId.toString());
            object.addProperty("summary", formatStateFlagLine(data, flagId));
            object.add("sources", idsToJson(data.stateFlagSourcesFor(flagId)));
            array.add(object);
        }
        return array;
    }

    private static JsonObject capabilityPoliciesToJson(ServerPlayer player) {
        JsonObject root = new JsonObject();
        CapabilityPolicyData data = CapabilityPolicyApi.getData(player);
        JsonArray policiesArray = new JsonArray();
        data.activePolicies().stream()
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .forEach(policyId -> {
                    JsonObject obj = new JsonObject();
                    obj.addProperty("id", policyId.toString());
                    obj.add("sources", idsToJson(data.sourcesFor(policyId)));
                    policiesArray.add(obj);
                });
        root.add("active_policies", policiesArray);
        return root;
    }

    private static JsonArray entityBindingsToJson(ServerPlayer player) {
        JsonArray array = new JsonArray();
        for (EntityBindingState state : EntityBindingApi.bindings(player)) {
            JsonObject obj = new JsonObject();
            obj.addProperty("instance_id", state.bindingInstanceId().toString());
            obj.addProperty("binding_id", state.bindingId().toString());
            obj.addProperty("primary_entity_id", state.primaryEntityId().toString());
            obj.addProperty("secondary_entity_id", state.secondaryEntityId().toString());
            obj.addProperty("source_id", state.sourceId().toString());
            obj.addProperty("started_game_time", state.startedGameTime());
            state.remainingTicks().ifPresent(ticks -> obj.addProperty("remaining_ticks", ticks));
            obj.addProperty("status", state.status().name().toLowerCase(Locale.ROOT));
            obj.addProperty("revision", state.revision());
            array.add(obj);
        }
        return array;
    }

    private static JsonObject lifecycleStageToJson(ServerPlayer player) {
        JsonObject root = new JsonObject();
        Optional<LifecycleStageState> stateOpt = LifecycleStageApi.state(player);
        root.addProperty("has_stage", stateOpt.isPresent());
        stateOpt.ifPresent(state -> {
            root.addProperty("current_stage_id", state.currentStageId().toString());
            root.addProperty("source_id", state.sourceId().toString());
            root.addProperty("entered_game_time", state.enteredGameTime());
            root.addProperty("elapsed_ticks", state.elapsedTicks());
            root.addProperty("status", state.status().name().toLowerCase(Locale.ROOT));
            state.pendingTransition().ifPresent(pending -> {
                JsonObject pendingObj = new JsonObject();
                pendingObj.addProperty("target_stage_id", pending.targetStageId().toString());
                pendingObj.addProperty("trigger", pending.trigger().name().toLowerCase(Locale.ROOT));
                pendingObj.addProperty("requested_game_time", pending.requestedGameTime());
                root.add("pending_transition", pendingObj);
            });
        });
        return root;
    }

    private static JsonObject visualFormsToJson(ServerPlayer player) {
        JsonObject root = new JsonObject();
        VisualFormData data = VisualFormApi.getData(player);
        JsonArray formsArray = new JsonArray();
        data.formSources().entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> {
                    JsonObject obj = new JsonObject();
                    obj.addProperty("form_id", entry.getKey().toString());
                    obj.addProperty("source_id", entry.getValue().toString());
                    VisualFormApi.findDefinition(entry.getKey()).ifPresent(def ->
                            obj.addProperty("kind", def.kind().name().toLowerCase(Locale.ROOT)));
                    formsArray.add(obj);
                });
        root.add("active_forms", formsArray);
        VisualFormApi.active(player).ifPresent(def ->
                root.addProperty("primary_form_id", def.id().toString()));
        return root;
    }

    private static JsonObject bodyTransitionToJson(ServerPlayer player) {
        JsonObject root = new JsonObject();
        Optional<BodyTransitionState> stateOpt = BodyTransitionApi.active(player);
        root.addProperty("is_transitioning", stateOpt.isPresent());
        stateOpt.ifPresent(state -> {
            root.addProperty("transition_id", state.transitionId().toString());
            root.addProperty("source_id", state.sourceId().toString());
            root.addProperty("controller_entity_id", state.controllerEntityId().toString());
            root.addProperty("current_body_entity_id", state.currentBodyEntityId().toString());
            state.originBodyEntityId().ifPresent(id ->
                    root.addProperty("origin_body_entity_id", id.toString()));
            root.addProperty("started_game_time", state.startedGameTime());
            root.addProperty("status", state.status().name().toLowerCase(Locale.ROOT));
        });
        return root;
    }

    private static JsonObject artifactStateToJson(Player player, AbilityData data, ArtifactDefinition artifact) {
        JsonObject object = new JsonObject();
        object.addProperty("id", artifact.id().toString());
        object.addProperty("summary", formatArtifactStateLine(player, data, artifact));
        object.addProperty("active", player != null && ArtifactApi.isActive(player, artifact.id()));
        object.addProperty("unlocked", ArtifactApi.isUnlocked(data, artifact.id()));
        object.add("item_ids", idsToJson(artifact.itemIds()));
        object.add("presence_modes", stringArrayToJson(artifact.presenceModes().stream()
                .map(mode -> mode.name().toLowerCase(Locale.ROOT))
                .toList()));
        object.add("equipped_bundles", idsToJson(artifact.equippedBundles()));
        object.add("unlocked_bundles", idsToJson(artifact.unlockedBundles()));
        object.add("unlock_sources", idsToJson(data.artifactUnlockSourcesFor(artifact.id())));
        object.addProperty("unlock_on_consume", artifact.unlockOnConsume());
        object.addProperty("requirements", formatRequirementDescriptions(artifact.requirements()));
        return object;
    }

    private static JsonObject activeModeStateToJson(AbilityData data, ModeDefinition mode) {
        JsonObject object = new JsonObject();
        object.addProperty("id", mode.abilityId().toString());
        object.addProperty("summary", formatModeStateLine(data, mode));
        object.addProperty("duration_ticks", data.activeDurationFor(mode.abilityId()));
        object.addProperty("duration", formatDuration(data.activeDurationFor(mode.abilityId())));
        object.add("metadata", metadataToJson(mode.familyId(), mode.groupId(), mode.pageId(), mode.tags()));
        object.addProperty("source_id", mode.sourceId().toString());
        object.addProperty("priority", mode.priority());
        object.addProperty("stackable", mode.stackable());
        if (mode.cycleGroupId() != null) {
            object.addProperty("cycle_group", mode.cycleGroupId().toString());
        }
        object.addProperty("cycle_order", mode.cycleOrder());
        object.add("reset_cycle_groups", idsToJson(mode.resetCycleGroupsOnActivate()));
        object.addProperty("cooldown_tick_rate_multiplier", mode.cooldownTickRateMultiplier());
        object.addProperty("health_cost_per_tick", mode.healthCostPerTick());
        object.addProperty("minimum_health", mode.minimumHealth());
        object.add("resource_delta_per_tick", doubleMapToJson(mode.resourceDeltaPerTick()));
        object.add("overlay_slots", overlayMapToJson(mode.overlayAbilities()));
        object.add("granted_abilities", idsToJson(mode.grantedAbilities()));
        object.add("granted_passives", idsToJson(mode.grantedPassives()));
        object.add("granted_items", idsToJson(mode.grantedItems()));
        object.add("granted_recipes", idsToJson(mode.grantedRecipes()));
        object.add("state_policies", idsToJson(mode.statePolicies()));
        object.add("state_flags", idsToJson(mode.stateFlags()));
        object.add("blocked_abilities", idsToJson(mode.blockedAbilities()));
        object.add("blocked_by_modes", idsToJson(mode.blockedByModes()));
        object.add("exclusive_modes", idsToJson(mode.exclusiveModes()));
        object.add("transforms_from", idsToJson(mode.transformsFrom()));
        return object;
    }

    private static JsonArray passiveStatesToJson(Player player, AbilityData data) {
        JsonArray array = new JsonArray();
        data.grantedPassives().stream()
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .forEach(passiveId -> PassiveApi.findPassive(passiveId).ifPresent(passive -> array.add(
                        passiveStateToJson(player, data, passive, data.passiveGrantSourcesFor(passiveId))
                )));
        return array;
    }

    private static JsonObject passiveStateToJson(
            Player player,
            AbilityData data,
            PassiveDefinition passive,
            Collection<ResourceLocation> sources
    ) {
        JsonObject object = new JsonObject();
        object.addProperty("id", passive.id().toString());
        object.addProperty("summary", formatPassiveStateLine(player, data, passive, sources));
        object.add("metadata", metadataToJson(passive.familyId(), passive.groupId(), passive.pageId(), passive.tags()));
        object.addProperty("grant_status", passive.firstFailedGrantRequirement(player, data).map(Component::getString).orElse("ok"));
        object.addProperty("active_status", passive.firstFailedActiveRequirement(player, data).map(Component::getString).orElse("ok"));
        object.addProperty("cooldown_tick_rate_multiplier", passive.cooldownTickRateMultiplier());
        object.add("hooks", stringArrayToJson(passive.authoredHooks().stream()
                .map(hook -> hook.name().toLowerCase(Locale.ROOT))
                .toList()));
        object.add("sound_triggers", stringArrayToJson(passive.soundTriggers().stream()
                .map(PassiveSoundTrigger::name)
                .map(name -> name.toLowerCase(Locale.ROOT))
                .toList()));
        object.add("sources", idsToJson(sources));
        return object;
    }

    private static JsonArray profileSelectionStatesToJson(ProfileSelectionData data) {
        JsonArray array = new JsonArray();
        data.selectedProfileIds().stream()
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .forEach(profileId -> data.selection(profileId).ifPresent(entry -> {
                    JsonObject object = new JsonObject();
                    object.addProperty("id", profileId.toString());
                    object.addProperty("summary", formatProfileSelectionLine(profileId, entry));
                    object.addProperty("origin", entry.origin().name().toLowerCase(Locale.ROOT));
                    object.addProperty("locked", entry.locked());
                    object.addProperty("admin_set", entry.adminSet());
                    object.addProperty("delegated", entry.delegated());
                    object.addProperty("temporary", entry.temporary());
                    object.addProperty("reason", entry.reason());
                    object.addProperty("group", entry.groupId().toString());
                    array.add(object);
                }));
        return array;
    }

    private static JsonArray pendingProfileGroupStatesToJson(ProfileSelectionData data) {
        JsonArray array = new JsonArray();
        data.pendingGroupIds().stream()
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .forEach(groupId -> data.pendingGroup(groupId).ifPresent(entry -> {
                    JsonObject object = new JsonObject();
                    object.addProperty("id", groupId.toString());
                    object.addProperty("summary", formatPendingProfileGroupLine(groupId, entry));
                    object.addProperty("trigger", entry.trigger().name().toLowerCase(Locale.ROOT));
                    object.addProperty("reason", entry.reason());
                    array.add(object);
                }));
        return array;
    }

    private static JsonObject intMapToJson(Map<ResourceLocation, Integer> values) {
        JsonObject object = new JsonObject();
        values.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> object.addProperty(entry.getKey().toString(), entry.getValue()));
        return object;
    }

    private static JsonObject stringMapToJson(Map<ResourceLocation, String> values) {
        JsonObject object = new JsonObject();
        values.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> object.addProperty(entry.getKey().toString(), entry.getValue()));
        return object;
    }

    private static JsonObject doubleMapToJson(Map<ResourceLocation, Double> values) {
        JsonObject object = new JsonObject();
        values.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> object.addProperty(entry.getKey().toString(), entry.getValue()));
        return object;
    }

    private static JsonObject overlayMapToJson(Map<Integer, ResourceLocation> values) {
        JsonObject object = new JsonObject();
        values.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> object.addProperty(Integer.toString(entry.getKey() + 1), entry.getValue().toString()));
        return object;
    }

    private static JsonArray trackIdsToJson(Collection<UpgradeTrackDefinition> tracks) {
        JsonArray array = new JsonArray();
        tracks.stream()
                .map(UpgradeTrackDefinition::id)
                .map(ResourceLocation::toString)
                .sorted()
                .forEach(array::add);
        return array;
    }

    private static JsonObject sourceGroupsToJson(Map<ResourceLocation, SourceGroup> sourceGroups) {
        JsonObject object = new JsonObject();
        sourceGroups.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> {
                    JsonObject group = new JsonObject();
                    group.add("abilities", idsToJson(entry.getValue().abilities()));
                    group.add("blocked_abilities", idsToJson(entry.getValue().blockedAbilities()));
                    group.add("state_policies", idsToJson(entry.getValue().statePolicies()));
                    group.add("state_flags", idsToJson(entry.getValue().stateFlags()));
                    group.add("grant_bundles", idsToJson(entry.getValue().grantBundles()));
                    group.add("identities", idsToJson(entry.getValue().identities()));
                    group.add("unlocked_artifacts", idsToJson(entry.getValue().unlockedArtifacts()));
                    group.add("passives", idsToJson(entry.getValue().passives()));
                    group.add("granted_items", idsToJson(entry.getValue().grantedItems()));
                    group.add("recipe_permissions", idsToJson(entry.getValue().recipePermissions()));
                    object.add(entry.getKey().toString(), group);
                });
        return object;
    }

    private static JsonObject sourceDescriptorsToJson(Map<ResourceLocation, GrantSourceDescriptor> sourceDescriptors) {
        JsonObject object = new JsonObject();
        sourceDescriptors.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> {
                    GrantSourceDescriptor descriptor = entry.getValue();
                    JsonObject descriptorJson = new JsonObject();
                    descriptorJson.addProperty("kind", descriptor.kind().name().toLowerCase(Locale.ROOT));
                    descriptor.primaryId().ifPresent(value -> descriptorJson.addProperty("primary_id", value.toString()));
                    descriptor.backingSourceId().ifPresent(value -> descriptorJson.addProperty("backing_source", value.toString()));
                    descriptorJson.add("identities", idsToJson(descriptor.identities()));
                    descriptorJson.add("grant_bundles", idsToJson(descriptor.grantBundles()));
                    descriptor.grantorId().ifPresent(value -> descriptorJson.addProperty("grantor_uuid", value.toString()));
                    descriptorJson.addProperty("managed", descriptor.managed());
                    descriptorJson.addProperty("reason", descriptor.reason());
                    descriptorJson.addProperty("disappears_when", descriptor.disappearsWhen());
                    descriptorJson.addProperty("summary", formatSourceDescriptor(descriptor));
                    object.add(entry.getKey().toString(), descriptorJson);
                });
        return object;
    }

    private static JsonObject metadataToJson(
            Optional<ResourceLocation> familyId,
            Optional<ResourceLocation> groupId,
            Optional<ResourceLocation> pageId,
            Collection<ResourceLocation> tags
    ) {
        JsonObject object = new JsonObject();
        familyId.ifPresent(value -> object.addProperty("family", value.toString()));
        groupId.ifPresent(value -> object.addProperty("group", value.toString()));
        pageId.ifPresent(value -> object.addProperty("page", value.toString()));
        object.add("tags", idsToJson(tags));
        return object;
    }

    private static JsonArray stringArrayToJson(Collection<String> values) {
        JsonArray array = new JsonArray();
        values.stream().sorted().forEach(array::add);
        return array;
    }

    private static Set<String> jsonArrayToSet(JsonArray array) {
        Set<String> values = new LinkedHashSet<>();
        if (array == null) {
            return values;
        }
        array.forEach(element -> values.add(element.getAsString()));
        return values;
    }

    private static Map<String, Integer> jsonObjectToIntMap(JsonObject object) {
        Map<String, Integer> values = new LinkedHashMap<>();
        if (object == null) {
            return values;
        }
        object.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> values.put(entry.getKey(), entry.getValue().getAsInt()));
        return values;
    }

    private static String joinStrings(Collection<String> values) {
        return values.isEmpty() ? "-" : values.stream().sorted().reduce((left, right) -> left + ", " + right).orElse("-");
    }

    private static Set<ResourceLocation> activeArtifactIds(Player player, AbilityData data) {
        return ArtifactApi.allArtifacts().stream()
                .filter(artifact -> ArtifactApi.isActive(player, artifact.id()))
                .map(ArtifactDefinition::id)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private static Set<ResourceLocation> nonIdentityStateFlags(AbilityData data) {
        return StateFlagApi.activeFlags(data).stream()
                .filter(flagId -> !IdentityApi.isIdentity(flagId))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private static String formatReactiveEvents(Collection<? extends Enum<?>> events) {
        return joinStrings(events.stream()
                .map(eventType -> eventType.name().toLowerCase(Locale.ROOT))
                .toList());
    }

    private static Map<ResourceLocation, Set<ResourceLocation>> nonIdentityStateFlagSources(AbilityData data) {
        Map<ResourceLocation, Set<ResourceLocation>> filtered = new LinkedHashMap<>();
        data.stateFlagSources().entrySet().stream()
                .filter(entry -> !IdentityApi.isIdentity(entry.getKey()))
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> filtered.put(entry.getKey(), Set.copyOf(entry.getValue())));
        return Map.copyOf(filtered);
    }

    private static String formatDuration(int ticks) {
        return ticks > 0 ? String.format(Locale.ROOT, "%.1fs", ticks / 20.0D) : "permanent";
    }

    private static String formatDecimal(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String formatModeUpkeep(ModeDefinition mode) {
        Collection<String> parts = new ArrayList<>();
        if (mode.healthCostPerTick() > 0.0D) {
            parts.add("health=" + formatDecimal(mode.healthCostPerTick()));
        }
        if (mode.minimumHealth() > 0.0D) {
            parts.add("min_health=" + formatDecimal(mode.minimumHealth()));
        }
        if (!mode.resourceDeltaPerTick().isEmpty()) {
            parts.add("resources=" + formatResourceDeltas(mode.resourceDeltaPerTick()));
        }
        return joinStrings(parts);
    }

    private static String formatResourceDeltas(Map<ResourceLocation, Double> values) {
        if (values.isEmpty()) {
            return "-";
        }
        return values.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .map(entry -> entry.getKey() + "=" + formatDecimal(entry.getValue()))
                .reduce((left, right) -> left + ", " + right)
                .orElse("-");
    }

    private static String formatOverlaySlots(Map<Integer, ResourceLocation> overlays) {
        if (overlays.isEmpty()) {
            return "-";
        }
        return overlays.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> (entry.getKey() + 1) + ":" + entry.getValue())
                .reduce((left, right) -> left + ", " + right)
                .orElse("-");
    }

    private static String formatModeGrantBundle(ModeDefinition mode) {
        Collection<String> parts = new ArrayList<>();
        if (!mode.grantedAbilities().isEmpty()) {
            parts.add("abilities[" + joinIds(mode.grantedAbilities()) + "]");
        }
        if (!mode.grantedPassives().isEmpty()) {
            parts.add("passives[" + joinIds(mode.grantedPassives()) + "]");
        }
        if (!mode.grantedItems().isEmpty()) {
            parts.add("items[" + joinIds(mode.grantedItems()) + "]");
        }
        if (!mode.grantedRecipes().isEmpty()) {
            parts.add("recipes[" + joinIds(mode.grantedRecipes()) + "]");
        }
        return joinStrings(parts);
    }

    enum SourceGroupField {
        ABILITY,
        BLOCKED_ABILITY,
        STATE_POLICY,
        STATE_FLAG,
        GRANT_BUNDLE,
        IDENTITY,
        UNLOCKED_ARTIFACT,
        PASSIVE,
        GRANTED_ITEM,
        RECIPE_PERMISSION
    }

    record SourceGroup(
            Set<ResourceLocation> abilities,
            Set<ResourceLocation> blockedAbilities,
            Set<ResourceLocation> statePolicies,
            Set<ResourceLocation> stateFlags,
            Set<ResourceLocation> grantBundles,
            Set<ResourceLocation> identities,
            Set<ResourceLocation> unlockedArtifacts,
            Set<ResourceLocation> passives,
            Set<ResourceLocation> grantedItems,
            Set<ResourceLocation> recipePermissions
    ) {}

    private static final class SourceGroupBuilder {
        private final Set<ResourceLocation> abilities = new LinkedHashSet<>();
        private final Set<ResourceLocation> blockedAbilities = new LinkedHashSet<>();
        private final Set<ResourceLocation> statePolicies = new LinkedHashSet<>();
        private final Set<ResourceLocation> stateFlags = new LinkedHashSet<>();
        private final Set<ResourceLocation> grantBundles = new LinkedHashSet<>();
        private final Set<ResourceLocation> identities = new LinkedHashSet<>();
        private final Set<ResourceLocation> unlockedArtifacts = new LinkedHashSet<>();
        private final Set<ResourceLocation> passives = new LinkedHashSet<>();
        private final Set<ResourceLocation> grantedItems = new LinkedHashSet<>();
        private final Set<ResourceLocation> recipePermissions = new LinkedHashSet<>();

        private void add(SourceGroupField field, ResourceLocation value) {
            switch (field) {
                case ABILITY -> this.abilities.add(value);
                case BLOCKED_ABILITY -> this.blockedAbilities.add(value);
                case STATE_POLICY -> this.statePolicies.add(value);
                case STATE_FLAG -> this.stateFlags.add(value);
                case GRANT_BUNDLE -> this.grantBundles.add(value);
                case IDENTITY -> this.identities.add(value);
                case UNLOCKED_ARTIFACT -> this.unlockedArtifacts.add(value);
                case PASSIVE -> this.passives.add(value);
                case GRANTED_ITEM -> this.grantedItems.add(value);
                case RECIPE_PERMISSION -> this.recipePermissions.add(value);
            }
        }

        private SourceGroup build() {
            return new SourceGroup(
                    Set.copyOf(this.abilities),
                    Set.copyOf(this.blockedAbilities),
                    Set.copyOf(this.statePolicies),
                    Set.copyOf(this.stateFlags),
                    Set.copyOf(this.grantBundles),
                    Set.copyOf(this.identities),
                    Set.copyOf(this.unlockedArtifacts),
                    Set.copyOf(this.passives),
                    Set.copyOf(this.grantedItems),
                    Set.copyOf(this.recipePermissions)
            );
        }
    }
}
