package com.whatxe.xlib.cue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.whatxe.xlib.ability.AbilityData;
import com.whatxe.xlib.api.event.XLibRuntimeCueEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.NeoForge;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class XLibCueApiTest {
    private static final ResourceLocation FIRST_SINK_ID = id("first_sink");
    private static final ResourceLocation SECOND_SINK_ID = id("second_sink");
    private static final ResourceLocation ABILITY_ID = id("cue_ability");

    @AfterEach
    void clearSinks() {
        XLibCueApi.clearSinks();
        XLibCueAdapterApi.clearAdapters();
        XLibCueRouteProfileApi.restoreDefaults();
    }

    @Test
    void registeredSinksReceiveEmittedCuesInRegistrationOrder() {
        List<String> calls = new ArrayList<>();
        XLibRuntimeCue cue = XLibRuntimeCue.activationStart(ABILITY_ID);

        XLibCueApi.registerSink(FIRST_SINK_ID, (player, data, emittedCue) -> calls.add("first:" + emittedCue.type().name()));
        XLibCueApi.registerSink(SECOND_SINK_ID, (player, data, emittedCue) -> calls.add("second:" + emittedCue.type().name()));

        XLibCueApi.emit(null, AbilityData.empty(), cue);

        assertEquals(List.of("first:ACTIVATION_START", "second:ACTIVATION_START"), calls);
    }

    @Test
    void emissionPostsRuntimeCueEvent() {
        List<XLibRuntimeCueEvent> events = new ArrayList<>();
        Consumer<XLibRuntimeCueEvent> listener = events::add;
        XLibRuntimeCue cue = XLibRuntimeCue.hitConfirm(ABILITY_ID);
        AbilityData data = AbilityData.empty().withCooldown(ABILITY_ID, 20);

        NeoForge.EVENT_BUS.start();
        NeoForge.EVENT_BUS.addListener(XLibRuntimeCueEvent.class, listener);
        try {
            XLibCueApi.emit(null, data, cue);
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
        }

        assertEquals(1, events.size());
        assertSame(cue, events.getFirst().cue());
        assertSame(data, events.getFirst().data());
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }
}
