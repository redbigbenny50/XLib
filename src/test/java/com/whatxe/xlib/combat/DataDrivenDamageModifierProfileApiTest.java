package com.whatxe.xlib.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonParser;
import com.whatxe.xlib.combat.DamageModifierProfileMergeMode;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class DataDrivenDamageModifierProfileApiTest {

    @Test
    void parsesFlatDamageProfileFields() {
        DataDrivenDamageModifierProfileApi.LoadedDamageModifierProfileDefinition loaded =
                DataDrivenDamageModifierProfileApi.parseDefinition(
                        id("damage_modifier_profiles/fireproof"),
                        JsonParser.parseString("""
                                {
                                  "incoming_damage_types": {
                                    "minecraft:in_fire": 0.5
                                  },
                                  "outgoing_damage_type_tags": {
                                    "xlib_test:damage/projectile": 1.25
                                  }
                                }
                                """)
                );

        assertEquals(id("damage_modifier_profiles/fireproof"), loaded.id());
        assertEquals(0.5D, loaded.definition().incomingDamageTypes().get(ResourceLocation.withDefaultNamespace("in_fire")));
        assertEquals(1.25D, loaded.definition().outgoingDamageTypeTags().get(id("damage/projectile")));
    }

    @Test
    void parsesNestedIncomingAndOutgoingSections() {
        DataDrivenDamageModifierProfileApi.LoadedDamageModifierProfileDefinition loaded =
                DataDrivenDamageModifierProfileApi.parseDefinition(
                        id("damage_modifier_profiles/nested"),
                        JsonParser.parseString("""
                                {
                                  "id": "xlib_test:profiles/nested",
                                  "incoming": {
                                    "damage_type_tags": {
                                      "xlib_test:damage/fire_like": 0.25
                                    }
                                  },
                                  "outgoing": {
                                    "damage_types": {
                                      "minecraft:fall": 0.0
                                    }
                                  }
                                }
                                """)
                );

        assertEquals(id("profiles/nested"), loaded.id());
        assertEquals(0.25D, loaded.definition().incomingDamageTypeTags().get(id("damage/fire_like")));
        assertEquals(0.0D, loaded.definition().outgoingDamageTypes().get(ResourceLocation.withDefaultNamespace("fall")));
    }

    @Test
    void parsesMergeModeAndPriority() {
        DataDrivenDamageModifierProfileApi.LoadedDamageModifierProfileDefinition loaded =
                DataDrivenDamageModifierProfileApi.parseDefinition(
                        id("damage_modifier_profiles/override_test"),
                        JsonParser.parseString("""
                                {
                                  "merge_mode": "override",
                                  "priority": 10,
                                  "incoming_damage_types": {
                                    "minecraft:in_fire": 0.5
                                  }
                                }
                                """)
                );

        assertEquals(DamageModifierProfileMergeMode.OVERRIDE, loaded.definition().mergeMode());
        assertEquals(10, loaded.definition().priority());
        assertEquals(0.5D, loaded.definition().incomingDamageTypes().get(ResourceLocation.withDefaultNamespace("in_fire")));
    }

    @Test
    void parsesFlatIncomingSection() {
        DataDrivenDamageModifierProfileApi.LoadedDamageModifierProfileDefinition loaded =
                DataDrivenDamageModifierProfileApi.parseDefinition(
                        id("damage_modifier_profiles/flat_test"),
                        JsonParser.parseString("""
                                {
                                  "incoming_flat": {
                                    "damage_types": {
                                      "minecraft:fall": -5.0
                                    },
                                    "damage_type_tags": {
                                      "xlib_test:damage/fire_like": 2.5
                                    }
                                  }
                                }
                                """)
                );

        assertEquals(-5.0D, loaded.definition().incomingFlatAdditions().get(ResourceLocation.withDefaultNamespace("fall")));
        assertEquals(2.5D, loaded.definition().incomingFlatAdditionTags().get(id("damage/fire_like")));
    }

    @Test
    void parsesFlatOutgoingSection() {
        DataDrivenDamageModifierProfileApi.LoadedDamageModifierProfileDefinition loaded =
                DataDrivenDamageModifierProfileApi.parseDefinition(
                        id("damage_modifier_profiles/flat_outgoing_test"),
                        JsonParser.parseString("""
                                {
                                  "outgoing_flat": {
                                    "damage_types": {
                                      "minecraft:arrow": 3.0
                                    },
                                    "damage_type_tags": {
                                      "xlib_test:damage/magic": -1.0
                                    }
                                  }
                                }
                                """)
                );

        assertEquals(3.0D, loaded.definition().outgoingFlatAdditions().get(ResourceLocation.withDefaultNamespace("arrow")));
        assertEquals(-1.0D, loaded.definition().outgoingFlatAdditionTags().get(id("damage/magic")));
    }

    @Test
    void parsesMergeModeAdditive() {
        DataDrivenDamageModifierProfileApi.LoadedDamageModifierProfileDefinition loaded =
                DataDrivenDamageModifierProfileApi.parseDefinition(
                        id("damage_modifier_profiles/additive_test"),
                        JsonParser.parseString("""
                                {
                                  "merge_mode": "additive"
                                }
                                """)
                );

        assertEquals(DamageModifierProfileMergeMode.ADDITIVE, loaded.definition().mergeMode());
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }
}
