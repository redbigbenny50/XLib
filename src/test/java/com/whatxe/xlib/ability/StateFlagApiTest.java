package com.whatxe.xlib.ability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class StateFlagApiTest {
    private static final ResourceLocation ACTIVE_FLAG_ID = id("flag/active");
    private static final ResourceLocation SECOND_FLAG_ID = id("flag/second");
    private static final ResourceLocation STALE_FLAG_ID = id("flag/stale");
    private static final ResourceLocation SOURCE_ID = id("source/context");

    @Test
    void activeFlagsResolveFromRegisteredIdsAndSanitizeRemovesStaleOnes() {
        unregisterFixtures();
        try {
            StateFlagApi.registerStateFlag(ACTIVE_FLAG_ID);
            StateFlagApi.registerStateFlag(SECOND_FLAG_ID);

            AbilityData data = AbilityData.empty()
                    .withStateFlagSource(ACTIVE_FLAG_ID, SOURCE_ID, true)
                    .withStateFlagSource(SECOND_FLAG_ID, SOURCE_ID, true)
                    .withStateFlagSource(STALE_FLAG_ID, SOURCE_ID, true);

            assertTrue(StateFlagApi.hasActiveFlag(data, ACTIVE_FLAG_ID));
            assertTrue(StateFlagApi.hasActiveFlag(data, SECOND_FLAG_ID));
            assertFalse(StateFlagApi.hasActiveFlag(data, STALE_FLAG_ID));
            assertEquals(Set.of(ACTIVE_FLAG_ID, SECOND_FLAG_ID), StateFlagApi.activeFlags(data));

            AbilityData sanitized = AbilityApi.sanitizeData(data);

            assertTrue(sanitized.hasStateFlag(ACTIVE_FLAG_ID));
            assertTrue(sanitized.hasStateFlag(SECOND_FLAG_ID));
            assertFalse(sanitized.hasStateFlag(STALE_FLAG_ID));
        } finally {
            unregisterFixtures();
        }
    }

    @Test
    void clearGrantSourceRemovesTrackedStateFlags() {
        unregisterFixtures();
        try {
            StateFlagApi.registerStateFlag(ACTIVE_FLAG_ID);

            AbilityData data = AbilityData.empty()
                    .withStateFlagSource(ACTIVE_FLAG_ID, SOURCE_ID, true)
                    .clearGrantSource(SOURCE_ID);

            assertFalse(data.hasStateFlag(ACTIVE_FLAG_ID));
            assertTrue(data.stateFlagSourcesFor(ACTIVE_FLAG_ID).isEmpty());
        } finally {
            unregisterFixtures();
        }
    }

    private static void unregisterFixtures() {
        StateFlagApi.unregisterStateFlag(ACTIVE_FLAG_ID);
        StateFlagApi.unregisterStateFlag(SECOND_FLAG_ID);
        StateFlagApi.unregisterStateFlag(STALE_FLAG_ID);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }
}
