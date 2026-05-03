package com.whatxe.xlib.ability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class SimpleContextGrantProviderTest {
    private static final ResourceLocation PROVIDER_ID = id("requirement_provider");
    private static final ResourceLocation SOURCE_ID = id("requirement_source");
    private static final ResourceLocation MODE_ID = id("requirement_mode");
    private static final ResourceLocation ABILITY_ID = id("granted_ability");
    private static final ResourceLocation STATE_POLICY_ID = id("granted_policy");
    private static final ResourceLocation STATE_FLAG_ID = id("granted_flag");

    @Test
    void builderCanAdaptAbilityRequirementsIntoContextConditions() {
        SimpleContextGrantProvider provider = SimpleContextGrantProvider.builder(PROVIDER_ID, SOURCE_ID)
                .when(AbilityRequirements.modeActive(MODE_ID))
                .grantAbility(ABILITY_ID)
                .build();

        Collection<ContextGrantSnapshot> activeSnapshots = provider.collect(null, AbilityData.empty().withMode(MODE_ID, true));

        assertEquals(1, activeSnapshots.size());
        assertTrue(activeSnapshots.iterator().next().abilities().contains(ABILITY_ID));
        assertTrue(provider.collect(null, AbilityData.empty()).isEmpty());
    }

    @Test
    void builderCanGrantStatePoliciesIntoContextSnapshots() {
        SimpleContextGrantProvider provider = SimpleContextGrantProvider.builder(PROVIDER_ID, SOURCE_ID)
                .grantStatePolicy(STATE_POLICY_ID)
                .grantStateFlag(STATE_FLAG_ID)
                .build();

        Collection<ContextGrantSnapshot> snapshots = provider.collect(null, AbilityData.empty());

        assertEquals(1, snapshots.size());
        assertTrue(snapshots.iterator().next().statePolicies().contains(STATE_POLICY_ID));
        assertTrue(snapshots.iterator().next().stateFlags().contains(STATE_FLAG_ID));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }
}
