package com.whatxe.xlib.event;

import com.whatxe.xlib.XLib;
import com.whatxe.xlib.ability.GrantedItemRuntime;
import java.util.Iterator;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemStackedOnOtherEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.minecraft.server.level.ServerPlayer;

@EventBusSubscriber(modid = XLib.MODID)
public final class GrantedItemHooks {
    private GrantedItemHooks() {}

    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        ItemStack stack = event.getEntity().getItem();
        if (!GrantedItemRuntime.isUndroppable(stack)) {
            return;
        }

        event.setCanceled(true);
        event.getEntity().discard();
        event.getPlayer().getInventory().placeItemBackInInventory(stack.copy(), true);
        event.getPlayer().containerMenu.broadcastChanges();
    }

    @SubscribeEvent
    public static void onItemStackedOnOther(ItemStackedOnOtherEvent event) {
        if (event.getSlot().container == event.getPlayer().getInventory()) {
            return;
        }
        if (!GrantedItemRuntime.blocksExternalStorage(event.getPlayer(), event.getCarriedItem())
                && !GrantedItemRuntime.blocksExternalStorage(event.getPlayer(), event.getStackedOnItem())) {
            return;
        }

        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer)) {
            return;
        }

        Iterator<net.minecraft.world.entity.item.ItemEntity> iterator = event.getDrops().iterator();
        while (iterator.hasNext()) {
            ItemStack stack = iterator.next().getItem();
            if (GrantedItemRuntime.isUndroppable(stack)) {
                iterator.remove();
            }
        }
    }
}

