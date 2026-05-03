package com.whatxe.xlib.cue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.whatxe.xlib.api.event.XLibClientRuntimeCueEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.NeoForge;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class XLibClientCueApiTest {
    private static final ResourceLocation ABILITY_ID = id("client_cue_ability");

    @AfterEach
    void cleanup() {
        XLibClientCueAdapterApi.clearAdapters();
        XLibCueRouteProfileApi.restoreDefaults();
    }

    @Test
    void emissionPostsClientRuntimeCueEvent() {
        List<XLibClientRuntimeCueEvent> events = new ArrayList<>();
        Consumer<XLibClientRuntimeCueEvent> listener = events::add;
        XLibRuntimeCue cue = XLibRuntimeCue.hitConfirm(ABILITY_ID);

        NeoForge.EVENT_BUS.start();
        NeoForge.EVENT_BUS.addListener(XLibClientRuntimeCueEvent.class, listener);
        try {
            XLibClientCueApi.emit(null, cue);
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
        }

        assertEquals(1, events.size());
        assertSame(cue, events.getFirst().cue());
        assertSame(null, events.getFirst().entity());
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }
}
