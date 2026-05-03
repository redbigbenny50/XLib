package com.whatxe.xlib.value;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.whatxe.xlib.XLib;
import com.whatxe.xlib.ability.AbilityData;
import com.whatxe.xlib.ability.AbilityApi;
import com.whatxe.xlib.ability.AbilityGrantApi;
import com.whatxe.xlib.ability.AbilityRequirement;
import com.whatxe.xlib.ability.AbilityRequirementJsonParser;
import com.whatxe.xlib.ability.DataDrivenConditionApi;
import com.whatxe.xlib.ability.DataDrivenDefinitionReaders;
import com.whatxe.xlib.attachment.ModAttachments;
import com.whatxe.xlib.capability.CapabilityPolicyApi;
import com.whatxe.xlib.capability.CapabilityPolicyData;
import com.whatxe.xlib.combat.DamageModifierProfileApi;
import com.whatxe.xlib.combat.DamageModifierProfileData;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.jetbrains.annotations.Nullable;

public final class DataDrivenTrackedValueRuleApi {
    private static final Gson GSON = new GsonBuilder().create();
    private static final String DIRECTORY = XLib.MODID + "/tracked_value_rules";
    private static volatile Map<ResourceLocation, LoadedTrackedValueRuleDefinition> loadedDefinitions = Map.of();
    private static volatile Map<TrackedValueRuleTrigger, List<TrackedValueRuleDefinition>> rulesByTrigger = Map.of();

    private DataDrivenTrackedValueRuleApi() {}

