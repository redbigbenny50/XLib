package com.whatxe.xlib.ability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class DataDrivenPassiveApiTest {
    private static final ResourceLocation TARGET_ABILITY_ID = id("ability/granted_target");

    @Test
    void passiveDefinitionsParseMetadataPresentationAndBoundedHooks() {
        AbilityApi.unregisterAbility(TARGET_ABILITY_ID);
        try {
            AbilityApi.registerAbility(AbilityDefinition.builder(TARGET_ABILITY_ID, AbilityIcon.ofTexture(id("icon/granted_target")))
                    .action((player, data) -> AbilityUseResult.success(data))
                    .build());

            DataDrivenPassiveApi.LoadedPassiveDefinition loaded = DataDrivenPassiveApi.parseDefinition(
                    id("passives/heat_aura"),
                    JsonParser.parseString("""
                            {
                              "id": "xlib_test:passive/heat_aura",
                              "display_name": "Heat Aura",
                              "description": "Keeps power flowing while active.",
                              "icon": "xlib_test:textures/gui/heat_aura",
                              "family": "xlib_test:family/passive",
                              "group": "xlib_test:group/fire",
                              "page": "xlib_test:page/core",
                              "tags": ["xlib_test:tag/reactive"],
                              "cooldown_tick_rate_multiplier": 1.25,
                              "on_jump_effects": [
                                {
                                  "type": "grant_ability",
                                  "ability": "xlib_test:ability/granted_target"
                                }
                              ]
                            }
                            """)
            );

            PassiveDefinition definition = loaded.definition();
            assertEquals(id("passive/heat_aura"), loaded.id());
            assertEquals("Heat Aura", definition.displayName().getString());
            assertEquals("Keeps power flowing while active.", definition.description().getString());
            assertTrue(definition.hasCustomDescription());
            assertEquals(id("family/passive"), definition.familyId().orElseThrow());
            assertEquals(id("group/fire"), definition.groupId().orElseThrow());
            assertEquals(id("page/core"), definition.pageId().orElseThrow());
            assertTrue(definition.hasTag(id("tag/reactive")));
            assertEquals(1.25D, definition.cooldownTickRateMultiplier());
            assertEquals(Set.of(PassiveDefinition.Hook.JUMP), definition.authoredHooks());

            AbilityData updatedData = definition.onJump(null, AbilityData.empty());
            assertTrue(updatedData.abilityGrantSourcesFor(TARGET_ABILITY_ID).contains(definition.id()));
        } finally {
            AbilityApi.unregisterAbility(TARGET_ABILITY_ID);
        }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }
}
