package com.whatxe.xlib.ability;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.whatxe.xlib.XLib;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

public final class DataDrivenConditionApi {
    private static final Gson GSON = new GsonBuilder().create();
    private static final String DIRECTORY = XLib.MODID + "/conditions";
    private static volatile Map<ResourceLocation, AbilityRequirement> loadedConditions = Map.of();
    private static volatile Map<ResourceLocation, AbilityRequirement> syncedConditions = Map.of();
    private static volatile Map<ResourceLocation, String> definitionJsonByFileId = Map.of();

    private DataDrivenConditionApi() {}

    public static void bootstrap() {}

    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new ReloadListener());
    }

    public static Collection<ResourceLocation> allConditionIds() {
        return List.copyOf(resolvedConditions().keySet());
    }

    public static Optional<AbilityRequirement> findCondition(ResourceLocation conditionId) {
        return Optional.ofNullable(resolvedConditions().get(conditionId));
    }

    public static AbilityRequirement requireCondition(ResourceLocation conditionId) {
        return findCondition(conditionId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown named datapack condition: " + conditionId));
    }

    public static Map<ResourceLocation, String> definitionJsonsForSync() {
        return Map.copyOf(definitionJsonByFileId);
    }

    public static void syncDefinitionsFromJson(Map<ResourceLocation, String> jsonByFileId) {
        syncedConditions = compileDefinitionsFromJson(jsonByFileId, "client-synced named condition");
    }

    public static void clearSyncedDefinitions() {
        syncedConditions = Map.of();
    }

    static void setLoadedConditionsForTesting(Map<ResourceLocation, AbilityRequirement> conditions) {
        loadedConditions = Map.copyOf(conditions);
    }

    private static Map<ResourceLocation, AbilityRequirement> resolvedConditions() {
        Map<ResourceLocation, AbilityRequirement> conditions = new LinkedHashMap<>(loadedConditions);
        syncedConditions.forEach(conditions::putIfAbsent);
        return Map.copyOf(conditions);
    }

    private static Map<ResourceLocation, AbilityRequirement> compileDefinitionsFromJson(
            Map<ResourceLocation, String> jsonByFileId,
            String label
    ) {
        Map<ResourceLocation, JsonElement> rawDefinitions = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, String> entry : jsonByFileId.entrySet()) {
            try {
                rawDefinitions.put(entry.getKey(), JsonParser.parseString(entry.getValue()));
            } catch (RuntimeException exception) {
                XLib.LOGGER.error("Failed to parse {} {}", label, entry.getKey(), exception);
            }
        }
        try {
            return AbilityRequirementJsonParser.compileDefinitions(rawDefinitions);
        } catch (RuntimeException exception) {
            XLib.LOGGER.error("Failed to compile {} set", label, exception);
            return Map.of();
        }
    }

    private static final class ReloadListener extends SimpleJsonResourceReloadListener {
        private ReloadListener() {
            super(GSON, DIRECTORY);
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> jsonById, ResourceManager resourceManager, ProfilerFiller profiler) {
            try {
                loadedConditions = AbilityRequirementJsonParser.compileDefinitions(jsonById);
                Map<ResourceLocation, String> definitionJsons = new LinkedHashMap<>();
                for (Map.Entry<ResourceLocation, JsonElement> entry : jsonById.entrySet()) {
                    definitionJsons.put(entry.getKey(), GSON.toJson(entry.getValue()));
                }
                definitionJsonByFileId = Map.copyOf(definitionJsons);
            } catch (RuntimeException exception) {
                XLib.LOGGER.error("Failed to compile named datapack conditions from {}", DIRECTORY, exception);
                loadedConditions = Map.of();
                definitionJsonByFileId = Map.of();
            }
        }
    }
}
