package com.whatxe.xlib.ability;

import com.whatxe.xlib.api.event.XLibAbilityActivationEvent;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;

public final class AbilityRuntime {
    private AbilityRuntime() {}

    public static AbilityUseResult activate(ServerPlayer player, AbilityData currentData, AbilityDefinition definition) {
        return activate(player, currentData, definition, -1);
    }

    public static AbilityUseResult activate(
            ServerPlayer player,
            AbilityData currentData,
            AbilityDefinition definition,
            int activatedSlot
    ) {
        ResourceLocation abilityId = definition.id();
        if (definition.toggleAbility() && currentData.isModeActive(abilityId)) {
            return endAbility(player, currentData, definition, AbilityEndReason.PLAYER_TOGGLED);
        }

        XLibAbilityActivationEvent.Pre preEvent = new XLibAbilityActivationEvent.Pre(player, definition, currentData);
        NeoForge.EVENT_BUS.post(preEvent);
        if (preEvent.isCanceled()) {
            definition.playSounds(player, AbilitySoundTrigger.FAIL);
            return finishActivation(player, currentData, definition, AbilityUseResult.fail(currentData, preEvent.failureFeedback()));
        }

        Optional<Component> requirementFailure = AbilityGrantApi.firstActivationFailure(player, currentData, definition);
        if (requirementFailure.isPresent()) {
            definition.playSounds(player, AbilitySoundTrigger.FAIL);
            return finishActivation(player, currentData, definition, AbilityUseResult.fail(currentData, requirementFailure.get()));
        }

        Optional<Component> modeFailure = ModeApi.firstActivationFailure(player, definition, currentData);
        if (modeFailure.isPresent()) {
            definition.playSounds(player, AbilitySoundTrigger.FAIL);
            return finishActivation(player, currentData, definition, AbilityUseResult.fail(currentData, modeFailure.get()));
        }

        if (definition.usesCharges()) {
            int availableCharges = currentData.chargeCountFor(abilityId, definition.maxCharges());
            if (availableCharges <= 0) {
                definition.playSounds(player, AbilitySoundTrigger.FAIL);
                return finishActivation(player, currentData, definition, AbilityUseResult.fail(
                        currentData,
                        Component.translatable(
                                "message.xlib.ability_cooldown",
                                definition.displayName(),
                                formatCooldown(currentData.chargeRechargeFor(abilityId))
                        )
                ));
            }
        } else {
            int cooldownTicks = currentData.cooldownFor(abilityId);
            if (cooldownTicks > 0) {
                definition.playSounds(player, AbilitySoundTrigger.FAIL);
                return finishActivation(player, currentData, definition, AbilityUseResult.fail(
                        currentData,
                        Component.translatable("message.xlib.ability_cooldown", definition.displayName(), formatCooldown(cooldownTicks))
                ));
            }
        }

        Optional<Component> resourceFailure = firstResourceFailure(currentData, definition);
        if (resourceFailure.isPresent()) {
            definition.playSounds(player, AbilitySoundTrigger.FAIL);
            return finishActivation(player, currentData, definition, AbilityUseResult.fail(currentData, resourceFailure.get()));
        }

        AbilityUseResult actionResult = definition.activate(player, currentData);
        if (!actionResult.consumed()) {
            if (actionResult.feedback() != null) {
                definition.playSounds(player, AbilitySoundTrigger.FAIL);
            }
            return finishActivation(player, currentData, definition, actionResult);
        }

        AbilityData updatedData = ComboChainApi.consumeComboAbility(actionResult.data(), abilityId);
        updatedData = spendResources(updatedData, definition);
        if (definition.toggleAbility()) {
            updatedData = ModeApi.prepareActivation(player, updatedData, definition);
            updatedData = updatedData.withMode(abilityId, true);
            updatedData = ModeApi.recordActivation(updatedData, definition);
            if (definition.durationTicks() > 0) {
                updatedData = updatedData.withActiveDuration(abilityId, definition.durationTicks());
            }
        }

        if (definition.usesCharges()) {
            int nextCharges = Math.max(0, updatedData.chargeCountFor(abilityId, definition.maxCharges()) - 1);
            updatedData = updatedData.withChargeCount(abilityId, nextCharges);
            if (definition.cooldownPolicy() == AbilityCooldownPolicy.ON_USE || !definition.toggleAbility()) {
                updatedData = beginChargeRecharge(updatedData, definition);
            }
        } else if (definition.cooldownPolicy() == AbilityCooldownPolicy.ON_USE && definition.cooldownTicks() > 0) {
            updatedData = updatedData.withCooldown(abilityId, definition.cooldownTicks());
        }

        updatedData = ComboChainApi.applyActivation(player, updatedData, abilityId, activatedSlot);
        definition.playSounds(player, AbilitySoundTrigger.ACTIVATE);
        AbilityUseResult result = new AbilityUseResult(updatedData, true, actionResult.feedback());
        ModeApi.postModeStarted(player, definition, currentData, updatedData);
        return finishActivation(player, currentData, definition, result);
    }

