package com.whatxe.xlib.capability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class DataDrivenCapabilityPolicyApiTest {

    @Test
    void parsesMinimalPolicyUsingFileId() {
        DataDrivenCapabilityPolicyApi.LoadedCapabilityPolicyDefinition loaded = DataDrivenCapabilityPolicyApi.parseDefinition(
                id("capability_policies/basic"),
                JsonParser.parseString("{}")
        );

        assertEquals(id("capability_policies/basic"), loaded.id());
        // Defaults: restrictive merge mode, priority 0, all dimensions fully permissive
        assertEquals(CapabilityPolicyMergeMode.RESTRICTIVE, loaded.definition().mergeMode());
        assertEquals(0, loaded.definition().priority());
        assertEquals(InventoryPolicy.FULL, loaded.definition().inventory());
        assertEquals(EquipmentPolicy.FULL, loaded.definition().equipment());
        assertEquals(MovementPolicy.FULL, loaded.definition().movement());
    }

    @Test
    void parsesExplicitIdOverridesFileId() {
        DataDrivenCapabilityPolicyApi.LoadedCapabilityPolicyDefinition loaded = DataDrivenCapabilityPolicyApi.parseDefinition(
                id("capability_policies/file_name"),
                JsonParser.parseString("""
                        { "id": "xlib_test:policies/explicit" }
                        """)
        );
        assertEquals(id("policies/explicit"), loaded.id());
    }

    @Test
    void parsesMergeModeAndPriority() {
        DataDrivenCapabilityPolicyApi.LoadedCapabilityPolicyDefinition loaded = DataDrivenCapabilityPolicyApi.parseDefinition(
                id("capability_policies/test"),
                JsonParser.parseString("""
                        { "merge_mode": "permissive", "priority": 10 }
                        """)
        );
        assertEquals(CapabilityPolicyMergeMode.PERMISSIVE, loaded.definition().mergeMode());
        assertEquals(10, loaded.definition().priority());
    }

    @Test
    void parsesInventoryDimension() {
        DataDrivenCapabilityPolicyApi.LoadedCapabilityPolicyDefinition loaded = DataDrivenCapabilityPolicyApi.parseDefinition(
                id("capability_policies/test"),
                JsonParser.parseString("""
                        {
                          "inventory": {
                            "can_open_inventory": false,
                            "can_move_items": false,
                            "can_use_hotbar": true,
                            "can_use_offhand": false
                          }
                        }
                        """)
        );
        InventoryPolicy inv = loaded.definition().inventory();
        assertFalse(inv.canOpenInventory());
        assertFalse(inv.canMoveItems());
        assertTrue(inv.canUseHotbar());
        assertFalse(inv.canUseOffhand());
    }

    @Test
    void parsesMovementDimension() {
        DataDrivenCapabilityPolicyApi.LoadedCapabilityPolicyDefinition loaded = DataDrivenCapabilityPolicyApi.parseDefinition(
                id("capability_policies/test"),
                JsonParser.parseString("""
                        {
                          "movement": {
                            "can_sprint": false,
                            "can_sneak": true,
                            "can_jump": false,
                            "can_fly": false
                          }
                        }
                        """)
        );
        MovementPolicy movement = loaded.definition().movement();
        assertFalse(movement.canSprint());
        assertTrue(movement.canSneak());
        assertFalse(movement.canJump());
        assertFalse(movement.canFly());
    }

    @Test
    void parsesHeldItemsWithTagLists() {
        DataDrivenCapabilityPolicyApi.LoadedCapabilityPolicyDefinition loaded = DataDrivenCapabilityPolicyApi.parseDefinition(
                id("capability_policies/test"),
                JsonParser.parseString("""
                        {
                          "held_items": {
                            "can_use_tools": false,
                            "allowed_item_tags": ["xlib_test:tags/swords"],
                            "blocked_item_tags": ["xlib_test:tags/axes"]
                          }
                        }
                        """)
        );
        HeldItemPolicy held = loaded.definition().heldItems();
        assertFalse(held.canUseTools());
        assertTrue(held.allowedItemTags().contains(ResourceLocation.parse("xlib_test:tags/swords")));
        assertTrue(held.blockedItemTags().contains(ResourceLocation.parse("xlib_test:tags/axes")));
    }

    @Test
    void parsesPickupDropWithTagLists() {
        DataDrivenCapabilityPolicyApi.LoadedCapabilityPolicyDefinition loaded = DataDrivenCapabilityPolicyApi.parseDefinition(
                id("capability_policies/test"),
                JsonParser.parseString("""
                        {
                          "pickup_drop": {
                            "can_pickup_items": true,
                            "can_drop_items": false,
                            "allowed_item_tags": ["xlib_test:tags/food"],
                            "blocked_item_tags": ["xlib_test:tags/cursed"]
                          }
                        }
                        """)
        );
        PickupDropPolicy pickupDrop = loaded.definition().pickupDrop();
        assertTrue(pickupDrop.canPickupItems());
        assertFalse(pickupDrop.canDropItems());
        assertTrue(pickupDrop.allowedItemTags().contains(ResourceLocation.parse("xlib_test:tags/food")));
        assertTrue(pickupDrop.blockedItemTags().contains(ResourceLocation.parse("xlib_test:tags/cursed")));
    }

    @Test
    void parsesExtendedCapabilityFilters() {
        DataDrivenCapabilityPolicyApi.LoadedCapabilityPolicyDefinition loaded = DataDrivenCapabilityPolicyApi.parseDefinition(
                id("capability_policies/test"),
                JsonParser.parseString("""
                        {
                          "inventory": {
                            "can_change_selected_hotbar_slot": false,
                            "allowed_hotbar_slots": [0, 2],
                            "blocked_hotbar_slots": [1]
                          },
                          "equipment": {
                            "allowed_armor_items": ["minecraft:diamond_chestplate"],
                            "blocked_armor_item_tags": ["xlib_test:armor/forbidden"]
                          },
                          "held_items": {
                            "allowed_items": ["minecraft:iron_sword"],
                            "blocked_items": ["minecraft:stick"]
                          },
                          "pickup_drop": {
                            "allowed_items": ["minecraft:apple"],
                            "blocked_items": ["minecraft:rotten_flesh"]
                          },
                          "interaction": {
                            "allowed_blocks": ["minecraft:crafting_table"],
                            "blocked_block_tags": ["xlib_test:blocks/forbidden"],
                            "allowed_entities": ["minecraft:horse"],
                            "blocked_entity_tags": ["xlib_test:entities/hostile"]
                          }
                        }
                        """)
        );

        assertFalse(loaded.definition().inventory().canChangeSelectedHotbarSlot());
        assertTrue(loaded.definition().inventory().allowedHotbarSlots().contains(0));
        assertTrue(loaded.definition().inventory().blockedHotbarSlots().contains(1));
        assertTrue(loaded.definition().equipment().allowedArmorItemIds().contains(ResourceLocation.parse("minecraft:diamond_chestplate")));
        assertTrue(loaded.definition().equipment().blockedArmorItemTags().contains(ResourceLocation.parse("xlib_test:armor/forbidden")));
        assertTrue(loaded.definition().heldItems().allowedItemIds().contains(ResourceLocation.parse("minecraft:iron_sword")));
        assertTrue(loaded.definition().heldItems().blockedItemIds().contains(ResourceLocation.parse("minecraft:stick")));
        assertTrue(loaded.definition().pickupDrop().allowedItemIds().contains(ResourceLocation.parse("minecraft:apple")));
        assertTrue(loaded.definition().pickupDrop().blockedItemIds().contains(ResourceLocation.parse("minecraft:rotten_flesh")));
        assertTrue(loaded.definition().interaction().allowedBlockIds().contains(ResourceLocation.parse("minecraft:crafting_table")));
        assertTrue(loaded.definition().interaction().blockedBlockTags().contains(ResourceLocation.parse("xlib_test:blocks/forbidden")));
        assertTrue(loaded.definition().interaction().allowedEntityIds().contains(ResourceLocation.parse("minecraft:horse")));
        assertTrue(loaded.definition().interaction().blockedEntityTags().contains(ResourceLocation.parse("xlib_test:entities/hostile")));
    }

    @Test
    void rejectsUnknownMergeMode() {
        assertThrows(IllegalArgumentException.class, () -> DataDrivenCapabilityPolicyApi.parseDefinition(
                id("capability_policies/test"),
                JsonParser.parseString("""
                        { "merge_mode": "additive" }
                        """)
        ));
    }

    @Test
    void parsesInteractionAndContainerDimensions() {
        DataDrivenCapabilityPolicyApi.LoadedCapabilityPolicyDefinition loaded = DataDrivenCapabilityPolicyApi.parseDefinition(
                id("capability_policies/test"),
                JsonParser.parseString("""
                        {
                          "interaction": {
                            "can_attack_players": false,
                            "can_attack_mobs": false
                          },
                          "containers": {
                            "can_open_chests": false,
                            "can_open_brewing_stands": false
                          },
                          "interaction": {
                            "can_attack_players": false,
                            "can_attack_mobs": false,
                            "can_use_beds": false,
                            "can_ride_entities": false
                          }
                        }
                        """)
        );
        assertFalse(loaded.definition().interaction().canAttackPlayers());
        assertFalse(loaded.definition().interaction().canAttackMobs());
        assertFalse(loaded.definition().interaction().canUseBeds());
        assertFalse(loaded.definition().interaction().canRideEntities());
        assertTrue(loaded.definition().interaction().canInteractWithBlocks());
        assertFalse(loaded.definition().containers().canOpenChests());
        assertFalse(loaded.definition().containers().canOpenBrewingStands());
        assertTrue(loaded.definition().containers().canOpenFurnaces());
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }
}
