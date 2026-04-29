package com.whatxe.xlib.progression;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

public final class DataDrivenUpgradePointTypeApi {
    private static final Gson GSON = new GsonBuilder().create();
    private static final String DIRECTORY = XLib.MODID + "/upgrade_point_types";
    private static volatile Map<ResourceLocation, LoadedUpgradePointType> loadedDefinitions = Map.of();
    private static volatile Map<ResourceLocation, LoadedUpgradePointType> syncedDefinitions = Map.of();
    private static volatile Map<ResourceLocation, String> definitionJsonByFileId = Map.of();

    private DataDrivenUpgradePointTypeApi() {}

    public static void bootstrap() {}

    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new ReloadListener());
    }

    public static Collection<ResourceLocation> allDefinitionIds() {
        return List.copyOf(resolvedDefinitions().keySet());
    }

    public static Optional<LoadedUpgradePointType> findDefinition(ResourceLocation definitionId) {
        return Optional.ofNullable(resolvedDefinitions().get(definitionId));
    }

    public static Map<ResourceLocation, String> definitionJsonsForSync() {
        return Map.copyOf(definitionJsonByFileId);
    }

    public static void syncDefinitionsFromJson(Map<ResourceLocation, String> jsonByFileId) {
        syncedDefinitions = parseDefinitionsFromJson(jsonByFileId, "client-synced upgrade point type");
    }

    public static void clearSyncedDefinitions() {
        syncedDefinitions = Map.of();
    }

    static void setDefinitionsForTesting(Map<ResourceLocation, LoadedUpgradePointType> definitions) {
        loadedDefinitions = Map.copyOf(definitions);
    }

    static LoadedUpgradePointType parseDefinition(ResourceLocation fileId, JsonElement element) {
        JsonObject object = GsonHelper.convertToJsonObject(element, "upgrade point type");
        ResourceLocation pointTypeId = object.has("id")
                ? ResourceLocation.parse(GsonHelper.getAsString(object, "id"))
                : fileId;
        return new LoadedUpgradePointType(pointTypeId, UpgradePointType.of(pointTypeId));
    }

    public record LoadedUpgradePointType(
            ResourceLocation id,
            UpgradePointType definition
    ) {}

    private static Map<ResourceLocation, LoadedUpgradePointType> resolvedDefinitions() {
        Map<ResourceLocation, LoadedUpgradePointType> definitions = new LinkedHashMap<>(loadedDefinitions);
        syncedDefinitions.forEach(definitions::putIfAbsent);
        return Map.copyOf(definitions);
    }

    private static Map<ResourceLocation, LoadedUpgradePointType> parseDefinitionsFromJson(
            Map<ResourceLocation, String> jsonByFileId,
            String label
    ) {
        Map<ResourceLocation, LoadedUpgradePointType> definitions = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, String> entry : jsonByFileId.entrySet()) {
            try {
                LoadedUpgradePointType definition = parseDefinition(entry.getKey(), JsonParser.parseString(entry.getValue()));
                LoadedUpgradePointType previous = definitions.putIfAbsent(definition.id(), definition);
                if (previous != null) {
                    throw new IllegalStateException("Duplicate " + label + " id: " + definition.id());
                }
            } catch (RuntimeException exception) {
                XLib.LOGGER.error("Failed to parse {} {}", label, entry.getKey(), exception);
            }
        }
        return Map.copyOf(definitions);
    }

    private static final class ReloadListener extends SimpleJsonResourceReloadListener {
        private ReloadListener() {
            super(GSON, DIRECTORY);
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> jsonById, ResourceManager resourceManager, ProfilerFiller profiler) {
            Map<ResourceLocation, LoadedUpgradePointType> definitions = new LinkedHashMap<>();
            Map<ResourceLocation, String> definitionJsons = new LinkedHashMap<>();
            for (Map.Entry<ResourceLocation, JsonElement> entry : jsonById.entrySet()) {
                try {
                    LoadedUpgradePointType definition = parseDefinition(entry.getKey(), entry.getValue());
                    LoadedUpgradePointType previous = definitions.putIfAbsent(definition.id(), definition);
                    if (previous != null) {
                        throw new IllegalStateException("Duplicate datapack upgrade point type id: " + definition.id());
                    }
                    definitionJsons.put(entry.getKey(), GSON.toJson(entry.getValue()));
                } catch (RuntimeException exception) {
                    XLib.LOGGER.error("Failed to parse datapack upgrade point type {}", entry.getKey(), exception);
                }
            }
            loadedDefinitions = Map.copyOf(definitions);
            definitionJsonByFileId = Map.copyOf(definitionJsons);
        }
    }
}
