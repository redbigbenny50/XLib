package com.whatxe.xlib.event;

import com.whatxe.xlib.XLib;
import com.whatxe.xlib.ability.AbilityData;
import com.whatxe.xlib.ability.AbilityResourceRuntime;
import com.whatxe.xlib.ability.PassiveRuntime;
import com.whatxe.xlib.api.event.XLibIncomingDamageEvent;
import com.whatxe.xlib.api.event.XLibOutgoingDamageEvent;
import com.whatxe.xlib.attachment.ModAttachments;
import com.whatxe.xlib.combat.CombatMarkApi;
import com.whatxe.xlib.combat.CombatMarkData;
import com.whatxe.xlib.combat.CombatReactionApi;
import com.whatxe.xlib.progression.UpgradeApi;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.minecraft.server.level.ServerPlayer;

@EventBusSubscriber(modid = XLib.MODID)
public final class AbilityGameplayHooks {
    private static final Map<UUID, ArmorSnapshot> ARMOR_CACHE = new HashMap<>();

    private AbilityGameplayHooks() {}

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        AbilityResourceRuntime.MutableDamageAmount damageAmount = new AbilityResourceRuntime.MutableDamageAmount(event.getAmount());
        if (event.getEntity() instanceof LivingEntity target
                && event.getSource().getEntity() instanceof ServerPlayer attackingPlayer) {
            XLibOutgoingDamageEvent outgoingEvent = new XLibOutgoingDamageEvent(
                    attackingPlayer,
                    target,
                    event.getSource(),
                    damageAmount.amount()
            );
            NeoForge.EVENT_BUS.post(outgoingEvent);
            if (outgoingEvent.isCanceled()) {
                event.setCanceled(true);
                return;
            }
            damageAmount.setAmount(outgoingEvent.amount());
        }

        if (event.getEntity() instanceof ServerPlayer player) {
            LivingEntity attacker = event.getSource().getEntity() instanceof LivingEntity living ? living : null;
            XLibIncomingDamageEvent incomingEvent = new XLibIncomingDamageEvent(player, event.getSource(), attacker, damageAmount.amount());
            NeoForge.EVENT_BUS.post(incomingEvent);
            if (incomingEvent.isCanceled()) {
                event.setCanceled(true);
                return;
            }
            damageAmount.setAmount(incomingEvent.amount());

            AbilityData currentData = ModAttachments.get(player);
            AbilityData updatedData = AbilityResourceRuntime.onIncomingDamage(player, currentData, event.getSource(), damageAmount);
            updatedData = PassiveRuntime.onHurt(player, updatedData, event.getSource(), damageAmount.amount());
            if (!updatedData.equals(currentData)) {
                ModAttachments.set(player, updatedData);
            }
        }
        event.setAmount(damageAmount.amount());
    }

    @SubscribeEvent
    public static void onDamagePost(LivingDamageEvent.Post event) {
        DamageSource source = event.getSource();
        LivingEntity targetEntity = event.getEntity();
        LivingEntity attackerEntity = source.getEntity() instanceof LivingEntity living && living != targetEntity ? living : null;
        if (event.getNewDamage() > 0.0F) {
            CombatReactionApi.recordIncomingHit(targetEntity, attackerEntity, event.getNewDamage());
        }
        if (!(source.getEntity() instanceof ServerPlayer player) || !(event.getEntity() instanceof LivingEntity target)) {
            return;
        }
        if (target == player || event.getNewDamage() <= 0.0F) {
            return;
        }

        AbilityData currentData = ModAttachments.get(player);
        AbilityData updatedData = PassiveRuntime.onHit(player, currentData, target);
        if (!updatedData.equals(currentData)) {
            ModAttachments.set(player, updatedData);
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof LivingEntity livingEntity) {
            CombatMarkData currentMarks = ModAttachments.getMarks(livingEntity);
            if (!currentMarks.marks().isEmpty()) {
                CombatMarkApi.clear(livingEntity);
            }
            CombatReactionApi.clear(livingEntity);
        }

        if (!(event.getSource().getEntity() instanceof ServerPlayer player) || !(event.getEntity() instanceof LivingEntity target)) {
            return;
        }
        if (target == player) {
            return;
        }

        AbilityData currentData = ModAttachments.get(player);
        AbilityData updatedData = PassiveRuntime.onKill(player, currentData, target);
        updatedData = AbilityResourceRuntime.onKill(player, updatedData, target);
        if (!updatedData.equals(currentData)) {
            ModAttachments.set(player, updatedData);
        }
        UpgradeApi.onKill(player, target);
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity livingEntity) || livingEntity.level().isClientSide) {
            return;
        }

        CombatMarkData currentMarks = ModAttachments.getMarks(livingEntity);
        if (currentMarks.marks().isEmpty()) {
            return;
        }

        CombatMarkData updatedMarks = CombatMarkApi.tick(livingEntity, currentMarks);
        if (!updatedMarks.equals(currentMarks)) {
            ModAttachments.setMarks(livingEntity, updatedMarks);
        }
    }

    @SubscribeEvent
    public static void onJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        AbilityData currentData = ModAttachments.get(player);
        AbilityData updatedData = PassiveRuntime.onJump(player, currentData);
        if (!updatedData.equals(currentData)) {
            ModAttachments.set(player, updatedData);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || event.isCanceled()) {
            return;
        }

        AbilityData currentData = ModAttachments.get(player);
        AbilityData updatedData = PassiveRuntime.onBlockBreak(player, currentData, event.getState(), event.getPos());
        if (!updatedData.equals(currentData)) {
            ModAttachments.set(player, updatedData);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ArmorSnapshot previousSnapshot = ARMOR_CACHE.get(player.getUUID());
        ArmorSnapshot currentSnapshot = ArmorSnapshot.capture(player);
        if (previousSnapshot == null) {
            ARMOR_CACHE.put(player.getUUID(), currentSnapshot);
            return;
        }

        AbilityData currentData = ModAttachments.get(player);
        AbilityData updatedData = currentData;
        for (EquipmentSlot slot : ArmorSnapshot.ARMOR_SLOTS) {
            ItemStack from = previousSnapshot.item(slot);
            ItemStack to = currentSnapshot.item(slot);
            if (!ItemStack.matches(from, to)) {
                updatedData = PassiveRuntime.onArmorChange(player, updatedData, slot, from, to);
            }
        }

        ARMOR_CACHE.put(player.getUUID(), currentSnapshot);
        if (!updatedData.equals(currentData)) {
            ModAttachments.set(player, updatedData);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        ARMOR_CACHE.remove(event.getEntity().getUUID());
    }

    private record ArmorSnapshot(Map<EquipmentSlot, ItemStack> items) {
        private static final EquipmentSlot[] ARMOR_SLOTS = {
                EquipmentSlot.HEAD,
                EquipmentSlot.CHEST,
                EquipmentSlot.LEGS,
                EquipmentSlot.FEET
        };

        private static ArmorSnapshot capture(ServerPlayer player) {
            Map<EquipmentSlot, ItemStack> items = new EnumMap<>(EquipmentSlot.class);
            for (EquipmentSlot slot : ARMOR_SLOTS) {
                items.put(slot, player.getItemBySlot(slot).copy());
            }
            return new ArmorSnapshot(items);
        }

        private ItemStack item(EquipmentSlot slot) {
            return this.items.getOrDefault(slot, ItemStack.EMPTY);
        }
    }
}

