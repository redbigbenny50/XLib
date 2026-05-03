package com.whatxe.xlib.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class DataDrivenTrackedValueApiTest {
    private static final ResourceLocation VALUE_ID = id("value/evolution");

    @Test
    void parsesTrackedValueDefinitionFromJson() {
        DataDrivenTrackedValueApi.LoadedTrackedValueDefinition definition = DataDrivenTrackedValueApi.parseDefinition(
                VALUE_ID,
                JsonParser.parseString("""
                        {
                          "display_name": "Evolution",
                          "min": 0,
                          "max": 250,
                          "starting_value": 12.5,
                          "tick_delta": -0.5,
                          "hud_color": "#44AAFF",
                          "replace_food_bar": true,
                          "food_replacement_intake_scale": 2.0,
                          "food_replacement_heal_threshold": 16,
                          "food_replacement_heal_interval_ticks": 40,
                          "food_replacement_heal_cost": 1.5,
                          "food_replacement_starvation_threshold": 2,
                          "food_replacement_starvation_interval_ticks": 60,
                          "food_replacement_starvation_damage": 2.5
                        }
                        """)
        );

        assertEquals(VALUE_ID, definition.id());
        assertEquals("Evolution", definition.definition().displayName().getString());
        assertEquals(0.0D, definition.definition().minValue(), 0.0001D);
        assertEquals(250.0D, definition.definition().maxValue(), 0.0001D);
        assertEquals(12.5D, definition.definition().startingValue(), 0.0001D);
        assertEquals(-0.5D, definition.definition().tickDelta(), 0.0001D);
        assertEquals(100, definition.definition().foodReplacementPriority());
        assertEquals(2.0D, definition.definition().foodReplacementIntakeScale(), 0.0001D);
        assertEquals(16, definition.definition().foodReplacementHealThreshold());
        assertEquals(40, definition.definition().foodReplacementHealIntervalTicks());
        assertEquals(1.5D, definition.definition().foodReplacementHealCost(), 0.0001D);
        assertEquals(2, definition.definition().foodReplacementStarvationThreshold());
        assertEquals(60, definition.definition().foodReplacementStarvationIntervalTicks());
        assertEquals(2.5F, definition.definition().foodReplacementStarvationDamage(), 0.0001F);
        assertTrue((definition.definition().hudColor() & 0x00FFFFFF) == 0x44AAFF);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }
}
