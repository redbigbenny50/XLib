package com.whatxe.xlib.value;

import com.whatxe.xlib.ability.AbilityData;
import com.whatxe.xlib.ability.AbilityRequirement;
import com.whatxe.xlib.ability.AbilityRequirements;
import com.whatxe.xlib.capability.CapabilityPolicyData;
import com.whatxe.xlib.combat.DamageModifierProfileData;
import com.whatxe.xlib.classification.EntityClassificationApi;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class TrackedValueRuleDefinition {
    private final ResourceLocation id;
    private final TrackedValueRuleTrigger trigger;
    private final int priority;
    private final ResourceLocation foodReplacementSourceId;
    private final List<AbilityRequirement> requirements;
    private final Set<ResourceLocation> targetEntityIds;
    private final Set<TagKey<EntityType<?>>> targetEntityTags;
    private final Set<ResourceLocation> itemIds;
    private final Set<TagKey<Item>> itemTags;
    private final Set<ResourceLocation> blockIds;
    private final Set<TagKey<Block>> blockTags;
    private final Set<ResourceLocation> advancementIds;
    private final Set<EquipmentSlot> armorSlots;
    private final Set<ResourceLocation> damageTypeIds;
    private final Set<TagKey<DamageType>> damageTypeTags;
    private final Set<ResourceLocation> clearValues;
    private final Map<ResourceLocation, Double> valueDeltas;
    private final Map<ResourceLocation, Double> setValues;
    private final Map<ResourceLocation, Double> multiplyValues;
    private final Map<ResourceLocation, Double> minValues;
    private final Map<ResourceLocation, Double> maxValues;
    private final Set<ResourceLocation> enableFoodReplacementValues;
    private final Set<ResourceLocation> disableFoodReplacementValues;
    private final Set<ResourceLocation> grantStatePolicies;
    private final Set<ResourceLocation> revokeStatePolicies;
    private final Set<ResourceLocation> grantStateFlags;
    private final Set<ResourceLocation> revokeStateFlags;
    private final Set<ResourceLocation> grantCapabilityPolicies;
    private final Set<ResourceLocation> revokeCapabilityPolicies;
    private final Set<ResourceLocation> grantDamageModifierProfiles;
    private final Set<ResourceLocation> revokeDamageModifierProfiles;
    private final ResourceLocation classificationSourceId;
    private final boolean clearClassificationSource;
    private final Set<ResourceLocation> grantSyntheticEntityTypes;
    private final Set<ResourceLocation> revokeSyntheticEntityTypes;
    private final Set<ResourceLocation> grantSyntheticTags;
    private final Set<ResourceLocation> revokeSyntheticTags;

    private TrackedValueRuleDefinition(
            ResourceLocation id,
            TrackedValueRuleTrigger trigger,
            int priority,
            ResourceLocation foodReplacementSourceId,
            List<AbilityRequirement> requirements,
            Set<ResourceLocation> targetEntityIds,
            Set<TagKey<EntityType<?>>> targetEntityTags,
            Set<ResourceLocation> itemIds,
            Set<TagKey<Item>> itemTags,
            Set<ResourceLocation> blockIds,
            Set<TagKey<Block>> blockTags,
            Set<ResourceLocation> advancementIds,
            Set<EquipmentSlot> armorSlots,
            Set<ResourceLocation> damageTypeIds,
            Set<TagKey<DamageType>> damageTypeTags,
            Set<ResourceLocation> clearValues,
            Map<ResourceLocation, Double> valueDeltas,
            Map<ResourceLocation, Double> setValues,
            Map<ResourceLocation, Double> multiplyValues,
            Map<ResourceLocation, Double> minValues,
            Map<ResourceLocation, Double> maxValues,
            Set<ResourceLocation> enableFoodReplacementValues,
            Set<ResourceLocation> disableFoodReplacementValues,
            Set<ResourceLocation> grantStatePolicies,
            Set<ResourceLocation> revokeStatePolicies,
            Set<ResourceLocation> grantStateFlags,
            Set<ResourceLocation> revokeStateFlags,
            Set<ResourceLocation> grantCapabilityPolicies,
            Set<ResourceLocation> revokeCapabilityPolicies,
            Set<ResourceLocation> grantDamageModifierProfiles,
            Set<ResourceLocation> revokeDamageModifierProfiles,
            ResourceLocation classificationSourceId,
            boolean clearClassificationSource,
            Set<ResourceLocation> grantSyntheticEntityTypes,
            Set<ResourceLocation> revokeSyntheticEntityTypes,
            Set<ResourceLocation> grantSyntheticTags,
            Set<ResourceLocation> revokeSyntheticTags
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.trigger = Objects.requireNonNull(trigger, "trigger");
        this.priority = priority;
        this.foodReplacementSourceId = Objects.requireNonNull(foodReplacementSourceId, "foodReplacementSourceId");
        this.requirements = List.copyOf(requirements);
        this.targetEntityIds = Set.copyOf(targetEntityIds);
        this.targetEntityTags = Set.copyOf(targetEntityTags);
        this.itemIds = Set.copyOf(itemIds);
        this.itemTags = Set.copyOf(itemTags);
        this.blockIds = Set.copyOf(blockIds);
        this.blockTags = Set.copyOf(blockTags);
        this.advancementIds = Set.copyOf(advancementIds);
        this.armorSlots = Set.copyOf(armorSlots);
        this.damageTypeIds = Set.copyOf(damageTypeIds);
        this.damageTypeTags = Set.copyOf(damageTypeTags);
        this.clearValues = Set.copyOf(clearValues);
        this.valueDeltas = Map.copyOf(valueDeltas);
        this.setValues = Map.copyOf(setValues);
        this.multiplyValues = Map.copyOf(multiplyValues);
        this.minValues = Map.copyOf(minValues);
        this.maxValues = Map.copyOf(maxValues);
        this.enableFoodReplacementValues = Set.copyOf(enableFoodReplacementValues);
        this.disableFoodReplacementValues = Set.copyOf(disableFoodReplacementValues);
        this.grantStatePolicies = Set.copyOf(grantStatePolicies);
        this.revokeStatePolicies = Set.copyOf(revokeStatePolicies);
        this.grantStateFlags = Set.copyOf(grantStateFlags);
        this.revokeStateFlags = Set.copyOf(revokeStateFlags);
        this.grantCapabilityPolicies = Set.copyOf(grantCapabilityPolicies);
        this.revokeCapabilityPolicies = Set.copyOf(revokeCapabilityPolicies);
        this.grantDamageModifierProfiles = Set.copyOf(grantDamageModifierProfiles);
        this.revokeDamageModifierProfiles = Set.copyOf(revokeDamageModifierProfiles);
        this.classificationSourceId = Objects.requireNonNull(classificationSourceId, "classificationSourceId");
        this.clearClassificationSource = clearClassificationSource;
        this.grantSyntheticEntityTypes = Set.copyOf(grantSyntheticEntityTypes);
        this.revokeSyntheticEntityTypes = Set.copyOf(revokeSyntheticEntityTypes);
        this.grantSyntheticTags = Set.copyOf(grantSyntheticTags);
        this.revokeSyntheticTags = Set.copyOf(revokeSyntheticTags);
    }

    public static Builder builder(ResourceLocation id, TrackedValueRuleTrigger trigger) {
        return new Builder(id, trigger);
    }

    public ResourceLocation id() {
        return this.id;
    }

    public TrackedValueRuleTrigger trigger() {
        return this.trigger;
    }

    public int priority() {
        return this.priority;
    }

    public ResourceLocation foodReplacementSourceId() {
        return this.foodReplacementSourceId;
    }

    public List<AbilityRequirement> requirements() {
        return this.requirements;
    }

    public Set<ResourceLocation> targetEntityIds() {
        return this.targetEntityIds;
    }

    public Set<TagKey<EntityType<?>>> targetEntityTags() {
        return this.targetEntityTags;
    }

    public Set<ResourceLocation> itemIds() {
        return this.itemIds;
    }

    public Set<TagKey<Item>> itemTags() {
        return this.itemTags;
    }

    public Set<ResourceLocation> blockIds() {
        return this.blockIds;
    }

    public Set<TagKey<Block>> blockTags() {
        return this.blockTags;
    }

    public Set<ResourceLocation> advancementIds() {
        return this.advancementIds;
    }

    public Set<EquipmentSlot> armorSlots() {
        return this.armorSlots;
    }

    public Set<ResourceLocation> damageTypeIds() {
        return this.damageTypeIds;
    }

    public Set<TagKey<DamageType>> damageTypeTags() {
        return this.damageTypeTags;
    }

    public Set<ResourceLocation> clearValues() {
        return this.clearValues;
    }

    public Map<ResourceLocation, Double> valueDeltas() {
        return this.valueDeltas;
    }

    public Map<ResourceLocation, Double> setValues() {
        return this.setValues;
    }

    public Map<ResourceLocation, Double> multiplyValues() {
        return this.multiplyValues;
    }

    public Map<ResourceLocation, Double> minValues() {
        return this.minValues;
    }

    public Map<ResourceLocation, Double> maxValues() {
        return this.maxValues;
    }

    public Set<ResourceLocation> enableFoodReplacementValues() {
        return this.enableFoodReplacementValues;
    }

    public Set<ResourceLocation> disableFoodReplacementValues() {
        return this.disableFoodReplacementValues;
    }

    public Set<ResourceLocation> grantStatePolicies() {
        return this.grantStatePolicies;
    }

    public Set<ResourceLocation> revokeStatePolicies() {
        return this.revokeStatePolicies;
    }

    public Set<ResourceLocation> grantStateFlags() {
        return this.grantStateFlags;
    }

    public Set<ResourceLocation> revokeStateFlags() {
        return this.revokeStateFlags;
    }

    public Set<ResourceLocation> grantCapabilityPolicies() {
        return this.grantCapabilityPolicies;
    }

    public Set<ResourceLocation> revokeCapabilityPolicies() {
        return this.revokeCapabilityPolicies;
    }

    public Set<ResourceLocation> grantDamageModifierProfiles() {
        return this.grantDamageModifierProfiles;
    }

    public Set<ResourceLocation> revokeDamageModifierProfiles() {
        return this.revokeDamageModifierProfiles;
    }

    public ResourceLocation classificationSourceId() {
        return this.classificationSourceId;
    }

    public boolean clearClassificationSource() {
        return this.clearClassificationSource;
    }

    public Set<ResourceLocation> grantSyntheticEntityTypes() {
        return this.grantSyntheticEntityTypes;
    }

    public Set<ResourceLocation> revokeSyntheticEntityTypes() {
        return this.revokeSyntheticEntityTypes;
    }

    public Set<ResourceLocation> grantSyntheticTags() {
        return this.grantSyntheticTags;
    }

    public Set<ResourceLocation> revokeSyntheticTags() {
        return this.revokeSyntheticTags;
    }

    public boolean matches(
            ServerPlayer player,
            AbilityData abilityData,
            @Nullable LivingEntity targetEntity,
            @Nullable ItemStack primaryStack,
            @Nullable ItemStack secondaryStack,
            @Nullable BlockState blockState,
            @Nullable ResourceLocation advancementId,
            @Nullable EquipmentSlot armorSlot,
            @Nullable DamageSource damageSource
    ) {
        if (AbilityRequirements.firstFailure(player, abilityData, this.requirements).isPresent()) {
            return false;
        }
        if (!matchesEntity(targetEntity)) {
            return false;
        }
        if (!matchesItem(primaryStack, secondaryStack)) {
            return false;
        }
        if (!matchesBlock(blockState)) {
            return false;
        }
        if (!matchesAdvancement(advancementId)) {
            return false;
        }
        if (!matchesArmorSlot(armorSlot)) {
            return false;
        }
        return matchesDamageType(damageSource);
    }

    public TrackedValueData applyTo(TrackedValueData currentData) {
        TrackedValueData updatedData = currentData;
        for (ResourceLocation valueId : this.clearValues) {
            updatedData = updatedData.withoutStoredAmount(valueId);
        }
        for (Map.Entry<ResourceLocation, Double> entry : this.setValues.entrySet()) {
            TrackedValueDefinition definition = TrackedValueApi.findDefinition(entry.getKey()).orElse(null);
            if (definition == null) {
                continue;
            }
            updatedData = updatedData.withExactAmount(entry.getKey(), definition.clamp(entry.getValue()));
        }
        for (Map.Entry<ResourceLocation, Double> entry : this.valueDeltas.entrySet()) {
            TrackedValueDefinition definition = TrackedValueApi.findDefinition(entry.getKey()).orElse(null);
            if (definition == null) {
                continue;
            }
            double currentAmount = TrackedValueApi.value(updatedData, entry.getKey());
            updatedData = updatedData.withExactAmount(entry.getKey(), definition.clamp(currentAmount + entry.getValue()));
        }
        for (Map.Entry<ResourceLocation, Double> entry : this.multiplyValues.entrySet()) {
            TrackedValueDefinition definition = TrackedValueApi.findDefinition(entry.getKey()).orElse(null);
            if (definition == null) {
                continue;
            }
            double currentAmount = TrackedValueApi.value(updatedData, entry.getKey());
            updatedData = updatedData.withExactAmount(entry.getKey(), definition.clamp(currentAmount * entry.getValue()));
        }
        for (Map.Entry<ResourceLocation, Double> entry : this.minValues.entrySet()) {
            TrackedValueDefinition definition = TrackedValueApi.findDefinition(entry.getKey()).orElse(null);
            if (definition == null) {
                continue;
            }
            double currentAmount = TrackedValueApi.value(updatedData, entry.getKey());
            updatedData = updatedData.withExactAmount(entry.getKey(), definition.clamp(Math.max(currentAmount, entry.getValue())));
        }
        for (Map.Entry<ResourceLocation, Double> entry : this.maxValues.entrySet()) {
            TrackedValueDefinition definition = TrackedValueApi.findDefinition(entry.getKey()).orElse(null);
            if (definition == null) {
                continue;
            }
            double currentAmount = TrackedValueApi.value(updatedData, entry.getKey());
            updatedData = updatedData.withExactAmount(entry.getKey(), definition.clamp(Math.min(currentAmount, entry.getValue())));
        }
        for (ResourceLocation valueId : this.enableFoodReplacementValues) {
            if (TrackedValueApi.findDefinition(valueId).isPresent()) {
                updatedData = updatedData.withFoodReplacementSource(valueId, this.foodReplacementSourceId, true);
            }
        }
        for (ResourceLocation valueId : this.disableFoodReplacementValues) {
            updatedData = updatedData.withFoodReplacementSource(valueId, this.foodReplacementSourceId, false);
        }
        return TrackedValueApi.sanitize(updatedData);
    }

    public AbilityData applyAbilityEffects(AbilityData currentData) {
        AbilityData updatedData = currentData;
        for (ResourceLocation policyId : this.grantStatePolicies) {
            updatedData = updatedData.withStatePolicySource(policyId, this.id, true);
        }
        for (ResourceLocation policyId : this.revokeStatePolicies) {
            updatedData = updatedData.withStatePolicySource(policyId, this.id, false);
        }
        for (ResourceLocation flagId : this.grantStateFlags) {
            updatedData = updatedData.withStateFlagSource(flagId, this.id, true);
        }
        for (ResourceLocation flagId : this.revokeStateFlags) {
            updatedData = updatedData.withStateFlagSource(flagId, this.id, false);
        }
        return updatedData;
    }

    public CapabilityPolicyData applyCapabilityEffects(CapabilityPolicyData currentData) {
        CapabilityPolicyData updatedData = currentData;
        for (ResourceLocation policyId : this.grantCapabilityPolicies) {
            updatedData = updatedData.withPolicySource(policyId, this.id, true);
        }
        for (ResourceLocation policyId : this.revokeCapabilityPolicies) {
            updatedData = updatedData.withPolicySource(policyId, this.id, false);
        }
        return updatedData;
    }

    public DamageModifierProfileData applyDamageModifierEffects(DamageModifierProfileData currentData) {
        DamageModifierProfileData updatedData = currentData;
        for (ResourceLocation profileId : this.grantDamageModifierProfiles) {
            updatedData = updatedData.withProfileSource(profileId, this.id, true);
        }
        for (ResourceLocation profileId : this.revokeDamageModifierProfiles) {
            updatedData = updatedData.withProfileSource(profileId, this.id, false);
        }
        return updatedData;
    }

    public void applyEntityEffects(ServerPlayer player) {
        if (this.clearClassificationSource) {
            EntityClassificationApi.clearSource(player, this.classificationSourceId);
        }
        for (ResourceLocation entityTypeId : this.grantSyntheticEntityTypes) {
            EntityClassificationApi.grantSyntheticEntityType(player, entityTypeId, this.classificationSourceId);
        }
        for (ResourceLocation entityTypeId : this.revokeSyntheticEntityTypes) {
            EntityClassificationApi.revokeSyntheticEntityType(player, entityTypeId, this.classificationSourceId);
        }
        for (ResourceLocation tagId : this.grantSyntheticTags) {
            EntityClassificationApi.grantSyntheticTag(player, tagId, this.classificationSourceId);
        }
        for (ResourceLocation tagId : this.revokeSyntheticTags) {
            EntityClassificationApi.revokeSyntheticTag(player, tagId, this.classificationSourceId);
        }
    }

    private boolean matchesEntity(@Nullable LivingEntity targetEntity) {
        if (this.targetEntityIds.isEmpty() && this.targetEntityTags.isEmpty()) {
            return true;
        }
        if (targetEntity == null) {
            return false;
        }
        return EntityClassificationApi.matchesSelector(
                targetEntity,
                this.targetEntityIds,
                this.targetEntityTags.stream().map(TagKey::location).toList()
        );
    }

    private boolean matchesItem(@Nullable ItemStack primaryStack, @Nullable ItemStack secondaryStack) {
        if (this.itemIds.isEmpty() && this.itemTags.isEmpty()) {
            return true;
        }
        return matchesSingleItem(primaryStack) || matchesSingleItem(secondaryStack);
    }

    private boolean matchesSingleItem(@Nullable ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return this.itemIds.contains(itemId) || this.itemTags.stream().anyMatch(stack::is);
    }

    private boolean matchesBlock(@Nullable BlockState blockState) {
        if (this.blockIds.isEmpty() && this.blockTags.isEmpty()) {
            return true;
        }
        if (blockState == null) {
            return false;
        }
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(blockState.getBlock());
        if (this.blockIds.contains(blockId)) {
            return true;
        }
        return this.blockTags.stream().anyMatch(blockState::is);
    }

    private boolean matchesAdvancement(@Nullable ResourceLocation advancementId) {
        if (this.advancementIds.isEmpty()) {
            return true;
        }
        return advancementId != null && this.advancementIds.contains(advancementId);
    }

    private boolean matchesArmorSlot(@Nullable EquipmentSlot armorSlot) {
        if (this.armorSlots.isEmpty()) {
            return true;
        }
        return armorSlot != null && this.armorSlots.contains(armorSlot);
    }

    private boolean matchesDamageType(@Nullable DamageSource damageSource) {
        if (this.damageTypeIds.isEmpty() && this.damageTypeTags.isEmpty()) {
            return true;
        }
        if (damageSource == null) {
            return false;
        }
        ResourceLocation damageTypeId = damageSource.typeHolder().unwrapKey()
                .map(key -> key.location())
                .orElse(null);
        if (damageTypeId != null && this.damageTypeIds.contains(damageTypeId)) {
            return true;
        }
        return this.damageTypeTags.stream().anyMatch(damageSource::is);
    }

    public static final class Builder {
        private final ResourceLocation id;
        private final TrackedValueRuleTrigger trigger;
        private int priority;
        private ResourceLocation foodReplacementSourceId;
        private final Set<AbilityRequirement> requirements = new LinkedHashSet<>();
        private final Set<ResourceLocation> targetEntityIds = new LinkedHashSet<>();
        private final Set<TagKey<EntityType<?>>> targetEntityTags = new LinkedHashSet<>();
        private final Set<ResourceLocation> itemIds = new LinkedHashSet<>();
        private final Set<TagKey<Item>> itemTags = new LinkedHashSet<>();
        private final Set<ResourceLocation> blockIds = new LinkedHashSet<>();
        private final Set<TagKey<Block>> blockTags = new LinkedHashSet<>();
        private final Set<ResourceLocation> advancementIds = new LinkedHashSet<>();
        private final Set<EquipmentSlot> armorSlots = new LinkedHashSet<>();
        private final Set<ResourceLocation> damageTypeIds = new LinkedHashSet<>();
        private final Set<TagKey<DamageType>> damageTypeTags = new LinkedHashSet<>();
        private final Set<ResourceLocation> clearValues = new LinkedHashSet<>();
        private final Map<ResourceLocation, Double> valueDeltas = new LinkedHashMap<>();
        private final Map<ResourceLocation, Double> setValues = new LinkedHashMap<>();
        private final Map<ResourceLocation, Double> multiplyValues = new LinkedHashMap<>();
        private final Map<ResourceLocation, Double> minValues = new LinkedHashMap<>();
        private final Map<ResourceLocation, Double> maxValues = new LinkedHashMap<>();
        private final Set<ResourceLocation> enableFoodReplacementValues = new LinkedHashSet<>();
        private final Set<ResourceLocation> disableFoodReplacementValues = new LinkedHashSet<>();
        private final Set<ResourceLocation> grantStatePolicies = new LinkedHashSet<>();
        private final Set<ResourceLocation> revokeStatePolicies = new LinkedHashSet<>();
        private final Set<ResourceLocation> grantStateFlags = new LinkedHashSet<>();
        private final Set<ResourceLocation> revokeStateFlags = new LinkedHashSet<>();
        private final Set<ResourceLocation> grantCapabilityPolicies = new LinkedHashSet<>();
        private final Set<ResourceLocation> revokeCapabilityPolicies = new LinkedHashSet<>();
        private final Set<ResourceLocation> grantDamageModifierProfiles = new LinkedHashSet<>();
        private final Set<ResourceLocation> revokeDamageModifierProfiles = new LinkedHashSet<>();
        private ResourceLocation classificationSourceId;
        private boolean clearClassificationSource;
        private final Set<ResourceLocation> grantSyntheticEntityTypes = new LinkedHashSet<>();
        private final Set<ResourceLocation> revokeSyntheticEntityTypes = new LinkedHashSet<>();
        private final Set<ResourceLocation> grantSyntheticTags = new LinkedHashSet<>();
        private final Set<ResourceLocation> revokeSyntheticTags = new LinkedHashSet<>();

        private Builder(ResourceLocation id, TrackedValueRuleTrigger trigger) {
            this.id = Objects.requireNonNull(id, "id");
            this.trigger = Objects.requireNonNull(trigger, "trigger");
            this.foodReplacementSourceId = id;
            this.classificationSourceId = id;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder foodReplacementSource(ResourceLocation sourceId) {
            this.foodReplacementSourceId = Objects.requireNonNull(sourceId, "sourceId");
            return this;
        }

        public Builder classificationSource(ResourceLocation sourceId) {
            this.classificationSourceId = Objects.requireNonNull(sourceId, "sourceId");
            return this;
        }

        public Builder requirement(AbilityRequirement requirement) {
            this.requirements.add(Objects.requireNonNull(requirement, "requirement"));
            return this;
        }

        public Builder requirements(Collection<AbilityRequirement> requirements) {
            requirements.stream().filter(Objects::nonNull).forEach(this.requirements::add);
            return this;
        }

        public Builder targetEntity(ResourceLocation entityTypeId) {
            this.targetEntityIds.add(Objects.requireNonNull(entityTypeId, "entityTypeId"));
            return this;
        }

        public Builder targetEntityTag(ResourceLocation tagId) {
            this.targetEntityTags.add(TagKey.create(Registries.ENTITY_TYPE, Objects.requireNonNull(tagId, "tagId")));
            return this;
        }

        public Builder item(ResourceLocation itemId) {
            this.itemIds.add(Objects.requireNonNull(itemId, "itemId"));
            return this;
        }

        public Builder itemTag(ResourceLocation tagId) {
            this.itemTags.add(TagKey.create(Registries.ITEM, Objects.requireNonNull(tagId, "tagId")));
            return this;
        }

        public Builder block(ResourceLocation blockId) {
            this.blockIds.add(Objects.requireNonNull(blockId, "blockId"));
            return this;
        }

        public Builder blockTag(ResourceLocation tagId) {
            this.blockTags.add(TagKey.create(Registries.BLOCK, Objects.requireNonNull(tagId, "tagId")));
            return this;
        }

        public Builder advancement(ResourceLocation advancementId) {
            this.advancementIds.add(Objects.requireNonNull(advancementId, "advancementId"));
            return this;
        }

        public Builder armorSlot(EquipmentSlot armorSlot) {
            this.armorSlots.add(Objects.requireNonNull(armorSlot, "armorSlot"));
            return this;
        }

        public Builder damageType(ResourceLocation damageTypeId) {
            this.damageTypeIds.add(Objects.requireNonNull(damageTypeId, "damageTypeId"));
            return this;
        }

        public Builder damageTypeTag(ResourceLocation tagId) {
            this.damageTypeTags.add(TagKey.create(Registries.DAMAGE_TYPE, Objects.requireNonNull(tagId, "tagId")));
            return this;
        }

        public Builder clearValue(ResourceLocation valueId) {
            this.clearValues.add(Objects.requireNonNull(valueId, "valueId"));
            return this;
        }

        public Builder addValue(ResourceLocation valueId, double amount) {
            this.valueDeltas.put(Objects.requireNonNull(valueId, "valueId"), validateFinite(amount, "amount"));
            return this;
        }

        public Builder setValue(ResourceLocation valueId, double amount) {
            this.setValues.put(Objects.requireNonNull(valueId, "valueId"), validateFinite(amount, "amount"));
            return this;
        }

        public Builder multiplyValue(ResourceLocation valueId, double amount) {
            this.multiplyValues.put(Objects.requireNonNull(valueId, "valueId"), validateFinite(amount, "amount"));
            return this;
        }

        public Builder minValue(ResourceLocation valueId, double amount) {
            this.minValues.put(Objects.requireNonNull(valueId, "valueId"), validateFinite(amount, "amount"));
            return this;
        }

        public Builder maxValue(ResourceLocation valueId, double amount) {
            this.maxValues.put(Objects.requireNonNull(valueId, "valueId"), validateFinite(amount, "amount"));
            return this;
        }

        public Builder enableFoodReplacement(ResourceLocation valueId) {
            this.enableFoodReplacementValues.add(Objects.requireNonNull(valueId, "valueId"));
            return this;
        }

        public Builder disableFoodReplacement(ResourceLocation valueId) {
            this.disableFoodReplacementValues.add(Objects.requireNonNull(valueId, "valueId"));
            return this;
        }

        public Builder grantStatePolicy(ResourceLocation policyId) {
            this.grantStatePolicies.add(Objects.requireNonNull(policyId, "policyId"));
            return this;
        }

        public Builder revokeStatePolicy(ResourceLocation policyId) {
            this.revokeStatePolicies.add(Objects.requireNonNull(policyId, "policyId"));
            return this;
        }

        public Builder grantStateFlag(ResourceLocation flagId) {
            this.grantStateFlags.add(Objects.requireNonNull(flagId, "flagId"));
            return this;
        }

        public Builder revokeStateFlag(ResourceLocation flagId) {
            this.revokeStateFlags.add(Objects.requireNonNull(flagId, "flagId"));
            return this;
        }

        public Builder grantCapabilityPolicy(ResourceLocation policyId) {
            this.grantCapabilityPolicies.add(Objects.requireNonNull(policyId, "policyId"));
            return this;
        }

        public Builder revokeCapabilityPolicy(ResourceLocation policyId) {
            this.revokeCapabilityPolicies.add(Objects.requireNonNull(policyId, "policyId"));
            return this;
        }

        public Builder grantDamageModifierProfile(ResourceLocation profileId) {
            this.grantDamageModifierProfiles.add(Objects.requireNonNull(profileId, "profileId"));
            return this;
        }

        public Builder revokeDamageModifierProfile(ResourceLocation profileId) {
            this.revokeDamageModifierProfiles.add(Objects.requireNonNull(profileId, "profileId"));
            return this;
        }

        public Builder clearClassificationSource(boolean clearClassificationSource) {
            this.clearClassificationSource = clearClassificationSource;
            return this;
        }

        public Builder grantSyntheticEntityType(ResourceLocation entityTypeId) {
            this.grantSyntheticEntityTypes.add(Objects.requireNonNull(entityTypeId, "entityTypeId"));
            return this;
        }

        public Builder revokeSyntheticEntityType(ResourceLocation entityTypeId) {
            this.revokeSyntheticEntityTypes.add(Objects.requireNonNull(entityTypeId, "entityTypeId"));
            return this;
        }

        public Builder grantSyntheticTag(ResourceLocation tagId) {
            this.grantSyntheticTags.add(Objects.requireNonNull(tagId, "tagId"));
            return this;
        }

        public Builder revokeSyntheticTag(ResourceLocation tagId) {
            this.revokeSyntheticTags.add(Objects.requireNonNull(tagId, "tagId"));
            return this;
        }

        public TrackedValueRuleDefinition build() {
            return new TrackedValueRuleDefinition(
                    this.id,
                    this.trigger,
                    this.priority,
                    this.foodReplacementSourceId,
                    List.copyOf(this.requirements),
                    Set.copyOf(this.targetEntityIds),
                    Set.copyOf(this.targetEntityTags),
                    Set.copyOf(this.itemIds),
                    Set.copyOf(this.itemTags),
                    Set.copyOf(this.blockIds),
                    Set.copyOf(this.blockTags),
                    Set.copyOf(this.advancementIds),
                    Set.copyOf(this.armorSlots),
                    Set.copyOf(this.damageTypeIds),
                    Set.copyOf(this.damageTypeTags),
                    Set.copyOf(this.clearValues),
                    Map.copyOf(this.valueDeltas),
                    Map.copyOf(this.setValues),
                    Map.copyOf(this.multiplyValues),
                    Map.copyOf(this.minValues),
                    Map.copyOf(this.maxValues),
                    Set.copyOf(this.enableFoodReplacementValues),
                    Set.copyOf(this.disableFoodReplacementValues),
                    Set.copyOf(this.grantStatePolicies),
                    Set.copyOf(this.revokeStatePolicies),
                    Set.copyOf(this.grantStateFlags),
                    Set.copyOf(this.revokeStateFlags),
                    Set.copyOf(this.grantCapabilityPolicies),
                    Set.copyOf(this.revokeCapabilityPolicies),
                    Set.copyOf(this.grantDamageModifierProfiles),
                    Set.copyOf(this.revokeDamageModifierProfiles),
                    this.classificationSourceId,
                    this.clearClassificationSource,
                    Set.copyOf(this.grantSyntheticEntityTypes),
                    Set.copyOf(this.revokeSyntheticEntityTypes),
                    Set.copyOf(this.grantSyntheticTags),
                    Set.copyOf(this.revokeSyntheticTags)
            );
        }
    }

    private static double validateFinite(double value, String label) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(label + " must be finite");
        }
        return value;
    }

    public static EquipmentSlot parseArmorSlot(String rawValue) {
        return EquipmentSlot.valueOf(rawValue.trim().toUpperCase(Locale.ROOT));
    }
}
