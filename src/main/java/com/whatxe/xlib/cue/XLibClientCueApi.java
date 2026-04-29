package com.whatxe.xlib.cue;

import com.whatxe.xlib.api.event.XLibClientRuntimeCueEvent;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

public final class XLibClientCueApi {
    private XLibClientCueApi() {}

    public static void emit(int entityId, XLibRuntimeCue cue) {
        Minecraft minecraft = Minecraft.getInstance();
        Entity entity = minecraft.level == null ? null : minecraft.level.getEntity(entityId);
        emit(entity, cue);
    }

    public static void emit(@Nullable Entity entity, XLibRuntimeCue cue) {
        XLibRuntimeCue resolvedCue = Objects.requireNonNull(cue, "cue");
        XLibCueRouteProfile routeProfile = XLibCueRouteProfileApi.active();
        NeoForge.EVENT_BUS.post(new XLibClientRuntimeCueEvent(entity, resolvedCue));
        for (XLibCueSurface surface : routeProfile.surfacesFor(resolvedCue.type())) {
            for (XLibClientCueAdapter adapter : List.copyOf(XLibClientCueAdapterApi.adapters(surface))) {
                if (adapter.supports(resolvedCue)) {
                    adapter.onCue(entity, resolvedCue, surface);
                }
            }
        }
    }
}
