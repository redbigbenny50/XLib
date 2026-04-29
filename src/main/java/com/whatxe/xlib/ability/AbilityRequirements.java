package com.whatxe.xlib.ability;

import com.whatxe.xlib.attachment.ModAttachments;
import com.whatxe.xlib.combat.CombatMarkState;
import com.whatxe.xlib.combat.CombatReactionApi;
import com.whatxe.xlib.progression.UpgradeApi;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;

public final class AbilityRequirements {
    private AbilityRequirements() {}

    public static AbilityRequirement always() {
        return AbilityRequirement.of(() -> Component.empty(), (player, data) -> Optional.empty());
    }

    public static AbilityRequirement never() {
        return AbilityRequirement.of(
                () -> Component.translatable("message.xlib.requirement_never"),
                (player, data) -> Optional.of(Component.translatable("message.xlib.requirement_never"))
        );
    }

    public static AbilityRequirement predicate(Component description, BiPredicate<Player, AbilityData> predicate) {
        return predicate(() -> description, predicate);
    }

    public static AbilityRequirement predicate(
            Supplier<Component> descriptionSupplier,
            BiPredicate<Player, AbilityData> predicate
    ) {
        Objects.requireNonNull(descriptionSupplier, "descriptionSupplier");
        Objects.requireNonNull(predicate, "predicate");
        return AbilityRequirement.of(descriptionSupplier, (player, data) -> predicate.test(player, data)
                ? Optional.empty()
                : Optional.of(descriptionSupplier.get()));
    }

    public static AbilityRequirement predicate(
            Component description,
            Component failure,
            BiPredicate<Player, AbilityData> predicate
    ) {
        return predicate(() -> description, () -> failure, predicate);
    }

    public static AbilityRequirement predicate(
            Supplier<Component> descriptionSupplier,
            Supplier<Component> failureSupplier,
            BiPredicate<Player, AbilityData> predicate
    ) {
        Objects.requireNonNull(descriptionSupplier, "descriptionSupplier");
        Objects.requireNonNull(failureSupplier, "failureSupplier");
        Objects.requireNonNull(predicate, "predicate");
        return AbilityRequirement.of(descriptionSupplier, (player, data) -> predicate.test(player, data)
                ? Optional.empty()
                : Optional.of(failureSupplier.get()));
    }

    public static AbilityRequirement all(AbilityRequirement... requirements) {
        return all(Arrays.asList(requirements));
    }

    public static AbilityRequirement all(Collection<AbilityRequirement> requirements) {
        List<AbilityRequirement> copied = List.copyOf(requirements);
        if (copied.isEmpty()) {
            throw new IllegalArgumentException("all(...) requires at least one requirement");
        }
        Supplier<Component> descriptionSupplier =
                () -> Component.translatable("message.xlib.requirement_all", joinedDescriptions(copied));
        return AbilityRequirement.of(descriptionSupplier, (player, data) -> firstFailure(player, data, copied));
    }

    public static AbilityRequirement any(AbilityRequirement... requirements) {
        return any(Arrays.asList(requirements));
    }

    public static AbilityRequirement any(Collection<AbilityRequirement> requirements) {
        List<AbilityRequirement> copied = List.copyOf(requirements);
        if (copied.isEmpty()) {
            throw new IllegalArgumentException("any(...) requires at least one requirement");
        }
        Supplier<Component> descriptionSupplier =
                () -> Component.translatable("message.xlib.requirement_any", joinedDescriptions(copied));
        return AbilityRequirement.of(descriptionSupplier, (player, data) -> {
            for (AbilityRequirement requirement : copied) {
                if (requirement.validate(player, data).isEmpty()) {
                    return Optional.empty();
                }
            }
            return Optional.of(descriptionSupplier.get());
        });
    }

    public static AbilityRequirement not(AbilityRequirement requirement) {
        AbilityRequirement resolvedRequirement = Objects.requireNonNull(requirement, "requirement");
        Supplier<Component> descriptionSupplier =
                () -> Component.translatable("message.xlib.requirement_not", resolvedRequirement.description());
        return AbilityRequirement.of(descriptionSupplier, (player, data) -> resolvedRequirement.validate(player, data).isPresent()
                ? Optional.empty()
                : Optional.of(descriptionSupplier.get()));
    }

    public static AbilityRequirement sprinting() {
        return predicate(() -> Component.translatable("message.xlib.requirement_sprinting"),
                (player, data) -> player != null && player.isSprinting());
    }

