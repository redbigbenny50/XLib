package com.whatxe.xlib.command;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.serialization.JsonOps;
import com.whatxe.xlib.ability.AbilityApi;
import com.whatxe.xlib.ability.AbilityData;
import com.whatxe.xlib.ability.AbilityDefinition;
import com.whatxe.xlib.ability.AbilityGrantApi;
import com.whatxe.xlib.ability.AbilityLoadoutApi;
import com.whatxe.xlib.ability.AbilityResourceDefinition;
import com.whatxe.xlib.ability.ComboChainApi;
import com.whatxe.xlib.ability.GrantedItemApi;
import com.whatxe.xlib.ability.GrantedItemGrantApi;
import com.whatxe.xlib.ability.ModeApi;
import com.whatxe.xlib.ability.PassiveApi;
import com.whatxe.xlib.ability.PassiveGrantApi;
import com.whatxe.xlib.ability.RecipePermissionApi;
import com.whatxe.xlib.attachment.ModAttachments;
import com.whatxe.xlib.network.ModPayloads;
import com.whatxe.xlib.progression.UpgradeApi;
import com.whatxe.xlib.progression.UpgradeProgressData;
import com.whatxe.xlib.progression.UpgradeTrackDefinition;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.RecipeHolder;

final class XLibCommandSupport {
    private static final DynamicCommandExceptionType UNKNOWN_ABILITY =
            new DynamicCommandExceptionType(value -> Component.translatable("command.xlib.abilities.invalid", value));
    private static final DynamicCommandExceptionType UNKNOWN_PASSIVE =
            new DynamicCommandExceptionType(value -> Component.translatable("command.xlib.passives.invalid", value));
    private static final DynamicCommandExceptionType UNKNOWN_GRANTED_ITEM =
            new DynamicCommandExceptionType(value -> Component.translatable("command.xlib.items.invalid", value));
    private static final DynamicCommandExceptionType UNKNOWN_RECIPE =
            new DynamicCommandExceptionType(value -> Component.translatable("command.xlib.recipes.invalid", value));
    private static final DynamicCommandExceptionType UNKNOWN_UPGRADE_NODE =
            new DynamicCommandExceptionType(value -> Component.translatable("command.xlib.progression.node_invalid", value));
    private static final DynamicCommandExceptionType UNKNOWN_UPGRADE_TRACK =
            new DynamicCommandExceptionType(value -> Component.translatable("command.xlib.progression.track_invalid", value));

    private XLibCommandSupport() {}

    static CompletableFuture<Suggestions> suggestRecipeIds(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder
    ) {
        LinkedHashSet<ResourceLocation> recipeIds = new LinkedHashSet<>(RecipePermissionApi.restrictedRecipes());
        context.getSource().getServer().getRecipeManager().getRecipes().stream()
                .map(RecipeHolder::id)
                .forEach(recipeIds::add);
        return SharedSuggestionProvider.suggestResource(recipeIds, builder);
    }

    static void validateAbility(ResourceLocation abilityId) throws CommandSyntaxException {
        if (AbilityApi.findAbility(abilityId).isEmpty()) {
            throw UNKNOWN_ABILITY.create(abilityId.toString());
        }
    }

    static void validatePassive(ResourceLocation passiveId) throws CommandSyntaxException {
        if (PassiveApi.findPassive(passiveId).isEmpty()) {
            throw UNKNOWN_PASSIVE.create(passiveId.toString());
        }
    }

    static void validateGrantedItem(ResourceLocation grantedItemId) throws CommandSyntaxException {
        if (GrantedItemApi.findGrantedItem(grantedItemId).isEmpty()) {
            throw UNKNOWN_GRANTED_ITEM.create(grantedItemId.toString());
        }
    }

    static void validateRecipe(CommandSourceStack source, ResourceLocation recipeId) throws CommandSyntaxException {
        if (!RecipePermissionApi.isRestricted(recipeId)
                && source.getServer().getRecipeManager().byKey(recipeId).isEmpty()) {
            throw UNKNOWN_RECIPE.create(recipeId.toString());
        }
    }

    static void validateUpgradeNode(ResourceLocation nodeId) throws CommandSyntaxException {
        if (UpgradeApi.findNode(nodeId).isEmpty()) {
            throw UNKNOWN_UPGRADE_NODE.create(nodeId.toString());
        }
    }

