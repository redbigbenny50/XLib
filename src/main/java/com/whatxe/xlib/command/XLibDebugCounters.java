package com.whatxe.xlib.command;

import com.google.gson.JsonObject;
import com.whatxe.xlib.ability.AbilityData;
import com.whatxe.xlib.ability.AbilityGrantApi;
import com.whatxe.xlib.ability.AbilityLoadoutApi;
import com.whatxe.xlib.ability.GrantedItemGrantApi;
import com.whatxe.xlib.ability.ModeApi;
import com.whatxe.xlib.ability.PassiveGrantApi;
import com.whatxe.xlib.ability.RecipePermissionApi;
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

    private XLibDebugCounters(
            int assignedSlots,
            int resolvedSlots,
            int activeModes,
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
            int lockedRecipes
    ) {
        this.assignedSlots = assignedSlots;
        this.resolvedSlots = resolvedSlots;
        this.activeModes = activeModes;
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
    }

    static XLibDebugCounters collect(ServerPlayer target) {
        AbilityData data = ModAttachments.get(target);
        int assignedSlots = 0;
        int resolvedSlots = 0;
        int comboOverrides = 0;
        int modeOverlaySlots = 0;
        for (int slot = 0; slot < AbilityData.SLOT_COUNT; slot++) {
            if (data.abilityInSlot(slot).isPresent()) {
                assignedSlots++;
            }
            if (AbilityLoadoutApi.resolvedAbilityId(data, slot).isPresent()) {
                resolvedSlots++;
            }
            if (data.comboOverrideInSlot(slot).isPresent()) {
                comboOverrides++;
            }
            if (ModeApi.resolveOverlayAbility(data, slot).isPresent()) {
                modeOverlaySlots++;
            }
        }

        Set<ResourceLocation> trackedSources = new LinkedHashSet<>(data.managedGrantSources());
        addTrackedSources(trackedSources, data.abilityGrantSources());
        addTrackedSources(trackedSources, data.abilityActivationBlockSources());
        addTrackedSources(trackedSources, data.passiveGrantSources());
        addTrackedSources(trackedSources, data.grantedItemSources());
        addTrackedSources(trackedSources, data.recipePermissionSources());

        return new XLibDebugCounters(
                assignedSlots,
                resolvedSlots,
                data.activeModes().size(),
                data.cooldowns().size(),
                data.charges().size(),
                data.chargeRechargeTicks().size(),
                data.resources().size(),
                data.comboWindows().size(),
                comboOverrides,
                modeOverlaySlots,
                data.modeLoadouts().size(),
                data.managedGrantSources().size(),
                trackedSources.size(),
                AbilityGrantApi.grantedAbilities(target).size(),
                AbilityGrantApi.blockedAbilities(target).size(),
                PassiveGrantApi.grantedPassives(target).size(),
                GrantedItemGrantApi.grantedItems(target).size(),
                RecipePermissionApi.permissions(target).size(),
                RecipePermissionApi.lockedRecipes(target).size()
        );
    }

    String summary() {
        return "assigned_slots=" + this.assignedSlots
                + ", resolved_slots=" + this.resolvedSlots
                + ", active_modes=" + this.activeModes
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
                + ", locked_recipes=" + this.lockedRecipes;
    }

    JsonObject toJson() {
        JsonObject object = new JsonObject();
        object.addProperty("assigned_slots", this.assignedSlots);
        object.addProperty("resolved_slots", this.resolvedSlots);
        object.addProperty("active_modes", this.activeModes);
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
