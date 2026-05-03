package com.whatxe.xlib.cue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.whatxe.xlib.ability.AbilityData;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class XLibCueAdapterApiTest {
    private static final ResourceLocation BODY_ADAPTER_ID = id("body_adapter");
    private static final ResourceLocation EFFECT_ADAPTER_ID = id("effect_adapter");
    private static final ResourceLocation CUSTOM_PROFILE_ID = id("model_only_profile");
    private static final ResourceLocation ABILITY_ID = id("adapter_test");

    @AfterEach
    void cleanup() {
        XLibCueApi.clearSinks();
        XLibCueAdapterApi.clearAdapters();
        XLibCueRouteProfileApi.restoreDefaults();
    }

    @Test
    void adaptersReceiveOnlySurfaceRoutedCues() {
        List<String> calls = new ArrayList<>();
        XLibCueAdapterApi.registerAdapter(
                BODY_ADAPTER_ID,
                XLibCueSurface.PLAYER_BODY_ANIMATION,
                (player, data, cue, surface) -> calls.add(surface.name() + ":" + cue.type().name())
        );
        XLibCueAdapterApi.registerAdapter(
                EFFECT_ADAPTER_ID,
                XLibCueSurface.EFFECT_PLAYBACK,
                (player, data, cue, surface) -> calls.add(surface.name() + ":" + cue.type().name())
        );

        XLibCueApi.emit(null, AbilityData.empty(), XLibRuntimeCue.activationStart(ABILITY_ID));

        assertEquals(
                List.of(
                        "PLAYER_BODY_ANIMATION:ACTIVATION_START",
                        "EFFECT_PLAYBACK:ACTIVATION_START"
                ),
                calls
        );
    }

    @Test
    void routeProfilesCanNarrowDispatchToSpecificCapabilitySurfaces() {
        List<String> calls = new ArrayList<>();
        XLibCueAdapterApi.registerAdapter(
                BODY_ADAPTER_ID,
                XLibCueSurface.PLAYER_BODY_ANIMATION,
                (player, data, cue, surface) -> calls.add("body")
        );
        XLibCueAdapterApi.registerAdapter(
                EFFECT_ADAPTER_ID,
                XLibCueSurface.MODEL_ANIMATION,
                (player, data, cue, surface) -> calls.add("model")
        );

        XLibCueRouteProfileApi.registerProfile(
                XLibCueRouteProfile.builder(CUSTOM_PROFILE_ID)
                        .route(XLibRuntimeCueType.STATE_ENTER, XLibCueSurface.MODEL_ANIMATION)
                        .build()
        );
        XLibCueRouteProfileApi.activate(CUSTOM_PROFILE_ID);

        XLibCueApi.emit(null, AbilityData.empty(), XLibRuntimeCue.stateEnter(ABILITY_ID));

        assertEquals(List.of("model"), calls);
    }

    @Test
    void adaptersCanRejectUnsupportedCueTypes() {
        List<String> calls = new ArrayList<>();
        XLibCueAdapterApi.registerAdapter(
                EFFECT_ADAPTER_ID,
                XLibCueSurface.EFFECT_PLAYBACK,
                new XLibCueAdapter() {
                    @Override
                    public boolean supports(XLibRuntimeCue cue) {
                        return cue.type() == XLibRuntimeCueType.HIT_CONFIRM;
                    }

                    @Override
                    public void onCue(net.minecraft.server.level.ServerPlayer player, AbilityData data, XLibRuntimeCue cue, XLibCueSurface surface) {
                        calls.add(cue.type().name());
                    }
                }
        );

        XLibCueApi.emit(null, AbilityData.empty(), XLibRuntimeCue.activationFail(ABILITY_ID));
        XLibCueApi.emit(null, AbilityData.empty(), XLibRuntimeCue.hitConfirm(ABILITY_ID));

        assertEquals(List.of("HIT_CONFIRM"), calls);
        assertTrue(XLibCueAdapterApi.adaptersForSurface(XLibCueSurface.EFFECT_PLAYBACK).containsKey(EFFECT_ADAPTER_ID));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }
}
