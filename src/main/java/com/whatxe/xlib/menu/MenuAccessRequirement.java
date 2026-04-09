package com.whatxe.xlib.menu;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public final class MenuAccessRequirement<D> {
    @FunctionalInterface
    public interface Evaluator<D> {
        Optional<Component> validate(@Nullable Player player, D data);
    }

    private final Supplier<Component> descriptionSupplier;
    private final Evaluator<D> evaluator;

    private MenuAccessRequirement(Supplier<Component> descriptionSupplier, Evaluator<D> evaluator) {
        this.descriptionSupplier = descriptionSupplier;
        this.evaluator = evaluator;
    }

    public static <D> MenuAccessRequirement<D> of(Component description, Evaluator<D> evaluator) {
        return of(() -> description, evaluator);
    }

    public static <D> MenuAccessRequirement<D> of(Supplier<Component> descriptionSupplier, Evaluator<D> evaluator) {
        return new MenuAccessRequirement<>(
                Objects.requireNonNull(descriptionSupplier, "descriptionSupplier"),
                Objects.requireNonNull(evaluator, "evaluator")
        );
    }

    public Component description() {
        return this.descriptionSupplier.get();
    }

    public Optional<Component> validate(@Nullable Player player, D data) {
        return this.evaluator.validate(player, data);
    }
}
