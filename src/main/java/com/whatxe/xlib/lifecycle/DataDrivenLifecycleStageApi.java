package com.whatxe.xlib.lifecycle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.whatxe.xlib.XLib;
import com.whatxe.xlib.ability.GrantBundleApi;
import com.whatxe.xlib.ability.IdentityApi;
import com.whatxe.xlib.ability.StateFlagApi;
import com.whatxe.xlib.capability.CapabilityPolicyApi;
import com.whatxe.xlib.form.VisualFormApi;
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

public final class DataDrivenLifecycleStageApi {
    private static final Gson GSON = new GsonBuilder().create();
    private static final String DIRECTORY = XLib.MODID + "/lifecycle_stages";
    private static volatile Map<ResourceLocation, LoadedLifecycleStageDefinition> loadedDefinitions = Map.of();
    private static volatile Map<ResourceLocation, LoadedLifecycleStageDefinition> syncedDefinitions = Map.of();
    private static volatile Map<ResourceLocation, String> definitionJsonByFileId = Map.of();

    private DataDrivenLifecycleStageApi() {}

    public static void bootstrap() {}

    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new ReloadListener());
    }

    public static Collection<ResourceLocation> allDefinitionIds() {
        return List.copyOf(resolvedDefinitions().keySet());
    }

    public static Optional<LoadedLifecycleStageDefinition> findDefinition(ResourceLocation definitionId) {
        return Optional.ofNullable(resolvedDefinitions().get(definitionId));
    }

    public static Map<ResourceLocation, String> definitionJsonsForSync() {
        return Map.copyOf(definitionJsonByFileId);
    }

    public static void syncDefinitionsFromJson(Map<ResourceLocation, String> jsonByFileId) {
        syncedDefinitions = parseDefinitionsFromJson(jsonByFileId, "client-synced lifecycle_stage");
    }

    public static void clearSyncedDefinitions() {
        syncedDefinitions = Map.of();
    }

    static void setDefinitionsForTesting(Map<ResourceLocation, LoadedLifecycleStageDefinition> definitions) {
        loadedDefinitions = Map.copyOf(definitions);
    }

    static LoadedLifecycleStageDefinition parseDefinition(ResourceLocation fileId, JsonElement element) {
        JsonObject object = GsonHelper.convertToJsonObject(element, "lifecycle_stage");
        ResourceLocation stageId = object.has("id")
                ? ResourceLocation.parse(GsonHelper.getAsString(object, "id"))
                : fileId;

        LifecycleStageDefinition.Builder builder = LifecycleStageDefinition.builder(stageId);

        if (object.has("duration_ticks")) {
            int duration = GsonHelper.getAsInt(object, "duration_ticks");
            if (duration <= 0) {
                throw new IllegalArgumentException("duration_ticks must be positive, got " + duration);
            }
            builder.durationTicks(duration);
        }

        if (object.has("auto_transitions")) {
            JsonArray transitions = GsonHelper.getAsJsonArray(object, "auto_transitions");
            for (JsonElement transitionElement : transitions) {
                builder.autoTransition(parseTransition(transitionElement));
            }
        }

        if (object.has("manual_transition_targets")) {
            for (JsonElement targetElement : GsonHelper.getAsJsonArray(object, "manual_transition_targets")) {
                builder.allowTransitionTo(ResourceLocation.parse(targetElement.getAsString()));
            }
        }

        if (object.has("project_state_flags")) {
            for (JsonElement flagElement : GsonHelper.getAsJsonArray(object, "project_state_flags")) {
                builder.projectStateFlag(ResourceLocation.parse(flagElement.getAsString()));
            }
        }

        if (object.has("project_grant_bundles")) {
            for (JsonElement bundleElement : GsonHelper.getAsJsonArray(object, "project_grant_bundles")) {
                builder.projectGrantBundle(ResourceLocation.parse(bundleElement.getAsString()));
            }
        }

        if (object.has("project_identities")) {
            for (JsonElement identityElement : GsonHelper.getAsJsonArray(object, "project_identities")) {
                builder.projectIdentity(ResourceLocation.parse(identityElement.getAsString()));
            }
        }

        if (object.has("project_capability_policies")) {
            for (JsonElement policyElement : GsonHelper.getAsJsonArray(object, "project_capability_policies")) {
                builder.projectCapabilityPolicy(ResourceLocation.parse(policyElement.getAsString()));
            }
        }

        if (object.has("project_visual_form")) {
            builder.projectVisualForm(ResourceLocation.parse(GsonHelper.getAsString(object, "project_visual_form")));
        }

        return new LoadedLifecycleStageDefinition(stageId, builder.build());
    }

    private static LifecycleStageTransition parseTransition(JsonElement element) {
        JsonObject object = GsonHelper.convertToJsonObject(element, "auto_transition");
        ResourceLocation target = ResourceLocation.parse(GsonHelper.getAsString(object, "target"));
        String triggerName = GsonHelper.getAsString(object, "trigger").toUpperCase(java.util.Locale.ROOT);
        LifecycleStageTrigger trigger;
        try {
            trigger = LifecycleStageTrigger.valueOf(triggerName);
        } catch (IllegalArgumentException ignored) {
            throw new IllegalArgumentException("Unknown lifecycle trigger '" + triggerName
                    + "'; valid values: timer, manual, death, respawn, advancement, condition");
        }
        boolean preserveElapsed = object.has("preserve_elapsed")
                && GsonHelper.getAsBoolean(object, "preserve_elapsed");
        return new LifecycleStageTransition(target, trigger, preserveElapsed);
    }

    public record LoadedLifecycleStageDefinition(
            ResourceLocation id,
            LifecycleStageDefinition definition
    ) {}

    private static Map<ResourceLocation, LoadedLifecycleStageDefinition> resolvedDefinitions() {
        Map<ResourceLocation, LoadedLifecycleStageDefinition> definitions = new LinkedHashMap<>(loadedDefinitions);
        syncedDefinitions.forEach(definitions::putIfAbsent);
        return Map.copyOf(definitions);
    }

    private static Map<ResourceLocation, LoadedLifecycleStageDefinition> parseDefinitionsFromJson(
            Map<ResourceLocation, String> jsonByFileId,
            String label
    ) {
        Map<ResourceLocation, LoadedLifecycleStageDefinition> definitions = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, String> entry : jsonByFileId.entrySet()) {
            try {
                LoadedLifecycleStageDefinition definition = parseDefinition(entry.getKey(), JsonParser.parseString(entry.getValue()));
                LoadedLifecycleStageDefinition previous = definitions.putIfAbsent(definition.id(), definition);
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
            Map<ResourceLocation, LoadedLifecycleStageDefinition> definitions = new LinkedHashMap<>();
            Map<ResourceLocation, String> definitionJsons = new LinkedHashMap<>();
            for (Map.Entry<ResourceLocation, JsonElement> entry : jsonById.entrySet()) {
                try {
                    LoadedLifecycleStageDefinition definition = parseDefinition(entry.getKey(), entry.getValue());
                    LoadedLifecycleStageDefinition previous = definitions.putIfAbsent(definition.id(), definition);
                    if (previous != null) {
                        throw new IllegalStateException("Duplicate datapack lifecycle_stage id: " + definition.id());
                    }
                    definitionJsons.put(entry.getKey(), GSON.toJson(entry.getValue()));
                } catch (RuntimeException exception) {
                    XLib.LOGGER.error("Failed to parse datapack lifecycle_stage {}", entry.getKey(), exception);
                }
            }
            loadedDefinitions = Map.copyOf(definitions);
            definitionJsonByFileId = Map.copyOf(definitionJsons);
            validateCrossReferences(definitions);
        }

        private static void validateCrossReferences(Map<ResourceLocation, LoadedLifecycleStageDefinition> definitions) {
            for (LoadedLifecycleStageDefinition loaded : definitions.values()) {
                LifecycleStageDefinition def = loaded.definition();
                ResourceLocation stageId = loaded.id();

                for (LifecycleStageTransition transition : def.autoTransitions()) {
                    if (!definitions.containsKey(transition.targetStageId())) {
                        XLib.LOGGER.warn("[xlib] lifecycle_stage '{}': auto_transition target '{}' is not a known lifecycle stage",
                                stageId, transition.targetStageId());
                    }
                }
                for (ResourceLocation targetId : def.manualTransitionTargets()) {
                    if (!definitions.containsKey(targetId)) {
                        XLib.LOGGER.warn("[xlib] lifecycle_stage '{}': manual_transition_target '{}' is not a known lifecycle stage",
                                stageId, targetId);
                    }
                }

                for (ResourceLocation bundleId : def.projectedGrantBundles()) {
                    if (GrantBundleApi.findBundle(bundleId).isEmpty()) {
                        XLib.LOGGER.warn("[xlib] lifecycle_stage '{}': projected grant bundle '{}' is not registered", stageId, bundleId);
                    }
                }
                for (ResourceLocation identityId : def.projectedIdentities()) {
                    if (IdentityApi.findIdentity(identityId).isEmpty()) {
                        XLib.LOGGER.warn("[xlib] lifecycle_stage '{}': projected identity '{}' is not registered", stageId, identityId);
                    }
                }
                for (ResourceLocation flagId : def.projectedStateFlags()) {
                    if (StateFlagApi.findStateFlag(flagId).isEmpty()) {
                        XLib.LOGGER.warn("[xlib] lifecycle_stage '{}': projected state flag '{}' is not registered", stageId, flagId);
                    }
                }
                for (ResourceLocation policyId : def.projectedCapabilityPolicies()) {
                    if (CapabilityPolicyApi.find(policyId).isEmpty()) {
                        XLib.LOGGER.warn("[xlib] lifecycle_stage '{}': projected capability policy '{}' is not registered", stageId, policyId);
                    }
                }
                def.projectedVisualForm().ifPresent(formId -> {
                    if (VisualFormApi.findDefinition(formId).isEmpty()) {
                        XLib.LOGGER.warn("[xlib] lifecycle_stage '{}': projected visual form '{}' is not registered", stageId, formId);
                    }
                });
            }
        }
    }
}