    static void validateUpgradeTrack(ResourceLocation trackId) throws CommandSyntaxException {
        if (UpgradeApi.findTrack(trackId).isEmpty()) {
            throw UNKNOWN_UPGRADE_TRACK.create(trackId.toString());
        }
    }

    static String formatSlots(AbilityData data) {
        StringBuilder builder = new StringBuilder();
        for (int slot = 0; slot < AbilityData.SLOT_COUNT; slot++) {
            if (slot > 0) {
                builder.append(" | ");
            }
            builder.append(slot + 1).append(":").append(data.abilityInSlot(slot).map(ResourceLocation::toString).orElse("-"));
        }
        return builder.toString();
    }

    static String formatResolvedSlots(AbilityData data) {
        StringBuilder builder = new StringBuilder();
        for (int slot = 0; slot < AbilityData.SLOT_COUNT; slot++) {
            if (slot > 0) {
                builder.append(" | ");
            }
            builder.append(slot + 1).append(":")
                    .append(AbilityLoadoutApi.resolvedAbilityId(data, slot).map(ResourceLocation::toString).orElse("-"));
        }
        return builder.toString();
    }

    static String formatCooldowns(AbilityData data) {
        if (data.cooldowns().isEmpty()) {
            return "-";
        }
        return data.cooldowns().entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .map(entry -> entry.getKey() + "=" + String.format(Locale.ROOT, "%.1fs", entry.getValue() / 20.0F))
                .reduce((left, right) -> left + ", " + right)
                .orElse("-");
    }

    static String formatCharges(AbilityData data) {
        if (data.charges().isEmpty()) {
            return "-";
        }
        return data.charges().entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .map(entry -> entry.getKey() + "=" + entry.getValue() + " (recharge=" + data.chargeRechargeFor(entry.getKey()) + ")")
                .reduce((left, right) -> left + ", " + right)
                .orElse("-");
    }

    static String formatResources(AbilityData data) {
        if (data.resources().isEmpty()) {
            return "-";
        }
        return AbilityApi.allResources().stream()
                .sorted(Comparator.comparing(resource -> resource.id().toString()))
                .map(AbilityResourceDefinition::id)
                .map(resourceId -> resourceId + "=" + formatExactAmount(data.resourceAmountExact(resourceId))
                        + " (regen=" + data.resourceRegenDelay(resourceId)
                        + ", decay=" + data.resourceDecayDelay(resourceId) + ")")
                .reduce((left, right) -> left + ", " + right)
                .orElse("-");
    }

    private static String formatExactAmount(double amount) {
        int whole = (int) Math.round(amount);
        if (Math.abs(amount - whole) < 0.001D) {
            return Integer.toString(whole);
        }
        return String.format(java.util.Locale.ROOT, "%.2f", amount);
    }

    static String formatComboWindows(AbilityData data) {
        if (data.comboWindows().isEmpty()) {
            return "-";
        }
        return data.comboWindows().entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .reduce((left, right) -> left + ", " + right)
                .orElse("-");
    }

    static String formatComboOverrides(AbilityData data) {
        StringBuilder builder = new StringBuilder();
        boolean wrote = false;
        for (int slot = 0; slot < AbilityData.SLOT_COUNT; slot++) {
            Optional<ResourceLocation> abilityId = data.comboOverrideInSlot(slot);
            if (abilityId.isEmpty()) {
                continue;
            }
            if (wrote) {
                builder.append(", ");
            }
            builder.append(slot + 1).append(":").append(abilityId.get()).append(" (").append(data.comboOverrideDurationForSlot(slot)).append(")");
            wrote = true;
        }
        return wrote ? builder.toString() : "-";
    }

    static String formatModeOverlays(AbilityData data) {
        StringBuilder builder = new StringBuilder();
        boolean wrote = false;
        for (int slot = 0; slot < AbilityData.SLOT_COUNT; slot++) {
            Optional<ResourceLocation> overlayAbility = ModeApi.resolveOverlayAbility(data, slot);
            if (overlayAbility.isEmpty()) {
                continue;
            }
            if (wrote) {
                builder.append(", ");
            }
            builder.append(slot + 1).append(":").append(overlayAbility.get());
            wrote = true;
        }
        return wrote ? builder.toString() : "-";
    }

    static String formatModeLoadouts(AbilityData data) {
        if (data.modeLoadouts().isEmpty()) {
            return "-";
        }
        return data.modeLoadouts().entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .map(entry -> entry.getKey() + "=" + formatModeLoadoutSlots(data, entry.getKey()))
                .reduce((left, right) -> left + "; " + right)
                .orElse("-");
    }

