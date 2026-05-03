package com.whatxe.xlib.ability;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.whatxe.xlib.combat.CombatTargetingMode;
import com.whatxe.xlib.combat.CombatTargetingProfile;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

final class DataDrivenRuntimeEffects {
    @FunctionalInterface
    interface Effect {
        AbilityData apply(@Nullable ServerPlayer player, AbilityData data, ResourceLocation sourceId);
    }

    private DataDrivenRuntimeEffects() {}

    static List<AbilityRequirement> readRequirements(JsonObject object, String singleKey, String pluralKey) {
        List<AbilityRequirement> requirements = new ArrayList<>();
        if (singleKey != null && object.has(singleKey)) {
            requirements.add(AbilityRequirementJsonParser.parse(object.get(singleKey), DataDrivenConditionApi::requireCondition));
        }
        if (pluralKey != null && object.has(pluralKey)) {
            for (JsonElement element : GsonHelper.getAsJsonArray(object, pluralKey)) {
                requirements.add(AbilityRequirementJsonParser.parse(element, DataDrivenConditionApi::requireCondition));
            }
        }
        return List.copyOf(requirements);
    }

    static Map<AbilitySoundTrigger, List<AbilitySound>> readAbilitySounds(JsonObject object) {
        if (!object.has("sounds")) {
            return Map.of();
        }
        Map<AbilitySoundTrigger, List<AbilitySound>> sounds = new EnumMap<>(AbilitySoundTrigger.class);
        JsonObject soundObject = GsonHelper.getAsJsonObject(object, "sounds");
        for (Map.Entry<String, JsonElement> entry : soundObject.entrySet()) {
            AbilitySoundTrigger trigger = AbilitySoundTrigger.valueOf(entry.getKey().trim().toUpperCase(Locale.ROOT));
            sounds.put(trigger, parseSounds(entry.getValue()));
        }
        return Map.copyOf(sounds);
    }

    static Map<PassiveSoundTrigger, List<AbilitySound>> readPassiveSounds(JsonObject object) {
        if (!object.has("sounds")) {
            return Map.of();
        }
        Map<PassiveSoundTrigger, List<AbilitySound>> sounds = new EnumMap<>(PassiveSoundTrigger.class);
        JsonObject soundObject = GsonHelper.getAsJsonObject(object, "sounds");
        for (Map.Entry<String, JsonElement> entry : soundObject.entrySet()) {
            PassiveSoundTrigger trigger = PassiveSoundTrigger.valueOf(entry.getKey().trim().toUpperCase(Locale.ROOT));
            sounds.put(trigger, parseSounds(entry.getValue()));
        }
        return Map.copyOf(sounds);
    }

    static AbilityDefinition.AbilityAction parseAbilityAction(ResourceLocation abilityId, JsonObject object) {
        JsonObject actionObject = GsonHelper.getAsJsonObject(object, "action");
        String type = GsonHelper.getAsString(actionObject, "type");
        AbilityDefinition.AbilityAction baseAction = switch (type) {
            case "success" -> (player, data) -> AbilityUseResult.success(data);
            case "melee_strike" -> AbilityActions.meleeStrike(
                    abilityId,
                    readTargetingProfile(actionObject),
                    (float) GsonHelper.getAsDouble(actionObject, "damage"),
                    readMissBehavior(actionObject, AbilityActions.MissBehavior.CONSUME)
            );
            case "dash_behind_and_strike" -> AbilityActions.dashBehindAndStrike(
                    abilityId,
                    readTargetingProfile(actionObject),
                    GsonHelper.getAsDouble(actionObject, "behind_distance"),
                    (float) GsonHelper.getAsDouble(actionObject, "damage"),
                    readMissBehavior(actionObject, AbilityActions.MissBehavior.CONSUME)
            );
            case "launch_target" -> AbilityActions.launchTarget(
                    abilityId,
                    readTargetingProfile(actionObject),
                    (float) GsonHelper.getAsDouble(actionObject, "damage"),
                    GsonHelper.getAsDouble(actionObject, "vertical_boost"),
                    readMissBehavior(actionObject, AbilityActions.MissBehavior.CONSUME)
            );
            case "counter_strike" -> AbilityActions.counterStrike(
                    abilityId,
                    GsonHelper.getAsInt(actionObject, "recent_hit_window_ticks"),
                    (float) GsonHelper.getAsDouble(actionObject, "minimum_damage"),
                    GsonHelper.getAsDouble(actionObject, "reflected_damage_multiplier"),
                    readMissBehavior(actionObject, AbilityActions.MissBehavior.FAIL)
            );
            default -> throw new IllegalArgumentException("Unknown ability action type: " + type);
        };
        return wrapAction(abilityId, baseAction, readEffects(actionObject, "effects"));
    }

