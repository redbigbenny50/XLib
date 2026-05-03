package com.whatxe.xlib.client;

import java.util.Objects;

public final class ProgressionMenuSessionStateApi {
    private static ProgressionMenuSessionState state = ProgressionMenuSessionState.defaultState();

    private ProgressionMenuSessionStateApi() {}

    public static ProgressionMenuSessionState state() {
        return state;
    }

    public static void setState(ProgressionMenuSessionState state) {
        ProgressionMenuSessionStateApi.state = Objects.requireNonNull(state, "state");
    }

    public static void reset() {
        state = ProgressionMenuSessionState.defaultState();
    }
}