    public static AbilityRequirement sneaking() {
        return predicate(() -> Component.translatable("message.xlib.requirement_sneaking"),
                (player, data) -> player != null && player.isCrouching());
    }

    public static AbilityRequirement onGround() {
        return predicate(() -> Component.translatable("message.xlib.requirement_on_ground"),
                (player, data) -> player != null && player.onGround());
    }

    public static AbilityRequirement inWater() {
        return predicate(() -> Component.translatable("message.xlib.requirement_in_water"),
                (player, data) -> player != null && player.isInWater());
    }

    public static AbilityRequirement holding(ItemLike itemLike) {
        Item item = Objects.requireNonNull(itemLike, "itemLike").asItem();
        return predicate(() -> Component.translatable("message.xlib.requirement_holding_item", item.getDescription()), (player, data) -> player != null
                && (player.getMainHandItem().is(item) || player.getOffhandItem().is(item)));
    }

    public static AbilityRequirement wearing(ItemLike itemLike) {
        Item item = Objects.requireNonNull(itemLike, "itemLike").asItem();
        return predicate(() -> Component.translatable("message.xlib.requirement_wearing_item", item.getDescription()), (player, data) -> player != null
                && player.getInventory().armor.stream().anyMatch(stack -> stack.is(item)));
    }

    public static AbilityRequirement resourceAtLeast(ResourceLocation resourceId, int amount) {
        Objects.requireNonNull(resourceId, "resourceId");
        return predicate(
                () -> Component.translatable("message.xlib.requirement_resource", amount, displayResourceName(resourceId)),
                (player, data) -> data.resourceAmount(resourceId) >= amount
        );
    }

    public static AbilityRequirement resourceAtLeastExact(ResourceLocation resourceId, double amount) {
        Objects.requireNonNull(resourceId, "resourceId");
        return predicate(
                () -> Component.translatable("message.xlib.requirement_resource_exact", amount, displayResourceName(resourceId)),
                (player, data) -> data.resourceAmountExact(resourceId) + 1.0E-9D >= amount
        );
    }

    public static AbilityRequirement modeActive(ResourceLocation modeAbilityId) {
        Objects.requireNonNull(modeAbilityId, "modeAbilityId");
        return predicate(
                () -> Component.translatable("message.xlib.requirement_mode_active", displayAbilityName(modeAbilityId)),
                (player, data) -> data.isModeActive(modeAbilityId)
        );
    }

    public static AbilityRequirement modeInactive(ResourceLocation modeAbilityId) {
        Objects.requireNonNull(modeAbilityId, "modeAbilityId");
        return predicate(
                () -> Component.translatable("message.xlib.requirement_mode_inactive", displayAbilityName(modeAbilityId)),
                (player, data) -> !data.isModeActive(modeAbilityId)
        );
    }

    public static AbilityRequirement hasAbility(ResourceLocation abilityId) {
        Objects.requireNonNull(abilityId, "abilityId");
        return predicate(
                () -> Component.translatable("message.xlib.requirement_has_ability", displayAbilityName(abilityId)),
                (player, data) -> data.canUseAbility(abilityId)
        );
    }

    public static AbilityRequirement lacksAbility(ResourceLocation abilityId) {
        Objects.requireNonNull(abilityId, "abilityId");
        return predicate(
                () -> Component.translatable("message.xlib.requirement_lacks_ability", displayAbilityName(abilityId)),
                (player, data) -> !data.canUseAbility(abilityId)
        );
    }

    public static AbilityRequirement hasPassive(ResourceLocation passiveId) {
        Objects.requireNonNull(passiveId, "passiveId");
        return predicate(
                () -> Component.translatable("message.xlib.requirement_has_passive", displayPassiveName(passiveId)),
                (player, data) -> data.hasPassive(passiveId)
        );
    }

    public static AbilityRequirement lacksPassive(ResourceLocation passiveId) {
        Objects.requireNonNull(passiveId, "passiveId");
        return predicate(
                () -> Component.translatable("message.xlib.requirement_lacks_passive", displayPassiveName(passiveId)),
                (player, data) -> !data.hasPassive(passiveId)
        );
    }

    public static AbilityRequirement dimension(ResourceLocation dimensionId) {
        Objects.requireNonNull(dimensionId, "dimensionId");
        return predicate(
                () -> Component.translatable("message.xlib.requirement_dimension", displayReadableId(dimensionId)),
                (player, data) -> player != null && player.level().dimension().location().equals(dimensionId)
        );
    }

