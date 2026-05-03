package com.whatxe.xlib.ability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class DataDrivenComboChainApiTest {
    @Test
    void comboChainDefinitionsParseBranchConditionsAndTriggerType() {
        DataDrivenComboChainApi.LoadedComboChainDefinition definition = DataDrivenComboChainApi.parseDefinition(
                id("combo_chains/flame_chain"),
                JsonParser.parseString("""
                        {
                          "id": "xlib_test:chain/flame_chain",
                          "trigger_ability": "xlib_test:ability/jab",
                          "combo_ability": "xlib_test:ability/finisher",
                          "trigger": "hit_confirm",
                          "window_ticks": 16,
                          "transform_triggered_slot": true,
                          "branches": [
                            {
                              "combo_ability": "xlib_test:ability/flame_finisher",
                              "condition": {
                                "type": "mode_active",
                                "mode": "xlib_test:mode/flame_form"
                              }
                            }
                          ]
                        }
                        """)
        );

        ComboChainDefinition chain = definition.definition();
        assertEquals(id("chain/flame_chain"), definition.id());
        assertEquals(id("ability/jab"), chain.triggerAbilityId());
        assertEquals(id("ability/finisher"), chain.comboAbilityId());
        assertEquals(ComboChainDefinition.TriggerType.HIT_CONFIRM, chain.triggerType());
        assertEquals(16, chain.windowTicks());
        assertTrue(chain.transformTriggeredSlot());
        assertEquals(1, definition.branches().size());
        assertEquals(id("ability/flame_finisher"), definition.branches().getFirst().comboAbilityId());
        assertTrue(definition.branches().getFirst().requirement().description().getString().length() > 0);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }
}
