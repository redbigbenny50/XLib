package com.whatxe.xlib.ability;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.whatxe.xlib.XLib;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

public final class DataDrivenArtifactApi {
    private static final Gson GSON = new GsonBuilder().create();
    private static final String DIRECTORY = XLib.MODID + "/artifacts";
    private static volatile Map<ResourceLocation, LoadedArtifactDefinition> loadedDefinitions = Map.of();
    private static volatile Map<ResourceLocation, LoadedArtifactDefinition> syncedDefinitions = Map.of();
    private static volatile Map<ResourceLocation, String> definitionJsonByFileId = Map.of();

    private DataDrivenArtifactApi() {}

    public static void bootstrap() {}

    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new ReloadListener());
    }

    public static Collection<ResourceLocation> allDefinitionIds() {
        return List.copyOf(resolvedDefinitions().keySet());
    }

    public static Optional<LoadedArtifactDefinition> findDefinition(ResourceLocation definitionId) {
        return Optional.ofNullable(resolvedDefinitions().get(definitionId));
    }

    public static Map<ResourceLocation, String> definitionJsonsForSync() {
        return Map.copyOf(definitionJsonByFileId);
    }

    public static void syncDefinitionsFromJson(Map<ResourceLocation, String> jsonByFileId) {
        syncedDefinitions = parseDefinitionsFromJson(jsonByFileId, "client-synced artifact");
    }

    public static void clearSyncedDefinitions() {
        syncedDefinitions = Map.of();
    }

    static void setDefinitionsForTesting(Map<ResourceLocation, LoadedArtifactDefinition> definitions) {
        loadedDefinitions = Map.copyOf(definitions);
    }

    static LoadedArtifactDefinition parseDefinition(ResourceLocation fileId, JsonElement element) {
        JsonObject object = GsonHelper.convertToJsonObject(element, "artifact");
        ResourceLocation artifactId = object.has("id")
                ? ResourceLocation.parse(GsonHelper.getAsString(object, "id"))
                : fileId;
        Set<ResourceLocation> itemIds = readLocations(object, "item", "items");
        if (itemIds.isEmpty()) {
            throw new IllegalArgumentException("Artifacts require at least one item id");
        }
        List<AbilityRequirement> requirements = object.has("when")
                ? List.of(AbilityRequirementJsonParser.parse(object.get("when"), DataDrivenConditionApi::requireCondition))
                : List.of();
        ArtifactDefinition definition = new ArtifactDefinition(
                artifactId,
                itemIds,
                readPresenceModes(object),
                readLocations(object, "equipped_bundle", "equipped_bundles"),
                readLocations(object, "unlocked_bundle", "unlocked_bundles"),
                requirements,
                object.has("unlock_on_consume") && GsonHelper.getAsBoolean(object, "unlock_on_consume")
        );
        return new LoadedArtifactDefinition(artifactId, definition);
    }

    private static Set<ArtifactPresenceMode> readPresenceModes(JsonObject object) {
        Set<ArtifactPresenceMode> presenceModes = new LinkedHashSet<>();
        if (object.has("presence")) {
            presenceModes.add(parsePresenceMode(GsonHelper.getAsString(object, "presence")));
        }
        if (object.has("presence_modes")) {
            for (JsonElement element : GsonHelper.getAsJsonArray(object, "presence_modes")) {
                presenceModes.add(parsePresenceMode(element.getAsString()));
            }
        }
        if (presenceModes.isEmpty()) {
            presenceModes.add(ArtifactPresenceMode.INVENTORY);
        }
        return Set.copyOf(presenceModes);
    }

    private static ArtifactPresenceMode parsePresenceMode(String rawValue) {
        return ArtifactPresenceMode.valueOf(rawValue.trim().toUpperCase(java.util.Locale.ROOT));
    }

    private static Set<ResourceLocation> readLocations(JsonObject object, String singleKey, String pluralKey) {
        Set<ResourceLocation> values = new LinkedHashSet<>();
        if (singleKey != null && object.has(singleKey)) {
            values.add(ResourceLocation.parse(GsonHelper.getAsString(object, singleKey)));
        }
        if (pluralKey != null && object.has(pluralKey)) {
            for (JsonElement element : GsonHelper.getAsJsonArray(object, pluralKey)) {
                values.add(ResourceLocation.parse(element.getAsString()));
            }
        }
        return Set.copyOf(values);
    }

    public record LoadedArtifactDefinition(
            ResourceLocation id,
            ArtifactDefinition definition
    ) {}

    private static Map<ResourceLocation, LoadedArtifactDefinition> resolvedDefinitions() {
        Map<ResourceLocation, LoadedArtifactDefinition> definitions = new LinkedHashMap<>(loadedDefinitions);
        syncedDefinitions.forEach(definitions::putIfAbsent);
        return Map.copyOf(definitions);
    }

    private static Map<ResourceLocation, LoadedArtifactDefinition> parseDefinitionsFromJson(
            Map<ResourceLocation, String> jsonByFileId,
            String label
    ) {
        Map<ResourceLocation, LoadedArtifactDefinition> definitions = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, String> entry : jsonByFileId.entrySet()) {
            try {
                LoadedArtifactDefinition definition = parseDefinition(entry.getKey(), JsonParser.parseString(entry.getValue()));
                LoadedArtifactDefinition previous = definitions.putIfAbsent(definition.id(), definition);
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
            Map<ResourceLocation, LoadedArtifactDefinition> definitions = new LinkedHashMap<>();
            Map<ResourceLocation, String> definitionJsons = new LinkedHashMap<>();
            for (Map.Entry<ResourceLocation, JsonElement> entry : jsonById.entrySet()) {
                try {
                    LoadedArtifactDefinition definition = parseDefinition(entry.getKey(), entry.getValue());
                    LoadedArtifactDefinition previous = definitions.putIfAbsent(definition.id(), definition);
                    if (previous != null) {
                        throw new IllegalStateException("Duplicate datapack artifact id: " + definition.id());
                    }
                    definitionJsons.put(entry.getKey(), GSON.toJson(entry.getValue()));
                } catch (RuntimeException exception) {
                    XLib.LOGGER.error("Failed to parse datapack artifact {}", entry.getKey(), exception);
                }
            }
            loadedDefinitions = Map.copyOf(definitions);
            definitionJsonByFileId = Map.copyOf(definitionJsons);
        }
    }
}
