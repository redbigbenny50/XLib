package com.whatxe.xlib.menu;

import com.whatxe.xlib.ability.AbilityData;
import com.whatxe.xlib.ability.AbilityRequirement;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public final class MenuAccessRequirements {
    private MenuAccessRequirements() {}

    public static <D> MenuAccessRequirement<D> predicate(
            Component description,
            BiPredicate<@Nullable Player, D> predicate
    ) {
        return predicate(() -> description, predicate);
    }

    public static <D> MenuAccessRequirement<D> predicate(
            Supplier<Component> descriptionSupplier,
            BiPredicate<@Nullable Player, D> predicate
    ) {
        Objects.requireNonNull(descriptionSupplier, "descriptionSupplier");
        Objects.requireNonNull(predicate, "predicate");
        return MenuAccessRequirement.of(descriptionSupplier, (player, data) -> predicate.test(player, data)
                ? Optional.empty()
                : Optional.of(descriptionSupplier.get()));
    }

    public static <D> MenuAccessRequirement<D> predicate(
            Component description,
            Component failure,
            BiPredicate<@Nullable Player, D> predicate
    ) {
        return predicate(() -> description, () -> failure, predicate);
    }

    public static <D> MenuAccessRequirement<D> predicate(
            Supplier<Component> descriptionSupplier,
            Supplier<Component> failureSupplier,
            BiPredicate<@Nullable Player, D> predicate
    ) {
        Objects.requireNonNull(descriptionSupplier, "descriptionSupplier");
        Objects.requireNonNull(failureSupplier, "failureSupplier");
        Objects.requireNonNull(predicate, "predicate");
        return MenuAccessRequirement.of(descriptionSupplier, (player, data) -> predicate.test(player, data)
                ? Optional.empty()
                : Optional.of(failureSupplier.get()));
    }

    public static MenuAccessRequirement<AbilityData> fromAbilityRequirement(AbilityRequirement requirement) {
        AbilityRequirement resolvedRequirement = Objects.requireNonNull(requirement, "requirement");
        return MenuAccessRequirement.of(resolvedRequirement::description, resolvedRequirement::validate);
    }

    public static <D> Optional<Component> firstFailure(
            @Nullable Player player,
            D data,
            Collection<MenuAccessRequirement<D>> requirements
    ) {
        for (MenuAccessRequirement<D> requirement : requirements) {
            Optional<Component> failure = requirement.validate(player, data);
            if (failure.isPresent()) {
                return failure;
            }
        }
        return Optional.empty();
    }
}
