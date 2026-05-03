package com.whatxe.xlib.cue;

import com.whatxe.xlib.ability.AbilityData;
import com.whatxe.xlib.api.event.XLibRuntimeCueEvent;
import com.whatxe.xlib.attachment.ModAttachments;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

public final class XLibCueApi {
    private static final Map<ResourceLocation, XLibCueSink> SINKS = new LinkedHashMap<>();

    private XLibCueApi() {}

    public static void registerSink(ResourceLocation sinkId, XLibCueSink sink) {
        SINKS.put(Objects.requireNonNull(sinkId, "sinkId"), Objects.requireNonNull(sink, "sink"));
    }

    public static void unregisterSink(ResourceLocation sinkId) {
        SINKS.remove(Objects.requireNonNull(sinkId, "sinkId"));
    }

    public static void clearSinks() {
        SINKS.clear();
    }

    public static Map<ResourceLocation, XLibCueSink> sinks() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(SINKS));
    }

    public static void emit(@Nullable ServerPlayer player, XLibRuntimeCue cue) {
        emit(player, player == null ? AbilityData.empty() : ModAttachments.get(player), cue);
    }

    public static void emit(@Nullable ServerPlayer player, AbilityData data, XLibRuntimeCue cue) {
        AbilityData resolvedData = Objects.requireNonNull(data, "data");
        XLibRuntimeCue resolvedCue = Objects.requireNonNull(cue, "cue");
        XLibCueRouteProfile routeProfile = XLibCueRouteProfileApi.active();
        NeoForge.EVENT_BUS.post(new XLibRuntimeCueEvent(player, resolvedData, resolvedCue));
        for (XLibCueSink sink : List.copyOf(SINKS.values())) {
            sink.onCue(player, resolvedData, resolvedCue);
        }
        for (XLibCueSurface surface : routeProfile.surfacesFor(resolvedCue.type())) {
            for (XLibCueAdapter adapter : XLibCueAdapterApi.adapters(surface)) {
                if (adapter.supports(resolvedCue)) {
                    adapter.onCue(player, resolvedData, resolvedCue, surface);
                }
            }
        }
    }
}
