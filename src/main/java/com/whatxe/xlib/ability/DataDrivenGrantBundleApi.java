package com.whatxe.xlib.ability;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.whatxe.xlib.XLib;
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

public final class DataDrivenGrantBundleApi {
    private static final Gson GSON = new GsonBuilder().create();
    private static final String DIRECTORY = XLib.MODID + "/grant_bundles";
    private static volatile Map<ResourceLocation, LoadedGrantBundleDefinition> loadedDefinitions = Map.of();

    private DataDrivenGrantBundleApi() {}

    public static void bootstrap() {}

    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new ReloadListener());
    }

    public static Collection<ResourceLocation> allDefinitionIds() {
        return List.copyOf(loadedDefinitions.keySet());
    }

    public static Optional<LoadedGrantBundleDefinition> findDefinition(ResourceLocation definitionId) {
        return Optional.ofNullable(loadedDefinitions.get(definitionId));
    }

    static void setDefinitionsForTesting(Map<ResourceLocation, LoadedGrantBundleDefinition> definitions) {
        loadedDefinitions = Map.copyOf(definitions);
    }

    static LoadedGrantBundleDefinition parseDefinition(ResourceLocation fileId, JsonElement element) {
        JsonObject object = GsonHelper.convertToJsonObject(element, "grant bundle");
        ResourceLocation bundleId = object.has("id")
                ? ResourceLocation.parse(GsonHelper.getAsString(object, "id"))
                : fileId;
        GrantBundleDefinition definition = new GrantBundleDefinition(
                bundleId,
                readLocations(object, "grant_ability", "grant_abilities"),
                readLocations(object, "grant_passive", "grant_passives"),
                readLocations(object, "grant_granted_item", "grant_granted_items"),
                readLocations(object, "grant_recipe_permission", "grant_recipe_permissions"),
                readLocations(object, "block_ability", "block_abilities"),
                readLocations(object, "grant_state_policy", "grant_state_policies"),
                readLocations(object, "grant_state_flag", "grant_state_flags")
        );
        return new LoadedGrantBundleDefinition(bundleId, definition);
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

    public record LoadedGrantBundleDefinition(
            ResourceLocation id,
            GrantBundleDefinition definition
    ) {}

    private static final class ReloadListener extends SimpleJsonResourceReloadListener {
        private ReloadListener() {
            super(GSON, DIRECTORY);
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> jsonById, ResourceManager resourceManager, ProfilerFiller profiler) {
            Map<ResourceLocation, LoadedGrantBundleDefinition> definitions = new LinkedHashMap<>();
            for (Map.Entry<ResourceLocation, JsonElement> entry : jsonById.entrySet()) {
                try {
                    LoadedGrantBundleDefinition definition = parseDefinition(entry.getKey(), entry.getValue());
                    LoadedGrantBundleDefinition previous = definitions.putIfAbsent(definition.id(), definition);
                    if (previous != null) {
                        throw new IllegalStateException("Duplicate datapack grant bundle id: " + definition.id());
                    }
                } catch (RuntimeException exception) {
                    XLib.LOGGER.error("Failed to parse datapack grant bundle {}", entry.getKey(), exception);
                }
            }
            loadedDefinitions = Map.copyOf(definitions);
        }
    }
}
