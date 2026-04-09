package com.whatxe.xlib.ability;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public record ProfileGroupDefinition(
        ResourceLocation id,
        Component displayName,
        Component description,
        @Nullable AbilityIcon icon,
        int selectionLimit,
        boolean requiredOnboarding,
        Set<ProfileOnboardingTrigger> onboardingTriggers,
        boolean autoOpenMenu,
        boolean blocksAbilityUse,
        boolean blocksAbilityMenu,
        boolean blocksProgression,
        boolean playerCanReset,
        boolean adminCanReset,
        boolean reopenOnReset
) {
    public ProfileGroupDefinition {
        Objects.requireNonNull(id, "id");
        displayName = displayName == null ? Component.literal(id.toString()) : displayName;
        description = description == null ? Component.empty() : description;
        selectionLimit = Math.max(1, selectionLimit);
        onboardingTriggers = Set.copyOf(onboardingTriggers);
    }

    public static Builder builder(ResourceLocation id) {
        return new Builder(id);
    }

    public static final class Builder {
        private final ResourceLocation id;
        private Component displayName;
        private Component description = Component.empty();
        private @Nullable AbilityIcon icon;
        private int selectionLimit = 1;
        private boolean requiredOnboarding;
        private final Set<ProfileOnboardingTrigger> onboardingTriggers = new LinkedHashSet<>();
        private boolean autoOpenMenu = true;
        private boolean blocksAbilityUse;
        private boolean blocksAbilityMenu;
        private boolean blocksProgression;
        private boolean playerCanReset;
        private boolean adminCanReset = true;
        private boolean reopenOnReset;

        private Builder(ResourceLocation id) {
            this.id = Objects.requireNonNull(id, "id");
        }

        public Builder displayName(Component displayName) {
            this.displayName = Objects.requireNonNull(displayName, "displayName");
            return this;
        }

        public Builder description(Component description) {
            this.description = Objects.requireNonNull(description, "description");
            return this;
        }

        public Builder icon(AbilityIcon icon) {
            this.icon = Objects.requireNonNull(icon, "icon");
            return this;
        }

        public Builder selectionLimit(int selectionLimit) {
            this.selectionLimit = Math.max(1, selectionLimit);
            return this;
        }

        public Builder requiredOnboarding() {
            this.requiredOnboarding = true;
            return this;
        }

        public Builder onboardingTrigger(ProfileOnboardingTrigger trigger) {
            this.onboardingTriggers.add(Objects.requireNonNull(trigger, "trigger"));
            return this;
        }

        public Builder onboardingTriggers(Collection<ProfileOnboardingTrigger> triggers) {
            triggers.stream().filter(Objects::nonNull).forEach(this.onboardingTriggers::add);
            return this;
        }

        public Builder autoOpenMenu(boolean autoOpenMenu) {
            this.autoOpenMenu = autoOpenMenu;
            return this;
        }

        public Builder blockAbilityUseUntilSelected() {
            this.blocksAbilityUse = true;
            return this;
        }

        public Builder blockAbilityMenuUntilSelected() {
            this.blocksAbilityMenu = true;
            return this;
        }

        public Builder blockProgressionUntilSelected() {
            this.blocksProgression = true;
            return this;
        }

        public Builder playerCanReset() {
            this.playerCanReset = true;
            return this;
        }

        public Builder adminCanReset(boolean adminCanReset) {
            this.adminCanReset = adminCanReset;
            return this;
        }

        public Builder reopenOnReset() {
            this.reopenOnReset = true;
            return this;
        }

        public ProfileGroupDefinition build() {
            return new ProfileGroupDefinition(
                    this.id,
                    this.displayName,
                    this.description,
                    this.icon,
                    this.selectionLimit,
                    this.requiredOnboarding,
                    this.onboardingTriggers,
                    this.autoOpenMenu,
                    this.blocksAbilityUse,
                    this.blocksAbilityMenu,
                    this.blocksProgression,
                    this.playerCanReset,
                    this.adminCanReset,
                    this.reopenOnReset
            );
        }
    }
}
