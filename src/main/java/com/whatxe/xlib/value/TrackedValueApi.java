package com.whatxe.xlib.value;

import com.whatxe.xlib.XLibRegistryGuard;
import com.whatxe.xlib.attachment.ModAttachments;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;

public final class TrackedValueApi {
    private static final Map<ResourceLocation, TrackedValueDefinition> DEFINITIONS = new LinkedHashMap<>();

    private TrackedValueApi() {}

    public static void bootstrap() {}

    public static TrackedValueDefinition register(TrackedValueDefinition definition) {
        XLibRegistryGuard.ensureMutable("tracked_values");
        TrackedValueDefinition previous = DEFINITIONS.putIfAbsent(definition.id(), definition);
        if (previous != null) {
            throw new IllegalStateException("Duplicate tracked value registration: " + definition.id());
        }
        return definition;
    }

    public static Optional<TrackedValueDefinition> unregister(ResourceLocation valueId) {
        XLibRegistryGuard.ensureMutable("tracked_values");
        return Optional.ofNullable(DEFINITIONS.remove(valueId));
    }

    public static Optional<TrackedValueDefinition> findDefinition(ResourceLocation valueId) {
        TrackedValueDefinition definition = DEFINITIONS.get(valueId);
        if (definition != null) {
            return Optional.of(definition);
        }
        return DataDrivenTrackedValueApi.findDefinition(valueId)
                .map(DataDrivenTrackedValueApi.LoadedTrackedValueDefinition::definition);
    }

    public static Collection<TrackedValueDefinition> allDefinitions() {
        return List.copyOf(resolvedDefinitions().values());
    }

    public static double value(Player player, ResourceLocation valueId) {
        return value(getData(player), valueId);
    }

    public static double value(TrackedValueData data, ResourceLocation valueId) {
        Objects.requireNonNull(data, "data");
        Objects.requireNonNull(valueId, "valueId");
        if (data.hasStoredAmount(valueId)) {
            return data.exactAmount(valueId);
        }
        return findDefinition(valueId).map(TrackedValueDefinition::startingValue).orElse(0.0D);
    }

    public static boolean hasFoodReplacement(Player player) {
        return activeFoodReplacementValueId(getData(player)).isPresent();
    }

    public static Optional<ResourceLocation> activeFoodReplacementValueId(Player player) {
        return activeFoodReplacementValueId(getData(player));
    }

    public static Optional<ResourceLocation> activeFoodReplacementValueId(TrackedValueData data) {
        LinkedHashSet<ResourceLocation> candidateValueIds = new LinkedHashSet<>(data.activeFoodReplacementValueIds());
        for (ResourceLocation valueId : data.storedValueIds()) {
            if (findDefinition(valueId).map(TrackedValueDefinition::foodReplacementPriority).orElse(0) > 0) {
                candidateValueIds.add(valueId);
            }
        }
        return candidateValueIds.stream()
                .filter(valueId -> findDefinition(valueId)
                        .map(definition -> definition.foodReplacementPriority() > 0 || data.hasFoodReplacement(valueId))
                        .orElse(false))
                .sorted((left, right) -> {
                    int priorityCompare = Integer.compare(
                            findDefinition(right).map(TrackedValueDefinition::foodReplacementPriority).orElse(0),
                            findDefinition(left).map(TrackedValueDefinition::foodReplacementPriority).orElse(0)
                    );
                    return priorityCompare != 0 ? priorityCompare : left.toString().compareTo(right.toString());
                })
                .findFirst();
    }

    public static Optional<TrackedValueDefinition> activeFoodReplacement(Player player) {
        return activeFoodReplacementValueId(player).flatMap(TrackedValueApi::findDefinition);
    }

    public static int effectiveFoodLevel(Player player) {
        Optional<ResourceLocation> activeValueId = activeFoodReplacementValueId(player);
        if (activeValueId.isEmpty()) {
            return player.getFoodData().getFoodLevel();
        }
        TrackedValueDefinition definition = findDefinition(activeValueId.get()).orElse(null);
        if (definition == null) {
            return player.getFoodData().getFoodLevel();
        }
        return definition.foodReplacementLevel(value(player, activeValueId.get()));
    }

