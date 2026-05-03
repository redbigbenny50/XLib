package com.whatxe.xlib.ability;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;

public final class BuiltInContextProviders {
    private BuiltInContextProviders() {}

    public static ContextGrantProvider dimensionPack(
            ResourceLocation providerId,
            ResourceLocation sourceId,
            ResourceLocation dimensionId,
            Consumer<SimpleContextGrantProvider.Builder> consumer
    ) {
        return build(providerId, sourceId, ContextGrantConditions.dimension(dimensionId), consumer);
    }

    public static ContextGrantProvider biomePack(
            ResourceLocation providerId,
            ResourceLocation sourceId,
            ResourceLocation biomeId,
            Consumer<SimpleContextGrantProvider.Builder> consumer
    ) {
        return build(providerId, sourceId, ContextGrantConditions.biome(biomeId), consumer);
    }

    public static ContextGrantProvider statusEffectPack(
            ResourceLocation providerId,
            ResourceLocation sourceId,
            ResourceLocation effectId,
            Consumer<SimpleContextGrantProvider.Builder> consumer
    ) {
        return build(providerId, sourceId, ContextGrantConditions.hasStatusEffect(effectId), consumer);
    }

    public static ContextGrantProvider teamPack(
            ResourceLocation providerId,
            ResourceLocation sourceId,
            String teamName,
            Consumer<SimpleContextGrantProvider.Builder> consumer
    ) {
        return build(providerId, sourceId, ContextGrantConditions.team(teamName), consumer);
    }

    public static ContextGrantProvider modePack(
            ResourceLocation providerId,
            ResourceLocation sourceId,
            ResourceLocation modeAbilityId,
            Consumer<SimpleContextGrantProvider.Builder> consumer
    ) {
        return build(providerId, sourceId, ContextGrantConditions.modeActive(modeAbilityId), consumer);
    }

    public static ContextGrantProvider armorSetPack(
            ResourceLocation providerId,
            ResourceLocation sourceId,
            Collection<? extends ItemLike> requiredArmor,
            Consumer<SimpleContextGrantProvider.Builder> consumer
    ) {
        return build(providerId, sourceId, ContextGrantConditions.wearingAll(requiredArmor), consumer);
    }

    private static ContextGrantProvider build(
            ResourceLocation providerId,
            ResourceLocation sourceId,
            ContextGrantCondition condition,
            Consumer<SimpleContextGrantProvider.Builder> consumer
    ) {
        SimpleContextGrantProvider.Builder builder = SimpleContextGrantProvider.builder(
                Objects.requireNonNull(providerId, "providerId"),
                Objects.requireNonNull(sourceId, "sourceId")
        ).when(Objects.requireNonNull(condition, "condition"));
        Objects.requireNonNull(consumer, "consumer").accept(builder);
        return builder.build();
    }
}
