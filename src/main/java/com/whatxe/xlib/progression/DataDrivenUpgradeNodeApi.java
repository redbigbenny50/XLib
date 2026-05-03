package com.whatxe.xlib.progression;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.whatxe.xlib.XLib;
import com.whatxe.xlib.ability.DataDrivenDefinitionReaders;
import java.util.ArrayList;
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

public final class DataDrivenUpgradeNodeApi {
    private static final Gson GSON = new GsonBuilder().create();
    private static final String DIRECTORY = XLib.MODID + "/upgrade_nodes";
    private static volatile Map<ResourceLocation, LoadedUpgradeNodeDefinition> loadedDefinitions = Map.of();
    private static volatile Map<ResourceLocation, LoadedUpgradeNodeDefinition> syncedDefinitions = Map.of();
    private static volatile Map<ResourceLocation, String> definitionJsonByFileId = Map.of();

    private DataDrivenUpgradeNodeApi() {}

    public static void bootstrap() {}

    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new ReloadListener());
    }

    public static Collection<ResourceLocation> allDefinitionIds() {
        return List.copyOf(resolvedDefinitions().keySet());
    }

    public static Optional<LoadedUpgradeNodeDefinition> findDefinition(ResourceLocation definitionId) {
        return Optional.ofNullable(resolvedDefinitions().get(definitionId));
    }

    public static Map<ResourceLocation, String> definitionJsonsForSync() {
        return Map.copyOf(definitionJsonByFileId);
    }

    public static void syncDefinitionsFromJson(Map<ResourceLocation, String> jsonByFileId) {
        syncedDefinitions = parseDefinitionsFromJson(jsonByFileId, "client-synced upgrade node");
    }

    public static void clearSyncedDefinitions() {
        syncedDefinitions = Map.of();
    }

    static void setDefinitionsForTesting(Map<ResourceLocation, LoadedUpgradeNodeDefinition> definitions) {
        loadedDefinitions = Map.copyOf(definitions);
    }

    static LoadedUpgradeNodeDefinition parseDefinition(ResourceLocation fileId, JsonElement element) {
        JsonObject object = GsonHelper.convertToJsonObject(element, "upgrade node");
        ResourceLocation nodeId = object.has("id")
                ? ResourceLocation.parse(GsonHelper.getAsString(object, "id"))
                : fileId;
        UpgradeNodeDefinition.Builder builder = UpgradeNodeDefinition.builder(nodeId);
        if (object.has("track")) {
            builder.track(ResourceLocation.parse(GsonHelper.getAsString(object, "track")));
        }
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
        if (object.has("choice_group")) {
            builder.choiceGroup(ResourceLocation.parse(GsonHelper.getAsString(object, "choice_group")));
        }
        for (Map.Entry<ResourceLocation, Integer> entry : readIntMap(object, "point_costs", true).entrySet()) {
            builder.pointCost(entry.getKey(), entry.getValue());
        }
        builder.requiredNodes(DataDrivenDefinitionReaders.readLocations(object, "required_node", "required_nodes"));
        builder.lockedNodes(DataDrivenDefinitionReaders.readLocations(object, "locked_node", "locked_nodes"));
        builder.lockedTracks(DataDrivenDefinitionReaders.readLocations(object, "locked_track", "locked_tracks"));
        for (UpgradeRequirement requirement : readRequirements(object)) {
            builder.requirement(requirement);
        }
        builder.rewards(readRewards(object));
        return new LoadedUpgradeNodeDefinition(nodeId, builder.build());
    }

    public record LoadedUpgradeNodeDefinition(
            ResourceLocation id,
            UpgradeNodeDefinition definition
    ) {}

    private static Map<ResourceLocation, LoadedUpgradeNodeDefinition> resolvedDefinitions() {
        Map<ResourceLocation, LoadedUpgradeNodeDefinition> definitions = new LinkedHashMap<>(loadedDefinitions);
        syncedDefinitions.forEach(definitions::putIfAbsent);
        return Map.copyOf(definitions);
    }

    private static List<UpgradeRequirement> readRequirements(JsonObject object) {
        List<UpgradeRequirement> requirements = new ArrayList<>();
        if (object.has("requirement")) {
            requirements.add(UpgradeRequirementJsonParser.parse(object.get("requirement")));
        }
        if (object.has("requirements")) {
            for (JsonElement element : GsonHelper.getAsJsonArray(object, "requirements")) {
                requirements.add(UpgradeRequirementJsonParser.parse(element));
            }
        }
        return List.copyOf(requirements);
    }

    private static UpgradeRewardBundle readRewards(JsonObject object) {
        if (!object.has("rewards")) {
            return UpgradeRewardBundle.builder().build();
        }
        JsonObject rewards = GsonHelper.getAsJsonObject(object, "rewards");
        return UpgradeRewardBundle.builder()
                .grantAbilities(DataDrivenDefinitionReaders.readLocations(rewards, "ability", "abilities"))
                .grantPassives(DataDrivenDefinitionReaders.readLocations(rewards, "passive", "passives"))
                .grantGrantedItems(DataDrivenDefinitionReaders.readLocations(rewards, "granted_item", "granted_items"))
                .grantRecipePermissions(DataDrivenDefinitionReaders.readLocations(rewards, "recipe_permission", "recipe_permissions"))
                .grantIdentities(DataDrivenDefinitionReaders.readLocations(rewards, "identity", "identities"))
                .build();
    }

    static Map<ResourceLocation, Integer> readIntMap(JsonObject object, String key, boolean positiveOnly) {
        Map<ResourceLocation, Integer> values = new LinkedHashMap<>();
        if (!object.has(key)) {
            return Map.of();
        }
        JsonObject mapObject = GsonHelper.getAsJsonObject(object, key);
        for (Map.Entry<String, JsonElement> entry : mapObject.entrySet()) {
            int value = entry.getValue().getAsInt();
            if (positiveOnly && value <= 0) {
                throw new IllegalArgumentException("Values in '" + key + "' must be positive");
            }
            values.put(ResourceLocation.parse(entry.getKey()), value);
        }
        return Map.copyOf(values);
    }

    private static Map<ResourceLocation, LoadedUpgradeNodeDefinition> parseDefinitionsFromJson(
            Map<ResourceLocation, String> jsonByFileId,
            String label
    ) {
        Map<ResourceLocation, LoadedUpgradeNodeDefinition> definitions = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, String> entry : jsonByFileId.entrySet()) {
            try {
                LoadedUpgradeNodeDefinition definition = parseDefinition(entry.getKey(), JsonParser.parseString(entry.getValue()));
                LoadedUpgradeNodeDefinition previous = definitions.putIfAbsent(definition.id(), definition);
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
            Map<ResourceLocation, LoadedUpgradeNodeDefinition> definitions = new LinkedHashMap<>();
            Map<ResourceLocation, String> definitionJsons = new LinkedHashMap<>();
            for (Map.Entry<ResourceLocation, JsonElement> entry : jsonById.entrySet()) {
                try {
                    LoadedUpgradeNodeDefinition definition = parseDefinition(entry.getKey(), entry.getValue());
                    LoadedUpgradeNodeDefinition previous = definitions.putIfAbsent(definition.id(), definition);
                    if (previous != null) {
                        throw new IllegalStateException("Duplicate datapack upgrade node id: " + definition.id());
                    }
                    definitionJsons.put(entry.getKey(), GSON.toJson(entry.getValue()));
                } catch (RuntimeException exception) {
                    XLib.LOGGER.error("Failed to parse datapack upgrade node {}", entry.getKey(), exception);
                }
            }
            loadedDefinitions = Map.copyOf(definitions);
            definitionJsonByFileId = Map.copyOf(definitionJsons);
        }
    }
}