    public static AbilityUseResult endAbility(
            ServerPlayer player,
            AbilityData currentData,
            AbilityDefinition definition,
            AbilityEndReason reason
    ) {
        AbilityData previousData = currentData;
        AbilityUseResult endResult = definition.end(player, currentData, reason);
        if (!endResult.consumed()) {
            return endResult;
        }

        AbilityData updatedData = endResult.data().withMode(definition.id(), false);
        if (definition.usesCharges()) {
            if (definition.cooldownPolicy() == AbilityCooldownPolicy.ON_END) {
                updatedData = beginChargeRecharge(updatedData, definition);
            }
        } else if (definition.cooldownPolicy() == AbilityCooldownPolicy.ON_END && definition.cooldownTicks() > 0) {
            updatedData = updatedData.withCooldown(definition.id(), definition.cooldownTicks());
        }

        definition.playSounds(player, reason.usesInterruptSound() ? AbilitySoundTrigger.INTERRUPT : AbilitySoundTrigger.END);
        ModeApi.postModeEnded(player, definition, previousData, updatedData, reason);
        return new AbilityUseResult(updatedData, true, endResult.feedback());
    }

    public static AbilityData tick(ServerPlayer player, AbilityData currentData) {
        AbilityData updatedData = AbilityGrantApi.sanitize(AbilityApi.sanitizeData(currentData));
        double cooldownTickRate = cooldownTickRateMultiplier(player, updatedData);
        updatedData = updatedData.tickCooldowns(cooldownTickRate);
        updatedData = ComboChainApi.tick(updatedData);
        updatedData = tickChargeRecharges(player, updatedData, cooldownTickRate);
        updatedData = AbilityResourceRuntime.tick(player, updatedData);

        for (ResourceLocation activeAbilityId : List.copyOf(updatedData.activeModes())) {
            Optional<AbilityDefinition> maybeAbility = AbilityApi.findAbility(activeAbilityId);
            if (maybeAbility.isEmpty()) {
                updatedData = updatedData.clearAbilityState(activeAbilityId);
                continue;
            }

            AbilityDefinition ability = maybeAbility.get();
            Optional<Component> activeRequirementFailure = ability.firstFailedActiveRequirement(player, updatedData);
            if (activeRequirementFailure.isPresent()) {
                AbilityUseResult endResult = endAbility(player, updatedData, ability, AbilityEndReason.REQUIREMENT_INVALIDATED);
                updatedData = endResult.data();
                player.displayClientMessage(endResult.feedback() != null ? endResult.feedback() : activeRequirementFailure.get(), true);
                continue;
            }

            updatedData = ability.tick(player, updatedData);
            if (!updatedData.isModeActive(activeAbilityId)) {
                continue;
            }

            if (ability.durationTicks() > 0) {
                int remainingTicks = updatedData.activeDurationFor(activeAbilityId);
                if (remainingTicks <= 1) {
                    AbilityUseResult endResult = endAbility(player, updatedData, ability, AbilityEndReason.DURATION_EXPIRED);
                    updatedData = endResult.data();
                    if (endResult.feedback() != null) {
                        player.displayClientMessage(endResult.feedback(), true);
                    }
                } else {
                    updatedData = updatedData.withActiveDuration(activeAbilityId, remainingTicks - 1);
                }
            }
        }

        return updatedData;
    }

    public static int displayCooldownTicks(AbilityDefinition definition, AbilityData data) {
        if (definition.usesCharges()) {
            int charges = data.chargeCountFor(definition.id(), definition.maxCharges());
            if (charges <= 0) {
                return data.chargeRechargeFor(definition.id());
            }
            return 0;
        }

        return data.cooldownFor(definition.id());
    }

    private static AbilityData beginChargeRecharge(AbilityData data, AbilityDefinition definition) {
        int currentCharges = data.chargeCountFor(definition.id(), definition.maxCharges());
        if (currentCharges >= definition.maxCharges()) {
            return data.withChargeRecharge(definition.id(), 0).withRecoveryTickProgress(definition.id(), 0);
        }
        if (data.chargeRechargeFor(definition.id()) > 0) {
            return data;
        }
        return data.withChargeRecharge(definition.id(), definition.chargeRechargeTicks())
                .withRecoveryTickProgress(definition.id(), 0);
    }

