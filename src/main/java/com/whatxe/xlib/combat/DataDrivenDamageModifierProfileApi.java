package com.whatxe.xlib.combat;

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

public final class DataDrivenDamageModifierProfileApi {
    private static final Gson GSON = new GsonBuilder().create();
    private static final String DIRECTORY = XLib.MODID + "/damage_modifier_profiles";
    private static volatile Map<ResourceLocation, LoadedDamageModifierProfileDefinition> loadedDefinitions = Map.of();

    private DataDrivenDamageModifierProfileApi() {}

    public static void bootstrap() {}

    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new ReloadListener());
    }

    public static Collection<ResourceLocation> allDefinitionIds() {
        return List.copyOf(loadedDefinitions.keySet());
    }

    public static Optional<LoadedDamageModifierProfileDefinition> findDefinition(ResourceLocation definitionId) {
        return Optional.ofNullable(loadedDefinitions.get(definitionId));
    }

    static Map<ResourceLocation, DamageModifierProfileDefinition> definitions() {
        LinkedHashMap<ResourceLocation, DamageModifierProfileDefinition> definitions = new LinkedHashMap<>();
        for (LoadedDamageModifierProfileDefinition loaded : loadedDefinitions.values()) {
            definitions.put(loaded.id(), loaded.definition());
        }
        return Map.copyOf(definitions);
    }

    static void setDefinitionsForTesting(Map<ResourceLocation, LoadedDamageModifierProfileDefinition> definitions) {
        loadedDefinitions = Map.copyOf(definitions);
    }

    static LoadedDamageModifierProfileDefinition parseDefinition(ResourceLocation fileId, JsonElement element) {
        JsonObject object = GsonHelper.convertToJsonObject(element, "damage modifier profile");
        ResourceLocation profileId = object.has("id")
                ? ResourceLocation.parse(GsonHelper.getAsString(object, "id"))
                : fileId;

        DamageModifierProfileDefinition.Builder builder = DamageModifierProfileDefinition.builder(profileId);
        readMultipliers(object, "incoming_damage_types").forEach(builder::incomingDamageType);
        readMultipliers(object, "incoming_damage_type_tags").forEach(builder::incomingDamageTypeTag);
        readMultipliers(object, "outgoing_damage_types").forEach(builder::outgoingDamageType);
        readMultipliers(object, "outgoing_damage_type_tags").forEach(builder::outgoingDamageTypeTag);

        if (object.has("incoming")) {
            JsonObject incoming = GsonHelper.getAsJsonObject(object, "incoming");
            readMultipliers(incoming, "damage_types").forEach(builder::incomingDamageType);
            readMultipliers(incoming, "damage_type_tags").forEach(builder::incomingDamageTypeTag);
        }
        if (object.has("outgoing")) {
            JsonObject outgoing = GsonHelper.getAsJsonObject(object, "outgoing");
            readMultipliers(outgoing, "damage_types").forEach(builder::outgoingDamageType);
            readMultipliers(outgoing, "damage_type_tags").forEach(builder::outgoingDamageTypeTag);
        }

        if (object.has("incoming_flat")) {
            JsonObject incomingFlat = GsonHelper.getAsJsonObject(object, "incoming_flat");
            readValues(incomingFlat, "damage_types").forEach(builder::incomingFlatAddition);
            readValues(incomingFlat, "damage_type_tags").forEach(builder::incomingFlatAdditionTag);
        }
        if (object.has("outgoing_flat")) {
            JsonObject outgoingFlat = GsonHelper.getAsJsonObject(object, "outgoing_flat");
            readValues(outgoingFlat, "damage_types").forEach(builder::outgoingFlatAddition);
            readValues(outgoingFlat, "damage_type_tags").forEach(builder::outgoingFlatAdditionTag);
        }

        if (object.has("merge_mode")) {
            String modeName = GsonHelper.getAsString(object, "merge_mode").toUpperCase(java.util.Locale.ROOT);
            try {
                builder.mergeMode(DamageModifierProfileMergeMode.valueOf(modeName));
            } catch (IllegalArgumentException ignored) {
                throw new IllegalArgumentException("Unknown merge_mode '" + modeName
                        + "'; valid values: multiplicative, additive, override");
            }
        }

        if (object.has("priority")) {
            builder.priority(GsonHelper.getAsInt(object, "priority"));
        }

        return new LoadedDamageModifierProfileDefinition(profileId, builder.build());
    }

    private static Map<ResourceLocation, Double> readMultipliers(JsonObject object, String key) {
        if (!object.has(key)) {
            return Map.of();
        }
        JsonObject mapObject = GsonHelper.getAsJsonObject(object, key);
        LinkedHashMap<ResourceLocation, Double> values = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : mapObject.entrySet()) {
            values.put(ResourceLocation.parse(entry.getKey()), entry.getValue().getAsDouble());
        }
        return Map.copyOf(values);
    }

    /** Reads a map of ResourceLocation -> double without range constraints (used for flat additions). */
    private static Map<ResourceLocation, Double> readValues(JsonObject object, String key) {
        if (!object.has(key)) {
            return Map.of();
        }
        JsonObject mapObject = GsonHelper.getAsJsonObject(object, key);
        LinkedHashMap<ResourceLocation, Double> values = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : mapObject.entrySet()) {
            values.put(ResourceLocation.parse(entry.getKey()), entry.getValue().getAsDouble());
        }
        return Map.copyOf(values);
    }

    public record LoadedDamageModifierProfileDefinition(
            ResourceLocation id,
            DamageModifierProfileDefinition definition
    ) {}

    private static final class ReloadListener extends SimpleJsonResourceReloadListener {
        private ReloadListener() {
            super(GSON, DIRECTORY);
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> jsonById, ResourceManager resourceManager, ProfilerFiller profiler) {
            LinkedHashMap<ResourceLocation, LoadedDamageModifierProfileDefinition> definitions = new LinkedHashMap<>();
            for (Map.Entry<ResourceLocation, JsonElement> entry : jsonById.entrySet()) {
                try {
                    LoadedDamageModifierProfileDefinition definition = parseDefinition(entry.getKey(), entry.getValue());
                    LoadedDamageModifierProfileDefinition previous = definitions.putIfAbsent(definition.id(), definition);
                    if (previous != null) {
                        throw new IllegalStateException("Duplicate datapack damage modifier profile id: " + definition.id());
                    }
                } catch (RuntimeException exception) {
                    XLib.LOGGER.error("Failed to parse datapack damage modifier profile {}", entry.getKey(), exception);
                }
            }
            loadedDefinitions = Map.copyOf(definitions);
        }
    }
}
