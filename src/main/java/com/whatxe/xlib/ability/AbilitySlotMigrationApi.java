package com.whatxe.xlib.ability;

import com.whatxe.xlib.XLibRegistryGuard;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

public final class AbilitySlotMigrationApi {
    private static final Comparator<AbilitySlotMigrationPlan> PRIORITY_ORDER = Comparator
            .comparingInt(AbilitySlotMigrationPlan::priority)
            .reversed()
            .thenComparing(plan -> plan.id().toString());
    private static final Map<ResourceLocation, AbilitySlotMigrationPlan> PLANS = new LinkedHashMap<>();

    private AbilitySlotMigrationApi() {}

    public static void bootstrap() {}

    public static AbilitySlotMigrationPlan registerPlan(AbilitySlotMigrationPlan plan) {
        XLibRegistryGuard.ensureMutable("ability_slot_migrations");
        AbilitySlotMigrationPlan resolvedPlan = Objects.requireNonNull(plan, "plan");
        AbilitySlotMigrationPlan previous = PLANS.putIfAbsent(resolvedPlan.id(), resolvedPlan);
        if (previous != null) {
            throw new IllegalStateException("Duplicate ability slot migration plan registration: " + resolvedPlan.id());
        }
        return resolvedPlan;
    }

    public static Optional<AbilitySlotMigrationPlan> unregisterPlan(ResourceLocation planId) {
        XLibRegistryGuard.ensureMutable("ability_slot_migrations");
        return Optional.ofNullable(PLANS.remove(Objects.requireNonNull(planId, "planId")));
    }

    public static Collection<AbilitySlotMigrationPlan> allPlans() {
        return PLANS.values().stream().sorted(PRIORITY_ORDER).toList();
    }

    public static void clearPlans() {
        XLibRegistryGuard.ensureMutable("ability_slot_migrations");
        PLANS.clear();
    }

    public static AbilityContainerState applyLegacyMigrations(AbilityContainerState state) {
        AbilityContainerState migrated = Objects.requireNonNull(state, "state");
        for (AbilitySlotMigrationPlan plan : allPlans()) {
            migrated = applyPlan(migrated, plan);
        }
        return migrated;
    }

    private static AbilityContainerState applyPlan(AbilityContainerState state, AbilitySlotMigrationPlan plan) {
        AbilityContainerState migrated = state;
        for (Map.Entry<Integer, AbilitySlotReference> entry : plan.baseSlotRemaps().entrySet()) {
            AbilitySlotReference source = AbilitySlotReference.primary(entry.getKey());
            if (!AbilitySlotContainerApi.isPrimarySlotReference(entry.getValue())) {
                continue;
            }
            Optional<ResourceLocation> abilityId = migrated.abilityInSlot(source);
            if (abilityId.isEmpty()) {
                continue;
            }
            migrated = migrated.withAbilityInSlot(entry.getValue(), abilityId.get()).withAbilityInSlot(source, null);
        }
        for (Map.Entry<Integer, AbilitySlotReference> entry : plan.comboOverrideRemaps().entrySet()) {
            AbilitySlotReference source = AbilitySlotReference.primary(entry.getKey());
            if (!AbilitySlotContainerApi.isPrimarySlotReference(entry.getValue())) {
                continue;
            }
            Optional<ResourceLocation> overrideId = migrated.comboOverrideInSlot(source);
            if (overrideId.isEmpty()) {
                continue;
            }
            int duration = migrated.comboOverrideDurationForSlot(source);
            migrated = migrated.withComboOverride(entry.getValue(), overrideId.get(), duration).withComboOverride(source, null, 0);
        }
        for (Map.Entry<ResourceLocation, Map<Integer, AbilitySlotReference>> modeEntry : plan.modeSlotRemaps().entrySet()) {
            for (Map.Entry<Integer, AbilitySlotReference> slotEntry : modeEntry.getValue().entrySet()) {
                AbilitySlotReference source = AbilitySlotReference.primary(slotEntry.getKey());
                if (!AbilitySlotContainerApi.isPrimarySlotReference(slotEntry.getValue())) {
                    continue;
                }
                Optional<ResourceLocation> abilityId = migrated.modeAbilityInSlot(modeEntry.getKey(), source);
                if (abilityId.isEmpty()) {
                    continue;
                }
                migrated = migrated.withModeAbilityInSlot(modeEntry.getKey(), slotEntry.getValue(), abilityId.get())
                        .withModeAbilityInSlot(modeEntry.getKey(), source, null);
            }
        }
        return migrated;
    }
}
