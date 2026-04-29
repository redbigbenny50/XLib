package com.whatxe.xlib.capability;

public enum CapabilityCheck {
    // Inventory
    OPEN_INVENTORY,
    MOVE_ITEMS,
    USE_HOTBAR,
    USE_OFFHAND_SLOT,
    // Equipment
    EQUIP_ARMOR,
    UNEQUIP_ARMOR,
    EQUIP_HELD_ITEMS,
    // Held items
    USE_MAIN_HAND,
    USE_OFFHAND,
    BLOCK_WITH_SHIELD,
    USE_TOOLS,
    USE_WEAPONS,
    PLACE_BLOCKS,
    BREAK_BLOCKS,
    // Crafting
    USE_PLAYER_CRAFTING,
    USE_CRAFTING_TABLE,
    // Containers
    OPEN_CONTAINERS,
    OPEN_CHESTS,
    OPEN_FURNACES,
    OPEN_SHULKER_BOXES,
    // Pickup / drop
    PICKUP_ITEMS,
    DROP_ITEMS,
    // Interaction
    INTERACT_WITH_BLOCKS,
    INTERACT_WITH_ENTITIES,
    ATTACK_PLAYERS,
    ATTACK_MOBS,
    // Menus
    OPEN_ABILITY_MENU,
    OPEN_PROGRESSION_MENU,
    OPEN_INVENTORY_SCREEN,
    // Movement
    SPRINT,
    SNEAK,
    JUMP,
    FLY
}
