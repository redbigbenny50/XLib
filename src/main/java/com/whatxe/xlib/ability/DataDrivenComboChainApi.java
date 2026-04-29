package com.whatxe.xlib.ability;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
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
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

public final class DataDrivenComboChainApi {
    private static final Gson GSON = new GsonBuilder().create();
    private static final String DIRECTORY = XLib.MODID + "/combo_chains";
    private static volatile Map<ResourceLocation, LoadedComboChainDefinition> loadedDefinitions = Map.of();

    private DataDrivenComboChainApi() {}

    public static void bootstrap() {}

    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new ReloadListener());
    }

    public static Collection<ResourceLocation> allDefinitionIds() {
        return List.copyOf(loadedDefinitions.keySet());
    }

    public static Optional<LoadedComboChainDefinition> findDefinition(ResourceLocation definitionId) {
        return Optional.ofNullable(loadedDefinitions.get(definitionId));
    }

    static void setDefinitionsForTesting(Map<ResourceLocation, LoadedComboChainDefinition> definitions) {
        loadedDefinitions = Map.copyOf(definitions);
    }

    static LoadedComboChainDefinition parseDefinition(ResourceLocation fileId, JsonElement element) {
        JsonObject object = GsonHelper.convertToJsonObject(element, "combo chain");
        ResourceLocation chainId = object.has("id")
                ? ResourceLocation.parse(GsonHelper.getAsString(object, "id"))
                : fileId;
        ComboChainDefinition.Builder builder = ComboChainDefinition.builder(
                chainId,
                ResourceLocation.parse(GsonHelper.getAsString(object, "trigger_ability")),
                ResourceLocation.parse(GsonHelper.getAsString(object, "combo_ability"))
        );
        if (object.has("window_ticks")) {
            builder.windowTicks(GsonHelper.getAsInt(object, "window_ticks"));
        }
        if (object.has("trigger")) {
            applyTriggerType(builder, GsonHelper.getAsString(object, "trigger"));
        }
        if (GsonHelper.getAsBoolean(object, "transform_triggered_slot", false)) {
            builder.transformTriggeredSlot();
        }
        if (object.has("target_slot")) {
            builder.targetSlot(GsonHelper.getAsInt(object, "target_slot"));
        }

        List<LoadedComboChainBranch> branches = readBranches(object);
        for (LoadedComboChainBranch branch : branches) {
            builder.branch(branch.comboAbilityId(), (player, data) -> branch.requirement().validate(player, data).isEmpty());
        }
        return new LoadedComboChainDefinition(chainId, builder.build(), List.copyOf(branches));
    }

    public record LoadedComboChainDefinition(
            ResourceLocation id,
            ComboChainDefinition definition,
            List<LoadedComboChainBranch> branches
    ) {}

    public record LoadedComboChainBranch(
            ResourceLocation comboAbilityId,
            AbilityRequirement requirement
    ) {}

    private static void applyTriggerType(ComboChainDefinition.Builder builder, String trigger) {
        String normalizedTrigger = trigger.toLowerCase(java.util.Locale.ROOT);
        switch (normalizedTrigger) {
            case "activation" -> builder.triggerOnActivation();
            case "hit", "hit_confirm" -> builder.triggerOnHit();
            case "end", "release" -> builder.triggerOnEnd();
            default -> throw new IllegalArgumentException("Unknown combo trigger type: " + trigger);
        }
    }

    private static List<LoadedComboChainBranch> readBranches(JsonObject object) {
        List<LoadedComboChainBranch> branches = new ArrayList<>();
        if (object.has("branch")) {
            branches.add(parseBranch(GsonHelper.getAsJsonObject(object, "branch")));
        }
        if (object.has("branches")) {
            JsonArray branchArray = GsonHelper.getAsJsonArray(object, "branches");
            for (JsonElement branchElement : branchArray) {
                branches.add(parseBranch(GsonHelper.convertToJsonObject(branchElement, "combo branch")));
            }
        }
        return List.copyOf(branches);
    }

    private static LoadedComboChainBranch parseBranch(JsonObject object) {
        ResourceLocation comboAbilityId = ResourceLocation.parse(GsonHelper.getAsString(object, "combo_ability"));
        JsonElement requirementElement = object.has("condition")
                ? object.get("condition")
                : object.has("when")
                ? object.get("when")
                : null;
        AbilityRequirement requirement = AbilityRequirementJsonParser.parse(
                requirementElement,
                id -> DataDrivenConditionApi.findCondition(id)
                        .orElseThrow(() -> new IllegalArgumentException("Unknown named condition reference: " + id))
        );
        return new LoadedComboChainBranch(comboAbilityId, requirement);
    }

    private static final class ReloadListener extends SimpleJsonResourceReloadListener {
        private ReloadListener() {
            super(GSON, DIRECTORY);
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> jsonById, ResourceManager resourceManager, ProfilerFiller profiler) {
            Map<ResourceLocation, LoadedComboChainDefinition> definitions = new LinkedHashMap<>();
            for (Map.Entry<ResourceLocation, JsonElement> entry : jsonById.entrySet()) {
                try {
                    LoadedComboChainDefinition definition = parseDefinition(entry.getKey(), entry.getValue());
                    LoadedComboChainDefinition previous = definitions.putIfAbsent(definition.id(), definition);
                    if (previous != null) {
                        throw new IllegalStateException("Duplicate datapack combo chain id: " + definition.id());
                    }
                } catch (RuntimeException exception) {
                    XLib.LOGGER.error("Failed to parse datapack combo chain {}", entry.getKey(), exception);
                }
            }
            loadedDefinitions = Map.copyOf(definitions);
        }
    }
}