    public static AbilityRequirement notDimension(ResourceLocation dimensionId) {
        Objects.requireNonNull(dimensionId, "dimensionId");
        return predicate(
                () -> Component.translatable("message.xlib.requirement_not_dimension", displayReadableId(dimensionId)),
                (player, data) -> player != null && !player.level().dimension().location().equals(dimensionId)
        );
    }

    public static AbilityRequirement biome(ResourceLocation biomeId) {
        Objects.requireNonNull(biomeId, "biomeId");
        return predicate(
                () -> Component.translatable("message.xlib.requirement_biome", displayReadableId(biomeId)),
                (player, data) -> player != null && player.level()
                .getBiome(player.blockPosition())
                .unwrapKey()
                .map(key -> key.location().equals(biomeId))
                .orElse(false)
        );
    }

    public static AbilityRequirement notBiome(ResourceLocation biomeId) {
        Objects.requireNonNull(biomeId, "biomeId");
        return predicate(
                () -> Component.translatable("message.xlib.requirement_not_biome", displayReadableId(biomeId)),
                (player, data) -> player != null && player.level()
                .getBiome(player.blockPosition())
                .unwrapKey()
                .map(key -> !key.location().equals(biomeId))
                .orElse(false)
        );
    }

    public static AbilityRequirement team(String teamName) {
        Objects.requireNonNull(teamName, "teamName");
        return predicate(() -> Component.translatable("message.xlib.requirement_team", teamName),
                (player, data) -> player != null && player.getTeam() != null && teamName.equals(player.getTeam().getName()));
    }

    public static AbilityRequirement notTeam(String teamName) {
        Objects.requireNonNull(teamName, "teamName");
        return predicate(() -> Component.translatable("message.xlib.requirement_not_team", teamName),
                (player, data) -> player != null && (player.getTeam() == null || !teamName.equals(player.getTeam().getName())));
    }

    public static AbilityRequirement statusEffect(ResourceLocation effectId) {
        Objects.requireNonNull(effectId, "effectId");
        return predicate(
                () -> Component.translatable("message.xlib.requirement_status_effect", displayReadableId(effectId)),
                (player, data) -> player != null && player.getActiveEffects().stream()
                        .anyMatch(activeEffect -> effectId.equals(BuiltInRegistries.MOB_EFFECT.getKey(activeEffect.getEffect().value())))
        );
    }

    public static AbilityRequirement noStatusEffect(ResourceLocation effectId) {
        Objects.requireNonNull(effectId, "effectId");
        return predicate(
                () -> Component.translatable("message.xlib.requirement_no_status_effect", displayReadableId(effectId)),
                (player, data) -> player != null && player.getActiveEffects().stream()
                        .noneMatch(activeEffect -> effectId.equals(BuiltInRegistries.MOB_EFFECT.getKey(activeEffect.getEffect().value())))
        );
    }

    public static AbilityRequirement markActive(ResourceLocation markId) {
        Objects.requireNonNull(markId, "markId");
        return predicate(
                () -> Component.translatable("message.xlib.requirement_mark_active", displayReadableId(markId)),
                (player, data) -> player != null && ModAttachments.getMarks(player).has(markId)
        );
    }

    public static AbilityRequirement markStacksAtLeast(ResourceLocation markId, int minimumStacks) {
        Objects.requireNonNull(markId, "markId");
        return predicate(
                () -> Component.translatable("message.xlib.requirement_mark_stacks", displayReadableId(markId), minimumStacks),
                (player, data) -> {
                    if (player == null) {
                        return false;
                    }
                    CombatMarkState state = ModAttachments.getMarks(player).state(markId);
                    return state != null && state.stacks() >= minimumStacks;
                }
        );
    }

    public static AbilityRequirement markValueAtLeast(ResourceLocation markId, double minimumValue) {
        Objects.requireNonNull(markId, "markId");
        return predicate(
                () -> Component.translatable("message.xlib.requirement_mark_value", displayReadableId(markId), minimumValue),
                (player, data) -> {
                    if (player == null) {
                        return false;
                    }
                    CombatMarkState state = ModAttachments.getMarks(player).state(markId);
                    return state != null && state.value() >= minimumValue;
                }
        );
    }

