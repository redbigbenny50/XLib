package com.whatxe.xlib.integration.blib;

import com.blib.api.client.animation.v1.command.AzCommand;
import com.whatxe.xlib.ability.AbilityEndReason;
import com.whatxe.xlib.cue.XLibCueSurface;
import com.whatxe.xlib.cue.XLibRuntimeCue;
import com.whatxe.xlib.cue.XLibRuntimeCueType;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public record XLibBLibCueBinding(
        ResourceLocation id,
        XLibCueSurface surface,
        Set<XLibRuntimeCueType> cueTypes,
        @Nullable ResourceLocation abilityId,
        @Nullable ResourceLocation stateId,
        Set<AbilityEndReason> endReasons,
        @Nullable Integer minProgress,
        @Nullable Integer maxProgress,
        int priority,
        boolean consumeCue,
        AzCommand command,
        Map<XLibRuntimeCueType, AzCommand> cueCommands
) {
    public XLibBLibCueBinding {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(surface, "surface");
        Objects.requireNonNull(cueTypes, "cueTypes");
        Objects.requireNonNull(endReasons, "endReasons");
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(cueCommands, "cueCommands");
        if (cueTypes.isEmpty()) {
            throw new IllegalArgumentException("cueTypes cannot be empty");
        }
        if (minProgress != null && minProgress < 0) {
            throw new IllegalArgumentException("minProgress cannot be negative");
        }
        if (maxProgress != null && maxProgress < 0) {
            throw new IllegalArgumentException("maxProgress cannot be negative");
        }
        if (minProgress != null && maxProgress != null && minProgress > maxProgress) {
            throw new IllegalArgumentException("minProgress cannot be greater than maxProgress");
        }
        cueTypes = Collections.unmodifiableSet(EnumSet.copyOf(cueTypes));
        endReasons = endReasons.isEmpty()
                ? Set.of()
                : Collections.unmodifiableSet(EnumSet.copyOf(endReasons));
        EnumMap<XLibRuntimeCueType, AzCommand> normalizedCommands = new EnumMap<>(XLibRuntimeCueType.class);
        for (Map.Entry<XLibRuntimeCueType, AzCommand> entry : cueCommands.entrySet()) {
            XLibRuntimeCueType cueType = Objects.requireNonNull(entry.getKey(), "cueCommandType");
            if (!cueTypes.contains(cueType)) {
                throw new IllegalArgumentException("cueCommands must target declared cueTypes");
            }
            normalizedCommands.put(
                    cueType,
                    Objects.requireNonNull(entry.getValue(), "cueCommand")
            );
        }
        cueCommands = Collections.unmodifiableMap(normalizedCommands);
    }

    public static Builder builder(ResourceLocation id, XLibCueSurface surface, AzCommand command) {
        return new Builder(id, surface, command);
    }

    public boolean matches(XLibRuntimeCue cue, XLibCueSurface routedSurface) {
        Objects.requireNonNull(cue, "cue");
        Objects.requireNonNull(routedSurface, "routedSurface");
        if (this.surface != routedSurface || !this.cueTypes.contains(cue.type())) {
            return false;
        }
        if (this.abilityId != null && !Objects.equals(this.abilityId, cue.abilityId())) {
            return false;
        }
        if (this.stateId != null && !Objects.equals(this.stateId, cue.stateId())) {
            return false;
        }
        if (!this.endReasons.isEmpty() && !this.endReasons.contains(cue.endReason())) {
            return false;
        }
        if (this.minProgress != null || this.maxProgress != null) {
            if (!cue.hasProgress()) {
                return false;
            }
            if (this.minProgress != null && cue.progress() < this.minProgress) {
                return false;
            }
            if (this.maxProgress != null && cue.progress() > this.maxProgress) {
                return false;
            }
        }
        return true;
    }

    public AzCommand resolveCommand(XLibRuntimeCue cue) {
        Objects.requireNonNull(cue, "cue");
        return this.cueCommands.getOrDefault(cue.type(), this.command);
    }

    public int specificityScore() {
        int score = 0;
        if (this.abilityId != null) {
            score++;
        }
        if (this.stateId != null) {
            score++;
        }
        if (!this.endReasons.isEmpty()) {
            score++;
        }
        if (this.minProgress != null) {
            score++;
        }
        if (this.maxProgress != null) {
            score++;
        }
        return score;
    }

    public static final class Builder {
        private final ResourceLocation id;
        private final XLibCueSurface surface;
        private final AzCommand command;
        private final EnumSet<XLibRuntimeCueType> cueTypes = EnumSet.noneOf(XLibRuntimeCueType.class);
        private final EnumSet<AbilityEndReason> endReasons = EnumSet.noneOf(AbilityEndReason.class);
        private final Map<XLibRuntimeCueType, AzCommand> cueCommands = new EnumMap<>(XLibRuntimeCueType.class);
        private @Nullable ResourceLocation abilityId;
        private @Nullable ResourceLocation stateId;
        private @Nullable Integer minProgress;
        private @Nullable Integer maxProgress;
        private int priority;
        private boolean consumeCue;

        private Builder(ResourceLocation id, XLibCueSurface surface, AzCommand command) {
            this.id = Objects.requireNonNull(id, "id");
            this.surface = Objects.requireNonNull(surface, "surface");
            this.command = Objects.requireNonNull(command, "command");
        }

        public Builder cueType(XLibRuntimeCueType cueType) {
            this.cueTypes.add(Objects.requireNonNull(cueType, "cueType"));
            return this;
        }

        public Builder cueTypes(XLibRuntimeCueType first, XLibRuntimeCueType... rest) {
            cueType(first);
            for (XLibRuntimeCueType cueType : Objects.requireNonNull(rest, "rest")) {
                cueType(cueType);
            }
            return this;
        }

        public Builder ability(ResourceLocation abilityId) {
            this.abilityId = Objects.requireNonNull(abilityId, "abilityId");
            return this;
        }

        public Builder state(ResourceLocation stateId) {
            this.stateId = Objects.requireNonNull(stateId, "stateId");
            return this;
        }

        public Builder endReason(AbilityEndReason endReason) {
            this.endReasons.add(Objects.requireNonNull(endReason, "endReason"));
            return this;
        }

        public Builder endReasons(AbilityEndReason first, AbilityEndReason... rest) {
            endReason(first);
            for (AbilityEndReason endReason : Objects.requireNonNull(rest, "rest")) {
                endReason(endReason);
            }
            return this;
        }

        public Builder progressAtLeast(int minProgress) {
            this.minProgress = minProgress;
            return this;
        }

        public Builder progressAtMost(int maxProgress) {
            this.maxProgress = maxProgress;
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder consumeCue() {
            return consumeCue(true);
        }

        public Builder consumeCue(boolean consumeCue) {
            this.consumeCue = consumeCue;
            return this;
        }

        public Builder cueCommand(XLibRuntimeCueType cueType, AzCommand command) {
            this.cueCommands.put(
                    Objects.requireNonNull(cueType, "cueType"),
                    Objects.requireNonNull(command, "command")
            );
            return this;
        }

        public XLibBLibCueBinding build() {
            return new XLibBLibCueBinding(
                    this.id,
                    this.surface,
                    this.cueTypes,
                    this.abilityId,
                    this.stateId,
                    this.endReasons,
                    this.minProgress,
                    this.maxProgress,
                    this.priority,
                    this.consumeCue,
                    this.command,
                    this.cueCommands
            );
        }
    }
}
