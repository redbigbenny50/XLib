package com.whatxe.xlib.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class LifecycleStageApiTest {
    private static final ResourceLocation STAGE_A = id("stage/larval");
    private static final ResourceLocation STAGE_B = id("stage/juvenile");
    private static final ResourceLocation STAGE_C = id("stage/adult");
    private static final ResourceLocation STALE_STAGE = id("stage/stale");

    @Test
    void registrationAndLookup() {
        try {
            LifecycleStageApi.register(LifecycleStageDefinition.builder(STAGE_A).build());
            assertTrue(LifecycleStageApi.findDefinition(STAGE_A).isPresent());
            assertFalse(LifecycleStageApi.findDefinition(STALE_STAGE).isPresent());
        } finally {
            unregisterFixtures();
        }
    }

    @Test
    void duplicateRegistrationThrows() {
        try {
            LifecycleStageApi.register(LifecycleStageDefinition.builder(STAGE_A).build());
            assertThrows(IllegalStateException.class, () ->
                    LifecycleStageApi.register(LifecycleStageDefinition.builder(STAGE_A).build()));
        } finally {
            unregisterFixtures();
        }
    }

    @Test
    void unregisterRemovesDefinition() {
        LifecycleStageApi.register(LifecycleStageDefinition.builder(STAGE_A).build());
        assertTrue(LifecycleStageApi.findDefinition(STAGE_A).isPresent());
        LifecycleStageApi.unregister(STAGE_A);
        assertFalse(LifecycleStageApi.findDefinition(STAGE_A).isPresent());
    }

    @Test
    void builderDefaultsAreCorrect() {
        LifecycleStageDefinition def = LifecycleStageDefinition.builder(STAGE_A).build();
        assertTrue(def.durationTicks().isEmpty());
        assertTrue(def.autoTransitions().isEmpty());
        assertTrue(def.manualTransitionTargets().isEmpty());
        assertTrue(def.projectedStateFlags().isEmpty());
        assertTrue(def.projectedCapabilityPolicies().isEmpty());
        assertTrue(def.projectedVisualForm().isEmpty());
    }

    @Test
    void builderWithTransitionsAndProjections() {
        LifecycleStageDefinition def = LifecycleStageDefinition.builder(STAGE_A)
                .durationTicks(1200)
                .autoTransition(LifecycleStageTransition.timer(STAGE_B))
                .allowTransitionTo(STAGE_C)
                .projectStateFlag(id("flag/burrowing"))
                .projectCapabilityPolicy(id("policy/no_inventory"))
                .build();

        assertEquals(1200, def.durationTicks().get());
        assertEquals(1, def.autoTransitions().size());
        assertEquals(LifecycleStageTrigger.TIMER, def.autoTransitions().get(0).trigger());
        assertEquals(STAGE_B, def.autoTransitions().get(0).targetStageId());
        assertTrue(def.manualTransitionTargets().contains(STAGE_C));
        assertTrue(def.projectedStateFlags().contains(id("flag/burrowing")));
        assertTrue(def.projectedCapabilityPolicies().contains(id("policy/no_inventory")));
    }

    @Test
    void lifecycleStageDataEmptyHasNoStage() {
        LifecycleStageData data = LifecycleStageData.empty();
        assertFalse(data.hasStage());
        assertTrue(data.activeStage().isEmpty());
    }

    @Test
    void lifecycleStageDataWithStageMutationsAreImmutable() {
        LifecycleStageState state = new LifecycleStageState(
                STAGE_A, id("source/test"), 0L, 0,
                java.util.Optional.empty(), LifecycleStageStatus.ACTIVE
        );
        LifecycleStageData empty = LifecycleStageData.empty();
        LifecycleStageData withStage = empty.withStage(state);

        assertTrue(withStage.hasStage());
        assertFalse(empty.hasStage());

        LifecycleStageData cleared = withStage.withoutStage();
        assertFalse(cleared.hasStage());
        assertTrue(withStage.hasStage());
    }

    @Test
    void sanitizeDropsUnknownStage() {
        LifecycleStageState state = new LifecycleStageState(
                STALE_STAGE, id("source/test"), 0L, 0,
                java.util.Optional.empty(), LifecycleStageStatus.ACTIVE
        );
        LifecycleStageData data = LifecycleStageData.empty().withStage(state);
        LifecycleStageData sanitized = LifecycleStageApi.sanitize(data);
        assertFalse(sanitized.hasStage());
    }

    @Test
    void sanitizePreservesKnownStage() {
        try {
            LifecycleStageApi.register(LifecycleStageDefinition.builder(STAGE_A).build());
            LifecycleStageState state = new LifecycleStageState(
                    STAGE_A, id("source/test"), 0L, 0,
                    java.util.Optional.empty(), LifecycleStageStatus.ACTIVE
            );
            LifecycleStageData data = LifecycleStageData.empty().withStage(state);
            LifecycleStageData sanitized = LifecycleStageApi.sanitize(data);
            assertTrue(sanitized.hasStage());
        } finally {
            unregisterFixtures();
        }
    }

    @Test
    void allDefinitionsReturnsRegisteredOnes() {
        try {
            LifecycleStageApi.register(LifecycleStageDefinition.builder(STAGE_A).build());
            LifecycleStageApi.register(LifecycleStageDefinition.builder(STAGE_B).build());
            assertEquals(2, LifecycleStageApi.all().size());
        } finally {
            unregisterFixtures();
        }
    }

    private void unregisterFixtures() {
        LifecycleStageApi.unregister(STAGE_A);
        LifecycleStageApi.unregister(STAGE_B);
        LifecycleStageApi.unregister(STAGE_C);
        LifecycleStageApi.unregister(STALE_STAGE);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("test", path);
    }
}
