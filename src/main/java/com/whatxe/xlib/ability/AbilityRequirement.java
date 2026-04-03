package com.whatxe.xlib.ability;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public final class AbilityRequirement {
    @FunctionalInterface
    public interface Evaluator {
        Optional<Component> validate(Player player, AbilityData data);
    }

    private final Supplier<Component> descriptionSupplier;
    private final Evaluator evaluator;

    private AbilityRequirement(Supplier<Component> descriptionSupplier, Evaluator evaluator) {
        this.descriptionSupplier = descriptionSupplier;
        this.evaluator = evaluator;
    }

    public static AbilityRequirement of(Component description, Evaluator evaluator) {
        return of(() -> description, evaluator);
    }

    public static AbilityRequirement of(Supplier<Component> descriptionSupplier, Evaluator evaluator) {
        return new AbilityRequirement(
                Objects.requireNonNull(descriptionSupplier, "descriptionSupplier"),
                Objects.requireNonNull(evaluator, "evaluator")
        );
    }

    public Component description() {
        return this.descriptionSupplier.get();
    }

    public Optional<Component> validate(Player player, AbilityData data) {
        return this.evaluator.validate(player, data);
    }
}