    public static TrackedValueData sanitize(TrackedValueData data) {
        Map<ResourceLocation, TrackedValueDefinition> definitions = resolvedDefinitions();
        Map<ResourceLocation, Integer> sanitizedWholes = new LinkedHashMap<>();
        Map<ResourceLocation, Integer> sanitizedPartials = new LinkedHashMap<>();
        for (ResourceLocation valueId : data.storedValueIds()) {
            TrackedValueDefinition definition = definitions.get(valueId);
            if (definition == null) {
                continue;
            }
            double clampedAmount = definition.clamp(data.exactAmount(valueId));
            int whole = (int) Math.floor(clampedAmount + 1.0E-9D);
            int partial = (int) Math.round((clampedAmount - whole) * 1000.0D);
            if (partial >= 1000) {
                whole += 1;
                partial -= 1000;
            }
            sanitizedWholes.put(valueId, whole);
            if (partial > 0) {
                sanitizedPartials.put(valueId, partial);
            }
        }

        Map<ResourceLocation, Set<ResourceLocation>> sanitizedFoodReplacementSources = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, Set<ResourceLocation>> entry : data.foodReplacementSources().entrySet()) {
            if (!definitions.containsKey(entry.getKey()) || entry.getValue().isEmpty()) {
                continue;
            }
            sanitizedFoodReplacementSources.put(entry.getKey(), entry.getValue());
        }

