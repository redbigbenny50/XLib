package com.whatxe.xlib.lifecycle;

import net.minecraft.resources.ResourceLocation;

public record PendingLifecycleTransition(
        ResourceLocation targetStageId,
        LifecycleStageTrigger trigger,
        long requestedGameTime
) {}