    public static AbilityRequirement recentlyHurtWithin(int ticks) {
        return predicate(
                () -> Component.translatable("message.xlib.requirement_recently_hurt", ticks),
                (player, data) -> player != null && CombatReactionApi.recentlyHurtWithin(player, ticks)
        );
    }

    public static AbilityRequirement cooldownReady(ResourceLocation abilityId) {
        Objects.requireNonNull(abilityId, "abilityId");
        return predicate(() -> Component.translatable("message.xlib.requirement_cooldown_ready", displayAbilityName(abilityId)), (player, data) -> {
            AbilityDefinition ability = AbilityApi.findAbility(abilityId).orElse(null);
            if (ability == null) {
                return false;
            }
            if (ability.usesCharges()) {
                return data.chargeCountFor(abilityId, ability.maxCharges()) > 0;
            }
            return data.cooldownFor(abilityId) <= 0;
        });
    }

    public static AbilityRequirement comboWindowActive(ResourceLocation abilityId) {
        Objects.requireNonNull(abilityId, "abilityId");
        return predicate(
                () -> Component.translatable("message.xlib.requirement_combo_window", displayAbilityName(abilityId)),
                (player, data) -> data.comboWindowFor(abilityId) > 0
        );
    }

    public static AbilityRequirement detectorActive(ResourceLocation detectorId) {
        Objects.requireNonNull(detectorId, "detectorId");
        return predicate(
                () -> Component.translatable("message.xlib.requirement_detector", displayReadableId(detectorId)),
                (player, data) -> AbilityDetectorApi.hasActiveDetector(data, detectorId)
        );
    }

    public static AbilityRequirement statePolicyActive(ResourceLocation statePolicyId) {
        Objects.requireNonNull(statePolicyId, "statePolicyId");
        return predicate(
                () -> Component.translatable("message.xlib.requirement_state_policy", displayReadableId(statePolicyId)),
                (player, data) -> StatePolicyApi.hasActivePolicy(data, statePolicyId)
        );
    }

    public static AbilityRequirement stateFlagActive(ResourceLocation stateFlagId) {
        Objects.requireNonNull(stateFlagId, "stateFlagId");
        return predicate(
                () -> Component.translatable("message.xlib.requirement_state_flag", displayReadableId(stateFlagId)),
                (player, data) -> StateFlagApi.hasActiveFlag(data, stateFlagId)
        );
    }

    public static AbilityRequirement capabilityPolicyActive(ResourceLocation policyId) {
        Objects.requireNonNull(policyId, "policyId");
        return predicate(
                () -> Component.translatable("message.xlib.requirement_capability_policy", displayReadableId(policyId)),
                (player, data) -> player != null && com.whatxe.xlib.capability.CapabilityPolicyApi.hasActivePolicy(player, policyId)
        );
    }

    public static AbilityRequirement lifecycleStageActive(ResourceLocation stageId) {
        Objects.requireNonNull(stageId, "stageId");
        return predicate(
                () -> Component.translatable("message.xlib.requirement_lifecycle_stage", displayReadableId(stageId)),
                (player, data) -> player != null && com.whatxe.xlib.lifecycle.LifecycleStageApi.isInStage(player, stageId)
        );
    }

    public static AbilityRequirement visualFormActive(ResourceLocation formId) {
        Objects.requireNonNull(formId, "formId");
        return predicate(
                () -> Component.translatable("message.xlib.requirement_visual_form", displayReadableId(formId)),
                (player, data) -> player != null && com.whatxe.xlib.form.VisualFormApi.hasForm(player, formId)
        );
    }

    public static AbilityRequirement bindingActive(ResourceLocation bindingId) {
        Objects.requireNonNull(bindingId, "bindingId");
        return predicate(
                () -> Component.translatable("message.xlib.requirement_binding", displayReadableId(bindingId)),
                (player, data) -> player instanceof net.minecraft.world.entity.LivingEntity le
                        && com.whatxe.xlib.binding.EntityBindingApi.bindings(le).stream()
                                .anyMatch(s -> s.bindingId().equals(bindingId))
        );
    }

    public static AbilityRequirement bindingKindActive(com.whatxe.xlib.binding.EntityBindingKind kind) {
        Objects.requireNonNull(kind, "kind");
        return predicate(
                () -> Component.translatable("message.xlib.requirement_binding_kind", kind.name()),
                (player, data) -> player instanceof net.minecraft.world.entity.LivingEntity le
                        && !com.whatxe.xlib.binding.EntityBindingApi.bindings(le, kind).isEmpty()
        );
    }

