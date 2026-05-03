package com.whatxe.xlib.capability;

import java.util.Map;
import net.minecraft.resources.ResourceLocation;

public record ResolvedCapabilityPolicyState(
        InventoryPolicy inventory,
        EquipmentPolicy equipment,
        HeldItemPolicy heldItems,
        CraftingPolicy crafting,
        ContainerPolicy containers,
        PickupDropPolicy pickupDrop,
        InteractionPolicy interaction,
        MenuPolicy menus,
        MovementPolicy movement,
        Map<ResourceLocation, ResourceLocation> contributingSources
) {
    public static final ResolvedCapabilityPolicyState UNRESTRICTED = new ResolvedCapabilityPolicyState(
            InventoryPolicy.FULL,
            EquipmentPolicy.FULL,
            HeldItemPolicy.FULL,
            CraftingPolicy.FULL,
            ContainerPolicy.FULL,
            PickupDropPolicy.FULL,
            InteractionPolicy.FULL,
            MenuPolicy.FULL,
            MovementPolicy.FULL,
            Map.of()
    );

    public boolean allows(CapabilityCheck check) {
        return switch (check) {
            case OPEN_INVENTORY -> inventory.canOpenInventory();
            case MOVE_ITEMS -> inventory.canMoveItems();
            case USE_HOTBAR -> inventory.canUseHotbar();
            case USE_OFFHAND_SLOT -> inventory.canUseOffhand();
            case EQUIP_ARMOR -> equipment.canEquipArmor();
            case UNEQUIP_ARMOR -> equipment.canUnequipArmor();
            case EQUIP_HELD_ITEMS -> equipment.canEquipHeldItems();
            case USE_MAIN_HAND -> heldItems.canUseMainHand();
            case USE_OFFHAND -> heldItems.canUseOffhand();
            case BLOCK_WITH_SHIELD -> heldItems.canBlockWithShields();
            case USE_TOOLS -> heldItems.canUseTools();
            case USE_WEAPONS -> heldItems.canUseWeapons();
            case PLACE_BLOCKS -> heldItems.canPlaceBlocks();
            case BREAK_BLOCKS -> heldItems.canBreakBlocks();
            case USE_PLAYER_CRAFTING -> crafting.canUsePlayerCrafting();
            case USE_CRAFTING_TABLE -> crafting.canUseCraftingTable();
            case OPEN_CONTAINERS -> containers.canOpenContainers();
            case OPEN_CHESTS -> containers.canOpenChests();
            case OPEN_FURNACES -> containers.canOpenFurnaces();
            case OPEN_BREWING_STANDS -> containers.canOpenBrewingStands();
            case OPEN_SHULKER_BOXES -> containers.canOpenShulkerBoxes();
            case PICKUP_ITEMS -> pickupDrop.canPickupItems();
            case DROP_ITEMS -> pickupDrop.canDropItems();
            case INTERACT_WITH_BLOCKS -> interaction.canInteractWithBlocks();
            case INTERACT_WITH_ENTITIES -> interaction.canInteractWithEntities();
            case USE_BEDS -> interaction.canUseBeds();
            case RIDE_ENTITIES -> interaction.canRideEntities();
            case ATTACK_PLAYERS -> interaction.canAttackPlayers();
            case ATTACK_MOBS -> interaction.canAttackMobs();
            case OPEN_ABILITY_MENU -> menus.canOpenAbilityMenu();
            case OPEN_PROGRESSION_MENU -> menus.canOpenProgressionMenu();
            case OPEN_INVENTORY_SCREEN -> menus.canOpenInventoryScreen();
            case SPRINT -> movement.canSprint();
            case SNEAK -> movement.canSneak();
            case JUMP -> movement.canJump();
            case FLY -> movement.canFly();
        };
    }
}
