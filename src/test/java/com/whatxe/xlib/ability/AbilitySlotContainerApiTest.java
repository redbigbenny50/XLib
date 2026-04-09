package com.whatxe.xlib.ability;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class AbilitySlotContainerApiTest {
    @Test
    void slotReferenceValidationOnlyAcceptsPrimaryBarSlots() {
        AbilitySlotContainerApi.bootstrap();
        AbilityData data = AbilityData.empty().withAbilityInSlot(AbilitySlotReference.primary(0), id("ability"));

        assertTrue(AbilitySlotContainerApi.isValidSlotReference(data, AbilitySlotReference.primary(0)));
        assertFalse(AbilitySlotContainerApi.isValidSlotReference(data, new AbilitySlotReference(AbilitySlotContainerApi.PRIMARY_CONTAINER_ID, 1, 0)));
        assertFalse(AbilitySlotContainerApi.isValidSlotReference(data, new AbilitySlotReference(AbilitySlotContainerApi.PRIMARY_CONTAINER_ID, 0, AbilityData.SLOT_COUNT)));
        assertFalse(AbilitySlotContainerApi.isValidSlotReference(data, new AbilitySlotReference(id("aux"), 0, 0)));
    }

    @Test
    void auxiliaryContainerRegistrationIsRejected() {
        AbilitySlotContainerApi.bootstrap();

        assertThrows(IllegalStateException.class, () -> AbilitySlotContainerApi.registerContainer(
                AbilitySlotContainerDefinition.builder(id("aux"), Component.literal("Aux"))
                        .owner(AbilitySlotContainerOwnerType.ADDON_SOURCE, id("aux"))
                        .slotsPerPage(2)
                        .defaultPageCount(2)
                        .build()
        ));

        assertTrue(AbilitySlotContainerApi.hasResolvedContainer(AbilityData.empty(), AbilitySlotContainerApi.PRIMARY_CONTAINER_ID));
        assertFalse(AbilitySlotContainerApi.hasResolvedContainer(AbilityData.empty(), id("aux")));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }
}