    public static AbilityRequirement bodyTransitionActive() {
        return predicate(
                Component.translatable("message.xlib.requirement_body_transition"),
                (player, data) -> player != null && com.whatxe.xlib.body.BodyTransitionApi.isTransitioning(player)
        );
    }

    public static AbilityRequirement identityActive(ResourceLocation identityId) {
        Objects.requireNonNull(identityId, "identityId");
        return predicate(
                () -> Component.translatable("message.xlib.requirement_identity", displayReadableId(identityId)),
                (player, data) -> IdentityApi.hasIdentity(data, identityId)
        );
    }

    public static AbilityRequirement artifactActive(ResourceLocation artifactId) {
        Objects.requireNonNull(artifactId, "artifactId");
        return predicate(
                () -> Component.translatable("message.xlib.requirement_artifact_active", displayReadableId(artifactId)),
                (player, data) -> player != null && ArtifactApi.isActive(player, artifactId)
        );
    }

    public static AbilityRequirement artifactUnlocked(ResourceLocation artifactId) {
        Objects.requireNonNull(artifactId, "artifactId");
        return predicate(
                () -> Component.translatable("message.xlib.requirement_artifact_unlocked", displayReadableId(artifactId)),
                (player, data) -> ArtifactApi.isUnlocked(data, artifactId)
        );
    }

    // -------------------------------------------------------------------------
    // Player state predicates
    // -------------------------------------------------------------------------

    public static AbilityRequirement inLava() {
        return predicate(() -> Component.translatable("message.xlib.requirement_in_lava"),
                (player, data) -> player != null && player.isInLava());
    }

    public static AbilityRequirement swimming() {
        return predicate(() -> Component.translatable("message.xlib.requirement_swimming"),
                (player, data) -> player != null && player.isSwimming());
    }

    public static AbilityRequirement onFire() {
        return predicate(() -> Component.translatable("message.xlib.requirement_on_fire"),
                (player, data) -> player != null && player.isOnFire());
    }

    public static AbilityRequirement gliding() {
        return predicate(() -> Component.translatable("message.xlib.requirement_gliding"),
                (player, data) -> player != null && player.isFallFlying());
    }

    public static AbilityRequirement underOpenSky() {
        return predicate(() -> Component.translatable("message.xlib.requirement_under_open_sky"),
                (player, data) -> player != null && player.level().canSeeSky(player.blockPosition()));
    }

    public static AbilityRequirement creative() {
        return predicate(() -> Component.translatable("message.xlib.requirement_creative"),
                (player, data) -> player != null && player.isCreative());
    }

    public static AbilityRequirement spectator() {
        return predicate(() -> Component.translatable("message.xlib.requirement_spectator"),
                (player, data) -> player != null && player.isSpectator());
    }

    // -------------------------------------------------------------------------
    // Block predicates
    // -------------------------------------------------------------------------

    public static AbilityRequirement standingOnBlock(ResourceLocation blockId) {
        Objects.requireNonNull(blockId, "blockId");
        return predicate(
                () -> Component.translatable("message.xlib.requirement_standing_on_block", displayReadableId(blockId)),
                (player, data) -> {
                    if (player == null) return false;
                    net.minecraft.world.level.block.state.BlockState state =
                            player.level().getBlockState(player.blockPosition().below());
                    ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
                    return blockId.equals(id);
                }
        );
    }

    public static AbilityRequirement standingOnBlockTag(ResourceLocation tagId) {
        Objects.requireNonNull(tagId, "tagId");
        TagKey<Block> tag = TagKey.create(Registries.BLOCK, tagId);
        return predicate(
                () -> Component.translatable("message.xlib.requirement_standing_on_block_tag", displayReadableId(tagId)),
                (player, data) -> player != null
                        && player.level().getBlockState(player.blockPosition().below()).is(tag)
        );
    }

    // -------------------------------------------------------------------------
    // Extended item holding predicates
    // -------------------------------------------------------------------------

    public static AbilityRequirement holdingAny(List<ItemLike> items) {
        List<Item> resolved = items.stream().map(i -> Objects.requireNonNull(i, "item").asItem()).toList();
        return predicate(
                () -> Component.translatable("message.xlib.requirement_holding_any"),
                (player, data) -> player != null && resolved.stream()
                        .anyMatch(item -> player.getMainHandItem().is(item) || player.getOffhandItem().is(item))
        );
    }

