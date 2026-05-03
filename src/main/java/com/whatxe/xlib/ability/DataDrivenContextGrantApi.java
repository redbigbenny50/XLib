package com.whatxe.xlib.ability;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.whatxe.xlib.XLib;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

public final class DataDrivenContextGrantApi {
    private static final Gson GSON = new GsonBuilder().create();
    private static final String DIRECTORY = XLib.MODID + "/context_grants";
    private static volatile Map<ResourceLocation, LoadedContextGrantDefinition> loadedDefinitions = Map.of();

    private DataDrivenContextGrantApi() {}

    public static void bootstrap() {}

    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new ReloadListener());
    }

    public static Collection<ResourceLocation> allDefinitionIds() {
        return List.copyOf(loadedDefinitions.keySet());
    }

    public static Optional<LoadedContextGrantDefinition> findDefinition(ResourceLocation definitionId) {
        return Optional.ofNullable(loadedDefinitions.get(definitionId));
    }

    public static Collection<ContextGrantSnapshot> collectSnapshots(ServerPlayer player, AbilityData currentData) {
        Map<ResourceLocation, ContextGrantSnapshot> mergedSnapshots = new LinkedHashMap<>();
        for (LoadedContextGrantDefinition definition : loadedDefinitions.values()) {
            try {
                if (definition.requirement().validate(player, currentData).isPresent() || definition.snapshot().isEmpty()) {
                    continue;
                }
                mergedSnapshots.merge(definition.snapshot().sourceId(), definition.snapshot(), ContextGrantSnapshot::merge);
            } catch (RuntimeException exception) {
                XLib.LOGGER.error(
                        "Datapack context grant {} failed for player {}",
                        definition.id(),
                        player.getGameProfile().getName(),
                        exception
                );
            }
        }
        return List.copyOf(mergedSnapshots.values());
    }

    static void setDefinitionsForTesting(Map<ResourceLocation, LoadedContextGrantDefinition> definitions) {
        loadedDefinitions = Map.copyOf(definitions);
    }

    static LoadedContextGrantDefinition parseDefinition(ResourceLocation fileId, JsonElement element) {
        JsonObject object = GsonHelper.convertToJsonObject(element, "context grant");
        ResourceLocation sourceId = object.has("source")
                ? ResourceLocation.parse(GsonHelper.getAsString(object, "source"))
                : object.has("source_id")
                ? ResourceLocation.parse(GsonHelper.getAsString(object, "source_id"))
                : fileId;
        AbilityRequirement requirement = AbilityRequirementJsonParser.parse(
                object.get("when"),
                DataDrivenConditionApi::requireCondition
        );
        ContextGrantSnapshot.Builder snapshot = ContextGrantSnapshot.builder(sourceId);
        snapshot.grantAbilities(readLocations(object, "grant_ability", "grant_abilities"));
        snapshot.grantPassives(readLocations(object, "grant_passive", "grant_passives"));
        snapshot.grantGrantedItems(readLocations(object, "grant_granted_item", "grant_granted_items"));
        snapshot.grantRecipePermissions(readLocations(object, "grant_recipe_permission", "grant_recipe_permissions"));
        snapshot.blockAbilities(readLocations(object, "block_ability", "block_abilities"));
        snapshot.grantStatePolicies(readLocations(object, "grant_state_policy", "grant_state_policies"));
        snapshot.grantStateFlags(readLocations(object, "grant_state_flag", "grant_state_flags"));
        return new LoadedContextGrantDefinition(fileId, requirement, snapshot.build());
    }

    private static void setLoadedDefinitions(Map<ResourceLocation, LoadedContextGrantDefinition> definitions) {
        loadedDefinitions = Map.copyOf(definitions);
    }

    private static List<ResourceLocation> readLocations(JsonObject object, String singleKey, String pluralKey) {
        List<ResourceLocation> values = new ArrayList<>();
        if (singleKey != null && object.has(singleKey)) {
            values.add(ResourceLocation.parse(GsonHelper.getAsString(object, singleKey)));
        }
        if (pluralKey != null && object.has(pluralKey)) {
            for (JsonElement element : GsonHelper.getAsJsonArray(object, pluralKey)) {
                values.add(ResourceLocation.parse(element.getAsString()));
            }
        }
        return List.copyOf(values);
    }

    public record LoadedContextGrantDefinition(
            ResourceLocation id,
            AbilityRequirement requirement,
            ContextGrantSnapshot snapshot
    ) {}

    private static final class ReloadListener extends SimpleJsonResourceReloadListener {
        private ReloadListener() {
            super(GSON, DIRECTORY);
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> jsonById, ResourceManager resourceManager, ProfilerFiller profiler) {
            Map<ResourceLocation, LoadedContextGrantDefinition> definitions = new LinkedHashMap<>();
            for (Map.Entry<ResourceLocation, JsonElement> entry : jsonById.entrySet()) {
                try {
                    definitions.put(entry.getKey(), parseDefinition(entry.getKey(), entry.getValue()));
                } catch (RuntimeException exception) {
                    XLib.LOGGER.error("Failed to parse datapack context grant {}", entry.getKey(), exception);
                }
            }
            setLoadedDefinitions(definitions);
        }
    }
}
