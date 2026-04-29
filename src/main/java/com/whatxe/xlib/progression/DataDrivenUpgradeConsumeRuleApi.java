package com.whatxe.xlib.progression;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.whatxe.xlib.XLib;
import com.whatxe.xlib.ability.AbilityRequirement;
import com.whatxe.xlib.ability.AbilityRequirementJsonParser;
import com.whatxe.xlib.ability.DataDrivenConditionApi;
import com.whatxe.xlib.ability.DataDrivenDefinitionReaders;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

public final class DataDrivenUpgradeConsumeRuleApi {
    private static final Gson GSON = new GsonBuilder().create();
    private static final String DIRECTORY = XLib.MODID + "/upgrade_consume_rules";
    private static volatile Map<ResourceLocation, LoadedUpgradeConsumeRule> loadedDefinitions = Map.of();

    private DataDrivenUpgradeConsumeRuleApi() {}

    public static void bootstrap() {}

    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new ReloadListener());
    }

    public static Collection<ResourceLocation> allDefinitionIds() {
        return List.copyOf(loadedDefinitions.keySet());
    }

    public static Optional<LoadedUpgradeConsumeRule> findDefinition(ResourceLocation definitionId) {
        return Optional.ofNullable(loadedDefinitions.get(definitionId));
    }

    static void setDefinitionsForTesting(Map<ResourceLocation, LoadedUpgradeConsumeRule> definitions) {
        loadedDefinitions = Map.copyOf(definitions);
    }

    static LoadedUpgradeConsumeRule parseDefinition(ResourceLocation fileId, JsonElement element) {
        JsonObject object = GsonHelper.convertToJsonObject(element, "upgrade consume rule");
        ResourceLocation ruleId = object.has("id")
                ? ResourceLocation.parse(GsonHelper.getAsString(object, "id"))
                : fileId;
        UpgradeConsumeRule.Builder builder = UpgradeConsumeRule.builder(ruleId);
        for (ResourceLocation itemId : DataDrivenDefinitionReaders.readLocations(object, "item", "items")) {
            builder.itemId(itemId);
        }
        for (ResourceLocation tagId : DataDrivenDefinitionReaders.readLocations(object, "item_tag", "item_tags")) {
            builder.itemTag(TagKey.create(Registries.ITEM, tagId));
        }
        if (object.has("food_only") && GsonHelper.getAsBoolean(object, "food_only")) {
            builder.foodOnly();
        }
        for (AbilityRequirement requirement : readRequirements(object)) {
            builder.requirement(requirement);
        }
        for (Map.Entry<ResourceLocation, Integer> entry : DataDrivenUpgradeNodeApi.readIntMap(object, "point_rewards", true).entrySet()) {
            builder.awardPoints(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<ResourceLocation, Integer> entry : DataDrivenUpgradeNodeApi.readIntMap(object, "counter_rewards", true).entrySet()) {
            builder.incrementCounter(entry.getKey(), entry.getValue());
        }
        return new LoadedUpgradeConsumeRule(ruleId, builder.build());
    }

    public record LoadedUpgradeConsumeRule(
            ResourceLocation id,
            UpgradeConsumeRule definition
    ) {}

    private static List<AbilityRequirement> readRequirements(JsonObject object) {
        List<AbilityRequirement> requirements = new ArrayList<>();
        if (object.has("condition")) {
            requirements.add(AbilityRequirementJsonParser.parse(object.get("condition"), DataDrivenConditionApi::requireCondition));
        }
        if (object.has("conditions")) {
            for (JsonElement element : GsonHelper.getAsJsonArray(object, "conditions")) {
                requirements.add(AbilityRequirementJsonParser.parse(element, DataDrivenConditionApi::requireCondition));
            }
        }
        return List.copyOf(requirements);
    }

    private static final class ReloadListener extends SimpleJsonResourceReloadListener {
        private ReloadListener() {
            super(GSON, DIRECTORY);
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> jsonById, ResourceManager resourceManager, ProfilerFiller profiler) {
            Map<ResourceLocation, LoadedUpgradeConsumeRule> definitions = new LinkedHashMap<>();
            for (Map.Entry<ResourceLocation, JsonElement> entry : jsonById.entrySet()) {
                try {
                    LoadedUpgradeConsumeRule definition = parseDefinition(entry.getKey(), entry.getValue());
                    LoadedUpgradeConsumeRule previous = definitions.putIfAbsent(definition.id(), definition);
                    if (previous != null) {
                        throw new IllegalStateException("Duplicate datapack upgrade consume rule id: " + definition.id());
                    }
                } catch (RuntimeException exception) {
                    XLib.LOGGER.error("Failed to parse datapack upgrade consume rule {}", entry.getKey(), exception);
                }
            }
            loadedDefinitions = Map.copyOf(definitions);
        }
    }
}
