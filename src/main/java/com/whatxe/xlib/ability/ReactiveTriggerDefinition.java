package com.whatxe.xlib.ability;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public record ReactiveTriggerDefinition(
        ResourceLocation id,
        Set<ReactiveEventType> events,
        Set<ResourceLocation> requiredDetectors,
        Set<ResourceLocation> consumedDetectors,
        TriggerCondition condition,
        TriggerAction action
) {
    @FunctionalInterface
    public interface TriggerCondition {
        boolean test(@Nullable ServerPlayer player, AbilityData data, ReactiveRuntimeEvent event);
    }

    @FunctionalInterface
    public interface TriggerAction {
        AbilityData apply(@Nullable ServerPlayer player, AbilityData data, ReactiveRuntimeEvent event);
    }

    public ReactiveTriggerDefinition {
        Objects.requireNonNull(id, "id");
        events = Set.copyOf(events);
        requiredDetectors = Set.copyOf(requiredDetectors);
        consumedDetectors = Set.copyOf(consumedDetectors);
        Objects.requireNonNull(condition, "condition");
        Objects.requireNonNull(action, "action");
    }

    public static Builder builder(ResourceLocation id) {
        return new Builder(id);
    }

    public boolean matches(@Nullable ServerPlayer player, AbilityData data, ReactiveRuntimeEvent event) {
        if (!this.events.contains(event.type())) {
            return false;
        }
        for (ResourceLocation detectorId : this.requiredDetectors) {
            if (!AbilityDetectorApi.hasActiveDetector(data, detectorId)) {
                return false;
            }
        }
        return this.condition.test(player, data, event);
    }

    public static final class Builder {
        private final ResourceLocation id;
        private final Set<ReactiveEventType> events = new LinkedHashSet<>();
        private final Set<ResourceLocation> requiredDetectors = new LinkedHashSet<>();
        private final Set<ResourceLocation> consumedDetectors = new LinkedHashSet<>();
        private TriggerCondition condition = (player, data, event) -> true;
        private TriggerAction action = (player, data, event) -> data;

        private Builder(ResourceLocation id) {
            this.id = Objects.requireNonNull(id, "id");
        }

        public Builder event(ReactiveEventType eventType) {
            this.events.add(Objects.requireNonNull(eventType, "eventType"));
            return this;
        }

        public Builder events(Collection<ReactiveEventType> eventTypes) {
            eventTypes.stream().filter(Objects::nonNull).forEach(this.events::add);
            return this;
        }

        public Builder requireDetector(ResourceLocation detectorId) {
            this.requiredDetectors.add(Objects.requireNonNull(detectorId, "detectorId"));
            return this;
        }

        public Builder requireDetectors(Collection<ResourceLocation> detectorIds) {
            detectorIds.stream().filter(Objects::nonNull).forEach(this.requiredDetectors::add);
            return this;
        }

        public Builder consumeDetector(ResourceLocation detectorId) {
            this.consumedDetectors.add(Objects.requireNonNull(detectorId, "detectorId"));
            return this;
        }

        public Builder consumeRequiredDetectors() {
            this.consumedDetectors.addAll(this.requiredDetectors);
            return this;
        }

        public Builder consumedDetectors(Collection<ResourceLocation> detectorIds) {
            detectorIds.stream().filter(Objects::nonNull).forEach(this.consumedDetectors::add);
            return this;
        }

        public Builder condition(TriggerCondition condition) {
            this.condition = Objects.requireNonNull(condition, "condition");
            return this;
        }

        public Builder action(TriggerAction action) {
            this.action = Objects.requireNonNull(action, "action");
            return this;
        }

        public ReactiveTriggerDefinition build() {
            if (this.events.isEmpty()) {
                throw new IllegalStateException("Reactive triggers require at least one event");
            }
            return new ReactiveTriggerDefinition(
                    this.id,
                    this.events,
                    this.requiredDetectors,
                    this.consumedDetectors,
                    this.condition,
                    this.action
            );
        }
    }
}
