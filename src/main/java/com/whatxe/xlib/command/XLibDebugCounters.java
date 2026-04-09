package com.whatxe.xlib.command;

import com.google.gson.JsonObject;
import com.whatxe.xlib.ability.AbilityData;
import com.whatxe.xlib.ability.AbilityDetectorApi;
import com.whatxe.xlib.ability.AbilityGrantApi;
import com.whatxe.xlib.ability.AbilityLoadoutApi;
import com.whatxe.xlib.ability.AbilitySlotContainerApi;
import com.whatxe.xlib.ability.AbilitySlotReference;
import com.whatxe.xlib.ability.ArtifactApi;
import com.whatxe.xlib.ability.GrantBundleApi;
import com.whatxe.xlib.ability.GrantedItemGrantApi;
import com.whatxe.xlib.ability.IdentityApi;
import com.whatxe.xlib.ability.ModeApi;
import com.whatxe.xlib.ability.PassiveGrantApi;
import com.whatxe.xlib.ability.ProfileApi;
import com.whatxe.xlib.ability.ProfileSelectionData;
import com.whatxe.xlib.ability.RecipePermissionApi;
import com.whatxe.xlib.ability.StateFlagApi;
import com.whatxe.xlib.ability.StatePolicyApi;
import com.whatxe.xlib.attachment.ModAttachments;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

final class XLibDebugCounters {
    private final int assignedSlots;
    private final int resolvedSlots;
    private final int activeModes;
    private final int activeDetectors;
    private final int activeIdentities;
    private final int activeArtifacts;
    private final int unlockedArtifacts;
    private final int activeGrantBundles;
    private final int activeStatePolicies;
    private final int activeStateFlags;
    private final int cooldownEntries;
    private final int chargeEntries;
    private final int chargeRechargeEntries;
    private final int resourceEntries;
    private final int comboWindows;
    private final int comboOverrides;
    private final int modeOverlaySlots;
    private final int modePresetCount;
    private final int managedSources;
    private final int trackedSourceGroups;
    private final int grantedAbilities;
    private final int blockedAbilities;
    private final int grantedPassives;
    private final int grantedItems;
    private final int recipePermissions;
    private final int lockedRecipes;
    private final int selectedProfiles;
    private final int pendingProfileGroups;

    private XLibDebugCounters(
            int assignedSlots,
            int resolvedSlots,
            int activeModes,
            int activeDetectors,
            int activeIdentities,
            int activeArtifacts,
            int unlockedArtifacts,
            int activeGrantBundles,
            int activeStatePolicies,
            int activeStateFlags,
            int cooldownEntries,
            int chargeEntries,
            int chargeRechargeEntries,
            int resourceEntries,
            int comboWindows,
            int comboOverrides,
            int modeOverlaySlots,
            int modePresetCount,
            int managedSources,
            int trackedSourceGroups,
            int grantedAbilities,
            int blockedAbilities,
            int grantedPassives,
            int grantedItems,
            int recipePermissions,
            int lockedRecipes,
            int selectedProfiles,
            int pendingProfileGroups
    ) {
        this.assignedSlots = assignedSlots;
        this.resolvedSlots = resolvedSlots;
        this.activeModes = activeModes;
        this.activeDetectors = activeDetectors;
        this.activeIdentities = activeIdentities;
        this.activeArtifacts = activeArtifacts;
        this.unlockedArtifacts = unlockedArtifacts;
        this.activeGrantBundles = activeGrantBundles;
        this.activeStatePolicies = activeStatePolicies;
        this.activeStateFlags = activeStateFlags;
        this.cooldownEntries = cooldownEntries;
        this.chargeEntries = chargeEntries;
        this.chargeRechargeEntries = chargeRechargeEntries;
        this.resourceEntries = resourceEntries;
        this.comboWindows = comboWindows;
        this.comboOverrides = comboOverrides;
        this.modeOverlaySlots = modeOverlaySlots;
        this.modePresetCount = modePresetCount;
        this.managedSources = managedSources;
        this.trackedSourceGroups = trackedSourceGroups;
        this.grantedAbilities = grantedAbilities;
        this.blockedAbilities = blockedAbilities;
        this.grantedPassives = grantedPassives;
        this.grantedItems = grantedItems;
        this.recipePermissions = recipePermissions;
        this.lockedRecipes = lockedRecipes;
        this.selectedProfiles = selectedProfiles;
        this.pendingProfileGroups = pendingProfileGroups;
    }

