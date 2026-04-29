package com.whatxe.xlib.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class DataDrivenUpgradeContentApiTest {
    @Test
    void upgradeTrackAndNodeDefinitionsParseMetadataAndRewards() {
        DataDrivenUpgradeTrackApi.LoadedUpgradeTrackDefinition track = DataDrivenUpgradeTrackApi.parseDefinition(
                id("upgrade_tracks/origin"),
                JsonParser.parseString("""
                        {
                          "id": "xlib_test:track/origin",
                          "family": "xlib_test:family/origin",
                          "group": "xlib_test:group/origin",
                          "page": "xlib_test:page/intro",
                          "tags": ["xlib_test:tag/path"],
                          "root_nodes": ["xlib_test:node/intro"]
                        }
                        """)
        );
        DataDrivenUpgradeNodeApi.LoadedUpgradeNodeDefinition node = DataDrivenUpgradeNodeApi.parseDefinition(
                id("upgrade_nodes/intro"),
                JsonParser.parseString("""
                        {
                          "id": "xlib_test:node/intro",
                          "track": "xlib_test:track/origin",
                          "point_costs": {
                            "xlib_test:point/energy": 3
                          },
                          "requirements": [
                            {
                              "type": "points_at_least",
                              "point_type": "xlib_test:point/energy",
                              "amount": 3
                            }
                          ],
                          "rewards": {
                            "abilities": ["xlib_test:ability/dash"],
                            "identities": ["xlib_test:identity/origin"]
                          }
                        }
                        """)
        );

        assertEquals(id("track/origin"), track.id());
        assertTrue(track.definition().rootNodes().contains(id("node/intro")));
        assertEquals(id("node/intro"), node.id());
        assertEquals(id("track/origin"), node.definition().trackId());
        assertEquals(3, node.definition().pointCosts().get(id("point/energy")));
        assertTrue(node.definition().rewards().abilities().contains(id("ability/dash")));
        assertTrue(node.definition().rewards().identities().contains(id("identity/origin")));
    }

    @Test
    void upgradeRuleDefinitionsParseRewardsAndMatchers() {
        DataDrivenUpgradeConsumeRuleApi.LoadedUpgradeConsumeRule consumeRule = DataDrivenUpgradeConsumeRuleApi.parseDefinition(
                id("upgrade_consume_rules/energy_food"),
                JsonParser.parseString("""
                        {
                          "id": "xlib_test:consume/energy_food",
                          "item": "minecraft:apple",
                          "food_only": true,
                          "point_rewards": {
                            "xlib_test:point/energy": 2
                          },
                          "counter_rewards": {
                            "xlib_test:counter/apples": 1
                          }
                        }
                        """)
        );
        DataDrivenUpgradeKillRuleApi.LoadedUpgradeKillRule killRule = DataDrivenUpgradeKillRuleApi.parseDefinition(
                id("upgrade_kill_rules/hunter"),
                JsonParser.parseString("""
                        {
                          "id": "xlib_test:kill/hunter",
                          "target": "minecraft:zombie",
                          "required_ability": "xlib_test:ability/dash",
                          "point_rewards": {
                            "xlib_test:point/energy": 4
                          }
                        }
                        """)
        );

        assertTrue(consumeRule.definition().foodOnly());
        assertEquals(2, consumeRule.definition().pointRewards().get(id("point/energy")));
        assertTrue(killRule.definition().targetEntityIds().contains(id("minecraft:zombie")));
        assertEquals(id("ability/dash"), killRule.definition().requiredAbilityId().orElseThrow());
    }

    private static ResourceLocation id(String path) {
        String namespace = path.contains(":") ? path.substring(0, path.indexOf(':')) : "xlib_test";
        String value = path.contains(":") ? path.substring(path.indexOf(':') + 1) : path;
        return ResourceLocation.fromNamespaceAndPath(namespace, value);
    }
}
