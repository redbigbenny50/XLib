package com.whatxe.xlib.event;

import com.whatxe.xlib.XLib;
import com.whatxe.xlib.capability.CapabilityCheck;
import com.whatxe.xlib.capability.CapabilityPolicyApi;
import com.whatxe.xlib.capability.ContainerPolicy;
import com.whatxe.xlib.capability.EquipmentPolicy;
import com.whatxe.xlib.capability.InventoryPolicy;
import com.whatxe.xlib.capability.ResolvedCapabilityPolicyState;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = XLib.MODID)
public final class CapabilityPolicyHooks {
    private static final Map<UUID, Map<EquipmentSlot, ItemStack>> PENDING_EQUIPMENT_RESTORES = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> LOCKED_HOTBAR_SLOTS = new ConcurrentHashMap<>();

    private CapabilityPolicyHooks() {}

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!hasActivePolicies(player)) return;
        if (isBlockedHandSlot(player, event.getHand())) {
            event.setCanceled(true);
            return;
        }
        if (isHeldItemBlocked(player, event.getHand())) {
            event.setUseItem(TriState.FALSE);
        }
        if (!CapabilityPolicyApi.allows(player, CapabilityCheck.INTERACT_WITH_BLOCKS)) {
            event.setCanceled(true);
            return;
        }
        BlockState blockState = player.level().getBlockState(event.getPos());
        if (!CapabilityPolicyApi.resolved(player).interaction().allowsBlock(blockState)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!hasActivePolicies(player)) return;
        if (isBlockedHandSlot(player, event.getHand())) {
            event.setCanceled(true);
            return;
        }
        if (isHeldItemBlocked(player, event.getHand())) {
            event.setCanceled(true);
            return;
        }
        if (event.getHand() == InteractionHand.MAIN_HAND && !CapabilityPolicyApi.allows(player, CapabilityCheck.USE_MAIN_HAND)) {
            event.setCanceled(true);
        } else if (event.getHand() == InteractionHand.OFF_HAND && !CapabilityPolicyApi.allows(player, CapabilityCheck.USE_OFFHAND)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!hasActivePolicies(player)) return;
        if (isBlockedHandSlot(player, event.getHand()) || isHeldItemBlocked(player, event.getHand())) {
            event.setCanceled(true);
            return;
        }
        if (!CapabilityPolicyApi.allows(player, CapabilityCheck.INTERACT_WITH_ENTITIES)
                || !CapabilityPolicyApi.resolved(player).interaction().allowsEntity(event.getTarget())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!hasActivePolicies(player)) return;
        if (isBlockedHandSlot(player, event.getHand())) {
            event.setCanceled(true);
            return;
        }
        if (isHeldItemBlocked(player, event.getHand())) {
            event.setCanceled(true);
            return;
        }
        if (!CapabilityPolicyApi.allows(player, CapabilityCheck.INTERACT_WITH_ENTITIES)
                || !CapabilityPolicyApi.resolved(player).interaction().allowsEntity(event.getTarget())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!hasActivePolicies(player)) return;
        if (!CapabilityPolicyApi.allows(player, CapabilityCheck.USE_HOTBAR)
                || !CapabilityPolicyApi.allows(player, CapabilityCheck.USE_MAIN_HAND)
                || isHeldItemBlocked(player, InteractionHand.MAIN_HAND)) {
            event.setCanceled(true);
            return;
        }
        Entity target = event.getTarget();
        ResolvedCapabilityPolicyState resolved = CapabilityPolicyApi.resolved(player);
        boolean blocked = target instanceof Player
                ? !resolved.allows(CapabilityCheck.ATTACK_PLAYERS)
                : !resolved.allows(CapabilityCheck.ATTACK_MOBS);
        if (blocked || !resolved.interaction().allowsEntity(target)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onIncomingDamageFromPlayer(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) return;
        if (!hasActivePolicies(attacker)) return;
        boolean isPlayer = event.getEntity() instanceof Player;
        boolean blocked = isPlayer
                ? !CapabilityPolicyApi.allows(attacker, CapabilityCheck.ATTACK_PLAYERS)
                : !CapabilityPolicyApi.allows(attacker, CapabilityCheck.ATTACK_MOBS);
        if (blocked || !CapabilityPolicyApi.resolved(attacker).interaction().allowsEntity(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onBreakBlock(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (!hasActivePolicies(player)) return;
        if (!CapabilityPolicyApi.resolved(player).interaction().allowsBlock(event.getState())) {
            event.setCanceled(true);
            return;
        }
        if (!CapabilityPolicyApi.allows(player, CapabilityCheck.BREAK_BLOCKS)
                || !CapabilityPolicyApi.allows(player, CapabilityCheck.USE_MAIN_HAND)
                || isHeldItemBlocked(player, InteractionHand.MAIN_HAND)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlaceBlock(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!hasActivePolicies(player)) return;
        if (!CapabilityPolicyApi.resolved(player).interaction().allowsBlock(event.getPlacedBlock())) {
            event.setCanceled(true);
            return;
        }
        if (!CapabilityPolicyApi.allows(player, CapabilityCheck.PLACE_BLOCKS)
                || !CapabilityPolicyApi.allows(player, CapabilityCheck.USE_MAIN_HAND)
                || isHeldItemBlocked(player, InteractionHand.MAIN_HAND)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (!hasActivePolicies(player)) return;
        if (!CapabilityPolicyApi.allows(player, CapabilityCheck.DROP_ITEMS)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onItemPickupPre(ItemEntityPickupEvent.Pre event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (!hasActivePolicies(player)) return;
        if (!CapabilityPolicyApi.resolved(player).pickupDrop().allowsPickup(event.getItemEntity().getItem())) {
            event.setCanPickup(TriState.FALSE);
        }
    }

    @SubscribeEvent
    public static void onContainerOpen(PlayerContainerEvent.Open event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!hasActivePolicies(player)) return;

        AbstractContainerMenu menu = event.getContainer();
        ResolvedCapabilityPolicyState resolved = CapabilityPolicyApi.resolved(player);

        if (!resolved.allows(CapabilityCheck.OPEN_CONTAINERS)) {
            player.closeContainer();
            return;
        }

        if (menu instanceof InventoryMenu) {
            if (!resolved.allows(CapabilityCheck.OPEN_INVENTORY_SCREEN)) {
                player.closeContainer();
            }
        } else if (menu instanceof CraftingMenu) {
            if (!resolved.allows(CapabilityCheck.USE_CRAFTING_TABLE)) {
                player.closeContainer();
            }
        } else if (isChestMenu(menu) && !resolved.allows(CapabilityCheck.OPEN_CHESTS)) {
            player.closeContainer();
        } else if (isFurnaceMenu(menu) && !resolved.allows(CapabilityCheck.OPEN_FURNACES)) {
            player.closeContainer();
        } else if (isBrewingMenu(menu) && !resolved.allows(CapabilityCheck.OPEN_BREWING_STANDS)) {
            player.closeContainer();
        } else if (isShulkerMenu(menu) && !resolved.allows(CapabilityCheck.OPEN_SHULKER_BOXES)) {
            player.closeContainer();
        }
    }

    /**
     * Enforces {@link ContainerPolicy#canTakeCraftingOutput()} by undoing crafted-item delivery.
     *
     * <p>This event fires after the item has already been moved into the player's inventory.
     * The enforcement undoes that transfer by removing the item from inventory and dropping it at
     * the player's feet with a short pickup delay so the player cannot immediately re-take it.
     *
     * <p>Enforcement boundaries for other workstation IO policy fields:
     * <ul>
     *   <li>Furnace: {@code canInsertIntoFurnace}, {@code canExtractFromFurnace}, {@code canTakeFurnaceOutput} —
     *       open-time enforcement is handled via {@code canOpenFurnaces}. Per-slot furnace enforcement
     *       requires dedicated slot-level hooks not yet available in NeoForge 1.21.1.</li>
     *   <li>Brewing: {@code canInsertIntoBrewing}, {@code canExtractFromBrewing}, {@code canTakeBrewingOutput} —
     *       same boundary as furnace: open-time only via {@code canOpenBrewingStands}.</li>
     *   <li>Anvil: {@code canInsertIntoAnvil}, {@code canTakeAnvilOutput} — open-time enforcement deferred;
     *       per-slot anvil hooks are not yet exposed.</li>
     * </ul>
     * All fields are modelled and parsed now; enforcement can be wired up incrementally as hooks become available.
     */
    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!hasActivePolicies(player)) return;
        ContainerPolicy containerPolicy = CapabilityPolicyApi.resolved(player).containers();
        if (containerPolicy.canTakeCraftingOutput()) return;

        // The item has already been moved into the player's inventory by the time this event fires.
        // Undo delivery by dropping the crafted item at the player's feet.
        ItemStack crafted = event.getCrafting();
        if (crafted.isEmpty()) return;
        ItemEntity dropped = new ItemEntity(
                player.level(),
                player.getX(),
                player.getY(),
                player.getZ(),
                crafted.copy()
        );
        dropped.setPickUpDelay(40);
        player.level().addFreshEntity(dropped);
        // Remove the newly delivered stack from the player's inventory.
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack slot = player.getInventory().getItem(i);
            if (ItemStack.matches(slot, crafted)) {
                player.getInventory().setItem(i, ItemStack.EMPTY);
                break;
            }
        }
    }

    /**
     * Documents and provides the enforcement entry-point for workstation IO policy.
     *
     * <p>Currently enforced at runtime:
     * <ul>
     *   <li>{@code canTakeCraftingOutput} — enforced via {@link PlayerEvent.ItemCraftedEvent} in
     *       {@link #onItemCrafted(PlayerEvent.ItemCraftedEvent)}.</li>
     * </ul>
     * Not yet enforced at slot level (deferred until NeoForge exposes per-slot hooks):
     * <ul>
     *   <li>Furnace: {@code canInsertIntoFurnace}, {@code canExtractFromFurnace}, {@code canTakeFurnaceOutput}</li>
     *   <li>Brewing: {@code canInsertIntoBrewing}, {@code canExtractFromBrewing}, {@code canTakeBrewingOutput}</li>
     *   <li>Anvil: {@code canInsertIntoAnvil}, {@code canTakeAnvilOutput}</li>
     * </ul>
     *
     * @param player the player whose workstation interactions are being evaluated
     * @param policy the resolved container policy for that player
     */
    public static void applyWorkstationPolicy(Player player, ContainerPolicy policy) {
        // canTakeCraftingOutput is enforced via onItemCrafted above.
        // All other workstation IO fields are policy-model-only until slot-level hooks are available.
    }

    @SubscribeEvent
    public static void onCanPlayerSleep(CanPlayerSleepEvent event) {
        ServerPlayer player = event.getEntity();
        if (!hasActivePolicies(player)) return;
        if (!CapabilityPolicyApi.allows(player, CapabilityCheck.USE_BEDS)) {
            event.setProblem(Player.BedSleepingProblem.OTHER_PROBLEM);
        }
    }

    @SubscribeEvent
    public static void onEntityMount(EntityMountEvent event) {
        if (!(event.getEntityMounting() instanceof ServerPlayer player)) return;
        if (!event.isMounting()) return;
        if (!hasActivePolicies(player)) return;
        if (!CapabilityPolicyApi.allows(player, CapabilityCheck.RIDE_ENTITIES)
                || !CapabilityPolicyApi.resolved(player).interaction().allowsEntity(event.getEntityBeingMounted())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingEquipmentChange(LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!hasActivePolicies(player)) return;

        EquipmentSlot slot = event.getSlot();
        if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) {
            return;
        }

        if (consumePendingRestore(player, slot, event.getTo())) {
            return;
        }

        boolean equippingArmor = !event.getTo().isEmpty();
        boolean unequippingArmor = !event.getFrom().isEmpty();
        boolean blocked = false;
        if (equippingArmor && !CapabilityPolicyApi.allows(player, CapabilityCheck.EQUIP_ARMOR)) {
            blocked = true;
        }
        if (unequippingArmor && !CapabilityPolicyApi.allows(player, CapabilityCheck.UNEQUIP_ARMOR)) {
            blocked = true;
        }
        if (equippingArmor && !CapabilityPolicyApi.resolved(player).equipment().allowsArmor(event.getTo())) {
            blocked = true;
        }
        if (!blocked) {
            return;
        }

        rememberPendingRestore(player, slot, event.getFrom());
        player.setItemSlot(slot, event.getFrom().copy());
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!hasActivePolicies(player)) {
            LOCKED_HOTBAR_SLOTS.remove(player.getUUID());
            return;
        }
        enforceSelectedHotbarSlot(player);
        enforceSuppressedArmorSlots(player);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        UUID playerId = player.getUUID();
        PENDING_EQUIPMENT_RESTORES.remove(playerId);
        LOCKED_HOTBAR_SLOTS.remove(playerId);
    }

    private static boolean hasActivePolicies(Player player) {
        return !CapabilityPolicyApi.getData(player).activePolicies().isEmpty();
    }

    private static boolean isBlockedHandSlot(ServerPlayer player, InteractionHand hand) {
        return switch (hand) {
            case MAIN_HAND -> !CapabilityPolicyApi.allows(player, CapabilityCheck.USE_HOTBAR)
                    || !CapabilityPolicyApi.resolved(player).inventory().allowsSelectedHotbarSlot(player.getInventory().selected);
            case OFF_HAND -> !CapabilityPolicyApi.allows(player, CapabilityCheck.USE_OFFHAND_SLOT);
        };
    }

    private static boolean isHeldItemBlocked(ServerPlayer player, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND && !CapabilityPolicyApi.allows(player, CapabilityCheck.USE_MAIN_HAND)) {
            return true;
        }
        if (hand == InteractionHand.OFF_HAND && !CapabilityPolicyApi.allows(player, CapabilityCheck.USE_OFFHAND)) {
            return true;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty()) {
            return false;
        }
        return !CapabilityPolicyApi.resolved(player).heldItems().allowsItem(stack);
    }

    private static boolean isChestMenu(AbstractContainerMenu menu) {
        return menu.getClass().getSimpleName().toLowerCase().contains("chest")
                || menuContainerType(menu, ChestBlockEntity.class);
    }

    private static boolean isFurnaceMenu(AbstractContainerMenu menu) {
        return menuContainerType(menu, AbstractFurnaceBlockEntity.class);
    }

    private static boolean isBrewingMenu(AbstractContainerMenu menu) {
        return menu instanceof BrewingStandMenu;
    }

    private static boolean isShulkerMenu(AbstractContainerMenu menu) {
        return menuContainerType(menu, ShulkerBoxBlockEntity.class);
    }

    private static boolean menuContainerType(AbstractContainerMenu menu, Class<?> blockEntityClass) {
        return menu.getSlot(0) != null
                && menu.getSlot(0).container != null
                && blockEntityClass.isInstance(menu.getSlot(0).container);
    }

    private static void rememberPendingRestore(ServerPlayer player, EquipmentSlot slot, ItemStack targetStack) {
        PENDING_EQUIPMENT_RESTORES
                .computeIfAbsent(player.getUUID(), ignored -> new EnumMap<>(EquipmentSlot.class))
                .put(slot, targetStack.copy());
    }

    private static void enforceSelectedHotbarSlot(ServerPlayer player) {
        ResolvedCapabilityPolicyState resolved = CapabilityPolicyApi.resolved(player);
        InventoryPolicy inventoryPolicy = resolved.inventory();
        int selectedSlot = player.getInventory().selected;
        Integer lockedSlot = LOCKED_HOTBAR_SLOTS.get(player.getUUID());

        if (!inventoryPolicy.canChangeSelectedHotbarSlot() && lockedSlot != null) {
            if (selectedSlot != lockedSlot) {
                player.getInventory().selected = lockedSlot;
            }
            return;
        }

        if (inventoryPolicy.allowsSelectedHotbarSlot(selectedSlot)) {
            LOCKED_HOTBAR_SLOTS.put(player.getUUID(), selectedSlot);
            return;
        }

        int fallbackSlot = findAllowedHotbarSlot(inventoryPolicy);
        player.getInventory().selected = fallbackSlot;
        LOCKED_HOTBAR_SLOTS.put(player.getUUID(), fallbackSlot);
    }

    private static int findAllowedHotbarSlot(InventoryPolicy inventoryPolicy) {
        for (int slot = 0; slot < 9; slot++) {
            if (inventoryPolicy.allowsSelectedHotbarSlot(slot)) {
                return slot;
            }
        }
        return 0;
    }

    private static boolean consumePendingRestore(ServerPlayer player, EquipmentSlot slot, ItemStack currentStack) {
        Map<EquipmentSlot, ItemStack> pendingBySlot = PENDING_EQUIPMENT_RESTORES.get(player.getUUID());
        if (pendingBySlot == null) {
            return false;
        }
        ItemStack expectedStack = pendingBySlot.get(slot);
        if (expectedStack == null || !ItemStack.matches(expectedStack, currentStack)) {
            return false;
        }
        pendingBySlot.remove(slot);
        if (pendingBySlot.isEmpty()) {
            PENDING_EQUIPMENT_RESTORES.remove(player.getUUID());
        }
        return true;
    }

    private static final EquipmentSlot[] ARMOR_SLOTS = {
        EquipmentSlot.HEAD,
        EquipmentSlot.CHEST,
        EquipmentSlot.LEGS,
        EquipmentSlot.FEET
    };

    private static void enforceSuppressedArmorSlots(ServerPlayer player) {
        EquipmentPolicy equipmentPolicy = CapabilityPolicyApi.resolved(player).equipment();
        Map<EquipmentSlot, ItemStack> pendingBySlot = PENDING_EQUIPMENT_RESTORES.get(player.getUUID());
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (!equipmentPolicy.suppressesSlot(slot)) {
                continue;
            }
            ItemStack current = player.getItemBySlot(slot);
            if (current.isEmpty()) {
                continue;
            }
            // Skip if we are already mid-restore for this slot to avoid double-stripping.
            if (pendingBySlot != null && pendingBySlot.containsKey(slot)) {
                continue;
            }
            ItemStack toReturn = current.copy();
            rememberPendingRestore(player, slot, ItemStack.EMPTY);
            player.setItemSlot(slot, ItemStack.EMPTY);
            if (!player.addItem(toReturn)) {
                player.drop(toReturn, false);
            }
        }
    }
}
