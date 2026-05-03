package com.whatxe.xlib.ability;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.whatxe.xlib.XLib;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

public final class DataDrivenProfileGroupApi {
    private static final Gson GSON = new GsonBuilder().create();
    private static final String DIRECTORY = XLib.MODID + "/profile_groups";
    private static volatile Map<ResourceLocation, LoadedProfileGroupDefinition> loadedDefinitions = Map.of();
    private static volatile Map<ResourceLocation, LoadedProfileGroupDefinition> syncedDefinitions = Map.of();
    private static volatile Map<ResourceLocation, String> definitionJsonByFileId = Map.of();

    private DataDrivenProfileGroupApi() {}

    public static void bootstrap() {}

    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new ReloadListener());
    }

    public static Collection<ResourceLocation> allDefinitionIds() {
        return List.copyOf(resolvedDefinitions().keySet());
    }

    public static Optional<LoadedProfileGroupDefinition> findDefinition(ResourceLocation definitionId) {
        return Optional.ofNullable(resolvedDefinitions().get(definitionId));
    }

    public static Map<ResourceLocation, String> definitionJsonsForSync() {
        return Map.copyOf(definitionJsonByFileId);
    }

    public static void syncDefinitionsFromJson(Map<ResourceLocation, String> jsonByFileId) {
        syncedDefinitions = parseDefinitionsFromJson(jsonByFileId, "client-synced profile group");
    }

    public static void clearSyncedDefinitions() {
        syncedDefinitions = Map.of();
    }

    static void setDefinitionsForTesting(Map<ResourceLocation, LoadedProfileGroupDefinition> definitions) {
        loadedDefinitions = Map.copyOf(definitions);
    }

    static LoadedProfileGroupDefinition parseDefinition(ResourceLocation fileId, JsonElement element) {
        JsonObject object = GsonHelper.convertToJsonObject(element, "profile group");
        ResourceLocation groupId = object.has("id")
                ? ResourceLocation.parse(GsonHelper.getAsString(object, "id"))
                : fileId;
        Set<ProfileOnboardingTrigger> onboardingTriggers = readOnboardingTriggers(object);
        ProfileGroupDefinition definition = new ProfileGroupDefinition(
                groupId,
                DataDrivenDefinitionReaders.readComponent(object, "display_name", Component.literal(groupId.toString())),
                DataDrivenDefinitionReaders.readComponent(object, "description", Component.empty()),
                DataDrivenDefinitionReaders.readOptionalIcon(object),
                object.has("selection_limit") ? GsonHelper.getAsInt(object, "selection_limit") : 1,
                object.has("required_onboarding") && GsonHelper.getAsBoolean(object, "required_onboarding"),
                onboardingTriggers,
                !object.has("auto_open_menu") || GsonHelper.getAsBoolean(object, "auto_open_menu"),
                object.has("blocks_ability_use") && GsonHelper.getAsBoolean(object, "blocks_ability_use"),
                object.has("blocks_ability_menu") && GsonHelper.getAsBoolean(object, "blocks_ability_menu"),
                object.has("blocks_progression") && GsonHelper.getAsBoolean(object, "blocks_progression"),
                object.has("player_can_reset") && GsonHelper.getAsBoolean(object, "player_can_reset"),
                !object.has("admin_can_reset") || GsonHelper.getAsBoolean(object, "admin_can_reset"),
                object.has("reopen_on_reset") && GsonHelper.getAsBoolean(object, "reopen_on_reset")
        );
        return new LoadedProfileGroupDefinition(groupId, definition);
    }

    public record LoadedProfileGroupDefinition(
            ResourceLocation id,
            ProfileGroupDefinition definition
    ) {}

    private static Map<ResourceLocation, LoadedProfileGroupDefinition> resolvedDefinitions() {
        Map<ResourceLocation, LoadedProfileGroupDefinition> definitions = new LinkedHashMap<>(loadedDefinitions);
        syncedDefinitions.forEach(definitions::putIfAbsent);
        return Map.copyOf(definitions);
    }

    private static Set<ProfileOnboardingTrigger> readOnboardingTriggers(JsonObject object) {
        Set<ProfileOnboardingTrigger> triggers = new LinkedHashSet<>();
        if (object.has("onboarding_trigger")) {
            triggers.add(parseOnboardingTrigger(GsonHelper.getAsString(object, "onboarding_trigger")));
        }
        if (object.has("onboarding_triggers")) {
            for (JsonElement element : GsonHelper.getAsJsonArray(object, "onboarding_triggers")) {
                triggers.add(parseOnboardingTrigger(element.getAsString()));
            }
        }
        return Set.copyOf(triggers);
    }

    private static ProfileOnboardingTrigger parseOnboardingTrigger(String value) {
        try {
            return ProfileOnboardingTrigger.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unknown profile onboarding trigger: " + value, exception);
        }
    }

    private static Map<ResourceLocation, LoadedProfileGroupDefinition> parseDefinitionsFromJson(
            Map<ResourceLocation, String> jsonByFileId,
            String label
    ) {
        Map<ResourceLocation, LoadedProfileGroupDefinition> definitions = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, String> entry : jsonByFileId.entrySet()) {
            try {
                LoadedProfileGroupDefinition definition = parseDefinition(entry.getKey(), JsonParser.parseString(entry.getValue()));
                LoadedProfileGroupDefinition previous = definitions.putIfAbsent(definition.id(), definition);
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
            Map<ResourceLocation, LoadedProfileGroupDefinition> definitions = new LinkedHashMap<>();
            Map<ResourceLocation, String> definitionJsons = new LinkedHashMap<>();
            for (Map.Entry<ResourceLocation, JsonElement> entry : jsonById.entrySet()) {
                try {
                    LoadedProfileGroupDefinition definition = parseDefinition(entry.getKey(), entry.getValue());
                    LoadedProfileGroupDefinition previous = definitions.putIfAbsent(definition.id(), definition);
                    if (previous != null) {
                        throw new IllegalStateException("Duplicate datapack profile group id: " + definition.id());
                    }
                    definitionJsons.put(entry.getKey(), GSON.toJson(entry.getValue()));
                } catch (RuntimeException exception) {
                    XLib.LOGGER.error("Failed to parse datapack profile group {}", entry.getKey(), exception);
                }
            }
            loadedDefinitions = Map.copyOf(definitions);
            definitionJsonByFileId = Map.copyOf(definitionJsons);
        }
    }
}
