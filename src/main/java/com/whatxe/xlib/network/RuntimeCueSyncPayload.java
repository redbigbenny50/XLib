package com.whatxe.xlib.network;

import com.whatxe.xlib.XLib;
import com.whatxe.xlib.cue.XLibClientCueApi;
import com.whatxe.xlib.cue.XLibRuntimeCue;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RuntimeCueSyncPayload(int entityId, XLibRuntimeCue cue) implements CustomPacketPayload {
    public static final Type<RuntimeCueSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(XLib.MODID, "sync_runtime_cue"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RuntimeCueSyncPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    RuntimeCueSyncPayload::entityId,
                    XLibRuntimeCue.STREAM_CODEC,
                    RuntimeCueSyncPayload::cue,
                    RuntimeCueSyncPayload::new
            );

    public RuntimeCueSyncPayload {
        cue = java.util.Objects.requireNonNull(cue, "cue");
    }

    public void applyClientSync() {
        XLibClientCueApi.emit(this.entityId, this.cue);
    }

    @Override
    public Type<RuntimeCueSyncPayload> type() {
        return TYPE;
    }
}
