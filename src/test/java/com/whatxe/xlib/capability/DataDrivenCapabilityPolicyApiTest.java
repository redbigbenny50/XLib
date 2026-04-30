package com.whatxe.xlib.capability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
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
                            "can_open_chests": false
                          }
                        }
                        """)
        );
        assertFalse(loaded.definition().interaction().canAttackPlayers());
        assertFalse(loaded.definition().interaction().canAttackMobs());
        assertTrue(loaded.definition().interaction().canInteractWithBlocks());
        assertFalse(loaded.definition().containers().canOpenChests());
        assertTrue(loaded.definition().containers().canOpenFurnaces());
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }
}
