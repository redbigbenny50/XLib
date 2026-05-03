package com.whatxe.xlib.form;

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

public final class DataDrivenVisualFormApi {
    private static final Gson GSON = new GsonBuilder().create();
    private static final String DIRECTORY = XLib.MODID + "/visual_forms";
    private static volatile Map<ResourceLocation, LoadedVisualFormDefinition> loadedDefinitions = Map.of();
    private static volatile Map<ResourceLocation, LoadedVisualFormDefinition> syncedDefinitions = Map.of();
    private static volatile Map<ResourceLocation, String> definitionJsonByFileId = Map.of();

    private DataDrivenVisualFormApi() {}

    public static void bootstrap() {}

    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new ReloadListener());
    }

    public static Collection<ResourceLocation> allDefinitionIds() {
        return List.copyOf(resolvedDefinitions().keySet());
    }

    public static Optional<LoadedVisualFormDefinition> findDefinition(ResourceLocation definitionId) {
        return Optional.ofNullable(resolvedDefinitions().get(definitionId));
    }

    public static Map<ResourceLocation, String> definitionJsonsForSync() {
        return Map.copyOf(definitionJsonByFileId);
    }

    public static void syncDefinitionsFromJson(Map<ResourceLocation, String> jsonByFileId) {
        syncedDefinitions = parseDefinitionsFromJson(jsonByFileId, "client-synced visual_form");
    }

    public static void clearSyncedDefinitions() {
        syncedDefinitions = Map.of();
    }

    static void setDefinitionsForTesting(Map<ResourceLocation, LoadedVisualFormDefinition> definitions) {
        loadedDefinitions = Map.copyOf(definitions);
    }

    static LoadedVisualFormDefinition parseDefinition(ResourceLocation fileId, JsonElement element) {
        JsonObject object = GsonHelper.convertToJsonObject(element, "visual_form");
        ResourceLocation formId = object.has("id")
                ? ResourceLocation.parse(GsonHelper.getAsString(object, "id"))
                : fileId;

        String kindName = GsonHelper.getAsString(object, "kind").toUpperCase(java.util.Locale.ROOT);
        VisualFormKind kind;
        try {
            kind = VisualFormKind.valueOf(kindName);
        } catch (IllegalArgumentException ignored) {
            throw new IllegalArgumentException("Unknown visual form kind '" + kindName
                    + "'; valid values: humanoid, creature, vehicle, construct, spirit, abstract");
        }

        VisualFormDefinition.Builder builder = VisualFormDefinition.builder(formId, kind);

        if (object.has("model_profile")) {
            builder.modelProfile(ResourceLocation.parse(GsonHelper.getAsString(object, "model_profile")));
        }

        if (object.has("cue_route_profile")) {
            builder.cueRouteProfile(ResourceLocation.parse(GsonHelper.getAsString(object, "cue_route_profile")));
        }

        if (object.has("hud_profile")) {
            builder.hudProfile(ResourceLocation.parse(GsonHelper.getAsString(object, "hud_profile")));
        }

        if (object.has("sound_profile")) {
            builder.soundProfile(ResourceLocation.parse(GsonHelper.getAsString(object, "sound_profile")));
        }

        if (object.has("first_person_policy")) {
            String policyName = GsonHelper.getAsString(object, "first_person_policy").toUpperCase(java.util.Locale.ROOT);
            FirstPersonPolicy firstPersonPolicy;
            try {
                firstPersonPolicy = FirstPersonPolicy.valueOf(policyName);
            } catch (IllegalArgumentException ignored) {
                throw new IllegalArgumentException("Unknown first_person_policy '" + policyName
                        + "'; valid values: default, hidden, custom");
            }
            builder.firstPersonPolicy(firstPersonPolicy);
        }

        if (object.has("render_scale")) {
            float scale = GsonHelper.getAsFloat(object, "render_scale");
            if (scale <= 0.0f) {
                throw new IllegalArgumentException("render_scale must be positive, got " + scale);
            }
            builder.renderScale(scale);
        }

        return new LoadedVisualFormDefinition(formId, builder.build());
    }

    public record LoadedVisualFormDefinition(
            ResourceLocation id,
            VisualFormDefinition definition
    ) {}

    private static Map<ResourceLocation, LoadedVisualFormDefinition> resolvedDefinitions() {
        Map<ResourceLocation, LoadedVisualFormDefinition> definitions = new LinkedHashMap<>(loadedDefinitions);
        syncedDefinitions.forEach(definitions::putIfAbsent);
        return Map.copyOf(definitions);
    }

    private static Map<ResourceLocation, LoadedVisualFormDefinition> parseDefinitionsFromJson(
            Map<ResourceLocation, String> jsonByFileId,
            String label
    ) {
        Map<ResourceLocation, LoadedVisualFormDefinition> definitions = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, String> entry : jsonByFileId.entrySet()) {
            try {
                LoadedVisualFormDefinition definition = parseDefinition(entry.getKey(), JsonParser.parseString(entry.getValue()));
                LoadedVisualFormDefinition previous = definitions.putIfAbsent(definition.id(), definition);
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
            Map<ResourceLocation, LoadedVisualFormDefinition> definitions = new LinkedHashMap<>();
            Map<ResourceLocation, String> definitionJsons = new LinkedHashMap<>();
            for (Map.Entry<ResourceLocation, JsonElement> entry : jsonById.entrySet()) {
                try {
                    LoadedVisualFormDefinition definition = parseDefinition(entry.getKey(), entry.getValue());
                    LoadedVisualFormDefinition previous = definitions.putIfAbsent(definition.id(), definition);
                    if (previous != null) {
                        throw new IllegalStateException("Duplicate datapack visual_form id: " + definition.id());
                    }
                    definitionJsons.put(entry.getKey(), GSON.toJson(entry.getValue()));
                } catch (RuntimeException exception) {
                    XLib.LOGGER.error("Failed to parse datapack visual_form {}", entry.getKey(), exception);
                }
            }
            loadedDefinitions = Map.copyOf(definitions);
            definitionJsonByFileId = Map.copyOf(definitionJsons);
        }
    }
}