    static XLibDebugCounters collect(ServerPlayer target) {
        AbilityData data = ModAttachments.get(target);
        ProfileSelectionData profileData = ProfileApi.get(target);
        int assignedSlots = 0;
        int resolvedSlots = 0;
        int comboOverrides = 0;
        int modeOverlaySlots = 0;
        int modePresetCount = 0;
        for (ResourceLocation containerId : data.containerState().containerIds().isEmpty()
                ? java.util.List.of(AbilitySlotContainerApi.PRIMARY_CONTAINER_ID)
                : data.containerState().containerIds()) {
            int pageCount = Math.max(1, AbilitySlotContainerApi.resolvedPageCount(data, containerId));
            int slotCount = Math.max(1, AbilitySlotContainerApi.resolvedSlotsPerPage(data, containerId));
            for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
                for (int slot = 0; slot < slotCount; slot++) {
                    AbilitySlotReference slotReference = new AbilitySlotReference(containerId, pageIndex, slot);
                    if (data.abilityInSlot(slotReference).isPresent()) {
                        assignedSlots++;
                    }
                    if (AbilityLoadoutApi.resolvedAbilityId(data, slotReference).isPresent()) {
                        resolvedSlots++;
                    }
                    if (data.comboOverrideInSlot(slotReference).isPresent()) {
                        comboOverrides++;
                    }
                    if (ModeApi.resolveOverlayAbility(data, slotReference).isPresent()) {
                        modeOverlaySlots++;
                    }
                }
            }
            modePresetCount += data.containerState().modeLoadouts(containerId).size();
        }

        Set<ResourceLocation> trackedSources = new LinkedHashSet<>(data.managedGrantSources());
        addTrackedSources(trackedSources, data.abilityGrantSources());
        addTrackedSources(trackedSources, data.abilityActivationBlockSources());
        addTrackedSources(trackedSources, data.statePolicySources());
        addTrackedSources(trackedSources, data.stateFlagSources());
        addTrackedSources(trackedSources, data.grantBundleSources());
        addTrackedSources(trackedSources, data.artifactUnlockSources());
        addTrackedSources(trackedSources, data.passiveGrantSources());
        addTrackedSources(trackedSources, data.grantedItemSources());
        addTrackedSources(trackedSources, data.recipePermissionSources());

