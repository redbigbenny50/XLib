package com.whatxe.xlib.ability;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class AbilityGrantApiTest {
    private static final ResourceLocation ABILITY_ID = id("command_override");

    @Test
    void commandGrantedAbilitiesBypassViewAssignAndActivationRequirements() {
        AbilityRequirement failingRequirement = AbilityRequirement.of(
                Component.literal("always fail"),
                (player, data) -> Optional.of(Component.literal("always fail"))
        );
        AbilityDefinition ability = AbilityDefinition.builder(ABILITY_ID, AbilityIcon.ofTexture(id("command_override_icon")))
                .renderRequirement(failingRequirement)
                .assignRequirement(failingRequirement)
                .activateRequirement(failingRequirement)
                .action((player, data) -> AbilityUseResult.success(data))
                .build();

        AbilityData grantedData = AbilityData.empty()
                .withAbilityAccessRestricted(true)
                .withAbilityGrantSource(ABILITY_ID, AbilityGrantApi.COMMAND_SOURCE, true);

        assertTrue(AbilityGrantApi.firstViewFailure(null, grantedData, ability).isEmpty());
        assertTrue(AbilityGrantApi.firstAssignmentFailure(null, grantedData, ability).isEmpty());
        assertTrue(AbilityGrantApi.firstActivationFailure(null, grantedData, ability).isEmpty());
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }
}
