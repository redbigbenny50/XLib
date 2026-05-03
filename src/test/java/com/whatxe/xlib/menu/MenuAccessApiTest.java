package com.whatxe.xlib.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.whatxe.xlib.ability.AbilityData;
import com.whatxe.xlib.progression.UpgradeProgressData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class MenuAccessApiTest {
    private static final ResourceLocation LOCKED_POLICY_ID = id("locked_policy");
    private static final ResourceLocation HIDDEN_POLICY_ID = id("hidden_policy");
    private static final ResourceLocation POINT_TYPE_ID = id("menu_points");

    @AfterEach
    void clearPolicies() {
        AbilityMenuAccessApi.clearPolicies();
        ProgressionMenuAccessApi.clearPolicies();
    }

    @Test
    void abilityMenuPoliciesEscalateFromLockedToHidden() {
        AbilityMenuAccessApi.registerPolicy(
                LOCKED_POLICY_ID,
                AbilityMenuAccessPolicy.builder()
                        .availableWhen(MenuAccessRequirements.predicate(Component.literal("Need unlock"), (player, data) -> false))
                        .build()
        );

        MenuAccessDecision lockedDecision = AbilityMenuAccessApi.decision(null, AbilityData.empty());
        assertEquals(MenuAccessState.LOCKED, lockedDecision.state());
        assertEquals("Need unlock", lockedDecision.reason().orElseThrow().getString());

        AbilityMenuAccessApi.registerPolicy(
                HIDDEN_POLICY_ID,
                AbilityMenuAccessPolicy.builder()
                        .visibleWhen(MenuAccessRequirements.predicate(Component.literal("Need visibility"), (player, data) -> false))
                        .build()
        );

        MenuAccessDecision hiddenDecision = AbilityMenuAccessApi.decision(null, AbilityData.empty());
        assertEquals(MenuAccessState.HIDDEN, hiddenDecision.state());
        assertEquals("Need visibility", hiddenDecision.reason().orElseThrow().getString());
    }

    @Test
    void progressionMenuPoliciesUseClientSafeProgressionRequirements() {
        ProgressionMenuAccessApi.registerPolicy(
                LOCKED_POLICY_ID,
                ProgressionMenuAccessPolicy.builder()
                        .availableWhen(ProgressionMenuRequirements.pointsAtLeast(POINT_TYPE_ID, 3))
                        .build()
        );

        assertEquals(MenuAccessState.LOCKED, ProgressionMenuAccessApi.decision(null, UpgradeProgressData.empty()).state());
        assertEquals(
                MenuAccessState.AVAILABLE,
                ProgressionMenuAccessApi.decision(null, UpgradeProgressData.empty().withPoints(POINT_TYPE_ID, 3)).state()
        );
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }
}
