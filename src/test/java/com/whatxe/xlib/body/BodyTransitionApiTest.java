package com.whatxe.xlib.body;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class BodyTransitionApiTest {
    private static final ResourceLocation TRANSITION_ID = id("transition/possess");
    private static final ResourceLocation TRANSITION_ID_2 = id("transition/hatch");
    private static final ResourceLocation STALE_TRANSITION_ID = id("transition/stale");
    private static final ResourceLocation SOURCE_A = id("source/lifecycle");

    @Test
    void registrationAndLookup() {
        try {
            BodyTransitionApi.register(BodyTransitionDefinition.builder(TRANSITION_ID, BodyTransitionKind.POSSESS).build());
            assertTrue(BodyTransitionApi.findDefinition(TRANSITION_ID).isPresent());
            assertFalse(BodyTransitionApi.findDefinition(STALE_TRANSITION_ID).isPresent());
        } finally {
            unregisterFixtures();
        }
    }

    @Test
    void duplicateRegistrationThrows() {
        try {
            BodyTransitionApi.register(BodyTransitionDefinition.builder(TRANSITION_ID, BodyTransitionKind.POSSESS).build());
            assertThrows(IllegalStateException.class, () ->
                    BodyTransitionApi.register(BodyTransitionDefinition.builder(TRANSITION_ID, BodyTransitionKind.REPLACE).build()));
        } finally {
            unregisterFixtures();
        }
    }

    @Test
    void unregisterRemovesDefinition() {
        BodyTransitionApi.register(BodyTransitionDefinition.builder(TRANSITION_ID, BodyTransitionKind.HATCH).build());
        assertTrue(BodyTransitionApi.findDefinition(TRANSITION_ID).isPresent());
        BodyTransitionApi.unregister(TRANSITION_ID);
        assertFalse(BodyTransitionApi.findDefinition(TRANSITION_ID).isPresent());
    }

    @Test
    void builderDefaultsAreCorrect() {
        BodyTransitionDefinition def = BodyTransitionDefinition.builder(TRANSITION_ID, BodyTransitionKind.EMERGE).build();
        assertEquals(BodyTransitionKind.EMERGE, def.kind());
        assertEquals(OriginBodyPolicy.PRESERVE, def.originBodyPolicy());
        assertEquals(BodyControlPolicy.FULL, def.controlPolicy());
        assertTrue(def.temporaryCapabilityPolicyId().isEmpty());
        assertTrue(def.temporaryVisualFormId().isEmpty());
        assertTrue(def.reversible());
    }

    @Test
    void builderWithOptions() {
        BodyTransitionDefinition def = BodyTransitionDefinition.builder(TRANSITION_ID, BodyTransitionKind.POSSESS)
                .originBodyPolicy(OriginBodyPolicy.SHELL)
                .controlPolicy(BodyControlPolicy.POSSESSED)
                .temporaryCapabilityPolicy(id("policy/possessed_restrictions"))
                .temporaryVisualForm(id("form/ethereal"))
                .reversible(false)
                .build();
        assertEquals(OriginBodyPolicy.SHELL, def.originBodyPolicy());
        assertEquals(BodyControlPolicy.POSSESSED, def.controlPolicy());
        assertTrue(def.temporaryCapabilityPolicyId().isPresent());
        assertTrue(def.temporaryVisualFormId().isPresent());
        assertFalse(def.reversible());
    }

    @Test
    void bodyTransitionDataEmptyHasNoTransition() {
        BodyTransitionData data = BodyTransitionData.empty();
        assertFalse(data.hasTransition());
        assertTrue(data.activeTransition().isEmpty());
    }

    @Test
    void bodyTransitionDataMutationsAreImmutable() {
        BodyTransitionState state = new BodyTransitionState(
                UUID.randomUUID(), UUID.randomUUID(), Optional.empty(),
                TRANSITION_ID, SOURCE_A, 0L, BodyTransitionStatus.ACTIVE
        );
        BodyTransitionData empty = BodyTransitionData.empty();
        BodyTransitionData withTransition = empty.withTransition(state);

        assertTrue(withTransition.hasTransition());
        assertFalse(empty.hasTransition());

        BodyTransitionData cleared = withTransition.withoutTransition();
        assertFalse(cleared.hasTransition());
        assertTrue(withTransition.hasTransition());
    }

    @Test
    void sanitizeDropsUnknownTransition() {
        BodyTransitionState state = new BodyTransitionState(
                UUID.randomUUID(), UUID.randomUUID(), Optional.empty(),
                STALE_TRANSITION_ID, SOURCE_A, 0L, BodyTransitionStatus.ACTIVE
        );
        BodyTransitionData data = BodyTransitionData.empty().withTransition(state);
        BodyTransitionData sanitized = BodyTransitionApi.sanitize(data);
        assertFalse(sanitized.hasTransition());
    }

    @Test
    void sanitizePreservesKnownTransition() {
        try {
            BodyTransitionApi.register(BodyTransitionDefinition.builder(TRANSITION_ID, BodyTransitionKind.PROJECT).build());
            BodyTransitionState state = new BodyTransitionState(
                    UUID.randomUUID(), UUID.randomUUID(), Optional.empty(),
                    TRANSITION_ID, SOURCE_A, 0L, BodyTransitionStatus.ACTIVE
            );
            BodyTransitionData data = BodyTransitionData.empty().withTransition(state);
            BodyTransitionData sanitized = BodyTransitionApi.sanitize(data);
            assertTrue(sanitized.hasTransition());
        } finally {
            unregisterFixtures();
        }
    }

    @Test
    void stateWithStatusIsImmutable() {
        BodyTransitionState state = new BodyTransitionState(
                UUID.randomUUID(), UUID.randomUUID(), Optional.empty(),
                TRANSITION_ID, SOURCE_A, 0L, BodyTransitionStatus.ACTIVE
        );
        BodyTransitionState returning = state.withStatus(BodyTransitionStatus.RETURNING);
        assertEquals(BodyTransitionStatus.ACTIVE, state.status());
        assertEquals(BodyTransitionStatus.RETURNING, returning.status());
    }

    @Test
    void allDefinitionsReturnsRegisteredOnes() {
        try {
            BodyTransitionApi.register(BodyTransitionDefinition.builder(TRANSITION_ID, BodyTransitionKind.POSSESS).build());
            BodyTransitionApi.register(BodyTransitionDefinition.builder(TRANSITION_ID_2, BodyTransitionKind.HATCH).build());
            assertEquals(2, BodyTransitionApi.all().size());
        } finally {
            unregisterFixtures();
        }
    }

    private void unregisterFixtures() {
        BodyTransitionApi.unregister(TRANSITION_ID);
        BodyTransitionApi.unregister(TRANSITION_ID_2);
        BodyTransitionApi.unregister(STALE_TRANSITION_ID);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("test", path);
    }
}
