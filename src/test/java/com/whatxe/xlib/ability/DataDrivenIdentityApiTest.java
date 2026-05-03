package com.whatxe.xlib.ability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class DataDrivenIdentityApiTest {
    @Test
    void identityDefinitionsParseInheritanceAndGrantBundles() {
        DataDrivenIdentityApi.LoadedIdentityDefinition definition = DataDrivenIdentityApi.parseDefinition(
                id("identities/dragon"),
                JsonParser.parseString("""
                        {
                          "id": "xlib_test:identity/dragon",
                          "inherits": ["xlib_test:identity/base"],
                          "grant_bundles": ["xlib_test:bundle/dragon"]
                        }
                        """)
        );

        assertEquals(id("identity/dragon"), definition.id());
        assertTrue(definition.definition().inheritedIdentities().contains(id("identity/base")));
        assertTrue(definition.definition().grantBundles().contains(id("bundle/dragon")));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }
}
