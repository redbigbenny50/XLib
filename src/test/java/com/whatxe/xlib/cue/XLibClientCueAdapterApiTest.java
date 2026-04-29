package com.whatxe.xlib.cue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class XLibClientCueAdapterApiTest {
    private static final ResourceLocation BODY_ADAPTER_ID = id("body_client_adapter");
    private static final ResourceLocation EFFECT_ADAPTER_ID = id("effect_client_adapter");
    private static final ResourceLocation CUSTOM_PROFILE_ID = id("client_model_only_profile");
    private static final ResourceLocation ABILITY_ID = id("client_adapter_test");

    @AfterEach
    void cleanup() {
        XLibClientCueAdapterApi.clearAdapters();
        XLibCueRouteProfileApi.restoreDefaults();
    }

    @Test
    void adaptersReceiveOnlySurfaceRoutedCues() {
        List<String> calls = new ArrayList<>();
        XLibClientCueAdapterApi.registerAdapter(
                BODY_ADAPTER_ID,
                XLibCueSurface.PLAYER_BODY_ANIMATION,
                (entity, cue, surface) -> calls.add(surface.name() + ":" + cue.type().name())
        );
        XLibClientCueAdapterApi.registerAdapter(
                EFFECT_ADAPTER_ID,
                XLibCueSurface.EFFECT_PLAYBACK,
                (entity, cue, surface) -> calls.add(surface.name() + ":" + cue.type().name())
        );

        XLibClientCueApi.emit(null, XLibRuntimeCue.activationStart(ABILITY_ID));

        assertEquals(
                List.of(
                        "PLAYER_BODY_ANIMATION:ACTIVATION_START",
                        "EFFECT_PLAYBACK:ACTIVATION_START"
                ),
                calls
        );
    }

    @Test
    void routeProfilesCanNarrowClientDispatchToSpecificCapabilitySurfaces() {
        List<String> calls = new ArrayList<>();
        XLibClientCueAdapterApi.registerAdapter(
                BODY_ADAPTER_ID,
                XLibCueSurface.PLAYER_BODY_ANIMATION,
                (entity, cue, surface) -> calls.add("body")
        );
        XLibClientCueAdapterApi.registerAdapter(
                EFFECT_ADAPTER_ID,
                XLibCueSurface.MODEL_ANIMATION,
                (entity, cue, surface) -> calls.add("model")
        );

        XLibCueRouteProfileApi.registerProfile(
                XLibCueRouteProfile.builder(CUSTOM_PROFILE_ID)
                        .route(XLibRuntimeCueType.STATE_ENTER, XLibCueSurface.MODEL_ANIMATION)
                        .build()
        );
        XLibCueRouteProfileApi.activate(CUSTOM_PROFILE_ID);

        XLibClientCueApi.emit(null, XLibRuntimeCue.stateEnter(ABILITY_ID));

        assertEquals(List.of("model"), calls);
    }

    @Test
    void adaptersCanRejectUnsupportedCueTypes() {
        List<String> calls = new ArrayList<>();
        XLibClientCueAdapterApi.registerAdapter(
                EFFECT_ADAPTER_ID,
                XLibCueSurface.EFFECT_PLAYBACK,
                new XLibClientCueAdapter() {
                    @Override
                    public boolean supports(XLibRuntimeCue cue) {
                        return cue.type() == XLibRuntimeCueType.HIT_CONFIRM;
                    }

                    @Override
                    public void onCue(net.minecraft.world.entity.Entity entity, XLibRuntimeCue cue, XLibCueSurface surface) {
                        calls.add(cue.type().name());
                    }
                }
        );

        XLibClientCueApi.emit(null, XLibRuntimeCue.activationFail(ABILITY_ID));
        XLibClientCueApi.emit(null, XLibRuntimeCue.hitConfirm(ABILITY_ID));

        assertEquals(List.of("HIT_CONFIRM"), calls);
        assertTrue(XLibClientCueAdapterApi.adaptersForSurface(XLibCueSurface.EFFECT_PLAYBACK).containsKey(EFFECT_ADAPTER_ID));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }
}
