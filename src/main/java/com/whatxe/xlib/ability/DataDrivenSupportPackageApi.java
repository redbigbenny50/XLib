package com.whatxe.xlib.ability;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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

public final class DataDrivenSupportPackageApi {
    private static final Gson GSON = new GsonBuilder().create();
    private static final String DIRECTORY = XLib.MODID + "/support_packages";
    private static volatile Map<ResourceLocation, LoadedSupportPackageDefinition> loadedDefinitions = Map.of();

    private DataDrivenSupportPackageApi() {}

    public static void bootstrap() {}

    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new ReloadListener());
    }

    public static Collection<ResourceLocation> allDefinitionIds() {
        return List.copyOf(loadedDefinitions.keySet());
    }

    public static Optional<LoadedSupportPackageDefinition> findDefinition(ResourceLocation definitionId) {
        return Optional.ofNullable(loadedDefinitions.get(definitionId));
    }

    static void setDefinitionsForTesting(Map<ResourceLocation, LoadedSupportPackageDefinition> definitions) {
        loadedDefinitions = Map.copyOf(definitions);
    }

    static LoadedSupportPackageDefinition parseDefinition(ResourceLocation fileId, JsonElement element) {
        JsonObject object = GsonHelper.convertToJsonObject(element, "support package");
        ResourceLocation supportPackageId = object.has("id")
                ? ResourceLocation.parse(GsonHelper.getAsString(object, "id"))
                : fileId;
        SupportPackageDefinition definition = new SupportPackageDefinition(
                supportPackageId,
                DataDrivenDefinitionReaders.readLocations(object, "grant_bundle", "grant_bundles"),
                DataDrivenDefinitionReaders.readLocations(object, "relationship", "relationships"),
                object.has("allow_self") && GsonHelper.getAsBoolean(object, "allow_self")
        );
        return new LoadedSupportPackageDefinition(supportPackageId, definition);
    }

    public record LoadedSupportPackageDefinition(
            ResourceLocation id,
            SupportPackageDefinition definition
    ) {}

    private static final class ReloadListener extends SimpleJsonResourceReloadListener {
        private ReloadListener() {
            super(GSON, DIRECTORY);
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> jsonById, ResourceManager resourceManager, ProfilerFiller profiler) {
            Map<ResourceLocation, LoadedSupportPackageDefinition> definitions = new LinkedHashMap<>();
            for (Map.Entry<ResourceLocation, JsonElement> entry : jsonById.entrySet()) {
                try {
                    LoadedSupportPackageDefinition definition = parseDefinition(entry.getKey(), entry.getValue());
                    LoadedSupportPackageDefinition previous = definitions.putIfAbsent(definition.id(), definition);
                    if (previous != null) {
                        throw new IllegalStateException("Duplicate datapack support package id: " + definition.id());
                    }
                } catch (RuntimeException exception) {
                    XLib.LOGGER.error("Failed to parse datapack support package {}", entry.getKey(), exception);
                }
            }
            loadedDefinitions = Map.copyOf(definitions);
        }
    }
}