        return new TrackedValueData(
                Map.copyOf(sanitizedWholes),
                Map.copyOf(sanitizedPartials),
                Map.copyOf(sanitizedFoodReplacementSources)
        );
    }

    public static void setValueExact(Player player, ResourceLocation valueId, double amount) {
        TrackedValueDefinition definition = findDefinition(valueId).orElse(null);
        if (definition == null) {
            return;
        }
        double clampedAmount = definition.clamp(amount);
        TrackedValueData currentData = getData(player);
        TrackedValueData updatedData = currentData.withExactAmount(valueId, clampedAmount);
        if (!updatedData.equals(currentData)) {
            setData(player, updatedData);
        }
    }

    public static void addValueExact(Player player, ResourceLocation valueId, double delta) {
        setValueExact(player, valueId, value(player, valueId) + delta);
    }

    public static void bindFoodReplacement(Player player, ResourceLocation valueId, ResourceLocation sourceId) {
        if (!DEFINITIONS.containsKey(valueId)) {
            return;
        }
        TrackedValueData currentData = getData(player);
        TrackedValueData updatedData = currentData;
        if (!updatedData.hasStoredAmount(valueId)) {
            updatedData = updatedData.withExactAmount(valueId, value(currentData, valueId));
        }
        updatedData = updatedData.withFoodReplacementSource(valueId, sourceId, true);
        if (!updatedData.equals(currentData)) {
            setData(player, updatedData);
        }
    }

    public static void revokeFoodReplacement(Player player, ResourceLocation valueId, ResourceLocation sourceId) {
        TrackedValueData currentData = getData(player);
        TrackedValueData updatedData = currentData.withFoodReplacementSource(valueId, sourceId, false);
        if (!updatedData.equals(currentData)) {
            setData(player, updatedData);
        }
    }

    public static void clearSource(Player player, ResourceLocation sourceId) {
        TrackedValueData currentData = getData(player);
        TrackedValueData updatedData = currentData.clearFoodReplacementSource(sourceId);
        if (!updatedData.equals(currentData)) {
            setData(player, updatedData);
        }
    }

    public static TrackedValueData tick(TrackedValueData currentData) {
        TrackedValueData updatedData = currentData;
        for (TrackedValueDefinition definition : resolvedDefinitions().values()) {
            if (Math.abs(definition.tickDelta()) < 1.0E-9D) {
                continue;
            }
            double currentAmount = value(updatedData, definition.id());
            double nextAmount = definition.clamp(currentAmount + definition.tickDelta());
            if (Math.abs(nextAmount - currentAmount) > 1.0E-9D || !updatedData.hasStoredAmount(definition.id())) {
                updatedData = updatedData.withExactAmount(definition.id(), nextAmount);
            }
        }
        return sanitize(updatedData);
    }

    public static void applyVanillaFoodSuppression(ServerPlayer player) {
        if (!hasFoodReplacement(player)) {
            return;
        }
        FoodData foodData = player.getFoodData();
        foodData.setFoodLevel(17);
        foodData.setSaturation(0.0F);
        foodData.setExhaustion(0.0F);
    }

    public static void onFoodConsumed(ServerPlayer player, ItemStack usedStack) {
        Optional<ResourceLocation> activeValueId = activeFoodReplacementValueId(player);
        if (activeValueId.isEmpty()) {
            return;
        }
        TrackedValueDefinition definition = findDefinition(activeValueId.get()).orElse(null);
        var foodProperties = usedStack.get(net.minecraft.core.component.DataComponents.FOOD);
        if (definition == null || foodProperties == null || definition.foodReplacementIntakeScale() <= 0.0D) {
            return;
        }
        addValueExact(player, activeValueId.get(), foodProperties.nutrition() * definition.foodReplacementIntakeScale());
    }

    public static void tickFoodReplacementSurvival(ServerPlayer player) {
        Optional<ResourceLocation> activeValueId = activeFoodReplacementValueId(player);
        if (activeValueId.isEmpty()) {
            return;
        }
        TrackedValueDefinition definition = findDefinition(activeValueId.get()).orElse(null);
        if (definition == null) {
            return;
        }

        int effectiveFoodLevel = definition.foodReplacementLevel(value(player, activeValueId.get()));
        if (player.isHurt()
                && effectiveFoodLevel >= definition.foodReplacementHealThreshold()
                && player.tickCount % definition.foodReplacementHealIntervalTicks() == 0) {
            player.heal(1.0F);
            if (definition.foodReplacementHealCost() > 0.0D) {
                addValueExact(player, activeValueId.get(), -definition.foodReplacementHealCost());
            }
        }

        if (effectiveFoodLevel <= definition.foodReplacementStarvationThreshold()
                && player.tickCount % definition.foodReplacementStarvationIntervalTicks() == 0
                && canTakeTrackedStarvation(player)) {
            player.hurt(player.damageSources().starve(), definition.foodReplacementStarvationDamage());
        }
    }

    public static TrackedValueData getData(Player player) {
        return player.getData(ModAttachments.PLAYER_TRACKED_VALUES);
    }

    public static void setData(Player player, TrackedValueData data) {
        player.setData(ModAttachments.PLAYER_TRACKED_VALUES, sanitize(data));
    }

    private static Map<ResourceLocation, TrackedValueDefinition> resolvedDefinitions() {
        Map<ResourceLocation, TrackedValueDefinition> definitions = new LinkedHashMap<>(DEFINITIONS);
        for (DataDrivenTrackedValueApi.LoadedTrackedValueDefinition definition : DataDrivenTrackedValueApi.allDefinitionIds().stream()
                .map(DataDrivenTrackedValueApi::findDefinition)
                .flatMap(Optional::stream)
                .toList()) {
            definitions.putIfAbsent(definition.id(), definition.definition());
        }
        return Map.copyOf(definitions);
    }

    private static boolean canTakeTrackedStarvation(ServerPlayer player) {
        Difficulty difficulty = player.level().getDifficulty();
        if (difficulty == Difficulty.PEACEFUL) {
            return false;
        }
        return switch (difficulty) {
            case EASY -> player.getHealth() > 10.0F;
            case NORMAL -> player.getHealth() > 1.0F;
            case HARD -> true;
            default -> false;
        };
    }
}
