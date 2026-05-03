package com.whatxe.xlib.integration.blib;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.blib.api.client.animation.v1.command.AzCommand;
import com.whatxe.xlib.ability.AbilityEndReason;
import com.whatxe.xlib.cue.XLibCueSurface;
import com.whatxe.xlib.cue.XLibRuntimeCue;
import com.whatxe.xlib.cue.XLibRuntimeCueType;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class XLibBLibCueBindingTest {
    private static final ResourceLocation BINDING_ID = id("binding");
    private static final ResourceLocation ABILITY_ID = id("ability");
    private static final ResourceLocation STATE_ID = id("state");

    @Test
    void abilityBindingsMatchOnlyExpectedAbilityAndSurface() {
        XLibBLibCueBinding binding = XLibBLibCueBinding.builder(
                        BINDING_ID,
                        XLibCueSurface.MODEL_ANIMATION,
                        command()
                )
                .cueType(XLibRuntimeCueType.ACTIVATION_START)
                .ability(ABILITY_ID)
                .build();

        assertTrue(binding.matches(XLibRuntimeCue.activationStart(ABILITY_ID), XLibCueSurface.MODEL_ANIMATION));
        assertFalse(binding.matches(XLibRuntimeCue.activationStart(id("other")), XLibCueSurface.MODEL_ANIMATION));
        assertFalse(binding.matches(XLibRuntimeCue.activationStart(ABILITY_ID), XLibCueSurface.EFFECT_PLAYBACK));
    }

    @Test
    void stateBindingsMatchOnlyExpectedStateAndCueType() {
        XLibBLibCueBinding binding = XLibBLibCueBinding.builder(
                        BINDING_ID,
                        XLibCueSurface.PLAYER_BODY_ANIMATION,
                        command()
                )
                .cueType(XLibRuntimeCueType.STATE_ENTER)
                .state(STATE_ID)
                .build();

        assertTrue(binding.matches(XLibRuntimeCue.stateEnter(STATE_ID), XLibCueSurface.PLAYER_BODY_ANIMATION));
        assertFalse(binding.matches(XLibRuntimeCue.stateExit(STATE_ID, AbilityEndReason.FORCE_ENDED), XLibCueSurface.PLAYER_BODY_ANIMATION));
        assertFalse(binding.matches(XLibRuntimeCue.stateEnter(id("other_state")), XLibCueSurface.PLAYER_BODY_ANIMATION));
    }

    @Test
    void endReasonAndProgressFiltersMatchOnlyQualifiedCues() {
        XLibBLibCueBinding binding = XLibBLibCueBinding.builder(
                        BINDING_ID,
                        XLibCueSurface.EFFECT_PLAYBACK,
                        command()
                )
                .cueType(XLibRuntimeCueType.RELEASE)
                .ability(ABILITY_ID)
                .endReason(AbilityEndReason.DURATION_EXPIRED)
                .progressAtLeast(10)
                .progressAtMost(20)
                .build();

        assertTrue(binding.matches(
                XLibRuntimeCue.release(ABILITY_ID, AbilityEndReason.DURATION_EXPIRED, 12, 20),
                XLibCueSurface.EFFECT_PLAYBACK
        ));
        assertFalse(binding.matches(
                XLibRuntimeCue.release(ABILITY_ID, AbilityEndReason.FORCE_ENDED, 12, 20),
                XLibCueSurface.EFFECT_PLAYBACK
        ));
        assertFalse(binding.matches(
                XLibRuntimeCue.release(ABILITY_ID, AbilityEndReason.DURATION_EXPIRED, 8, 20),
                XLibCueSurface.EFFECT_PLAYBACK
        ));
        assertFalse(binding.matches(
                XLibRuntimeCue.activationStart(ABILITY_ID),
                XLibCueSurface.EFFECT_PLAYBACK
        ));
    }

    @Test
    void cueSpecificCommandsOverrideDefaultCommandForMatchingCueType() {
        AzCommand defaultCommand = command();
        AzCommand releaseCommand = command();
        XLibBLibCueBinding binding = XLibBLibCueBinding.builder(
                        BINDING_ID,
                        XLibCueSurface.MODEL_ANIMATION,
                        defaultCommand
                )
                .cueTypes(XLibRuntimeCueType.ACTIVATION_START, XLibRuntimeCueType.RELEASE)
                .cueCommand(XLibRuntimeCueType.RELEASE, releaseCommand)
                .build();

        assertSame(defaultCommand, binding.resolveCommand(XLibRuntimeCue.activationStart(ABILITY_ID)));
        assertSame(
                releaseCommand,
                binding.resolveCommand(
                        XLibRuntimeCue.release(ABILITY_ID, AbilityEndReason.DURATION_EXPIRED, 20, 20)
                )
        );
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }

    private static AzCommand command() {
        return new AzCommand(List.of());
    }
}
