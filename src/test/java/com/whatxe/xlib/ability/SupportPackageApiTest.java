package com.whatxe.xlib.ability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class SupportPackageApiTest {
    private static final ResourceLocation BUNDLE_ID = id("bundle/support");
    private static final ResourceLocation RELATIONSHIP_ID = id("relationship/ally");
    private static final ResourceLocation SUPPORT_PACKAGE_ID = id("support/guardian");
    private static final UUID SUPPORTER_ID = UUID.fromString("00000000-0000-0000-0000-000000000123");

    @Test
    void sourceIdsRoundTripAndDefinitionsRetainRelationships() {
        SupportPackageApi.unregisterSupportPackage(SUPPORT_PACKAGE_ID);
        try {
            SupportPackageDefinition supportPackage = SupportPackageDefinition.builder(SUPPORT_PACKAGE_ID)
                    .grantBundle(BUNDLE_ID)
                    .relationship(RELATIONSHIP_ID)
                    .allowSelf()
                    .build();
            SupportPackageApi.registerSupportPackage(supportPackage);

            ResourceLocation sourceId = SupportPackageApi.sourceIdFor(SUPPORTER_ID, SUPPORT_PACKAGE_ID);

            assertEquals(Optional.of(new SupportPackageApi.SupportSource(SUPPORTER_ID, SUPPORT_PACKAGE_ID)), SupportPackageApi.parseSource(sourceId));
            assertEquals(Set.of(BUNDLE_ID), SupportPackageApi.findSupportPackage(SUPPORT_PACKAGE_ID).orElseThrow().grantBundles());
            assertEquals(Set.of(RELATIONSHIP_ID), SupportPackageApi.findSupportPackage(SUPPORT_PACKAGE_ID).orElseThrow().requiredRelationships());
            assertTrue(SupportPackageApi.findSupportPackage(SUPPORT_PACKAGE_ID).orElseThrow().allowSelf());
        } finally {
            SupportPackageApi.unregisterSupportPackage(SUPPORT_PACKAGE_ID);
        }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }
}