    public static AbilityRequirement holdingTag(ResourceLocation tagId) {
        Objects.requireNonNull(tagId, "tagId");
        TagKey<Item> tag = TagKey.create(Registries.ITEM, tagId);
        return predicate(
                () -> Component.translatable("message.xlib.requirement_holding_tag", displayReadableId(tagId)),
                (player, data) -> player != null
                        && (player.getMainHandItem().is(tag) || player.getOffhandItem().is(tag))
        );
    }

    public static AbilityRequirement holdingAnyTags(List<ResourceLocation> tagIds) {
        Objects.requireNonNull(tagIds, "tagIds");
        List<TagKey<Item>> tags = tagIds.stream().map(id -> TagKey.create(Registries.ITEM, id)).toList();
        return predicate(
                () -> Component.translatable("message.xlib.requirement_holding_any_tag"),
                (player, data) -> player != null && tags.stream()
                        .anyMatch(tag -> player.getMainHandItem().is(tag) || player.getOffhandItem().is(tag))
        );
    }

    public static AbilityRequirement holdingAllTags(List<ResourceLocation> tagIds) {
        Objects.requireNonNull(tagIds, "tagIds");
        List<TagKey<Item>> tags = tagIds.stream().map(id -> TagKey.create(Registries.ITEM, id)).toList();
        return predicate(
                () -> Component.translatable("message.xlib.requirement_holding_all_tags"),
                (player, data) -> {
                    if (player == null) return false;
                    return tags.stream().allMatch(tag ->
                            player.getMainHandItem().is(tag) || player.getOffhandItem().is(tag));
                }
        );
    }

    // -------------------------------------------------------------------------
    // Extended item wearing predicates
    // -------------------------------------------------------------------------

    public static AbilityRequirement wearingAny(List<ItemLike> items) {
        List<Item> resolved = items.stream().map(i -> Objects.requireNonNull(i, "item").asItem()).toList();
        return predicate(
                () -> Component.translatable("message.xlib.requirement_wearing_any"),
                (player, data) -> player != null
                        && player.getInventory().armor.stream().anyMatch(stack -> resolved.stream().anyMatch(stack::is))
        );
    }

    public static AbilityRequirement wearingAll(List<ItemLike> items) {
        List<Item> resolved = items.stream().map(i -> Objects.requireNonNull(i, "item").asItem()).toList();
        return predicate(
                () -> Component.translatable("message.xlib.requirement_wearing_all"),
                (player, data) -> {
                    if (player == null) return false;
                    return resolved.stream().allMatch(item ->
                            player.getInventory().armor.stream().anyMatch(stack -> stack.is(item)));
                }
        );
    }

    public static AbilityRequirement wearingTag(ResourceLocation tagId) {
        Objects.requireNonNull(tagId, "tagId");
        TagKey<Item> tag = TagKey.create(Registries.ITEM, tagId);
        return predicate(
                () -> Component.translatable("message.xlib.requirement_wearing_tag", displayReadableId(tagId)),
                (player, data) -> player != null
                        && player.getInventory().armor.stream().anyMatch(stack -> stack.is(tag))
        );
    }

    public static AbilityRequirement wearingAnyTags(List<ResourceLocation> tagIds) {
        Objects.requireNonNull(tagIds, "tagIds");
        List<TagKey<Item>> tags = tagIds.stream().map(id -> TagKey.create(Registries.ITEM, id)).toList();
        return predicate(
                () -> Component.translatable("message.xlib.requirement_wearing_any_tag"),
                (player, data) -> player != null
                        && tags.stream().anyMatch(tag ->
                                player.getInventory().armor.stream().anyMatch(stack -> stack.is(tag)))
        );
    }

    public static AbilityRequirement wearingAllTags(List<ResourceLocation> tagIds) {
        Objects.requireNonNull(tagIds, "tagIds");
        List<TagKey<Item>> tags = tagIds.stream().map(id -> TagKey.create(Registries.ITEM, id)).toList();
        return predicate(
                () -> Component.translatable("message.xlib.requirement_wearing_all_tags"),
                (player, data) -> {
                    if (player == null) return false;
                    return tags.stream().allMatch(tag ->
                            player.getInventory().armor.stream().anyMatch(stack -> stack.is(tag)));
                }
        );
    }

