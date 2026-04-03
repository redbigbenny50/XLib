package com.whatxe.xlib.ability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class ModeAbilityDefinitionTest {
    private static final ResourceLocation MODE_ID = id("mode_wrapper");
    private static final ResourceLocation GRANTED_ABILITY_ID = id("wrapper_granted_ability");

    @Test
    void registerModeAbilityRegistersBothTheToggleAbilityAndTheMode() {
        ModeApi.unregisterModeAbility(MODE_ID);
        try {
            ModeAbilityDefinition definition = ModeAbilityDefinition.builder(MODE_ID, AbilityIcon.ofTexture(id("wrapper_icon")))
                    .cooldownTicks(40)
                    .stackable()
                    .grantAbility(GRANTED_ABILITY_ID)
                    .action((player, data) -> AbilityUseResult.success(data))
                    .register();

            assertTrue(AbilityApi.findAbility(MODE_ID).isPresent());
            assertTrue(ModeApi.findMode(MODE_ID).isPresent());
            assertTrue(definition.ability().toggleAbility());
            assertEquals(MODE_ID, definition.mode().abilityId());
            assertTrue(definition.mode().stackable());
            assertTrue(definition.mode().grantedAbilities().contains(GRANTED_ABILITY_ID));
        } finally {
            ModeApi.unregisterModeAbility(MODE_ID);
        }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }
}
