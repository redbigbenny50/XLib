package com.whatxe.xlib.ability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class AbilityLoadoutApiTest {
    private static final ResourceLocation BASE_ABILITY_ID = id("base_ability");
    private static final ResourceLocation MODE_ABILITY_ID = id("mode_ability");
    private static final ResourceLocation OVERLAY_ABILITY_ID = id("overlay_ability");
    private static final ResourceLocation COMBO_ABILITY_ID = id("combo_ability");

    @Test
    void comboOverrideWinsOverModeOverlayAndBaseLoadout() {
        unregisterFixtures();
        registerSimpleAbility(BASE_ABILITY_ID);
        registerSimpleAbility(MODE_ABILITY_ID);
        registerSimpleAbility(OVERLAY_ABILITY_ID);
        registerSimpleAbility(COMBO_ABILITY_ID);

        ModeApi.registerMode(ModeDefinition.builder(MODE_ABILITY_ID)
                .overlayAbility(0, OVERLAY_ABILITY_ID)
                .build());

        AbilityData data = AbilityData.empty()
                .withAbilityInSlot(0, BASE_ABILITY_ID)
                .withMode(MODE_ABILITY_ID, true)
                .withComboOverride(0, COMBO_ABILITY_ID, 20);

        assertEquals(COMBO_ABILITY_ID, AbilityLoadoutApi.resolvedAbilityId(data, 0).orElseThrow());

        unregisterFixtures();
    }

    @Test
    void modeOverlayAppliesWhenNoComboOverrideExists() {
        unregisterFixtures();
        registerSimpleAbility(BASE_ABILITY_ID);
        registerSimpleAbility(MODE_ABILITY_ID);
        registerSimpleAbility(OVERLAY_ABILITY_ID);

        ModeApi.registerMode(ModeDefinition.builder(MODE_ABILITY_ID)
                .overlayAbility(0, OVERLAY_ABILITY_ID)
                .build());

        AbilityData data = AbilityData.empty()
                .withAbilityInSlot(0, BASE_ABILITY_ID)
                .withMode(MODE_ABILITY_ID, true);

        assertEquals(OVERLAY_ABILITY_ID, AbilityLoadoutApi.resolvedAbilityId(data, 0).orElseThrow());
        assertTrue(AbilityLoadoutApi.hasModeOverlay(data, 0));

        unregisterFixtures();
    }

    private static void registerSimpleAbility(ResourceLocation abilityId) {
        AbilityApi.unregisterAbility(abilityId);
        AbilityApi.registerAbility(AbilityDefinition.builder(abilityId, AbilityIcon.ofTexture(id(abilityId.getPath() + "_icon")))
                .action((player, data) -> AbilityUseResult.success(data, Component.literal("ok")))
                .build());
    }

    private static void unregisterFixtures() {
        AbilityApi.unregisterAbility(BASE_ABILITY_ID);
        AbilityApi.unregisterAbility(MODE_ABILITY_ID);
        AbilityApi.unregisterAbility(OVERLAY_ABILITY_ID);
        AbilityApi.unregisterAbility(COMBO_ABILITY_ID);
        ModeApi.unregisterMode(MODE_ABILITY_ID);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }
}