    private static String formatModeLoadoutSlots(AbilityData data, ResourceLocation modeId) {
        StringBuilder builder = new StringBuilder();
        for (int slot = 0; slot < AbilityData.SLOT_COUNT; slot++) {
            if (slot > 0) {
                builder.append(" | ");
            }
            builder.append(slot + 1).append(":")
                    .append(data.modeAbilityInSlot(modeId, slot).map(ResourceLocation::toString).orElse("-"));
        }
        return builder.toString();
    }

    static String formatSourceMap(Map<ResourceLocation, Set<ResourceLocation>> sourceMap) {
        if (sourceMap.isEmpty()) {
            return "-";
        }
        return sourceMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .map(entry -> entry.getKey() + "=" + joinIds(entry.getValue()))
                .reduce((left, right) -> left + "; " + right)
                .orElse("-");
    }

    static String formatNumericMap(Map<ResourceLocation, Integer> values) {
        if (values.isEmpty()) {
            return "-";
        }
        return values.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .reduce((left, right) -> left + ", " + right)
                .orElse("-");
    }

    static String formatTrackIds(Collection<UpgradeTrackDefinition> tracks) {
        if (tracks.isEmpty()) {
            return "-";
        }
        return tracks.stream()
                .map(UpgradeTrackDefinition::id)
                .map(ResourceLocation::toString)
                .sorted()
                .reduce((left, right) -> left + ", " + right)
                .orElse("-");
    }

    static String joinIds(Collection<ResourceLocation> ids) {
        return ids.isEmpty()
                ? "-"
                : ids.stream().map(ResourceLocation::toString).sorted().reduce((left, right) -> left + ", " + right).orElse("-");
    }

    static String formatSourceGroups(AbilityData data) {
        Map<ResourceLocation, SourceGroup> groups = buildSourceGroups(data);
        if (groups.isEmpty()) {
            return "-";
        }
        return groups.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .map(entry -> entry.getKey() + "[" + joinIds(entry.getValue().abilities())
                        + "|" + joinIds(entry.getValue().passives())
                        + "|" + joinIds(entry.getValue().grantedItems())
                        + "|" + joinIds(entry.getValue().recipePermissions())
                        + "|" + joinIds(entry.getValue().blockedAbilities()) + "]")
                .reduce((left, right) -> left + "; " + right)
                .orElse("-");
    }

    static JsonObject buildDebugJson(ServerPlayer target) {
        AbilityData data = ModAttachments.get(target);
        UpgradeProgressData progressionData = UpgradeApi.get(target);
        XLibDebugCounters counters = XLibDebugCounters.collect(target);
        JsonObject root = new JsonObject();
        root.addProperty("player_name", target.getGameProfile().getName());
        root.addProperty("player_uuid", target.getStringUUID());
        root.addProperty("play_protocol_version", ModPayloads.PLAY_PROTOCOL_VERSION);
        root.addProperty("play_protocol_series", ModPayloads.PLAY_PROTOCOL.series());
        root.addProperty("play_protocol_revision", ModPayloads.PLAY_PROTOCOL.revision());
        root.addProperty("ability_access_restricted", data.abilityAccessRestricted());
        root.add("managed_sources", idsToJson(data.managedGrantSources()));
        root.add("debug_counters", counters.toJson());
        root.add("granted_abilities", idsToJson(AbilityGrantApi.grantedAbilities(target)));
        root.add("blocked_abilities", idsToJson(AbilityGrantApi.blockedAbilities(target)));
        root.add("passives", idsToJson(PassiveGrantApi.grantedPassives(target)));
        root.add("granted_items", idsToJson(GrantedItemGrantApi.grantedItems(target)));
        root.add("recipe_permissions", idsToJson(RecipePermissionApi.permissions(target)));
        root.add("locked_recipes", idsToJson(RecipePermissionApi.lockedRecipes(target)));
        root.add("point_balances", intMapToJson(progressionData.pointBalances()));
        root.add("counters", intMapToJson(progressionData.counters()));
        root.add("unlocked_nodes", idsToJson(progressionData.unlockedNodes()));
        root.add("visible_tracks", trackIdsToJson(UpgradeApi.visibleTracks(progressionData)));
        root.addProperty("resolved_slots", formatResolvedSlots(data));
        root.addProperty("mode_loadouts", formatModeLoadouts(data));
        root.addProperty("mode_overlays", formatModeOverlays(data));
        root.addProperty("combo_windows_summary", formatComboWindows(data));
        root.addProperty("combo_overrides_summary", formatComboOverrides(data));
        root.add("ability_sources", sourceMapToJson(data.abilityGrantSources()));
        root.add("activation_block_sources", sourceMapToJson(data.abilityActivationBlockSources()));
        root.add("passive_sources", sourceMapToJson(data.passiveGrantSources()));
        root.add("granted_item_sources", sourceMapToJson(data.grantedItemSources()));
        root.add("recipe_permission_sources", sourceMapToJson(data.recipePermissionSources()));
        root.add("source_groups", sourceGroupsToJson(buildSourceGroups(data)));
        JsonElement rawData = AbilityData.CODEC.encodeStart(JsonOps.INSTANCE, data).getOrThrow(IllegalStateException::new);
        root.add("ability_data", rawData);
        JsonElement rawProgressionData = UpgradeProgressData.CODEC.encodeStart(JsonOps.INSTANCE, progressionData)
                .getOrThrow(IllegalStateException::new);
        root.add("progression_data", rawProgressionData);
        return root;
    }