    // -------------------------------------------------------------------------
    // Biome tag predicates
    // -------------------------------------------------------------------------

    public static AbilityRequirement biomeTag(ResourceLocation tagId) {
        Objects.requireNonNull(tagId, "tagId");
        net.minecraft.tags.TagKey<net.minecraft.world.level.biome.Biome> tag =
                TagKey.create(Registries.BIOME, tagId);
        return predicate(
                () -> Component.translatable("message.xlib.requirement_biome_tag", displayReadableId(tagId)),
                (player, data) -> player != null && player.level().getBiome(player.blockPosition()).is(tag)
        );
    }

    public static AbilityRequirement notBiomeTag(ResourceLocation tagId) {
        Objects.requireNonNull(tagId, "tagId");
        net.minecraft.tags.TagKey<net.minecraft.world.level.biome.Biome> tag =
                TagKey.create(Registries.BIOME, tagId);
        return predicate(
                () -> Component.translatable("message.xlib.requirement_not_biome_tag", displayReadableId(tagId)),
                (player, data) -> player != null && !player.level().getBiome(player.blockPosition()).is(tag)
        );
    }

    // -------------------------------------------------------------------------
    // Weather predicates
    // -------------------------------------------------------------------------

    public static AbilityRequirement raining() {
        return predicate(() -> Component.translatable("message.xlib.requirement_raining"),
                (player, data) -> player != null && player.level().isRaining());
    }

    public static AbilityRequirement thundering() {
        return predicate(() -> Component.translatable("message.xlib.requirement_thundering"),
                (player, data) -> player != null && player.level().isThundering());
    }

    public static AbilityRequirement clearWeather() {
        return predicate(() -> Component.translatable("message.xlib.requirement_clear_weather"),
                (player, data) -> player != null && !player.level().isRaining() && !player.level().isThundering());
    }

    // -------------------------------------------------------------------------
    // Time predicates
    // -------------------------------------------------------------------------

    public static AbilityRequirement daytime() {
        return predicate(() -> Component.translatable("message.xlib.requirement_daytime"),
                (player, data) -> player != null && player.level().isDay());
    }

    public static AbilityRequirement nighttime() {
        return predicate(() -> Component.translatable("message.xlib.requirement_nighttime"),
                (player, data) -> player != null && player.level().isNight());
    }

    public static AbilityRequirement timeBetween(long start, long end) {
        return predicate(
                () -> Component.translatable("message.xlib.requirement_time_between", start, end),
                (player, data) -> {
                    if (player == null) return false;
                    long time = player.level().getDayTime() % 24000L;
                    if (start <= end) {
                        return time >= start && time <= end;
                    }
                    return time >= start || time <= end;
                }
        );
    }

    // -------------------------------------------------------------------------
    // Health / food / xp predicates
    // -------------------------------------------------------------------------

    public static AbilityRequirement healthAtLeast(double amount) {
        return predicate(
                () -> Component.translatable("message.xlib.requirement_health_at_least", amount),
                (player, data) -> player != null && player.getHealth() >= amount
        );
    }

    public static AbilityRequirement healthAtMost(double amount) {
        return predicate(
                () -> Component.translatable("message.xlib.requirement_health_at_most", amount),
                (player, data) -> player != null && player.getHealth() <= amount
        );
    }

    public static AbilityRequirement foodAtLeast(int amount) {
        return predicate(
                () -> Component.translatable("message.xlib.requirement_food_at_least", amount),
                (player, data) -> player != null && player.getFoodData().getFoodLevel() >= amount
        );
    }

    public static AbilityRequirement foodAtMost(int amount) {
        return predicate(
                () -> Component.translatable("message.xlib.requirement_food_at_most", amount),
                (player, data) -> player != null && player.getFoodData().getFoodLevel() <= amount
        );
    }

    public static AbilityRequirement xpLevelAtLeast(int level) {
        return predicate(
                () -> Component.translatable("message.xlib.requirement_xp_level_at_least", level),
                (player, data) -> player != null && player.experienceLevel >= level
        );
    }

    public static AbilityRequirement xpLevelAtMost(int level) {
        return predicate(
                () -> Component.translatable("message.xlib.requirement_xp_level_at_most", level),
                (player, data) -> player != null && player.experienceLevel <= level
        );
    }

    // -------------------------------------------------------------------------
    // Scoreboard predicates
    // -------------------------------------------------------------------------

