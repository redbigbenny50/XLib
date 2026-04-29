package com.whatxe.xlib.lifecycle;

import com.whatxe.xlib.XLib;
import com.whatxe.xlib.XLibRegistryGuard;
import com.whatxe.xlib.attachment.ModAttachments;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class LifecycleStageApi {
    public static final ResourceLocation COMMAND_SOURCE =
            ResourceLocation.fromNamespaceAndPath(XLib.MODID, "command_lifecycle_stage");

    private static final Map<ResourceLocation, LifecycleStageDefinition> DEFINITIONS = new LinkedHashMap<>();

    private LifecycleStageApi() {}

    public static void bootstrap() {}

    // --- Registration ---

    public static LifecycleStageDefinition register(LifecycleStageDefinition definition) {
        XLibRegistryGuard.ensureMutable("lifecycle_stages");
        LifecycleStageDefinition previous = DEFINITIONS.putIfAbsent(definition.id(), definition);
        if (previous != null) {
            throw new IllegalStateException("Duplicate lifecycle stage registration: " + definition.id());
        }
        return definition;
    }

    public static Optional<LifecycleStageDefinition> unregister(ResourceLocation id) {
        XLibRegistryGuard.ensureMutable("lifecycle_stages");
        return Optional.ofNullable(DEFINITIONS.remove(id));
    }

    public static Optional<LifecycleStageDefinition> findDefinition(ResourceLocation id) {
        return Optional.ofNullable(DEFINITIONS.get(id));
    }

    public static Collection<LifecycleStageDefinition> all() {
        return List.copyOf(DEFINITIONS.values());
    }

    // --- State queries ---

    public static Optional<LifecycleStageState> state(Player player) {
        return getData(player).activeStage();
    }

    public static boolean isInStage(Player player, ResourceLocation stageId) {
        return state(player).map(s -> s.currentStageId().equals(stageId)).orElse(false);
    }

    // --- Mutations ---

    public static boolean setStage(ServerPlayer player, ResourceLocation stageId, ResourceLocation sourceId) {
        LifecycleStageDefinition def = DEFINITIONS.get(stageId);
        if (def == null) return false;

        long gameTime = player.level().getGameTime();
        LifecycleStageState newState = new LifecycleStageState(
                stageId, sourceId, gameTime, 0, Optional.empty(), LifecycleStageStatus.ACTIVE
        );
        setData(player, getData(player).withStage(newState));
        applyProjections(player, def, sourceId);
        return true;
    }

    public static boolean requestTransition(ServerPlayer player, ResourceLocation targetStageId, ResourceLocation sourceId) {
        Optional<LifecycleStageState> current = state(player);
        if (current.isEmpty()) return false;

        LifecycleStageDefinition currentDef = DEFINITIONS.get(current.get().currentStageId());
        if (currentDef == null) return false;
        if (!currentDef.manualTransitionTargets().contains(targetStageId)) return false;

        long gameTime = player.level().getGameTime();
        PendingLifecycleTransition pending = new PendingLifecycleTransition(
                targetStageId, LifecycleStageTrigger.MANUAL, gameTime
        );
        setData(player, getData(player).withStage(current.get().withPending(pending)));
        return true;
    }

    public static boolean completePendingTransition(ServerPlayer player) {
        Optional<LifecycleStageState> current = state(player);
        if (current.isEmpty() || current.get().pendingTransition().isEmpty()) return false;

        ResourceLocation targetId = current.get().pendingTransition().get().targetStageId();
        ResourceLocation sourceId = current.get().sourceId();
        clearProjections(player, current.get().currentStageId(), sourceId);
        return setStage(player, targetId, sourceId);
    }

    public static boolean clearStage(ServerPlayer player, ResourceLocation sourceId) {
        Optional<LifecycleStageState> current = state(player);
        if (current.isEmpty()) return false;
        clearProjections(player, current.get().currentStageId(), sourceId);
        setData(player, getData(player).withoutStage());
        return true;
    }

    // --- Tick ---

    public static void tick(ServerPlayer player) {
        Optional<LifecycleStageState> current = state(player);
        if (current.isEmpty()) return;

        LifecycleStageState s = current.get();
        LifecycleStageDefinition def = DEFINITIONS.get(s.currentStageId());
        if (def == null) return;

        int newElapsed = s.elapsedTicks() + 1;
        LifecycleStageState updated = s.withElapsed(newElapsed);

        // Check timer-based auto-transitions
        if (def.durationTicks().isPresent() && newElapsed >= def.durationTicks().get()) {
            for (LifecycleStageTransition transition : def.autoTransitions()) {
                if (transition.trigger() == LifecycleStageTrigger.TIMER) {
                    clearProjections(player, s.currentStageId(), s.sourceId());
                    setStage(player, transition.targetStageId(), s.sourceId());
                    return;
                }
            }
        }

        setData(player, getData(player).withStage(updated));
    }

    // --- Trigger-based hooks ---

    public static void onDeath(ServerPlayer player) {
        Optional<LifecycleStageState> current = state(player);
        if (current.isEmpty()) return;
        LifecycleStageDefinition def = DEFINITIONS.get(current.get().currentStageId());
        if (def == null) return;
        for (LifecycleStageTransition transition : def.autoTransitions()) {
            if (transition.trigger() == LifecycleStageTrigger.DEATH) {
                clearProjections(player, current.get().currentStageId(), current.get().sourceId());
                setStage(player, transition.targetStageId(), current.get().sourceId());
                return;
            }
        }
    }

    public static void onRespawn(ServerPlayer player) {
        Optional<LifecycleStageState> current = state(player);
        if (current.isEmpty()) return;
        LifecycleStageDefinition def = DEFINITIONS.get(current.get().currentStageId());
        if (def == null) return;
        for (LifecycleStageTransition transition : def.autoTransitions()) {
            if (transition.trigger() == LifecycleStageTrigger.RESPAWN) {
                clearProjections(player, current.get().currentStageId(), current.get().sourceId());
                setStage(player, transition.targetStageId(), current.get().sourceId());
                return;
            }
        }
    }

    // --- Projection helpers ---

    private static void applyProjections(ServerPlayer player, LifecycleStageDefinition def, ResourceLocation sourceId) {
        for (ResourceLocation flagId : def.projectedStateFlags()) {
            tryApplyStateFlag(player, flagId, sourceId);
        }
        for (ResourceLocation policyId : def.projectedCapabilityPolicies()) {
            tryApplyCapabilityPolicy(player, policyId, sourceId);
        }
    }

    private static void clearProjections(ServerPlayer player, ResourceLocation stageId, ResourceLocation sourceId) {
        LifecycleStageDefinition def = DEFINITIONS.get(stageId);
        if (def == null) return;
        for (ResourceLocation flagId : def.projectedStateFlags()) {
            tryRevokeStateFlag(player, flagId, sourceId);
        }
        for (ResourceLocation policyId : def.projectedCapabilityPolicies()) {
            tryRevokeCapabilityPolicy(player, policyId, sourceId);
        }
    }

    private static void tryApplyStateFlag(ServerPlayer player, ResourceLocation flagId, ResourceLocation sourceId) {
        try {
            com.whatxe.xlib.ability.StateFlagApi.grant(player, flagId, sourceId);
        } catch (Exception ignored) {}
    }

    private static void tryRevokeStateFlag(ServerPlayer player, ResourceLocation flagId, ResourceLocation sourceId) {
        try {
            com.whatxe.xlib.ability.StateFlagApi.revoke(player, flagId, sourceId);
        } catch (Exception ignored) {}
    }

    private static void tryApplyCapabilityPolicy(ServerPlayer player, ResourceLocation policyId, ResourceLocation sourceId) {
        try {
            com.whatxe.xlib.capability.CapabilityPolicyApi.apply(player, policyId, sourceId);
        } catch (Exception ignored) {}
    }

    private static void tryRevokeCapabilityPolicy(ServerPlayer player, ResourceLocation policyId, ResourceLocation sourceId) {
        try {
            com.whatxe.xlib.capability.CapabilityPolicyApi.revoke(player, policyId, sourceId);
        } catch (Exception ignored) {}
    }

    // --- Sanitize ---

    public static LifecycleStageData sanitize(LifecycleStageData data) {
        if (data.activeStage().isEmpty()) return data;
        ResourceLocation stageId = data.activeStage().get().currentStageId();
        if (!DEFINITIONS.containsKey(stageId)) return LifecycleStageData.empty();
        return data;
    }

    // --- Attachment access ---

    public static LifecycleStageData getData(Player player) {
        return player.getData(ModAttachments.PLAYER_LIFECYCLE_STAGE);
    }

    public static void setData(ServerPlayer player, LifecycleStageData data) {
        player.setData(ModAttachments.PLAYER_LIFECYCLE_STAGE, sanitize(data));
    }
}
