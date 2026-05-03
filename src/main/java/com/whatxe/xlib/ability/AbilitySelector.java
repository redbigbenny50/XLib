package com.whatxe.xlib.ability;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public record AbilitySelector(AbilitySelectorType type, @Nullable ResourceLocation id) {
    public AbilitySelector {
        type = Objects.requireNonNull(type, "type");
        if (type == AbilitySelectorType.ALL) {
            id = null;
        } else {
            id = Objects.requireNonNull(id, "id");
        }
    }

    public static AbilitySelector ability(ResourceLocation abilityId) {
        return new AbilitySelector(AbilitySelectorType.ABILITY, abilityId);
    }

    public static AbilitySelector family(ResourceLocation familyId) {
        return new AbilitySelector(AbilitySelectorType.FAMILY, familyId);
    }

    public static AbilitySelector group(ResourceLocation groupId) {
        return new AbilitySelector(AbilitySelectorType.GROUP, groupId);
    }

    public static AbilitySelector page(ResourceLocation pageId) {
        return new AbilitySelector(AbilitySelectorType.PAGE, pageId);
    }

    public static AbilitySelector tag(ResourceLocation tagId) {
        return new AbilitySelector(AbilitySelectorType.TAG, tagId);
    }

    public static AbilitySelector allAbilities() {
        return new AbilitySelector(AbilitySelectorType.ALL, null);
    }

    public boolean matches(AbilityDefinition ability) {
        Objects.requireNonNull(ability, "ability");
        return switch (this.type) {
            case ABILITY -> ability.id().equals(this.id);
            case FAMILY -> ability.familyId().filter(this.id::equals).isPresent();
            case GROUP -> ability.groupId().filter(this.id::equals).isPresent();
            case PAGE -> ability.pageId().filter(this.id::equals).isPresent();
            case TAG -> ability.hasTag(this.id);
            case ALL -> true;
        };
    }

    public Collection<ResourceLocation> resolveAbilityIds() {
        return switch (this.type) {
            case ABILITY -> AbilityApi.findAbility(this.id).map(AbilityDefinition::id).stream().toList();
            case FAMILY -> AbilityApi.abilitiesInFamily(this.id).stream().map(AbilityDefinition::id).toList();
            case GROUP -> AbilityApi.abilitiesInGroup(this.id).stream().map(AbilityDefinition::id).toList();
            case PAGE -> AbilityApi.abilitiesOnPage(this.id).stream().map(AbilityDefinition::id).toList();
            case TAG -> AbilityApi.abilitiesWithTag(this.id).stream().map(AbilityDefinition::id).toList();
            case ALL -> AbilityApi.allAbilities().stream().map(AbilityDefinition::id).toList();
        };
    }

    public Optional<ResourceLocation> selectorId() {
        return Optional.ofNullable(this.id);
    }

    public String summary() {
        return switch (this.type) {
            case ABILITY -> "ability:" + this.id;
            case FAMILY -> "family:" + this.id;
            case GROUP -> "group:" + this.id;
            case PAGE -> "page:" + this.id;
            case TAG -> "tag:" + this.id;
            case ALL -> "all";
        };
    }

    public Set<ResourceLocation> resolveAbilityIdsSet() {
        return Set.copyOf(resolveAbilityIds());
    }
}
