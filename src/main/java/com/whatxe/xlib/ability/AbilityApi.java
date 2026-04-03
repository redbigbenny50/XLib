package com.whatxe.xlib.ability;

import com.whatxe.xlib.XLibRegistryGuard;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

public final class AbilityApi {
    private static final Map<ResourceLocation, AbilityDefinition> ABILITIES = new LinkedHashMap<>();
    private static final Map<ResourceLocation, AbilityResourceDefinition> RESOURCES = new LinkedHashMap<>();
    private static final Map<Integer, ResourceLocation> DEFAULT_LOADOUT = new LinkedHashMap<>();

    private AbilityApi() {}

    public static void bootstrap() {}

    public static AbilityDefinition registerAbility(AbilityDefinition ability) {
        XLibRegistryGuard.ensureMutable("abilities");
        AbilityDefinition previous = ABILITIES.putIfAbsent(ability.id(), ability);
        if (previous != null) {
            throw new IllegalStateException("Duplicate ability registration: " + ability.id());
        }
        return ability;
    }

    public static Optional<AbilityDefinition> unregisterAbility(ResourceLocation id) {
        XLibRegistryGuard.ensureMutable("abilities");
        AbilityDefinition removed = ABILITIES.remove(id);
        if (removed == null) {
            return Optional.empty();
        }

        DEFAULT_LOADOUT.entrySet().removeIf(entry -> entry.getValue().equals(id));
        return Optional.of(removed);
    }

    public static AbilityResourceDefinition registerResource(AbilityResourceDefinition resource) {
        XLibRegistryGuard.ensureMutable("resources");
        AbilityResourceDefinition previous = RESOURCES.putIfAbsent(resource.id(), resource);
        if (previous != null) {
            throw new IllegalStateException("Duplicate resource registration: " + resource.id());
        }
        return resource;
    }

    public static Optional<AbilityResourceDefinition> unregisterResource(ResourceLocation id) {
        XLibRegistryGuard.ensureMutable("resources");
        return Optional.ofNullable(RESOURCES.remove(id));
    }

    public static Optional<AbilityDefinition> findAbility(ResourceLocation id) {
        return Optional.ofNullable(ABILITIES.get(id));
    }

    public static Collection<AbilityDefinition> allAbilities() {
        return List.copyOf(ABILITIES.values());
    }

    public static Optional<AbilityResourceDefinition> findResource(ResourceLocation id) {
        return Optional.ofNullable(RESOURCES.get(id));
    }

    public static Collection<AbilityResourceDefinition> allResources() {
        return List.copyOf(RESOURCES.values());
    }

    public static void setDefaultAbility(int slot, ResourceLocation abilityId) {
        XLibRegistryGuard.ensureMutable("default_loadout");
        if (slot < 0 || slot >= AbilityData.SLOT_COUNT) {
            throw new IllegalArgumentException("Invalid default loadout slot: " + slot);
        }
        DEFAULT_LOADOUT.put(slot, abilityId);
    }

    public static void clearDefaultAbility(int slot) {
        XLibRegistryGuard.ensureMutable("default_loadout");
        if (slot < 0 || slot >= AbilityData.SLOT_COUNT) {
            throw new IllegalArgumentException("Invalid default loadout slot: " + slot);
        }
        DEFAULT_LOADOUT.remove(slot);
    }

    public static void clearDefaultLoadout() {
        XLibRegistryGuard.ensureMutable("default_loadout");
        DEFAULT_LOADOUT.clear();
    }

    public static AbilityData createDefaultData() {
        AbilityData data = AbilityData.empty();
        for (AbilityResourceDefinition resource : RESOURCES.values()) {
            data = data.withResourceAmount(resource.id(), resource.startingAmount());
        }
        for (Map.Entry<Integer, ResourceLocation> entry : DEFAULT_LOADOUT.entrySet()) {
            data = data.withAbilityInSlot(entry.getKey(), entry.getValue());
        }
        return data;
    }

