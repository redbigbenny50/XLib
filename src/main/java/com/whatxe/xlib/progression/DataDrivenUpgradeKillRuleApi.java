package com.whatxe.xlib.progression;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.whatxe.xlib.XLib;
import com.whatxe.xlib.ability.DataDrivenDefinitionReaders;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

public final class DataDrivenUpgradeKillRuleApi {
    private static final Gson GSON = new GsonBuilder().create();
    private static final String DIRECTORY = XLib.MODID + "/upgrade_kill_rules";
    private static volatile Map<ResourceLocation, LoadedUpgradeKillRule> loadedDefinitions = Map.of();

    private DataDrivenUpgradeKillRuleApi() {}

    public static void bootstrap() {}

    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new ReloadListener());
    }

    public static Collection<ResourceLocation> allDefinitionIds() {
        return List.copyOf(loadedDefinitions.keySet());
    }

    public static Optional<LoadedUpgradeKillRule> findDefinition(ResourceLocation definitionId) {
        return Optional.ofNullable(loadedDefinitions.get(definitionId));
    }

    static void setDefinitionsForTesting(Map<ResourceLocation, LoadedUpgradeKillRule> definitions) {
        loadedDefinitions = Map.copyOf(definitions);
    }

    static LoadedUpgradeKillRule parseDefinition(ResourceLocation fileId, JsonElement element) {
        JsonObject object = GsonHelper.convertToJsonObject(element, "upgrade kill rule");
        ResourceLocation ruleId = object.has("id")
                ? ResourceLocation.parse(GsonHelper.getAsString(object, "id"))
                : fileId;
        UpgradeKillRule.Builder builder = UpgradeKillRule.builder(ruleId);
        builder.targets(DataDrivenDefinitionReaders.readLocations(object, "target", "targets"));
        for (ResourceLocation tagId : DataDrivenDefinitionReaders.readLocations(object, "target_tag", "target_tags")) {
            builder.targetTag(TagKey.create(Registries.ENTITY_TYPE, tagId));
        }
        if (object.has("required_ability")) {
            builder.requiredAbility(ResourceLocation.parse(GsonHelper.getAsString(object, "required_ability")));
        }
        for (Map.Entry<ResourceLocation, Integer> entry : DataDrivenUpgradeNodeApi.readIntMap(object, "point_rewards", true).entrySet()) {
            builder.awardPoints(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<ResourceLocation, Integer> entry : DataDrivenUpgradeNodeApi.readIntMap(object, "counter_rewards", true).entrySet()) {
            builder.incrementCounter(entry.getKey(), entry.getValue());
        }
        return new LoadedUpgradeKillRule(ruleId, builder.build());
    }

    public record LoadedUpgradeKillRule(
            ResourceLocation id,
            UpgradeKillRule definition
    ) {}

    private static final class ReloadListener extends SimpleJsonResourceReloadListener {
        private ReloadListener() {
            super(GSON, DIRECTORY);
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> jsonById, ResourceManager resourceManager, ProfilerFiller profiler) {
            Map<ResourceLocation, LoadedUpgradeKillRule> definitions = new LinkedHashMap<>();
            for (Map.Entry<ResourceLocation, JsonElement> entry : jsonById.entrySet()) {
                try {
                    LoadedUpgradeKillRule definition = parseDefinition(entry.getKey(), entry.getValue());
                    LoadedUpgradeKillRule previous = definitions.putIfAbsent(definition.id(), definition);
                    if (previous != null) {
                        throw new IllegalStateException("Duplicate datapack upgrade kill rule id: " + definition.id());
                    }
                } catch (RuntimeException exception) {
                    XLib.LOGGER.error("Failed to parse datapack upgrade kill rule {}", entry.getKey(), exception);
                }
            }
            loadedDefinitions = Map.copyOf(definitions);
        }
    }
}
