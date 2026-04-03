package com.whatxe.xlib.ability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class AbilityDataTest {
    private static final ResourceLocation ABILITY_ID = id("dash");
    private static final ResourceLocation GRANTED_ITEM_ID = id("fang");
    private static final ResourceLocation RECIPE_ID = id("fang_recipe");
    private static final ResourceLocation SOURCE_A = id("source_a");
    private static final ResourceLocation SOURCE_B = id("source_b");

    @Test
    void grantSourcesRemainIndependent() {
        AbilityData data = AbilityData.empty()
                .withAbilityGrantSource(ABILITY_ID, SOURCE_A, true)
                .withAbilityGrantSource(ABILITY_ID, SOURCE_B, true)
                .withAbilityAccessRestricted(true);

        AbilityData revoked = data.withAbilityGrantSource(ABILITY_ID, SOURCE_A, false);

        assertTrue(revoked.canUseAbility(ABILITY_ID));
        assertEquals(1, revoked.abilityGrantSourcesFor(ABILITY_ID).size());
        assertTrue(revoked.abilityGrantSourcesFor(ABILITY_ID).contains(SOURCE_B));
    }

    @Test
    void activationBlockSourcesRemainIndependent() {
        AbilityData data = AbilityData.empty()
                .withAbilityActivationBlockSource(ABILITY_ID, SOURCE_A, true)
                .withAbilityActivationBlockSource(ABILITY_ID, SOURCE_B, true);

        AbilityData partiallyUnblocked = data.withAbilityActivationBlockSource(ABILITY_ID, SOURCE_A, false);
        AbilityData fullyUnblocked = partiallyUnblocked.withAbilityActivationBlockSource(ABILITY_ID, SOURCE_B, false);

        assertTrue(partiallyUnblocked.isAbilityActivationBlocked(ABILITY_ID));
        assertEquals(1, partiallyUnblocked.activationBlockSourcesFor(ABILITY_ID).size());
        assertTrue(partiallyUnblocked.activationBlockSourcesFor(ABILITY_ID).contains(SOURCE_B));
        assertFalse(fullyUnblocked.isAbilityActivationBlocked(ABILITY_ID));
    }

    @Test
    void sanitizeClearsRestrictedSlotsWithoutGrants() {
        AbilityData restrictedData = AbilityData.empty()
                .withAbilityAccessRestricted(true)
                .withAbilityInSlot(0, ABILITY_ID)
                .withMode(ABILITY_ID, true);

        AbilityData sanitized = AbilityGrantApi.sanitize(restrictedData);

        assertTrue(sanitized.abilityInSlot(0).isEmpty());
        assertFalse(sanitized.isModeActive(ABILITY_ID));
    }

    @Test
    void grantedItemSourcesRemainIndependent() {
        AbilityData data = AbilityData.empty()
                .withGrantedItemSource(GRANTED_ITEM_ID, SOURCE_A, true)
                .withGrantedItemSource(GRANTED_ITEM_ID, SOURCE_B, true);

        AbilityData revoked = data.withGrantedItemSource(GRANTED_ITEM_ID, SOURCE_A, false);

        assertTrue(revoked.hasGrantedItem(GRANTED_ITEM_ID));
        assertEquals(1, revoked.grantedItemSourcesFor(GRANTED_ITEM_ID).size());
        assertTrue(revoked.grantedItemSourcesFor(GRANTED_ITEM_ID).contains(SOURCE_B));
    }

    @Test
    void recipePermissionSourcesRemainIndependent() {
        AbilityData data = AbilityData.empty()
                .withRecipePermissionSource(RECIPE_ID, SOURCE_A, true)
                .withRecipePermissionSource(RECIPE_ID, SOURCE_B, true);

        AbilityData revoked = data.withRecipePermissionSource(RECIPE_ID, SOURCE_A, false);

        assertTrue(revoked.hasRecipePermission(RECIPE_ID));
        assertEquals(1, revoked.recipePermissionSourcesFor(RECIPE_ID).size());
        assertTrue(revoked.recipePermissionSourcesFor(RECIPE_ID).contains(SOURCE_B));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }
}

