package com.whatxe.xlib.client;

import java.util.Objects;

public final class AbilityMenuSessionStateApi {
    private static AbilityMenuSessionState state = AbilityMenuSessionState.defaultState();

    private AbilityMenuSessionStateApi() {}

    public static AbilityMenuSessionState state() {
        return state;
    }

    public static void setState(AbilityMenuSessionState state) {
        AbilityMenuSessionStateApi.state = Objects.requireNonNull(state, "state");
    }

    public static void reset() {
        state = AbilityMenuSessionState.defaultState();
    }
}
