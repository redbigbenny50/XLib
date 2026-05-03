package com.whatxe.xlib.capability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class CapabilityPolicyApiTest {
    private static final ResourceLocation POLICY_ID = id("policy/beast_form");
    private static final ResourceLocation POLICY_ID_2 = id("policy/underwater_form");
    private static final ResourceLocation STALE_POLICY_ID = id("policy/stale");
    private static final ResourceLocation SOURCE_A = id("source/form_system");
    private static final ResourceLocation SOURCE_B = id("source/artifact_system");

    @Test
    void noPoliciesYieldsUnrestricted() {
        CapabilityPolicyData data = CapabilityPolicyData.empty();
        ResolvedCapabilityPolicyState resolved = CapabilityPolicyApi.resolved(data);
        for (CapabilityCheck check : CapabilityCheck.values()) {
            assertTrue(resolved.allows(check), "Expected " + check + " to be allowed with no active policies");
        }
    }

    @Test
    void singlePolicyRestrictsCorrectly() {
        unregisterFixtures();
        try {
            CapabilityPolicyApi.register(CapabilityPolicyDefinition.builder(POLICY_ID)
                    .inventory(InventoryPolicy.builder().canOpenInventory(false).canMoveItems(false).build())
                    .equipment(EquipmentPolicy.builder().canEquipArmor(false).canUnequipArmor(false).build())
                    .heldItems(HeldItemPolicy.builder().canBreakBlocks(false).canPlaceBlocks(false).canUseTools(false).build())
                    .crafting(CraftingPolicy.builder().canUsePlayerCrafting(false).canUseCraftingTable(false).build())
                    .containers(ContainerPolicy.builder().canOpenBrewingStands(false).build())
                    .interaction(InteractionPolicy.builder().canUseBeds(false).canRideEntities(false).build())
                    .build());

            CapabilityPolicyData data = CapabilityPolicyData.empty()
                    .withPolicySource(POLICY_ID, SOURCE_A, true);
            ResolvedCapabilityPolicyState resolved = CapabilityPolicyApi.resolved(data);

            assertFalse(resolved.allows(CapabilityCheck.OPEN_INVENTORY));
            assertFalse(resolved.allows(CapabilityCheck.MOVE_ITEMS));
            assertFalse(resolved.allows(CapabilityCheck.BREAK_BLOCKS));
            assertFalse(resolved.allows(CapabilityCheck.PLACE_BLOCKS));
            assertFalse(resolved.allows(CapabilityCheck.USE_TOOLS));
            assertFalse(resolved.allows(CapabilityCheck.EQUIP_ARMOR));
            assertFalse(resolved.allows(CapabilityCheck.UNEQUIP_ARMOR));
            assertFalse(resolved.allows(CapabilityCheck.USE_PLAYER_CRAFTING));
            assertFalse(resolved.allows(CapabilityCheck.USE_CRAFTING_TABLE));
            assertFalse(resolved.allows(CapabilityCheck.OPEN_BREWING_STANDS));
            assertFalse(resolved.allows(CapabilityCheck.USE_BEDS));
            assertFalse(resolved.allows(CapabilityCheck.RIDE_ENTITIES));

            assertTrue(resolved.allows(CapabilityCheck.USE_HOTBAR));
            assertTrue(resolved.allows(CapabilityCheck.ATTACK_MOBS));
            assertTrue(resolved.allows(CapabilityCheck.SPRINT));
            assertTrue(resolved.allows(CapabilityCheck.OPEN_CONTAINERS));
        } finally {
            unregisterFixtures();
        }
    }

    @Test
    void restrictiveMergeAccumulatesHeldAndPickupTagFilters() {
        unregisterFixtures();
        try {
            CapabilityPolicyApi.register(CapabilityPolicyDefinition.builder(POLICY_ID)
                    .heldItems(HeldItemPolicy.builder()
                            .allowItemTag(id("tags/swords"))
                            .blockItemTag(id("tags/cursed"))
                            .build())
                    .pickupDrop(PickupDropPolicy.builder()
                            .allowItemTag(id("tags/food"))
                            .build())
                    .build());
            CapabilityPolicyApi.register(CapabilityPolicyDefinition.builder(POLICY_ID_2)
                    .heldItems(HeldItemPolicy.builder()
                            .allowItemTag(id("tags/axes"))
                            .blockItemTag(id("tags/forbidden"))
                            .build())
                    .pickupDrop(PickupDropPolicy.builder()
                            .blockItemTag(id("tags/junk"))
                            .build())
                    .build());

            CapabilityPolicyData data = CapabilityPolicyData.empty()
                    .withPolicySource(POLICY_ID, SOURCE_A, true)
                    .withPolicySource(POLICY_ID_2, SOURCE_A, true);
            ResolvedCapabilityPolicyState resolved = CapabilityPolicyApi.resolved(data);

            assertTrue(resolved.heldItems().allowedItemTags().contains(id("tags/swords")));
            assertTrue(resolved.heldItems().allowedItemTags().contains(id("tags/axes")));
            assertTrue(resolved.heldItems().blockedItemTags().contains(id("tags/cursed")));
            assertTrue(resolved.heldItems().blockedItemTags().contains(id("tags/forbidden")));
            assertTrue(resolved.pickupDrop().allowedItemTags().contains(id("tags/food")));
            assertTrue(resolved.pickupDrop().blockedItemTags().contains(id("tags/junk")));
        } finally {
            unregisterFixtures();
        }
    }

    @Test
    void multiplePoliciesMergeRestrictively() {
        unregisterFixtures();
        try {
            CapabilityPolicyApi.register(CapabilityPolicyDefinition.builder(POLICY_ID)
                    .inventory(InventoryPolicy.builder().canOpenInventory(false).build())
                    .heldItems(HeldItemPolicy.builder().canBreakBlocks(false).build())
                    .build());
            CapabilityPolicyApi.register(CapabilityPolicyDefinition.builder(POLICY_ID_2)
                    .interaction(InteractionPolicy.builder().canAttackPlayers(false).canRideEntities(false).build())
                    .containers(ContainerPolicy.builder().canOpenBrewingStands(false).build())
                    .movement(MovementPolicy.builder().canSprint(false).build())
                    .build());

            CapabilityPolicyData data = CapabilityPolicyData.empty()
                    .withPolicySource(POLICY_ID, SOURCE_A, true)
                    .withPolicySource(POLICY_ID_2, SOURCE_A, true);
            ResolvedCapabilityPolicyState resolved = CapabilityPolicyApi.resolved(data);

            assertFalse(resolved.allows(CapabilityCheck.OPEN_INVENTORY));
            assertFalse(resolved.allows(CapabilityCheck.ATTACK_PLAYERS));
            assertFalse(resolved.allows(CapabilityCheck.SPRINT));
            assertFalse(resolved.allows(CapabilityCheck.BREAK_BLOCKS));
            assertFalse(resolved.allows(CapabilityCheck.OPEN_BREWING_STANDS));
            assertFalse(resolved.allows(CapabilityCheck.RIDE_ENTITIES));

            assertTrue(resolved.allows(CapabilityCheck.ATTACK_MOBS));
            assertTrue(resolved.allows(CapabilityCheck.USE_HOTBAR));
            assertTrue(resolved.allows(CapabilityCheck.MOVE_ITEMS));
        } finally {
            unregisterFixtures();
        }
    }

    @Test
    void revokeSourceRemovesPolicyWhenEmpty() {
        unregisterFixtures();
        try {
            CapabilityPolicyApi.register(CapabilityPolicyDefinition.builder(POLICY_ID)
                    .inventory(InventoryPolicy.builder().canOpenInventory(false).build())
                    .build());

            CapabilityPolicyData data = CapabilityPolicyData.empty()
                    .withPolicySource(POLICY_ID, SOURCE_A, true)
                    .withPolicySource(POLICY_ID, SOURCE_B, true);

            data = data.withPolicySource(POLICY_ID, SOURCE_A, false);
            assertTrue(data.hasPolicy(POLICY_ID), "Policy should remain since SOURCE_B is still active");

            data = data.withPolicySource(POLICY_ID, SOURCE_B, false);
            assertFalse(data.hasPolicy(POLICY_ID), "Policy should be gone when all sources removed");
            assertTrue(CapabilityPolicyApi.resolved(data).allows(CapabilityCheck.OPEN_INVENTORY));
        } finally {
            unregisterFixtures();
        }
    }

    @Test
    void clearPolicySourceRemovesAllEntriesForSource() {
        unregisterFixtures();
        try {
            CapabilityPolicyApi.register(CapabilityPolicyDefinition.builder(POLICY_ID)
                    .inventory(InventoryPolicy.builder().canOpenInventory(false).build())
                    .build());
            CapabilityPolicyApi.register(CapabilityPolicyDefinition.builder(POLICY_ID_2)
                    .movement(MovementPolicy.builder().canSprint(false).build())
                    .build());

            CapabilityPolicyData data = CapabilityPolicyData.empty()
                    .withPolicySource(POLICY_ID, SOURCE_A, true)
                    .withPolicySource(POLICY_ID_2, SOURCE_A, true)
                    .withPolicySource(POLICY_ID, SOURCE_B, true);

            data = data.clearPolicySource(SOURCE_A);

            assertFalse(data.sourcesFor(POLICY_ID).contains(SOURCE_A));
            assertFalse(data.hasPolicy(POLICY_ID_2));
            assertTrue(data.hasPolicy(POLICY_ID), "POLICY_ID should remain via SOURCE_B");
        } finally {
            unregisterFixtures();
        }
    }

    @Test
    void sanitizeDropsStalePolicies() {
        unregisterFixtures();
        try {
            CapabilityPolicyApi.register(CapabilityPolicyDefinition.builder(POLICY_ID)
                    .inventory(InventoryPolicy.builder().canOpenInventory(false).build())
                    .build());

            CapabilityPolicyData data = CapabilityPolicyData.empty()
                    .withPolicySource(POLICY_ID, SOURCE_A, true)
                    .withPolicySource(STALE_POLICY_ID, SOURCE_A, true);

            CapabilityPolicyData sanitized = CapabilityPolicyApi.sanitize(data);

            assertTrue(sanitized.hasPolicy(POLICY_ID));
            assertFalse(sanitized.hasPolicy(STALE_POLICY_ID));
        } finally {
            unregisterFixtures();
        }
    }

    @Test
    void activePoliciesListReflectsRegisteredSubset() {
        unregisterFixtures();
        try {
            CapabilityPolicyApi.register(CapabilityPolicyDefinition.builder(POLICY_ID)
                    .inventory(InventoryPolicy.builder().canOpenInventory(false).build())
                    .build());

            CapabilityPolicyData data = CapabilityPolicyData.empty()
                    .withPolicySource(POLICY_ID, SOURCE_A, true)
                    .withPolicySource(STALE_POLICY_ID, SOURCE_A, true);

            assertEquals(1, CapabilityPolicyApi.activePolicies(data).size());
            assertEquals(POLICY_ID, CapabilityPolicyApi.activePolicies(data).get(0).id());
        } finally {
            unregisterFixtures();
        }
    }

    private void unregisterFixtures() {
        CapabilityPolicyApi.unregister(POLICY_ID);
        CapabilityPolicyApi.unregister(POLICY_ID_2);
        CapabilityPolicyApi.unregister(STALE_POLICY_ID);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("test", path);
    }
}
