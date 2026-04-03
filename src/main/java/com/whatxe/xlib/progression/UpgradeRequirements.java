package com.whatxe.xlib.progression;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiPredicate;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public final class UpgradeRequirements {
    private UpgradeRequirements() {}

    public static UpgradeRequirement predicate(
            Component description,
            BiPredicate<@Nullable ServerPlayer, UpgradeProgressData> predicate
    ) {
        Objects.requireNonNull(predicate, "predicate");
        return UpgradeRequirement.of(description, (player, data) -> predicate.test(player, data)
                ? Optional.empty()
                : Optional.of(description));
    }

    public static UpgradeRequirement advancement(ResourceLocation advancementId) {
        return advancement(advancementId, displayAdvancementName(advancementId));
    }

    public static UpgradeRequirement advancement(ResourceLocation advancementId, Component advancementName) {
        Objects.requireNonNull(advancementId, "advancementId");
        Component message = Component.translatable("message.xlib.upgrade.requirement_advancement", advancementName);
        return predicate(message, (player, data) -> player != null
                && player.server.getAdvancements().get(advancementId) != null
                && player.getAdvancements().getOrStartProgress(player.server.getAdvancements().get(advancementId)).isDone());
    }

    public static UpgradeRequirement counterAtLeast(ResourceLocation counterId, int amount) {
        Objects.requireNonNull(counterId, "counterId");
        Component message = Component.translatable("message.xlib.upgrade.requirement_counter", amount, displayCounterName(counterId));
        return predicate(message, (player, data) -> data.counter(counterId) >= amount);
    }

    public static UpgradeRequirement pointsAtLeast(ResourceLocation pointTypeId, int amount) {
        Objects.requireNonNull(pointTypeId, "pointTypeId");
        Component pointName = UpgradeApi.findPointType(pointTypeId)
                .map(UpgradePointType::displayName)
                .orElse(Component.literal(pointTypeId.toString()));
        Component message = Component.translatable("message.xlib.upgrade.requirement_points", amount, pointName);
        return predicate(message, (player, data) -> data.points(pointTypeId) >= amount);
    }

    public static UpgradeRequirement nodeUnlocked(ResourceLocation nodeId) {
        Objects.requireNonNull(nodeId, "nodeId");
        Component message = Component.translatable("message.xlib.upgrade.requirement_node", displayNodeName(nodeId));
        return predicate(message, (player, data) -> data.hasUnlockedNode(nodeId));
    }

    public static UpgradeRequirement all(UpgradeRequirement... requirements) {
        return all(java.util.List.of(requirements));
    }

    public static UpgradeRequirement all(Collection<UpgradeRequirement> requirements) {
        java.util.List<UpgradeRequirement> copied = java.util.List.copyOf(requirements);
        Component description = Component.translatable("message.xlib.upgrade.requirement_all", joinedDescriptions(copied));
        return UpgradeRequirement.of(description, (player, data) -> firstFailure(player, data, copied));
    }

    public static UpgradeRequirement any(UpgradeRequirement... requirements) {
        return any(java.util.List.of(requirements));
    }

    public static UpgradeRequirement any(Collection<UpgradeRequirement> requirements) {
        java.util.List<UpgradeRequirement> copied = java.util.List.copyOf(requirements);
        if (copied.isEmpty()) {
            throw new IllegalArgumentException("any(...) requires at least one requirement");
        }
        Component description = Component.translatable("message.xlib.upgrade.requirement_any", joinedDescriptions(copied));
        return UpgradeRequirement.of(description, (player, data) -> {
            for (UpgradeRequirement requirement : copied) {
                if (requirement.validate(player, data).isEmpty()) {
                    return Optional.empty();
                }
            }
            return Optional.of(description);
        });
    }

    public static UpgradeRequirement trackCompleted(ResourceLocation trackId) {
        Objects.requireNonNull(trackId, "trackId");
        Component message = Component.translatable("message.xlib.upgrade.requirement_track", UpgradeApi.displayTrackName(trackId));
        return predicate(message, (player, data) -> UpgradeApi.trackCompleted(data, trackId));
    }

    public static UpgradeRequirement anyNodeUnlocked(ResourceLocation... nodeIds) {
        return anyNodeUnlocked(java.util.List.of(nodeIds));
    }

    public static UpgradeRequirement anyNodeUnlocked(Collection<ResourceLocation> nodeIds) {
        java.util.List<ResourceLocation> copied = java.util.List.copyOf(nodeIds);
        if (copied.isEmpty()) {
            throw new IllegalArgumentException("anyNodeUnlocked(...) requires at least one node id");
        }
        Component message = Component.translatable("message.xlib.upgrade.requirement_any_node", joinedNodeNames(copied));
        return predicate(message, (player, data) -> copied.stream().anyMatch(data::hasUnlockedNode));
    }

    public static Optional<Component> firstFailure(
            @Nullable ServerPlayer player,
            UpgradeProgressData data,
            Collection<UpgradeRequirement> requirements
    ) {
        for (UpgradeRequirement requirement : requirements) {
            Optional<Component> failure = requirement.validate(player, data);
            if (failure.isPresent()) {
                return failure;
            }
        }
        return Optional.empty();
    }

    static Component displayNodeName(ResourceLocation nodeId) {
        return UpgradeApi.findNode(nodeId)
                .map(UpgradeNodeDefinition::displayName)
                .orElse(Component.literal(nodeId.toString()));
    }

    public static Component displayAdvancementName(ResourceLocation advancementId) {
        return Component.literal(humanize(advancementId));
    }

    public static Component displayCounterName(ResourceLocation counterId) {
        return Component.literal(humanize(counterId));
    }

    private static Component joinedDescriptions(Collection<UpgradeRequirement> requirements) {
        MutableComponent joined = Component.empty();
        boolean first = true;
        for (UpgradeRequirement requirement : requirements) {
            if (!first) {
                joined = joined.append(Component.literal(", "));
            }
            joined = joined.append(requirement.description());
            first = false;
        }
        return joined;
    }

    private static Component joinedNodeNames(Collection<ResourceLocation> nodeIds) {
        MutableComponent joined = Component.empty();
        boolean first = true;
        for (ResourceLocation nodeId : nodeIds) {
            if (!first) {
                joined = joined.append(Component.literal(", "));
            }
            joined = joined.append(displayNodeName(nodeId));
            first = false;
        }
        return joined;
    }

    private static String humanize(ResourceLocation id) {
        String path = id.getPath();
        if (path.startsWith("demo_")) {
            path = path.substring("demo_".length());
        }

        StringBuilder builder = new StringBuilder(path.length());
        boolean capitalizeNext = true;
        for (int index = 0; index < path.length(); index++) {
            char character = path.charAt(index);
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