    static AbilityDefinition.AbilityTicker tickerFromEffects(ResourceLocation sourceId, List<Effect> effects) {
        if (effects.isEmpty()) {
            return (player, data) -> data;
        }
        return (player, data) -> applyAll(player, data, sourceId, effects);
    }

    static AbilityDefinition.AbilityEnder enderFromEffects(ResourceLocation sourceId, List<Effect> effects) {
        if (effects.isEmpty()) {
            return (player, data, reason) -> AbilityUseResult.success(data);
        }
        return (player, data, reason) -> AbilityUseResult.success(applyAll(player, data, sourceId, effects));
    }

    static PassiveDefinition.PassiveTicker passiveTickerFromEffects(ResourceLocation sourceId, List<Effect> effects) {
        if (effects.isEmpty()) {
            return (player, data) -> data;
        }
        return (player, data) -> applyAll(player, data, sourceId, effects);
    }

    static PassiveDefinition.PassiveAction passiveActionFromEffects(ResourceLocation sourceId, List<Effect> effects) {
        if (effects.isEmpty()) {
            return (player, data) -> data;
        }
        return (player, data) -> applyAll(player, data, sourceId, effects);
    }

    static PassiveDefinition.PassiveHitAction passiveHitActionFromEffects(ResourceLocation sourceId, List<Effect> effects) {
        if (effects.isEmpty()) {
            return (player, data, target) -> data;
        }
        return (player, data, target) -> applyAll(player, data, sourceId, effects);
    }

    static PassiveDefinition.PassiveHurtAction passiveHurtActionFromEffects(ResourceLocation sourceId, List<Effect> effects) {
        if (effects.isEmpty()) {
            return (player, data, source, amount) -> data;
        }
        return (player, data, source, amount) -> applyAll(player, data, sourceId, effects);
    }

    static PassiveDefinition.PassiveEatAction passiveEatActionFromEffects(ResourceLocation sourceId, List<Effect> effects) {
        if (effects.isEmpty()) {
            return (player, data, stack) -> data;
        }
        return (player, data, stack) -> applyAll(player, data, sourceId, effects);
    }

    static PassiveDefinition.PassiveBlockBreakAction passiveBlockBreakActionFromEffects(ResourceLocation sourceId, List<Effect> effects) {
        if (effects.isEmpty()) {
            return (player, data, state, pos) -> data;
        }
        return (player, data, state, pos) -> applyAll(player, data, sourceId, effects);
    }

    static PassiveDefinition.PassiveArmorChangeAction passiveArmorChangeActionFromEffects(ResourceLocation sourceId, List<Effect> effects) {
        if (effects.isEmpty()) {
            return (player, data, slot, from, to) -> data;
        }
        return (player, data, slot, from, to) -> applyAll(player, data, sourceId, effects);
    }

    static List<Effect> readEffects(JsonObject object, String key) {
        if (!object.has(key)) {
            return List.of();
        }
        return parseEffects(object.get(key));
    }

