package com.whatxe.xlib.integration.blib;

import com.blib.api.client.animation.v1.command.AzCommand;
import com.whatxe.xlib.XLib;
import com.whatxe.xlib.cue.XLibClientCueAdapter;
import com.whatxe.xlib.cue.XLibClientCueAdapterApi;
import com.whatxe.xlib.cue.XLibCueSurface;
import com.whatxe.xlib.cue.XLibRuntimeCue;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

public final class XLibBLibCueBridgeApi {
    private static final Map<ResourceLocation, XLibBLibCueBinding> BINDINGS = new LinkedHashMap<>();
    private static final Comparator<XLibBLibCueBinding> MATCH_ORDER =
            Comparator.comparingInt(XLibBLibCueBinding::priority)
                    .reversed()
                    .thenComparing(Comparator.comparingInt(XLibBLibCueBinding::specificityScore).reversed());
    private static final ResourceLocation BODY_ADAPTER_ID =
            ResourceLocation.fromNamespaceAndPath(XLib.MODID, "blib_body_cue_bridge");
    private static final ResourceLocation MODEL_ADAPTER_ID =
            ResourceLocation.fromNamespaceAndPath(XLib.MODID, "blib_model_cue_bridge");
    private static final ResourceLocation EFFECT_ADAPTER_ID =
            ResourceLocation.fromNamespaceAndPath(XLib.MODID, "blib_effect_cue_bridge");
    private static boolean bootstrapped;

    private XLibBLibCueBridgeApi() {}

    public static void bootstrap() {
        if (bootstrapped) {
            return;
        }
        registerAdapter(BODY_ADAPTER_ID, XLibCueSurface.PLAYER_BODY_ANIMATION);
        registerAdapter(MODEL_ADAPTER_ID, XLibCueSurface.MODEL_ANIMATION);
        registerAdapter(EFFECT_ADAPTER_ID, XLibCueSurface.EFFECT_PLAYBACK);
        bootstrapped = true;
    }

    public static void register(XLibBLibCueBinding binding) {
        XLibBLibCueBinding resolvedBinding = Objects.requireNonNull(binding, "binding");
        BINDINGS.put(resolvedBinding.id(), resolvedBinding);
    }

    public static Optional<XLibBLibCueBinding> unregister(ResourceLocation bindingId) {
        return Optional.ofNullable(BINDINGS.remove(Objects.requireNonNull(bindingId, "bindingId")));
    }

    public static void clear() {
        BINDINGS.clear();
    }

    public static Collection<XLibBLibCueBinding> bindings() {
        return List.copyOf(BINDINGS.values());
    }

    static List<PlannedDispatch> planDispatches(XLibRuntimeCue cue, XLibCueSurface routedSurface) {
        Objects.requireNonNull(cue, "cue");
        Objects.requireNonNull(routedSurface, "routedSurface");
        List<XLibBLibCueBinding> matches = BINDINGS.values().stream()
                .filter(binding -> binding.matches(cue, routedSurface))
                .toList();
        if (matches.isEmpty()) {
            return List.of();
        }
        List<XLibBLibCueBinding> ordered = new java.util.ArrayList<>(matches);
        ordered.sort(MATCH_ORDER);
        List<PlannedDispatch> planned = new java.util.ArrayList<>(ordered.size());
        for (XLibBLibCueBinding binding : ordered) {
            planned.add(new PlannedDispatch(binding, binding.resolveCommand(cue)));
            if (binding.consumeCue()) {
                break;
            }
        }
        return List.copyOf(planned);
    }

    private static void registerAdapter(ResourceLocation adapterId, XLibCueSurface surface) {
        XLibClientCueAdapterApi.registerAdapter(adapterId, surface, new XLibClientCueAdapter() {
            @Override
            public void onCue(@Nullable Entity entity, XLibRuntimeCue cue, XLibCueSurface routedSurface) {
                if (entity == null) {
                    return;
                }
                for (PlannedDispatch dispatch : planDispatches(cue, routedSurface)) {
                    dispatch.command().sendForEntity(entity);
                }
            }
        });
    }

    record PlannedDispatch(XLibBLibCueBinding binding, AzCommand command) {
        PlannedDispatch {
            Objects.requireNonNull(binding, "binding");
            Objects.requireNonNull(command, "command");
        }
    }
}
