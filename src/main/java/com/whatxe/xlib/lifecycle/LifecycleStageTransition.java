package com.whatxe.xlib.lifecycle;

import net.minecraft.resources.ResourceLocation;

public record LifecycleStageTransition(
        ResourceLocation targetStageId,
        LifecycleStageTrigger trigger,
        boolean preserveElapsed
) {
    public static LifecycleStageTransition timer(ResourceLocation targetStageId) {
        return new LifecycleStageTransition(targetStageId, LifecycleStageTrigger.TIMER, false);
    }

    public static LifecycleStageTransition manual(ResourceLocation targetStageId) {
        return new LifecycleStageTransition(targetStageId, LifecycleStageTrigger.MANUAL, false);
    }

    public static LifecycleStageTransition onDeath(ResourceLocation targetStageId) {
        return new LifecycleStageTransition(targetStageId, LifecycleStageTrigger.DEATH, false);
    }

    public static LifecycleStageTransition onRespawn(ResourceLocation targetStageId) {
        return new LifecycleStageTransition(targetStageId, LifecycleStageTrigger.RESPAWN, false);
    }
}
