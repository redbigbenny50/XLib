package com.whatxe.xlib.ability;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.whatxe.xlib.binding.EntityBindingKind;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.ItemLike;

public final class AbilityRequirementJsonParser {
    @FunctionalInterface
    public interface NamedConditionResolver {
        NamedConditionResolver NONE = id -> {
            throw new IllegalArgumentException("Unknown named condition reference: " + id);
        };

        AbilityRequirement resolve(ResourceLocation id);
    }

    private AbilityRequirementJsonParser() {}

    public static AbilityRequirement parse(JsonElement element) {
        return parse(element, NamedConditionResolver.NONE);
    }

    public static AbilityRequirement parse(JsonElement element, NamedConditionResolver namedConditionResolver) {
        return parse(element, namedConditionResolver, "$");
    }

    private static AbilityRequirement parse(
            JsonElement element,
            NamedConditionResolver namedConditionResolver,
            String path
    ) {
        Objects.requireNonNull(namedConditionResolver, "namedConditionResolver");
        try {
            if (element == null || element.isJsonNull()) {
                return AbilityRequirements.always();
            }
            if (element.isJsonArray()) {
                return AbilityRequirements.all(parseArray(element.getAsJsonArray(), namedConditionResolver, path));
            }
            if (element.isJsonPrimitive()) {
                JsonPrimitive primitive = element.getAsJsonPrimitive();
                if (primitive.isBoolean()) {
                    return primitive.getAsBoolean() ? AbilityRequirements.always() : AbilityRequirements.never();
                }
                throw new IllegalArgumentException("Expected a boolean, object, or array requirement declaration");
            }

            JsonObject object = GsonHelper.convertToJsonObject(element, "requirement");
            if (!object.has("type")) {
                throw new IllegalArgumentException("Missing 'type' field");
            }
            String type = GsonHelper.getAsString(object, "type");
            return switch (type) {
                case "always" -> AbilityRequirements.always();
                case "never" -> AbilityRequirements.never();
                case "all" -> AbilityRequirements.all(parseChildren(object, "conditions", namedConditionResolver, path));
                case "any" -> AbilityRequirements.any(parseChildren(object, "conditions", namedConditionResolver, path));
                case "not" -> AbilityRequirements.not(parse(readRequiredElement(object, "condition", path), namedConditionResolver, path + ".condition"));
                case "condition_ref" -> resolveNamedCondition(object, namedConditionResolver, path);
                case "sprinting" -> AbilityRequirements.sprinting();
                case "sneaking" -> AbilityRequirements.sneaking();
                case "on_ground" -> AbilityRequirements.onGround();
                case "in_water" -> AbilityRequirements.inWater();
                case "in_lava" -> AbilityRequirements.inLava();
                case "swimming" -> AbilityRequirements.swimming();
                case "on_fire" -> AbilityRequirements.onFire();
                case "gliding" -> AbilityRequirements.gliding();
                case "under_open_sky" -> AbilityRequirements.underOpenSky();
                case "standing_on_block" -> AbilityRequirements.standingOnBlock(readId(object, "block", path));
                case "standing_on_block_tag" -> AbilityRequirements.standingOnBlockTag(
                        readId(object, object.has("block_tag") ? "block_tag" : "tag", path)
                );
                case "creative" -> AbilityRequirements.creative();
                case "spectator" -> AbilityRequirements.spectator();
                case "holding" -> AbilityRequirements.holding(readItem(object, "item", path));
                case "holding_any" -> AbilityRequirements.holdingAny(readItems(object, "item", "items", path));
                case "holding_tag" -> AbilityRequirements.holdingTag(readId(object, object.has("item_tag") ? "item_tag" : "tag", path));
                case "holding_any_tag" -> AbilityRequirements.holdingAnyTags(readIds(object, "item_tag", "item_tags", path));
                case "holding_all_tags" -> AbilityRequirements.holdingAllTags(readIds(object, null, "item_tags", path));
                case "wearing" -> AbilityRequirements.wearing(readItem(object, "item", path));
                case "wearing_any" -> AbilityRequirements.wearingAny(readItems(object, "item", "items", path));
                case "wearing_all" -> AbilityRequirements.wearingAll(readItems(object, null, "items", path));
                case "wearing_tag" -> AbilityRequirements.wearingTag(readId(object, object.has("item_tag") ? "item_tag" : "tag", path));
                case "wearing_any_tag" -> AbilityRequirements.wearingAnyTags(readIds(object, "item_tag", "item_tags", path));
                case "wearing_all_tags" -> AbilityRequirements.wearingAllTags(readIds(object, null, "item_tags", path));
                case "resource_at_least" -> parseResourceAtLeast(object, path);
                case "tracked_value_at_least" -> AbilityRequirements.trackedValueAtLeast(
                        readId(object, "value", path),
                        GsonHelper.getAsDouble(object, "amount")
                );
                case "tracked_value_at_most" -> AbilityRequirements.trackedValueAtMost(
                        readId(object, "value", path),
                        GsonHelper.getAsDouble(object, "amount")
                );
                case "tracked_value_between" -> AbilityRequirements.trackedValueBetween(
                        readId(object, "value", path),
                        GsonHelper.getAsDouble(object, "min"),
                        GsonHelper.getAsDouble(object, "max")
                );
                case "mode_active" -> AbilityRequirements.modeActive(readId(object, "mode", path));
                case "mode_inactive" -> AbilityRequirements.modeInactive(readId(object, "mode", path));
                case "has_ability" -> AbilityRequirements.hasAbility(readId(object, "ability", path));
                case "lacks_ability" -> AbilityRequirements.lacksAbility(readId(object, "ability", path));
                case "has_passive" -> AbilityRequirements.hasPassive(readId(object, "passive", path));
                case "lacks_passive" -> AbilityRequirements.lacksPassive(readId(object, "passive", path));
                case "dimension" -> AbilityRequirements.dimension(readId(object, "dimension", path));
                case "not_dimension" -> AbilityRequirements.notDimension(readId(object, "dimension", path));
                case "biome" -> AbilityRequirements.biome(readId(object, "biome", path));
                case "not_biome" -> AbilityRequirements.notBiome(readId(object, "biome", path));
                case "biome_tag" -> AbilityRequirements.biomeTag(readId(object, object.has("biome_tag") ? "biome_tag" : "tag", path));
                case "not_biome_tag" -> AbilityRequirements.notBiomeTag(readId(object, object.has("biome_tag") ? "biome_tag" : "tag", path));
                case "team" -> AbilityRequirements.team(GsonHelper.getAsString(object, "team"));
                case "not_team" -> AbilityRequirements.notTeam(GsonHelper.getAsString(object, "team"));
                case "status_effect" -> AbilityRequirements.statusEffect(readId(object, "effect", path));
                case "no_status_effect" -> AbilityRequirements.noStatusEffect(readId(object, "effect", path));
                case "counts_as_entity" -> AbilityRequirements.countsAsEntity(readId(object, "entity", path));
                case "counts_as_entity_tag" -> AbilityRequirements.countsAsEntityTag(
                        readId(object, object.has("entity_tag") ? "entity_tag" : "tag", path)
                );
                case "health_at_least" -> AbilityRequirements.healthAtLeast(GsonHelper.getAsDouble(object, "amount"));
                case "health_at_most" -> AbilityRequirements.healthAtMost(GsonHelper.getAsDouble(object, "amount"));
                case "food_at_least" -> AbilityRequirements.foodAtLeast(GsonHelper.getAsInt(object, "amount"));
                case "food_at_most" -> AbilityRequirements.foodAtMost(GsonHelper.getAsInt(object, "amount"));
                case "xp_level_at_least" -> AbilityRequirements.xpLevelAtLeast(GsonHelper.getAsInt(object, "level"));
                case "xp_level_at_most" -> AbilityRequirements.xpLevelAtMost(GsonHelper.getAsInt(object, "level"));
                case "raining" -> AbilityRequirements.raining();
                case "thundering" -> AbilityRequirements.thundering();
                case "clear_weather" -> AbilityRequirements.clearWeather();
                case "daytime" -> AbilityRequirements.daytime();
                case "nighttime" -> AbilityRequirements.nighttime();
                case "time_between" -> AbilityRequirements.timeBetween(
                        GsonHelper.getAsLong(object, "start"),
                        GsonHelper.getAsLong(object, "end")
                );
                case "mark_active" -> AbilityRequirements.markActive(readId(object, "mark", path));
                case "mark_stacks_at_least" -> AbilityRequirements.markStacksAtLeast(
                        readId(object, "mark", path),
                        GsonHelper.getAsInt(object, "amount")
                );
                case "mark_value_at_least" -> AbilityRequirements.markValueAtLeast(
                        readId(object, "mark", path),
                        GsonHelper.getAsDouble(object, "amount")
                );
                case "recently_hurt_within" -> AbilityRequirements.recentlyHurtWithin(GsonHelper.getAsInt(object, "ticks"));
                case "cooldown_ready" -> AbilityRequirements.cooldownReady(readId(object, "ability", path));
                case "combo_window_active" -> AbilityRequirements.comboWindowActive(readId(object, "ability", path));
                case "detector_active" -> AbilityRequirements.detectorActive(readId(object, "detector", path));
                case "state_policy_active" -> AbilityRequirements.statePolicyActive(readId(object, "state_policy", path));
                case "state_flag_active" -> AbilityRequirements.stateFlagActive(readId(object, "state_flag", path));
                case "identity_active" -> AbilityRequirements.identityActive(readId(object, "identity", path));
                case "artifact_active" -> AbilityRequirements.artifactActive(readId(object, "artifact", path));
                case "artifact_unlocked" -> AbilityRequirements.artifactUnlocked(readId(object, "artifact", path));
                case "capability_policy_active" -> AbilityRequirements.capabilityPolicyActive(readId(object, "policy", path));
                case "lifecycle_stage_active" -> AbilityRequirements.lifecycleStageActive(readId(object, "stage", path));
                case "visual_form_active" -> AbilityRequirements.visualFormActive(readId(object, "form", path));
                case "binding_active" -> AbilityRequirements.bindingActive(readId(object, "binding", path));
                case "binding_kind_active" -> AbilityRequirements.bindingKindActive(
                        parseBindingKind(GsonHelper.getAsString(object, "kind"), path)
                );
                case "body_transition_active" -> AbilityRequirements.bodyTransitionActive();
                case "score_at_least" -> AbilityRequirements.scoreAtLeast(
                        GsonHelper.getAsString(object, "objective"),
                        GsonHelper.getAsInt(object, "value")
                );
                case "score_at_most" -> AbilityRequirements.scoreAtMost(
                        GsonHelper.getAsString(object, "objective"),
                        GsonHelper.getAsInt(object, "value")
                );
                case "score_between" -> AbilityRequirements.scoreBetween(
                        GsonHelper.getAsString(object, "objective"),
                        GsonHelper.getAsInt(object, "min"),
                        GsonHelper.getAsInt(object, "max")
                );
                case "counter_at_least" -> AbilityRequirements.counterAtLeast(
                        readId(object, "counter", path),
                        GsonHelper.getAsInt(object, "amount")
                );
                case "counter_at_most" -> AbilityRequirements.counterAtMost(
                        readId(object, "counter", path),
                        GsonHelper.getAsInt(object, "amount")
                );
                case "counter_between" -> AbilityRequirements.counterBetween(
                        readId(object, "counter", path),
                        GsonHelper.getAsInt(object, "min"),
                        GsonHelper.getAsInt(object, "max")
                );
                default -> throw new IllegalArgumentException("Unknown requirement type: " + type);
            };
        } catch (RuntimeException exception) {
            throw withPath(path, exception);
        }
    }

