package com.whatxe.xlib.ability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class AbilityApiTest {
    private static final ResourceLocation ABILITY_ID = id("unregister_test");
    private static final ResourceLocation STALE_ABILITY_ID = id("stale_unregister_test");

    @Test
    void unregisterAbilityRemovesDefinitionAndDefaultLoadout() {
        AbilityApi.unregisterAbility(ABILITY_ID);

        AbilityDefinition definition = AbilityDefinition.builder(ABILITY_ID, AbilityIcon.ofTexture(id("ability_icon")))
                .action((player, data) -> AbilityUseResult.success(data))
                .build();

        AbilityApi.registerAbility(definition);
        AbilityApi.setDefaultAbility(0, ABILITY_ID);

        assertTrue(AbilityApi.findAbility(ABILITY_ID).isPresent());
        assertEquals(ABILITY_ID, AbilityApi.createDefaultData().abilityInSlot(0).orElseThrow());

        AbilityApi.unregisterAbility(ABILITY_ID);

        assertTrue(AbilityApi.findAbility(ABILITY_ID).isEmpty());
        assertTrue(AbilityApi.createDefaultData().abilityInSlot(0).isEmpty());
    }

    @Test
    void sanitizeDataClearsStateForUnregisteredAbilities() {
        AbilityApi.unregisterAbility(STALE_ABILITY_ID);

        AbilityDefinition definition = AbilityDefinition.builder(STALE_ABILITY_ID, AbilityIcon.ofTexture(id("stale_ability_icon")))
                .action((player, data) -> AbilityUseResult.success(data))
                .build();

        AbilityApi.registerAbility(definition);

        AbilityData data = AbilityData.empty()
                .withAbilityInSlot(0, STALE_ABILITY_ID)
                .withCooldown(STALE_ABILITY_ID, 40)
                .withMode(STALE_ABILITY_ID, true)
                .withActiveDuration(STALE_ABILITY_ID, 20)
                .withChargeCount(STALE_ABILITY_ID, 1)
                .withChargeRecharge(STALE_ABILITY_ID, 10)
                .withAbilityGrantSource(STALE_ABILITY_ID, id("source"), true)
                .withAbilityActivationBlockSource(STALE_ABILITY_ID, id("block_source"), true);

        AbilityApi.unregisterAbility(STALE_ABILITY_ID);
        AbilityData sanitized = AbilityApi.sanitizeData(data);

        assertTrue(sanitized.abilityInSlot(0).isEmpty());
        assertEquals(0, sanitized.cooldownFor(STALE_ABILITY_ID));
        assertFalse(sanitized.isModeActive(STALE_ABILITY_ID));
        assertEquals(0, sanitized.activeDurationFor(STALE_ABILITY_ID));
        assertTrue(sanitized.abilityGrantSourcesFor(STALE_ABILITY_ID).isEmpty());
        assertTrue(sanitized.activationBlockSourcesFor(STALE_ABILITY_ID).isEmpty());
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }
}

