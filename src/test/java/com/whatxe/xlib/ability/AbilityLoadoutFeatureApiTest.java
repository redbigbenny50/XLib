package com.whatxe.xlib.ability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AbilityLoadoutFeatureApiTest {
    private static final ResourceLocation MANAGEMENT_SOURCE_ID = id("management_source");
    private static final ResourceLocation QUICK_SWITCH_SOURCE_ID = id("quick_switch_source");
    private static final ResourceLocation SILENT_SOURCE_ID = id("silent_source");

    @AfterEach
    void clearFeatures() {
        AbilityLoadoutFeatureApi.clearFeatures();
    }

    @Test
    void decisionsMergeAcrossRegisteredFeatures() {
        AbilityLoadoutFeatureApi.registerFeature(MANAGEMENT_SOURCE_ID, AbilityLoadoutFeature.managementOnly(
                (player, data) -> AbilityLoadoutFeatureDecision.managementOnly()
        ));
        AbilityLoadoutFeatureApi.registerFeature(QUICK_SWITCH_SOURCE_ID, AbilityLoadoutFeature.managementAndQuickSwitch(
                (player, data) -> AbilityLoadoutFeatureDecision.managementAndQuickSwitch()
        ));

        AbilityLoadoutFeatureDecision decision = AbilityLoadoutFeatureApi.decision(null, AbilityData.empty());

        assertTrue(decision.managementEnabled());
        assertTrue(decision.quickSwitchEnabled());
    }

    @Test
    void quickSwitchDecisionDoesNotLeakFromFeaturesThatDoNotAdvertiseKeybinds() {
        AbilityLoadoutFeatureApi.registerFeature(SILENT_SOURCE_ID, AbilityLoadoutFeature.managementOnly(
                (player, data) -> AbilityLoadoutFeatureDecision.managementAndQuickSwitch()
        ));

        AbilityLoadoutFeatureDecision decision = AbilityLoadoutFeatureApi.decision(null, AbilityData.empty());

        assertTrue(decision.managementEnabled());
        assertFalse(decision.quickSwitchEnabled());
        assertFalse(AbilityLoadoutFeatureApi.shouldRegisterQuickSwitchKeybind());
    }

    @Test
    void quickSwitchKeybindRegistrationDependsOnAdvertisedFeatures() {
        assertFalse(AbilityLoadoutFeatureApi.shouldRegisterQuickSwitchKeybind());

        AbilityLoadoutFeatureApi.registerFeature(QUICK_SWITCH_SOURCE_ID, AbilityLoadoutFeature.alwaysEnabledWithQuickSwitch());

        assertTrue(AbilityLoadoutFeatureApi.shouldRegisterQuickSwitchKeybind());
        assertEquals(1, AbilityLoadoutFeatureApi.features().size());
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }
}
