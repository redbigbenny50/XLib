package com.whatxe.xlib.ability;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class GrantConditionsTest {
    private static final ResourceLocation MODE_ID = id("grant_mode");
    private static final ResourceLocation RESOURCE_ID = id("grant_resource");

    @Test
    void requirementAdaptersAndCompositionReuseAbilityRequirements() {
        GrantCondition modeCondition = GrantConditions.fromRequirement(AbilityRequirements.modeActive(MODE_ID));
        GrantCondition resourceCondition = GrantConditions.resourceAtLeast(RESOURCE_ID, 3);
        GrantCondition combined = modeCondition.and(resourceCondition);

        AbilityData modeOnly = AbilityData.empty().withMode(MODE_ID, true);
        AbilityData complete = modeOnly.withResourceAmount(RESOURCE_ID, 3);

        assertTrue(modeCondition.test(null, modeOnly, null));
        assertFalse(modeCondition.test(null, AbilityData.empty(), null));
        assertTrue(GrantConditions.any(modeCondition, resourceCondition).test(null, modeOnly, null));
        assertTrue(combined.test(null, complete, null));
        assertFalse(combined.test(null, modeOnly, null));
        assertTrue(GrantConditions.not(modeCondition).test(null, AbilityData.empty(), null));
        assertTrue(GrantConditions.allMatch(null, complete, null, List.of(modeCondition, resourceCondition)));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }
}
