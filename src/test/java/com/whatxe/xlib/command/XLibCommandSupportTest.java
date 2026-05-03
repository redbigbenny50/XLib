package com.whatxe.xlib.command;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.whatxe.xlib.ability.AbilityData;
import com.whatxe.xlib.ability.AbilityApi;
import com.whatxe.xlib.ability.AbilityDetectorApi;
import com.whatxe.xlib.ability.AbilityDetectorDefinition;
import com.whatxe.xlib.ability.AbilityDefinition;
import com.whatxe.xlib.ability.AbilityIcon;
import com.whatxe.xlib.ability.AbilityRequirement;
import com.whatxe.xlib.ability.AbilitySelector;
import com.whatxe.xlib.ability.AbilityUseResult;
import com.whatxe.xlib.ability.ArtifactApi;
import com.whatxe.xlib.ability.ArtifactDefinition;
import com.whatxe.xlib.ability.GrantBundleApi;
import com.whatxe.xlib.ability.GrantBundleDefinition;
import com.whatxe.xlib.ability.IdentityApi;
import com.whatxe.xlib.ability.IdentityDefinition;
import com.whatxe.xlib.ability.ModeDefinition;
import com.whatxe.xlib.ability.PassiveDefinition;
import com.whatxe.xlib.ability.PassiveSoundTrigger;
import com.whatxe.xlib.ability.ProfileApi;
import com.whatxe.xlib.ability.ProfileDefinition;
import com.whatxe.xlib.ability.ProfileGroupDefinition;
import com.whatxe.xlib.ability.ProfileOnboardingTrigger;
import com.whatxe.xlib.ability.ProfilePendingSelection;
import com.whatxe.xlib.ability.ProfileSelectionData;
import com.whatxe.xlib.ability.ProfileSelectionEntry;
import com.whatxe.xlib.ability.ProfileSelectionOrigin;
import com.whatxe.xlib.ability.ReactiveEventType;
import com.whatxe.xlib.ability.StateFlagApi;
import com.whatxe.xlib.ability.StatePolicyApi;
import com.whatxe.xlib.ability.StatePolicyDefinition;
import com.whatxe.xlib.ability.SupportPackageApi;
import com.whatxe.xlib.ability.SupportPackageDefinition;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class XLibCommandSupportTest {
    private static final ResourceLocation PASSIVE_ID = id("passive/stateful");
    private static final ResourceLocation MODE_ID = id("mode/storm");
    private static final ResourceLocation OVERLAY_ID = id("ability/overlay");
    private static final ResourceLocation GRANTED_ABILITY_ID = id("ability/granted");
    private static final ResourceLocation GRANTED_PASSIVE_ID = id("passive/granted");
    private static final ResourceLocation GRANTED_ITEM_ID = id("item/granted");
    private static final ResourceLocation GRANTED_RECIPE_ID = id("recipe/granted");
    private static final ResourceLocation BLOCKED_ABILITY_ID = id("ability/blocked");
    private static final ResourceLocation BLOCKED_BY_MODE_ID = id("mode/anti_storm");
    private static final ResourceLocation EXCLUSIVE_MODE_ID = id("mode/exclusive");
    private static final ResourceLocation TRANSFORM_PARENT_ID = id("mode/base");
    private static final ResourceLocation CYCLE_GROUP_ID = id("cycle/core");
    private static final ResourceLocation RESOURCE_ID = id("resource/focus");
    private static final ResourceLocation STATE_POLICY_ID = id("state/policy");
    private static final ResourceLocation STATE_FLAG_ID = id("state/flag");
    private static final ResourceLocation SOURCE_ID = id("source/context");
    private static final ResourceLocation FAMILY_ID = id("family/test");
    private static final ResourceLocation GROUP_ID = id("group/test");
    private static final ResourceLocation PAGE_ID = id("page/test");
    private static final ResourceLocation TAG_ID = id("tag/test");
    private static final ResourceLocation DETECTOR_ID = id("detector/counter");
    private static final ResourceLocation GRANT_BUNDLE_ID = id("bundle/shared");
    private static final ResourceLocation IDENTITY_ID = id("identity/origin");
    private static final ResourceLocation SUPPORT_PACKAGE_ID = id("support/guardian");
    private static final ResourceLocation ARTIFACT_ID = id("artifact/relic");
    private static final ResourceLocation PROFILE_GROUP_ID = id("profile_group/core");
    private static final ResourceLocation PROFILE_ID = id("profile/ember");
    private static final ResourceLocation STICK_ID = ResourceLocation.withDefaultNamespace("stick");
    private static final ResourceLocation LOCKED_ABILITY_ID = id("ability/locked");
    private static final ResourceLocation SILENCED_ABILITY_ID = id("ability/silenced");
    private static final ResourceLocation SUPPRESSED_ABILITY_ID = id("ability/suppressed");
    private static final UUID SUPPORTER_ID = UUID.fromString("00000000-0000-0000-0000-000000000321");

    @Test
    void passiveStateLineIncludesMetadataStatusesHooksAndSources() {
        AbilityRequirement grantRequirement = AbilityRequirement.of(
                Component.literal("Grant blocked"),
                (player, data) -> data.isModeActive(MODE_ID) ? Optional.empty() : Optional.of(Component.literal("Grant blocked"))
        );
        AbilityRequirement activeRequirement = AbilityRequirement.of(
                Component.literal("Need more focus"),
                (player, data) -> data.resourceAmount(RESOURCE_ID) >= 5 ? Optional.empty() : Optional.of(Component.literal("Need more focus"))
        );
        PassiveDefinition passive = PassiveDefinition.builder(PASSIVE_ID, AbilityIcon.ofTexture(id("icon/passive")))
                .family(FAMILY_ID)
                .group(GROUP_ID)
                .page(PAGE_ID)
                .tag(TAG_ID)
                .grantRequirement(grantRequirement)
                .activeRequirement(activeRequirement)
                .ticker((player, data) -> data)
                .sound(PassiveSoundTrigger.GRANTED, id("sound/granted"))
                .build();
        AbilityData data = AbilityData.empty()
                .withMode(MODE_ID, true)
                .withResourceAmount(RESOURCE_ID, 2);

        String line = XLibCommandSupport.formatPassiveStateLine(null, data, passive, Set.of(SOURCE_ID));

        assertTrue(line.contains(PASSIVE_ID.toString()));
        assertTrue(line.contains("metadata={family=" + FAMILY_ID));
        assertTrue(line.contains("grant=ok"));
        assertTrue(line.contains("active=Need more focus"));
        assertTrue(line.contains("hooks=tick"));
        assertTrue(line.contains("sound_triggers=granted"));
        assertTrue(line.contains("sources=" + SOURCE_ID));
    }

    @Test
    void modeStateLineIncludesBundlesOverlaysAndUpkeep() {
        ModeDefinition mode = ModeDefinition.builder(MODE_ID)
                .family(FAMILY_ID)
                .group(GROUP_ID)
                .page(PAGE_ID)
                .tag(TAG_ID)
                .priority(7)
                .stackable()
                .orderedCycle(CYCLE_GROUP_ID, 2)
                .resetCycleGroupOnActivate(id("cycle/reset"))
                .cooldownTickRateMultiplier(0.5D)
                .healthCostPerTick(1.25D)
                .minimumHealth(4.0D)
                .resourceDeltaPerTick(RESOURCE_ID, -2.0D)
                .overlayAbility(0, OVERLAY_ID)
                .grantAbility(GRANTED_ABILITY_ID)
                .grantPassive(GRANTED_PASSIVE_ID)
                .grantGrantedItem(GRANTED_ITEM_ID)
                .grantRecipePermission(GRANTED_RECIPE_ID)
                .statePolicy(STATE_POLICY_ID)
                .stateFlag(STATE_FLAG_ID)
                .blockAbility(BLOCKED_ABILITY_ID)
                .blockedByMode(BLOCKED_BY_MODE_ID)
                .exclusiveWith(EXCLUSIVE_MODE_ID)
                .transformsFrom(TRANSFORM_PARENT_ID)
                .build();
        AbilityData data = AbilityData.empty()
                .withMode(MODE_ID, true)
                .withActiveDuration(MODE_ID, 100);

        String line = XLibCommandSupport.formatModeStateLine(data, mode);

        assertTrue(line.contains(MODE_ID.toString()));
        assertTrue(line.contains("duration=5.0s"));
        assertTrue(line.contains("metadata={family=" + FAMILY_ID));
        assertTrue(line.contains("priority=7"));
        assertTrue(line.contains("stackable=true"));
        assertTrue(line.contains("cycle=" + CYCLE_GROUP_ID + "#2"));
        assertTrue(line.contains("overlays=1:" + OVERLAY_ID));
        assertTrue(line.contains("health=1.25"));
        assertTrue(line.contains("min_health=4.00"));
        assertTrue(line.contains("resources=" + RESOURCE_ID + "=-2.00"));
        assertTrue(line.contains(GRANTED_ABILITY_ID.toString()));
        assertTrue(line.contains(GRANTED_PASSIVE_ID.toString()));
        assertTrue(line.contains(GRANTED_ITEM_ID.toString()));
        assertTrue(line.contains(GRANTED_RECIPE_ID.toString()));
        assertTrue(line.contains("state_policies=" + STATE_POLICY_ID));
        assertTrue(line.contains("state_flags=" + STATE_FLAG_ID));
        assertTrue(line.contains("blocks=" + BLOCKED_ABILITY_ID));
        assertTrue(line.contains("blocked_by=" + BLOCKED_BY_MODE_ID));
        assertTrue(line.contains("exclusive=" + EXCLUSIVE_MODE_ID));
        assertTrue(line.contains("transforms_from=" + TRANSFORM_PARENT_ID));
    }

    @Test
    void statePolicyLineAndSourceGroupsIncludeResolvedAbilitiesAndSources() {
        unregisterFixtures();
        try {
            AbilityApi.registerAbility(AbilityDefinition.builder(LOCKED_ABILITY_ID, AbilityIcon.ofTexture(id("icon/locked")))
                    .action((player, data) -> AbilityUseResult.success(data))
                    .build());
            AbilityApi.registerAbility(AbilityDefinition.builder(SILENCED_ABILITY_ID, AbilityIcon.ofTexture(id("icon/silenced")))
                    .family(FAMILY_ID)
                    .action((player, data) -> AbilityUseResult.success(data))
                    .build());
            AbilityApi.registerAbility(AbilityDefinition.builder(SUPPRESSED_ABILITY_ID, AbilityIcon.ofTexture(id("icon/suppressed")))
                    .tag(TAG_ID)
                    .action((player, data) -> AbilityUseResult.success(data))
                    .build());
            StatePolicyApi.registerStatePolicy(StatePolicyDefinition.builder(STATE_POLICY_ID)
                    .lock(AbilitySelector.ability(LOCKED_ABILITY_ID))
                    .silence(AbilitySelector.family(FAMILY_ID))
                    .suppress(AbilitySelector.tag(TAG_ID))
                    .build());
            StateFlagApi.registerStateFlag(STATE_FLAG_ID);

            AbilityData data = AbilityData.empty()
                    .withStatePolicySource(STATE_POLICY_ID, SOURCE_ID, true)
                    .withStateFlagSource(STATE_FLAG_ID, SOURCE_ID, true);

            String line = XLibCommandSupport.formatStatePolicyLine(data, StatePolicyApi.findStatePolicy(STATE_POLICY_ID).orElseThrow());
            String stateFlagLine = XLibCommandSupport.buildStateFlagLines(data).iterator().next();
            XLibCommandSupport.SourceGroup sourceGroup = XLibCommandSupport.buildSourceGroups(data).get(SOURCE_ID);

            assertTrue(line.contains(STATE_POLICY_ID.toString()));
            assertTrue(line.contains("locked=" + LOCKED_ABILITY_ID));
            assertTrue(line.contains("silenced=" + SILENCED_ABILITY_ID));
            assertTrue(line.contains("suppressed=" + SUPPRESSED_ABILITY_ID));
            assertTrue(line.contains("sources=" + SOURCE_ID));
            assertTrue(stateFlagLine.contains(STATE_FLAG_ID.toString()));
            assertTrue(stateFlagLine.contains("sources=" + SOURCE_ID));
            assertTrue(XLibCommandSupport.formatSourceGroups(data).contains(STATE_POLICY_ID.toString()));
            assertTrue(XLibCommandSupport.formatSourceGroups(data).contains(STATE_FLAG_ID.toString()));
            assertTrue(sourceGroup.statePolicies().contains(STATE_POLICY_ID));
            assertTrue(sourceGroup.stateFlags().contains(STATE_FLAG_ID));
        } finally {
            unregisterFixtures();
        }
    }

    @Test
    void identityBundleLinesAndSourceDescriptorsExposeOwnershipDetails() {
        unregisterFixtures();
        try {
            GrantBundleApi.registerBundle(GrantBundleDefinition.builder(GRANT_BUNDLE_ID)
                    .grantAbility(GRANTED_ABILITY_ID)
                    .build());
            IdentityApi.registerIdentity(IdentityDefinition.builder(IDENTITY_ID)
                    .grantBundle(GRANT_BUNDLE_ID)
                    .build());

            ResourceLocation projectionSourceId = IdentityApi.projectionSourceIdFor(SOURCE_ID);
            AbilityData data = AbilityData.empty()
                    .withStateFlagSource(IDENTITY_ID, SOURCE_ID, true)
                    .withGrantBundleSource(GRANT_BUNDLE_ID, projectionSourceId, true);

            String identityLine = XLibCommandSupport.buildIdentityStateLines(data).iterator().next();
            String bundleLine = XLibCommandSupport.buildGrantBundleStateLines(data).iterator().next();
            XLibCommandSupport.SourceGroup sourceGroup = XLibCommandSupport.buildSourceGroups(data).get(SOURCE_ID);
            XLibCommandSupport.SourceGroup projectionGroup = XLibCommandSupport.buildSourceGroups(data).get(projectionSourceId);
            String identityDescriptor = XLibCommandSupport.formatSourceDescriptor(XLibCommandSupport.buildSourceDescriptors(data).get(SOURCE_ID));
            String projectionDescriptor =
                    XLibCommandSupport.formatSourceDescriptor(XLibCommandSupport.buildSourceDescriptors(data).get(projectionSourceId));

            assertTrue(identityLine.contains(IDENTITY_ID.toString()));
            assertTrue(identityLine.contains("bundles=" + GRANT_BUNDLE_ID));
            assertTrue(identityLine.contains("sources=" + SOURCE_ID));
            assertTrue(bundleLine.contains(GRANT_BUNDLE_ID.toString()));
            assertTrue(bundleLine.contains("abilities=" + GRANTED_ABILITY_ID));
            assertTrue(bundleLine.contains("sources=" + projectionSourceId));
            assertTrue(sourceGroup.identities().contains(IDENTITY_ID));
            assertTrue(projectionGroup.grantBundles().contains(GRANT_BUNDLE_ID));
            assertTrue(XLibCommandSupport.formatSourceGroups(data).contains(IDENTITY_ID.toString()));
            assertTrue(XLibCommandSupport.formatSourceGroups(data).contains(GRANT_BUNDLE_ID.toString()));
            assertTrue(identityDescriptor.contains("kind=identity"));
            assertTrue(identityDescriptor.contains("grant_bundles=" + GRANT_BUNDLE_ID));
            assertTrue(projectionDescriptor.contains("kind=identity_projection"));
            assertTrue(projectionDescriptor.contains("backing_source=" + SOURCE_ID));
        } finally {
            unregisterFixtures();
        }
    }

    @Test
    void supportPackageSourcesExposePackageAndGrantorDetails() {
        unregisterFixtures();
        try {
            GrantBundleApi.registerBundle(GrantBundleDefinition.builder(GRANT_BUNDLE_ID)
                    .grantAbility(GRANTED_ABILITY_ID)
                    .build());
            SupportPackageApi.registerSupportPackage(SupportPackageDefinition.builder(SUPPORT_PACKAGE_ID)
                    .grantBundle(GRANT_BUNDLE_ID)
                    .build());

            ResourceLocation supportSourceId = SupportPackageApi.sourceIdFor(SUPPORTER_ID, SUPPORT_PACKAGE_ID);
            AbilityData data = AbilityData.empty()
                    .withGrantBundleSource(GRANT_BUNDLE_ID, supportSourceId, true);

            String descriptor = XLibCommandSupport.formatSourceDescriptor(XLibCommandSupport.buildSourceDescriptors(data).get(supportSourceId));

            assertTrue(descriptor.contains("kind=support_package"));
            assertTrue(descriptor.contains("primary=" + SUPPORT_PACKAGE_ID));
            assertTrue(descriptor.contains("grant_bundles=" + GRANT_BUNDLE_ID));
            assertTrue(descriptor.contains("grantor=" + SUPPORTER_ID));
        } finally {
            unregisterFixtures();
        }
    }

    @Test
    void artifactUnlockSourcesExposeArtifactDetails() {
        unregisterFixtures();
        try {
            GrantBundleApi.registerBundle(GrantBundleDefinition.builder(GRANT_BUNDLE_ID)
                    .grantAbility(GRANTED_ABILITY_ID)
                    .build());
            ArtifactApi.registerArtifact(ArtifactDefinition.builder(ARTIFACT_ID)
                    .itemId(STICK_ID)
                    .unlockedBundle(GRANT_BUNDLE_ID)
                    .unlockOnConsume()
                    .build());

            ResourceLocation artifactSourceId = ArtifactApi.unlockSourceIdFor(ARTIFACT_ID);
            AbilityData data = AbilityData.empty()
                    .withArtifactUnlockSource(ARTIFACT_ID, artifactSourceId, true)
                    .withGrantBundleSource(GRANT_BUNDLE_ID, artifactSourceId, true);

            String descriptor = XLibCommandSupport.formatSourceDescriptor(XLibCommandSupport.buildSourceDescriptors(data).get(artifactSourceId));

            assertTrue(descriptor.contains("kind=artifact_unlock"));
            assertTrue(descriptor.contains("primary=" + ARTIFACT_ID));
            assertTrue(descriptor.contains("grant_bundles=" + GRANT_BUNDLE_ID));
            assertTrue(XLibCommandSupport.buildSourceGroups(data).get(artifactSourceId).unlockedArtifacts().contains(ARTIFACT_ID));
        } finally {
            unregisterFixtures();
        }
    }

    @Test
    void detectorAndArtifactStateLinesExposeRuntimeDetails() {
        unregisterFixtures();
        try {
            AbilityDetectorApi.registerDetector(AbilityDetectorDefinition.builder(DETECTOR_ID, 8)
                    .event(ReactiveEventType.HURT)
                    .build());
            ArtifactApi.registerArtifact(ArtifactDefinition.builder(ARTIFACT_ID)
                    .itemId(STICK_ID)
                    .presence(com.whatxe.xlib.ability.ArtifactPresenceMode.MAIN_HAND)
                    .unlockOnConsume()
                    .build());

            AbilityData data = AbilityData.empty()
                    .withDetectorWindow(DETECTOR_ID, 4)
                    .withArtifactUnlockSource(ARTIFACT_ID, SOURCE_ID, true);

            String detectorLine = XLibCommandSupport.buildDetectorStateLines(data).iterator().next();
            String artifactLine = XLibCommandSupport.buildArtifactStateLines(null, data).iterator().next();

            assertTrue(detectorLine.contains(DETECTOR_ID.toString()));
            assertTrue(detectorLine.contains("remaining_ticks=4"));
            assertTrue(detectorLine.contains("duration_ticks=8"));
            assertTrue(detectorLine.contains("events=hurt"));
            assertTrue(artifactLine.contains(ARTIFACT_ID.toString()));
            assertTrue(artifactLine.contains("active=false"));
            assertTrue(artifactLine.contains("unlocked=true"));
            assertTrue(artifactLine.contains("unlock_sources=" + SOURCE_ID));
        } finally {
            unregisterFixtures();
        }
    }

    @Test
    void profileSelectionLinesExposeOriginLockAndPendingTriggerDetails() {
        unregisterFixtures();
        try {
            ProfileApi.registerGroup(ProfileGroupDefinition.builder(PROFILE_GROUP_ID)
                    .displayName(Component.literal("Core"))
                    .requiredOnboarding()
                    .onboardingTrigger(ProfileOnboardingTrigger.FIRST_LOGIN)
                    .build());
            ProfileApi.registerProfile(ProfileDefinition.builder(PROFILE_ID, PROFILE_GROUP_ID, AbilityIcon.ofTexture(id("icon/profile")))
                    .displayName(Component.literal("Ember"))
                    .build());

            ProfileSelectionData data = new ProfileSelectionData(
                    java.util.Map.of(PROFILE_ID, new ProfileSelectionEntry(PROFILE_GROUP_ID, ProfileSelectionOrigin.ADMIN, true, "admin command")),
                    java.util.Map.of(PROFILE_GROUP_ID, new ProfilePendingSelection(ProfileOnboardingTrigger.FIRST_LOGIN, "first_login")),
                    java.util.Map.of(PROFILE_GROUP_ID, 2),
                    java.util.Map.of(PROFILE_GROUP_ID, "rerolled"),
                    true
            );

            String selectionLine = XLibCommandSupport.buildProfileSelectionLines(data).iterator().next();
            String pendingLine = XLibCommandSupport.buildPendingProfileGroupLines(data).iterator().next();

            assertTrue(selectionLine.contains(PROFILE_ID.toString()));
            assertTrue(selectionLine.contains("origin=admin"));
            assertTrue(selectionLine.contains("locked=true"));
            assertTrue(selectionLine.contains("admin_set=true"));
            assertTrue(selectionLine.contains("group=Core"));
            assertTrue(pendingLine.contains(PROFILE_GROUP_ID.toString()));
            assertTrue(pendingLine.contains("trigger=first_login"));
            assertTrue(XLibCommandSupport.formatStringMap(data.lastResetReasons()).contains("rerolled"));
        } finally {
            unregisterFixtures();
        }
    }

    private static void unregisterFixtures() {
        AbilityApi.unregisterAbility(LOCKED_ABILITY_ID);
        AbilityApi.unregisterAbility(SILENCED_ABILITY_ID);
        AbilityApi.unregisterAbility(SUPPRESSED_ABILITY_ID);
        AbilityDetectorApi.unregisterDetector(DETECTOR_ID);
        ArtifactApi.unregisterArtifact(ARTIFACT_ID);
        GrantBundleApi.unregisterBundle(GRANT_BUNDLE_ID);
        IdentityApi.unregisterIdentity(IDENTITY_ID);
        SupportPackageApi.unregisterSupportPackage(SUPPORT_PACKAGE_ID);
        ProfileApi.unregisterProfile(PROFILE_ID);
        ProfileApi.unregisterGroup(PROFILE_GROUP_ID);
        StateFlagApi.unregisterStateFlag(STATE_FLAG_ID);
        StatePolicyApi.unregisterStatePolicy(STATE_POLICY_ID);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }
}
