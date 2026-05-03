package com.whatxe.xlib.value;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.whatxe.xlib.XLib;
import com.whatxe.xlib.ability.DataDrivenDefinitionReaders;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

public final class DataDrivenTrackedValueApi {
    private static final Gson GSON = new GsonBuilder().create();
    private static final String DIRECTORY = XLib.MODID + "/tracked_values";
    private static volatile Map<ResourceLocation, LoadedTrackedValueDefinition> loadedDefinitions = Map.of();
    private static volatile Map<ResourceLocation, LoadedTrackedValueDefinition> syncedDefinitions = Map.of();
    private static volatile Map<ResourceLocation, String> definitionJsonByFileId = Map.of();

    private DataDrivenTrackedValueApi() {}

    public static void bootstrap() {}

    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new ReloadListener());
    }

    public static Collection<ResourceLocation> allDefinitionIds() {
        return List.copyOf(resolvedDefinitions().keySet());
    }

    public static Optional<LoadedTrackedValueDefinition> findDefinition(ResourceLocation definitionId) {
        return Optional.ofNullable(resolvedDefinitions().get(definitionId));
    }

    public static Map<ResourceLocation, String> definitionJsonsForSync() {
        return Map.copyOf(definitionJsonByFileId);
    }

    public static void syncDefinitionsFromJson(Map<ResourceLocation, String> jsonByFileId) {
        syncedDefinitions = parseDefinitionsFromJson(jsonByFileId, "client-synced tracked value");
    }

    public static void clearSyncedDefinitions() {
        syncedDefinitions = Map.of();
    }

    static void setDefinitionsForTesting(Map<ResourceLocation, LoadedTrackedValueDefinition> definitions) {
        loadedDefinitions = Map.copyOf(definitions);
    }

    static LoadedTrackedValueDefinition parseDefinition(ResourceLocation fileId, JsonElement element) {
        var object = GsonHelper.convertToJsonObject(element, "tracked value");
        ResourceLocation valueId = object.has("id")
                ? ResourceLocation.parse(GsonHelper.getAsString(object, "id"))
                : fileId;
        Component fallbackName = Component.literal(valueId.toString());
        TrackedValueDefinition definition = TrackedValueDefinition.builder(valueId)
                .displayName(DataDrivenDefinitionReaders.readComponent(object, "display_name", fallbackName))
                .minValue(object.has("min") ? GsonHelper.getAsDouble(object, "min")
                        : object.has("min_value") ? GsonHelper.getAsDouble(object, "min_value") : 0.0D)
                .maxValue(object.has("max") ? GsonHelper.getAsDouble(object, "max")
                        : object.has("max_value") ? GsonHelper.getAsDouble(object, "max_value") : 100.0D)
                .startingValue(object.has("starting_value") ? GsonHelper.getAsDouble(object, "starting_value")
                        : object.has("starting_amount") ? GsonHelper.getAsDouble(object, "starting_amount") : 0.0D)
                .tickDelta(object.has("tick_delta") ? GsonHelper.getAsDouble(object, "tick_delta") : 0.0D)
                .hudColor(readColor(object, "hud_color", 0xFF8AD8FF))
                .foodReplacementPriority(object.has("food_replacement_priority")
                        ? GsonHelper.getAsInt(object, "food_replacement_priority")
                        : object.has("replace_food_bar") && GsonHelper.getAsBoolean(object, "replace_food_bar") ? 100 : 0)
                .foodReplacementIntakeScale(object.has("food_replacement_intake_scale")
                        ? GsonHelper.getAsDouble(object, "food_replacement_intake_scale")
                        : 0.0D)
                .foodReplacementHealThreshold(object.has("food_replacement_heal_threshold")
                        ? GsonHelper.getAsInt(object, "food_replacement_heal_threshold")
                        : 18)
                .foodReplacementHealIntervalTicks(object.has("food_replacement_heal_interval_ticks")
                        ? GsonHelper.getAsInt(object, "food_replacement_heal_interval_ticks")
                        : 80)
                .foodReplacementHealCost(object.has("food_replacement_heal_cost")
                        ? GsonHelper.getAsDouble(object, "food_replacement_heal_cost")
                        : 1.0D)
                .foodReplacementStarvationThreshold(object.has("food_replacement_starvation_threshold")
                        ? GsonHelper.getAsInt(object, "food_replacement_starvation_threshold")
                        : 0)
                .foodReplacementStarvationIntervalTicks(object.has("food_replacement_starvation_interval_ticks")
                        ? GsonHelper.getAsInt(object, "food_replacement_starvation_interval_ticks")
                        : 80)
                .foodReplacementStarvationDamage(object.has("food_replacement_starvation_damage")
                        ? GsonHelper.getAsFloat(object, "food_replacement_starvation_damage")
                        : 1.0F)
                .build();
        return new LoadedTrackedValueDefinition(valueId, definition);
    }

    public record LoadedTrackedValueDefinition(
            ResourceLocation id,
            TrackedValueDefinition definition
    ) {}

    private static Map<ResourceLocation, LoadedTrackedValueDefinition> resolvedDefinitions() {
        Map<ResourceLocation, LoadedTrackedValueDefinition> definitions = new LinkedHashMap<>(loadedDefinitions);
        syncedDefinitions.forEach(definitions::putIfAbsent);
        return Map.copyOf(definitions);
    }

    private static Map<ResourceLocation, LoadedTrackedValueDefinition> parseDefinitionsFromJson(
            Map<ResourceLocation, String> jsonByFileId,
            String label
    ) {
        Map<ResourceLocation, LoadedTrackedValueDefinition> definitions = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, String> entry : jsonByFileId.entrySet()) {
            try {
                LoadedTrackedValueDefinition definition = parseDefinition(entry.getKey(), JsonParser.parseString(entry.getValue()));
                LoadedTrackedValueDefinition previous = definitions.putIfAbsent(definition.id(), definition);
                if (previous != null) {
                    throw new IllegalStateException("Duplicate " + label + " id: " + definition.id());
                }
            } catch (RuntimeException exception) {
                XLib.LOGGER.error("Failed to parse {} {}", label, entry.getKey(), exception);
            }
        }
        return Map.copyOf(definitions);
    }

    private static int readColor(com.google.gson.JsonObject object, String key, int fallback) {
        if (!object.has(key)) {
            return fallback;
        }
        JsonElement element = object.get(key);
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            return element.getAsInt();
        }
        String raw = GsonHelper.getAsString(object, key).trim();
        if (raw.startsWith("#")) {
            return (int) Long.parseLong(raw.substring(1), 16) | 0xFF000000;
        }
        if (raw.startsWith("0x") || raw.startsWith("0X")) {
            return (int) Long.parseLong(raw.substring(2), 16);
        }
        return Integer.decode(raw);
    }

    private static final class ReloadListener extends SimpleJsonResourceReloadListener {
        private ReloadListener() {
            super(GSON, DIRECTORY);
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> jsonById, ResourceManager resourceManager, ProfilerFiller profiler) {
            Map<ResourceLocation, LoadedTrackedValueDefinition> definitions = new LinkedHashMap<>();
            Map<ResourceLocation, String> definitionJsons = new LinkedHashMap<>();
            for (Map.Entry<ResourceLocation, JsonElement> entry : jsonById.entrySet()) {
                try {
                    LoadedTrackedValueDefinition definition = parseDefinition(entry.getKey(), entry.getValue());
                    LoadedTrackedValueDefinition previous = definitions.putIfAbsent(definition.id(), definition);
                    if (previous != null) {
                        throw new IllegalStateException("Duplicate datapack tracked value id: " + definition.id());
                    }
                    definitionJsons.put(entry.getKey(), GSON.toJson(entry.getValue()));
                } catch (RuntimeException exception) {
                    XLib.LOGGER.error("Failed to parse datapack tracked value {}", entry.getKey(), exception);
                }
            }
            loadedDefinitions = Map.copyOf(definitions);
            definitionJsonByFileId = Map.copyOf(definitionJsons);
        }
    }
}