    public static Map<ResourceLocation, AbilityRequirement> compileDefinitions(Map<ResourceLocation, JsonElement> rawDefinitions) {
        Map<ResourceLocation, JsonElement> copiedDefinitions = Map.copyOf(rawDefinitions);
        Map<ResourceLocation, AbilityRequirement> compiledDefinitions = new LinkedHashMap<>();
        LinkedHashSet<ResourceLocation> resolutionPath = new LinkedHashSet<>();
        for (ResourceLocation id : copiedDefinitions.keySet()) {
            compileDefinition(id, copiedDefinitions, compiledDefinitions, resolutionPath);
        }
        return Map.copyOf(compiledDefinitions);
    }

    private static AbilityRequirement compileDefinition(
            ResourceLocation id,
            Map<ResourceLocation, JsonElement> rawDefinitions,
            Map<ResourceLocation, AbilityRequirement> compiledDefinitions,
            LinkedHashSet<ResourceLocation> resolutionPath
    ) {
        AbilityRequirement existing = compiledDefinitions.get(id);
        if (existing != null) {
            return existing;
        }
        if (!resolutionPath.add(id)) {
            throw new IllegalArgumentException("Circular named condition reference: " + joinResolutionPath(resolutionPath, id));
        }
        JsonElement element = rawDefinitions.get(id);
        if (element == null) {
            throw new IllegalArgumentException("Unknown named condition definition: " + id
                    + " (resolution path: " + joinResolutionPath(resolutionPath, null) + ")");
        }
        try {
            AbilityRequirement compiled = parse(element, referenceId -> compileDefinition(
                    referenceId,
                    rawDefinitions,
                    compiledDefinitions,
                    resolutionPath
            ));
            compiledDefinitions.put(id, compiled);
            return compiled;
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Failed to compile named condition " + id + ": " + exception.getMessage(), exception);
        } finally {
            resolutionPath.remove(id);
        }
    }

