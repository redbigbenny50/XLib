package com.whatxe.xlib.ability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class AbilityRequirementJsonParserTest {
    private static final ResourceLocation RESOURCE_ID = id("focus");
    private static final ResourceLocation FLAG_ID = id("empowered");
    private static final ResourceLocation SOURCE_ID = id("source");
    private static final ResourceLocation ABILITY_ID = id("dash");
    private static final ResourceLocation COUNTER_ID = id("counter/hunts");
    private static final ResourceLocation MARK_ID = id("mark/exposed");

    @Test
    void compositeRequirementsReuseExistingRequirementSemantics() {
        AbilityApi.unregisterResource(RESOURCE_ID);
        StateFlagApi.unregisterStateFlag(FLAG_ID);

        AbilityApi.registerResource(AbilityResourceDefinition.builder(RESOURCE_ID).build());
        StateFlagApi.registerStateFlag(FLAG_ID);

        try {
            AbilityRequirement requirement = AbilityRequirementJsonParser.parse(JsonParser.parseString("""
                    {
                      "type": "all",
                      "conditions": [
                        {
                          "type": "resource_at_least",
                          "resource": "xlib_test:focus",
                          "amount": 10
                        },
                        {
                          "type": "not",
                          "condition": {
                            "type": "state_flag_active",
                            "state_flag": "xlib_test:empowered"
                          }
                        }
                      ]
                    }
                    """));

            AbilityData readyData = AbilityApi.createDefaultData().withResourceAmount(RESOURCE_ID, 20);
            AbilityData lowResourceData = AbilityApi.createDefaultData().withResourceAmount(RESOURCE_ID, 5);
            AbilityData flaggedData = readyData.withStateFlagSource(FLAG_ID, SOURCE_ID, true);

            assertTrue(requirement.validate(null, readyData).isEmpty());
            assertTrue(requirement.validate(null, lowResourceData).isPresent());
            assertTrue(requirement.validate(null, flaggedData).isPresent());
        } finally {
            AbilityApi.unregisterResource(RESOURCE_ID);
            StateFlagApi.unregisterStateFlag(FLAG_ID);
        }
    }

    @Test
    void contextGrantDefinitionsParseAuthoredSnapshots() {
        DataDrivenContextGrantApi.LoadedContextGrantDefinition definition = DataDrivenContextGrantApi.parseDefinition(
                id("context/test_overlay"),
                JsonParser.parseString("""
                        {
                          "source": "xlib_test:score_pack",
                          "when": {
                            "type": "score_at_least",
                            "objective": "focus",
                            "value": 5
                          },
                          "grant_abilities": ["xlib_test:dash"],
                          "grant_state_flags": ["xlib_test:empowered"]
                        }
                        """)
        );

        assertEquals(id("score_pack"), definition.snapshot().sourceId());
        assertTrue(definition.snapshot().abilities().contains(ABILITY_ID));
        assertTrue(definition.snapshot().stateFlags().contains(FLAG_ID));
    }

    @Test
    void namedConditionDefinitionsCanReferenceEachOther() {
        Map<ResourceLocation, AbilityRequirement> compiled = AbilityRequirementJsonParser.compileDefinitions(Map.of(
                id("conditions/base"), JsonParser.parseString("""
                        {
                          "type": "resource_at_least",
                          "resource": "xlib_test:focus",
                          "amount": 3
                        }
                        """),
                id("conditions/derived"), JsonParser.parseString("""
                        {
                          "type": "all",
                          "conditions": [
                            {
                              "type": "condition_ref",
                              "id": "xlib_test:conditions/base"
                            },
                            {
                              "type": "not",
                              "condition": false
                            }
                          ]
                        }
                        """)
        ));

        AbilityRequirement derived = compiled.get(id("conditions/derived"));
        assertTrue(derived.validate(null, AbilityApi.createDefaultData().withResourceAmount(RESOURCE_ID, 5)).isEmpty());
        assertTrue(derived.validate(null, AbilityApi.createDefaultData().withResourceAmount(RESOURCE_ID, 1)).isPresent());
    }

    @Test
    void expandedBuiltinRequirementTypesParseFromJson() {
        AbilityRequirement itemTagRequirement = AbilityRequirementJsonParser.parse(JsonParser.parseString("""
                {
                  "type": "holding_tag",
                  "item_tag": "minecraft:logs"
                }
                """));
        AbilityRequirement timeRequirement = AbilityRequirementJsonParser.parse(JsonParser.parseString("""
                {
                  "type": "time_between",
                  "start": 12000,
                  "end": 23000
                }
                """));
        AbilityRequirement healthRequirement = AbilityRequirementJsonParser.parse(JsonParser.parseString("""
                {
                  "type": "health_at_least",
                  "amount": 6.5
                }
                """));
        AbilityRequirement clearWeatherRequirement = AbilityRequirementJsonParser.parse(JsonParser.parseString("""
                {
                  "type": "clear_weather"
                }
                """));
        AbilityRequirement daytimeRequirement = AbilityRequirementJsonParser.parse(JsonParser.parseString("""
                {
                  "type": "daytime"
                }
                """));
        AbilityRequirement nighttimeRequirement = AbilityRequirementJsonParser.parse(JsonParser.parseString("""
                {
                  "type": "nighttime"
                }
                """));
        AbilityRequirement swimmingRequirement = AbilityRequirementJsonParser.parse(JsonParser.parseString("""
                {
                  "type": "swimming"
                }
                """));
        AbilityRequirement openSkyRequirement = AbilityRequirementJsonParser.parse(JsonParser.parseString("""
                {
                  "type": "under_open_sky"
                }
                """));
        AbilityRequirement standingOnBlockRequirement = AbilityRequirementJsonParser.parse(JsonParser.parseString("""
                {
                  "type": "standing_on_block",
                  "block": "minecraft:stone"
                }
                """));
        AbilityRequirement standingOnBlockTagRequirement = AbilityRequirementJsonParser.parse(JsonParser.parseString("""
                {
                  "type": "standing_on_block_tag",
                  "block_tag": "minecraft:dirt"
                }
                """));
        AbilityRequirement biomeTagRequirement = AbilityRequirementJsonParser.parse(JsonParser.parseString("""
                {
                  "type": "biome_tag",
                  "biome_tag": "minecraft:is_overworld"
                }
                """));
        AbilityRequirement counterRequirement = AbilityRequirementJsonParser.parse(JsonParser.parseString("""
                {
                  "type": "counter_at_least",
                  "counter": "xlib_test:counter/hunts",
                  "amount": 3
                }
                """));
        AbilityRequirement markRequirement = AbilityRequirementJsonParser.parse(JsonParser.parseString("""
                {
                  "type": "mark_stacks_at_least",
                  "mark": "xlib_test:mark/exposed",
                  "amount": 2
                }
                """));

        assertTrue(itemTagRequirement.validate(null, AbilityApi.createDefaultData()).isPresent());
        assertTrue(timeRequirement.validate(null, AbilityApi.createDefaultData()).isPresent());
        assertTrue(healthRequirement.validate(null, AbilityApi.createDefaultData()).isPresent());
        assertEquals("message.xlib.requirement_clear_weather", clearWeatherRequirement.description().getString());
        assertEquals("message.xlib.requirement_daytime", daytimeRequirement.description().getString());
        assertEquals("message.xlib.requirement_nighttime", nighttimeRequirement.description().getString());
        assertEquals("message.xlib.requirement_swimming", swimmingRequirement.description().getString());
        assertEquals("message.xlib.requirement_under_open_sky", openSkyRequirement.description().getString());
        assertEquals("message.xlib.requirement_standing_on_block", standingOnBlockRequirement.description().getString());
        assertEquals("message.xlib.requirement_standing_on_block_tag", standingOnBlockTagRequirement.description().getString());
        assertEquals("message.xlib.requirement_biome_tag", biomeTagRequirement.description().getString());
        assertEquals("message.xlib.requirement_counter_at_least", counterRequirement.description().getString());
        assertEquals("message.xlib.requirement_mark_stacks", markRequirement.description().getString());
    }

    @Test
    void malformedNestedRequirementReportsJsonPath() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                AbilityRequirementJsonParser.parse(JsonParser.parseString("""
                        {
                          "type": "all",
                          "conditions": [
                            true,
                            {
                              "type": "not",
                              "condition": {
                                "unexpected": "value"
                              }
                            }
                          ]
                        }
                        """))
        );

        assertTrue(exception.getMessage().contains("$.conditions[1].condition"));
        assertTrue(exception.getMessage().contains("Missing 'type' field"));
    }

    @Test
    void missingNamedConditionReportsResolutionPath() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                AbilityRequirementJsonParser.compileDefinitions(Map.of(
                        id("conditions/root"), JsonParser.parseString("""
                                {
                                  "type": "condition_ref",
                                  "id": "xlib_test:conditions/missing"
                                }
                                """)
                ))
        );

        assertTrue(exception.getMessage().contains("Failed to compile named condition xlib_test:conditions/root"));
        assertTrue(exception.getMessage().contains("Unknown named condition definition: xlib_test:conditions/missing"));
        assertTrue(exception.getMessage().contains("xlib_test:conditions/root -> xlib_test:conditions/missing"));
    }

    @Test
    void circularNamedConditionsReportFullReferenceChain() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                AbilityRequirementJsonParser.compileDefinitions(Map.of(
                        id("conditions/root"), JsonParser.parseString("""
                                {
                                  "type": "condition_ref",
                                  "id": "xlib_test:conditions/branch"
                                }
                                """),
                        id("conditions/branch"), JsonParser.parseString("""
                                {
                                  "type": "condition_ref",
                                  "id": "xlib_test:conditions/root"
                                }
                                """)
                ))
        );

        assertTrue(exception.getMessage().contains("Circular named condition reference"));
        assertTrue(exception.getMessage().contains("xlib_test:conditions/root"));
        assertTrue(exception.getMessage().contains("xlib_test:conditions/branch"));
    }

    @Test
    void entitySystemRequirementTypesParseFromJson() {
        AbilityRequirement capabilityPolicyRequirement = AbilityRequirementJsonParser.parse(JsonParser.parseString("""
                {
                  "type": "capability_policy_active",
                  "policy": "xlib_test:policy/restricted"
                }
                """));
        AbilityRequirement lifecycleStageRequirement = AbilityRequirementJsonParser.parse(JsonParser.parseString("""
                {
                  "type": "lifecycle_stage_active",
                  "stage": "xlib_test:stage/larval"
                }
                """));
        AbilityRequirement visualFormRequirement = AbilityRequirementJsonParser.parse(JsonParser.parseString("""
                {
                  "type": "visual_form_active",
                  "form": "xlib_test:form/wolf_body"
                }
                """));
        AbilityRequirement bindingActiveRequirement = AbilityRequirementJsonParser.parse(JsonParser.parseString("""
                {
                  "type": "binding_active",
                  "binding": "xlib_test:binding/tether"
                }
                """));
        AbilityRequirement bindingKindRequirement = AbilityRequirementJsonParser.parse(JsonParser.parseString("""
                {
                  "type": "binding_kind_active",
                  "kind": "control"
                }
                """));
        AbilityRequirement bodyTransitionRequirement = AbilityRequirementJsonParser.parse(JsonParser.parseString("""
                {
                  "type": "body_transition_active"
                }
                """));

        assertEquals("message.xlib.requirement_capability_policy", capabilityPolicyRequirement.description().getString());
        assertEquals("message.xlib.requirement_lifecycle_stage", lifecycleStageRequirement.description().getString());
        assertEquals("message.xlib.requirement_visual_form", visualFormRequirement.description().getString());
        assertEquals("message.xlib.requirement_binding", bindingActiveRequirement.description().getString());
        assertEquals("message.xlib.requirement_binding_kind", bindingKindRequirement.description().getString());
        assertEquals("message.xlib.requirement_body_transition", bodyTransitionRequirement.description().getString());
    }

    @Test
    void bindingKindActiveRejectsUnknownKind() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                AbilityRequirementJsonParser.parse(JsonParser.parseString("""
                        {
                          "type": "binding_kind_active",
                          "kind": "not_a_real_kind"
                        }
                        """))
        );

        assertTrue(exception.getMessage().contains("not_a_real_kind"));
        assertTrue(exception.getMessage().contains("kind"));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }
}
