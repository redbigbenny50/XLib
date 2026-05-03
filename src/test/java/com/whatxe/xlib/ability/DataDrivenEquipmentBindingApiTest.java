package com.whatxe.xlib.ability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class DataDrivenEquipmentBindingApiTest {
    @Test
    void equipmentBindingDefinitionsParseBundlesArtifactsAndSnapshotGrants() {
        DataDrivenEquipmentBindingApi.LoadedEquipmentBindingDefinition definition = DataDrivenEquipmentBindingApi.parseDefinition(
                id("bindings/dragon_set"),
                JsonParser.parseString("""
                        {
                          "source": "xlib_test:binding/dragon_set",
                          "items": [
                            "minecraft:diamond_helmet",
                            "minecraft:diamond_chestplate"
                          ],
                          "presence_modes": ["armor"],
                          "match": "all",
                          "grant_bundles": ["xlib_test:bundle/dragon_set"],
                          "unlock_artifacts": ["xlib_test:artifact/dragon_crest"],
                          "grant_state_flags": ["xlib_test:state/dragon_set_active"]
                        }
                        """)
        );

        assertEquals(id("binding/dragon_set"), definition.sourceId());
        assertTrue(definition.grantBundles().contains(id("bundle/dragon_set")));
        assertTrue(definition.unlockArtifacts().contains(id("artifact/dragon_crest")));
        assertTrue(definition.snapshot().stateFlags().contains(id("state/dragon_set_active")));
    }

    @Test
    void equipmentBindingDefinitionsSupportItemTagsAndSlotBindings() {
        DataDrivenEquipmentBindingApi.LoadedEquipmentBindingDefinition definition = DataDrivenEquipmentBindingApi.parseDefinition(
                id("bindings/tagged_set"),
                JsonParser.parseString("""
                        {
                          "item_tags": [
                            "minecraft:logs",
                            "minecraft:planks"
                          ],
                          "presence_modes": ["inventory", "hotbar"],
                          "match": "any",
                          "slot_items": {
                            "head": "minecraft:diamond_helmet"
                          },
                          "slot_tags": {
                            "off_hand": "minecraft:planks"
                          },
                          "grant_bundles": ["xlib_test:bundle/tagged_set"]
                        }
                        """)
        );

        assertTrue(definition.itemTagIds().contains(ResourceLocation.withDefaultNamespace("logs")));
        assertTrue(definition.itemTagIds().contains(ResourceLocation.withDefaultNamespace("planks")));
        assertEquals(ResourceLocation.withDefaultNamespace("diamond_helmet"), definition.slotItems().get(DataDrivenEquipmentBindingApi.EquipmentSlotBinding.HEAD));
        assertEquals(ResourceLocation.withDefaultNamespace("planks"), definition.slotTags().get(DataDrivenEquipmentBindingApi.EquipmentSlotBinding.OFF_HAND));
    }

    @Test
    void equipmentBindingArtifactSourceIdsRoundTrip() {
        ResourceLocation sourceId = id("binding/dragon_set");
        ResourceLocation artifactSourceId = DataDrivenEquipmentBindingApi.artifactSourceIdFor(sourceId);

        assertEquals(sourceId, DataDrivenEquipmentBindingApi.parseArtifactSourceId(artifactSourceId).orElseThrow());
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }
}
