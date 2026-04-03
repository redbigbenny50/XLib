package com.whatxe.xlib.ability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class ModeApiTest {
    private static final ResourceLocation BASE_ABILITY_ID = id("base_ability");
    private static final ResourceLocation LOW_PRIORITY_MODE_ID = id("low_priority_mode");
    private static final ResourceLocation HIGH_PRIORITY_MODE_ID = id("high_priority_mode");
    private static final ResourceLocation LOW_PRIORITY_OVERLAY_ID = id("low_priority_overlay");
    private static final ResourceLocation HIGH_PRIORITY_OVERLAY_ID = id("high_priority_overlay");
    private static final ResourceLocation BUNDLE_MODE_ID = id("bundle_mode");
    private static final ResourceLocation GRANTED_ABILITY_ID = id("granted_ability");
    private static final ResourceLocation PASSIVE_ID = id("passive");
    private static final ResourceLocation GRANTED_ITEM_ID = id("granted_item");
    private static final ResourceLocation RECIPE_ID = id("recipe");
    private static final ResourceLocation BLOCKED_ABILITY_ID = id("blocked_ability");

    @Test
    void higherPriorityModeOverlayWinsWhenMultipleModesShareTheSameSlot() {
        unregisterFixtures();
        try {
            ModeApi.registerMode(ModeDefinition.builder(LOW_PRIORITY_MODE_ID)
                    .priority(1)
                    .overlayAbility(0, LOW_PRIORITY_OVERLAY_ID)
                    .build());
            ModeApi.registerMode(ModeDefinition.builder(HIGH_PRIORITY_MODE_ID)
                    .priority(10)
                    .overlayAbility(0, HIGH_PRIORITY_OVERLAY_ID)
                    .build());

            AbilityData data = AbilityData.empty()
                    .withAbilityInSlot(0, BASE_ABILITY_ID)
                    .withMode(LOW_PRIORITY_MODE_ID, true)
                    .withMode(HIGH_PRIORITY_MODE_ID, true);

            assertEquals(HIGH_PRIORITY_OVERLAY_ID, ModeApi.resolveOverlayAbility(data, 0).orElseThrow());
            assertEquals(HIGH_PRIORITY_OVERLAY_ID, AbilityLoadoutApi.resolvedAbilityId(data, 0).orElseThrow());
        } finally {
            unregisterFixtures();
        }
    }

    @Test
    void activeModeSnapshotsExposeTheirBundledGrantsAndBlocks() {
        unregisterFixtures();
        try {
            ModeDefinition mode = ModeApi.registerMode(ModeDefinition.builder(BUNDLE_MODE_ID)
                    .grantAbility(GRANTED_ABILITY_ID)
                    .grantPassive(PASSIVE_ID)
                    .grantGrantedItem(GRANTED_ITEM_ID)
                    .grantRecipePermission(RECIPE_ID)
                    .blockAbility(BLOCKED_ABILITY_ID)
                    .build());

            List<ContextGrantSnapshot> snapshots = ModeApi.collectSnapshots(
                    AbilityData.empty().withMode(BUNDLE_MODE_ID, true)
            );

            assertEquals(1, snapshots.size());
            ContextGrantSnapshot snapshot = snapshots.get(0);
            assertEquals(mode.sourceId(), snapshot.sourceId());
            assertTrue(snapshot.abilities().contains(GRANTED_ABILITY_ID));
            assertTrue(snapshot.passives().contains(PASSIVE_ID));
            assertTrue(snapshot.grantedItems().contains(GRANTED_ITEM_ID));
            assertTrue(snapshot.recipePermissions().contains(RECIPE_ID));
            assertTrue(snapshot.blockedAbilities().contains(BLOCKED_ABILITY_ID));
        } finally {
            unregisterFixtures();
        }
    }

    private static void unregisterFixtures() {
        ModeApi.unregisterMode(LOW_PRIORITY_MODE_ID);
        ModeApi.unregisterMode(HIGH_PRIORITY_MODE_ID);
        ModeApi.unregisterMode(BUNDLE_MODE_ID);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }
}
