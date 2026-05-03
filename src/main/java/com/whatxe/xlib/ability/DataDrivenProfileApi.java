package com.whatxe.xlib.ability;

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
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

public final class DataDrivenProfileApi {
    private static final Gson GSON = new GsonBuilder().create();
    private static final String DIRECTORY = XLib.MODID + "/profiles";
    private static volatile Map<ResourceLocation, LoadedProfileDefinition> loadedDefinitions = Map.of();
    private static volatile Map<ResourceLocation, LoadedProfileDefinition> syncedDefinitions = Map.of();
    private static volatile Map<ResourceLocation, String> definitionJsonByFileId = Map.of();

    private DataDrivenProfileApi() {}

    public static void bootstrap() {}

    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new ReloadListener());
    }

    public static Collection<ResourceLocation> allDefinitionIds() {
        return List.copyOf(resolvedDefinitions().keySet());
    }

    public static Optional<LoadedProfileDefinition> findDefinition(ResourceLocation definitionId) {
        return Optional.ofNullable(resolvedDefinitions().get(definitionId));
    }

    public static Map<ResourceLocation, String> definitionJsonsForSync() {
        return Map.copyOf(definitionJsonByFileId);
    }

    public static void syncDefinitionsFromJson(Map<ResourceLocation, String> jsonByFileId) {
        syncedDefinitions = parseDefinitionsFromJson(jsonByFileId, "client-synced profile");
    }

    public static void clearSyncedDefinitions() {
        syncedDefinitions = Map.of();
    }

    static void setDefinitionsForTesting(Map<ResourceLocation, LoadedProfileDefinition> definitions) {
        loadedDefinitions = Map.copyOf(definitions);
    }

    static LoadedProfileDefinition parseDefinition(ResourceLocation fileId, JsonElement element) {
        JsonObject object = GsonHelper.convertToJsonObject(element, "profile");
        ResourceLocation profileId = object.has("id")
                ? ResourceLocation.parse(GsonHelper.getAsString(object, "id"))
                : fileId;
        ResourceLocation groupId = ResourceLocation.parse(GsonHelper.getAsString(object, "group"));
        ProfileDefinition definition = new ProfileDefinition(
                profileId,
                groupId,
                DataDrivenDefinitionReaders.readComponent(object, "display_name", Component.literal(profileId.toString())),
                DataDrivenDefinitionReaders.readComponent(object, "description", Component.empty()),
                DataDrivenDefinitionReaders.readRequiredIcon(object),
                DataDrivenDefinitionReaders.readLocations(object, "incompatible_with", "incompatible_profiles"),
                DataDrivenDefinitionReaders.readLocations(object, "grant_bundle", "grant_bundles"),
                DataDrivenDefinitionReaders.readLocations(object, "identity", "identities"),
                DataDrivenDefinitionReaders.readLocations(object, "ability", "abilities"),
                DataDrivenDefinitionReaders.readLocations(object, "mode", "modes"),
                DataDrivenDefinitionReaders.readLocations(object, "passive", "passives"),
                DataDrivenDefinitionReaders.readLocations(object, "granted_item", "granted_items"),
                DataDrivenDefinitionReaders.readLocations(object, "recipe_permission", "recipe_permissions"),
                DataDrivenDefinitionReaders.readLocations(object, "state_flag", "state_flags"),
                DataDrivenDefinitionReaders.readLocations(object, "unlock_artifact", "unlock_artifacts"),
                DataDrivenDefinitionReaders.readLocations(object, "starting_node", "starting_nodes")
        );
        return new LoadedProfileDefinition(profileId, definition);
    }

    public record LoadedProfileDefinition(
            ResourceLocation id,
            ProfileDefinition definition
    ) {}

    private static Map<ResourceLocation, LoadedProfileDefinition> resolvedDefinitions() {
        Map<ResourceLocation, LoadedProfileDefinition> definitions = new LinkedHashMap<>(loadedDefinitions);
        syncedDefinitions.forEach(definitions::putIfAbsent);
        return Map.copyOf(definitions);
    }

    private static Map<ResourceLocation, LoadedProfileDefinition> parseDefinitionsFromJson(
            Map<ResourceLocation, String> jsonByFileId,
            String label
    ) {
        Map<ResourceLocation, LoadedProfileDefinition> definitions = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, String> entry : jsonByFileId.entrySet()) {
            try {
                LoadedProfileDefinition definition = parseDefinition(entry.getKey(), JsonParser.parseString(entry.getValue()));
                LoadedProfileDefinition previous = definitions.putIfAbsent(definition.id(), definition);
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
            Map<ResourceLocation, LoadedProfileDefinition> definitions = new LinkedHashMap<>();
            Map<ResourceLocation, String> definitionJsons = new LinkedHashMap<>();
            for (Map.Entry<ResourceLocation, JsonElement> entry : jsonById.entrySet()) {
                try {
                    LoadedProfileDefinition definition = parseDefinition(entry.getKey(), entry.getValue());
                    LoadedProfileDefinition previous = definitions.putIfAbsent(definition.id(), definition);
                    if (previous != null) {
                        throw new IllegalStateException("Duplicate datapack profile id: " + definition.id());
                    }
                    definitionJsons.put(entry.getKey(), GSON.toJson(entry.getValue()));
                } catch (RuntimeException exception) {
                    XLib.LOGGER.error("Failed to parse datapack profile {}", entry.getKey(), exception);
                }
            }
            loadedDefinitions = Map.copyOf(definitions);
            definitionJsonByFileId = Map.copyOf(definitionJsons);
        }
    }
}
