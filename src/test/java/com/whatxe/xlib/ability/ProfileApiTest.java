package com.whatxe.xlib.ability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ProfileApiTest {
    private static final ResourceLocation GROUP_ID = id("group/core");
    private static final ResourceLocation PROFILE_A_ID = id("profile/a");
    private static final ResourceLocation PROFILE_B_ID = id("profile/b");

    @AfterEach
    void unregisterFixtures() {
        ProfileApi.unregisterProfile(PROFILE_A_ID);
        ProfileApi.unregisterProfile(PROFILE_B_ID);
        ProfileApi.unregisterGroup(GROUP_ID);
    }

    @Test
    void sanitizeDataEnforcesGroupLimitsAndConflicts() {
        ProfileApi.registerGroup(ProfileGroupDefinition.builder(GROUP_ID)
                .displayName(Component.literal("Core"))
                .requiredOnboarding()
                .onboardingTrigger(ProfileOnboardingTrigger.FIRST_LOGIN)
                .build());
        ProfileApi.registerProfile(ProfileDefinition.builder(PROFILE_A_ID, GROUP_ID, AbilityIcon.ofTexture(id("icon/a")))
                .displayName(Component.literal("A"))
                .incompatibleWith(PROFILE_B_ID)
                .build());
        ProfileApi.registerProfile(ProfileDefinition.builder(PROFILE_B_ID, GROUP_ID, AbilityIcon.ofTexture(id("icon/b")))
                .displayName(Component.literal("B"))
                .build());

        ProfileSelectionData sanitized = ProfileApi.sanitizeData(new ProfileSelectionData(
                java.util.Map.of(
                        PROFILE_A_ID, new ProfileSelectionEntry(GROUP_ID, ProfileSelectionOrigin.PLAYER, false, ""),
                        PROFILE_B_ID, new ProfileSelectionEntry(GROUP_ID, ProfileSelectionOrigin.PLAYER, false, "")
                ),
                java.util.Map.of(GROUP_ID, new ProfilePendingSelection(ProfileOnboardingTrigger.FIRST_LOGIN, "first_login")),
                java.util.Map.of(),
                java.util.Map.of(),
                true
        ));

        assertEquals(1, sanitized.selectedProfileIds().size());
        assertTrue(sanitized.selectedProfileIds().contains(PROFILE_A_ID) || sanitized.selectedProfileIds().contains(PROFILE_B_ID));
        assertFalse(sanitized.hasPendingGroup(GROUP_ID));
    }

    @Test
    void pendingGroupsStayWhenRequiredSelectionIsStillMissing() {
        ProfileApi.registerGroup(ProfileGroupDefinition.builder(GROUP_ID)
                .displayName(Component.literal("Core"))
                .requiredOnboarding()
                .selectionLimit(2)
                .onboardingTrigger(ProfileOnboardingTrigger.FIRST_LOGIN)
                .build());
        ProfileApi.registerProfile(ProfileDefinition.builder(PROFILE_A_ID, GROUP_ID, AbilityIcon.ofTexture(id("icon/a"))).build());
        ProfileApi.registerProfile(ProfileDefinition.builder(PROFILE_B_ID, GROUP_ID, AbilityIcon.ofTexture(id("icon/b"))).build());

        ProfileSelectionData sanitized = ProfileApi.sanitizeData(new ProfileSelectionData(
                java.util.Map.of(PROFILE_A_ID, new ProfileSelectionEntry(GROUP_ID, ProfileSelectionOrigin.PLAYER, false, "")),
                java.util.Map.of(GROUP_ID, new ProfilePendingSelection(ProfileOnboardingTrigger.FIRST_LOGIN, "first_login")),
                java.util.Map.of(),
                java.util.Map.of(),
                true
        ));

        assertEquals(java.util.Set.of(PROFILE_A_ID), sanitized.selectedProfileIds());
        assertTrue(sanitized.hasPendingGroup(GROUP_ID));
    }

    @Test
    void profileSourceIdsRoundTrip() {
        ResourceLocation sourceId = ProfileApi.sourceIdFor(PROFILE_A_ID);

        assertEquals(PROFILE_A_ID, ProfileApi.parseSourceId(sourceId).orElseThrow());
        assertTrue(ProfileApi.parseSourceId(id("not_profile_source")).isEmpty());
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }
}
