package com.whatxe.xlib.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.whatxe.xlib.ability.AbilitySlotContainerApi;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AbilityContainerLayoutApiTest {
    private static final ResourceLocation CONTAINER_ID = id("layout_container");

    @AfterEach
    void restoreDefaults() {
        AbilityContainerLayoutApi.restoreDefaults();
    }

    @Test
    void registeredLayoutsExposePrimaryStripMetadata() {
        AbilitySlotContainerApi.bootstrap();
        AbilityContainerLayoutApi.restoreDefaults();
        AbilityContainerLayoutDefinition definition = AbilityContainerLayoutApi.resolvedLayout(AbilitySlotContainerApi.PRIMARY_CONTAINER_ID);
        AbilitySlotLayoutPlanner.LayoutPlan plan = AbilitySlotLayoutPlanner.plan(definition, 4, 36, 20, 4);

        assertEquals(AbilitySlotLayoutMode.STRIP, definition.layoutMode());
        assertFalse(definition.showPageTabs());
        assertEquals(4, plan.slots().size());
        assertTrue(AbilitySlotLayoutPlanner.planPageTabs(definition, plan, 1, 18, 16, 4).isEmpty());
    }

    @Test
    void auxiliaryLayoutRegistrationIsRejected() {
        AbilitySlotContainerApi.bootstrap();
        AbilityContainerLayoutApi.restoreDefaults();

        assertThrows(IllegalStateException.class, () -> AbilityContainerLayoutApi.register(
                AbilityContainerLayoutDefinition.builder(CONTAINER_ID)
                        .layoutMode(AbilitySlotLayoutMode.RADIAL)
                        .radialRadius(28)
                        .build()
        ));
    }

    @Test
    void restoreDefaultsClearsCustomLayoutsAndFallbackUsesDefaults() {
        AbilityContainerLayoutApi.restoreDefaults();
        assertFalse(AbilityContainerLayoutApi.unregister(AbilitySlotContainerApi.PRIMARY_CONTAINER_ID).isPresent());

        AbilityContainerLayoutApi.restoreDefaults();

        assertTrue(AbilityContainerLayoutApi.find(AbilitySlotContainerApi.PRIMARY_CONTAINER_ID).isPresent());
        assertFalse(AbilityContainerLayoutApi.find(CONTAINER_ID).isPresent());
        assertEquals(1, AbilityContainerLayoutApi.allLayouts().size());

        AbilityContainerLayoutDefinition fallback = AbilityContainerLayoutApi.resolvedLayout(CONTAINER_ID);
        assertEquals(AbilitySlotContainerApi.PRIMARY_CONTAINER_ID, fallback.containerId());
        assertEquals(AbilitySlotLayoutMode.STRIP, fallback.layoutMode());
        assertEquals(AbilitySlotLayoutAnchor.BOTTOM_CENTER, fallback.anchor());
        assertFalse(fallback.showPageTabs());
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }
}
