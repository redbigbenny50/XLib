package com.whatxe.xlib.ability;

import com.whatxe.xlib.attachment.ModAttachments;
import com.whatxe.xlib.combat.CombatMarkState;
import com.whatxe.xlib.combat.CombatReactionApi;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;

public final class AbilityRequirements {
    private AbilityRequirements() {}

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