    private static String joinResolutionPath(Set<ResourceLocation> resolutionPath, ResourceLocation repeatedId) {
        List<String> segments = new ArrayList<>();
        for (ResourceLocation id : resolutionPath) {
            segments.add(id.toString());
        }
        if (repeatedId != null) {
            segments.add(repeatedId.toString());
        }
        return String.join(" -> ", segments);
    }

    private static List<AbilityRequirement> parseArray(
            JsonArray array,
            NamedConditionResolver namedConditionResolver,
            String path
    ) {
        List<AbilityRequirement> requirements = new ArrayList<>();
        for (int index = 0; index < array.size(); index++) {
            requirements.add(parse(array.get(index), namedConditionResolver, path + "[" + index + "]"));
        }
        if (requirements.isEmpty()) {
            throw new IllegalArgumentException("Requirement arrays must contain at least one child condition");
        }
        return List.copyOf(requirements);
    }

    private static List<AbilityRequirement> parseChildren(
            JsonObject object,
            String key,
            NamedConditionResolver namedConditionResolver,
            String path
    ) {
        if (!object.has(key)) {
            throw new IllegalArgumentException("Missing '" + key + "' array");
        }
        return parseArray(GsonHelper.getAsJsonArray(object, key), namedConditionResolver, path + "." + key);
    }

