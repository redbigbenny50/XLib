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

public final class DataDrivenAbilityApi {
    private static final Gson GSON = new GsonBuilder().create();
    private static final String DIRECTORY = XLib.MODID + "/abilities";
    private static volatile Map<ResourceLocation, LoadedAbilityDefinition> loadedDefinitions = Map.of();
    private static volatile Map<ResourceLocation, LoadedAbilityDefinition> syncedDefinitions = Map.of();
    private static volatile Map<ResourceLocation, String> definitionJsonByFileId = Map.of();

    private DataDrivenAbilityApi() {}

    public static void bootstrap() {}

    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new ReloadListener());
    }

    public static Collection<ResourceLocation> allDefinitionIds() {
        return List.copyOf(resolvedDefinitions().keySet());
    }

    public static Optional<LoadedAbilityDefinition> findDefinition(ResourceLocation definitionId) {
        return Optional.ofNullable(resolvedDefinitions().get(definitionId));
    }

    public static Map<ResourceLocation, String> definitionJsonsForSync() {
        return Map.copyOf(definitionJsonByFileId);
    }

    public static void syncDefinitionsFromJson(Map<ResourceLocation, String> jsonByFileId) {
        syncedDefinitions = parseDefinitionsFromJson(jsonByFileId, "client-synced ability");
    }

    public static void clearSyncedDefinitions() {
        syncedDefinitions = Map.of();
    }

    static void setDefinitionsForTesting(Map<ResourceLocation, LoadedAbilityDefinition> definitions) {
        loadedDefinitions = Map.copyOf(definitions);
    }

    static LoadedAbilityDefinition parseDefinition(ResourceLocation fileId, JsonElement element) {
        JsonObject object = GsonHelper.convertToJsonObject(element, "ability");
        ResourceLocation abilityId = object.has("id")
                ? ResourceLocation.parse(GsonHelper.getAsString(object, "id"))
                : fileId;

        AbilityDefinition.Builder builder = AbilityDefinition.builder(abilityId, DataDrivenDefinitionReaders.readRequiredIcon(object))
                .displayName(DataDrivenDefinitionReaders.readComponent(object, "display_name", Component.literal(abilityId.toString())));
        if (object.has("description")) {
            builder.description(DataDrivenDefinitionReaders.readComponent(object, "description", Component.empty()));
        } else {
            builder.emptyDescription();
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

        if (object.has("cooldown_ticks")) {
            builder.cooldownTicks(GsonHelper.getAsInt(object, "cooldown_ticks"));
        }
        if (object.has("cooldown_policy")) {
            builder.cooldownPolicy(AbilityCooldownPolicy.valueOf(
                    GsonHelper.getAsString(object, "cooldown_policy").trim().toUpperCase(java.util.Locale.ROOT)
            ));
        }
        if (GsonHelper.getAsBoolean(object, "toggle_ability", false)) {
            builder.toggleAbility();
        }
        if (object.has("duration_ticks")) {
            builder.durationTicks(GsonHelper.getAsInt(object, "duration_ticks"));
        }
        if (object.has("charges")) {
            JsonObject charges = GsonHelper.getAsJsonObject(object, "charges");
            builder.charges(GsonHelper.getAsInt(charges, "max"), GsonHelper.getAsInt(charges, "recharge_ticks"));
        } else if (object.has("max_charges")) {
            builder.charges(GsonHelper.getAsInt(object, "max_charges"), GsonHelper.getAsInt(object, "charge_recharge_ticks"));
        }

        for (AbilityRequirement requirement : DataDrivenRuntimeEffects.readRequirements(object, "assign_requirement", "assign_requirements")) {
            builder.assignRequirement(requirement);
        }
        for (AbilityRequirement requirement : DataDrivenRuntimeEffects.readRequirements(object, "activate_requirement", "activate_requirements")) {
            builder.activateRequirement(requirement);
        }
        for (AbilityRequirement requirement : DataDrivenRuntimeEffects.readRequirements(object, "stay_active_requirement", "stay_active_requirements")) {
            builder.stayActiveRequirement(requirement);
        }
        for (AbilityRequirement requirement : DataDrivenRuntimeEffects.readRequirements(object, "render_requirement", "render_requirements")) {
            builder.renderRequirement(requirement);
        }

        if (object.has("resource_costs")) {
            JsonObject resourceCosts = GsonHelper.getAsJsonObject(object, "resource_costs");
            for (Map.Entry<String, JsonElement> entry : resourceCosts.entrySet()) {
                builder.resourceCost(ResourceLocation.parse(entry.getKey()), entry.getValue().getAsInt());
            }
        }
        for (Map.Entry<AbilitySoundTrigger, List<AbilitySound>> entry : DataDrivenRuntimeEffects.readAbilitySounds(object).entrySet()) {
            for (AbilitySound sound : entry.getValue()) {
                builder.sound(entry.getKey(), sound);
            }
        }

        builder.action(DataDrivenRuntimeEffects.parseAbilityAction(abilityId, object));

        List<DataDrivenRuntimeEffects.Effect> tickEffects = DataDrivenRuntimeEffects.readEffects(object, "tick_effects");
        if (!tickEffects.isEmpty()) {
            builder.ticker(DataDrivenRuntimeEffects.tickerFromEffects(abilityId, tickEffects));
        }
        List<DataDrivenRuntimeEffects.Effect> endEffects = DataDrivenRuntimeEffects.readEffects(object, "end_effects");
        if (!endEffects.isEmpty()) {
            builder.ender(DataDrivenRuntimeEffects.enderFromEffects(abilityId, endEffects));
        }

        return new LoadedAbilityDefinition(abilityId, builder.build());
    }

    public record LoadedAbilityDefinition(
            ResourceLocation id,
            AbilityDefinition definition
    ) {}

    private static Map<ResourceLocation, LoadedAbilityDefinition> resolvedDefinitions() {
        Map<ResourceLocation, LoadedAbilityDefinition> definitions = new LinkedHashMap<>(loadedDefinitions);
        syncedDefinitions.forEach(definitions::putIfAbsent);
        return Map.copyOf(definitions);
    }

    private static Map<ResourceLocation, LoadedAbilityDefinition> parseDefinitionsFromJson(
            Map<ResourceLocation, String> jsonByFileId,
            String label
    ) {
        Map<ResourceLocation, LoadedAbilityDefinition> definitions = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, String> entry : jsonByFileId.entrySet()) {
            try {
                LoadedAbilityDefinition definition = parseDefinition(entry.getKey(), JsonParser.parseString(entry.getValue()));
                LoadedAbilityDefinition previous = definitions.putIfAbsent(definition.id(), definition);
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
            Map<ResourceLocation, LoadedAbilityDefinition> definitions = new LinkedHashMap<>();
            Map<ResourceLocation, String> definitionJsons = new LinkedHashMap<>();
            for (Map.Entry<ResourceLocation, JsonElement> entry : jsonById.entrySet()) {
                try {
                    LoadedAbilityDefinition definition = parseDefinition(entry.getKey(), entry.getValue());
                    LoadedAbilityDefinition previous = definitions.putIfAbsent(definition.id(), definition);
                    if (previous != null) {
                        throw new IllegalStateException("Duplicate datapack ability id: " + definition.id());
                    }
                    definitionJsons.put(entry.getKey(), GSON.toJson(entry.getValue()));
                } catch (RuntimeException exception) {
                    XLib.LOGGER.error("Failed to parse datapack ability {}", entry.getKey(), exception);
                }
            }
            loadedDefinitions = Map.copyOf(definitions);
            definitionJsonByFileId = Map.copyOf(definitionJsons);
        }
    }
}
