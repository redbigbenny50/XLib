package com.whatxe.xlib.ability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class DataDrivenGrantBundleApiTest {
    @Test
    void grantBundleDefinitionsParseAuthoredBundleContents() {
        DataDrivenGrantBundleApi.LoadedGrantBundleDefinition definition = DataDrivenGrantBundleApi.parseDefinition(
                id("bundles/dragon_set"),
                JsonParser.parseString("""
                        {
                          "id": "xlib_test:bundle/dragon_set",
                          "grant_abilities": ["xlib_test:ability/breath"],
                          "grant_passives": ["xlib_test:passive/scales"],
                          "grant_granted_items": ["xlib_test:item/relic"],
                          "grant_recipe_permissions": ["xlib_test:recipe/forge"],
                          "block_abilities": ["xlib_test:ability/freeze"],
                          "grant_state_policies": ["xlib_test:state/warded"],
                          "grant_state_flags": ["xlib_test:state/dragon_set"]
                        }
                        """)
        );

        assertEquals(id("bundle/dragon_set"), definition.id());
        assertTrue(definition.definition().abilities().contains(id("ability/breath")));
        assertTrue(definition.definition().passives().contains(id("passive/scales")));
        assertTrue(definition.definition().grantedItems().contains(id("item/relic")));
        assertTrue(definition.definition().recipePermissions().contains(id("recipe/forge")));
        assertTrue(definition.definition().blockedAbilities().contains(id("ability/freeze")));
        assertTrue(definition.definition().statePolicies().contains(id("state/warded")));
        assertTrue(definition.definition().stateFlags().contains(id("state/dragon_set")));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }
}