    private static AbilityRequirement parseResourceAtLeast(JsonObject object, String path) {
        ResourceLocation resourceId = readId(object, "resource", path);
        double amount = GsonHelper.getAsDouble(object, "amount");
        if (Math.rint(amount) == amount) {
            return AbilityRequirements.resourceAtLeast(resourceId, (int) amount);
        }
        return AbilityRequirements.resourceAtLeastExact(resourceId, amount);
    }

    private static AbilityRequirement resolveNamedCondition(
            JsonObject object,
            NamedConditionResolver namedConditionResolver,
            String path
    ) {
        String key = object.has("id") ? "id" : "condition";
        ResourceLocation conditionId = readId(object, key, path);
        try {
            return namedConditionResolver.resolve(conditionId);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Failed to resolve named condition " + conditionId + ": " + exception.getMessage(), exception);
        }
    }

    private static JsonElement readRequiredElement(JsonObject object, String key, String path) {
        if (!object.has(key)) {
            throw new IllegalArgumentException("Missing '" + key + "' field");
        }
        return object.get(key);
    }

    private static ResourceLocation readId(JsonObject object, String key, String path) {
        if (!object.has(key)) {
            throw new IllegalArgumentException("Missing '" + key + "' field");
        }
        try {
            return ResourceLocation.parse(GsonHelper.getAsString(object, key));
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Invalid resource id in '" + key + "': " + exception.getMessage(), exception);
        }
    }

