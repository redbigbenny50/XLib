package com.whatxe.xlib.ability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class ComboChainApiTest {
    private static final ResourceLocation CHAIN_ID = id("chain");
    private static final ResourceLocation HIT_CHAIN_ID = id("hit_chain");
    private static final ResourceLocation TRIGGER_ABILITY_ID = id("trigger");
    private static final ResourceLocation COMBO_ABILITY_ID = id("followup");
    private static final ResourceLocation HIT_COMBO_ABILITY_ID = id("hit_followup");

    @Test
    void applyActivationOpensAComboWindowAndOverridesTheTriggeredSlot() {
        unregisterFixtures();
        try {
            ComboChainApi.registerChain(ComboChainDefinition.builder(CHAIN_ID, TRIGGER_ABILITY_ID, COMBO_ABILITY_ID)
                    .windowTicks(12)
                    .transformTriggeredSlot()
                    .build());

            AbilityData updatedData = ComboChainApi.applyActivation(AbilityData.empty(), TRIGGER_ABILITY_ID, 3);

            assertEquals(12, updatedData.comboWindowFor(COMBO_ABILITY_ID));
            assertEquals(COMBO_ABILITY_ID, updatedData.comboOverrideInSlot(3).orElseThrow());
            assertEquals(12, updatedData.comboOverrideDurationForSlot(3));
        } finally {
            unregisterFixtures();
        }
    }

    @Test
    void consumingAComboAbilityClearsItsWindowAndSlotOverride() {
        AbilityData data = AbilityData.empty()
                .withComboWindow(COMBO_ABILITY_ID, 8)
                .withComboOverride(2, COMBO_ABILITY_ID, 8)
                .withComboOverride(5, TRIGGER_ABILITY_ID, 4);

        AbilityData updatedData = ComboChainApi.consumeComboAbility(data, COMBO_ABILITY_ID);

        assertEquals(0, updatedData.comboWindowFor(COMBO_ABILITY_ID));
        assertTrue(updatedData.comboOverrideInSlot(2).isEmpty());
        assertEquals(TRIGGER_ABILITY_ID, updatedData.comboOverrideInSlot(5).orElseThrow());
    }

    @Test
    void tickingComboStateExpiresOneTickWindowsAndOverrides() {
        AbilityData data = AbilityData.empty()
                .withComboWindow(COMBO_ABILITY_ID, 1)
                .withComboOverride(1, COMBO_ABILITY_ID, 1);

        AbilityData updatedData = ComboChainApi.tick(data);

        assertEquals(0, updatedData.comboWindowFor(COMBO_ABILITY_ID));
        assertTrue(updatedData.comboOverrideInSlot(1).isEmpty());
        assertEquals(0, updatedData.comboOverrideDurationForSlot(1));
    }

    @Test
    void hitConfirmChainsOnlyOpenWhenTheirTriggerTypeFires() {
        unregisterFixtures();
        try {
            ComboChainApi.registerChain(ComboChainDefinition.builder(HIT_CHAIN_ID, TRIGGER_ABILITY_ID, HIT_COMBO_ABILITY_ID)
                    .windowTicks(9)
                    .transformTriggeredSlot()
                    .triggerOnHit()
                    .build());

            AbilityData activationData = ComboChainApi.applyActivation(AbilityData.empty(), TRIGGER_ABILITY_ID, 4);
            assertEquals(0, activationData.comboWindowFor(HIT_COMBO_ABILITY_ID));

            AbilityData hitData = ComboChainApi.applyTrigger(
                    null,
                    AbilityData.empty(),
                    TRIGGER_ABILITY_ID,
                    4,
                    ComboChainDefinition.TriggerType.HIT_CONFIRM
            );
            assertEquals(9, hitData.comboWindowFor(HIT_COMBO_ABILITY_ID));
            assertEquals(HIT_COMBO_ABILITY_ID, hitData.comboOverrideInSlot(4).orElseThrow());
        } finally {
            unregisterFixtures();
        }
    }

    private static void unregisterFixtures() {
        ComboChainApi.unregisterChain(CHAIN_ID);
        ComboChainApi.unregisterChain(HIT_CHAIN_ID);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }
}
