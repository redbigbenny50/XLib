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

public final class DataDrivenPassiveApi {
    private static final Gson GSON = new GsonBuilder().create();
    private static final String DIRECTORY = XLib.MODID + "/passives";
    private static volatile Map<ResourceLocation, LoadedPassiveDefinition> loadedDefinitions = Map.of();
    private static volatile Map<ResourceLocation, LoadedPassiveDefinition> syncedDefinitions = Map.of();
    private static volatile Map<ResourceLocation, String> definitionJsonByFileId = Map.of();

    private DataDrivenPassiveApi() {}

    public static void bootstrap() {}

    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new ReloadListener());
    }

    public static Collection<ResourceLocation> allDefinitionIds() {
        return List.copyOf(resolvedDefinitions().keySet());
    }

    public static Optional<LoadedPassiveDefinition> findDefinition(ResourceLocation definitionId) {
        return Optional.ofNullable(resolvedDefinitions().get(definitionId));
    }

    public static Map<ResourceLocation, String> definitionJsonsForSync() {
        return Map.copyOf(definitionJsonByFileId);
    }

    public static void syncDefinitionsFromJson(Map<ResourceLocation, String> jsonByFileId) {
        syncedDefinitions = parseDefinitionsFromJson(jsonByFileId, "client-synced passive");
    }

    public static void clearSyncedDefinitions() {
        syncedDefinitions = Map.of();
    }

    static void setDefinitionsForTesting(Map<ResourceLocation, LoadedPassiveDefinition> definitions) {
        loadedDefinitions = Map.copyOf(definitions);
    }

    static LoadedPassiveDefinition parseDefinition(ResourceLocation fileId, JsonElement element) {
        JsonObject object = GsonHelper.convertToJsonObject(element, "passive");
        ResourceLocation passiveId = object.has("id")
                ? ResourceLocation.parse(GsonHelper.getAsString(object, "id"))
                : fileId;

        PassiveDefinition.Builder builder = PassiveDefinition.builder(passiveId, DataDrivenDefinitionReaders.readRequiredIcon(object))
                .displayName(DataDrivenDefinitionReaders.readComponent(object, "display_name", Component.literal(passiveId.toString())));
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

        for (AbilityRequirement requirement : DataDrivenRuntimeEffects.readRequirements(object, "grant_requirement", "grant_requirements")) {
            builder.grantRequirement(requirement);
        }
        for (AbilityRequirement requirement : DataDrivenRuntimeEffects.readRequirements(object, "active_requirement", "active_requirements")) {
            builder.activeRequirement(requirement);
        }
        if (object.has("cooldown_tick_rate_multiplier")) {
            builder.cooldownTickRateMultiplier(GsonHelper.getAsDouble(object, "cooldown_tick_rate_multiplier"));
        }
        for (Map.Entry<PassiveSoundTrigger, List<AbilitySound>> entry : DataDrivenRuntimeEffects.readPassiveSounds(object).entrySet()) {
            for (AbilitySound sound : entry.getValue()) {
                builder.sound(entry.getKey(), sound);
            }
        }

        List<DataDrivenRuntimeEffects.Effect> tickEffects = DataDrivenRuntimeEffects.readEffects(object, "tick_effects");
        if (!tickEffects.isEmpty()) {
            builder.ticker(DataDrivenRuntimeEffects.passiveTickerFromEffects(passiveId, tickEffects));
        }
        List<DataDrivenRuntimeEffects.Effect> grantedEffects = DataDrivenRuntimeEffects.readEffects(object, "on_granted_effects");
        if (!grantedEffects.isEmpty()) {
            builder.onGranted(DataDrivenRuntimeEffects.passiveActionFromEffects(passiveId, grantedEffects));
        }
        List<DataDrivenRuntimeEffects.Effect> revokedEffects = DataDrivenRuntimeEffects.readEffects(object, "on_revoked_effects");
        if (!revokedEffects.isEmpty()) {
            builder.onRevoked(DataDrivenRuntimeEffects.passiveActionFromEffects(passiveId, revokedEffects));
        }
        List<DataDrivenRuntimeEffects.Effect> hitEffects = DataDrivenRuntimeEffects.readEffects(object, "on_hit_effects");
        if (!hitEffects.isEmpty()) {
            builder.onHit(DataDrivenRuntimeEffects.passiveHitActionFromEffects(passiveId, hitEffects));
        }
        List<DataDrivenRuntimeEffects.Effect> killEffects = DataDrivenRuntimeEffects.readEffects(object, "on_kill_effects");
        if (!killEffects.isEmpty()) {
            builder.onKill(DataDrivenRuntimeEffects.passiveHitActionFromEffects(passiveId, killEffects));
        }
        List<DataDrivenRuntimeEffects.Effect> hurtEffects = DataDrivenRuntimeEffects.readEffects(object, "on_hurt_effects");
        if (!hurtEffects.isEmpty()) {
            builder.onHurt(DataDrivenRuntimeEffects.passiveHurtActionFromEffects(passiveId, hurtEffects));
        }
        List<DataDrivenRuntimeEffects.Effect> jumpEffects = DataDrivenRuntimeEffects.readEffects(object, "on_jump_effects");
        if (!jumpEffects.isEmpty()) {
            builder.onJump(DataDrivenRuntimeEffects.passiveActionFromEffects(passiveId, jumpEffects));
        }
        List<DataDrivenRuntimeEffects.Effect> eatEffects = DataDrivenRuntimeEffects.readEffects(object, "on_eat_effects");
        if (!eatEffects.isEmpty()) {
            builder.onEat(DataDrivenRuntimeEffects.passiveEatActionFromEffects(passiveId, eatEffects));
        }
        List<DataDrivenRuntimeEffects.Effect> blockBreakEffects = DataDrivenRuntimeEffects.readEffects(object, "on_block_break_effects");
        if (!blockBreakEffects.isEmpty()) {
            builder.onBlockBreak(DataDrivenRuntimeEffects.passiveBlockBreakActionFromEffects(passiveId, blockBreakEffects));
        }
        List<DataDrivenRuntimeEffects.Effect> armorChangeEffects = DataDrivenRuntimeEffects.readEffects(object, "on_armor_change_effects");
        if (!armorChangeEffects.isEmpty()) {
            builder.onArmorChange(DataDrivenRuntimeEffects.passiveArmorChangeActionFromEffects(passiveId, armorChangeEffects));
        }

        return new LoadedPassiveDefinition(passiveId, builder.build());
    }

    public record LoadedPassiveDefinition(
            ResourceLocation id,
            PassiveDefinition definition
    ) {}

    private static Map<ResourceLocation, LoadedPassiveDefinition> resolvedDefinitions() {
        Map<ResourceLocation, LoadedPassiveDefinition> definitions = new LinkedHashMap<>(loadedDefinitions);
        syncedDefinitions.forEach(definitions::putIfAbsent);
        return Map.copyOf(definitions);
    }

    private static Map<ResourceLocation, LoadedPassiveDefinition> parseDefinitionsFromJson(
            Map<ResourceLocation, String> jsonByFileId,
            String label
    ) {
        Map<ResourceLocation, LoadedPassiveDefinition> definitions = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, String> entry : jsonByFileId.entrySet()) {
            try {
                LoadedPassiveDefinition definition = parseDefinition(entry.getKey(), JsonParser.parseString(entry.getValue()));
                LoadedPassiveDefinition previous = definitions.putIfAbsent(definition.id(), definition);
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
            Map<ResourceLocation, LoadedPassiveDefinition> definitions = new LinkedHashMap<>();
            Map<ResourceLocation, String> definitionJsons = new LinkedHashMap<>();
            for (Map.Entry<ResourceLocation, JsonElement> entry : jsonById.entrySet()) {
                try {
                    LoadedPassiveDefinition definition = parseDefinition(entry.getKey(), entry.getValue());
                    LoadedPassiveDefinition previous = definitions.putIfAbsent(definition.id(), definition);
                    if (previous != null) {
                        throw new IllegalStateException("Duplicate datapack passive id: " + definition.id());
                    }
                    definitionJsons.put(entry.getKey(), GSON.toJson(entry.getValue()));
                } catch (RuntimeException exception) {
                    XLib.LOGGER.error("Failed to parse datapack passive {}", entry.getKey(), exception);
                }
            }
            loadedDefinitions = Map.copyOf(definitions);
            definitionJsonByFileId = Map.copyOf(definitionJsons);
        }
    }
}
