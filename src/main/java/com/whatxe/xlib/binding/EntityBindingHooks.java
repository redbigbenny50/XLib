package com.whatxe.xlib.binding;

import com.whatxe.xlib.XLib;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = XLib.MODID)
public final class EntityBindingHooks {
    private EntityBindingHooks() {}

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        EntityBindingApi.onServerStart(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        EntityBindingApi.onServerStop();
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof LivingEntity le) {
            EntityBindingApi.onEntityLoad(le);
        }
    }

    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof LivingEntity le) {
            EntityBindingApi.onEntityUnload(le);
        }
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!event.getEntity().level().isClientSide() && event.getEntity() instanceof LivingEntity le) {
            EntityBindingApi.tick(le);
        }
    }

    @SubscribeEvent
    public static void onEntityDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!entity.level().isClientSide()) {
            EntityBindingApi.breakByCondition(entity, EntityBindingBreakCondition.PRIMARY_DIES);
            EntityBindingApi.breakByCondition(entity, EntityBindingBreakCondition.SECONDARY_DIES);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof LivingEntity le && !le.level().isClientSide()) {
            EntityBindingApi.breakByCondition(le, EntityBindingBreakCondition.PRIMARY_DISCONNECTS);
            EntityBindingApi.breakByCondition(le, EntityBindingBreakCondition.SECONDARY_DISCONNECTS);
        }
    }
}
