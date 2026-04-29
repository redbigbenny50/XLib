package com.whatxe.xlib.ability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class DataDrivenSupportPackageApiTest {
    @Test
    void supportPackageDefinitionsParseRelationshipsAndSelfAllowance() {
        DataDrivenSupportPackageApi.LoadedSupportPackageDefinition definition = DataDrivenSupportPackageApi.parseDefinition(
                id("support_packages/guardian"),
                JsonParser.parseString("""
                        {
                          "id": "xlib_test:support/guardian",
                          "grant_bundles": ["xlib_test:bundle/guardian"],
                          "relationships": ["xlib_test:relationship/ally"],
                          "allow_self": true
                        }
                        """)
        );

        assertEquals(id("support/guardian"), definition.id());
        assertTrue(definition.definition().grantBundles().contains(id("bundle/guardian")));
        assertTrue(definition.definition().requiredRelationships().contains(id("relationship/ally")));
        assertTrue(definition.definition().allowSelf());
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }
}
