package com.whatxe.xlib.ability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class StatePolicyApiTest {
    private static final ResourceLocation LOCKED_ABILITY_ID = id("ability/locked");
    private static final ResourceLocation SILENCED_ABILITY_ID = id("ability/silenced");
    private static final ResourceLocation SUPPRESSED_ABILITY_ID = id("ability/suppressed");
    private static final ResourceLocation UNTOUCHED_ABILITY_ID = id("ability/untouched");
    private static final ResourceLocation FAMILY_ID = id("family/forms");
    private static final ResourceLocation TAG_ID = id("tag/volatile");
    private static final ResourceLocation POLICY_ID = id("policy/control");
    private static final ResourceLocation FAST_POLICY_ID = id("policy/fast");
    private static final ResourceLocation SLOW_POLICY_ID = id("policy/slow");
    private static final ResourceLocation SOURCE_ID = id("source/context");
    private static final ResourceLocation STALE_POLICY_ID = id("policy/stale");

    @Test
    void selectorsResolveAndControlStatusReflectsActivePolicies() {
        unregisterFixtures();
        try {
            registerFixtureAbility(LOCKED_ABILITY_ID, null, Set.of());
            registerFixtureAbility(SILENCED_ABILITY_ID, FAMILY_ID, Set.of());
            registerFixtureAbility(SUPPRESSED_ABILITY_ID, null, Set.of(TAG_ID));
            registerFixtureAbility(UNTOUCHED_ABILITY_ID, null, Set.of());
            StatePolicyApi.registerStatePolicy(StatePolicyDefinition.builder(POLICY_ID)
                    .lock(AbilitySelector.ability(LOCKED_ABILITY_ID))
                    .silence(AbilitySelector.family(FAMILY_ID))
                    .suppress(AbilitySelector.tag(TAG_ID))
                    .build());

            AbilityData data = AbilityData.empty().withStatePolicySource(POLICY_ID, SOURCE_ID, true);

            assertEquals(Set.of(LOCKED_ABILITY_ID), StatePolicyApi.lockedAbilities(data));
            assertEquals(Set.of(SILENCED_ABILITY_ID), StatePolicyApi.silencedAbilities(data));
            assertEquals(Set.of(SUPPRESSED_ABILITY_ID), StatePolicyApi.suppressedAbilities(data));
            assertEquals(
                    Set.of(LOCKED_ABILITY_ID, SILENCED_ABILITY_ID, SUPPRESSED_ABILITY_ID),
                    StatePolicyApi.affectedAbilities(data)
            );

            StateControlStatus lockedStatus = StatePolicyApi.controlStatus(data, AbilityApi.findAbility(LOCKED_ABILITY_ID).orElseThrow());
            StateControlStatus silencedStatus = StatePolicyApi.controlStatus(data, AbilityApi.findAbility(SILENCED_ABILITY_ID).orElseThrow());
            StateControlStatus suppressedStatus = StatePolicyApi.controlStatus(data, AbilityApi.findAbility(SUPPRESSED_ABILITY_ID).orElseThrow());
            StateControlStatus untouchedStatus = StatePolicyApi.controlStatus(data, AbilityApi.findAbility(UNTOUCHED_ABILITY_ID).orElseThrow());

            assertTrue(lockedStatus.locked());
            assertTrue(lockedStatus.assignmentBlocked());
            assertFalse(lockedStatus.activationBlocked());
            assertTrue(silencedStatus.silenced());
            assertTrue(silencedStatus.activationBlocked());
            assertTrue(suppressedStatus.suppressed());
            assertTrue(suppressedStatus.activationBlocked());
            assertFalse(untouchedStatus.activationBlocked());
        } finally {
            unregisterFixtures();
        }
    }

    @Test
    void cooldownMultiplierStacksAcrossPoliciesAndSanitizeRemovesStaleOnes() {
        unregisterFixtures();
        try {
            StatePolicyApi.registerStatePolicy(StatePolicyDefinition.builder(FAST_POLICY_ID)
                    .cooldownTickRateMultiplier(0.5D)
                    .build());
            StatePolicyApi.registerStatePolicy(StatePolicyDefinition.builder(SLOW_POLICY_ID)
                    .cooldownTickRateMultiplier(0.8D)
                    .build());

            AbilityData data = AbilityData.empty()
                    .withStatePolicySource(FAST_POLICY_ID, SOURCE_ID, true)
                    .withStatePolicySource(SLOW_POLICY_ID, SOURCE_ID, true)
                    .withStatePolicySource(STALE_POLICY_ID, SOURCE_ID, true);

            assertEquals(0.4D, StatePolicyApi.cooldownTickRateMultiplier(data), 0.0001D);

            AbilityData sanitized = AbilityApi.sanitizeData(data);

            assertTrue(sanitized.hasStatePolicy(FAST_POLICY_ID));
            assertTrue(sanitized.hasStatePolicy(SLOW_POLICY_ID));
            assertFalse(sanitized.hasStatePolicy(STALE_POLICY_ID));
        } finally {
            unregisterFixtures();
        }
    }

    private static void registerFixtureAbility(
            ResourceLocation abilityId,
            ResourceLocation familyId,
            Set<ResourceLocation> tags
    ) {
        AbilityDefinition.Builder builder = AbilityDefinition.builder(abilityId, AbilityIcon.ofTexture(id("icon/" + abilityId.getPath())))
                .action((player, data) -> AbilityUseResult.success(data));
        if (familyId != null) {
            builder.family(familyId);
        }
        for (ResourceLocation tagId : tags) {
            builder.tag(tagId);
        }
        AbilityApi.registerAbility(builder.build());
    }

    private static void unregisterFixtures() {
        AbilityApi.unregisterAbility(LOCKED_ABILITY_ID);
        AbilityApi.unregisterAbility(SILENCED_ABILITY_ID);
        AbilityApi.unregisterAbility(SUPPRESSED_ABILITY_ID);
        AbilityApi.unregisterAbility(UNTOUCHED_ABILITY_ID);
        StatePolicyApi.unregisterStatePolicy(POLICY_ID);
        StatePolicyApi.unregisterStatePolicy(FAST_POLICY_ID);
        StatePolicyApi.unregisterStatePolicy(SLOW_POLICY_ID);
        StatePolicyApi.unregisterStatePolicy(STALE_POLICY_ID);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }
}
