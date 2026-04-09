package com.whatxe.xlib.menu;

import com.whatxe.xlib.progression.UpgradeProgressData;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public final class ProgressionMenuAccessPolicy {
    private static final ProgressionMenuAccessPolicy ALLOW_ALL = new Builder().build();

    private final List<MenuAccessRequirement<UpgradeProgressData>> visibilityRequirements;
    private final List<MenuAccessRequirement<UpgradeProgressData>> availabilityRequirements;

    private ProgressionMenuAccessPolicy(
            List<MenuAccessRequirement<UpgradeProgressData>> visibilityRequirements,
            List<MenuAccessRequirement<UpgradeProgressData>> availabilityRequirements
    ) {
        this.visibilityRequirements = List.copyOf(visibilityRequirements);
        this.availabilityRequirements = List.copyOf(availabilityRequirements);
    }

    public static ProgressionMenuAccessPolicy allowAll() {
        return ALLOW_ALL;
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<MenuAccessRequirement<UpgradeProgressData>> visibilityRequirements() {
        return this.visibilityRequirements;
    }

    public List<MenuAccessRequirement<UpgradeProgressData>> availabilityRequirements() {
        return this.availabilityRequirements;
    }

    public MenuAccessDecision evaluate(@Nullable Player player, UpgradeProgressData data) {
        return MenuAccessRequirements.firstFailure(player, data, this.visibilityRequirements)
                .map(MenuAccessDecision::hidden)
                .orElseGet(() -> MenuAccessRequirements.firstFailure(player, data, this.availabilityRequirements)
                        .map(MenuAccessDecision::locked)
                        .orElseGet(MenuAccessDecision::available));
    }

    public static final class Builder {
        private final List<MenuAccessRequirement<UpgradeProgressData>> visibilityRequirements = new ArrayList<>();
        private final List<MenuAccessRequirement<UpgradeProgressData>> availabilityRequirements = new ArrayList<>();

        public Builder visibleWhen(MenuAccessRequirement<UpgradeProgressData> requirement) {
            this.visibilityRequirements.add(Objects.requireNonNull(requirement, "requirement"));
            return this;
        }

        public Builder availableWhen(MenuAccessRequirement<UpgradeProgressData> requirement) {
            this.availabilityRequirements.add(Objects.requireNonNull(requirement, "requirement"));
            return this;
        }

        public ProgressionMenuAccessPolicy build() {
            return new ProgressionMenuAccessPolicy(this.visibilityRequirements, this.availabilityRequirements);
        }
    }
}
