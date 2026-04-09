package com.whatxe.xlib.cue;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

public final class XLibCueRouteProfileApi {
    private static final Map<ResourceLocation, XLibCueRouteProfile> PROFILES = new LinkedHashMap<>();
    private static ResourceLocation activeProfileId;

    private XLibCueRouteProfileApi() {}

    public static void bootstrap() {
        if (!PROFILES.isEmpty()) {
            return;
        }
        restoreDefaults();
    }

    public static void restoreDefaults() {
        PROFILES.clear();
        registerBuiltIn(XLibCueRouteProfile.defaultProfile());
        registerBuiltIn(XLibCueRouteProfile.effectsHeavyProfile());
        activeProfileId = XLibCueRouteProfile.defaultProfile().id();
    }

    public static XLibCueRouteProfile registerProfile(XLibCueRouteProfile profile) {
        Objects.requireNonNull(profile, "profile");
        XLibCueRouteProfile previous = PROFILES.putIfAbsent(profile.id(), profile);
        if (previous != null) {
            throw new IllegalStateException("Duplicate cue route profile registration: " + profile.id());
        }
        return profile;
    }

    public static Optional<XLibCueRouteProfile> unregisterProfile(ResourceLocation profileId) {
        XLibCueRouteProfile removed = PROFILES.remove(Objects.requireNonNull(profileId, "profileId"));
        if (Objects.equals(activeProfileId, profileId)) {
            activeProfileId = XLibCueRouteProfile.defaultProfile().id();
        }
        if (PROFILES.isEmpty()) {
            restoreDefaults();
        }
        return Optional.ofNullable(removed);
    }

    public static void activate(ResourceLocation profileId) {
        if (!PROFILES.containsKey(Objects.requireNonNull(profileId, "profileId"))) {
            throw new IllegalArgumentException("Unknown cue route profile: " + profileId);
        }
        activeProfileId = profileId;
    }

    public static XLibCueRouteProfile active() {
        bootstrap();
        XLibCueRouteProfile active = PROFILES.get(activeProfileId);
        if (active != null) {
            return active;
        }
        return PROFILES.get(XLibCueRouteProfile.defaultProfile().id());
    }

    public static Optional<XLibCueRouteProfile> find(ResourceLocation profileId) {
        bootstrap();
        return Optional.ofNullable(PROFILES.get(profileId));
    }

    public static Collection<XLibCueRouteProfile> allProfiles() {
        bootstrap();
        return java.util.List.copyOf(PROFILES.values());
    }

    private static void registerBuiltIn(XLibCueRouteProfile profile) {
        PROFILES.put(profile.id(), profile);
    }
}
