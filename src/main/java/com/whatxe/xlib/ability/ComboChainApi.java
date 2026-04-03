package com.whatxe.xlib.ability;

import com.whatxe.xlib.XLibRegistryGuard;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class ComboChainApi {
    private static final Map<ResourceLocation, ComboChainDefinition> CHAINS = new LinkedHashMap<>();

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
        return CHAINS.values().stream()
                .filter(chain -> chain.triggerAbilityId().equals(triggerAbilityId))
                .toList();
    }

    public static AbilityData consumeComboAbility(AbilityData data, ResourceLocation abilityId) {
        return data.clearComboWindowsForAbility(abilityId);
    }

    public static AbilityData applyActivation(AbilityData data, ResourceLocation triggerAbilityId, int activatedSlot) {
        return applyActivation(null, data, triggerAbilityId, activatedSlot);
    }

    public static AbilityData applyActivation(ServerPlayer player, AbilityData data, ResourceLocation triggerAbilityId, int activatedSlot) {
        AbilityData updatedData = data;
        for (ComboChainDefinition chain : chainsForTrigger(triggerAbilityId)) {
            ResourceLocation comboAbilityId = chain.resolveComboAbilityId(player, updatedData);
            updatedData = updatedData.withComboWindow(comboAbilityId, chain.windowTicks());
            int comboSlot = resolveComboSlot(chain, activatedSlot);
            if (comboSlot >= 0) {
                updatedData = updatedData.withComboOverride(comboSlot, comboAbilityId, chain.windowTicks());
            }
        }
        return updatedData;
    }

    public static AbilityData tick(AbilityData data) {
        return data.tickComboWindows().tickComboOverrides();
    }

    private static int resolveComboSlot(ComboChainDefinition chain, int activatedSlot) {
        if (chain.targetSlot() != null) {
            return chain.targetSlot();
        }
        if (chain.transformTriggeredSlot() && activatedSlot >= 0 && activatedSlot < AbilityData.SLOT_COUNT) {
            return activatedSlot;
        }
        return -1;
    }
}