    static Map<ResourceLocation, SourceGroup> buildSourceGroups(AbilityData data) {
        Map<ResourceLocation, SourceGroupBuilder> builders = new LinkedHashMap<>();
        mergeSourceGroups(builders, data.abilityGrantSources(), SourceGroupField.ABILITY);
        mergeSourceGroups(builders, data.abilityActivationBlockSources(), SourceGroupField.BLOCKED_ABILITY);
        mergeSourceGroups(builders, data.passiveGrantSources(), SourceGroupField.PASSIVE);
        mergeSourceGroups(builders, data.grantedItemSources(), SourceGroupField.GRANTED_ITEM);
        mergeSourceGroups(builders, data.recipePermissionSources(), SourceGroupField.RECIPE_PERMISSION);
        Map<ResourceLocation, SourceGroup> groups = new LinkedHashMap<>();
        builders.forEach((sourceId, builder) -> groups.put(sourceId, builder.build()));
        return groups;
    }

    private static void mergeSourceGroups(
            Map<ResourceLocation, SourceGroupBuilder> builders,
            Map<ResourceLocation, Set<ResourceLocation>> sourceMap,
            SourceGroupField field
    ) {
        for (Map.Entry<ResourceLocation, Set<ResourceLocation>> entry : sourceMap.entrySet()) {
            for (ResourceLocation sourceId : entry.getValue()) {
                builders.computeIfAbsent(sourceId, ignored -> new SourceGroupBuilder()).add(field, entry.getKey());
            }
        }
    }

    static void sendArrayDiff(
            CommandSourceStack source,
            String fieldName,
            JsonObject previousSnapshot,
            JsonObject currentSnapshot
    ) {
        Set<String> previousValues = jsonArrayToSet(previousSnapshot.getAsJsonArray(fieldName));
        Set<String> currentValues = jsonArrayToSet(currentSnapshot.getAsJsonArray(fieldName));
        Set<String> added = new LinkedHashSet<>(currentValues);
        added.removeAll(previousValues);
        Set<String> removed = new LinkedHashSet<>(previousValues);
        removed.removeAll(currentValues);
        source.sendSuccess(() -> Component.literal(fieldName + " | +" + joinStrings(added) + " | -" + joinStrings(removed)), false);
    }

    static void sendNumericMapDiff(
            CommandSourceStack source,
            String fieldName,
            JsonObject previousSnapshot,
            JsonObject currentSnapshot
    ) {
        Map<String, Integer> previousValues = jsonObjectToIntMap(previousSnapshot.getAsJsonObject(fieldName));
        Map<String, Integer> currentValues = jsonObjectToIntMap(currentSnapshot.getAsJsonObject(fieldName));
        Set<String> keys = new LinkedHashSet<>(previousValues.keySet());
        keys.addAll(currentValues.keySet());
        Collection<String> added = new ArrayList<>();
        Collection<String> changed = new ArrayList<>();
        Collection<String> removed = new ArrayList<>();
        keys.stream().sorted().forEach(key -> {
            Integer previousValue = previousValues.get(key);
            Integer currentValue = currentValues.get(key);
            if (previousValue == null && currentValue != null) {
                added.add(key + "=" + currentValue);
            } else if (previousValue != null && currentValue == null) {
                removed.add(key + "=" + previousValue);
            } else if (previousValue != null && currentValue != null && !previousValue.equals(currentValue)) {
                changed.add(key + ":" + previousValue + "->" + currentValue);
            }
        });
        source.sendSuccess(
                () -> Component.literal(fieldName + " | +" + joinStrings(added) + " | ~" + joinStrings(changed) + " | -" + joinStrings(removed)),
                false
        );
    }

