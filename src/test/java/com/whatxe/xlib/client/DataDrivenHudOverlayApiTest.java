package com.whatxe.xlib.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.gson.JsonParser;
import com.whatxe.xlib.ability.AbilityRequirement;
import com.whatxe.xlib.ability.AbilityRequirements;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class DataDrivenHudOverlayApiTest {
    @AfterEach
    void clearNamedClientConditions() {
        ClientConditionApi.setLoadedConditionsForTesting(Map.of());
    }

    @Test
    void textOverlayDefinitionsParseWithScoreboardAppendSupport() {
        DataDrivenHudOverlayApi.HudOverlayDefinition definition = DataDrivenHudOverlayApi.parseDefinition(
                id("hud/focus_counter"),
                JsonParser.parseString("""
                        {
                          "type": "text",
                          "anchor": "above_hotbar_center",
                          "x": 0,
                          "y": -12,
                          "priority": 4,
                          "scale": 1.5,
                          "text": "Focus: ",
                          "append_score_objective": "focus",
                          "color": "#FFE38B"
                        }
                        """)
        );

        assertEquals(4, definition.priority());
        assertEquals(id("hud/focus_counter"), definition.id());
    }

    @Test
    void resourceBarOverlayDefinitionsParse() {
        DataDrivenHudOverlayApi.HudOverlayDefinition definition = DataDrivenHudOverlayApi.parseDefinition(
                id("hud/focus_bar"),
                JsonParser.parseString("""
                        {
                          "type": "resource_bar",
                          "anchor": "above_hotbar_left",
                          "resource": "xlib_test:focus",
                          "width": 92,
                          "height": 12,
                          "show_name": false,
                          "show_value": true
                        }
                        """)
        );

        assertEquals(id("hud/focus_bar"), definition.id());
        assertNotNull(definition);
    }

    @Test
    void statusBadgeOverlayDefinitionsParse() {
        DataDrivenHudOverlayApi.HudOverlayDefinition definition = DataDrivenHudOverlayApi.parseDefinition(
                id("hud/warning_badge"),
                JsonParser.parseString("""
                        {
                          "type": "status_badge",
                          "anchor": "top_right",
                          "text": "Warning ",
                          "append_score_objective": "alerts",
                          "background_color": "#AA101010",
                          "border_color": "#FFE38B",
                          "padding_x": 5,
                          "padding_y": 3
                        }
                        """)
        );

        assertEquals(id("hud/warning_badge"), definition.id());
    }

    @Test
    void abilityStatusBadgeOverlayDefinitionsParse() {
        DataDrivenHudOverlayApi.HudOverlayDefinition definition = DataDrivenHudOverlayApi.parseDefinition(
                id("hud/dash_status"),
                JsonParser.parseString("""
                        {
                          "type": "ability_status_badge",
                          "anchor": "bottom_right",
                          "ability": "xlib_test:dash",
                          "text": "Dash ",
                          "show_icon": true,
                          "icon_size": 14,
                          "show_when_ready": false,
                          "background_color": "#AA101010",
                          "border_color": "#FF33D17A"
                        }
                        """)
        );

        assertEquals(id("hud/dash_status"), definition.id());
    }

    @Test
    void iconOverlayDefinitionsCanUseNamedClientConditions() {
        AbilityRequirement namedCondition = AbilityRequirements.always();
        ClientConditionApi.setLoadedConditionsForTesting(Map.of(id("conditions/show_icon"), namedCondition));

        DataDrivenHudOverlayApi.HudOverlayDefinition definition = DataDrivenHudOverlayApi.parseDefinition(
                id("hud/skill_icon"),
                JsonParser.parseString("""
                        {
                          "type": "icon",
                          "anchor": "above_hotbar_right",
                          "ability": "xlib_test:dash",
                          "size": 18,
                          "when": {
                            "type": "condition_ref",
                            "id": "xlib_test:conditions/show_icon"
                          }
                        }
                        """)
        );

        assertEquals(id("hud/skill_icon"), definition.id());
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }
}
