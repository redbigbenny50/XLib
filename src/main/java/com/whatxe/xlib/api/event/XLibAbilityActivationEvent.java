package com.whatxe.xlib.api.event;

import com.whatxe.xlib.ability.AbilityData;
import com.whatxe.xlib.ability.AbilityDefinition;
import com.whatxe.xlib.ability.AbilityUseResult;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import org.jetbrains.annotations.Nullable;

public abstract class XLibAbilityActivationEvent extends Event {
    private final ServerPlayer player;
    private final AbilityDefinition ability;
    private final AbilityData currentData;

    protected XLibAbilityActivationEvent(ServerPlayer player, AbilityDefinition ability, AbilityData currentData) {
        this.player = player;
        this.ability = ability;
        this.currentData = currentData;
    }

    public ServerPlayer player() {
        return this.player;
    }

    public AbilityDefinition ability() {
        return this.ability;
    }

    public AbilityData currentData() {
        return this.currentData;
    }

    public static final class Pre extends XLibAbilityActivationEvent implements ICancellableEvent {
        @Nullable
        private Component failureFeedback;

        public Pre(ServerPlayer player, AbilityDefinition ability, AbilityData currentData) {
            super(player, ability, currentData);
        }

        @Nullable
        public Component failureFeedback() {
            return this.failureFeedback;
        }

        public void setFailureFeedback(@Nullable Component failureFeedback) {
            this.failureFeedback = failureFeedback;
        }
    }

    public static final class Post extends XLibAbilityActivationEvent {
        private final AbilityUseResult result;

        public Post(ServerPlayer player, AbilityDefinition ability, AbilityData currentData, AbilityUseResult result) {
            super(player, ability, currentData);
            this.result = result;
        }

        public AbilityUseResult result() {
            return this.result;
        }
    }
}
