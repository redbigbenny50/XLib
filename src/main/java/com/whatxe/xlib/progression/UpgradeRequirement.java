package com.whatxe.xlib.progression;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public final class UpgradeRequirement {
    @FunctionalInterface
    public interface Evaluator {
        Optional<Component> validate(@Nullable ServerPlayer player, UpgradeProgressData data);
    }

    private final Component description;
    private final Evaluator evaluator;

    private UpgradeRequirement(Component description, Evaluator evaluator) {
        this.description = description;
        this.evaluator = evaluator;
    }

    public static UpgradeRequirement of(Component description, Evaluator evaluator) {
        return new UpgradeRequirement(
                Objects.requireNonNull(description, "description"),
                Objects.requireNonNull(evaluator, "evaluator")
        );
    }

    public Component description() {
        return this.description;
    }

    public Optional<Component> validate(@Nullable ServerPlayer player, UpgradeProgressData data) {
        return this.evaluator.validate(player, data);
    }
}