    public static AbilityData sanitizeData(AbilityData data) {
        AbilityData sanitized = AbilityData.empty().withAbilityAccessRestricted(data.abilityAccessRestricted());
        for (int slot = 0; slot < AbilityData.SLOT_COUNT; slot++) {
            ResourceLocation abilityId = data.abilityInSlot(slot).orElse(null);
            if (abilityId != null && ABILITIES.containsKey(abilityId)) {
                sanitized = sanitized.withAbilityInSlot(slot, abilityId);
            }
        }
        for (Map.Entry<ResourceLocation, List<Optional<ResourceLocation>>> entry : data.modeLoadouts().entrySet()) {
            for (int slot = 0; slot < AbilityData.SLOT_COUNT; slot++) {
                ResourceLocation abilityId = data.modeAbilityInSlot(entry.getKey(), slot).orElse(null);
                if (abilityId != null && ABILITIES.containsKey(abilityId)) {
                    sanitized = sanitized.withModeAbilityInSlot(entry.getKey(), slot, abilityId);
                }
            }
        }

        for (Map.Entry<ResourceLocation, Integer> entry : data.cooldowns().entrySet()) {
            if (ABILITIES.containsKey(entry.getKey())) {
                sanitized = sanitized.withCooldown(entry.getKey(), entry.getValue());
                sanitized = sanitized.withRecoveryTickProgress(entry.getKey(), data.recoveryTickProgressFor(entry.getKey()));
            }
        }
        for (ResourceLocation activeAbilityId : data.activeModes()) {
            if (ABILITIES.containsKey(activeAbilityId)) {
                sanitized = sanitized.withMode(activeAbilityId, true);
                sanitized = sanitized.withActiveDuration(activeAbilityId, data.activeDurationFor(activeAbilityId));
            }
        }
        for (Map.Entry<ResourceLocation, Integer> entry : data.comboWindows().entrySet()) {
            if (ABILITIES.containsKey(entry.getKey())) {
                sanitized = sanitized.withComboWindow(entry.getKey(), entry.getValue());
            }
        }
        for (int slot = 0; slot < AbilityData.SLOT_COUNT; slot++) {
            ResourceLocation comboAbilityId = data.comboOverrideInSlot(slot).orElse(null);
            if (comboAbilityId != null && ABILITIES.containsKey(comboAbilityId)) {
                sanitized = sanitized.withComboOverride(slot, comboAbilityId, data.comboOverrideDurationForSlot(slot));
            }
        }
        for (Map.Entry<ResourceLocation, Integer> entry : data.charges().entrySet()) {
            if (ABILITIES.containsKey(entry.getKey())) {
                sanitized = sanitized.withChargeCount(entry.getKey(), entry.getValue());
            }
        }
        for (Map.Entry<ResourceLocation, Integer> entry : data.chargeRechargeTicks().entrySet()) {
            if (ABILITIES.containsKey(entry.getKey())) {
                sanitized = sanitized.withChargeRecharge(entry.getKey(), entry.getValue());
                sanitized = sanitized.withRecoveryTickProgress(entry.getKey(), data.recoveryTickProgressFor(entry.getKey()));
            }
        }
        for (Map.Entry<ResourceLocation, List<ResourceLocation>> entry : data.modeCycleHistory().entrySet()) {
            for (ResourceLocation modeId : entry.getValue()) {
                if (ABILITIES.containsKey(modeId)) {
                    sanitized = sanitized.withModeUsedInCycle(entry.getKey(), modeId);
                }
            }
        }
        for (Map.Entry<ResourceLocation, Set<ResourceLocation>> entry : data.abilityGrantSources().entrySet()) {
            if (ABILITIES.containsKey(entry.getKey())) {
                for (ResourceLocation sourceId : entry.getValue()) {
                    sanitized = sanitized.withAbilityGrantSource(entry.getKey(), sourceId, true);
                }
            }
        }
        for (Map.Entry<ResourceLocation, Set<ResourceLocation>> entry : data.passiveGrantSources().entrySet()) {
            if (PassiveApi.findPassive(entry.getKey()).isPresent()) {
                for (ResourceLocation sourceId : entry.getValue()) {
                    sanitized = sanitized.withPassiveGrantSource(entry.getKey(), sourceId, true);
                }
            }
        }
        for (Map.Entry<ResourceLocation, Set<ResourceLocation>> entry : data.grantedItemSources().entrySet()) {
            if (GrantedItemApi.findGrantedItem(entry.getKey()).isPresent()) {
                for (ResourceLocation sourceId : entry.getValue()) {
                    sanitized = sanitized.withGrantedItemSource(entry.getKey(), sourceId, true);
                }
            }
        }
        for (Map.Entry<ResourceLocation, Set<ResourceLocation>> entry : data.recipePermissionSources().entrySet()) {
            if (RecipePermissionApi.isRestricted(entry.getKey())) {
                for (ResourceLocation sourceId : entry.getValue()) {
                    sanitized = sanitized.withRecipePermissionSource(entry.getKey(), sourceId, true);
                }
            }
        }
        for (Map.Entry<ResourceLocation, Set<ResourceLocation>> entry : data.abilityActivationBlockSources().entrySet()) {
            if (ABILITIES.containsKey(entry.getKey())) {
                for (ResourceLocation sourceId : entry.getValue()) {
                    sanitized = sanitized.withAbilityActivationBlockSource(entry.getKey(), sourceId, true);
                }
            }
        }
        for (ResourceLocation managedSource : data.managedGrantSources()) {
            sanitized = sanitized.withManagedGrantSource(managedSource, true);
        }

        for (AbilityResourceDefinition resource : RESOURCES.values()) {
            double amount = data.resources().containsKey(resource.id())
                    ? Math.min(resource.totalCapacity(), data.resourceAmountExact(resource.id()))
                    : resource.startingAmount();
            sanitized = sanitized.withResourceAmountExact(resource.id(), amount);
            sanitized = sanitized.withResourceRegenDelay(resource.id(), data.resourceRegenDelay(resource.id()));
            sanitized = sanitized.withResourceDecayDelay(resource.id(), data.resourceDecayDelay(resource.id()));
        }

        return sanitized;
    }
}

