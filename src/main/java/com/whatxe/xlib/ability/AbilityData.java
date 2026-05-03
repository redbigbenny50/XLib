package com.whatxe.xlib.ability;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public record AbilityData(
        List<Optional<ResourceLocation>> slots,
        Map<ResourceLocation, Integer> cooldowns,
        Set<ResourceLocation> activeModes,
        Map<ResourceLocation, Integer> activeDurations,
        Map<ResourceLocation, Integer> detectorWindows,
        Map<ResourceLocation, Integer> comboWindows,
        List<Optional<ResourceLocation>> comboOverrides,
        List<Integer> comboOverrideDurations,
        Map<ResourceLocation, Integer> charges,
        Map<ResourceLocation, Integer> chargeRechargeTicks,
        Map<ResourceLocation, Integer> recoveryTickProgress,
        Map<ResourceLocation, Integer> resources,
        Map<ResourceLocation, Integer> resourceRegenDelays,
        Map<ResourceLocation, Integer> resourceDecayDelays,
        Map<ResourceLocation, Integer> resourcePartials,
        Map<ResourceLocation, Set<ResourceLocation>> abilityGrantSources,
        Map<ResourceLocation, Set<ResourceLocation>> passiveGrantSources,
        Map<ResourceLocation, Set<ResourceLocation>> grantedItemSources,
        Map<ResourceLocation, Set<ResourceLocation>> recipePermissionSources,
        Map<ResourceLocation, Set<ResourceLocation>> abilityActivationBlockSources,
        Map<ResourceLocation, Set<ResourceLocation>> statePolicySources,
        Map<ResourceLocation, Set<ResourceLocation>> stateFlagSources,
        Map<ResourceLocation, Set<ResourceLocation>> grantBundleSources,
        Map<ResourceLocation, Set<ResourceLocation>> artifactUnlockSources,
        Set<ResourceLocation> managedGrantSources,
        boolean abilityAccessRestricted,
        Map<ResourceLocation, List<ResourceLocation>> modeCycleHistory,
        Map<ResourceLocation, List<Optional<ResourceLocation>>> modeLoadouts,
        AbilityContainerState containerState
) {
    public static final int SLOT_COUNT = 9;

    private static final Codec<Optional<ResourceLocation>> OPTIONAL_RESOURCE_CODEC = Codec.STRING.xmap(
            value -> value.isBlank() ? Optional.empty() : Optional.of(ResourceLocation.parse(value)),
            value -> value.map(ResourceLocation::toString).orElse("")
    );
    private static final Codec<Set<ResourceLocation>> RESOURCE_SET_CODEC = ResourceLocation.CODEC.listOf()
            .xmap(Set::copyOf, List::copyOf);
    private static final Codec<List<ResourceLocation>> RESOURCE_LIST_CODEC = ResourceLocation.CODEC.listOf()
            .xmap(List::copyOf, List::copyOf);
    private static final Codec<Map<ResourceLocation, Set<ResourceLocation>>> RESOURCE_SET_MAP_CODEC =
            Codec.unboundedMap(ResourceLocation.CODEC, RESOURCE_SET_CODEC);
    private static final Codec<Map<ResourceLocation, List<ResourceLocation>>> RESOURCE_LIST_MAP_CODEC =
            Codec.unboundedMap(ResourceLocation.CODEC, RESOURCE_LIST_CODEC);
    private static final ComboCodecState EMPTY_COMBO_STATE = new ComboCodecState(Map.of(), List.of(), List.of());
    private static final GrantCodecState EMPTY_GRANT_STATE =
            new GrantCodecState(Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Set.of(), false);
    private static final LayoutCodecState EMPTY_LAYOUT_STATE = new LayoutCodecState(Map.of(), Map.of(), AbilityContainerState.empty());

    public static final Codec<AbilityData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            OPTIONAL_RESOURCE_CODEC.listOf().optionalFieldOf("slots", List.of()).forGetter(AbilityData::slots),
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT).optionalFieldOf("cooldowns", Map.of()).forGetter(AbilityData::cooldowns),
            ResourceLocation.CODEC.listOf()
                    .xmap(Set::copyOf, List::copyOf)
                    .optionalFieldOf("active_modes", Set.of())
                    .forGetter(AbilityData::activeModes),
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT).optionalFieldOf("active_durations", Map.of()).forGetter(AbilityData::activeDurations),
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT).optionalFieldOf("detector_windows", Map.of()).forGetter(AbilityData::detectorWindows),
            ComboCodecState.CODEC.optionalFieldOf("combo_state", EMPTY_COMBO_STATE).forGetter(data -> new ComboCodecState(
                    data.comboWindows(),
                    data.comboOverrides(),
                    data.comboOverrideDurations()
            )),
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT).optionalFieldOf("charges", Map.of()).forGetter(AbilityData::charges),
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT).optionalFieldOf("charge_recharge_ticks", Map.of()).forGetter(AbilityData::chargeRechargeTicks),
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT).optionalFieldOf("recovery_tick_progress", Map.of()).forGetter(AbilityData::recoveryTickProgress),
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT).optionalFieldOf("resources", Map.of()).forGetter(AbilityData::resources),
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT).optionalFieldOf("resource_regen_delays", Map.of()).forGetter(AbilityData::resourceRegenDelays),
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT).optionalFieldOf("resource_decay_delays", Map.of()).forGetter(AbilityData::resourceDecayDelays),
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT).optionalFieldOf("resource_partials", Map.of()).forGetter(AbilityData::resourcePartials),
            GrantCodecState.CODEC.optionalFieldOf("grant_state", EMPTY_GRANT_STATE).forGetter(data -> new GrantCodecState(
                    data.abilityGrantSources(),
                    data.passiveGrantSources(),
                    data.grantedItemSources(),
                    data.recipePermissionSources(),
                    data.abilityActivationBlockSources(),
                    data.statePolicySources(),
                    data.stateFlagSources(),
                    data.grantBundleSources(),
                    data.artifactUnlockSources(),
                    data.managedGrantSources(),
                    data.abilityAccessRestricted()
            )),
            LayoutCodecState.CODEC.optionalFieldOf("layout_state", EMPTY_LAYOUT_STATE)
                    .forGetter(data -> new LayoutCodecState(data.modeCycleHistory(), data.modeLoadouts(), data.containerState()))
    ).apply(instance, (
            slots,
            cooldowns,
            activeModes,
            activeDurations,
            detectorWindows,
            comboState,
            charges,
            chargeRechargeTicks,
            recoveryTickProgress,
            resources,
            resourceRegenDelays,
            resourceDecayDelays,
            resourcePartials,
            grantState,
            layoutState
    ) -> new AbilityData(
            slots,
            cooldowns,
            activeModes,
            activeDurations,
            detectorWindows,
            comboState.comboWindows(),
            comboState.comboOverrides(),
            comboState.comboOverrideDurations(),
            charges,
            chargeRechargeTicks,
            recoveryTickProgress,
            resources,
            resourceRegenDelays,
            resourceDecayDelays,
            resourcePartials,
            grantState.abilityGrantSources(),
            grantState.passiveGrantSources(),
            grantState.grantedItemSources(),
            grantState.recipePermissionSources(),
            grantState.abilityActivationBlockSources(),
            grantState.statePolicySources(),
            grantState.stateFlagSources(),
            grantState.grantBundleSources(),
            grantState.artifactUnlockSources(),
            grantState.managedGrantSources(),
            grantState.abilityAccessRestricted(),
            layoutState.modeCycleHistory(),
            layoutState.modeLoadouts(),
            layoutState.containerState()
    )));

    public static final StreamCodec<RegistryFriendlyByteBuf, AbilityData> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);

    private record ComboCodecState(
            Map<ResourceLocation, Integer> comboWindows,
            List<Optional<ResourceLocation>> comboOverrides,
            List<Integer> comboOverrideDurations
    ) {
        private static final Codec<ComboCodecState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT).optionalFieldOf("combo_windows", Map.of()).forGetter(ComboCodecState::comboWindows),
                OPTIONAL_RESOURCE_CODEC.listOf().optionalFieldOf("combo_overrides", List.of()).forGetter(ComboCodecState::comboOverrides),
                Codec.INT.listOf().optionalFieldOf("combo_override_durations", List.of()).forGetter(ComboCodecState::comboOverrideDurations)
        ).apply(instance, ComboCodecState::new));
    }

    private record GrantCodecState(
            Map<ResourceLocation, Set<ResourceLocation>> abilityGrantSources,
            Map<ResourceLocation, Set<ResourceLocation>> passiveGrantSources,
            Map<ResourceLocation, Set<ResourceLocation>> grantedItemSources,
            Map<ResourceLocation, Set<ResourceLocation>> recipePermissionSources,
            Map<ResourceLocation, Set<ResourceLocation>> abilityActivationBlockSources,
            Map<ResourceLocation, Set<ResourceLocation>> statePolicySources,
            Map<ResourceLocation, Set<ResourceLocation>> stateFlagSources,
            Map<ResourceLocation, Set<ResourceLocation>> grantBundleSources,
            Map<ResourceLocation, Set<ResourceLocation>> artifactUnlockSources,
            Set<ResourceLocation> managedGrantSources,
            boolean abilityAccessRestricted
    ) {
        private static final Codec<GrantCodecState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                RESOURCE_SET_MAP_CODEC.optionalFieldOf("ability_grant_sources", Map.of()).forGetter(GrantCodecState::abilityGrantSources),
                RESOURCE_SET_MAP_CODEC.optionalFieldOf("passive_grant_sources", Map.of()).forGetter(GrantCodecState::passiveGrantSources),
                RESOURCE_SET_MAP_CODEC.optionalFieldOf("granted_item_sources", Map.of()).forGetter(GrantCodecState::grantedItemSources),
                RESOURCE_SET_MAP_CODEC.optionalFieldOf("recipe_permission_sources", Map.of()).forGetter(GrantCodecState::recipePermissionSources),
                RESOURCE_SET_MAP_CODEC.optionalFieldOf("ability_activation_block_sources", Map.of()).forGetter(GrantCodecState::abilityActivationBlockSources),
                RESOURCE_SET_MAP_CODEC.optionalFieldOf("state_policy_sources", Map.of()).forGetter(GrantCodecState::statePolicySources),
                RESOURCE_SET_MAP_CODEC.optionalFieldOf("state_flag_sources", Map.of()).forGetter(GrantCodecState::stateFlagSources),
                RESOURCE_SET_MAP_CODEC.optionalFieldOf("grant_bundle_sources", Map.of()).forGetter(GrantCodecState::grantBundleSources),
                RESOURCE_SET_MAP_CODEC.optionalFieldOf("artifact_unlock_sources", Map.of()).forGetter(GrantCodecState::artifactUnlockSources),
                RESOURCE_SET_CODEC.optionalFieldOf("managed_grant_sources", Set.of()).forGetter(GrantCodecState::managedGrantSources),
                Codec.BOOL.optionalFieldOf("ability_access_restricted", false).forGetter(GrantCodecState::abilityAccessRestricted)
        ).apply(instance, GrantCodecState::new));
    }

    private record LayoutCodecState(
            Map<ResourceLocation, List<ResourceLocation>> modeCycleHistory,
            Map<ResourceLocation, List<Optional<ResourceLocation>>> modeLoadouts,
            AbilityContainerState containerState
    ) {
        private static final Codec<LayoutCodecState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                RESOURCE_LIST_MAP_CODEC.optionalFieldOf("mode_cycle_history", Map.of()).forGetter(LayoutCodecState::modeCycleHistory),
                Codec.unboundedMap(ResourceLocation.CODEC, OPTIONAL_RESOURCE_CODEC.listOf())
                        .optionalFieldOf("mode_loadouts", Map.of())
                        .forGetter(LayoutCodecState::modeLoadouts),
                AbilityContainerState.CODEC.optionalFieldOf("container_state", AbilityContainerState.empty())
                        .forGetter(LayoutCodecState::containerState)
        ).apply(instance, LayoutCodecState::new));
    }

    public AbilityData {
        slots = normalizeSlots(slots);
        cooldowns = normalizePositiveMap(cooldowns);
        activeModes = Set.copyOf(activeModes);
        activeDurations = normalizePositiveMap(activeDurations);
        detectorWindows = normalizePositiveMap(detectorWindows);
        comboWindows = normalizePositiveMap(comboWindows);
        comboOverrides = normalizeSlots(comboOverrides);
        comboOverrideDurations = normalizeSlotIntegers(comboOverrideDurations);
        charges = normalizeNonNegativeMap(charges);
        chargeRechargeTicks = normalizePositiveMap(chargeRechargeTicks);
        recoveryTickProgress = normalizeProgressMap(recoveryTickProgress);
        resources = normalizeNonNegativeMap(resources);
        resourceRegenDelays = normalizePositiveMap(resourceRegenDelays);
        resourceDecayDelays = normalizePositiveMap(resourceDecayDelays);
        resourcePartials = normalizePartialMap(resourcePartials);
        abilityGrantSources = normalizeGrantSourceMap(abilityGrantSources);
        passiveGrantSources = normalizeGrantSourceMap(passiveGrantSources);
        grantedItemSources = normalizeGrantSourceMap(grantedItemSources);
        recipePermissionSources = normalizeGrantSourceMap(recipePermissionSources);
        abilityActivationBlockSources = normalizeGrantSourceMap(abilityActivationBlockSources);
        statePolicySources = normalizeGrantSourceMap(statePolicySources);
        stateFlagSources = normalizeGrantSourceMap(stateFlagSources);
        grantBundleSources = normalizeGrantSourceMap(grantBundleSources);
        artifactUnlockSources = normalizeGrantSourceMap(artifactUnlockSources);
        managedGrantSources = Set.copyOf(managedGrantSources);
        modeCycleHistory = normalizeModeCycleHistory(modeCycleHistory);
        modeLoadouts = normalizeModeLoadouts(modeLoadouts);
        containerState = collapseToPrimaryBar(containerState == null ? AbilityContainerState.empty() : containerState, slots, comboOverrides, comboOverrideDurations, modeLoadouts);
        slots = containerState.legacyPrimarySlots(SLOT_COUNT);
        comboOverrides = containerState.legacyPrimaryComboOverrides(SLOT_COUNT);
        comboOverrideDurations = containerState.legacyPrimaryComboDurations(SLOT_COUNT);
        modeLoadouts = normalizeModeLoadouts(containerState.legacyPrimaryModeLoadouts(SLOT_COUNT));
    }

    public static AbilityData empty() {
        return new AbilityData(
                List.of(),
                Map.of(),
                Set.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                List.of(),
                List.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Set.of(),
                false,
                Map.of(),
                Map.of(),
                AbilityContainerState.empty()
        );
    }

    public Optional<ResourceLocation> abilityInSlot(int slot) {
        return abilityInSlot(AbilitySlotReference.primary(slot));
    }

    public Optional<ResourceLocation> abilityInSlot(AbilitySlotReference slotReference) {
        return this.containerState.abilityInSlot(slotReference);
    }

    public Optional<ResourceLocation> modeAbilityInSlot(ResourceLocation modeId, int slot) {
        return modeAbilityInSlot(modeId, AbilitySlotReference.primary(slot));
    }

    public Optional<ResourceLocation> modeAbilityInSlot(ResourceLocation modeId, AbilitySlotReference slotReference) {
        return this.containerState.modeAbilityInSlot(modeId, slotReference);
    }

    public int cooldownFor(ResourceLocation abilityId) {
        return this.cooldowns.getOrDefault(abilityId, 0);
    }

    public boolean isModeActive(ResourceLocation abilityId) {
        return this.activeModes.contains(abilityId);
    }

    public int activeDurationFor(ResourceLocation abilityId) {
        return this.activeDurations.getOrDefault(abilityId, 0);
    }

    public int detectorWindowFor(ResourceLocation detectorId) {
        return this.detectorWindows.getOrDefault(detectorId, 0);
    }

    public int comboWindowFor(ResourceLocation abilityId) {
        return this.comboWindows.getOrDefault(abilityId, 0);
    }

    public Optional<ResourceLocation> comboOverrideInSlot(int slot) {
        return comboOverrideInSlot(AbilitySlotReference.primary(slot));
    }

    public Optional<ResourceLocation> comboOverrideInSlot(AbilitySlotReference slotReference) {
        return this.containerState.comboOverrideInSlot(slotReference);
    }

    public int comboOverrideDurationForSlot(int slot) {
        return comboOverrideDurationForSlot(AbilitySlotReference.primary(slot));
    }

    public int comboOverrideDurationForSlot(AbilitySlotReference slotReference) {
        return this.containerState.comboOverrideDurationForSlot(slotReference);
    }

    public int activeContainerPage(ResourceLocation containerId) {
        return this.containerState.activePage(containerId);
    }

    public int containerPageCount(ResourceLocation containerId) {
        return this.containerState.pageCount(containerId);
    }

    public int containerSlotCount(ResourceLocation containerId, int pageIndex) {
        return this.containerState.slotCount(containerId, pageIndex);
    }

    public int chargeCountFor(ResourceLocation abilityId, int defaultValue) {
        return this.charges.getOrDefault(abilityId, defaultValue);
    }

    public int chargeRechargeFor(ResourceLocation abilityId) {
        return this.chargeRechargeTicks.getOrDefault(abilityId, 0);
    }

    public int recoveryTickProgressFor(ResourceLocation abilityId) {
        return this.recoveryTickProgress.getOrDefault(abilityId, 0);
    }

    public int resourceAmount(ResourceLocation resourceId) {
        return this.resources.getOrDefault(resourceId, 0);
    }

    public int resourcePartialAmount(ResourceLocation resourceId) {
        return this.resourcePartials.getOrDefault(resourceId, 0);
    }

    public double resourceAmountExact(ResourceLocation resourceId) {
        return resourceAmount(resourceId) + (resourcePartialAmount(resourceId) / 1000.0D);
    }

    public int resourceRegenDelay(ResourceLocation resourceId) {
        return this.resourceRegenDelays.getOrDefault(resourceId, 0);
    }

    public int resourceDecayDelay(ResourceLocation resourceId) {
        return this.resourceDecayDelays.getOrDefault(resourceId, 0);
    }

    public Set<ResourceLocation> grantedAbilities() {
        return this.abilityGrantSources.keySet();
    }

    public Set<ResourceLocation> abilityGrantSourcesFor(ResourceLocation abilityId) {
        return this.abilityGrantSources.getOrDefault(abilityId, Set.of());
    }

    public boolean canUseAbility(ResourceLocation abilityId) {
        return !this.abilityAccessRestricted || this.abilityGrantSources.containsKey(abilityId);
    }

    public Set<ResourceLocation> grantedPassives() {
        return this.passiveGrantSources.keySet();
    }

    public Set<ResourceLocation> passiveGrantSourcesFor(ResourceLocation passiveId) {
        return this.passiveGrantSources.getOrDefault(passiveId, Set.of());
    }

    public boolean hasPassive(ResourceLocation passiveId) {
        return this.passiveGrantSources.containsKey(passiveId);
    }

    public Set<ResourceLocation> grantedItems() {
        return this.grantedItemSources.keySet();
    }

    public Set<ResourceLocation> grantedItemSourcesFor(ResourceLocation grantedItemId) {
        return this.grantedItemSources.getOrDefault(grantedItemId, Set.of());
    }

    public boolean hasGrantedItem(ResourceLocation grantedItemId) {
        return this.grantedItemSources.containsKey(grantedItemId);
    }

    public Set<ResourceLocation> recipePermissions() {
        return this.recipePermissionSources.keySet();
    }

    public Set<ResourceLocation> recipePermissionSourcesFor(ResourceLocation recipeId) {
        return this.recipePermissionSources.getOrDefault(recipeId, Set.of());
    }

    public boolean hasRecipePermission(ResourceLocation recipeId) {
        return this.recipePermissionSources.containsKey(recipeId);
    }

    public Set<ResourceLocation> activationBlockSourcesFor(ResourceLocation abilityId) {
        return this.abilityActivationBlockSources.getOrDefault(abilityId, Set.of());
    }

    public boolean isAbilityActivationBlocked(ResourceLocation abilityId) {
        return this.abilityActivationBlockSources.containsKey(abilityId);
    }

    public Set<ResourceLocation> activeStatePolicies() {
        return this.statePolicySources.keySet();
    }

    public Set<ResourceLocation> statePolicySourcesFor(ResourceLocation statePolicyId) {
        return this.statePolicySources.getOrDefault(statePolicyId, Set.of());
    }

    public boolean hasStatePolicy(ResourceLocation statePolicyId) {
        return this.statePolicySources.containsKey(statePolicyId);
    }

    public Set<ResourceLocation> activeStateFlags() {
        return this.stateFlagSources.keySet();
    }

    public Set<ResourceLocation> stateFlagSourcesFor(ResourceLocation stateFlagId) {
        return this.stateFlagSources.getOrDefault(stateFlagId, Set.of());
    }

    public boolean hasStateFlag(ResourceLocation stateFlagId) {
        return this.stateFlagSources.containsKey(stateFlagId);
    }

    public Set<ResourceLocation> activeGrantBundles() {
        return this.grantBundleSources.keySet();
    }

    public Set<ResourceLocation> unlockedArtifacts() {
        return this.artifactUnlockSources.keySet();
    }

    public Set<ResourceLocation> grantBundleSourcesFor(ResourceLocation bundleId) {
        return this.grantBundleSources.getOrDefault(bundleId, Set.of());
    }

    public boolean hasGrantBundle(ResourceLocation bundleId) {
        return this.grantBundleSources.containsKey(bundleId);
    }

    public Set<ResourceLocation> artifactUnlockSourcesFor(ResourceLocation artifactId) {
        return this.artifactUnlockSources.getOrDefault(artifactId, Set.of());
    }

    public boolean hasUnlockedArtifact(ResourceLocation artifactId) {
        return this.artifactUnlockSources.containsKey(artifactId);
    }

    public Set<ResourceLocation> activeDetectors() {
        return this.detectorWindows.keySet();
    }

    public List<ResourceLocation> modeCycleHistoryFor(ResourceLocation cycleGroupId) {
        return this.modeCycleHistory.getOrDefault(cycleGroupId, List.of());
    }

    public boolean hasModeBeenUsedInCycle(ResourceLocation cycleGroupId, ResourceLocation modeId) {
        return modeCycleHistoryFor(cycleGroupId).contains(modeId);
    }

    public AbilityData withAbilityInSlot(int slot, @Nullable ResourceLocation abilityId) {
        return withAbilityInSlot(AbilitySlotReference.primary(slot), abilityId);
    }

    public AbilityData withAbilityInSlot(AbilitySlotReference slotReference, @Nullable ResourceLocation abilityId) {
        if (!isValidSlotReference(slotReference)) {
            return this;
        }

        AbilityContainerState updatedContainerState = this.containerState.withAbilityInSlot(slotReference, abilityId);
        return copyWithContainerState(updatedContainerState);
    }

    public AbilityData withModeAbilityInSlot(ResourceLocation modeId, int slot, @Nullable ResourceLocation abilityId) {
        return withModeAbilityInSlot(modeId, AbilitySlotReference.primary(slot), abilityId);
    }

    public AbilityData withModeAbilityInSlot(ResourceLocation modeId, AbilitySlotReference slotReference, @Nullable ResourceLocation abilityId) {
        if (!isValidSlotReference(slotReference)) {
            return this;
        }

        AbilityContainerState updatedContainerState = this.containerState.withModeAbilityInSlot(modeId, slotReference, abilityId);
        return copyWithContainerState(updatedContainerState);
    }

    public AbilityData clearModeLoadout(ResourceLocation modeId) {
        AbilityContainerState updatedContainerState = this.containerState.clearModeLoadout(modeId);
        if (updatedContainerState.equals(this.containerState)) {
            return this;
        }
        return copyWithContainerState(updatedContainerState);
    }

    public AbilityData withCooldown(ResourceLocation abilityId, int ticks) {
        Map<ResourceLocation, Integer> updatedCooldowns = new LinkedHashMap<>(this.cooldowns);
        Map<ResourceLocation, Integer> updatedRecoveryProgress = new LinkedHashMap<>(this.recoveryTickProgress);
        if (ticks > 0) {
            updatedCooldowns.put(abilityId, ticks);
        } else {
            updatedCooldowns.remove(abilityId);
            updatedRecoveryProgress.remove(abilityId);
        }
        return copy(this.slots, updatedCooldowns, this.activeModes, this.activeDurations,
                this.comboWindows, this.comboOverrides, this.comboOverrideDurations, this.charges, this.chargeRechargeTicks,
                updatedRecoveryProgress, this.resources, this.resourceRegenDelays, this.resourceDecayDelays, this.resourcePartials, this.abilityGrantSources,
                this.passiveGrantSources, this.grantedItemSources, this.recipePermissionSources,
                this.abilityActivationBlockSources, this.statePolicySources,
                this.managedGrantSources, this.abilityAccessRestricted, this.modeCycleHistory, this.modeLoadouts);
    }

    public AbilityData tickCooldowns() {
        return tickCooldowns(1.0D);
    }

    public AbilityData tickCooldowns(double tickRate) {
        if (this.cooldowns.isEmpty()) {
            return this;
        }

        Map<ResourceLocation, Integer> updatedCooldowns = new LinkedHashMap<>();
        Map<ResourceLocation, Integer> updatedRecoveryProgress = new LinkedHashMap<>(this.recoveryTickProgress);
        boolean changed = false;
        long tickUnits = Math.max(0L, Math.round(tickRate * 1000.0D));
        for (Map.Entry<ResourceLocation, Integer> entry : this.cooldowns.entrySet()) {
            long progressUnits = updatedRecoveryProgress.getOrDefault(entry.getKey(), 0) + tickUnits;
            int decrement = (int) (progressUnits / 1000L);
            int nextValue = entry.getValue() - decrement;
            if (nextValue > 0) {
                updatedCooldowns.put(entry.getKey(), nextValue);
                int remainingProgress = (int) (progressUnits % 1000L);
                if (remainingProgress > 0) {
                    updatedRecoveryProgress.put(entry.getKey(), remainingProgress);
                } else {
                    updatedRecoveryProgress.remove(entry.getKey());
                }
            } else {
                updatedRecoveryProgress.remove(entry.getKey());
            }
            if (nextValue != entry.getValue()
                    || updatedRecoveryProgress.getOrDefault(entry.getKey(), 0) != this.recoveryTickProgress.getOrDefault(entry.getKey(), 0)) {
                changed = true;
            }
        }

        return changed
                ? copy(this.slots, updatedCooldowns, this.activeModes, this.activeDurations,
                        this.comboWindows, this.comboOverrides, this.comboOverrideDurations, this.charges, this.chargeRechargeTicks,
                        updatedRecoveryProgress, this.resources, this.resourceRegenDelays, this.resourceDecayDelays, this.resourcePartials, this.abilityGrantSources,
                        this.passiveGrantSources, this.grantedItemSources, this.recipePermissionSources,
                        this.abilityActivationBlockSources, this.statePolicySources,
                        this.managedGrantSources, this.abilityAccessRestricted, this.modeCycleHistory, this.modeLoadouts)
                : this;
    }

    public AbilityData withMode(ResourceLocation abilityId, boolean active) {
        Set<ResourceLocation> updatedModes = new LinkedHashSet<>(this.activeModes);
        Map<ResourceLocation, Integer> updatedDurations = new LinkedHashMap<>(this.activeDurations);
        if (active) {
            updatedModes.add(abilityId);
        } else {
            updatedModes.remove(abilityId);
            updatedDurations.remove(abilityId);
        }
        return copy(this.slots, this.cooldowns, updatedModes, updatedDurations,
                this.comboWindows, this.comboOverrides, this.comboOverrideDurations, this.charges, this.chargeRechargeTicks,
                this.resources, this.resourceRegenDelays, this.resourceDecayDelays, this.abilityGrantSources,
                this.passiveGrantSources, this.grantedItemSources, this.recipePermissionSources,
                this.abilityActivationBlockSources, this.statePolicySources,
                this.managedGrantSources, this.abilityAccessRestricted, this.modeLoadouts);
    }

    public AbilityData withActiveDuration(ResourceLocation abilityId, int ticks) {
        Map<ResourceLocation, Integer> updatedDurations = new LinkedHashMap<>(this.activeDurations);
        if (ticks > 0) {
            updatedDurations.put(abilityId, ticks);
        } else {
            updatedDurations.remove(abilityId);
        }
        return copy(this.slots, this.cooldowns, this.activeModes, updatedDurations,
                this.comboWindows, this.comboOverrides, this.comboOverrideDurations, this.charges, this.chargeRechargeTicks,
                this.resources, this.resourceRegenDelays, this.resourceDecayDelays, this.abilityGrantSources,
                this.passiveGrantSources, this.grantedItemSources, this.recipePermissionSources,
                this.abilityActivationBlockSources, this.statePolicySources,
                this.managedGrantSources, this.abilityAccessRestricted, this.modeLoadouts);
    }

    public AbilityData withDetectorWindow(ResourceLocation detectorId, int ticks) {
        Map<ResourceLocation, Integer> updatedWindows = new LinkedHashMap<>(this.detectorWindows);
        if (ticks > 0) {
            updatedWindows.put(detectorId, ticks);
        } else {
            updatedWindows.remove(detectorId);
        }
        return copyWithDetectors(this.slots, this.cooldowns, this.activeModes, this.activeDurations,
                updatedWindows, this.comboWindows, this.comboOverrides, this.comboOverrideDurations, this.charges, this.chargeRechargeTicks,
                this.resources, this.resourceRegenDelays, this.resourceDecayDelays, this.abilityGrantSources,
                this.passiveGrantSources, this.grantedItemSources, this.recipePermissionSources,
                this.abilityActivationBlockSources, this.statePolicySources,
                this.managedGrantSources, this.abilityAccessRestricted, this.modeLoadouts);
    }

    public AbilityData tickDetectorWindows() {
        if (this.detectorWindows.isEmpty()) {
            return this;
        }

        Map<ResourceLocation, Integer> updatedWindows = new LinkedHashMap<>();
        boolean changed = false;
        for (Map.Entry<ResourceLocation, Integer> entry : this.detectorWindows.entrySet()) {
            int nextValue = entry.getValue() - 1;
            if (nextValue > 0) {
                updatedWindows.put(entry.getKey(), nextValue);
            }
            if (nextValue != entry.getValue()) {
                changed = true;
            }
        }

        return changed
                ? copyWithDetectors(this.slots, this.cooldowns, this.activeModes, this.activeDurations,
                        updatedWindows, this.comboWindows, this.comboOverrides, this.comboOverrideDurations, this.charges, this.chargeRechargeTicks,
                        this.resources, this.resourceRegenDelays, this.resourceDecayDelays, this.abilityGrantSources,
                        this.passiveGrantSources, this.grantedItemSources, this.recipePermissionSources,
                        this.abilityActivationBlockSources, this.statePolicySources,
                        this.managedGrantSources, this.abilityAccessRestricted, this.modeLoadouts)
                : this;
    }

    public AbilityData withModeUsedInCycle(ResourceLocation cycleGroupId, ResourceLocation modeId) {
        Map<ResourceLocation, List<ResourceLocation>> updatedCycleHistory = new LinkedHashMap<>(this.modeCycleHistory);
        List<ResourceLocation> usedModes = new ArrayList<>(updatedCycleHistory.getOrDefault(cycleGroupId, List.of()));
        if (!usedModes.contains(modeId)) {
            usedModes.add(modeId);
        }
        if (usedModes.isEmpty()) {
            updatedCycleHistory.remove(cycleGroupId);
        } else {
            updatedCycleHistory.put(cycleGroupId, List.copyOf(usedModes));
        }
        return copy(this.slots, this.cooldowns, this.activeModes, this.activeDurations,
                this.comboWindows, this.comboOverrides, this.comboOverrideDurations, this.charges, this.chargeRechargeTicks,
                this.recoveryTickProgress, this.resources, this.resourceRegenDelays, this.resourceDecayDelays, this.resourcePartials, this.abilityGrantSources,
                this.passiveGrantSources, this.grantedItemSources, this.recipePermissionSources,
                this.abilityActivationBlockSources, this.statePolicySources,
                this.managedGrantSources, this.abilityAccessRestricted, updatedCycleHistory, this.modeLoadouts);
    }

    public AbilityData clearModeCycleGroup(ResourceLocation cycleGroupId) {
        if (!this.modeCycleHistory.containsKey(cycleGroupId)) {
            return this;
        }
        Map<ResourceLocation, List<ResourceLocation>> updatedCycleHistory = new LinkedHashMap<>(this.modeCycleHistory);
        updatedCycleHistory.remove(cycleGroupId);
        return copy(this.slots, this.cooldowns, this.activeModes, this.activeDurations,
                this.comboWindows, this.comboOverrides, this.comboOverrideDurations, this.charges, this.chargeRechargeTicks,
                this.recoveryTickProgress, this.resources, this.resourceRegenDelays, this.resourceDecayDelays, this.resourcePartials, this.abilityGrantSources,
                this.passiveGrantSources, this.grantedItemSources, this.recipePermissionSources,
                this.abilityActivationBlockSources, this.statePolicySources,
                this.managedGrantSources, this.abilityAccessRestricted, updatedCycleHistory, this.modeLoadouts);
    }

    public AbilityData withComboWindow(ResourceLocation abilityId, int ticks) {
        Map<ResourceLocation, Integer> updatedWindows = new LinkedHashMap<>(this.comboWindows);
        if (ticks > 0) {
            updatedWindows.put(abilityId, ticks);
        } else {
            updatedWindows.remove(abilityId);
        }
        return copy(this.slots, this.cooldowns, this.activeModes, this.activeDurations,
                updatedWindows, this.comboOverrides, this.comboOverrideDurations, this.charges, this.chargeRechargeTicks,
                this.resources, this.resourceRegenDelays, this.resourceDecayDelays, this.abilityGrantSources,
                this.passiveGrantSources, this.grantedItemSources, this.recipePermissionSources,
                this.abilityActivationBlockSources, this.statePolicySources,
                this.managedGrantSources, this.abilityAccessRestricted, this.modeLoadouts);
    }

    public AbilityData withComboOverride(int slot, @Nullable ResourceLocation abilityId, int durationTicks) {
        return withComboOverride(AbilitySlotReference.primary(slot), abilityId, durationTicks);
    }

    public AbilityData withComboOverride(AbilitySlotReference slotReference, @Nullable ResourceLocation abilityId, int durationTicks) {
        if (!isValidSlotReference(slotReference)) {
            return this;
        }

        AbilityContainerState updatedContainerState = this.containerState.withComboOverride(slotReference, abilityId, durationTicks);
        return copyWithContainerState(updatedContainerState);
    }

    public AbilityData withContainerActivePage(ResourceLocation containerId, int pageIndex) {
        AbilityContainerState updatedContainerState = this.containerState.withActivePage(containerId, pageIndex);
        if (updatedContainerState.equals(this.containerState)) {
            return this;
        }
        return copyWithContainerState(updatedContainerState);
    }

    public AbilityData withChargeCount(ResourceLocation abilityId, int count) {
        Map<ResourceLocation, Integer> updatedCharges = new LinkedHashMap<>(this.charges);
        if (count >= 0) {
            updatedCharges.put(abilityId, count);
        } else {
            updatedCharges.remove(abilityId);
        }
        return copy(this.slots, this.cooldowns, this.activeModes, this.activeDurations,
                this.comboWindows, this.comboOverrides, this.comboOverrideDurations, updatedCharges, this.chargeRechargeTicks,
                this.resources, this.resourceRegenDelays, this.resourceDecayDelays, this.abilityGrantSources,
                this.passiveGrantSources, this.grantedItemSources, this.recipePermissionSources,
                this.abilityActivationBlockSources, this.statePolicySources,
                this.managedGrantSources, this.abilityAccessRestricted, this.modeLoadouts);
    }

    public AbilityData withChargeRecharge(ResourceLocation abilityId, int ticks) {
        Map<ResourceLocation, Integer> updatedRechargeTicks = new LinkedHashMap<>(this.chargeRechargeTicks);
        Map<ResourceLocation, Integer> updatedRecoveryProgress = new LinkedHashMap<>(this.recoveryTickProgress);
        if (ticks > 0) {
            updatedRechargeTicks.put(abilityId, ticks);
        } else {
            updatedRechargeTicks.remove(abilityId);
            updatedRecoveryProgress.remove(abilityId);
        }
        return copy(this.slots, this.cooldowns, this.activeModes, this.activeDurations,
                this.comboWindows, this.comboOverrides, this.comboOverrideDurations, this.charges, updatedRechargeTicks,
                updatedRecoveryProgress, this.resources, this.resourceRegenDelays, this.resourceDecayDelays, this.resourcePartials, this.abilityGrantSources,
                this.passiveGrantSources, this.grantedItemSources, this.recipePermissionSources,
                this.abilityActivationBlockSources, this.statePolicySources,
                this.managedGrantSources, this.abilityAccessRestricted, this.modeCycleHistory, this.modeLoadouts);
    }

    public AbilityData withRecoveryTickProgress(ResourceLocation abilityId, int progressUnits) {
        Map<ResourceLocation, Integer> updatedRecoveryProgress = new LinkedHashMap<>(this.recoveryTickProgress);
        if (progressUnits > 0) {
            updatedRecoveryProgress.put(abilityId, Math.min(progressUnits, 999));
        } else {
            updatedRecoveryProgress.remove(abilityId);
        }
        return copy(this.slots, this.cooldowns, this.activeModes, this.activeDurations,
                this.comboWindows, this.comboOverrides, this.comboOverrideDurations, this.charges, this.chargeRechargeTicks,
                updatedRecoveryProgress, this.resources, this.resourceRegenDelays, this.resourceDecayDelays, this.resourcePartials, this.abilityGrantSources,
                this.passiveGrantSources, this.grantedItemSources, this.recipePermissionSources,
                this.abilityActivationBlockSources, this.statePolicySources,
                this.managedGrantSources, this.abilityAccessRestricted, this.modeCycleHistory, this.modeLoadouts);
    }

    public AbilityData withResourceAmount(ResourceLocation resourceId, int amount) {
        Map<ResourceLocation, Integer> updatedResources = new LinkedHashMap<>(this.resources);
        Map<ResourceLocation, Integer> updatedPartials = new LinkedHashMap<>(this.resourcePartials);
        if (amount >= 0) {
            updatedResources.put(resourceId, amount);
            updatedPartials.remove(resourceId);
        } else {
            updatedResources.remove(resourceId);
            updatedPartials.remove(resourceId);
        }
        return copy(this.slots, this.cooldowns, this.activeModes, this.activeDurations,
                this.comboWindows, this.comboOverrides, this.comboOverrideDurations, this.charges, this.chargeRechargeTicks,
                updatedResources, this.resourceRegenDelays, this.resourceDecayDelays, updatedPartials, this.abilityGrantSources,
                this.passiveGrantSources, this.grantedItemSources, this.recipePermissionSources,
                this.abilityActivationBlockSources, this.statePolicySources,
                this.managedGrantSources, this.abilityAccessRestricted, this.modeLoadouts);
    }

    public AbilityData withResourceAmountExact(ResourceLocation resourceId, double amount) {
        if (amount < 0.0D) {
            return withResourceAmount(resourceId, -1);
        }

        int wholeAmount = (int) Math.floor(amount + 1.0E-9D);
        int partialAmount = (int) Math.round((amount - wholeAmount) * 1000.0D);
        if (partialAmount >= 1000) {
            wholeAmount += 1;
            partialAmount -= 1000;
        }
        if (partialAmount < 0) {
            partialAmount = 0;
        }

        Map<ResourceLocation, Integer> updatedResources = new LinkedHashMap<>(this.resources);
        Map<ResourceLocation, Integer> updatedPartials = new LinkedHashMap<>(this.resourcePartials);
        updatedResources.put(resourceId, wholeAmount);
        if (partialAmount > 0) {
            updatedPartials.put(resourceId, partialAmount);
        } else {
            updatedPartials.remove(resourceId);
        }
        return copy(this.slots, this.cooldowns, this.activeModes, this.activeDurations,
                this.comboWindows, this.comboOverrides, this.comboOverrideDurations, this.charges, this.chargeRechargeTicks,
                updatedResources, this.resourceRegenDelays, this.resourceDecayDelays, updatedPartials, this.abilityGrantSources,
                this.passiveGrantSources, this.grantedItemSources, this.recipePermissionSources,
                this.abilityActivationBlockSources, this.statePolicySources,
                this.managedGrantSources, this.abilityAccessRestricted, this.modeLoadouts);
    }

    public AbilityData withResourceRegenDelay(ResourceLocation resourceId, int ticks) {
        Map<ResourceLocation, Integer> updatedDelays = new LinkedHashMap<>(this.resourceRegenDelays);
        if (ticks > 0) {
            updatedDelays.put(resourceId, ticks);
        } else {
            updatedDelays.remove(resourceId);
        }
        return copy(this.slots, this.cooldowns, this.activeModes, this.activeDurations,
                this.comboWindows, this.comboOverrides, this.comboOverrideDurations, this.charges, this.chargeRechargeTicks,
                this.resources, updatedDelays, this.resourceDecayDelays, this.abilityGrantSources,
                this.passiveGrantSources, this.grantedItemSources, this.recipePermissionSources,
                this.abilityActivationBlockSources, this.statePolicySources,
                this.managedGrantSources, this.abilityAccessRestricted, this.modeLoadouts);
    }

    public AbilityData withResourceDecayDelay(ResourceLocation resourceId, int ticks) {
        Map<ResourceLocation, Integer> updatedDelays = new LinkedHashMap<>(this.resourceDecayDelays);
        if (ticks > 0) {
            updatedDelays.put(resourceId, ticks);
        } else {
            updatedDelays.remove(resourceId);
        }
        return copy(this.slots, this.cooldowns, this.activeModes, this.activeDurations,
                this.comboWindows, this.comboOverrides, this.comboOverrideDurations, this.charges, this.chargeRechargeTicks,
                this.resources, this.resourceRegenDelays, updatedDelays, this.abilityGrantSources,
                this.passiveGrantSources, this.grantedItemSources, this.recipePermissionSources,
                this.abilityActivationBlockSources, this.statePolicySources,
                this.managedGrantSources, this.abilityAccessRestricted, this.modeLoadouts);
    }

    public AbilityData clearAbilityState(ResourceLocation abilityId) {
        Map<ResourceLocation, Integer> updatedCooldowns = new LinkedHashMap<>(this.cooldowns);
        Map<ResourceLocation, Integer> updatedDurations = new LinkedHashMap<>(this.activeDurations);
        Map<ResourceLocation, Integer> updatedComboWindows = new LinkedHashMap<>(this.comboWindows);
        Map<ResourceLocation, Integer> updatedCharges = new LinkedHashMap<>(this.charges);
        Map<ResourceLocation, Integer> updatedChargeRecharge = new LinkedHashMap<>(this.chargeRechargeTicks);
        Map<ResourceLocation, Integer> updatedRecoveryProgress = new LinkedHashMap<>(this.recoveryTickProgress);
        Set<ResourceLocation> updatedModes = new LinkedHashSet<>(this.activeModes);
        Map<ResourceLocation, List<ResourceLocation>> updatedCycleHistory = removeModeFromCycleHistory(this.modeCycleHistory, abilityId);
        updatedCooldowns.remove(abilityId);
        updatedDurations.remove(abilityId);
        updatedComboWindows.remove(abilityId);
        updatedCharges.remove(abilityId);
        updatedChargeRecharge.remove(abilityId);
        updatedRecoveryProgress.remove(abilityId);
        updatedModes.remove(abilityId);
        AbilityContainerState updatedContainerState = this.containerState.clearAbilityState(abilityId);
        AbilityData updatedData = copy(this.slots, updatedCooldowns, updatedModes, updatedDurations,
                updatedComboWindows, this.comboOverrides, this.comboOverrideDurations, updatedCharges, updatedChargeRecharge,
                updatedRecoveryProgress, this.resources, this.resourceRegenDelays, this.resourceDecayDelays, this.resourcePartials, this.abilityGrantSources,
                this.passiveGrantSources, this.grantedItemSources, this.recipePermissionSources,
                this.abilityActivationBlockSources, this.statePolicySources,
                this.managedGrantSources, this.abilityAccessRestricted, updatedCycleHistory, this.modeLoadouts);
        return updatedContainerState.equals(this.containerState) ? updatedData : updatedData.copyWithContainerState(updatedContainerState);
    }

    public AbilityData clearComboWindowsForAbility(ResourceLocation abilityId) {
        AbilityData updatedData = withComboWindow(abilityId, 0);
        AbilityContainerState updatedContainerState = updatedData.containerState.clearComboOverridesForAbility(abilityId);
        if (!updatedContainerState.equals(updatedData.containerState)) {
            updatedData = updatedData.copyWithContainerState(updatedContainerState);
        }
        return updatedData;
    }

    public AbilityData tickComboWindows() {
        if (this.comboWindows.isEmpty()) {
            return this;
        }

        Map<ResourceLocation, Integer> updatedWindows = new LinkedHashMap<>();
        boolean changed = false;
        for (Map.Entry<ResourceLocation, Integer> entry : this.comboWindows.entrySet()) {
            int nextValue = entry.getValue() - 1;
            if (nextValue > 0) {
                updatedWindows.put(entry.getKey(), nextValue);
            }
            if (nextValue != entry.getValue()) {
                changed = true;
            }
        }

        return changed
                ? copy(this.slots, this.cooldowns, this.activeModes, this.activeDurations,
                        updatedWindows, this.comboOverrides, this.comboOverrideDurations, this.charges, this.chargeRechargeTicks,
                        this.resources, this.resourceRegenDelays, this.resourceDecayDelays, this.abilityGrantSources,
                        this.passiveGrantSources, this.grantedItemSources, this.recipePermissionSources,
                        this.abilityActivationBlockSources, this.statePolicySources,
                        this.managedGrantSources, this.abilityAccessRestricted, this.modeLoadouts)
                : this;
    }

    public AbilityData tickComboOverrides() {
        AbilityContainerState updatedContainerState = this.containerState.tickComboOverrides();
        return updatedContainerState.equals(this.containerState) ? this : copyWithContainerState(updatedContainerState);
    }

    public AbilityData withAbilityGrantSource(ResourceLocation abilityId, ResourceLocation sourceId, boolean granted) {
        return copy(this.slots, this.cooldowns, this.activeModes, this.activeDurations,
                this.comboWindows, this.comboOverrides, this.comboOverrideDurations, this.charges, this.chargeRechargeTicks,
                this.resources, this.resourceRegenDelays, this.resourceDecayDelays,
                updateGrantSourceMap(this.abilityGrantSources, abilityId, sourceId, granted),
                this.passiveGrantSources, this.grantedItemSources, this.recipePermissionSources,
                this.abilityActivationBlockSources, this.statePolicySources,
                this.managedGrantSources, this.abilityAccessRestricted, this.modeLoadouts);
    }

    public AbilityData withPassiveGrantSource(ResourceLocation passiveId, ResourceLocation sourceId, boolean granted) {
        return copy(this.slots, this.cooldowns, this.activeModes, this.activeDurations,
                this.comboWindows, this.comboOverrides, this.comboOverrideDurations, this.charges, this.chargeRechargeTicks,
                this.resources, this.resourceRegenDelays, this.resourceDecayDelays, this.abilityGrantSources,
                updateGrantSourceMap(this.passiveGrantSources, passiveId, sourceId, granted), this.grantedItemSources,
                this.recipePermissionSources, this.abilityActivationBlockSources, this.statePolicySources,
                this.managedGrantSources, this.abilityAccessRestricted, this.modeLoadouts);
    }

    public AbilityData withGrantedItemSource(ResourceLocation grantedItemId, ResourceLocation sourceId, boolean granted) {
        return copy(this.slots, this.cooldowns, this.activeModes, this.activeDurations,
                this.comboWindows, this.comboOverrides, this.comboOverrideDurations, this.charges, this.chargeRechargeTicks,
                this.resources, this.resourceRegenDelays, this.resourceDecayDelays, this.abilityGrantSources,
                this.passiveGrantSources, updateGrantSourceMap(this.grantedItemSources, grantedItemId, sourceId, granted),
                this.recipePermissionSources, this.abilityActivationBlockSources, this.statePolicySources,
                this.managedGrantSources, this.abilityAccessRestricted, this.modeLoadouts);
    }

    public AbilityData withRecipePermissionSource(ResourceLocation recipeId, ResourceLocation sourceId, boolean granted) {
        return copy(this.slots, this.cooldowns, this.activeModes, this.activeDurations,
                this.comboWindows, this.comboOverrides, this.comboOverrideDurations, this.charges, this.chargeRechargeTicks,
                this.resources, this.resourceRegenDelays, this.resourceDecayDelays, this.abilityGrantSources,
                this.passiveGrantSources, this.grantedItemSources,
                updateGrantSourceMap(this.recipePermissionSources, recipeId, sourceId, granted),
                this.abilityActivationBlockSources, this.statePolicySources,
                this.managedGrantSources, this.abilityAccessRestricted, this.modeLoadouts);
    }

    public AbilityData withAbilityActivationBlockSource(ResourceLocation abilityId, ResourceLocation sourceId, boolean blocked) {
        return copy(this.slots, this.cooldowns, this.activeModes, this.activeDurations,
                this.comboWindows, this.comboOverrides, this.comboOverrideDurations, this.charges, this.chargeRechargeTicks,
                this.resources, this.resourceRegenDelays, this.resourceDecayDelays, this.abilityGrantSources,
                this.passiveGrantSources, this.grantedItemSources, this.recipePermissionSources,
                updateGrantSourceMap(this.abilityActivationBlockSources, abilityId, sourceId, blocked),
                this.statePolicySources, this.managedGrantSources, this.abilityAccessRestricted, this.modeLoadouts);
    }

    public AbilityData withStatePolicySource(ResourceLocation statePolicyId, ResourceLocation sourceId, boolean active) {
        return copy(this.slots, this.cooldowns, this.activeModes, this.activeDurations,
                this.comboWindows, this.comboOverrides, this.comboOverrideDurations, this.charges, this.chargeRechargeTicks,
                this.resources, this.resourceRegenDelays, this.resourceDecayDelays, this.abilityGrantSources,
                this.passiveGrantSources, this.grantedItemSources, this.recipePermissionSources,
                this.abilityActivationBlockSources, updateGrantSourceMap(this.statePolicySources, statePolicyId, sourceId, active),
                this.managedGrantSources, this.abilityAccessRestricted, this.modeLoadouts);
    }

    public AbilityData withStateFlagSource(ResourceLocation stateFlagId, ResourceLocation sourceId, boolean active) {
        return copyWithStateFlags(this.slots, this.cooldowns, this.activeModes, this.activeDurations,
                this.comboWindows, this.comboOverrides, this.comboOverrideDurations, this.charges, this.chargeRechargeTicks,
                this.resources, this.resourceRegenDelays, this.resourceDecayDelays, this.abilityGrantSources,
                this.passiveGrantSources, this.grantedItemSources, this.recipePermissionSources,
                this.abilityActivationBlockSources, this.statePolicySources,
                updateGrantSourceMap(this.stateFlagSources, stateFlagId, sourceId, active), this.grantBundleSources,
                this.managedGrantSources, this.abilityAccessRestricted, this.modeLoadouts);
    }

    public AbilityData withGrantBundleSource(ResourceLocation bundleId, ResourceLocation sourceId, boolean active) {
        return copyWithStateFlags(this.slots, this.cooldowns, this.activeModes, this.activeDurations,
                this.comboWindows, this.comboOverrides, this.comboOverrideDurations, this.charges, this.chargeRechargeTicks,
                this.resources, this.resourceRegenDelays, this.resourceDecayDelays, this.abilityGrantSources,
                this.passiveGrantSources, this.grantedItemSources, this.recipePermissionSources,
                this.abilityActivationBlockSources, this.statePolicySources,
                this.stateFlagSources, updateGrantSourceMap(this.grantBundleSources, bundleId, sourceId, active),
                this.managedGrantSources, this.abilityAccessRestricted, this.modeLoadouts);
    }

    public AbilityData withArtifactUnlockSource(ResourceLocation artifactId, ResourceLocation sourceId, boolean unlocked) {
        return copyWithArtifacts(this.slots, this.cooldowns, this.activeModes, this.activeDurations,
                this.detectorWindows,
                this.comboWindows, this.comboOverrides, this.comboOverrideDurations, this.charges, this.chargeRechargeTicks,
                this.resources, this.resourceRegenDelays, this.resourceDecayDelays, this.abilityGrantSources,
                this.passiveGrantSources, this.grantedItemSources, this.recipePermissionSources,
                this.abilityActivationBlockSources, this.statePolicySources,
                this.stateFlagSources, this.grantBundleSources,
                updateGrantSourceMap(this.artifactUnlockSources, artifactId, sourceId, unlocked),
                this.managedGrantSources, this.abilityAccessRestricted, this.modeLoadouts);
    }

    public AbilityData clearAbilityGrantSources() {
        return copy(this.slots, this.cooldowns, this.activeModes, this.activeDurations,
                this.comboWindows, this.comboOverrides, this.comboOverrideDurations, this.charges, this.chargeRechargeTicks,
                this.resources, this.resourceRegenDelays, this.resourceDecayDelays, Map.of(), this.passiveGrantSources,
                this.grantedItemSources, this.recipePermissionSources, this.abilityActivationBlockSources,
                this.statePolicySources, this.managedGrantSources, this.abilityAccessRestricted, this.modeLoadouts);
    }

    public AbilityData clearPassiveGrantSources() {
        return copy(this.slots, this.cooldowns, this.activeModes, this.activeDurations,
                this.comboWindows, this.comboOverrides, this.comboOverrideDurations, this.charges, this.chargeRechargeTicks,
                this.resources, this.resourceRegenDelays, this.resourceDecayDelays, this.abilityGrantSources, Map.of(),
                this.grantedItemSources, this.recipePermissionSources, this.abilityActivationBlockSources,
                this.statePolicySources, this.managedGrantSources, this.abilityAccessRestricted, this.modeLoadouts);
    }

    public AbilityData clearGrantedItemSources() {
        return copy(this.slots, this.cooldowns, this.activeModes, this.activeDurations,
                this.comboWindows, this.comboOverrides, this.comboOverrideDurations, this.charges, this.chargeRechargeTicks,
                this.resources, this.resourceRegenDelays, this.resourceDecayDelays, this.abilityGrantSources,
                this.passiveGrantSources, Map.of(), this.recipePermissionSources, this.abilityActivationBlockSources,
                this.statePolicySources, this.managedGrantSources, this.abilityAccessRestricted, this.modeLoadouts);
    }

    public AbilityData clearRecipePermissionSources() {
        return copy(this.slots, this.cooldowns, this.activeModes, this.activeDurations,
                this.comboWindows, this.comboOverrides, this.comboOverrideDurations, this.charges, this.chargeRechargeTicks,
                this.resources, this.resourceRegenDelays, this.resourceDecayDelays, this.abilityGrantSources,
                this.passiveGrantSources, this.grantedItemSources, Map.of(), this.abilityActivationBlockSources,
                this.statePolicySources, this.managedGrantSources, this.abilityAccessRestricted, this.modeLoadouts);
    }

    public AbilityData clearAbilityActivationBlockSources() {
        return copy(this.slots, this.cooldowns, this.activeModes, this.activeDurations,
                this.comboWindows, this.comboOverrides, this.comboOverrideDurations, this.charges, this.chargeRechargeTicks,
                this.resources, this.resourceRegenDelays, this.resourceDecayDelays, this.abilityGrantSources,
                this.passiveGrantSources, this.grantedItemSources, this.recipePermissionSources, Map.of(),
                this.statePolicySources, this.managedGrantSources, this.abilityAccessRestricted, this.modeLoadouts);
    }

    public AbilityData clearStatePolicySources() {
        return copy(this.slots, this.cooldowns, this.activeModes, this.activeDurations,
                this.comboWindows, this.comboOverrides, this.comboOverrideDurations, this.charges, this.chargeRechargeTicks,
                this.resources, this.resourceRegenDelays, this.resourceDecayDelays, this.abilityGrantSources,
                this.passiveGrantSources, this.grantedItemSources, this.recipePermissionSources,
                this.abilityActivationBlockSources, Map.of(),
                this.managedGrantSources, this.abilityAccessRestricted, this.modeLoadouts);
    }

    public AbilityData clearStateFlagSources() {
        return copyWithStateFlags(this.slots, this.cooldowns, this.activeModes, this.activeDurations,
                this.comboWindows, this.comboOverrides, this.comboOverrideDurations, this.charges, this.chargeRechargeTicks,
                this.resources, this.resourceRegenDelays, this.resourceDecayDelays, this.abilityGrantSources,
                this.passiveGrantSources, this.grantedItemSources, this.recipePermissionSources,
                this.abilityActivationBlockSources, this.statePolicySources, Map.of(), this.grantBundleSources,
                this.managedGrantSources, this.abilityAccessRestricted, this.modeLoadouts);
    }

    public AbilityData clearGrantBundleSources() {
        return copyWithStateFlags(this.slots, this.cooldowns, this.activeModes, this.activeDurations,
                this.comboWindows, this.comboOverrides, this.comboOverrideDurations, this.charges, this.chargeRechargeTicks,
                this.resources, this.resourceRegenDelays, this.resourceDecayDelays, this.abilityGrantSources,
                this.passiveGrantSources, this.grantedItemSources, this.recipePermissionSources,
                this.abilityActivationBlockSources, this.statePolicySources, this.stateFlagSources, Map.of(),
                this.managedGrantSources, this.abilityAccessRestricted, this.modeLoadouts);
    }

    public AbilityData clearAbilityGrantSource(ResourceLocation sourceId) {
        return copy(this.slots, this.cooldowns, this.activeModes, this.activeDurations,
                this.comboWindows, this.comboOverrides, this.comboOverrideDurations, this.charges, this.chargeRechargeTicks,
                this.resources, this.resourceRegenDelays, this.resourceDecayDelays, removeGrantSource(this.abilityGrantSources, sourceId),
                this.passiveGrantSources, this.grantedItemSources, this.recipePermissionSources,
                this.abilityActivationBlockSources, this.statePolicySources,
                this.managedGrantSources, this.abilityAccessRestricted, this.modeLoadouts);
    }

    public AbilityData clearPassiveGrantSource(ResourceLocation sourceId) {
        return copy(this.slots, this.cooldowns, this.activeModes, this.activeDurations,
                this.comboWindows, this.comboOverrides, this.comboOverrideDurations, this.charges, this.chargeRechargeTicks,
                this.resources, this.resourceRegenDelays, this.resourceDecayDelays, this.abilityGrantSources,
                removeGrantSource(this.passiveGrantSources, sourceId), this.grantedItemSources,
                this.recipePermissionSources, this.abilityActivationBlockSources, this.statePolicySources,
                this.managedGrantSources, this.abilityAccessRestricted, this.modeLoadouts);
    }

    public AbilityData clearGrantedItemSource(ResourceLocation sourceId) {
        return copy(this.slots, this.cooldowns, this.activeModes, this.activeDurations,
                this.comboWindows, this.comboOverrides, this.comboOverrideDurations, this.charges, this.chargeRechargeTicks,
                this.resources, this.resourceRegenDelays, this.resourceDecayDelays, this.abilityGrantSources,
                this.passiveGrantSources, removeGrantSource(this.grantedItemSources, sourceId),
                this.recipePermissionSources, this.abilityActivationBlockSources, this.statePolicySources,
                this.managedGrantSources, this.abilityAccessRestricted, this.modeLoadouts);
    }

    public AbilityData clearRecipePermissionSource(ResourceLocation sourceId) {
        return copy(this.slots, this.cooldowns, this.activeModes, this.activeDurations,
                this.comboWindows, this.comboOverrides, this.comboOverrideDurations, this.charges, this.chargeRechargeTicks,
                this.resources, this.resourceRegenDelays, this.resourceDecayDelays, this.abilityGrantSources,
                this.passiveGrantSources, this.grantedItemSources, removeGrantSource(this.recipePermissionSources, sourceId),
                this.abilityActivationBlockSources, this.statePolicySources,
                this.managedGrantSources, this.abilityAccessRestricted, this.modeLoadouts);
    }

    public AbilityData clearAbilityActivationBlockSource(ResourceLocation sourceId) {
        return copy(this.slots, this.cooldowns, this.activeModes, this.activeDurations,
                this.comboWindows, this.comboOverrides, this.comboOverrideDurations, this.charges, this.chargeRechargeTicks,
                this.resources, this.resourceRegenDelays, this.resourceDecayDelays, this.abilityGrantSources,
                this.passiveGrantSources, this.grantedItemSources, this.recipePermissionSources,
                removeGrantSource(this.abilityActivationBlockSources, sourceId),
                this.statePolicySources, this.managedGrantSources, this.abilityAccessRestricted, this.modeLoadouts);
    }

    public AbilityData clearStatePolicySource(ResourceLocation sourceId) {
        return copy(this.slots, this.cooldowns, this.activeModes, this.activeDurations,
                this.comboWindows, this.comboOverrides, this.comboOverrideDurations, this.charges, this.chargeRechargeTicks,
                this.resources, this.resourceRegenDelays, this.resourceDecayDelays, this.abilityGrantSources,
                this.passiveGrantSources, this.grantedItemSources, this.recipePermissionSources,
                this.abilityActivationBlockSources, removeGrantSource(this.statePolicySources, sourceId),
                this.managedGrantSources, this.abilityAccessRestricted, this.modeLoadouts);
    }

    public AbilityData clearStateFlagSource(ResourceLocation sourceId) {
        return copyWithStateFlags(this.slots, this.cooldowns, this.activeModes, this.activeDurations,
                this.comboWindows, this.comboOverrides, this.comboOverrideDurations, this.charges, this.chargeRechargeTicks,
                this.resources, this.resourceRegenDelays, this.resourceDecayDelays, this.abilityGrantSources,
                this.passiveGrantSources, this.grantedItemSources, this.recipePermissionSources,
                this.abilityActivationBlockSources, this.statePolicySources, removeGrantSource(this.stateFlagSources, sourceId),
                this.grantBundleSources,
                this.managedGrantSources, this.abilityAccessRestricted, this.modeLoadouts);
    }

    public AbilityData clearGrantBundleSource(ResourceLocation sourceId) {
        return copyWithStateFlags(this.slots, this.cooldowns, this.activeModes, this.activeDurations,
                this.comboWindows, this.comboOverrides, this.comboOverrideDurations, this.charges, this.chargeRechargeTicks,
                this.resources, this.resourceRegenDelays, this.resourceDecayDelays, this.abilityGrantSources,
                this.passiveGrantSources, this.grantedItemSources, this.recipePermissionSources,
                this.abilityActivationBlockSources, this.statePolicySources, this.stateFlagSources,
                removeGrantSource(this.grantBundleSources, sourceId),
                this.managedGrantSources, this.abilityAccessRestricted, this.modeLoadouts);
    }

    public AbilityData clearGrantSource(ResourceLocation sourceId) {
        return copyWithArtifacts(this.slots, this.cooldowns, this.activeModes, this.activeDurations,
                this.detectorWindows,
                this.comboWindows, this.comboOverrides, this.comboOverrideDurations, this.charges, this.chargeRechargeTicks,
                this.resources, this.resourceRegenDelays, this.resourceDecayDelays, removeGrantSource(this.abilityGrantSources, sourceId),
                removeGrantSource(this.passiveGrantSources, sourceId), removeGrantSource(this.grantedItemSources, sourceId),
                removeGrantSource(this.recipePermissionSources, sourceId), removeGrantSource(this.abilityActivationBlockSources, sourceId),
                removeGrantSource(this.statePolicySources, sourceId), removeGrantSource(this.stateFlagSources, sourceId),
                removeGrantSource(this.grantBundleSources, sourceId), removeGrantSource(this.artifactUnlockSources, sourceId),
                removeFromSet(this.managedGrantSources, sourceId),
                this.abilityAccessRestricted, this.modeLoadouts);
    }

    public AbilityData withManagedGrantSource(ResourceLocation sourceId, boolean managed) {
        Set<ResourceLocation> updatedManagedSources = new LinkedHashSet<>(this.managedGrantSources);
        if (managed) {
            updatedManagedSources.add(sourceId);
        } else {
            updatedManagedSources.remove(sourceId);
        }
        return copy(this.slots, this.cooldowns, this.activeModes, this.activeDurations,
                this.comboWindows, this.comboOverrides, this.comboOverrideDurations, this.charges, this.chargeRechargeTicks,
                this.resources, this.resourceRegenDelays, this.resourceDecayDelays, this.abilityGrantSources,
                this.passiveGrantSources, this.grantedItemSources, this.recipePermissionSources,
                this.abilityActivationBlockSources, this.statePolicySources,
                updatedManagedSources, this.abilityAccessRestricted, this.modeLoadouts);
    }

    public AbilityData withAbilityAccessRestricted(boolean restricted) {
        return copy(this.slots, this.cooldowns, this.activeModes, this.activeDurations,
                this.comboWindows, this.comboOverrides, this.comboOverrideDurations, this.charges, this.chargeRechargeTicks,
                this.resources, this.resourceRegenDelays, this.resourceDecayDelays, this.abilityGrantSources,
                this.passiveGrantSources, this.grantedItemSources, this.recipePermissionSources,
                this.abilityActivationBlockSources, this.statePolicySources,
                this.managedGrantSources, restricted, this.modeLoadouts);
    }

    private static boolean isValidSlot(int slot) {
        return slot >= 0 && slot < SLOT_COUNT;
    }

    private static boolean isValidSlotReference(AbilitySlotReference slotReference) {
        return AbilitySlotContainerApi.isPrimarySlotReference(slotReference);
    }

    private static AbilityContainerState collapseToPrimaryBar(
            AbilityContainerState containerState,
            List<Optional<ResourceLocation>> slots,
            List<Optional<ResourceLocation>> comboOverrides,
            List<Integer> comboOverrideDurations,
            Map<ResourceLocation, List<Optional<ResourceLocation>>> modeLoadouts
    ) {
        AbilityContainerState resolvedState = containerState;
        if (resolvedState.isEmpty()) {
            resolvedState = AbilityContainerState.fromLegacy(slots, comboOverrides, comboOverrideDurations, modeLoadouts);
        }
        return AbilityContainerState.fromLegacy(
                resolvedState.legacyPrimarySlots(SLOT_COUNT),
                resolvedState.legacyPrimaryComboOverrides(SLOT_COUNT),
                resolvedState.legacyPrimaryComboDurations(SLOT_COUNT),
                resolvedState.legacyPrimaryModeLoadouts(SLOT_COUNT)
        );
    }

    private static List<Optional<ResourceLocation>> normalizeSlots(List<Optional<ResourceLocation>> slots) {
        List<Optional<ResourceLocation>> normalized = new ArrayList<>(SLOT_COUNT);
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            normalized.add(slot < slots.size() ? slots.get(slot) : Optional.empty());
        }
        return List.copyOf(normalized);
    }

    private static List<Integer> normalizeSlotIntegers(List<Integer> values) {
        List<Integer> normalized = new ArrayList<>(SLOT_COUNT);
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            int value = slot < values.size() && values.get(slot) != null ? values.get(slot) : 0;
            normalized.add(Math.max(0, value));
        }
        return List.copyOf(normalized);
    }

    private static Map<ResourceLocation, Integer> normalizePositiveMap(Map<ResourceLocation, Integer> source) {
        Map<ResourceLocation, Integer> normalized = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, Integer> entry : source.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null && entry.getValue() > 0) {
                normalized.put(entry.getKey(), entry.getValue());
            }
        }
        return Map.copyOf(normalized);
    }

    private static Map<ResourceLocation, Integer> normalizeNonNegativeMap(Map<ResourceLocation, Integer> source) {
        Map<ResourceLocation, Integer> normalized = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, Integer> entry : source.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null && entry.getValue() >= 0) {
                normalized.put(entry.getKey(), entry.getValue());
            }
        }
        return Map.copyOf(normalized);
    }

    private static Map<ResourceLocation, Integer> normalizeProgressMap(Map<ResourceLocation, Integer> source) {
        Map<ResourceLocation, Integer> normalized = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, Integer> entry : source.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue() <= 0) {
                continue;
            }
            normalized.put(entry.getKey(), Math.min(entry.getValue(), 999));
        }
        return Map.copyOf(normalized);
    }

    private static Map<ResourceLocation, Integer> normalizePartialMap(Map<ResourceLocation, Integer> source) {
        Map<ResourceLocation, Integer> normalized = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, Integer> entry : source.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue() <= 0) {
                continue;
            }
            normalized.put(entry.getKey(), Math.min(entry.getValue(), 999));
        }
        return Map.copyOf(normalized);
    }

    private static Map<ResourceLocation, Set<ResourceLocation>> normalizeGrantSourceMap(Map<ResourceLocation, Set<ResourceLocation>> source) {
        Map<ResourceLocation, Set<ResourceLocation>> normalized = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, Set<ResourceLocation>> entry : source.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }

            Set<ResourceLocation> normalizedSources = new LinkedHashSet<>();
            for (ResourceLocation sourceId : entry.getValue()) {
                if (sourceId != null) {
                    normalizedSources.add(sourceId);
                }
            }
            if (!normalizedSources.isEmpty()) {
                normalized.put(entry.getKey(), Set.copyOf(normalizedSources));
            }
        }
        return Map.copyOf(normalized);
    }

    private static Map<ResourceLocation, List<Optional<ResourceLocation>>> normalizeModeLoadouts(
            Map<ResourceLocation, List<Optional<ResourceLocation>>> source
    ) {
        Map<ResourceLocation, List<Optional<ResourceLocation>>> normalized = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, List<Optional<ResourceLocation>>> entry : source.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            List<Optional<ResourceLocation>> normalizedSlots = normalizeSlots(entry.getValue());
            if (normalizedSlots.stream().allMatch(Optional::isEmpty)) {
                continue;
            }
            normalized.put(entry.getKey(), normalizedSlots);
        }
        return Map.copyOf(normalized);
    }

    private static Map<ResourceLocation, List<ResourceLocation>> normalizeModeCycleHistory(
            Map<ResourceLocation, List<ResourceLocation>> source
    ) {
        Map<ResourceLocation, List<ResourceLocation>> normalized = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, List<ResourceLocation>> entry : source.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            List<ResourceLocation> normalizedModes = new ArrayList<>();
            Set<ResourceLocation> seenModes = new LinkedHashSet<>();
            for (ResourceLocation modeId : entry.getValue()) {
                if (modeId != null && seenModes.add(modeId)) {
                    normalizedModes.add(modeId);
                }
            }
            if (!normalizedModes.isEmpty()) {
                normalized.put(entry.getKey(), List.copyOf(normalizedModes));
            }
        }
        return Map.copyOf(normalized);
    }

    private static Map<ResourceLocation, Set<ResourceLocation>> updateGrantSourceMap(
            Map<ResourceLocation, Set<ResourceLocation>> sourceMap,
            ResourceLocation targetId,
            ResourceLocation sourceId,
            boolean granted
    ) {
        Map<ResourceLocation, Set<ResourceLocation>> updatedMap = new LinkedHashMap<>(sourceMap);
        Set<ResourceLocation> updatedSources = new LinkedHashSet<>(updatedMap.getOrDefault(targetId, Set.of()));
        if (granted) {
            updatedSources.add(sourceId);
        } else {
            updatedSources.remove(sourceId);
        }

        if (updatedSources.isEmpty()) {
            updatedMap.remove(targetId);
        } else {
            updatedMap.put(targetId, Set.copyOf(updatedSources));
        }
        return Map.copyOf(updatedMap);
    }

    private static Map<ResourceLocation, Set<ResourceLocation>> removeGrantSource(
            Map<ResourceLocation, Set<ResourceLocation>> sourceMap,
            ResourceLocation sourceId
    ) {
        Map<ResourceLocation, Set<ResourceLocation>> updatedMap = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, Set<ResourceLocation>> entry : sourceMap.entrySet()) {
            Set<ResourceLocation> updatedSources = new LinkedHashSet<>(entry.getValue());
            updatedSources.remove(sourceId);
            if (!updatedSources.isEmpty()) {
                updatedMap.put(entry.getKey(), Set.copyOf(updatedSources));
            }
        }
        return Map.copyOf(updatedMap);
    }

    private static Set<ResourceLocation> removeFromSet(Set<ResourceLocation> sourceSet, ResourceLocation sourceId) {
        Set<ResourceLocation> updatedSet = new LinkedHashSet<>(sourceSet);
        updatedSet.remove(sourceId);
        return Set.copyOf(updatedSet);
    }

    private static Map<ResourceLocation, List<ResourceLocation>> removeModeFromCycleHistory(
            Map<ResourceLocation, List<ResourceLocation>> sourceMap,
            ResourceLocation modeId
    ) {
        Map<ResourceLocation, List<ResourceLocation>> updatedMap = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, List<ResourceLocation>> entry : sourceMap.entrySet()) {
            List<ResourceLocation> updatedModes = new ArrayList<>(entry.getValue());
            updatedModes.removeIf(modeId::equals);
            if (!updatedModes.isEmpty()) {
                updatedMap.put(entry.getKey(), List.copyOf(updatedModes));
            }
        }
        return Map.copyOf(updatedMap);
    }

    private AbilityData copyWithContainerState(AbilityContainerState containerState) {
        return copy(
                containerState.legacyPrimarySlots(SLOT_COUNT),
                this.cooldowns,
                this.activeModes,
                this.activeDurations,
                this.detectorWindows,
                this.comboWindows,
                containerState.legacyPrimaryComboOverrides(SLOT_COUNT),
                containerState.legacyPrimaryComboDurations(SLOT_COUNT),
                this.charges,
                this.chargeRechargeTicks,
                this.recoveryTickProgress,
                this.resources,
                this.resourceRegenDelays,
                this.resourceDecayDelays,
                this.resourcePartials,
                this.abilityGrantSources,
                this.passiveGrantSources,
                this.grantedItemSources,
                this.recipePermissionSources,
                this.abilityActivationBlockSources,
                this.statePolicySources,
                this.stateFlagSources,
                this.grantBundleSources,
                this.artifactUnlockSources,
                this.managedGrantSources,
                this.abilityAccessRestricted,
                this.modeCycleHistory,
                containerState.legacyPrimaryModeLoadouts(SLOT_COUNT),
                containerState
        );
    }

    private AbilityData copy(
            List<Optional<ResourceLocation>> slots,
            Map<ResourceLocation, Integer> cooldowns,
            Set<ResourceLocation> activeModes,
            Map<ResourceLocation, Integer> activeDurations,
            Map<ResourceLocation, Integer> comboWindows,
            List<Optional<ResourceLocation>> comboOverrides,
            List<Integer> comboOverrideDurations,
            Map<ResourceLocation, Integer> charges,
            Map<ResourceLocation, Integer> chargeRechargeTicks,
            Map<ResourceLocation, Integer> resources,
            Map<ResourceLocation, Integer> resourceRegenDelays,
            Map<ResourceLocation, Integer> resourceDecayDelays,
            Map<ResourceLocation, Set<ResourceLocation>> abilityGrantSources,
            Map<ResourceLocation, Set<ResourceLocation>> passiveGrantSources,
            Map<ResourceLocation, Set<ResourceLocation>> grantedItemSources,
            Map<ResourceLocation, Set<ResourceLocation>> recipePermissionSources,
            Map<ResourceLocation, Set<ResourceLocation>> abilityActivationBlockSources,
            Map<ResourceLocation, Set<ResourceLocation>> statePolicySources,
            Set<ResourceLocation> managedGrantSources,
            boolean abilityAccessRestricted,
            Map<ResourceLocation, List<Optional<ResourceLocation>>> modeLoadouts
    ) {
        return copyWithDetectors(
                slots,
                cooldowns,
                activeModes,
                activeDurations,
                this.detectorWindows,
                comboWindows,
                comboOverrides,
                comboOverrideDurations,
                charges,
                chargeRechargeTicks,
                resources,
                resourceRegenDelays,
                resourceDecayDelays,
                abilityGrantSources,
                passiveGrantSources,
                grantedItemSources,
                recipePermissionSources,
                abilityActivationBlockSources,
                statePolicySources,
                managedGrantSources,
                abilityAccessRestricted,
                modeLoadouts
        );
    }

    private AbilityData copyWithDetectors(
            List<Optional<ResourceLocation>> slots,
            Map<ResourceLocation, Integer> cooldowns,
            Set<ResourceLocation> activeModes,
            Map<ResourceLocation, Integer> activeDurations,
            Map<ResourceLocation, Integer> detectorWindows,
            Map<ResourceLocation, Integer> comboWindows,
            List<Optional<ResourceLocation>> comboOverrides,
            List<Integer> comboOverrideDurations,
            Map<ResourceLocation, Integer> charges,
            Map<ResourceLocation, Integer> chargeRechargeTicks,
            Map<ResourceLocation, Integer> resources,
            Map<ResourceLocation, Integer> resourceRegenDelays,
            Map<ResourceLocation, Integer> resourceDecayDelays,
            Map<ResourceLocation, Set<ResourceLocation>> abilityGrantSources,
            Map<ResourceLocation, Set<ResourceLocation>> passiveGrantSources,
            Map<ResourceLocation, Set<ResourceLocation>> grantedItemSources,
            Map<ResourceLocation, Set<ResourceLocation>> recipePermissionSources,
            Map<ResourceLocation, Set<ResourceLocation>> abilityActivationBlockSources,
            Map<ResourceLocation, Set<ResourceLocation>> statePolicySources,
            Set<ResourceLocation> managedGrantSources,
            boolean abilityAccessRestricted,
            Map<ResourceLocation, List<Optional<ResourceLocation>>> modeLoadouts
    ) {
        return copyWithArtifacts(
                slots,
                cooldowns,
                activeModes,
                activeDurations,
                detectorWindows,
                comboWindows,
                comboOverrides,
                comboOverrideDurations,
                charges,
                chargeRechargeTicks,
                resources,
                resourceRegenDelays,
                resourceDecayDelays,
                abilityGrantSources,
                passiveGrantSources,
                grantedItemSources,
                recipePermissionSources,
                abilityActivationBlockSources,
                statePolicySources,
                this.stateFlagSources,
                this.grantBundleSources,
                this.artifactUnlockSources,
                managedGrantSources,
                abilityAccessRestricted,
                modeLoadouts
        );
    }

    private AbilityData copyWithStateFlags(
            List<Optional<ResourceLocation>> slots,
            Map<ResourceLocation, Integer> cooldowns,
            Set<ResourceLocation> activeModes,
            Map<ResourceLocation, Integer> activeDurations,
            Map<ResourceLocation, Integer> comboWindows,
            List<Optional<ResourceLocation>> comboOverrides,
            List<Integer> comboOverrideDurations,
            Map<ResourceLocation, Integer> charges,
            Map<ResourceLocation, Integer> chargeRechargeTicks,
            Map<ResourceLocation, Integer> resources,
            Map<ResourceLocation, Integer> resourceRegenDelays,
            Map<ResourceLocation, Integer> resourceDecayDelays,
            Map<ResourceLocation, Set<ResourceLocation>> abilityGrantSources,
            Map<ResourceLocation, Set<ResourceLocation>> passiveGrantSources,
            Map<ResourceLocation, Set<ResourceLocation>> grantedItemSources,
            Map<ResourceLocation, Set<ResourceLocation>> recipePermissionSources,
            Map<ResourceLocation, Set<ResourceLocation>> abilityActivationBlockSources,
            Map<ResourceLocation, Set<ResourceLocation>> statePolicySources,
            Map<ResourceLocation, Set<ResourceLocation>> stateFlagSources,
            Map<ResourceLocation, Set<ResourceLocation>> grantBundleSources,
            Set<ResourceLocation> managedGrantSources,
            boolean abilityAccessRestricted,
            Map<ResourceLocation, List<Optional<ResourceLocation>>> modeLoadouts
    ) {
        return copyWithArtifacts(
                slots,
                cooldowns,
                activeModes,
                activeDurations,
                this.detectorWindows,
                comboWindows,
                comboOverrides,
                comboOverrideDurations,
                charges,
                chargeRechargeTicks,
                resources,
                resourceRegenDelays,
                resourceDecayDelays,
                abilityGrantSources,
                passiveGrantSources,
                grantedItemSources,
                recipePermissionSources,
                abilityActivationBlockSources,
                statePolicySources,
                stateFlagSources,
                grantBundleSources,
                this.artifactUnlockSources,
                managedGrantSources,
                abilityAccessRestricted,
                modeLoadouts
        );
    }

    private AbilityData copyWithArtifacts(
            List<Optional<ResourceLocation>> slots,
            Map<ResourceLocation, Integer> cooldowns,
            Set<ResourceLocation> activeModes,
            Map<ResourceLocation, Integer> activeDurations,
            Map<ResourceLocation, Integer> detectorWindows,
            Map<ResourceLocation, Integer> comboWindows,
            List<Optional<ResourceLocation>> comboOverrides,
            List<Integer> comboOverrideDurations,
            Map<ResourceLocation, Integer> charges,
            Map<ResourceLocation, Integer> chargeRechargeTicks,
            Map<ResourceLocation, Integer> resources,
            Map<ResourceLocation, Integer> resourceRegenDelays,
            Map<ResourceLocation, Integer> resourceDecayDelays,
            Map<ResourceLocation, Set<ResourceLocation>> abilityGrantSources,
            Map<ResourceLocation, Set<ResourceLocation>> passiveGrantSources,
            Map<ResourceLocation, Set<ResourceLocation>> grantedItemSources,
            Map<ResourceLocation, Set<ResourceLocation>> recipePermissionSources,
            Map<ResourceLocation, Set<ResourceLocation>> abilityActivationBlockSources,
            Map<ResourceLocation, Set<ResourceLocation>> statePolicySources,
            Map<ResourceLocation, Set<ResourceLocation>> stateFlagSources,
            Map<ResourceLocation, Set<ResourceLocation>> grantBundleSources,
            Map<ResourceLocation, Set<ResourceLocation>> artifactUnlockSources,
            Set<ResourceLocation> managedGrantSources,
            boolean abilityAccessRestricted,
            Map<ResourceLocation, List<Optional<ResourceLocation>>> modeLoadouts
    ) {
        return copy(
                slots,
                cooldowns,
                activeModes,
                activeDurations,
                detectorWindows,
                comboWindows,
                comboOverrides,
                comboOverrideDurations,
                charges,
                chargeRechargeTicks,
                this.recoveryTickProgress,
                resources,
                resourceRegenDelays,
                resourceDecayDelays,
                this.resourcePartials,
                abilityGrantSources,
                passiveGrantSources,
                grantedItemSources,
                recipePermissionSources,
                abilityActivationBlockSources,
                statePolicySources,
                stateFlagSources,
                grantBundleSources,
                artifactUnlockSources,
                managedGrantSources,
                abilityAccessRestricted,
                this.modeCycleHistory,
                modeLoadouts,
                this.containerState
        );
    }

    private AbilityData copy(
            List<Optional<ResourceLocation>> slots,
            Map<ResourceLocation, Integer> cooldowns,
            Set<ResourceLocation> activeModes,
            Map<ResourceLocation, Integer> activeDurations,
            Map<ResourceLocation, Integer> comboWindows,
            List<Optional<ResourceLocation>> comboOverrides,
            List<Integer> comboOverrideDurations,
            Map<ResourceLocation, Integer> charges,
            Map<ResourceLocation, Integer> chargeRechargeTicks,
            Map<ResourceLocation, Integer> recoveryTickProgress,
            Map<ResourceLocation, Integer> resources,
            Map<ResourceLocation, Integer> resourceRegenDelays,
            Map<ResourceLocation, Integer> resourceDecayDelays,
            Map<ResourceLocation, Integer> resourcePartials,
            Map<ResourceLocation, Set<ResourceLocation>> abilityGrantSources,
            Map<ResourceLocation, Set<ResourceLocation>> passiveGrantSources,
            Map<ResourceLocation, Set<ResourceLocation>> grantedItemSources,
            Map<ResourceLocation, Set<ResourceLocation>> recipePermissionSources,
            Map<ResourceLocation, Set<ResourceLocation>> abilityActivationBlockSources,
            Map<ResourceLocation, Set<ResourceLocation>> statePolicySources,
            Set<ResourceLocation> managedGrantSources,
            boolean abilityAccessRestricted,
            Map<ResourceLocation, List<ResourceLocation>> modeCycleHistory,
            Map<ResourceLocation, List<Optional<ResourceLocation>>> modeLoadouts
    ) {
        return copy(
                slots,
                cooldowns,
                activeModes,
                activeDurations,
                this.detectorWindows,
                comboWindows,
                comboOverrides,
                comboOverrideDurations,
                charges,
                chargeRechargeTicks,
                recoveryTickProgress,
                resources,
                resourceRegenDelays,
                resourceDecayDelays,
                resourcePartials,
                abilityGrantSources,
                passiveGrantSources,
                grantedItemSources,
                recipePermissionSources,
                abilityActivationBlockSources,
                statePolicySources,
                this.stateFlagSources,
                this.grantBundleSources,
                this.artifactUnlockSources,
                managedGrantSources,
                abilityAccessRestricted,
                modeCycleHistory,
                modeLoadouts,
                this.containerState
        );
    }

    private AbilityData copy(
            List<Optional<ResourceLocation>> slots,
            Map<ResourceLocation, Integer> cooldowns,
            Set<ResourceLocation> activeModes,
            Map<ResourceLocation, Integer> activeDurations,
            Map<ResourceLocation, Integer> comboWindows,
            List<Optional<ResourceLocation>> comboOverrides,
            List<Integer> comboOverrideDurations,
            Map<ResourceLocation, Integer> charges,
            Map<ResourceLocation, Integer> chargeRechargeTicks,
            Map<ResourceLocation, Integer> resources,
            Map<ResourceLocation, Integer> resourceRegenDelays,
            Map<ResourceLocation, Integer> resourceDecayDelays,
            Map<ResourceLocation, Integer> resourcePartials,
            Map<ResourceLocation, Set<ResourceLocation>> abilityGrantSources,
            Map<ResourceLocation, Set<ResourceLocation>> passiveGrantSources,
            Map<ResourceLocation, Set<ResourceLocation>> grantedItemSources,
            Map<ResourceLocation, Set<ResourceLocation>> recipePermissionSources,
            Map<ResourceLocation, Set<ResourceLocation>> abilityActivationBlockSources,
            Map<ResourceLocation, Set<ResourceLocation>> statePolicySources,
            Set<ResourceLocation> managedGrantSources,
            boolean abilityAccessRestricted,
            Map<ResourceLocation, List<Optional<ResourceLocation>>> modeLoadouts
    ) {
        return copy(
                slots,
                cooldowns,
                activeModes,
                activeDurations,
                this.detectorWindows,
                comboWindows,
                comboOverrides,
                comboOverrideDurations,
                charges,
                chargeRechargeTicks,
                this.recoveryTickProgress,
                resources,
                resourceRegenDelays,
                resourceDecayDelays,
                resourcePartials,
                abilityGrantSources,
                passiveGrantSources,
                grantedItemSources,
                recipePermissionSources,
                abilityActivationBlockSources,
                statePolicySources,
                this.stateFlagSources,
                this.grantBundleSources,
                this.artifactUnlockSources,
                managedGrantSources,
                abilityAccessRestricted,
                this.modeCycleHistory,
                modeLoadouts,
                this.containerState
        );
    }

    private AbilityData copyWithStateFlags(
            List<Optional<ResourceLocation>> slots,
            Map<ResourceLocation, Integer> cooldowns,
            Set<ResourceLocation> activeModes,
            Map<ResourceLocation, Integer> activeDurations,
            Map<ResourceLocation, Integer> comboWindows,
            List<Optional<ResourceLocation>> comboOverrides,
            List<Integer> comboOverrideDurations,
            Map<ResourceLocation, Integer> charges,
            Map<ResourceLocation, Integer> chargeRechargeTicks,
            Map<ResourceLocation, Integer> resources,
            Map<ResourceLocation, Integer> resourceRegenDelays,
            Map<ResourceLocation, Integer> resourceDecayDelays,
            Map<ResourceLocation, Integer> resourcePartials,
            Map<ResourceLocation, Set<ResourceLocation>> abilityGrantSources,
            Map<ResourceLocation, Set<ResourceLocation>> passiveGrantSources,
            Map<ResourceLocation, Set<ResourceLocation>> grantedItemSources,
            Map<ResourceLocation, Set<ResourceLocation>> recipePermissionSources,
            Map<ResourceLocation, Set<ResourceLocation>> abilityActivationBlockSources,
            Map<ResourceLocation, Set<ResourceLocation>> statePolicySources,
            Map<ResourceLocation, Set<ResourceLocation>> stateFlagSources,
            Map<ResourceLocation, Set<ResourceLocation>> grantBundleSources,
            Map<ResourceLocation, Set<ResourceLocation>> artifactUnlockSources,
            Set<ResourceLocation> managedGrantSources,
            boolean abilityAccessRestricted,
            Map<ResourceLocation, List<Optional<ResourceLocation>>> modeLoadouts
    ) {
        return copy(
                slots,
                cooldowns,
                activeModes,
                activeDurations,
                this.detectorWindows,
                comboWindows,
                comboOverrides,
                comboOverrideDurations,
                charges,
                chargeRechargeTicks,
                this.recoveryTickProgress,
                resources,
                resourceRegenDelays,
                resourceDecayDelays,
                resourcePartials,
                abilityGrantSources,
                passiveGrantSources,
                grantedItemSources,
                recipePermissionSources,
                abilityActivationBlockSources,
                statePolicySources,
                stateFlagSources,
                grantBundleSources,
                artifactUnlockSources,
                managedGrantSources,
                abilityAccessRestricted,
                this.modeCycleHistory,
                modeLoadouts,
                this.containerState
        );
    }

    private static AbilityData copy(
            List<Optional<ResourceLocation>> slots,
            Map<ResourceLocation, Integer> cooldowns,
            Set<ResourceLocation> activeModes,
            Map<ResourceLocation, Integer> activeDurations,
            Map<ResourceLocation, Integer> detectorWindows,
            Map<ResourceLocation, Integer> comboWindows,
            List<Optional<ResourceLocation>> comboOverrides,
            List<Integer> comboOverrideDurations,
            Map<ResourceLocation, Integer> charges,
            Map<ResourceLocation, Integer> chargeRechargeTicks,
            Map<ResourceLocation, Integer> recoveryTickProgress,
            Map<ResourceLocation, Integer> resources,
            Map<ResourceLocation, Integer> resourceRegenDelays,
            Map<ResourceLocation, Integer> resourceDecayDelays,
            Map<ResourceLocation, Integer> resourcePartials,
            Map<ResourceLocation, Set<ResourceLocation>> abilityGrantSources,
            Map<ResourceLocation, Set<ResourceLocation>> passiveGrantSources,
            Map<ResourceLocation, Set<ResourceLocation>> grantedItemSources,
            Map<ResourceLocation, Set<ResourceLocation>> recipePermissionSources,
            Map<ResourceLocation, Set<ResourceLocation>> abilityActivationBlockSources,
            Map<ResourceLocation, Set<ResourceLocation>> statePolicySources,
            Map<ResourceLocation, Set<ResourceLocation>> stateFlagSources,
            Map<ResourceLocation, Set<ResourceLocation>> grantBundleSources,
            Map<ResourceLocation, Set<ResourceLocation>> artifactUnlockSources,
            Set<ResourceLocation> managedGrantSources,
            boolean abilityAccessRestricted,
            Map<ResourceLocation, List<ResourceLocation>> modeCycleHistory,
            Map<ResourceLocation, List<Optional<ResourceLocation>>> modeLoadouts,
            AbilityContainerState containerState
    ) {
        return new AbilityData(
                slots,
                cooldowns,
                activeModes,
                activeDurations,
                detectorWindows,
                comboWindows,
                comboOverrides,
                comboOverrideDurations,
                charges,
                chargeRechargeTicks,
                recoveryTickProgress,
                resources,
                resourceRegenDelays,
                resourceDecayDelays,
                resourcePartials,
                abilityGrantSources,
                passiveGrantSources,
                grantedItemSources,
                recipePermissionSources,
                abilityActivationBlockSources,
                statePolicySources,
                stateFlagSources,
                grantBundleSources,
                artifactUnlockSources,
                managedGrantSources,
                abilityAccessRestricted,
                modeCycleHistory,
                modeLoadouts,
                containerState
        );
    }
}