    private static List<AbilitySound> parseSounds(JsonElement element) {
        List<AbilitySound> sounds = new ArrayList<>();
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                sounds.add(parseSound(child));
            }
        } else {
            sounds.add(parseSound(element));
        }
        return List.copyOf(sounds);
    }

    private static AbilitySound parseSound(JsonElement element) {
        if (element instanceof JsonPrimitive primitive && primitive.isString()) {
            return AbilitySound.of(ResourceLocation.parse(primitive.getAsString()));
        }
        JsonObject object = GsonHelper.convertToJsonObject(element, "sound");
        ResourceLocation soundId = ResourceLocation.parse(GsonHelper.getAsString(object, object.has("id") ? "id" : "sound"));
        float volume = object.has("volume") ? (float) GsonHelper.getAsDouble(object, "volume") : 1.0F;
        float pitch = object.has("pitch") ? (float) GsonHelper.getAsDouble(object, "pitch") : 1.0F;
        return new AbilitySound(soundId, volume, pitch);
    }

    private static AbilityDefinition.AbilityAction wrapAction(
            ResourceLocation sourceId,
            AbilityDefinition.AbilityAction baseAction,
            List<Effect> effects
    ) {
        if (effects.isEmpty()) {
            return baseAction;
        }
        return (player, data) -> {
            AbilityUseResult result = baseAction.activate(player, data);
            if (!result.consumed()) {
                return result;
            }
            AbilityData updatedData = applyAll(player, result.data(), sourceId, effects);
            return result.feedback() == null
                    ? AbilityUseResult.success(updatedData)
                    : AbilityUseResult.success(updatedData, result.feedback());
        };
    }

    private static AbilityData applyAll(
            @Nullable ServerPlayer player,
            AbilityData data,
            ResourceLocation sourceId,
            List<Effect> effects
    ) {
        AbilityData updatedData = data;
        for (Effect effect : effects) {
            updatedData = effect.apply(player, updatedData, sourceId);
        }
        return AbilityGrantApi.sanitize(AbilityApi.sanitizeData(updatedData));
    }

    private static List<Effect> parseEffects(JsonElement element) {
        List<Effect> effects = new ArrayList<>();
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                effects.add(parseEffect(child));
            }
        } else {
            effects.add(parseEffect(element));
        }
        return List.copyOf(effects);
    }

    private static Effect parseEffect(JsonElement element) {
        JsonObject object = GsonHelper.convertToJsonObject(element, "effect");
        String type = GsonHelper.getAsString(object, "type");
        return switch (type) {
            case "resource_delta" -> {
                ResourceLocation resourceId = ResourceLocation.parse(GsonHelper.getAsString(object, "resource"));
                double amount = GsonHelper.getAsDouble(object, "amount");
                yield (player, data, sourceId) -> AbilityResourceApi.addAmountExact(data, resourceId, amount);
            }
            case "set_detector_window" -> {
                ResourceLocation detectorId = ResourceLocation.parse(GsonHelper.getAsString(object, "detector"));
                int ticks = GsonHelper.getAsInt(object, "ticks");
                yield (player, data, sourceId) -> data.withDetectorWindow(detectorId, ticks);
            }
            case "set_combo_window" -> {
                ResourceLocation abilityId = ResourceLocation.parse(GsonHelper.getAsString(object, "ability"));
                int ticks = GsonHelper.getAsInt(object, "ticks");
                yield (player, data, sourceId) -> data.withComboWindow(abilityId, ticks);
            }
            case "set_cooldown" -> {
                ResourceLocation abilityId = ResourceLocation.parse(GsonHelper.getAsString(object, "ability"));
                int ticks = GsonHelper.getAsInt(object, "ticks");
                yield (player, data, sourceId) -> data.withCooldown(abilityId, ticks);
            }
            case "set_charge_count" -> {
                ResourceLocation abilityId = ResourceLocation.parse(GsonHelper.getAsString(object, "ability"));
                int count = GsonHelper.getAsInt(object, "count");
                yield (player, data, sourceId) -> data.withChargeCount(abilityId, count);
            }
            case "set_charge_recharge" -> {
                ResourceLocation abilityId = ResourceLocation.parse(GsonHelper.getAsString(object, "ability"));
                int ticks = GsonHelper.getAsInt(object, "ticks");
                yield (player, data, sourceId) -> data.withChargeRecharge(abilityId, ticks);
            }
            case "grant_ability" -> grantAbilityEffect(object, true);
            case "revoke_ability" -> grantAbilityEffect(object, false);
            case "grant_passive" -> grantPassiveEffect(object, true);
            case "revoke_passive" -> grantPassiveEffect(object, false);
            case "block_ability" -> blockAbilityEffect(object, true);
            case "unblock_ability" -> blockAbilityEffect(object, false);
            case "grant_state_policy" -> statePolicyEffect(object, true);
            case "revoke_state_policy" -> statePolicyEffect(object, false);
            case "grant_state_flag" -> stateFlagEffect(object, true);
            case "revoke_state_flag" -> stateFlagEffect(object, false);
            case "reset_cycle_group" -> {
                Set<ResourceLocation> cycleGroupIds = DataDrivenDefinitionReaders.readLocations(object, "cycle_group", "cycle_groups");
                if (cycleGroupIds.isEmpty()) {
                    throw new IllegalArgumentException("reset_cycle_group requires 'cycle_group' or 'cycle_groups'");
                }
                yield (player, data, sourceId) -> {
                    AbilityData updatedData = data;
                    for (ResourceLocation cycleGroupId : cycleGroupIds) {
                        updatedData = updatedData.clearModeCycleGroup(cycleGroupId);
                    }
                    return updatedData;
                };
            }
            default -> throw new IllegalArgumentException("Unknown runtime effect type: " + type);
        };
    }

    private static Effect grantAbilityEffect(JsonObject object, boolean granted) {
        Set<ResourceLocation> abilityIds = DataDrivenDefinitionReaders.readLocations(object, "ability", "abilities");
        if (abilityIds.isEmpty()) {
            throw new IllegalArgumentException("Ability grant effects require 'ability' or 'abilities'");
        }
        return (player, data, sourceId) -> {
            AbilityData updatedData = data;
            for (ResourceLocation abilityId : abilityIds) {
                updatedData = updatedData.withAbilityGrantSource(abilityId, sourceId, granted);
            }
            return updatedData;
        };
    }

    private static Effect grantPassiveEffect(JsonObject object, boolean granted) {
        Set<ResourceLocation> passiveIds = DataDrivenDefinitionReaders.readLocations(object, "passive", "passives");
        if (passiveIds.isEmpty()) {
            throw new IllegalArgumentException("Passive grant effects require 'passive' or 'passives'");
        }
        return (player, data, sourceId) -> {
            AbilityData updatedData = data;
            for (ResourceLocation passiveId : passiveIds) {
                updatedData = applyPassiveGrantSourceChange(player, updatedData, passiveId, sourceId, granted);
            }
            return updatedData;
        };
    }

    private static Effect blockAbilityEffect(JsonObject object, boolean blocked) {
        Set<ResourceLocation> abilityIds = DataDrivenDefinitionReaders.readLocations(object, "ability", "abilities");
        if (abilityIds.isEmpty()) {
            throw new IllegalArgumentException("Ability block effects require 'ability' or 'abilities'");
        }
        return (player, data, sourceId) -> {
            AbilityData updatedData = data;
            for (ResourceLocation abilityId : abilityIds) {
                updatedData = updatedData.withAbilityActivationBlockSource(abilityId, sourceId, blocked);
            }
            return updatedData;
        };
    }

    private static Effect statePolicyEffect(JsonObject object, boolean active) {
        Set<ResourceLocation> policyIds = DataDrivenDefinitionReaders.readLocations(object, "state_policy", "state_policies");
        if (policyIds.isEmpty()) {
            throw new IllegalArgumentException("State policy effects require 'state_policy' or 'state_policies'");
        }
        return (player, data, sourceId) -> {
            AbilityData updatedData = data;
            for (ResourceLocation policyId : policyIds) {
                updatedData = updatedData.withStatePolicySource(policyId, sourceId, active);
            }
            return updatedData;
        };
    }

    private static Effect stateFlagEffect(JsonObject object, boolean active) {
        Set<ResourceLocation> flagIds = DataDrivenDefinitionReaders.readLocations(object, "state_flag", "state_flags");
        if (flagIds.isEmpty()) {
            throw new IllegalArgumentException("State flag effects require 'state_flag' or 'state_flags'");
        }
        return (player, data, sourceId) -> {
            AbilityData updatedData = data;
            for (ResourceLocation flagId : flagIds) {
                updatedData = updatedData.withStateFlagSource(flagId, sourceId, active);
            }
            return updatedData;
        };
    }

    private static AbilityData applyPassiveGrantSourceChange(
            @Nullable ServerPlayer player,
            AbilityData currentData,
            ResourceLocation passiveId,
            ResourceLocation sourceId,
            boolean granted
    ) {
        boolean wasGranted = currentData.hasPassive(passiveId);
        AbilityData updatedData = currentData.withPassiveGrantSource(passiveId, sourceId, granted);
        boolean isGranted = updatedData.hasPassive(passiveId);

        if (player == null || wasGranted == isGranted) {
            return updatedData;
        }

        PassiveDefinition passive = PassiveApi.findPassive(passiveId).orElse(null);
        if (passive == null) {
            return updatedData;
        }

        if (isGranted) {
            if (passive.firstFailedGrantRequirement(player, updatedData).isPresent()) {
                return currentData;
            }
            AbilityData grantedData = passive.onGranted(player, updatedData);
            passive.playSounds(player, PassiveSoundTrigger.GRANTED);
            return grantedData;
        }

        AbilityData revokedData = passive.onRevoked(player, updatedData);
        passive.playSounds(player, PassiveSoundTrigger.REVOKED);
        return revokedData;
    }

    private static CombatTargetingProfile readTargetingProfile(JsonObject object) {
        JsonObject targetingObject = object.has("targeting")
                ? GsonHelper.getAsJsonObject(object, "targeting")
                : object;
        CombatTargetingProfile.Builder builder = CombatTargetingProfile.builder();
        if (targetingObject.has("mode")) {
            builder.mode(CombatTargetingMode.valueOf(GsonHelper.getAsString(targetingObject, "mode").trim().toUpperCase(Locale.ROOT)));
        }
        if (targetingObject.has("range")) {
            builder.range(GsonHelper.getAsDouble(targetingObject, "range"));
        }
        if (targetingObject.has("radius")) {
            builder.radius(GsonHelper.getAsDouble(targetingObject, "radius"));
        }
        if (targetingObject.has("angle_degrees")) {
            builder.angleDegrees(GsonHelper.getAsDouble(targetingObject, "angle_degrees"));
        }
        if (targetingObject.has("vertical_range")) {
            builder.verticalRange(GsonHelper.getAsDouble(targetingObject, "vertical_range"));
        }
        if (targetingObject.has("require_line_of_sight")) {
            builder.requireLineOfSight(GsonHelper.getAsBoolean(targetingObject, "require_line_of_sight"));
        }
        if (targetingObject.has("max_targets")) {
            builder.maxTargets(GsonHelper.getAsInt(targetingObject, "max_targets"));
        }
        return builder.build();
    }

    private static AbilityActions.MissBehavior readMissBehavior(
            JsonObject object,
            AbilityActions.MissBehavior fallback
    ) {
        if (!object.has("miss_behavior")) {
            return fallback;
        }
        return AbilityActions.MissBehavior.valueOf(GsonHelper.getAsString(object, "miss_behavior").trim().toUpperCase(Locale.ROOT));
    }
}
