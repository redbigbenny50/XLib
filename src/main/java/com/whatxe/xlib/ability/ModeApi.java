package com.whatxe.xlib.ability;

import com.whatxe.xlib.XLibRegistryGuard;
import com.whatxe.xlib.attachment.ModAttachments;
import com.whatxe.xlib.api.event.XLibModeEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;

public final class ModeApi {
    private static final Map<ResourceLocation, ModeDefinition> MODES = new LinkedHashMap<>();
    private static final Comparator<ModeDefinition> ACTIVE_MODE_ORDER = Comparator
            .comparingInt(ModeDefinition::priority)
            .reversed()
            .thenComparing(mode -> mode.abilityId().toString());

    private ModeApi() {}

    public static void bootstrap() {}

    public static ModeDefinition registerMode(ModeDefinition mode) {
        XLibRegistryGuard.ensureMutable("modes");
        ModeDefinition previous = MODES.putIfAbsent(mode.abilityId(), mode);
        if (previous != null) {
            throw new IllegalStateException("Duplicate mode registration: " + mode.abilityId());
        }
        return mode;
    }

    public static Optional<ModeDefinition> unregisterMode(ResourceLocation modeAbilityId) {
        XLibRegistryGuard.ensureMutable("modes");
        return Optional.ofNullable(MODES.remove(modeAbilityId));
    }

    public static Optional<ModeDefinition> findMode(ResourceLocation modeAbilityId) {
        return Optional.ofNullable(MODES.get(modeAbilityId));
    }

    public static Collection<ModeDefinition> allModes() {
        return List.copyOf(MODES.values());
    }

    public static Optional<ResourceLocation> resolveOverlayAbility(AbilityData data, int slot) {
        for (ModeDefinition mode : activeModes(data)) {
            ResourceLocation overlayAbility = mode.overlayAbilities().get(slot);
            if (overlayAbility != null) {
                return Optional.of(overlayAbility);
            }
        }
        return Optional.empty();
    }

    public static List<ContextGrantSnapshot> collectSnapshots(AbilityData data) {
        List<ContextGrantSnapshot> snapshots = new ArrayList<>();
        for (ModeDefinition mode : activeModes(data)) {
            ContextGrantSnapshot snapshot = mode.snapshot();
            if (!snapshot.isEmpty()) {
                snapshots.add(snapshot);
            }
        }
        return List.copyOf(snapshots);
    }

    public static Optional<Component> firstActivationFailure(Player player, AbilityDefinition ability, AbilityData data) {
        ModeDefinition mode = findMode(ability.id()).orElse(null);
        if (mode == null) {
            return Optional.empty();
        }

        ResourceLocation blockingMode = mode.blockedByModes().stream()
                .filter(data::isModeActive)
                .findFirst()
                .orElse(null);
        if (blockingMode != null) {
            return Optional.of(Component.translatable(
                    "message.xlib.mode_blocked_by_mode",
                    displayName(blockingMode)
            ));
        }

        ResourceLocation cycleGroupId = mode.cycleGroupId();
        if (cycleGroupId != null
                && data.hasModeBeenUsedInCycle(cycleGroupId, mode.abilityId())
                && !mode.resetCycleGroupsOnActivate().contains(cycleGroupId)) {
            return Optional.of(Component.translatable("message.xlib.mode_cycle_spent", displayName(mode.abilityId())));
        }

        if (!mode.transformsFrom().isEmpty() && mode.transformsFrom().stream().noneMatch(data::isModeActive)) {
            return Optional.of(Component.translatable(
                    "message.xlib.mode_requires_parent",
                    joinedDisplayNames(mode.transformsFrom())
            ));
        }

        return Optional.empty();
    }