        return new XLibDebugCounters(
                assignedSlots,
                resolvedSlots,
                data.activeModes().size(),
                AbilityDetectorApi.activeDetectors(data).size(),
                IdentityApi.activeIdentities(data).size(),
                (int) ArtifactApi.allArtifacts().stream().filter(artifact -> ArtifactApi.isActive(target, artifact.id())).count(),
                ArtifactApi.unlockedArtifacts(data).size(),
                GrantBundleApi.activeBundles(data).size(),
                StatePolicyApi.activePolicies(data).size(),
                (int) StateFlagApi.activeFlags(data).stream().filter(flagId -> !IdentityApi.isIdentity(flagId)).count(),
                data.cooldowns().size(),
                data.charges().size(),
                data.chargeRechargeTicks().size(),
                data.resources().size(),
                data.comboWindows().size(),
                comboOverrides,
                modeOverlaySlots,
                modePresetCount,
                data.managedGrantSources().size(),
                trackedSources.size(),
                AbilityGrantApi.grantedAbilities(target).size(),
                AbilityGrantApi.blockedAbilities(target).size(),
                PassiveGrantApi.grantedPassives(target).size(),
                GrantedItemGrantApi.grantedItems(target).size(),
                RecipePermissionApi.permissions(target).size(),
                RecipePermissionApi.lockedRecipes(target).size(),
                profileData.selectedProfileIds().size(),
                profileData.pendingGroupIds().size()
        );
    }

    String summary() {
        return "assigned_slots=" + this.assignedSlots
                + ", resolved_slots=" + this.resolvedSlots
                + ", active_modes=" + this.activeModes
                + ", active_detectors=" + this.activeDetectors
                + ", active_identities=" + this.activeIdentities
                + ", active_artifacts=" + this.activeArtifacts
                + ", unlocked_artifacts=" + this.unlockedArtifacts
                + ", active_grant_bundles=" + this.activeGrantBundles
                + ", active_state_policies=" + this.activeStatePolicies
                + ", active_state_flags=" + this.activeStateFlags
                + ", cooldown_entries=" + this.cooldownEntries
                + ", charge_entries=" + this.chargeEntries
                + ", charge_recharge_entries=" + this.chargeRechargeEntries
                + ", resource_entries=" + this.resourceEntries
                + ", combo_windows=" + this.comboWindows
                + ", combo_overrides=" + this.comboOverrides
                + ", mode_overlay_slots=" + this.modeOverlaySlots
                + ", mode_presets=" + this.modePresetCount
                + ", managed_sources=" + this.managedSources
                + ", tracked_source_groups=" + this.trackedSourceGroups
                + ", granted_abilities=" + this.grantedAbilities
                + ", blocked_abilities=" + this.blockedAbilities
                + ", granted_passives=" + this.grantedPassives
                + ", granted_items=" + this.grantedItems
                + ", recipe_permissions=" + this.recipePermissions
                + ", locked_recipes=" + this.lockedRecipes
                + ", selected_profiles=" + this.selectedProfiles
                + ", pending_profile_groups=" + this.pendingProfileGroups;
    }

    JsonObject toJson() {
        JsonObject object = new JsonObject();
        object.addProperty("assigned_slots", this.assignedSlots);
        object.addProperty("resolved_slots", this.resolvedSlots);
        object.addProperty("active_modes", this.activeModes);
        object.addProperty("active_detectors", this.activeDetectors);
        object.addProperty("active_identities", this.activeIdentities);
        object.addProperty("active_artifacts", this.activeArtifacts);
        object.addProperty("unlocked_artifacts", this.unlockedArtifacts);
        object.addProperty("active_grant_bundles", this.activeGrantBundles);
        object.addProperty("active_state_policies", this.activeStatePolicies);
        object.addProperty("active_state_flags", this.activeStateFlags);
        object.addProperty("cooldown_entries", this.cooldownEntries);
        object.addProperty("charge_entries", this.chargeEntries);
        object.addProperty("charge_recharge_entries", this.chargeRechargeEntries);
        object.addProperty("resource_entries", this.resourceEntries);
        object.addProperty("combo_windows", this.comboWindows);
        object.addProperty("combo_overrides", this.comboOverrides);
        object.addProperty("mode_overlay_slots", this.modeOverlaySlots);
        object.addProperty("mode_presets", this.modePresetCount);
        object.addProperty("managed_sources", this.managedSources);
        object.addProperty("tracked_source_groups", this.trackedSourceGroups);
        object.addProperty("granted_abilities", this.grantedAbilities);
        object.addProperty("blocked_abilities", this.blockedAbilities);
        object.addProperty("granted_passives", this.grantedPassives);
        object.addProperty("granted_items", this.grantedItems);
        object.addProperty("recipe_permissions", this.recipePermissions);
        object.addProperty("locked_recipes", this.lockedRecipes);
        object.addProperty("selected_profiles", this.selectedProfiles);
        object.addProperty("pending_profile_groups", this.pendingProfileGroups);
        return object;
    }

    private static void addTrackedSources(
            Set<ResourceLocation> trackedSources,
            Map<ResourceLocation, Set<ResourceLocation>> sourceMap
    ) {
        for (Set<ResourceLocation> sources : sourceMap.values()) {
            trackedSources.addAll(sources);
        }
    }
}
