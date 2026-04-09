package com.whatxe.xlib.ability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class AbilityRequirementsTest {
    private static final ResourceLocation MODE_ID = id("berserk_mode");
    private static final ResourceLocation COMBO_ID = id("combo_followup");
    private static final ResourceLocation DETECTOR_ID = id("detector/counter");
    private static final ResourceLocation STATE_FLAG_ID = id("state/empowered");
    private static final ResourceLocation IDENTITY_ID = id("identity/origin");
    private static final ResourceLocation ARTIFACT_ID = id("artifact/relic");
    private static final ResourceLocation SOURCE_ID = id("source/context");
    private static final ResourceLocation STICK_ID = ResourceLocation.withDefaultNamespace("stick");

    @Test
    void modeActiveOnlyPassesWhenModeIsRunning() {
        AbilityRequirement requirement = AbilityRequirements.modeActive(MODE_ID);

        assertTrue(requirement.validate(null, AbilityData.empty().withMode(MODE_ID, true)).isEmpty());
        assertFalse(requirement.validate(null, AbilityData.empty()).isEmpty());
    }

    @Test
    void modeInactiveOnlyPassesWhenModeIsNotRunning() {
        AbilityRequirement requirement = AbilityRequirements.modeInactive(MODE_ID);

        assertTrue(requirement.validate(null, AbilityData.empty()).isEmpty());
        assertFalse(requirement.validate(null, AbilityData.empty().withMode(MODE_ID, true)).isEmpty());
    }

    @Test
    void comboWindowOnlyPassesDuringActiveWindow() {
        AbilityRequirement requirement = AbilityRequirements.comboWindowActive(COMBO_ID);

        assertTrue(requirement.validate(null, AbilityData.empty().withComboWindow(COMBO_ID, 10)).isEmpty());
        assertFalse(requirement.validate(null, AbilityData.empty()).isEmpty());
    }

    @Test
    void stateFlagRequirementOnlyPassesWhenFlagIsActive() {
        StateFlagApi.unregisterStateFlag(STATE_FLAG_ID);
        try {
            StateFlagApi.registerStateFlag(STATE_FLAG_ID);
            AbilityRequirement requirement = AbilityRequirements.stateFlagActive(STATE_FLAG_ID);

            assertTrue(requirement.validate(null, AbilityData.empty().withStateFlagSource(STATE_FLAG_ID, SOURCE_ID, true)).isEmpty());
            assertFalse(requirement.validate(null, AbilityData.empty()).isEmpty());
        } finally {
            StateFlagApi.unregisterStateFlag(STATE_FLAG_ID);
        }
    }

    @Test
    void detectorRequirementOnlyPassesWhileWindowIsActive() {
        AbilityDetectorApi.unregisterDetector(DETECTOR_ID);
        try {
            AbilityDetectorApi.registerDetector(AbilityDetectorDefinition.builder(DETECTOR_ID, 5)
                    .event(ReactiveEventType.HURT)
                    .build());
            AbilityRequirement requirement = AbilityRequirements.detectorActive(DETECTOR_ID);

            assertTrue(requirement.validate(null, AbilityData.empty().withDetectorWindow(DETECTOR_ID, 3)).isEmpty());
            assertFalse(requirement.validate(null, AbilityData.empty()).isEmpty());
        } finally {
            AbilityDetectorApi.unregisterDetector(DETECTOR_ID);
        }
    }

    @Test
    void identityAndArtifactUnlockRequirementsUseRegisteredFrameworkState() {
        IdentityApi.unregisterIdentity(IDENTITY_ID);
        ArtifactApi.unregisterArtifact(ARTIFACT_ID);
        try {
            IdentityApi.registerIdentity(IdentityDefinition.builder(IDENTITY_ID).build());
            ArtifactApi.registerArtifact(ArtifactDefinition.builder(ARTIFACT_ID)
                    .itemId(STICK_ID)
                    .unlockOnConsume()
                    .build());

            AbilityData data = AbilityData.empty()
                    .withStateFlagSource(IDENTITY_ID, SOURCE_ID, true)
                    .withArtifactUnlockSource(ARTIFACT_ID, SOURCE_ID, true);

            assertTrue(AbilityRequirements.identityActive(IDENTITY_ID).validate(null, data).isEmpty());
            assertTrue(AbilityRequirements.artifactUnlocked(ARTIFACT_ID).validate(null, data).isEmpty());
            assertFalse(AbilityRequirements.artifactUnlocked(id("artifact/missing")).validate(null, data).isEmpty());
        } finally {
            IdentityApi.unregisterIdentity(IDENTITY_ID);
            ArtifactApi.unregisterArtifact(ARTIFACT_ID);
        }
    }

    @Test
    void allAnyAndNotComposeExistingRequirements() {
        AbilityRequirement modeRequirement = AbilityRequirements.modeActive(MODE_ID);
        AbilityRequirement comboRequirement = AbilityRequirements.comboWindowActive(COMBO_ID);
        AbilityRequirement allRequirement = AbilityRequirements.all(modeRequirement, comboRequirement);
        AbilityRequirement anyRequirement = AbilityRequirements.any(modeRequirement, comboRequirement);
        AbilityRequirement notRequirement = AbilityRequirements.not(modeRequirement);

        AbilityData modeOnly = AbilityData.empty().withMode(MODE_ID, true);
        AbilityData complete = modeOnly.withComboWindow(COMBO_ID, 10);

        assertEquals(
                comboRequirement.validate(null, modeOnly).orElseThrow().getString(),
                allRequirement.validate(null, modeOnly).orElseThrow().getString()
        );
        assertTrue(anyRequirement.validate(null, modeOnly).isEmpty());
        assertTrue(allRequirement.validate(null, complete).isEmpty());
        assertFalse(notRequirement.validate(null, modeOnly).isEmpty());
        assertTrue(notRequirement.validate(null, AbilityData.empty()).isEmpty());
    }

    @Test
    void predicateDescriptionsCanResolveLazily() {
        AtomicReference<Component> description = new AtomicReference<>(Component.literal("First"));
        AbilityRequirement requirement = AbilityRequirements.predicate(description::get, (player, data) -> false);

        assertEquals("First", requirement.description().getString());
        assertEquals("First", requirement.validate(null, AbilityData.empty()).orElseThrow().getString());

        description.set(Component.literal("Second"));

        assertEquals("Second", requirement.description().getString());
        assertEquals("Second", requirement.validate(null, AbilityData.empty()).orElseThrow().getString());
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }
}