    public static AbilityData prepareActivation(ServerPlayer player, AbilityData data, AbilityDefinition ability) {
        ModeDefinition mode = findMode(ability.id()).orElse(null);
        if (mode == null) {
            return data;
        }

        AbilityData updatedData = data;
        for (ResourceLocation cycleGroupId : mode.resetCycleGroupsOnActivate()) {
            updatedData = updatedData.clearModeCycleGroup(cycleGroupId);
        }
        for (ResourceLocation activeModeId : Set.copyOf(updatedData.activeModes())) {
            if (activeModeId.equals(mode.abilityId())) {
                continue;
            }

            ModeDefinition activeMode = findMode(activeModeId).orElse(null);
            boolean endsForTransform = mode.transformsFrom().contains(activeModeId);
            boolean endsForExclusivity = mode.exclusiveModes().contains(activeModeId)
                    || (activeMode != null && activeMode.exclusiveModes().contains(mode.abilityId()));
            if (!endsForTransform && !endsForExclusivity) {
                continue;
            }

            AbilityDefinition activeAbility = AbilityApi.findAbility(activeModeId).orElse(null);
            if (activeAbility == null) {
                updatedData = updatedData.withMode(activeModeId, false);
                continue;
            }

            AbilityUseResult endResult = AbilityRuntime.endAbility(
                    player,
                    updatedData,
                    activeAbility,
                    endsForTransform ? AbilityEndReason.REPLACED_BY_TRANSFORM : AbilityEndReason.REPLACED_BY_EXCLUSIVE
            );
            updatedData = endResult.data();
        }
        return updatedData;
    }

    public static AbilityData recordActivation(AbilityData data, AbilityDefinition ability) {
        ModeDefinition mode = findMode(ability.id()).orElse(null);
        if (mode == null || mode.cycleGroupId() == null) {
            return data;
        }
        return data.withModeUsedInCycle(mode.cycleGroupId(), mode.abilityId());
    }

    public static AbilityData resetCycleGroup(AbilityData data, ResourceLocation cycleGroupId) {
        return data.clearModeCycleGroup(cycleGroupId);
    }

    public static boolean resetCycleGroup(Player player, ResourceLocation cycleGroupId) {
        AbilityData currentData = ModAttachments.get(player);
        AbilityData updatedData = resetCycleGroup(currentData, cycleGroupId);
        if (updatedData.equals(currentData)) {
            return false;
        }
        ModAttachments.set(player, updatedData);
        return true;
    }

    public static List<ResourceLocation> cycleHistory(AbilityData data, ResourceLocation cycleGroupId) {
        return data.modeCycleHistoryFor(cycleGroupId);
    }

    public static double cooldownTickRateMultiplier(AbilityData data) {
        double multiplier = 1.0D;
        for (ModeDefinition mode : activeModes(data)) {
            multiplier *= mode.cooldownTickRateMultiplier();
        }
        return multiplier;
    }

    public static void postModeStarted(ServerPlayer player, AbilityDefinition ability, AbilityData previousData, AbilityData updatedData) {
        if (!ability.toggleAbility() || !updatedData.isModeActive(ability.id())) {
            return;
        }
        NeoForge.EVENT_BUS.post(new XLibModeEvent.Started(player, ability, previousData, updatedData));
    }

    public static void postModeEnded(
            ServerPlayer player,
            AbilityDefinition ability,
            AbilityData previousData,
            AbilityData updatedData,
            AbilityEndReason reason
    ) {
        if (!ability.toggleAbility()) {
            return;
        }

        XLibModeEvent.Ended event = switch (reason) {
            case DURATION_EXPIRED -> new XLibModeEvent.DurationExpired(player, ability, previousData, updatedData);
            case REQUIREMENT_INVALIDATED -> new XLibModeEvent.RequirementInvalidated(player, ability, previousData, updatedData);
            case FORCE_ENDED -> new XLibModeEvent.ForceEnded(player, ability, previousData, updatedData);
            case REPLACED_BY_TRANSFORM -> new XLibModeEvent.ReplacedByTransform(player, ability, previousData, updatedData);
            case REPLACED_BY_EXCLUSIVE -> new XLibModeEvent.ReplacedByExclusive(player, ability, previousData, updatedData);
            case PLAYER_TOGGLED -> new XLibModeEvent.Ended(player, ability, previousData, updatedData, reason);
        };
        NeoForge.EVENT_BUS.post(event);
    }

    public static List<ModeDefinition> activeModes(AbilityData data) {
        return data.activeModes().stream()
                .map(MODES::get)
                .filter(Objects::nonNull)
                .sorted(ACTIVE_MODE_ORDER)
                .toList();
    }

    private static Component displayName(ResourceLocation abilityId) {
        return AbilityApi.findAbility(abilityId)
                .map(AbilityDefinition::displayName)
                .orElse(Component.literal(abilityId.toString()));
    }

    private static Component joinedDisplayNames(Collection<ResourceLocation> abilityIds) {
        String joined = abilityIds.stream()
                .map(ModeApi::displayName)
                .map(Component::getString)
                .collect(Collectors.joining(", "));
        return Component.literal(joined);
    }
}