    private static ItemLike readItem(JsonObject object, String key, String path) {
        return BuiltInRegistries.ITEM.getOptional(readId(object, key, path))
                .orElseThrow(() -> new IllegalArgumentException("Unknown item id in requirement: " + GsonHelper.getAsString(object, key)));
    }

    private static List<ResourceLocation> readIds(JsonObject object, String singleKey, String pluralKey, String path) {
        List<ResourceLocation> values = new ArrayList<>();
        if (singleKey != null && object.has(singleKey)) {
            values.add(readId(object, singleKey, path));
        }
        if (pluralKey != null && object.has(pluralKey)) {
            JsonArray array = GsonHelper.getAsJsonArray(object, pluralKey);
            for (int index = 0; index < array.size(); index++) {
                try {
                    values.add(ResourceLocation.parse(array.get(index).getAsString()));
                } catch (RuntimeException exception) {
                    throw new IllegalArgumentException("Invalid resource id in '" + pluralKey + "[" + index + "]': " + exception.getMessage(), exception);
                }
            }
        }
        if (values.isEmpty()) {
            throw new IllegalArgumentException("Expected at least one id entry in '" + (pluralKey != null ? pluralKey : singleKey) + "'");
        }
        return List.copyOf(values);
    }

    private static List<ItemLike> readItems(JsonObject object, String singleKey, String pluralKey, String path) {
        List<ItemLike> items = new ArrayList<>();
        if (singleKey != null && object.has(singleKey)) {
            items.add(readItem(object, singleKey, path));
        }
        if (pluralKey != null && object.has(pluralKey)) {
            JsonArray array = GsonHelper.getAsJsonArray(object, pluralKey);
            for (int index = 0; index < array.size(); index++) {
                ResourceLocation itemId;
                try {
                    itemId = ResourceLocation.parse(array.get(index).getAsString());
                } catch (RuntimeException exception) {
                    throw new IllegalArgumentException("Invalid resource id in '" + pluralKey + "[" + index + "]': " + exception.getMessage(), exception);
                }
                items.add(BuiltInRegistries.ITEM.getOptional(itemId)
                        .orElseThrow(() -> new IllegalArgumentException("Unknown item id in requirement: " + itemId)));
            }
        }
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Expected at least one item entry in '" + (pluralKey != null ? pluralKey : singleKey) + "'");
        }
        return List.copyOf(items);
    }

    private static EntityBindingKind parseBindingKind(String value, String path) {
        try {
            return EntityBindingKind.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown binding kind '" + value + "' at " + path + ".kind. Valid values: "
                    + java.util.Arrays.stream(EntityBindingKind.values())
                            .map(k -> k.name().toLowerCase(Locale.ROOT))
                            .collect(java.util.stream.Collectors.joining(", ")));
        }
    }

    private static RequirementParseException withPath(String path, RuntimeException exception) {
        if (exception instanceof RequirementParseException parseException) {
            return parseException;
        }
        return new RequirementParseException("Invalid requirement at " + path + ": " + exception.getMessage(), exception);
    }

    private static final class RequirementParseException extends IllegalArgumentException {
        private RequirementParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
