package com.whatxe.xlib.ability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class GrantBundleApiTest {
    private static final ResourceLocation ACTIVE_BUNDLE_ID = id("bundle/active");
    private static final ResourceLocation STALE_BUNDLE_ID = id("bundle/stale");
    private static final ResourceLocation SOURCE_ID = id("source/context");

    @Test
    void activeBundlesResolveFromRegisteredIdsAndSanitizeRemovesStaleOnes() {
        unregisterFixtures();
        try {
            GrantBundleApi.registerBundle(GrantBundleDefinition.builder(ACTIVE_BUNDLE_ID).build());

            AbilityData data = AbilityData.empty()
                    .withGrantBundleSource(ACTIVE_BUNDLE_ID, SOURCE_ID, true)
                    .withGrantBundleSource(STALE_BUNDLE_ID, SOURCE_ID, true);

            assertEquals(Set.of(ACTIVE_BUNDLE_ID), GrantBundleApi.activeBundles(data));
            assertEquals(Set.of(ACTIVE_BUNDLE_ID), GrantBundleApi.bundlesForSource(data, SOURCE_ID));

            AbilityData sanitized = AbilityApi.sanitizeData(data);

            assertTrue(sanitized.hasGrantBundle(ACTIVE_BUNDLE_ID));
            assertFalse(sanitized.hasGrantBundle(STALE_BUNDLE_ID));
        } finally {
            unregisterFixtures();
        }
    }

    @Test
    void clearGrantSourceRemovesTrackedBundles() {
        unregisterFixtures();
        try {
            GrantBundleApi.registerBundle(GrantBundleDefinition.builder(ACTIVE_BUNDLE_ID).build());

            AbilityData data = AbilityData.empty()
                    .withGrantBundleSource(ACTIVE_BUNDLE_ID, SOURCE_ID, true)
                    .clearGrantSource(SOURCE_ID);

            assertFalse(data.hasGrantBundle(ACTIVE_BUNDLE_ID));
            assertTrue(data.grantBundleSourcesFor(ACTIVE_BUNDLE_ID).isEmpty());
        } finally {
            unregisterFixtures();
        }
    }

    private static void unregisterFixtures() {
        GrantBundleApi.unregisterBundle(ACTIVE_BUNDLE_ID);
        GrantBundleApi.unregisterBundle(STALE_BUNDLE_ID);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }
}
