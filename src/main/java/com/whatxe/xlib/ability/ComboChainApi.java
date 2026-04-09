package com.whatxe.xlib.ability;

import com.whatxe.xlib.XLibRegistryGuard;
import com.whatxe.xlib.attachment.ModAttachments;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class ComboChainApi {
    private static final Map<ResourceLocation, ComboChainDefinition> CHAINS = new LinkedHashMap<>();
    private static final Map<UUID, Map<ResourceLocation, AbilitySlotReference>> LAST_ACTIVATED_SLOTS = new ConcurrentHashMap<>();

    private ComboChainApi() {}

    public static void bootstrap() {}

    public static ComboChainDefinition registerChain(ComboChainDefinition definition) {
        XLibRegistryGuard.ensureMutable("combo_chains");
        ComboChainDefinition previous = CHAINS.putIfAbsent(definition.id(), definition);
        if (previous != null) {
            throw new IllegalStateException("Duplicate combo chain registration: " + definition.id());
        }
        return definition;
    }

    public static Optional<ComboChainDefinition> unregisterChain(ResourceLocation chainId) {
        XLibRegistryGuard.ensureMutable("combo_chains");
        return Optional.ofNullable(CHAINS.remove(chainId));
    }

    public static Collection<ComboChainDefinition> allChains() {
        return List.copyOf(CHAINS.values());
    }

    public static List<ComboChainDefinition> chainsForTrigger(ResourceLocation triggerAbilityId) {
        return chainsForTrigger(triggerAbilityId, null);
    }

    public static List<ComboChainDefinition> chainsForTrigger(
            ResourceLocation triggerAbilityId,
            ComboChainDefinition.TriggerType triggerType
    ) {
        return CHAINS.values().stream()
                .filter(chain -> chain.triggerAbilityId().equals(triggerAbilityId))
                .filter(chain -> triggerType == null || chain.triggerType() == triggerType)
                .toList();
    }

    public static AbilityData consumeComboAbility(AbilityData data, ResourceLocation abilityId) {
        return data.clearComboWindowsForAbility(abilityId);
    }

    public static AbilityData applyActivation(AbilityData data, ResourceLocation triggerAbilityId, int activatedSlot) {
        return applyActivation(null, data, triggerAbilityId, AbilitySlotReference.primary(activatedSlot));
    }

    public static AbilityData applyActivation(ServerPlayer player, AbilityData data, ResourceLocation triggerAbilityId, int activatedSlot) {
        return applyActivation(player, data, triggerAbilityId, AbilitySlotReference.primary(activatedSlot));
    }

    public static AbilityData applyActivation(
            ServerPlayer player,
            AbilityData data,
            ResourceLocation triggerAbilityId,
            AbilitySlotReference activatedSlot
    ) {
        rememberActivatedSlot(player, triggerAbilityId, activatedSlot);
        return applyTrigger(player, data, triggerAbilityId, activatedSlot, ComboChainDefinition.TriggerType.ACTIVATION);
    }

    public static AbilityData applyEnd(ServerPlayer player, AbilityData data, ResourceLocation triggerAbilityId) {
        AbilityData updatedData = applyTrigger(
                player,
                data,
                triggerAbilityId,
                rememberedActivatedSlot(player, triggerAbilityId),
                ComboChainDefinition.TriggerType.END
        );
        forgetActivatedSlot(player, triggerAbilityId);
        return updatedData;
    }

    public static AbilityData applyTrigger(
            ServerPlayer player,
            AbilityData data,
            ResourceLocation triggerAbilityId,
            int activatedSlot,
            ComboChainDefinition.TriggerType triggerType
    ) {
        return applyTrigger(player, data, triggerAbilityId, AbilitySlotReference.primary(activatedSlot), triggerType);
    }

    public static AbilityData applyTrigger(
            ServerPlayer player,
            AbilityData data,
            ResourceLocation triggerAbilityId,
            AbilitySlotReference activatedSlot,
            ComboChainDefinition.TriggerType triggerType
    ) {
        AbilityData updatedData = data;
        for (ComboChainDefinition chain : chainsForTrigger(triggerAbilityId, triggerType)) {
            ResourceLocation comboAbilityId = chain.resolveComboAbilityId(player, updatedData);
            updatedData = updatedData.withComboWindow(comboAbilityId, chain.windowTicks());
            AbilitySlotReference comboSlot = resolveComboSlot(chain, activatedSlot);
            if (comboSlot != null) {
                updatedData = updatedData.withComboOverride(comboSlot, comboAbilityId, chain.windowTicks());
            }
        }
        return updatedData;
    }

    public static void onHitConfirm(ServerPlayer player, ResourceLocation triggerAbilityId) {
        AbilityData currentData = ModAttachments.get(player);
        AbilityData updatedData = applyTrigger(
                player,
                currentData,
                triggerAbilityId,
                rememberedActivatedSlot(player, triggerAbilityId),
                ComboChainDefinition.TriggerType.HIT_CONFIRM
        );
        if (!updatedData.equals(currentData)) {
            ModAttachments.set(player, updatedData);
        }
    }

    public static AbilityData tick(AbilityData data) {
        return data.tickComboWindows().tickComboOverrides();
    }

    private static AbilitySlotReference resolveComboSlot(ComboChainDefinition chain, AbilitySlotReference activatedSlot) {
        if (chain.targetSlotReference() != null) {
            return chain.targetSlotReference();
        }
        if (chain.transformTriggeredSlot() && activatedSlot != null) {
            return activatedSlot;
        }
        return null;
    }

    private static void rememberActivatedSlot(ServerPlayer player, ResourceLocation triggerAbilityId, AbilitySlotReference activatedSlot) {
        if (player == null || activatedSlot == null) {
            return;
        }
        LAST_ACTIVATED_SLOTS
                .computeIfAbsent(player.getUUID(), ignored -> new ConcurrentHashMap<>())
                .put(triggerAbilityId, activatedSlot);
    }

    private static AbilitySlotReference rememberedActivatedSlot(ServerPlayer player, ResourceLocation triggerAbilityId) {
        if (player == null) {
            return null;
        }
        return LAST_ACTIVATED_SLOTS.getOrDefault(player.getUUID(), Map.of()).get(triggerAbilityId);
    }

    private static void forgetActivatedSlot(ServerPlayer player, ResourceLocation triggerAbilityId) {
        if (player == null) {
            return;
        }
        Map<ResourceLocation, AbilitySlotReference> rememberedSlots = LAST_ACTIVATED_SLOTS.get(player.getUUID());
        if (rememberedSlots == null) {
            return;
        }
        rememberedSlots.remove(triggerAbilityId);
        if (rememberedSlots.isEmpty()) {
            LAST_ACTIVATED_SLOTS.remove(player.getUUID());
        }
    }
}
