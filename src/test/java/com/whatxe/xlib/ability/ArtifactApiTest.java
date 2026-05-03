package com.whatxe.xlib.ability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class ArtifactApiTest {
    private static final ResourceLocation ARTIFACT_ID = id("artifact/relic");
    private static final ResourceLocation STALE_ARTIFACT_ID = id("artifact/stale");
    private static final ResourceLocation SOURCE_ID = id("source/relic");
    private static final ResourceLocation STICK_ID = ResourceLocation.withDefaultNamespace("stick");
    private static final ResourceLocation BREAD_ID = ResourceLocation.withDefaultNamespace("bread");

    @Test
    void unlockedArtifactsRoundTripAndSanitizeRemoveStaleRegistrations() {
        unregisterFixtures();
        try {
            ArtifactApi.registerArtifact(ArtifactDefinition.builder(ARTIFACT_ID)
                    .itemId(STICK_ID)
                    .unlockOnConsume()
                    .build());

            AbilityData data = AbilityData.empty()
                    .withArtifactUnlockSource(ARTIFACT_ID, SOURCE_ID, true)
                    .withArtifactUnlockSource(STALE_ARTIFACT_ID, SOURCE_ID, true);

            AbilityData sanitized = AbilityApi.sanitizeData(data);

            assertTrue(ArtifactApi.isUnlocked(sanitized, ARTIFACT_ID));
            assertFalse(ArtifactApi.isUnlocked(sanitized, STALE_ARTIFACT_ID));
            assertEquals(Set.of(ARTIFACT_ID), ArtifactApi.unlockedArtifacts(sanitized));
        } finally {
            unregisterFixtures();
        }
    }

    @Test
    void matchingArtifactsUseRegisteredItemIds() {
        unregisterFixtures();
        try {
            ArtifactApi.registerArtifact(ArtifactDefinition.builder(ARTIFACT_ID)
                    .itemId(STICK_ID)
                    .presence(ArtifactPresenceMode.MAIN_HAND)
                    .build());

            assertEquals(Set.of(ARTIFACT_ID), ArtifactApi.matchingArtifacts(STICK_ID).stream()
                    .map(ArtifactDefinition::id)
                    .collect(java.util.stream.Collectors.toSet()));
            assertTrue(ArtifactApi.matchingArtifacts(BREAD_ID).isEmpty());
        } finally {
            unregisterFixtures();
        }
    }

    private static void unregisterFixtures() {
        ArtifactApi.unregisterArtifact(ARTIFACT_ID);
        ArtifactApi.unregisterArtifact(STALE_ARTIFACT_ID);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }
}
