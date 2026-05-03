package com.whatxe.xlib.ability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class IdentityApiTest {
    private static final ResourceLocation BASE_BUNDLE_ID = id("bundle/base");
    private static final ResourceLocation SPECIAL_BUNDLE_ID = id("bundle/special");
    private static final ResourceLocation BASE_IDENTITY_ID = id("identity/base");
    private static final ResourceLocation SPECIAL_IDENTITY_ID = id("identity/special");
    private static final ResourceLocation STALE_IDENTITY_ID = id("identity/stale");
    private static final ResourceLocation SOURCE_ID = id("source/origin");

    @Test
    void inheritedBundlesResolveForActiveIdentitySources() {
        unregisterFixtures();
        try {
            GrantBundleApi.registerBundle(GrantBundleDefinition.builder(BASE_BUNDLE_ID).build());
            GrantBundleApi.registerBundle(GrantBundleDefinition.builder(SPECIAL_BUNDLE_ID).build());
            IdentityApi.registerIdentity(IdentityDefinition.builder(BASE_IDENTITY_ID)
                    .grantBundle(BASE_BUNDLE_ID)
                    .build());
            IdentityApi.registerIdentity(IdentityDefinition.builder(SPECIAL_IDENTITY_ID)
                    .inherits(BASE_IDENTITY_ID)
                    .grantBundle(SPECIAL_BUNDLE_ID)
                    .build());

            AbilityData data = AbilityData.empty()
                    .withStateFlagSource(SPECIAL_IDENTITY_ID, SOURCE_ID, true)
                    .withStateFlagSource(STALE_IDENTITY_ID, SOURCE_ID, true);

            assertTrue(IdentityApi.hasIdentity(data, SPECIAL_IDENTITY_ID));
            assertFalse(IdentityApi.hasIdentity(data, STALE_IDENTITY_ID));
            assertEquals(Set.of(SPECIAL_IDENTITY_ID), IdentityApi.activeIdentities(data));
            assertEquals(Set.of(BASE_BUNDLE_ID, SPECIAL_BUNDLE_ID), IdentityApi.resolvedGrantBundles(SPECIAL_IDENTITY_ID));
            assertEquals(Set.of(BASE_BUNDLE_ID, SPECIAL_BUNDLE_ID), IdentityApi.grantBundlesForSource(data, SOURCE_ID));

            AbilityData sanitized = AbilityApi.sanitizeData(data);

            assertTrue(sanitized.hasStateFlag(SPECIAL_IDENTITY_ID));
            assertFalse(sanitized.hasStateFlag(STALE_IDENTITY_ID));
        } finally {
            unregisterFixtures();
        }
    }

    @Test
    void projectionSourceIdsRoundTrip() {
        ResourceLocation projectionSourceId = IdentityApi.projectionSourceIdFor(SOURCE_ID);

        assertEquals(Optional.of(SOURCE_ID), IdentityApi.parseProjectionSourceId(projectionSourceId));
    }

    private static void unregisterFixtures() {
        IdentityApi.unregisterIdentity(BASE_IDENTITY_ID);
        IdentityApi.unregisterIdentity(SPECIAL_IDENTITY_ID);
        IdentityApi.unregisterIdentity(STALE_IDENTITY_ID);
        GrantBundleApi.unregisterBundle(BASE_BUNDLE_ID);
        GrantBundleApi.unregisterBundle(SPECIAL_BUNDLE_ID);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }
}
