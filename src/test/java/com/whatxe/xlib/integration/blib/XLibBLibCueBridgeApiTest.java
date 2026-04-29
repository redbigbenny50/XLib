package com.whatxe.xlib.integration.blib;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.blib.api.client.animation.v1.command.AzCommand;
import com.whatxe.xlib.ability.AbilityEndReason;
import com.whatxe.xlib.cue.XLibCueSurface;
import com.whatxe.xlib.cue.XLibRuntimeCue;
import com.whatxe.xlib.cue.XLibRuntimeCueType;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class XLibBLibCueBridgeApiTest {
    private static final ResourceLocation ABILITY_ID = id("bridge_ability");

    @AfterEach
    void cleanup() {
        XLibBLibCueBridgeApi.clear();
    }

    @Test
    void dispatchPlanOrdersBindingsByPriorityThenSpecificity() {
        AzCommand defaultCommand = command();
        AzCommand specificCommand = command();
        AzCommand higherPriorityCommand = command();

        XLibBLibCueBridgeApi.register(XLibBLibCueBinding.builder(
                        id("default"),
                        XLibCueSurface.MODEL_ANIMATION,
                        defaultCommand
                )
                .cueType(XLibRuntimeCueType.ACTIVATION_START)
                .build());
        XLibBLibCueBridgeApi.register(XLibBLibCueBinding.builder(
                        id("specific"),
                        XLibCueSurface.MODEL_ANIMATION,
                        specificCommand
                )
                .cueType(XLibRuntimeCueType.ACTIVATION_START)
                .ability(ABILITY_ID)
                .build());
        XLibBLibCueBridgeApi.register(XLibBLibCueBinding.builder(
                        id("higher_priority"),
                        XLibCueSurface.MODEL_ANIMATION,
                        higherPriorityCommand
                )
                .cueType(XLibRuntimeCueType.ACTIVATION_START)
                .priority(10)
                .build());

        List<XLibBLibCueBridgeApi.PlannedDispatch> plan = XLibBLibCueBridgeApi.planDispatches(
                XLibRuntimeCue.activationStart(ABILITY_ID),
                XLibCueSurface.MODEL_ANIMATION
        );

        assertEquals(
                List.of(id("higher_priority"), id("specific"), id("default")),
                plan.stream().map(dispatch -> dispatch.binding().id()).collect(Collectors.toList())
        );
        assertSame(higherPriorityCommand, plan.get(0).command());
        assertSame(specificCommand, plan.get(1).command());
        assertSame(defaultCommand, plan.get(2).command());
    }

    @Test
    void dispatchPlanStopsAfterFirstConsumingBinding() {
        AzCommand specificReleaseCommand = command();
        AzCommand fallbackCommand = command();

        XLibBLibCueBridgeApi.register(XLibBLibCueBinding.builder(
                        id("consuming"),
                        XLibCueSurface.EFFECT_PLAYBACK,
                        command()
                )
                .cueType(XLibRuntimeCueType.RELEASE)
                .ability(ABILITY_ID)
                .endReason(AbilityEndReason.DURATION_EXPIRED)
                .cueCommand(XLibRuntimeCueType.RELEASE, specificReleaseCommand)
                .consumeCue()
                .build());
        XLibBLibCueBridgeApi.register(XLibBLibCueBinding.builder(
                        id("fallback"),
                        XLibCueSurface.EFFECT_PLAYBACK,
                        fallbackCommand
                )
                .cueType(XLibRuntimeCueType.RELEASE)
                .build());

        List<XLibBLibCueBridgeApi.PlannedDispatch> plan = XLibBLibCueBridgeApi.planDispatches(
                XLibRuntimeCue.release(ABILITY_ID, AbilityEndReason.DURATION_EXPIRED, 15, 20),
                XLibCueSurface.EFFECT_PLAYBACK
        );

        assertEquals(List.of(id("consuming")), plan.stream()
                .map(dispatch -> dispatch.binding().id())
                .collect(Collectors.toList()));
        assertSame(specificReleaseCommand, plan.getFirst().command());
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }

    private static AzCommand command() {
        return new AzCommand(List.of());
    }
}
