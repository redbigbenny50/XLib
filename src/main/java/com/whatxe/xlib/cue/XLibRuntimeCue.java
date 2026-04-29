package com.whatxe.xlib.cue;

import com.whatxe.xlib.ability.AbilityEndReason;
import java.util.Objects;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public record XLibRuntimeCue(
        XLibRuntimeCueType type,
        @Nullable ResourceLocation abilityId,
        @Nullable ResourceLocation stateId,
        @Nullable AbilityEndReason endReason,
        int progress,
        int maxProgress
) {
    private static final XLibRuntimeCueType[] CUE_TYPES = XLibRuntimeCueType.values();
    private static final AbilityEndReason[] END_REASONS = AbilityEndReason.values();

    public static final StreamCodec<RegistryFriendlyByteBuf, XLibRuntimeCue> STREAM_CODEC =
            StreamCodec.of(
                    (buf, cue) -> {
                        buf.writeVarInt(cue.type.ordinal());
                        buf.writeBoolean(cue.abilityId != null);
                        if (cue.abilityId != null) ResourceLocation.STREAM_CODEC.encode(buf, cue.abilityId);
                        buf.writeBoolean(cue.stateId != null);
                        if (cue.stateId != null) ResourceLocation.STREAM_CODEC.encode(buf, cue.stateId);
                        buf.writeBoolean(cue.endReason != null);
                        if (cue.endReason != null) buf.writeVarInt(cue.endReason.ordinal());
                        buf.writeVarInt(cue.progress);
                        buf.writeVarInt(cue.maxProgress);
                    },
                    buf -> new XLibRuntimeCue(
                            CUE_TYPES[buf.readVarInt()],
                            buf.readBoolean() ? ResourceLocation.STREAM_CODEC.decode(buf) : null,
                            buf.readBoolean() ? ResourceLocation.STREAM_CODEC.decode(buf) : null,
                            buf.readBoolean() ? END_REASONS[buf.readVarInt()] : null,
                            buf.readVarInt(),
                            buf.readVarInt()
                    )
            );

    public XLibRuntimeCue {
        type = Objects.requireNonNull(type, "type");
        if (progress < 0) {
            throw new IllegalArgumentException("progress cannot be negative");
        }
        if (maxProgress < 0) {
            throw new IllegalArgumentException("maxProgress cannot be negative");
        }
        if (maxProgress > 0 && progress > maxProgress) {
            throw new IllegalArgumentException("progress cannot be greater than maxProgress");
        }
    }

    public static XLibRuntimeCue activationStart(ResourceLocation abilityId) {
        return new XLibRuntimeCue(XLibRuntimeCueType.ACTIVATION_START, requiredAbilityId(abilityId), null, null, 0, 0);
    }

    public static XLibRuntimeCue activationFail(ResourceLocation abilityId) {
        return new XLibRuntimeCue(XLibRuntimeCueType.ACTIVATION_FAIL, requiredAbilityId(abilityId), null, null, 0, 0);
    }

    public static XLibRuntimeCue chargeProgress(ResourceLocation abilityId, int chargedTicks, int maxChargeTicks) {
        return new XLibRuntimeCue(
                XLibRuntimeCueType.CHARGE_PROGRESS,
                requiredAbilityId(abilityId),
                null,
                null,
                chargedTicks,
                maxChargeTicks
        );
    }

    public static XLibRuntimeCue release(
            ResourceLocation abilityId,
            AbilityEndReason reason,
            int chargedTicks,
            int maxChargeTicks
    ) {
        return new XLibRuntimeCue(
                XLibRuntimeCueType.RELEASE,
                requiredAbilityId(abilityId),
                null,
                Objects.requireNonNull(reason, "reason"),
                chargedTicks,
                maxChargeTicks
        );
    }

    public static XLibRuntimeCue hitConfirm(ResourceLocation abilityId) {
        return new XLibRuntimeCue(XLibRuntimeCueType.HIT_CONFIRM, requiredAbilityId(abilityId), null, null, 0, 0);
    }

    public static XLibRuntimeCue interrupt(ResourceLocation abilityId, AbilityEndReason reason) {
        return new XLibRuntimeCue(
                XLibRuntimeCueType.INTERRUPT,
                requiredAbilityId(abilityId),
                null,
                Objects.requireNonNull(reason, "reason"),
                0,
                0
        );
    }

    public static XLibRuntimeCue stateEnter(ResourceLocation stateId) {
        return new XLibRuntimeCue(XLibRuntimeCueType.STATE_ENTER, null, requiredStateId(stateId), null, 0, 0);
    }

    public static XLibRuntimeCue stateExit(ResourceLocation stateId, AbilityEndReason reason) {
        return new XLibRuntimeCue(
                XLibRuntimeCueType.STATE_EXIT,
                null,
                requiredStateId(stateId),
                Objects.requireNonNull(reason, "reason"),
                0,
                0
        );
    }

    public boolean hasProgress() {
        return this.maxProgress > 0;
    }

    private static ResourceLocation requiredAbilityId(ResourceLocation abilityId) {
        return Objects.requireNonNull(abilityId, "abilityId");
    }

    private static ResourceLocation requiredStateId(ResourceLocation stateId) {
        return Objects.requireNonNull(stateId, "stateId");
    }
}
