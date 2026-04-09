package com.whatxe.xlib.ability;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public record AbilityDetectorDefinition(
        ResourceLocation id,
        int durationTicks,
        Set<ReactiveEventType> events,
        ReactiveCondition condition
) {
    @FunctionalInterface
    public interface ReactiveCondition {
        boolean test(@Nullable ServerPlayer player, AbilityData data, ReactiveRuntimeEvent event);
    }

    public AbilityDetectorDefinition {
        Objects.requireNonNull(id, "id");
        if (durationTicks <= 0) {
            throw new IllegalArgumentException("Detector duration must be positive");
        }
        events = Set.copyOf(events);
        Objects.requireNonNull(condition, "condition");
    }

    public static Builder builder(ResourceLocation id, int durationTicks) {
        return new Builder(id, durationTicks);
    }

    public boolean matches(@Nullable ServerPlayer player, AbilityData data, ReactiveRuntimeEvent event) {
        return this.events.contains(event.type()) && this.condition.test(player, data, event);
    }

    public static final class Builder {
        private final ResourceLocation id;
        private final int durationTicks;
        private final Set<ReactiveEventType> events = new LinkedHashSet<>();
        private ReactiveCondition condition = (player, data, event) -> true;

        private Builder(ResourceLocation id, int durationTicks) {
            this.id = Objects.requireNonNull(id, "id");
            this.durationTicks = durationTicks;
        }

        public Builder event(ReactiveEventType eventType) {
            this.events.add(Objects.requireNonNull(eventType, "eventType"));
            return this;
        }

        public Builder events(Collection<ReactiveEventType> eventTypes) {
            eventTypes.stream().filter(Objects::nonNull).forEach(this.events::add);
            return this;
        }

        public Builder condition(ReactiveCondition condition) {
            this.condition = Objects.requireNonNull(condition, "condition");
            return this;
        }

        public AbilityDetectorDefinition build() {
            if (this.events.isEmpty()) {
                throw new IllegalStateException("Detectors require at least one reactive event");
            }
            return new AbilityDetectorDefinition(this.id, this.durationTicks, this.events, this.condition);
        }
    }
}
