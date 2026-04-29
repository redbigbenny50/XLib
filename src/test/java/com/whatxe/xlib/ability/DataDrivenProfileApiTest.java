package com.whatxe.xlib.ability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class DataDrivenProfileApiTest {
    @Test
    void profileGroupDefinitionsParseOnboardingFlags() {
        DataDrivenProfileGroupApi.LoadedProfileGroupDefinition definition = DataDrivenProfileGroupApi.parseDefinition(
                id("profile_groups/origin"),
                JsonParser.parseString("""
                        {
                          "id": "xlib_test:group/origin",
                          "display_name": "Origin",
                          "selection_limit": 2,
                          "required_onboarding": true,
                          "onboarding_triggers": ["first_login", "command"],
                          "blocks_ability_use": true,
                          "player_can_reset": true
                        }
                        """)
        );

        assertEquals(id("group/origin"), definition.id());
        assertEquals(2, definition.definition().selectionLimit());
        assertTrue(definition.definition().requiredOnboarding());
        assertTrue(definition.definition().onboardingTriggers().contains(ProfileOnboardingTrigger.FIRST_LOGIN));
        assertTrue(definition.definition().onboardingTriggers().contains(ProfileOnboardingTrigger.COMMAND));
        assertTrue(definition.definition().blocksAbilityUse());
        assertTrue(definition.definition().playerCanReset());
    }

    @Test
    void profileDefinitionsParseAuthoredGrantTargets() {
        DataDrivenProfileApi.LoadedProfileDefinition definition = DataDrivenProfileApi.parseDefinition(
                id("profiles/vanguard"),
                JsonParser.parseString("""
                        {
                          "id": "xlib_test:profile/vanguard",
                          "group": "xlib_test:group/origin",
                          "display_name": "Vanguard",
                          "icon": "xlib_test:textures/gui/vanguard.png",
                          "grant_bundles": ["xlib_test:bundle/core"],
                          "abilities": ["xlib_test:ability/dash"],
                          "passives": ["xlib_test:passive/guard"],
                          "state_flags": ["xlib_test:flag/ready"],
                          "starting_nodes": ["xlib_test:node/intro"]
                        }
                        """)
        );

        assertEquals(id("profile/vanguard"), definition.id());
        assertEquals(id("group/origin"), definition.definition().groupId());
        assertTrue(definition.definition().grantBundles().contains(id("bundle/core")));
        assertTrue(definition.definition().abilities().contains(id("ability/dash")));
        assertTrue(definition.definition().passives().contains(id("passive/guard")));
        assertTrue(definition.definition().stateFlags().contains(id("flag/ready")));
        assertTrue(definition.definition().startingNodes().contains(id("node/intro")));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }
}
