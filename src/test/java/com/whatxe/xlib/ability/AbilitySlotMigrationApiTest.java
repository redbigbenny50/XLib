package com.whatxe.xlib.ability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AbilitySlotMigrationApiTest {
    private static final ResourceLocation PLAN_ID = id("migrate_primary");
    private static final ResourceLocation MODE_ID = id("mode");
    private static final ResourceLocation ABILITY_ID = id("base_ability");
    private static final ResourceLocation COMBO_ID = id("combo_ability");
    private static final ResourceLocation MODE_ABILITY_ID = id("mode_ability");
    private static final ResourceLocation TARGET_CONTAINER_ID = id("aux");

    @AfterEach
    void clearPlans() {
        AbilitySlotMigrationApi.clearPlans();
    }

    @Test
    void legacyPlansIgnoreAuxiliaryTargetsAndKeepPrimaryAssignments() {
        AbilitySlotMigrationApi.registerPlan(AbilitySlotMigrationPlan.builder(PLAN_ID)
                .remapBaseSlot(4, new AbilitySlotReference(TARGET_CONTAINER_ID, 0, 0))
                .remapComboOverride(4, new AbilitySlotReference(TARGET_CONTAINER_ID, 0, 1))
                .remapModeSlot(MODE_ID, 4, new AbilitySlotReference(TARGET_CONTAINER_ID, 1, 0))
                .build());

        AbilityContainerState legacy = AbilityContainerState.fromLegacy(
                legacyList(ABILITY_ID, 4),
                legacyList(COMBO_ID, 4),
                List.of(0, 0, 0, 0, 12, 0, 0, 0, 0),
                Map.of(MODE_ID, legacyList(MODE_ABILITY_ID, 4))
        );

        AbilityContainerState migrated = AbilitySlotMigrationApi.applyLegacyMigrations(legacy);

        assertEquals(ABILITY_ID, migrated.abilityInSlot(AbilitySlotReference.primary(4)).orElseThrow());
        assertEquals(COMBO_ID, migrated.comboOverrideInSlot(AbilitySlotReference.primary(4)).orElseThrow());
        assertEquals(12, migrated.comboOverrideDurationForSlot(AbilitySlotReference.primary(4)));
        assertEquals(MODE_ABILITY_ID, migrated.modeAbilityInSlot(MODE_ID, AbilitySlotReference.primary(4)).orElseThrow());
        assertEquals(Optional.empty(), migrated.abilityInSlot(new AbilitySlotReference(TARGET_CONTAINER_ID, 0, 0)));
    }

    @Test
    void higherPriorityPlansWinWhenMultiplePlansTargetSameLegacySlot() {
        AbilitySlotReference highTarget = AbilitySlotReference.primary(1);
        AbilitySlotReference lowTarget = AbilitySlotReference.primary(0);
        AbilitySlotMigrationApi.registerPlan(AbilitySlotMigrationPlan.builder(id("low"))
                .priority(0)
                .remapBaseSlot(4, lowTarget)
                .build());
        AbilitySlotMigrationApi.registerPlan(AbilitySlotMigrationPlan.builder(id("high"))
                .priority(10)
                .remapBaseSlot(4, highTarget)
                .build());

        AbilityContainerState migrated = AbilitySlotMigrationApi.applyLegacyMigrations(
                AbilityContainerState.fromLegacy(
                        legacyList(ABILITY_ID, 4),
                        java.util.Collections.nCopies(AbilityData.SLOT_COUNT, Optional.empty()),
                        java.util.Collections.nCopies(AbilityData.SLOT_COUNT, 0),
                        Map.of()
                )
        );

        assertEquals(Optional.empty(), migrated.abilityInSlot(AbilitySlotReference.primary(4)));
        assertEquals(ABILITY_ID, migrated.abilityInSlot(highTarget).orElseThrow());
        assertEquals(Optional.empty(), migrated.abilityInSlot(lowTarget));
    }

    @Test
    void unregisterPlanRemovesItFromTheOrderedPlanList() {
        AbilitySlotMigrationPlan lowPlan = AbilitySlotMigrationPlan.builder(id("ordered_low")).priority(1).build();
        AbilitySlotMigrationPlan highPlan = AbilitySlotMigrationPlan.builder(id("ordered_high")).priority(5).build();
        AbilitySlotMigrationApi.registerPlan(lowPlan);
        AbilitySlotMigrationApi.registerPlan(highPlan);

        assertEquals(List.of(highPlan, lowPlan), List.copyOf(AbilitySlotMigrationApi.allPlans()));
        assertEquals(highPlan, AbilitySlotMigrationApi.unregisterPlan(highPlan.id()).orElseThrow());
        assertEquals(List.of(lowPlan), List.copyOf(AbilitySlotMigrationApi.allPlans()));
        assertTrue(AbilitySlotMigrationApi.unregisterPlan(highPlan.id()).isEmpty());
    }

    private static List<Optional<ResourceLocation>> legacyList(ResourceLocation abilityId, int slotIndex) {
        List<Optional<ResourceLocation>> slots = new java.util.ArrayList<>(java.util.Collections.nCopies(AbilityData.SLOT_COUNT, Optional.empty()));
        slots.set(slotIndex, Optional.of(abilityId));
        return List.copyOf(slots);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }
}
