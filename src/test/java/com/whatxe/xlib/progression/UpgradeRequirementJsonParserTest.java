package com.whatxe.xlib.progression;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class UpgradeRequirementJsonParserTest {
    private static final ResourceLocation POINT_TYPE_ID = id("point/energy");
    private static final ResourceLocation NODE_ID = id("node/intro");

    @Test
    void compositeRequirementsReuseExistingUpgradeRequirementSemantics() {
        UpgradeRequirement requirement = UpgradeRequirementJsonParser.parse(JsonParser.parseString("""
                {
                  "type": "all",
                  "requirements": [
                    {
                      "type": "points_at_least",
                      "point_type": "xlib_test:point/energy",
                      "amount": 5
                    },
                    {
                      "type": "any_node_unlocked",
                      "nodes": ["xlib_test:node/intro", "xlib_test:node/alt"]
                    }
                  ]
                }
                """));

        UpgradeProgressData missingAll = UpgradeProgressData.empty();
        UpgradeProgressData ready = UpgradeProgressData.empty()
                .withPoints(POINT_TYPE_ID, 5)
                .withUnlockedNode(NODE_ID, true);

        assertTrue(requirement.validate(null, missingAll).isPresent());
        assertTrue(requirement.validate(null, ready).isEmpty());
    }

    @Test
    void malformedNestedRequirementReportsJsonPath() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                UpgradeRequirementJsonParser.parse(JsonParser.parseString("""
                        {
                          "type": "all",
                          "requirements": [
                            true,
                            {
                              "type": "not",
                              "requirement": {
                                "unexpected": "value"
                              }
                            }
                          ]
                        }
                        """))
        );

        assertTrue(exception.getMessage().contains("$.requirements[1].requirement"));
        assertTrue(exception.getMessage().contains("Missing 'type' field"));
    }

    @Test
    void invalidNestedIdReportsIndexedFieldPathContext() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                UpgradeRequirementJsonParser.parse(JsonParser.parseString("""
                        {
                          "type": "any_node_unlocked",
                          "nodes": ["xlib_test:node/intro", "bad id"]
                        }
                        """))
        );

        assertTrue(exception.getMessage().contains("$"));
        assertTrue(exception.getMessage().contains("nodes[1]"));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }
}
