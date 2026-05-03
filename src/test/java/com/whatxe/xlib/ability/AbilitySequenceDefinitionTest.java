package com.whatxe.xlib.ability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class AbilitySequenceDefinitionTest {
    private static final ResourceLocation ABILITY_ID = id("ability/sequence");

    @Test
    void sequenceBuilderDrivesOrderedStageCallbacks() {
        List<String> calls = new ArrayList<>();
        AbilitySequenceDefinition sequence = AbilitySequenceDefinition.builder()
                .stage(AbilitySequenceStage.builder("windup", 2)
                        .onEnter((player, data, state) -> {
                            calls.add("enter:windup:" + state.stageElapsedTicks());
                            return data;
                        })
                        .onTick((player, data, state) -> {
                            calls.add("tick:windup:" + state.stageElapsedTicks());
                            return data;
                        })
                        .onComplete((player, data, state) -> {
                            calls.add("complete:windup:" + state.stageElapsedTicks());
                            return data;
                        })
                        .build())
                .stage(AbilitySequenceStage.builder("release", 1)
                        .onEnter((player, data, state) -> {
                            calls.add("enter:release:" + state.stageElapsedTicks());
                            return data;
                        })
                        .onTick((player, data, state) -> {
                            calls.add("tick:release:" + state.stageElapsedTicks());
                            return data;
                        })
                        .onEnd((player, data, state, reason) -> {
                            calls.add("end:release:" + reason.name());
                            return data;
                        })
                        .build())
                .build();
        AbilityDefinition definition = AbilityDefinition.builder(ABILITY_ID, AbilityIcon.ofTexture(id("icon/sequence")))
                .sequence(sequence)
                .build();

        AbilityUseResult activation = definition.activate(null, AbilityData.empty());
        definition.tick(null, activation.data().withMode(ABILITY_ID, true).withActiveDuration(ABILITY_ID, 3));
        definition.tick(null, activation.data().withMode(ABILITY_ID, true).withActiveDuration(ABILITY_ID, 2));
        definition.tick(null, activation.data().withMode(ABILITY_ID, true).withActiveDuration(ABILITY_ID, 1));
        definition.end(null, activation.data().withMode(ABILITY_ID, true).withActiveDuration(ABILITY_ID, 1), AbilityEndReason.DURATION_EXPIRED);

        assertTrue(definition.toggleAbility());
        assertEquals(3, definition.durationTicks());
        assertEquals(List.of(
                "enter:windup:0",
                "tick:windup:0",
                "tick:windup:1",
                "complete:windup:1",
                "enter:release:0",
                "tick:release:0",
                "end:release:DURATION_EXPIRED"
        ), calls);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }
}
