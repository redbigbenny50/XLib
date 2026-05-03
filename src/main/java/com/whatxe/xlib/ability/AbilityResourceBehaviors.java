package com.whatxe.xlib.ability;

import com.whatxe.xlib.classification.EntityClassificationApi;
import java.util.Objects;
import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;

public final class AbilityResourceBehaviors {
    private AbilityResourceBehaviors() {}

    public static AbilityResourceBehavior drainWhileSprinting(int amount, int intervalTicks) {
        return conditionalTick(
                intervalTicks,
                player -> player.isSprinting(),
                (player, data, resource) -> AbilityResourceApi.addAmount(data, resource.id(), -amount)
        );
    }

    public static AbilityResourceBehavior refillOnKill(int amount) {
        return new AbilityResourceBehavior() {
            @Override
            public AbilityData onKill(ServerPlayer player, LivingEntity target, AbilityData data, AbilityResourceDefinition resource) {
                return AbilityResourceApi.addAmount(data, resource.id(), amount);
            }
        };
    }

    public static AbilityResourceBehavior refillOnKillMatchingEntity(ResourceLocation entityTypeId, int amount) {
        Objects.requireNonNull(entityTypeId, "entityTypeId");
        return new AbilityResourceBehavior() {
            @Override
            public AbilityData onKill(ServerPlayer player, LivingEntity target, AbilityData data, AbilityResourceDefinition resource) {
                return EntityClassificationApi.matchesSelector(target, java.util.Set.of(entityTypeId), java.util.Set.of())
                        ? AbilityResourceApi.addAmount(data, resource.id(), amount)
                        : data;
            }
        };
    }

    public static AbilityResourceBehavior refillOnKillMatchingTag(ResourceLocation tagId, int amount) {
        Objects.requireNonNull(tagId, "tagId");
        return new AbilityResourceBehavior() {
            @Override
            public AbilityData onKill(ServerPlayer player, LivingEntity target, AbilityData data, AbilityResourceDefinition resource) {
                return EntityClassificationApi.matchesSelector(target, java.util.Set.of(), java.util.Set.of(tagId))
                        ? AbilityResourceApi.addAmount(data, resource.id(), amount)
                        : data;
            }
        };
    }

    public static AbilityResourceBehavior refillOnEat(int amount) {
        return refillOnEat(stack -> stack.has(DataComponents.FOOD), amount);
    }

    public static AbilityResourceBehavior refillOnEat(Predicate<ItemStack> itemPredicate, int amount) {
        Objects.requireNonNull(itemPredicate, "itemPredicate");
        return new AbilityResourceBehavior() {
            @Override
            public AbilityData onEat(ServerPlayer player, ItemStack stack, AbilityData data, AbilityResourceDefinition resource) {
                return itemPredicate.test(stack) ? AbilityResourceApi.addAmount(data, resource.id(), amount) : data;
            }
        };
    }

    public static AbilityResourceBehavior refillOverTimeWhen(
            Predicate<ServerPlayer> playerPredicate,
            int amount,
            int intervalTicks
    ) {
        Objects.requireNonNull(playerPredicate, "playerPredicate");
        return conditionalTick(intervalTicks, playerPredicate, (player, data, resource) -> AbilityResourceApi.addAmount(data, resource.id(), amount));
    }

    public static AbilityResourceBehavior refillOverTimeExactWhen(
            Predicate<ServerPlayer> playerPredicate,
            double amount,
            int intervalTicks
    ) {
        Objects.requireNonNull(playerPredicate, "playerPredicate");
        return conditionalTick(intervalTicks, playerPredicate, (player, data, resource) -> AbilityResourceApi.addAmountExact(data, resource.id(), amount));
    }

