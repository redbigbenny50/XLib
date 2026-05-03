package com.whatxe.xlib.ability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class AbilityControlProfileApiTest {
    private static final ResourceLocation ABILITY_ID = id("bound_ability");

    @Test
    void primaryContainerExposesBuiltInControlBindings() {
        AbilityControlProfileApi.bootstrap();
        AbilitySlotContainerApi.bootstrap();
        AbilityData data = AbilityData.empty().withAbilityInSlot(AbilitySlotReference.primary(0), ABILITY_ID);

        assertEquals(AbilityData.SLOT_COUNT, AbilityControlProfileApi.activeBindings(null, data).stream()
                .filter(binding -> AbilitySlotContainerApi.PRIMARY_CONTAINER_ID.equals(binding.action().containerId()))
                .count());
        assertTrue(AbilityControlProfileApi.activeBindings(null, data).stream()
                .anyMatch(binding -> binding.action().containerId() != null
                        && binding.action().containerId().equals(AbilitySlotContainerApi.PRIMARY_CONTAINER_ID)
                        && binding.trigger().hintLabel().equals("1")));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }
}
