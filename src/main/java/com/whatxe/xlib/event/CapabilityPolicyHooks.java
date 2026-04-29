package com.whatxe.xlib.event;

import com.whatxe.xlib.XLib;
import com.whatxe.xlib.capability.CapabilityCheck;
import com.whatxe.xlib.capability.CapabilityPolicyApi;
import com.whatxe.xlib.capability.ResolvedCapabilityPolicyState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = XLib.MODID)
public final class CapabilityPolicyHooks {
    private CapabilityPolicyHooks() {}

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!hasActivePolicies(player)) return;
        if (!CapabilityPolicyApi.allows(player, CapabilityCheck.INTERACT_WITH_BLOCKS)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!hasActivePolicies(player)) return;
        if (!CapabilityPolicyApi.allows(player, CapabilityCheck.USE_MAIN_HAND)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!hasActivePolicies(player)) return;
        if (!CapabilityPolicyApi.allows(player, CapabilityCheck.INTERACT_WITH_ENTITIES)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!hasActivePolicies(player)) return;
        Entity target = event.getTarget();
        ResolvedCapabilityPolicyState resolved = CapabilityPolicyApi.resolved(player);
        boolean blocked = target instanceof Player
                ? !resolved.allows(CapabilityCheck.ATTACK_PLAYERS)
                : !resolved.allows(CapabilityCheck.ATTACK_MOBS);
        if (blocked) {
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
        if (blocked) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onBreakBlock(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (!hasActivePolicies(player)) return;
        if (!CapabilityPolicyApi.allows(player, CapabilityCheck.BREAK_BLOCKS)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlaceBlock(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!hasActivePolicies(player)) return;
        if (!CapabilityPolicyApi.allows(player, CapabilityCheck.PLACE_BLOCKS)) {
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
        } else if (isShulkerMenu(menu) && !resolved.allows(CapabilityCheck.OPEN_SHULKER_BOXES)) {
            player.closeContainer();
        }
    }

    private static boolean hasActivePolicies(Player player) {
        return !CapabilityPolicyApi.getData(player).activePolicies().isEmpty();
    }

    private static boolean isChestMenu(AbstractContainerMenu menu) {
        return menu.getClass().getSimpleName().toLowerCase().contains("chest")
                || menuContainerType(menu, ChestBlockEntity.class);
    }

    private static boolean isFurnaceMenu(AbstractContainerMenu menu) {
        return menuContainerType(menu, AbstractFurnaceBlockEntity.class);
    }

    private static boolean isShulkerMenu(AbstractContainerMenu menu) {
        return menuContainerType(menu, ShulkerBoxBlockEntity.class);
    }

    private static boolean menuContainerType(AbstractContainerMenu menu, Class<?> blockEntityClass) {
        return menu.getSlot(0) != null
                && menu.getSlot(0).container != null
                && blockEntityClass.isInstance(menu.getSlot(0).container);
    }
}
