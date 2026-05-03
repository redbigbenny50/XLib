package com.whatxe.xlib.ability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class DataDrivenModeApiTest {
    @Test
    void modeDefinitionsParseMetadataCycleAndProjectionFields() {
        DataDrivenModeApi.LoadedModeDefinition definition = DataDrivenModeApi.parseDefinition(
                id("modes/flame_form"),
                JsonParser.parseString("""
                        {
                          "ability": "xlib_test:mode/flame_form",
                          "family": "xlib_test:family/forms",
                          "group": "xlib_test:group/awakenings",
                          "page": "xlib_test:page/offense",
                          "tags": ["xlib_test:tag/fire"],
                          "priority": 5,
                          "stackable": true,
                          "cycle_group": "xlib_test:cycle/forms",
                          "cycle_order": 2,
                          "reset_cycle_groups": ["xlib_test:cycle/forms"],
                          "cooldown_tick_rate_multiplier": 1.5,
                          "health_cost_per_tick": 1.0,
                          "minimum_health": 2.0,
                          "resource_delta_per_tick": {
                            "xlib_test:resource/ki": -0.5
                          },
                          "exclusive_modes": ["xlib_test:mode/base_form"],
                          "blocked_by_modes": ["xlib_test:mode/exhausted"],
                          "transforms_from": ["xlib_test:mode/base_form"],
                          "overlay_abilities": {
                            "0": "xlib_test:ability/flame_burst"
                          },
                          "grant_abilities": ["xlib_test:ability/flare"],
                          "grant_passives": ["xlib_test:passive/heat"],
                          "grant_state_flags": ["xlib_test:flag/flame_form"],
                          "block_abilities": ["xlib_test:ability/guard"]
                        }
                        """)
        );

        ModeDefinition mode = definition.definition();
        assertEquals(id("mode/flame_form"), definition.id());
        assertEquals(id("family/forms"), mode.familyId().orElseThrow());
        assertEquals(id("group/awakenings"), mode.groupId().orElseThrow());
        assertEquals(id("page/offense"), mode.pageId().orElseThrow());
        assertTrue(mode.hasTag(id("tag/fire")));
        assertEquals(5, mode.priority());
        assertTrue(mode.stackable());
        assertEquals(id("cycle/forms"), mode.cycleGroupId());
        assertEquals(2, mode.cycleOrder());
        assertTrue(mode.resetCycleGroupsOnActivate().contains(id("cycle/forms")));
        assertEquals(1.5D, mode.cooldownTickRateMultiplier());
        assertEquals(-0.5D, mode.resourceDeltaPerTick().get(id("resource/ki")));
        assertEquals(id("ability/flame_burst"), mode.overlayAbilities().get(0));
        assertTrue(mode.grantedAbilities().contains(id("ability/flare")));
        assertTrue(mode.grantedPassives().contains(id("passive/heat")));
        assertTrue(mode.stateFlags().contains(id("flag/flame_form")));
        assertTrue(mode.blockedAbilities().contains(id("ability/guard")));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }
}
