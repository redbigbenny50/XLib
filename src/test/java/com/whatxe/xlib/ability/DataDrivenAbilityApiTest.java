package com.whatxe.xlib.ability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class DataDrivenAbilityApiTest {
    private static final ResourceLocation TARGET_ABILITY_ID = id("ability/granted_target");
    private static final ResourceLocation CONDITION_ID = id("condition/always_authored");

    @Test
    void abilityDefinitionsParseMetadataPresentationAndBoundedEffects() {
        AbilityApi.unregisterAbility(TARGET_ABILITY_ID);
        try {
            AbilityApi.registerAbility(AbilityDefinition.builder(TARGET_ABILITY_ID, AbilityIcon.ofTexture(id("icon/granted_target")))
                    .action((player, data) -> AbilityUseResult.success(data))
                    .build());

            DataDrivenAbilityApi.LoadedAbilityDefinition loaded = DataDrivenAbilityApi.parseDefinition(
                    id("abilities/flame_burst"),
                    JsonParser.parseString("""
                            {
                              "id": "xlib_test:ability/flame_burst",
                              "display_name": "Flame Burst",
                              "description": "Burst of flame.",
                              "icon": "xlib_test:textures/gui/flame_burst",
                              "family": "xlib_test:family/offense",
                              "group": "xlib_test:group/fire",
                              "page": "xlib_test:page/core",
                              "tags": ["xlib_test:tag/burst"],
                              "cooldown_ticks": 40,
                              "cooldown_policy": "on_use",
                              "activate_requirements": [
                                {
                                  "type": "score_at_least",
                                  "objective": "focus",
                                  "value": 3
                                }
                              ],
                              "resource_costs": {
                                "xlib_test:resource/ki": 10
                              },
                              "action": {
                                "type": "success",
                                "effects": [
                                  {
                                    "type": "grant_ability",
                                    "ability": "xlib_test:ability/granted_target"
                                  }
                                ]
                              }
                            }
                            """)
            );

            AbilityDefinition definition = loaded.definition();
            assertEquals(id("ability/flame_burst"), loaded.id());
            assertEquals("Flame Burst", definition.displayName().getString());
            assertEquals("Burst of flame.", definition.description().getString());
            assertTrue(definition.hasCustomDescription());
            assertEquals(id("family/offense"), definition.familyId().orElseThrow());
            assertEquals(id("group/fire"), definition.groupId().orElseThrow());
            assertEquals(id("page/core"), definition.pageId().orElseThrow());
            assertTrue(definition.hasTag(id("tag/burst")));
            assertEquals(40, definition.cooldownTicks());
            assertEquals(1, definition.activateRequirements().size());
            assertEquals(1, definition.resourceCosts().size());

            AbilityUseResult result = definition.activate(null, AbilityData.empty());
            assertTrue(result.data().abilityGrantSourcesFor(TARGET_ABILITY_ID).contains(definition.id()));
        } finally {
            AbilityApi.unregisterAbility(TARGET_ABILITY_ID);
        }
    }

    @Test
    void syncedAbilitiesCanResolveNamedConditionReferences() {
        DataDrivenConditionApi.clearSyncedDefinitions();
        DataDrivenAbilityApi.clearSyncedDefinitions();
        try {
            DataDrivenConditionApi.syncDefinitionsFromJson(Map.of(
                    CONDITION_ID,
                    """
                    {
                      "type": "always"
                    }
                    """
            ));
            DataDrivenAbilityApi.syncDefinitionsFromJson(Map.of(
                    id("abilities/synced"),
                    """
                    {
                      "id": "xlib_test:ability/synced",
                      "display_name": "Synced Ability",
                      "icon": "xlib_test:textures/gui/synced",
                      "activate_requirements": [
                        {
                          "type": "condition_ref",
                          "id": "xlib_test:condition/always_authored"
                        }
                      ],
                      "action": {
                        "type": "success"
                      }
                    }
                    """
            ));

            AbilityDefinition definition = AbilityApi.findAbility(id("ability/synced")).orElseThrow();
            assertEquals(Component.literal("Synced Ability").getString(), definition.displayName().getString());
            assertTrue(definition.firstFailedActivationRequirement(null, AbilityData.empty()).isEmpty());
        } finally {
            DataDrivenAbilityApi.clearSyncedDefinitions();
            DataDrivenConditionApi.clearSyncedDefinitions();
        }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }
}
