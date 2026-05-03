package com.whatxe.xlib.ability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class AbilityContainerStateTest {
    private static final ResourceLocation CONTAINER_ID = id("auxiliary_container");
    private static final ResourceLocation MODE_ID = id("mode_profile");
    private static final ResourceLocation BASE_ABILITY_ID = id("base_ability");
    private static final ResourceLocation MODE_ABILITY_ID = id("mode_ability");
    private static final ResourceLocation COMBO_ABILITY_ID = id("combo_ability");

    @Test
    void auxiliaryAssignmentsAndPagesAreIgnored() {
        AbilitySlotContainerApi.bootstrap();

        AbilitySlotReference primarySlot = AbilitySlotReference.primary(2);
        AbilitySlotReference pageZeroSlot = new AbilitySlotReference(CONTAINER_ID, 0, 0);
        AbilitySlotReference pageOneSlot = new AbilitySlotReference(CONTAINER_ID, 1, 1);
        AbilityData data = AbilityData.empty()
                .withAbilityInSlot(primarySlot, BASE_ABILITY_ID)
                .withAbilityInSlot(pageZeroSlot, MODE_ABILITY_ID)
                .withModeAbilityInSlot(MODE_ID, pageOneSlot, MODE_ABILITY_ID)
                .withComboOverride(pageOneSlot, COMBO_ABILITY_ID, 12)
                .withContainerActivePage(CONTAINER_ID, 1);

        assertEquals(BASE_ABILITY_ID, data.abilityInSlot(primarySlot).orElseThrow());
        assertTrue(data.abilityInSlot(pageZeroSlot).isEmpty());
        assertTrue(data.modeAbilityInSlot(MODE_ID, pageOneSlot).isEmpty());
        assertTrue(data.comboOverrideInSlot(pageOneSlot).isEmpty());
        assertEquals(0, data.activeContainerPage(CONTAINER_ID));
        assertEquals(0, AbilitySlotContainerApi.resolvedSlotsPerPage(data, CONTAINER_ID));
        assertEquals(AbilityData.SLOT_COUNT, AbilitySlotContainerApi.resolvedSlotsPerPage(data, AbilitySlotContainerApi.PRIMARY_CONTAINER_ID));
        assertTrue(AbilityLoadoutApi.resolvedAbilityId(data, primarySlot).filter(BASE_ABILITY_ID::equals).isPresent());
        assertFalse(data.containerState().hasContainer(CONTAINER_ID));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }
}
