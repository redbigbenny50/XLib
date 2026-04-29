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

public final class DataDrivenModeApi {
    private static final Gson GSON = new GsonBuilder().create();
    private static final String DIRECTORY = XLib.MODID + "/modes";
    private static volatile Map<ResourceLocation, LoadedModeDefinition> loadedDefinitions = Map.of();

    private DataDrivenModeApi() {}

    public static void bootstrap() {}

    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new ReloadListener());
    }

    public static Collection<ResourceLocation> allDefinitionIds() {
        return List.copyOf(loadedDefinitions.keySet());
    }

    public static Optional<LoadedModeDefinition> findDefinition(ResourceLocation definitionId) {
        return Optional.ofNullable(loadedDefinitions.get(definitionId));
    }

    static void setDefinitionsForTesting(Map<ResourceLocation, LoadedModeDefinition> definitions) {
        loadedDefinitions = Map.copyOf(definitions);
    }

    static LoadedModeDefinition parseDefinition(ResourceLocation fileId, JsonElement element) {
        JsonObject object = GsonHelper.convertToJsonObject(element, "mode");
        ResourceLocation abilityId = object.has("ability")
                ? ResourceLocation.parse(GsonHelper.getAsString(object, "ability"))
                : object.has("id")
                ? ResourceLocation.parse(GsonHelper.getAsString(object, "id"))
                : fileId;

        ModeDefinition.Builder builder = ModeDefinition.builder(abilityId);
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
        if (object.has("priority")) {
            builder.priority(GsonHelper.getAsInt(object, "priority"));
        }
        if (GsonHelper.getAsBoolean(object, "stackable", false)
                || GsonHelper.getAsBoolean(object, "overlay_mode", false)) {
            builder.stackable();
        }
        if (object.has("cycle_group")) {
            builder.cycleGroup(ResourceLocation.parse(GsonHelper.getAsString(object, "cycle_group")));
        }
        if (object.has("cycle_order")) {
            builder.cycleOrder(GsonHelper.getAsInt(object, "cycle_order"));
        }
        for (ResourceLocation groupId : DataDrivenDefinitionReaders.readLocations(object, "reset_cycle_group", "reset_cycle_groups")) {
            builder.resetCycleGroupOnActivate(groupId);
        }
        if (GsonHelper.getAsBoolean(object, "reset_own_cycle_group_on_activate", false)) {
            builder.resetOwnCycleGroupOnActivate();
        }
        if (object.has("cooldown_tick_rate_multiplier")) {
            builder.cooldownTickRateMultiplier(GsonHelper.getAsDouble(object, "cooldown_tick_rate_multiplier"));
        }
        if (object.has("health_cost_per_tick")) {
            builder.healthCostPerTick(GsonHelper.getAsDouble(object, "health_cost_per_tick"));
        }
        if (object.has("minimum_health")) {
            builder.minimumHealth(GsonHelper.getAsDouble(object, "minimum_health"));
        }
        for (Map.Entry<ResourceLocation, Double> entry : readDoubleMap(object, "resource_delta_per_tick").entrySet()) {
            builder.resourceDeltaPerTick(entry.getKey(), entry.getValue());
        }
        builder.exclusiveWith(DataDrivenDefinitionReaders.readLocations(object, "exclusive_mode", "exclusive_modes"));
        builder.blockedByModes(DataDrivenDefinitionReaders.readLocations(object, "blocked_by_mode", "blocked_by_modes"));
        builder.transformsFrom(readTransformsFrom(object));
        for (Map.Entry<Integer, ResourceLocation> entry : readPrimarySlotAbilityMap(object, "overlay_abilities").entrySet()) {
            builder.overlayAbility(entry.getKey(), entry.getValue());
        }
        builder.grantAbilities(DataDrivenDefinitionReaders.readLocations(object, "grant_ability", "grant_abilities"));
        builder.grantPassives(DataDrivenDefinitionReaders.readLocations(object, "grant_passive", "grant_passives"));
        builder.grantGrantedItems(DataDrivenDefinitionReaders.readLocations(object, "grant_granted_item", "grant_granted_items"));
        builder.grantRecipePermissions(DataDrivenDefinitionReaders.readLocations(object, "grant_recipe_permission", "grant_recipe_permissions"));
        builder.blockAbilities(DataDrivenDefinitionReaders.readLocations(object, "block_ability", "block_abilities"));
        builder.statePolicies(DataDrivenDefinitionReaders.readLocations(object, "grant_state_policy", "grant_state_policies"));
        builder.stateFlags(DataDrivenDefinitionReaders.readLocations(object, "grant_state_flag", "grant_state_flags"));
        return new LoadedModeDefinition(abilityId, builder.build());
    }

    public record LoadedModeDefinition(
            ResourceLocation id,
            ModeDefinition definition
    ) {}

    private static Collection<ResourceLocation> readTransformsFrom(JsonObject object) {
        if (object.has("transforms_from") && object.get("transforms_from").isJsonPrimitive()) {
            return DataDrivenDefinitionReaders.readLocations(object, "transforms_from", null);
        }
        return DataDrivenDefinitionReaders.readLocations(object, "transform_from", "transforms_from");
    }

    private static Map<ResourceLocation, Double> readDoubleMap(JsonObject object, String key) {
        if (!object.has(key)) {
            return Map.of();
        }
        Map<ResourceLocation, Double> values = new LinkedHashMap<>();
        JsonObject mapObject = GsonHelper.getAsJsonObject(object, key);
        for (Map.Entry<String, JsonElement> entry : mapObject.entrySet()) {
            values.put(ResourceLocation.parse(entry.getKey()), entry.getValue().getAsDouble());
        }
        return Map.copyOf(values);
    }

    private static Map<Integer, ResourceLocation> readPrimarySlotAbilityMap(JsonObject object, String key) {
        if (!object.has(key)) {
            return Map.of();
        }
        Map<Integer, ResourceLocation> values = new LinkedHashMap<>();
        JsonObject mapObject = GsonHelper.getAsJsonObject(object, key);
        for (Map.Entry<String, JsonElement> entry : mapObject.entrySet()) {
            values.put(Integer.parseInt(entry.getKey()), ResourceLocation.parse(entry.getValue().getAsString()));
        }
        return Map.copyOf(values);
    }

    private static final class ReloadListener extends SimpleJsonResourceReloadListener {
        private ReloadListener() {
            super(GSON, DIRECTORY);
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> jsonById, ResourceManager resourceManager, ProfilerFiller profiler) {
            Map<ResourceLocation, LoadedModeDefinition> definitions = new LinkedHashMap<>();
            for (Map.Entry<ResourceLocation, JsonElement> entry : jsonById.entrySet()) {
                try {
                    LoadedModeDefinition definition = parseDefinition(entry.getKey(), entry.getValue());
                    LoadedModeDefinition previous = definitions.putIfAbsent(definition.id(), definition);
                    if (previous != null) {
                        throw new IllegalStateException("Duplicate datapack mode id: " + definition.id());
                    }
                } catch (RuntimeException exception) {
                    XLib.LOGGER.error("Failed to parse datapack mode {}", entry.getKey(), exception);
                }
            }
            loadedDefinitions = Map.copyOf(definitions);
        }
    }
}
