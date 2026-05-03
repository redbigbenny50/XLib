package com.whatxe.xlib.progression;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.whatxe.xlib.XLib;
import com.whatxe.xlib.ability.DataDrivenDefinitionReaders;
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

public final class DataDrivenUpgradeTrackApi {
    private static final Gson GSON = new GsonBuilder().create();
    private static final String DIRECTORY = XLib.MODID + "/upgrade_tracks";
    private static volatile Map<ResourceLocation, LoadedUpgradeTrackDefinition> loadedDefinitions = Map.of();
    private static volatile Map<ResourceLocation, LoadedUpgradeTrackDefinition> syncedDefinitions = Map.of();
    private static volatile Map<ResourceLocation, String> definitionJsonByFileId = Map.of();

    private DataDrivenUpgradeTrackApi() {}

    public static void bootstrap() {}

    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new ReloadListener());
    }

    public static Collection<ResourceLocation> allDefinitionIds() {
        return List.copyOf(resolvedDefinitions().keySet());
    }

    public static Optional<LoadedUpgradeTrackDefinition> findDefinition(ResourceLocation definitionId) {
        return Optional.ofNullable(resolvedDefinitions().get(definitionId));
    }

    public static Map<ResourceLocation, String> definitionJsonsForSync() {
        return Map.copyOf(definitionJsonByFileId);
    }

    public static void syncDefinitionsFromJson(Map<ResourceLocation, String> jsonByFileId) {
        syncedDefinitions = parseDefinitionsFromJson(jsonByFileId, "client-synced upgrade track");
    }

    public static void clearSyncedDefinitions() {
        syncedDefinitions = Map.of();
    }

    static void setDefinitionsForTesting(Map<ResourceLocation, LoadedUpgradeTrackDefinition> definitions) {
        loadedDefinitions = Map.copyOf(definitions);
    }

    static LoadedUpgradeTrackDefinition parseDefinition(ResourceLocation fileId, JsonElement element) {
        JsonObject object = GsonHelper.convertToJsonObject(element, "upgrade track");
        ResourceLocation trackId = object.has("id")
                ? ResourceLocation.parse(GsonHelper.getAsString(object, "id"))
                : fileId;
        UpgradeTrackDefinition.Builder builder = UpgradeTrackDefinition.builder(trackId);
        if (object.has("family")) {
            builder.family(ResourceLocation.parse(GsonHelper.getAsString(object, "family")));
        }
        if (object.has("group")) {
            builder.group(ResourceLocation.parse(GsonHelper.getAsString(object, "group")));
        }
        if (object.has("page")) {
            builder.page(ResourceLocation.parse(GsonHelper.getAsString(object, "page")));
        }
        builder.tags(DataDrivenDefinitionReaders.readLocations(object, "tag", "tags"));
        builder.rootNodes(DataDrivenDefinitionReaders.readLocations(object, "root_node", "root_nodes"));
        builder.exclusiveWith(DataDrivenDefinitionReaders.readLocations(object, "exclusive_track", "exclusive_tracks"));
        return new LoadedUpgradeTrackDefinition(trackId, builder.build());
    }

    public record LoadedUpgradeTrackDefinition(
            ResourceLocation id,
            UpgradeTrackDefinition definition
    ) {}

    private static Map<ResourceLocation, LoadedUpgradeTrackDefinition> resolvedDefinitions() {
        Map<ResourceLocation, LoadedUpgradeTrackDefinition> definitions = new LinkedHashMap<>(loadedDefinitions);
        syncedDefinitions.forEach(definitions::putIfAbsent);
        return Map.copyOf(definitions);
    }

    private static Map<ResourceLocation, LoadedUpgradeTrackDefinition> parseDefinitionsFromJson(
            Map<ResourceLocation, String> jsonByFileId,
            String label
    ) {
        Map<ResourceLocation, LoadedUpgradeTrackDefinition> definitions = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, String> entry : jsonByFileId.entrySet()) {
            try {
                LoadedUpgradeTrackDefinition definition = parseDefinition(entry.getKey(), JsonParser.parseString(entry.getValue()));
                LoadedUpgradeTrackDefinition previous = definitions.putIfAbsent(definition.id(), definition);
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
            Map<ResourceLocation, LoadedUpgradeTrackDefinition> definitions = new LinkedHashMap<>();
            Map<ResourceLocation, String> definitionJsons = new LinkedHashMap<>();
            for (Map.Entry<ResourceLocation, JsonElement> entry : jsonById.entrySet()) {
                try {
                    LoadedUpgradeTrackDefinition definition = parseDefinition(entry.getKey(), entry.getValue());
                    LoadedUpgradeTrackDefinition previous = definitions.putIfAbsent(definition.id(), definition);
                    if (previous != null) {
                        throw new IllegalStateException("Duplicate datapack upgrade track id: " + definition.id());
                    }
                    definitionJsons.put(entry.getKey(), GSON.toJson(entry.getValue()));
                } catch (RuntimeException exception) {
                    XLib.LOGGER.error("Failed to parse datapack upgrade track {}", entry.getKey(), exception);
                }
            }
            loadedDefinitions = Map.copyOf(definitions);
            definitionJsonByFileId = Map.copyOf(definitionJsons);
        }
    }
}