    private static JsonArray idsToJson(Collection<ResourceLocation> ids) {
        JsonArray array = new JsonArray();
        ids.stream().map(ResourceLocation::toString).sorted().forEach(array::add);
        return array;
    }

    private static JsonObject sourceMapToJson(Map<ResourceLocation, Set<ResourceLocation>> sourceMap) {
        JsonObject object = new JsonObject();
        sourceMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> object.add(entry.getKey().toString(), idsToJson(entry.getValue())));
        return object;
    }

    private static JsonObject intMapToJson(Map<ResourceLocation, Integer> values) {
        JsonObject object = new JsonObject();
        values.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> object.addProperty(entry.getKey().toString(), entry.getValue()));
        return object;
    }

    private static JsonArray trackIdsToJson(Collection<UpgradeTrackDefinition> tracks) {
        JsonArray array = new JsonArray();
        tracks.stream()
                .map(UpgradeTrackDefinition::id)
                .map(ResourceLocation::toString)
                .sorted()
                .forEach(array::add);
        return array;
    }

    private static JsonObject sourceGroupsToJson(Map<ResourceLocation, SourceGroup> sourceGroups) {
        JsonObject object = new JsonObject();
        sourceGroups.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> {
                    JsonObject group = new JsonObject();
                    group.add("abilities", idsToJson(entry.getValue().abilities()));
                    group.add("blocked_abilities", idsToJson(entry.getValue().blockedAbilities()));
                    group.add("passives", idsToJson(entry.getValue().passives()));
                    group.add("granted_items", idsToJson(entry.getValue().grantedItems()));
                    group.add("recipe_permissions", idsToJson(entry.getValue().recipePermissions()));
                    object.add(entry.getKey().toString(), group);
                });
        return object;
    }

    private static Set<String> jsonArrayToSet(JsonArray array) {
        Set<String> values = new LinkedHashSet<>();
        if (array == null) {
            return values;
        }
        array.forEach(element -> values.add(element.getAsString()));
        return values;
    }

    private static Map<String, Integer> jsonObjectToIntMap(JsonObject object) {
        Map<String, Integer> values = new LinkedHashMap<>();
        if (object == null) {
            return values;
        }
        object.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> values.put(entry.getKey(), entry.getValue().getAsInt()));
        return values;
    }

    private static String joinStrings(Collection<String> values) {
        return values.isEmpty() ? "-" : values.stream().sorted().reduce((left, right) -> left + ", " + right).orElse("-");
    }

    enum SourceGroupField {
        ABILITY,
        BLOCKED_ABILITY,
        PASSIVE,
        GRANTED_ITEM,
        RECIPE_PERMISSION
    }

    record SourceGroup(
            Set<ResourceLocation> abilities,
            Set<ResourceLocation> blockedAbilities,
            Set<ResourceLocation> passives,
            Set<ResourceLocation> grantedItems,
            Set<ResourceLocation> recipePermissions
    ) {}

    private static final class SourceGroupBuilder {
        private final Set<ResourceLocation> abilities = new LinkedHashSet<>();
        private final Set<ResourceLocation> blockedAbilities = new LinkedHashSet<>();
        private final Set<ResourceLocation> passives = new LinkedHashSet<>();
        private final Set<ResourceLocation> grantedItems = new LinkedHashSet<>();
        private final Set<ResourceLocation> recipePermissions = new LinkedHashSet<>();

        private void add(SourceGroupField field, ResourceLocation value) {
            switch (field) {
                case ABILITY -> this.abilities.add(value);
                case BLOCKED_ABILITY -> this.blockedAbilities.add(value);
                case PASSIVE -> this.passives.add(value);
                case GRANTED_ITEM -> this.grantedItems.add(value);
                case RECIPE_PERMISSION -> this.recipePermissions.add(value);
            }
        }

        private SourceGroup build() {
            return new SourceGroup(
                    Set.copyOf(this.abilities),
                    Set.copyOf(this.blockedAbilities),
                    Set.copyOf(this.passives),
                    Set.copyOf(this.grantedItems),
                    Set.copyOf(this.recipePermissions)
            );
        }
    }
}
