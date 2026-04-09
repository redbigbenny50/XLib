package com.whatxe.xlib.ability;

import com.whatxe.xlib.XLib;
import com.whatxe.xlib.XLibRegistryGuard;
import com.whatxe.xlib.attachment.ModAttachments;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public final class StatePolicyApi {
    public static final ResourceLocation COMMAND_SOURCE =
            ResourceLocation.fromNamespaceAndPath(XLib.MODID, "command_state_policy");

    private static final Map<ResourceLocation, StatePolicyDefinition> STATE_POLICIES = new LinkedHashMap<>();

    private StatePolicyApi() {}

    public static void bootstrap() {}

    public static StatePolicyDefinition registerStatePolicy(StatePolicyDefinition policy) {
        XLibRegistryGuard.ensureMutable("state_policies");
        StatePolicyDefinition previous = STATE_POLICIES.putIfAbsent(policy.id(), policy);
        if (previous != null) {
            throw new IllegalStateException("Duplicate state policy registration: " + policy.id());
        }
        return policy;
    }

    public static Optional<StatePolicyDefinition> unregisterStatePolicy(ResourceLocation policyId) {
        XLibRegistryGuard.ensureMutable("state_policies");
        return Optional.ofNullable(STATE_POLICIES.remove(policyId));
    }

    public static Optional<StatePolicyDefinition> findStatePolicy(ResourceLocation policyId) {
        return Optional.ofNullable(STATE_POLICIES.get(policyId));
    }

    public static Collection<StatePolicyDefinition> allStatePolicies() {
        return List.copyOf(STATE_POLICIES.values());
    }

    public static boolean hasActivePolicy(Player player, ResourceLocation policyId) {
        return hasActivePolicy(ModAttachments.get(player), policyId);
    }

    public static boolean hasActivePolicy(AbilityData data, ResourceLocation policyId) {
        return data.hasStatePolicy(policyId) && findStatePolicy(policyId).isPresent();
    }

    public static List<StatePolicyDefinition> activePolicies(AbilityData data) {
        return data.activeStatePolicies().stream()
                .map(STATE_POLICIES::get)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    public static Set<ResourceLocation> policySources(Player player, ResourceLocation policyId) {
        return Set.copyOf(ModAttachments.get(player).statePolicySourcesFor(policyId));
    }

    public static double cooldownTickRateMultiplier(AbilityData data) {
        double multiplier = 1.0D;
        for (StatePolicyDefinition policy : activePolicies(data)) {
            multiplier *= policy.cooldownTickRateMultiplier();
        }
        return multiplier;
    }

    public static StateControlStatus controlStatus(AbilityData data, AbilityDefinition ability) {
        Set<ResourceLocation> lockingPolicies = new LinkedHashSet<>();
        Set<ResourceLocation> silencingPolicies = new LinkedHashSet<>();
        Set<ResourceLocation> suppressingPolicies = new LinkedHashSet<>();
        for (StatePolicyDefinition policy : activePolicies(data)) {
            if (matchesAny(policy.lockedAbilities(), ability)) {
                lockingPolicies.add(policy.id());
            }
            if (matchesAny(policy.silencedAbilities(), ability)) {
                silencingPolicies.add(policy.id());
            }
            if (matchesAny(policy.suppressedAbilities(), ability)) {
                suppressingPolicies.add(policy.id());
            }
        }
        return new StateControlStatus(lockingPolicies, silencingPolicies, suppressingPolicies);
    }

    public static Set<ResourceLocation> affectedAbilities(AbilityData data) {
        Set<ResourceLocation> affected = new LinkedHashSet<>();
        for (StatePolicyDefinition policy : activePolicies(data)) {
            affected.addAll(resolveSelectors(policy.lockedAbilities()));
            affected.addAll(resolveSelectors(policy.silencedAbilities()));
            affected.addAll(resolveSelectors(policy.suppressedAbilities()));
        }
        return Set.copyOf(affected);
    }

    public static Set<ResourceLocation> lockedAbilities(AbilityData data) {
        return resolvePolicySelectors(activePolicies(data), StatePolicyDefinition::lockedAbilities);
    }

    public static Set<ResourceLocation> silencedAbilities(AbilityData data) {
        return resolvePolicySelectors(activePolicies(data), StatePolicyDefinition::silencedAbilities);
    }

    public static Set<ResourceLocation> suppressedAbilities(AbilityData data) {
        return resolvePolicySelectors(activePolicies(data), StatePolicyDefinition::suppressedAbilities);
    }

    public static void grant(Player player, ResourceLocation policyId) {
        grant(player, policyId, COMMAND_SOURCE);
    }

    public static void grant(Player player, ResourceLocation policyId, ResourceLocation sourceId) {
        update(player, AbilityGrantApi.sanitize(AbilityApi.sanitizeData(
                ModAttachments.get(player).withStatePolicySource(policyId, sourceId, true)
        )));
    }

    public static void grant(Player player, Collection<ResourceLocation> policyIds, ResourceLocation sourceId) {
        AbilityData data = ModAttachments.get(player);
        for (ResourceLocation policyId : new LinkedHashSet<>(policyIds)) {
            data = data.withStatePolicySource(policyId, sourceId, true);
        }
        update(player, AbilityGrantApi.sanitize(AbilityApi.sanitizeData(data)));
    }

    public static void revoke(Player player, ResourceLocation policyId) {
        revoke(player, policyId, COMMAND_SOURCE);
    }

    public static void revoke(Player player, ResourceLocation policyId, ResourceLocation sourceId) {
        update(player, AbilityGrantApi.sanitize(AbilityApi.sanitizeData(
                ModAttachments.get(player).withStatePolicySource(policyId, sourceId, false)
        )));
    }

    public static void clearPolicies(Player player) {
        update(player, AbilityGrantApi.sanitize(AbilityApi.sanitizeData(
                ModAttachments.get(player).clearStatePolicySources()
        )));
    }

    public static void syncSourcePolicies(Player player, ResourceLocation sourceId, Collection<ResourceLocation> policyIds) {
        AbilityData data = ModAttachments.get(player).withManagedGrantSource(sourceId, true);
        Set<ResourceLocation> desiredPolicies = new LinkedHashSet<>(policyIds);
        for (ResourceLocation policyId : Set.copyOf(data.activeStatePolicies())) {
            if (data.statePolicySourcesFor(policyId).contains(sourceId) && !desiredPolicies.contains(policyId)) {
                data = data.withStatePolicySource(policyId, sourceId, false);
            }
        }
        for (ResourceLocation policyId : desiredPolicies) {
            data = data.withStatePolicySource(policyId, sourceId, true);
        }
        update(player, AbilityGrantApi.sanitize(AbilityApi.sanitizeData(data)));
    }

    public static AbilityData revokeSourcePolicies(AbilityData data, ResourceLocation sourceId) {
        return data.clearStatePolicySource(sourceId);
    }

    private static boolean matchesAny(Collection<AbilitySelector> selectors, AbilityDefinition ability) {
        for (AbilitySelector selector : selectors) {
            if (selector.matches(ability)) {
                return true;
            }
        }
        return false;
    }

    private static Set<ResourceLocation> resolvePolicySelectors(
            Collection<StatePolicyDefinition> policies,
            java.util.function.Function<StatePolicyDefinition, Collection<AbilitySelector>> selectorGetter
    ) {
        Set<ResourceLocation> resolved = new LinkedHashSet<>();
        for (StatePolicyDefinition policy : policies) {
            resolved.addAll(resolveSelectors(selectorGetter.apply(policy)));
        }
        return Set.copyOf(resolved);
    }

    private static Set<ResourceLocation> resolveSelectors(Collection<AbilitySelector> selectors) {
        Set<ResourceLocation> resolved = new LinkedHashSet<>();
        for (AbilitySelector selector : selectors) {
            resolved.addAll(selector.resolveAbilityIds());
        }
        return Set.copyOf(resolved);
    }

    private static void update(Player player, AbilityData updatedData) {
        if (!updatedData.equals(ModAttachments.get(player))) {
            ModAttachments.set(player, updatedData);
        }
    }
}
