package com.whatxe.xlib.ability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class AbilityRequirementsTest {
    private static final ResourceLocation MODE_ID = id("berserk_mode");
    private static final ResourceLocation COMBO_ID = id("combo_followup");

    @Test
    void modeActiveOnlyPassesWhenModeIsRunning() {
        AbilityRequirement requirement = AbilityRequirements.modeActive(MODE_ID);

        assertTrue(requirement.validate(null, AbilityData.empty().withMode(MODE_ID, true)).isEmpty());
        assertFalse(requirement.validate(null, AbilityData.empty()).isEmpty());
    }

    @Test
    void modeInactiveOnlyPassesWhenModeIsNotRunning() {
        AbilityRequirement requirement = AbilityRequirements.modeInactive(MODE_ID);

        assertTrue(requirement.validate(null, AbilityData.empty()).isEmpty());
        assertFalse(requirement.validate(null, AbilityData.empty().withMode(MODE_ID, true)).isEmpty());
    }

    @Test
    void comboWindowOnlyPassesDuringActiveWindow() {
        AbilityRequirement requirement = AbilityRequirements.comboWindowActive(COMBO_ID);

        assertTrue(requirement.validate(null, AbilityData.empty().withComboWindow(COMBO_ID, 10)).isEmpty());
        assertFalse(requirement.validate(null, AbilityData.empty()).isEmpty());
    }

    @Test
    void predicateDescriptionsCanResolveLazily() {
        AtomicReference<Component> description = new AtomicReference<>(Component.literal("First"));
        AbilityRequirement requirement = AbilityRequirements.predicate(description::get, (player, data) -> false);

        assertEquals("First", requirement.description().getString());
        assertEquals("First", requirement.validate(null, AbilityData.empty()).orElseThrow().getString());

        description.set(Component.literal("Second"));

        assertEquals("Second", requirement.description().getString());
        assertEquals("Second", requirement.validate(null, AbilityData.empty()).orElseThrow().getString());
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }
}
