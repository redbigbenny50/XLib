package com.whatxe.xlib.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class UpgradeApiTest {
    private static final ResourceLocation POINT_TYPE_ID = id("meat_points");
    private static final ResourceLocation TRACK_A_ID = id("track_a");
    private static final ResourceLocation TRACK_B_ID = id("track_b");
    private static final ResourceLocation PARENT_NODE_ID = id("parent_node");
    private static final ResourceLocation CHILD_NODE_ID = id("child_node");
    private static final ResourceLocation TRACK_A_ROOT_ID = id("track_a_root");
    private static final ResourceLocation TRACK_A_CHILD_ID = id("track_a_child");
    private static final ResourceLocation TRACK_B_ROOT_ID = id("track_b_root");
    private static final ResourceLocation COUNTER_ID = id("meat_eaten");
    private static final ResourceLocation REWARDED_ABILITY_ID = id("rewarded_ability");
    private static final ResourceLocation OTHER_ABILITY_ID = id("other_ability");

    @Test
    void unlockFailureRespectsPrerequisitesCostsAndCounters() {
        unregisterFixtures();
        try {
            UpgradeApi.registerPointType(UpgradePointType.of(POINT_TYPE_ID));
            UpgradeApi.registerNode(UpgradeNodeDefinition.builder(PARENT_NODE_ID).build());
            UpgradeApi.registerNode(UpgradeNodeDefinition.builder(CHILD_NODE_ID)
                    .requiredNode(PARENT_NODE_ID)
                    .pointCost(POINT_TYPE_ID, 5)
                    .requirement(UpgradeRequirements.counterAtLeast(COUNTER_ID, 3))
                    .build());

            UpgradeProgressData missingAll = UpgradeProgressData.empty();
            assertTrue(UpgradeApi.firstUnlockFailure(null, missingAll, UpgradeApi.findNode(CHILD_NODE_ID).orElseThrow()).isPresent());

            UpgradeProgressData missingCounter = UpgradeProgressData.empty()
                    .withUnlockedNode(PARENT_NODE_ID, true)
                    .withPoints(POINT_TYPE_ID, 5);
            assertTrue(UpgradeApi.firstUnlockFailure(null, missingCounter, UpgradeApi.findNode(CHILD_NODE_ID).orElseThrow()).isPresent());

            UpgradeProgressData ready = missingCounter.withCounter(COUNTER_ID, 3);
            assertTrue(UpgradeApi.firstUnlockFailure(null, ready, UpgradeApi.findNode(CHILD_NODE_ID).orElseThrow()).isEmpty());
        } finally {
            unregisterFixtures();
        }
    }

    @Test
    void sanitizeDataPrunesChildNodesWhenParentsDisappear() {
        unregisterFixtures();
        try {
            UpgradeApi.registerNode(UpgradeNodeDefinition.builder(PARENT_NODE_ID).build());
            UpgradeApi.registerNode(UpgradeNodeDefinition.builder(CHILD_NODE_ID)
                    .requiredNode(PARENT_NODE_ID)
                    .build());

            UpgradeProgressData sanitized = UpgradeApi.sanitizeData(
                    UpgradeProgressData.empty().withUnlockedNode(CHILD_NODE_ID, true)
            );
            assertFalse(sanitized.hasUnlockedNode(CHILD_NODE_ID));

            UpgradeProgressData kept = UpgradeApi.sanitizeData(
                    UpgradeProgressData.empty()
                            .withUnlockedNode(PARENT_NODE_ID, true)
                            .withUnlockedNode(CHILD_NODE_ID, true)
            );
            assertTrue(kept.hasUnlockedNode(PARENT_NODE_ID));
            assertTrue(kept.hasUnlockedNode(CHILD_NODE_ID));
        } finally {
            unregisterFixtures();
        }
    }

    @Test
    void exclusiveTracksHideAndBlockTheOtherPathOnceChosen() {
        unregisterFixtures();
        try {
            UpgradeApi.registerTrack(UpgradeTrackDefinition.builder(TRACK_A_ID)
                    .exclusiveWith(TRACK_B_ID)
                    .rootNode(TRACK_A_ROOT_ID)
                    .build());
            UpgradeApi.registerTrack(UpgradeTrackDefinition.builder(TRACK_B_ID)
                    .exclusiveWith(TRACK_A_ID)
                    .rootNode(TRACK_B_ROOT_ID)
                    .build());
            UpgradeApi.registerNode(UpgradeNodeDefinition.builder(TRACK_A_ROOT_ID)
                    .track(TRACK_A_ID)
                    .build());
            UpgradeApi.registerNode(UpgradeNodeDefinition.builder(TRACK_B_ROOT_ID)
                    .track(TRACK_B_ID)
                    .build());

            UpgradeProgressData chosenA = UpgradeProgressData.empty().withUnlockedNode(TRACK_A_ROOT_ID, true);
            assertTrue(UpgradeApi.isTrackBlocked(chosenA, TRACK_B_ID));
            assertFalse(UpgradeApi.isTrackBlocked(chosenA, TRACK_A_ID));
            assertTrue(UpgradeApi.firstUnlockFailure(null, chosenA, UpgradeApi.findNode(TRACK_B_ROOT_ID).orElseThrow()).isPresent());
            assertEquals(
                    java.util.List.of(TRACK_A_ID),
                    UpgradeApi.visibleTracks(chosenA).stream().map(UpgradeTrackDefinition::id).toList()
            );
        } finally {
            unregisterFixtures();
        }
    }

    @Test
    void revokeTrackRemovesUnlockedNodesAndUnblocksExclusiveTracks() {
        unregisterFixtures();
        try {
            UpgradeApi.registerTrack(UpgradeTrackDefinition.builder(TRACK_A_ID)
                    .exclusiveWith(TRACK_B_ID)
                    .rootNode(TRACK_A_ROOT_ID)
                    .build());
            UpgradeApi.registerTrack(UpgradeTrackDefinition.builder(TRACK_B_ID)
                    .exclusiveWith(TRACK_A_ID)
                    .rootNode(TRACK_B_ROOT_ID)
                    .build());
            UpgradeApi.registerNode(UpgradeNodeDefinition.builder(TRACK_A_ROOT_ID)
                    .track(TRACK_A_ID)
                    .build());
            UpgradeApi.registerNode(UpgradeNodeDefinition.builder(TRACK_A_CHILD_ID)
                    .track(TRACK_A_ID)
                    .requiredNode(TRACK_A_ROOT_ID)
                    .build());
            UpgradeApi.registerNode(UpgradeNodeDefinition.builder(TRACK_B_ROOT_ID)
                    .track(TRACK_B_ID)
                    .build());

            UpgradeProgressData chosenA = UpgradeProgressData.empty()
                    .withUnlockedNode(TRACK_A_ROOT_ID, true)
                    .withUnlockedNode(TRACK_A_CHILD_ID, true);
            UpgradeProgressData revoked = UpgradeApi.revokeTrackNodes(chosenA, TRACK_A_ID);

            assertFalse(revoked.hasUnlockedNode(TRACK_A_ROOT_ID));
            assertFalse(revoked.hasUnlockedNode(TRACK_A_CHILD_ID));
            assertFalse(UpgradeApi.isTrackBlocked(revoked, TRACK_B_ID));
            assertEquals(
                    java.util.List.of(TRACK_A_ID, TRACK_B_ID),
                    UpgradeApi.visibleTracks(revoked).stream().map(UpgradeTrackDefinition::id).toList()
            );
        } finally {
            unregisterFixtures();
        }
    }

    @Test
    void revokeAbilityRewardNodesOnlyRemovesMatchingProgressionNodes() {
        unregisterFixtures();
        try {
            UpgradeApi.registerNode(UpgradeNodeDefinition.builder(TRACK_A_ROOT_ID)
                    .rewards(UpgradeRewardBundle.builder()
                            .grantAbility(REWARDED_ABILITY_ID)
                            .build())
                    .build());
            UpgradeApi.registerNode(UpgradeNodeDefinition.builder(TRACK_A_CHILD_ID)
                    .rewards(UpgradeRewardBundle.builder()
                            .grantAbility(OTHER_ABILITY_ID)
                            .build())
                    .build());

            UpgradeProgressData unlocked = UpgradeProgressData.empty()
                    .withUnlockedNode(TRACK_A_ROOT_ID, true)
                    .withUnlockedNode(TRACK_A_CHILD_ID, true);
            UpgradeProgressData revoked = UpgradeApi.revokeAbilityRewardNodes(
                    unlocked,
                    java.util.List.of(
                            UpgradeApi.findNode(TRACK_A_ROOT_ID).orElseThrow().sourceId(),
                            UpgradeApi.findNode(TRACK_A_CHILD_ID).orElseThrow().sourceId()
                    ),
                    REWARDED_ABILITY_ID
            );

            assertFalse(revoked.hasUnlockedNode(TRACK_A_ROOT_ID));
            assertTrue(revoked.hasUnlockedNode(TRACK_A_CHILD_ID));
        } finally {
            unregisterFixtures();
        }
    }

    private static void unregisterFixtures() {
        UpgradeApi.unregisterNode(TRACK_A_ROOT_ID);
        UpgradeApi.unregisterNode(TRACK_A_CHILD_ID);
        UpgradeApi.unregisterNode(TRACK_B_ROOT_ID);
        UpgradeApi.unregisterNode(PARENT_NODE_ID);
        UpgradeApi.unregisterNode(CHILD_NODE_ID);
        UpgradeApi.unregisterTrack(TRACK_A_ID);
        UpgradeApi.unregisterTrack(TRACK_B_ID);
        UpgradeApi.unregisterPointType(POINT_TYPE_ID);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }
}
