package com.whatxe.xlib.progression;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiPredicate;
import net.minecraft.network.chat.Component;
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