    public static AbilityRequirement scoreAtLeast(String objective, int value) {
        Objects.requireNonNull(objective, "objective");
        return predicate(
                () -> Component.translatable("message.xlib.requirement_score_at_least", objective, value),
                (player, data) -> com.whatxe.xlib.ability.AbilityScoreboardApi.readScore(player, objective)
                        .stream().anyMatch(score -> score >= value)
        );
    }

    public static AbilityRequirement scoreAtMost(String objective, int value) {
        Objects.requireNonNull(objective, "objective");
        return predicate(
                () -> Component.translatable("message.xlib.requirement_score_at_most", objective, value),
                (player, data) -> com.whatxe.xlib.ability.AbilityScoreboardApi.readScore(player, objective)
                        .stream().anyMatch(score -> score <= value)
        );
    }

    public static AbilityRequirement scoreBetween(String objective, int min, int max) {
        Objects.requireNonNull(objective, "objective");
        return predicate(
                () -> Component.translatable("message.xlib.requirement_score_between", objective, min, max),
                (player, data) -> com.whatxe.xlib.ability.AbilityScoreboardApi.readScore(player, objective)
                        .stream().anyMatch(score -> score >= min && score <= max)
        );
    }

    // -------------------------------------------------------------------------
    // Progression counter predicates
    // -------------------------------------------------------------------------

    public static AbilityRequirement counterAtLeast(ResourceLocation counterId, int amount) {
        Objects.requireNonNull(counterId, "counterId");
        return predicate(
                () -> Component.translatable("message.xlib.requirement_counter_at_least", displayReadableId(counterId), amount),
                (player, data) -> player != null && UpgradeApi.counter(player, counterId) >= amount
        );
    }

    public static AbilityRequirement counterAtMost(ResourceLocation counterId, int amount) {
        Objects.requireNonNull(counterId, "counterId");
        return predicate(
                () -> Component.translatable("message.xlib.requirement_counter_at_most", displayReadableId(counterId), amount),
                (player, data) -> player != null && UpgradeApi.counter(player, counterId) <= amount
        );
    }

    public static AbilityRequirement counterBetween(ResourceLocation counterId, int min, int max) {
        Objects.requireNonNull(counterId, "counterId");
        return predicate(
                () -> Component.translatable("message.xlib.requirement_counter_between", displayReadableId(counterId), min, max),
                (player, data) -> {
                    if (player == null) return false;
                    int count = UpgradeApi.counter(player, counterId);
                    return count >= min && count <= max;
                }
        );
    }

    public static Optional<Component> firstFailure(
            Player player,
            AbilityData data,
            Collection<AbilityRequirement> requirements
    ) {
        for (AbilityRequirement requirement : requirements) {
            Optional<Component> failure = requirement.validate(player, data);
            if (failure.isPresent()) {
                return failure;
            }
        }
        return Optional.empty();
    }

    private static Component joinedDescriptions(Collection<AbilityRequirement> requirements) {
        MutableComponent joined = Component.empty();
        boolean first = true;
        for (AbilityRequirement requirement : requirements) {
            if (!first) {
                joined = joined.append(Component.literal(", "));
            }
            joined = joined.append(requirement.description());
            first = false;
        }
        return joined;
    }

    private static Component displayResourceName(ResourceLocation resourceId) {
        return AbilityApi.findResource(resourceId)
                .map(AbilityResourceDefinition::displayName)
                .orElse(displayReadableId(resourceId));
    }

    private static Component displayAbilityName(ResourceLocation abilityId) {
        return AbilityApi.findAbility(abilityId)
                .map(AbilityDefinition::displayName)
                .orElse(displayReadableId(abilityId));
    }

    private static Component displayPassiveName(ResourceLocation passiveId) {
        return PassiveApi.findPassive(passiveId)
                .map(PassiveDefinition::displayName)
                .orElse(displayReadableId(passiveId));
    }

    private static Component displayReadableId(ResourceLocation id) {
        return Component.literal(humanize(id.getPath()));
    }

    private static String humanize(String value) {
        StringBuilder builder = new StringBuilder(value.length());
        boolean capitalizeNext = true;
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == '_' || character == '/' || character == '-') {
                builder.append(' ');
                capitalizeNext = true;
                continue;
            }

            builder.append(capitalizeNext ? Character.toUpperCase(character) : character);
            capitalizeNext = false;
        }
        return builder.toString();
    }
}

