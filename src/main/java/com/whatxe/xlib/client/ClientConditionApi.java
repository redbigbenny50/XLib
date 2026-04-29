package com.whatxe.xlib.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.whatxe.xlib.XLib;
import com.whatxe.xlib.ability.AbilityRequirement;
import com.whatxe.xlib.ability.AbilityRequirementJsonParser;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

public final class ClientConditionApi {
    private static final Gson GSON = new GsonBuilder().create();
    private static final String DIRECTORY = XLib.MODID + "/conditions";
    private static volatile Map<ResourceLocation, AbilityRequirement> loadedConditions = Map.of();

    private ClientConditionApi() {}

    public static void bootstrap() {}

    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new ReloadListener());
    }

    public static Collection<ResourceLocation> allConditionIds() {
        return List.copyOf(loadedConditions.keySet());
    }

    public static Optional<AbilityRequirement> findCondition(ResourceLocation conditionId) {
        return Optional.ofNullable(loadedConditions.get(conditionId));
    }

    public static AbilityRequirement requireCondition(ResourceLocation conditionId) {
        return findCondition(conditionId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown named client condition: " + conditionId));
    }

    static void setLoadedConditionsForTesting(Map<ResourceLocation, AbilityRequirement> conditions) {
        loadedConditions = Map.copyOf(conditions);
    }

    private static final class ReloadListener extends SimpleJsonResourceReloadListener {
        private ReloadListener() {
            super(GSON, DIRECTORY);
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> jsonById, ResourceManager resourceManager, ProfilerFiller profiler) {
            try {
                loadedConditions = AbilityRequirementJsonParser.compileDefinitions(jsonById);
            } catch (RuntimeException exception) {
                XLib.LOGGER.error("Failed to compile named client conditions from {}", DIRECTORY, exception);
                loadedConditions = Map.of();
            }
        }
    }
}
