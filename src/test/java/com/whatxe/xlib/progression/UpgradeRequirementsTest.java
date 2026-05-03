package com.whatxe.xlib.progression;

import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class UpgradeRequirementsTest {
    private static final ResourceLocation TRACK_ID = id("composite_track");
    private static final ResourceLocation NODE_A_ID = id("composite_node_a");
    private static final ResourceLocation NODE_B_ID = id("composite_node_b");
    private static final ResourceLocation COUNTER_ID = id("composite_counter");

    @Test
    void anyAndAllComposeExistingRequirements() {
        UpgradeRequirement counterRequirement = UpgradeRequirements.counterAtLeast(COUNTER_ID, 2);
        UpgradeRequirement nodeRequirement = UpgradeRequirements.nodeUnlocked(NODE_A_ID);

        UpgradeRequirement anyRequirement = UpgradeRequirements.any(counterRequirement, nodeRequirement);
        UpgradeRequirement allRequirement = UpgradeRequirements.all(counterRequirement, nodeRequirement);

        UpgradeProgressData counterOnly = UpgradeProgressData.empty().withCounter(COUNTER_ID, 2);
        UpgradeProgressData allSatisfied = counterOnly.withUnlockedNode(NODE_A_ID, true);

        assertTrue(anyRequirement.validate(null, counterOnly).isEmpty());
        assertTrue(allRequirement.validate(null, counterOnly).isPresent());
        assertTrue(allRequirement.validate(null, allSatisfied).isEmpty());
    }

    @Test
    void trackCompletedAndAnyNodeUnlockedUseRegisteredTrackState() {
        unregisterFixtures();
        try {
            UpgradeApi.registerTrack(UpgradeTrackDefinition.builder(TRACK_ID)
                    .rootNode(NODE_A_ID)
                    .build());
            UpgradeApi.registerNode(UpgradeNodeDefinition.builder(NODE_A_ID)
                    .track(TRACK_ID)
                    .build());
            UpgradeApi.registerNode(UpgradeNodeDefinition.builder(NODE_B_ID)
                    .track(TRACK_ID)
                    .requiredNode(NODE_A_ID)
                    .build());

            UpgradeRequirement trackCompleted = UpgradeRequirements.trackCompleted(TRACK_ID);
            UpgradeRequirement anyNodeUnlocked = UpgradeRequirements.anyNodeUnlocked(NODE_A_ID, NODE_B_ID);

            UpgradeProgressData partial = UpgradeProgressData.empty().withUnlockedNode(NODE_A_ID, true);
            UpgradeProgressData complete = partial.withUnlockedNode(NODE_B_ID, true);

            assertTrue(trackCompleted.validate(null, partial).isPresent());
            assertTrue(trackCompleted.validate(null, complete).isEmpty());
            assertTrue(anyNodeUnlocked.validate(null, partial).isEmpty());
        } finally {
            unregisterFixtures();
        }
    }

    private static void unregisterFixtures() {
        UpgradeApi.unregisterNode(NODE_A_ID);
        UpgradeApi.unregisterNode(NODE_B_ID);
        UpgradeApi.unregisterTrack(TRACK_ID);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }
}
