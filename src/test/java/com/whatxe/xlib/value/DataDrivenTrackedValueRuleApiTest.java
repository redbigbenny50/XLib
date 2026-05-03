package com.whatxe.xlib.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class DataDrivenTrackedValueRuleApiTest {
    private static final ResourceLocation EVOLUTION_ID = id("value/evolution");
    private static final ResourceLocation CORRUPTION_ID = id("value/corruption");
    private static final ResourceLocation RULE_ID = id("rule/gain_on_tick");
    private static final ResourceLocation FOOD_SOURCE_ID = id("state/evolution_form");

    @Test
    void parsesAndAppliesTrackedValueRule() {
        TrackedValueApi.unregister(EVOLUTION_ID);
        TrackedValueApi.unregister(CORRUPTION_ID);
        try {
            TrackedValueApi.register(TrackedValueDefinition.builder(EVOLUTION_ID)
                    .displayName(Component.literal("Evolution"))
                    .minValue(0.0D)
                    .maxValue(100.0D)
                    .startingValue(5.0D)
                    .foodReplacementPriority(25)
                    .build());
            TrackedValueApi.register(TrackedValueDefinition.builder(CORRUPTION_ID)
                    .displayName(Component.literal("Corruption"))
                    .minValue(0.0D)
                    .maxValue(100.0D)
                    .startingValue(2.0D)
                    .build());

            DataDrivenTrackedValueRuleApi.LoadedTrackedValueRuleDefinition definition = DataDrivenTrackedValueRuleApi.parseDefinition(
                    RULE_ID,
                    JsonParser.parseString("""
                            {
                              "trigger": "item_used",
                              "priority": 40,
                              "food_replacement_source": "xlib_test:state/evolution_form",
                              "conditions": [ true ],
                              "advancements": [ "minecraft:story/mine_stone" ],
                              "armor_slots": [ "head", "chest" ],
                              "clear_values": [ "xlib_test:value/corruption" ],
                              "value_deltas": {
                                "xlib_test:value/evolution": 3.5
                              },
                              "set_values": {
                                "xlib_test:value/evolution": 10
                              },
                              "multiply_values": {
                                "xlib_test:value/evolution": 2.0
                              },
                              "min_values": {
                                "xlib_test:value/evolution": 30.0
                              },
                              "max_values": {
                                "xlib_test:value/evolution": 25.0
                              },
                              "damage_types": [ "minecraft:in_fire" ],
                              "damage_type_tags": [ "minecraft:is_fire" ],
                              "classification_source": "xlib_test:state/evolution_form",
                              "clear_classification_source": true,
                              "grant_synthetic_entity_types": [ "minecraft:zombie" ],
                              "revoke_synthetic_entity_types": [ "minecraft:husk" ],
                              "grant_synthetic_tags": [ "xlib_test:xenomorph_like" ],
                              "revoke_synthetic_tags": [ "xlib_test:marine_like" ],
                              "enable_food_replacements": [
                                "xlib_test:value/evolution"
                              ],
                              "disable_food_replacements": [
                                "xlib_test:value/corruption"
                              ],
                              "grant_state_policies": [
                                "xlib_test:state/policy_a"
                              ],
                              "revoke_state_policies": [
                                "xlib_test:state/policy_b"
                              ],
                              "grant_state_flags": [
                                "xlib_test:state/flag_a"
                              ],
                              "revoke_state_flags": [
                                "xlib_test:state/flag_b"
                              ],
                              "grant_capability_policies": [
                                "xlib_test:capability/policy_a"
                              ],
                              "revoke_capability_policies": [
                                "xlib_test:capability/policy_b"
                              ],
                              "grant_damage_profiles": [
                                "xlib_test:damage/profile_a"
                              ],
                              "revoke_damage_profiles": [
                                "xlib_test:damage/profile_b"
                              ]
                            }
                            """)
            );

            TrackedValueData updatedData = definition.definition().applyTo(
                    TrackedValueData.empty()
                            .withExactAmount(CORRUPTION_ID, 17.0D)
                            .withFoodReplacementSource(CORRUPTION_ID, FOOD_SOURCE_ID, true)
            );

            assertEquals(25.0D, TrackedValueApi.value(updatedData, EVOLUTION_ID), 0.0001D);
            assertEquals(2.0D, TrackedValueApi.value(updatedData, CORRUPTION_ID), 0.0001D);
            assertEquals(40, definition.definition().priority());
            assertEquals(FOOD_SOURCE_ID, definition.definition().foodReplacementSourceId());
            assertEquals(FOOD_SOURCE_ID, definition.definition().classificationSourceId());
            assertTrue(definition.definition().clearClassificationSource());
            assertTrue(definition.definition().advancementIds().contains(ResourceLocation.parse("minecraft:story/mine_stone")));
            assertEquals(2, definition.definition().armorSlots().size());
            assertTrue(definition.definition().damageTypeIds().contains(ResourceLocation.parse("minecraft:in_fire")));
            assertEquals(1, definition.definition().damageTypeTags().size());
            assertTrue(definition.definition().grantStatePolicies().contains(id("state/policy_a")));
            assertTrue(definition.definition().revokeStatePolicies().contains(id("state/policy_b")));
            assertTrue(definition.definition().grantStateFlags().contains(id("state/flag_a")));
            assertTrue(definition.definition().revokeStateFlags().contains(id("state/flag_b")));
            assertTrue(definition.definition().grantCapabilityPolicies().contains(id("capability/policy_a")));
            assertTrue(definition.definition().revokeCapabilityPolicies().contains(id("capability/policy_b")));
            assertTrue(definition.definition().grantDamageModifierProfiles().contains(id("damage/profile_a")));
            assertTrue(definition.definition().revokeDamageModifierProfiles().contains(id("damage/profile_b")));
            assertTrue(definition.definition().grantSyntheticEntityTypes().contains(ResourceLocation.parse("minecraft:zombie")));
            assertTrue(definition.definition().revokeSyntheticEntityTypes().contains(ResourceLocation.parse("minecraft:husk")));
            assertTrue(definition.definition().grantSyntheticTags().contains(id("xenomorph_like")));
            assertTrue(definition.definition().revokeSyntheticTags().contains(id("marine_like")));
            assertTrue(updatedData.hasFoodReplacement(EVOLUTION_ID));
            assertFalse(updatedData.hasFoodReplacement(CORRUPTION_ID));
        } finally {
            TrackedValueApi.unregister(EVOLUTION_ID);
            TrackedValueApi.unregister(CORRUPTION_ID);
        }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }
}
