package com.whatxe.xlib.api.event;

import com.whatxe.xlib.ability.AbilityData;
import com.whatxe.xlib.ability.AbilityDefinition;
import com.whatxe.xlib.ability.AbilityEndReason;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

public sealed class XLibModeEvent extends Event permits XLibModeEvent.Started, XLibModeEvent.Ended {
    private final ServerPlayer player;
    private final AbilityDefinition mode;
    private final AbilityData previousData;
    private final AbilityData currentData;

    protected XLibModeEvent(
            ServerPlayer player,
            AbilityDefinition mode,
            AbilityData previousData,
            AbilityData currentData
    ) {
        this.player = player;
        this.mode = mode;
        this.previousData = previousData;
        this.currentData = currentData;
    }

    public ServerPlayer player() {
        return this.player;
    }

    public AbilityDefinition mode() {
        return this.mode;
    }

    public AbilityData previousData() {
        return this.previousData;
    }

    public AbilityData currentData() {
        return this.currentData;
    }

    public static final class Started extends XLibModeEvent {
        public Started(ServerPlayer player, AbilityDefinition mode, AbilityData previousData, AbilityData currentData) {
            super(player, mode, previousData, currentData);
        }
    }

    public static sealed class Ended extends XLibModeEvent permits DurationExpired, RequirementInvalidated, ForceEnded, ReplacedByTransform, ReplacedByExclusive {
        private final AbilityEndReason reason;

        public Ended(
                ServerPlayer player,
                AbilityDefinition mode,
                AbilityData previousData,
                AbilityData currentData,
                AbilityEndReason reason
        ) {
            super(player, mode, previousData, currentData);
            this.reason = reason;
        }

        public AbilityEndReason reason() {
            return this.reason;
        }
    }

    public static final class DurationExpired extends Ended {
        public DurationExpired(ServerPlayer player, AbilityDefinition mode, AbilityData previousData, AbilityData currentData) {
            super(player, mode, previousData, currentData, AbilityEndReason.DURATION_EXPIRED);
        }
    }

    public static final class RequirementInvalidated extends Ended {
        public RequirementInvalidated(ServerPlayer player, AbilityDefinition mode, AbilityData previousData, AbilityData currentData) {
            super(player, mode, previousData, currentData, AbilityEndReason.REQUIREMENT_INVALIDATED);
        }
    }

    public static final class ForceEnded extends Ended {
        public ForceEnded(ServerPlayer player, AbilityDefinition mode, AbilityData previousData, AbilityData currentData) {
            super(player, mode, previousData, currentData, AbilityEndReason.FORCE_ENDED);
        }
    }

    public static final class ReplacedByTransform extends Ended {
        public ReplacedByTransform(ServerPlayer player, AbilityDefinition mode, AbilityData previousData, AbilityData currentData) {
            super(player, mode, previousData, currentData, AbilityEndReason.REPLACED_BY_TRANSFORM);
        }
    }

    public static final class ReplacedByExclusive extends Ended {
        public ReplacedByExclusive(ServerPlayer player, AbilityDefinition mode, AbilityData previousData, AbilityData currentData) {
            super(player, mode, previousData, currentData, AbilityEndReason.REPLACED_BY_EXCLUSIVE);
        }
    }
}