    public static AbilityResourceBehavior refillInBiome(TagKey<Biome> biomeTag, int amount, int intervalTicks) {
        Objects.requireNonNull(biomeTag, "biomeTag");
        return new AbilityResourceBehavior() {
            @Override
            public AbilityData onBiome(ServerPlayer player, Holder<Biome> biome, AbilityData data, AbilityResourceDefinition resource) {
                if (intervalTicks > 1 && player.tickCount % intervalTicks != 0) {
                    return data;
                }
                return biome.is(biomeTag) ? AbilityResourceApi.addAmount(data, resource.id(), amount) : data;
            }
        };
    }

    public static AbilityResourceBehavior decay(int amount, int delayTicks, int intervalTicks) {
        return new AbilityResourceBehavior() {
            @Override
            public AbilityData onTick(ServerPlayer player, AbilityData data, AbilityResourceDefinition resource) {
                if (data.resourceAmountExact(resource.id()) <= 0.0D) {
                    return data.withResourceDecayDelay(resource.id(), 0);
                }
                int currentDelay = data.resourceDecayDelay(resource.id());
                if (currentDelay <= 0) {
                    currentDelay = delayTicks;
                }
                if (currentDelay > 1) {
                    return data.withResourceDecayDelay(resource.id(), currentDelay - 1);
                }
                if (intervalTicks > 1 && player.tickCount % intervalTicks != 0) {
                    return data;
                }
                AbilityData updatedData = AbilityResourceApi.addAmount(data, resource.id(), -amount);
                return updatedData.withResourceDecayDelay(resource.id(), delayTicks);
            }
        };
    }

    public static AbilityResourceBehavior decayExact(double amount, int delayTicks, int intervalTicks) {
        return new AbilityResourceBehavior() {
            @Override
            public AbilityData onTick(ServerPlayer player, AbilityData data, AbilityResourceDefinition resource) {
                if (data.resourceAmountExact(resource.id()) <= 0.0D) {
                    return data.withResourceDecayDelay(resource.id(), 0);
                }
                int currentDelay = data.resourceDecayDelay(resource.id());
                if (currentDelay <= 0) {
                    currentDelay = delayTicks;
                }
                if (currentDelay > 1) {
                    return data.withResourceDecayDelay(resource.id(), currentDelay - 1);
                }
                if (intervalTicks > 1 && player.tickCount % intervalTicks != 0) {
                    return data;
                }
                AbilityData updatedData = AbilityResourceApi.addAmountExact(data, resource.id(), -amount);
                return updatedData.withResourceDecayDelay(resource.id(), delayTicks);
            }
        };
    }

    public static AbilityResourceBehavior temporaryShield(int decayAmount, int delayTicks, int intervalTicks) {
        AbilityResourceBehavior decay = decay(decayAmount, delayTicks, intervalTicks);
        return new AbilityResourceBehavior() {
            @Override
            public AbilityData onTick(ServerPlayer player, AbilityData data, AbilityResourceDefinition resource) {
                return decay.onTick(player, data, resource);
            }

            @Override
            public AbilityData onIncomingDamage(
                    ServerPlayer player,
                    DamageSource source,
                    float amount,
                    AbilityData data,
                    AbilityResourceDefinition resource
            ) {
                return data.withResourceDecayDelay(resource.id(), delayTicks);
            }
        };
    }

    public static AbilityResourceBehavior conditionalTick(
            int intervalTicks,
            Predicate<ServerPlayer> playerPredicate,
            ResourceChange change
    ) {
        Objects.requireNonNull(playerPredicate, "playerPredicate");
        Objects.requireNonNull(change, "change");
        return new AbilityResourceBehavior() {
            @Override
            public AbilityData onTick(ServerPlayer player, AbilityData data, AbilityResourceDefinition resource) {
                if (intervalTicks > 1 && player.tickCount % intervalTicks != 0) {
                    return data;
                }
                return playerPredicate.test(player) ? change.apply(player, data, resource) : data;
            }
        };
    }

    @FunctionalInterface
    public interface ResourceChange {
        AbilityData apply(ServerPlayer player, AbilityData data, AbilityResourceDefinition resource);
    }
}

