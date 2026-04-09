package com.whatxe.xlib.menu;

import com.whatxe.xlib.ability.AbilityData;
import com.whatxe.xlib.ability.AbilityRequirement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public final class AbilityMenuAccessPolicy {
    private static final AbilityMenuAccessPolicy ALLOW_ALL = new Builder().build();

    private final List<MenuAccessRequirement<AbilityData>> visibilityRequirements;
    private final List<MenuAccessRequirement<AbilityData>> availabilityRequirements;

    private AbilityMenuAccessPolicy(
            List<MenuAccessRequirement<AbilityData>> visibilityRequirements,
            List<MenuAccessRequirement<AbilityData>> availabilityRequirements
    ) {
        this.visibilityRequirements = List.copyOf(visibilityRequirements);
        this.availabilityRequirements = List.copyOf(availabilityRequirements);
    }

    public static AbilityMenuAccessPolicy allowAll() {
        return ALLOW_ALL;
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<MenuAccessRequirement<AbilityData>> visibilityRequirements() {
        return this.visibilityRequirements;
    }

    public List<MenuAccessRequirement<AbilityData>> availabilityRequirements() {
        return this.availabilityRequirements;
    }

    public MenuAccessDecision evaluate(@Nullable Player player, AbilityData data) {
        return MenuAccessRequirements.firstFailure(player, data, this.visibilityRequirements)
                .map(MenuAccessDecision::hidden)
                .orElseGet(() -> MenuAccessRequirements.firstFailure(player, data, this.availabilityRequirements)
                        .map(MenuAccessDecision::locked)
                        .orElseGet(MenuAccessDecision::available));
    }

    public static final class Builder {
        private final List<MenuAccessRequirement<AbilityData>> visibilityRequirements = new ArrayList<>();
        private final List<MenuAccessRequirement<AbilityData>> availabilityRequirements = new ArrayList<>();

        public Builder visibleWhen(MenuAccessRequirement<AbilityData> requirement) {
            this.visibilityRequirements.add(Objects.requireNonNull(requirement, "requirement"));
            return this;
        }

        public Builder visibleWhen(AbilityRequirement requirement) {
            return visibleWhen(MenuAccessRequirements.fromAbilityRequirement(requirement));
        }

        public Builder availableWhen(MenuAccessRequirement<AbilityData> requirement) {
            this.availabilityRequirements.add(Objects.requireNonNull(requirement, "requirement"));
            return this;
        }

        public Builder availableWhen(AbilityRequirement requirement) {
            return availableWhen(MenuAccessRequirements.fromAbilityRequirement(requirement));
        }

        public AbilityMenuAccessPolicy build() {
            return new AbilityMenuAccessPolicy(this.visibilityRequirements, this.availabilityRequirements);
        }
    }
}
