package com.whatxe.xlib.ability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class DataDrivenArtifactApiTest {
    @Test
    void artifactDefinitionsParsePresenceRequirementsAndBundles() {
        DataDrivenArtifactApi.LoadedArtifactDefinition definition = DataDrivenArtifactApi.parseDefinition(
                id("artifacts/dragon_crest"),
                JsonParser.parseString("""
                        {
                          "id": "xlib_test:artifact/dragon_crest",
                          "items": ["minecraft:stick"],
                          "presence_modes": ["main_hand", "off_hand"],
                          "equipped_bundles": ["xlib_test:bundle/equipped"],
                          "unlocked_bundles": ["xlib_test:bundle/unlocked"],
                          "unlock_on_consume": true,
                          "when": {
                            "type": "score_at_least",
                            "objective": "focus",
                            "value": 3
                          }
                        }
                        """)
        );

        assertEquals(id("artifact/dragon_crest"), definition.id());
        assertTrue(definition.definition().itemIds().contains(ResourceLocation.withDefaultNamespace("stick")));
        assertTrue(definition.definition().presenceModes().contains(ArtifactPresenceMode.MAIN_HAND));
        assertTrue(definition.definition().presenceModes().contains(ArtifactPresenceMode.OFF_HAND));
        assertTrue(definition.definition().equippedBundles().contains(id("bundle/equipped")));
        assertTrue(definition.definition().unlockedBundles().contains(id("bundle/unlocked")));
        assertTrue(definition.definition().unlockOnConsume());
        assertEquals(1, definition.definition().requirements().size());
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }
}
