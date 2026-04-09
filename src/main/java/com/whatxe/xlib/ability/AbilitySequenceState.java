package com.whatxe.xlib.ability;

import net.minecraft.resources.ResourceLocation;

public record AbilitySequenceState(
        ResourceLocation abilityId,
        int stageIndex,
        int stageCount,
        int stageElapsedTicks,
        int stageRemainingTicks,
        int stageDurationTicks,
        int totalElapsedTicks,
        int totalRemainingTicks,
        int totalDurationTicks
) {}