    private static AbilityData tickChargeRecharges(ServerPlayer player, AbilityData data, double tickRate) {
        AbilityData updatedData = data;
        long tickUnits = tickUnits(tickRate);
        Set<ResourceLocation> trackedAbilities = new LinkedHashSet<>(updatedData.charges().keySet());
        trackedAbilities.addAll(updatedData.chargeRechargeTicks().keySet());
        for (ResourceLocation abilityId : trackedAbilities) {
            AbilityDefinition ability = AbilityApi.findAbility(abilityId).orElse(null);
            if (ability == null || !ability.usesCharges()) {
                updatedData = updatedData.withChargeCount(abilityId, -1)
                        .withChargeRecharge(abilityId, 0)
                        .withRecoveryTickProgress(abilityId, 0);
                continue;
            }

            int currentCharges = updatedData.chargeCountFor(abilityId, ability.maxCharges());
            int rechargeTicks = updatedData.chargeRechargeFor(abilityId);
            if (currentCharges >= ability.maxCharges()) {
                if (rechargeTicks > 0) {
                    updatedData = updatedData.withChargeRecharge(abilityId, 0).withRecoveryTickProgress(abilityId, 0);
                }
                continue;
            }

            if (rechargeTicks > 0) {
                long progressUnits = updatedData.recoveryTickProgressFor(abilityId) + tickUnits;
                int nextCharges = currentCharges;
                int remainingRechargeTicks = rechargeTicks;
                while (nextCharges < ability.maxCharges()
                        && remainingRechargeTicks > 0
                        && progressUnits >= (long) remainingRechargeTicks * 1000L) {
                    progressUnits -= (long) remainingRechargeTicks * 1000L;
                    nextCharges++;
                    remainingRechargeTicks = nextCharges < ability.maxCharges() ? ability.chargeRechargeTicks() : 0;
                }

                if (nextCharges >= ability.maxCharges()) {
                    updatedData = updatedData.withChargeCount(abilityId, nextCharges)
                            .withChargeRecharge(abilityId, 0)
                            .withRecoveryTickProgress(abilityId, 0);
                    continue;
                }

                int decrementedTicks = (int) (progressUnits / 1000L);
                int nextRechargeTicks = Math.max(0, remainingRechargeTicks - decrementedTicks);
                int remainingProgressUnits = (int) (progressUnits % 1000L);
                updatedData = updatedData.withChargeCount(abilityId, nextCharges)
                        .withChargeRecharge(abilityId, nextRechargeTicks)
                        .withRecoveryTickProgress(abilityId, remainingProgressUnits);
                continue;
            }

            if (!updatedData.isModeActive(abilityId)) {
                updatedData = updatedData.withChargeRecharge(abilityId, ability.chargeRechargeTicks())
                        .withRecoveryTickProgress(abilityId, 0);
            }
        }
        return updatedData;
    }

    private static Optional<Component> firstResourceFailure(AbilityData data, AbilityDefinition definition) {
        for (AbilityResourceCost cost : definition.resourceCosts()) {
            Optional<AbilityResourceDefinition> maybeResource = AbilityApi.findResource(cost.resourceId());
            if (maybeResource.isEmpty()) {
                return Optional.of(Component.translatable(
                        "message.xlib.resource_missing_definition",
                        definition.displayName(),
                        cost.resourceId().toString()
                ));
            }

            AbilityResourceDefinition resource = maybeResource.get();
            if (data.resourceAmount(resource.id()) < cost.amount()) {
                return Optional.of(Component.translatable("message.xlib.resource_missing", resource.displayName(), cost.amount()));
            }
        }
        return Optional.empty();
    }

    private static AbilityData spendResources(AbilityData data, AbilityDefinition definition) {
        AbilityData updatedData = data;
        for (AbilityResourceCost cost : definition.resourceCosts()) {
            Optional<AbilityResourceDefinition> maybeResource = AbilityApi.findResource(cost.resourceId());
            if (maybeResource.isEmpty()) {
                continue;
            }

            AbilityResourceDefinition resource = maybeResource.get();
            int nextAmount = Math.max(0, updatedData.resourceAmount(resource.id()) - cost.amount());
            updatedData = AbilityResourceApi.withAmount(updatedData, resource.id(), nextAmount);
            if (resource.regenAmount() > 0) {
                updatedData = updatedData.withResourceRegenDelay(resource.id(), resource.regenIntervalTicks());
            }
        }
        return updatedData;
    }

    private static String formatCooldown(int cooldownTicks) {
        if (cooldownTicks <= 0) {
            return "0.0";
        }
        return String.format(Locale.ROOT, "%.1f", cooldownTicks / 20.0F);
    }

    private static double cooldownTickRateMultiplier(ServerPlayer player, AbilityData data) {
        double multiplier = ModeApi.cooldownTickRateMultiplier(data);
        for (ResourceLocation passiveId : Set.copyOf(data.grantedPassives())) {
            PassiveDefinition passive = PassiveApi.findPassive(passiveId).orElse(null);
            if (passive == null || passive.firstFailedActiveRequirement(player, data).isPresent()) {
                continue;
            }
            multiplier *= passive.cooldownTickRateMultiplier();
        }
        return multiplier;
    }

    private static long tickUnits(double tickRate) {
        return Math.max(0L, Math.round(tickRate * 1000.0D));
    }

    private static AbilityUseResult finishActivation(
            ServerPlayer player,
            AbilityData currentData,
            AbilityDefinition definition,
            AbilityUseResult result
    ) {
        NeoForge.EVENT_BUS.post(new XLibAbilityActivationEvent.Post(player, definition, currentData, result));
        return result;
    }
}