    public static void bootstrap() {}

    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new ReloadListener());
    }

    public static Collection<ResourceLocation> allDefinitionIds() {
        return List.copyOf(loadedDefinitions.keySet());
    }

    public static Optional<LoadedTrackedValueRuleDefinition> findDefinition(ResourceLocation definitionId) {
        return Optional.ofNullable(loadedDefinitions.get(definitionId));
    }

    static void setDefinitionsForTesting(Map<ResourceLocation, LoadedTrackedValueRuleDefinition> definitions) {
        loadedDefinitions = Map.copyOf(definitions);
        rulesByTrigger = indexByTrigger(definitions.values());
    }

    public static void dispatchTick(ServerPlayer player) {
        dispatch(player, TrackedValueRuleTrigger.TICK, null, null, null, null, null, null, null);
    }

    public static void dispatchDamageTaken(ServerPlayer player, @Nullable LivingEntity attacker, DamageSource damageSource) {
        dispatch(player, TrackedValueRuleTrigger.DAMAGE_TAKEN, attacker, null, null, null, null, null, damageSource);
    }

    public static void dispatchDamageDealt(ServerPlayer player, LivingEntity target, DamageSource damageSource) {
        dispatch(player, TrackedValueRuleTrigger.DAMAGE_DEALT, target, null, null, null, null, null, damageSource);
    }

    public static void dispatchKill(ServerPlayer player, LivingEntity target, DamageSource damageSource) {
        dispatch(player, TrackedValueRuleTrigger.KILL, target, null, null, null, null, null, damageSource);
    }

    public static void dispatchJump(ServerPlayer player) {
        dispatch(player, TrackedValueRuleTrigger.JUMP, null, null, null, null, null, null, null);
    }

    public static void dispatchArmorChanged(ServerPlayer player, EquipmentSlot slot, ItemStack from, ItemStack to) {
        dispatch(player, TrackedValueRuleTrigger.ARMOR_CHANGED, null, from, to, null, null, slot, null);
    }

    public static void dispatchItemUsed(ServerPlayer player, ItemStack stack) {
        dispatch(player, TrackedValueRuleTrigger.ITEM_USED, null, stack, null, null, null, null, null);
    }

    public static void dispatchItemConsumed(ServerPlayer player, ItemStack stack) {
        dispatch(player, TrackedValueRuleTrigger.ITEM_CONSUMED, null, stack, null, null, null, null, null);
    }

    public static void dispatchBlockBroken(ServerPlayer player, BlockState blockState) {
        dispatch(player, TrackedValueRuleTrigger.BLOCK_BROKEN, null, null, null, blockState, null, null, null);
    }

    public static void dispatchAdvancementEarned(ServerPlayer player, ResourceLocation advancementId) {
        dispatch(player, TrackedValueRuleTrigger.ADVANCEMENT_EARNED, null, null, null, null, advancementId, null, null);
    }

    public static void dispatchAdvancementProgress(ServerPlayer player, ResourceLocation advancementId) {
        dispatch(player, TrackedValueRuleTrigger.ADVANCEMENT_PROGRESS, null, null, null, null, advancementId, null, null);
    }

    static LoadedTrackedValueRuleDefinition parseDefinition(ResourceLocation fileId, JsonElement element) {
        JsonObject object = GsonHelper.convertToJsonObject(element, "tracked value rule");
        ResourceLocation ruleId = object.has("id")
                ? ResourceLocation.parse(GsonHelper.getAsString(object, "id"))
                : fileId;
        TrackedValueRuleTrigger trigger = TrackedValueRuleTrigger.parse(GsonHelper.getAsString(object, "trigger"));
        TrackedValueRuleDefinition.Builder builder = TrackedValueRuleDefinition.builder(ruleId, trigger)
                .priority(object.has("priority") ? GsonHelper.getAsInt(object, "priority") : 0);
        if (object.has("food_replacement_source")) {
            builder.foodReplacementSource(ResourceLocation.parse(GsonHelper.getAsString(object, "food_replacement_source")));
        }
        if (object.has("classification_source")) {
            builder.classificationSource(ResourceLocation.parse(GsonHelper.getAsString(object, "classification_source")));
        }
        for (AbilityRequirement requirement : readRequirements(object)) {
            builder.requirement(requirement);
        }
        for (ResourceLocation entityId : DataDrivenDefinitionReaders.readLocations(object, "target_entity", "target_entities")) {
            builder.targetEntity(entityId);
        }
        for (ResourceLocation tagId : DataDrivenDefinitionReaders.readLocations(object, "target_entity_tag", "target_entity_tags")) {
            builder.targetEntityTag(tagId);
        }
        for (ResourceLocation itemId : DataDrivenDefinitionReaders.readLocations(object, "item", "items")) {
            builder.item(itemId);
        }
        for (ResourceLocation tagId : DataDrivenDefinitionReaders.readLocations(object, "item_tag", "item_tags")) {
            builder.itemTag(tagId);
        }
        for (ResourceLocation blockId : DataDrivenDefinitionReaders.readLocations(object, "block", "blocks")) {
            builder.block(blockId);
        }
        for (ResourceLocation tagId : DataDrivenDefinitionReaders.readLocations(object, "block_tag", "block_tags")) {
            builder.blockTag(tagId);
        }
        for (ResourceLocation advancementId : DataDrivenDefinitionReaders.readLocations(object, "advancement", "advancements")) {
            builder.advancement(advancementId);
        }
        for (String slotName : readStrings(object, "armor_slot", "armor_slots")) {
            builder.armorSlot(TrackedValueRuleDefinition.parseArmorSlot(slotName));
        }
        for (ResourceLocation damageTypeId : DataDrivenDefinitionReaders.readLocations(object, "damage_type", "damage_types")) {
            builder.damageType(damageTypeId);
        }
        for (ResourceLocation tagId : DataDrivenDefinitionReaders.readLocations(object, "damage_type_tag", "damage_type_tags")) {
            builder.damageTypeTag(tagId);
        }
        for (ResourceLocation valueId : DataDrivenDefinitionReaders.readLocations(object, "clear_value", "clear_values")) {
            builder.clearValue(valueId);
        }
        readDoubleMap(object, "value_deltas").forEach(builder::addValue);
        readDoubleMap(object, "set_values").forEach(builder::setValue);
        readDoubleMap(object, "multiply_values").forEach(builder::multiplyValue);
        readDoubleMap(object, "min_values").forEach(builder::minValue);
        readDoubleMap(object, "max_values").forEach(builder::maxValue);
        for (ResourceLocation valueId : DataDrivenDefinitionReaders.readLocations(object, "enable_food_replacement", "enable_food_replacements")) {
            builder.enableFoodReplacement(valueId);
        }
        for (ResourceLocation valueId : DataDrivenDefinitionReaders.readLocations(object, "disable_food_replacement", "disable_food_replacements")) {
            builder.disableFoodReplacement(valueId);
        }
        for (ResourceLocation policyId : DataDrivenDefinitionReaders.readLocations(object, "grant_state_policy", "grant_state_policies")) {
            builder.grantStatePolicy(policyId);
        }
        for (ResourceLocation policyId : DataDrivenDefinitionReaders.readLocations(object, "revoke_state_policy", "revoke_state_policies")) {
            builder.revokeStatePolicy(policyId);
        }
        for (ResourceLocation flagId : DataDrivenDefinitionReaders.readLocations(object, "grant_state_flag", "grant_state_flags")) {
            builder.grantStateFlag(flagId);
        }
        for (ResourceLocation flagId : DataDrivenDefinitionReaders.readLocations(object, "revoke_state_flag", "revoke_state_flags")) {
            builder.revokeStateFlag(flagId);
        }
        for (ResourceLocation policyId : DataDrivenDefinitionReaders.readLocations(object, "grant_capability_policy", "grant_capability_policies")) {
            builder.grantCapabilityPolicy(policyId);
        }
        for (ResourceLocation policyId : DataDrivenDefinitionReaders.readLocations(object, "revoke_capability_policy", "revoke_capability_policies")) {
            builder.revokeCapabilityPolicy(policyId);
        }
        for (ResourceLocation profileId : DataDrivenDefinitionReaders.readLocations(object, "grant_damage_profile", "grant_damage_profiles")) {
            builder.grantDamageModifierProfile(profileId);
        }
        for (ResourceLocation profileId : DataDrivenDefinitionReaders.readLocations(object, "revoke_damage_profile", "revoke_damage_profiles")) {
            builder.revokeDamageModifierProfile(profileId);
        }
        builder.clearClassificationSource(object.has("clear_classification_source")
                && GsonHelper.getAsBoolean(object, "clear_classification_source"));
        for (ResourceLocation entityTypeId : DataDrivenDefinitionReaders.readLocations(object, "grant_synthetic_entity_type", "grant_synthetic_entity_types")) {
            builder.grantSyntheticEntityType(entityTypeId);
        }
        for (ResourceLocation entityTypeId : DataDrivenDefinitionReaders.readLocations(object, "revoke_synthetic_entity_type", "revoke_synthetic_entity_types")) {
            builder.revokeSyntheticEntityType(entityTypeId);
        }
        for (ResourceLocation tagId : DataDrivenDefinitionReaders.readLocations(object, "grant_synthetic_tag", "grant_synthetic_tags")) {
            builder.grantSyntheticTag(tagId);
        }
        for (ResourceLocation tagId : DataDrivenDefinitionReaders.readLocations(object, "revoke_synthetic_tag", "revoke_synthetic_tags")) {
            builder.revokeSyntheticTag(tagId);
        }
        return new LoadedTrackedValueRuleDefinition(ruleId, builder.build());
    }

    public record LoadedTrackedValueRuleDefinition(
            ResourceLocation id,
            TrackedValueRuleDefinition definition
    ) {}

    private static void dispatch(
            ServerPlayer player,
            TrackedValueRuleTrigger trigger,
            @Nullable LivingEntity targetEntity,
            @Nullable ItemStack primaryStack,
            @Nullable ItemStack secondaryStack,
            @Nullable BlockState blockState,
            @Nullable ResourceLocation advancementId,
            @Nullable EquipmentSlot armorSlot,
            @Nullable DamageSource damageSource
    ) {
        List<TrackedValueRuleDefinition> rules = rulesByTrigger.get(trigger);
        if (rules == null || rules.isEmpty()) {
            return;
        }

        AbilityData abilityData = ModAttachments.get(player);
        AbilityData updatedAbilityData = abilityData;
        CapabilityPolicyData currentCapabilityData = CapabilityPolicyApi.getData(player);
        CapabilityPolicyData updatedCapabilityData = currentCapabilityData;
        DamageModifierProfileData currentDamageProfileData = DamageModifierProfileApi.getData(player);
        DamageModifierProfileData updatedDamageProfileData = currentDamageProfileData;
        TrackedValueData currentData = ModAttachments.getTrackedValues(player);
        TrackedValueData updatedData = currentData;
        for (TrackedValueRuleDefinition rule : rules) {
            if (rule.matches(player, updatedAbilityData, targetEntity, primaryStack, secondaryStack, blockState, advancementId, armorSlot, damageSource)) {
                updatedData = rule.applyTo(updatedData);
                updatedAbilityData = rule.applyAbilityEffects(updatedAbilityData);
                updatedCapabilityData = rule.applyCapabilityEffects(updatedCapabilityData);
                updatedDamageProfileData = rule.applyDamageModifierEffects(updatedDamageProfileData);
                rule.applyEntityEffects(player);
            }
        }
        if (!updatedData.equals(currentData)) {
            ModAttachments.setTrackedValues(player, updatedData);
        }
        if (!updatedAbilityData.equals(abilityData)) {
            ModAttachments.set(player, AbilityGrantApi.sanitize(AbilityApi.sanitizeData(updatedAbilityData)));
        }
        if (!updatedCapabilityData.equals(currentCapabilityData)) {
            CapabilityPolicyApi.setData(player, updatedCapabilityData);
        }
        if (!updatedDamageProfileData.equals(currentDamageProfileData)) {
            DamageModifierProfileApi.setData(player, updatedDamageProfileData);
        }
    }

    private static List<AbilityRequirement> readRequirements(JsonObject object) {
        List<AbilityRequirement> requirements = new ArrayList<>();
        if (object.has("condition")) {
            requirements.add(AbilityRequirementJsonParser.parse(object.get("condition"), DataDrivenConditionApi::requireCondition));
        }
        if (object.has("conditions")) {
            for (JsonElement element : GsonHelper.getAsJsonArray(object, "conditions")) {
                requirements.add(AbilityRequirementJsonParser.parse(element, DataDrivenConditionApi::requireCondition));
            }
        }
        return List.copyOf(requirements);
    }

    private static Map<ResourceLocation, Double> readDoubleMap(JsonObject object, String key) {
        if (!object.has(key)) {
            return Map.of();
        }
        JsonObject mapObject = GsonHelper.getAsJsonObject(object, key);
        Map<ResourceLocation, Double> values = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : mapObject.entrySet()) {
            values.put(ResourceLocation.parse(entry.getKey()), entry.getValue().getAsDouble());
        }
        return Map.copyOf(values);
    }

    private static List<String> readStrings(JsonObject object, String singleKey, String pluralKey) {
        List<String> values = new ArrayList<>();
        if (singleKey != null && object.has(singleKey)) {
            values.add(GsonHelper.getAsString(object, singleKey));
        }
        if (pluralKey != null && object.has(pluralKey)) {
            for (JsonElement element : GsonHelper.getAsJsonArray(object, pluralKey)) {
                values.add(GsonHelper.convertToString(element, pluralKey));
            }
        }
        return List.copyOf(values);
    }

    private static Map<TrackedValueRuleTrigger, List<TrackedValueRuleDefinition>> indexByTrigger(
            Collection<LoadedTrackedValueRuleDefinition> definitions
    ) {
        Map<TrackedValueRuleTrigger, List<TrackedValueRuleDefinition>> indexed = new EnumMap<>(TrackedValueRuleTrigger.class);
        for (TrackedValueRuleTrigger trigger : TrackedValueRuleTrigger.values()) {
            indexed.put(trigger, new ArrayList<>());
        }
        for (LoadedTrackedValueRuleDefinition definition : definitions) {
            indexed.get(definition.definition().trigger()).add(definition.definition());
        }
        Map<TrackedValueRuleTrigger, List<TrackedValueRuleDefinition>> copied = new EnumMap<>(TrackedValueRuleTrigger.class);
        for (Map.Entry<TrackedValueRuleTrigger, List<TrackedValueRuleDefinition>> entry : indexed.entrySet()) {
            List<TrackedValueRuleDefinition> sortedRules = new ArrayList<>(entry.getValue());
            sortedRules.sort(Comparator.comparingInt(TrackedValueRuleDefinition::priority)
                    .reversed()
                    .thenComparing(rule -> rule.id().toString()));
            copied.put(entry.getKey(), List.copyOf(sortedRules));
        }
        return Map.copyOf(copied);
    }

    private static final class ReloadListener extends SimpleJsonResourceReloadListener {
        private ReloadListener() {
            super(GSON, DIRECTORY);
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> jsonById, ResourceManager resourceManager, ProfilerFiller profiler) {
            Map<ResourceLocation, LoadedTrackedValueRuleDefinition> definitions = new LinkedHashMap<>();
            for (Map.Entry<ResourceLocation, JsonElement> entry : jsonById.entrySet()) {
                try {
                    LoadedTrackedValueRuleDefinition definition = parseDefinition(entry.getKey(), entry.getValue());
                    LoadedTrackedValueRuleDefinition previous = definitions.putIfAbsent(definition.id(), definition);
                    if (previous != null) {
                        throw new IllegalStateException("Duplicate datapack tracked value rule id: " + definition.id());
                    }
                } catch (RuntimeException exception) {
                    XLib.LOGGER.error("Failed to parse datapack tracked value rule {}", entry.getKey(), exception);
                }
            }
            loadedDefinitions = Map.copyOf(definitions);
            rulesByTrigger = indexByTrigger(definitions.values());
        }
    }
}
