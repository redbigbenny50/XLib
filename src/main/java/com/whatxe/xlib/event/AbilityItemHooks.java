package com.whatxe.xlib.event;

import com.whatxe.xlib.XLib;
import com.whatxe.xlib.ability.AbilityData;
import com.whatxe.xlib.ability.AbilityGrantApi;
import com.whatxe.xlib.ability.AbilityGrantingItem;
import com.whatxe.xlib.ability.AbilityRequirements;
import com.whatxe.xlib.ability.ArtifactApi;
import com.whatxe.xlib.ability.ArtifactDefinition;
import com.whatxe.xlib.ability.ContextGrantApi;
import com.whatxe.xlib.ability.ContextGrantSnapshot;
import com.whatxe.xlib.ability.AbilityResourceRuntime;
import com.whatxe.xlib.ability.AbilityResourceApi;
import com.whatxe.xlib.ability.AbilityResourceItem;
import com.whatxe.xlib.ability.AbilityUnlockItem;
import com.whatxe.xlib.ability.GrantedItemGrantApi;
import com.whatxe.xlib.ability.GrantBundleApi;
import com.whatxe.xlib.ability.GrantConditions;
import com.whatxe.xlib.ability.ModeApi;
import com.whatxe.xlib.ability.PassiveRuntime;
import com.whatxe.xlib.ability.PassiveGrantApi;
import com.whatxe.xlib.ability.RecipePermissionApi;
import com.whatxe.xlib.ability.ReactiveRuntimeEvent;
import com.whatxe.xlib.ability.ReactiveTriggerApi;
import com.whatxe.xlib.ability.StatePolicyApi;
import com.whatxe.xlib.ability.StateFlagApi;
import com.whatxe.xlib.attachment.ModAttachments;
import com.whatxe.xlib.progression.UpgradeApi;
import com.whatxe.xlib.value.DataDrivenTrackedValueRuleApi;
import com.whatxe.xlib.value.TrackedValueApi;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = XLib.MODID)
public final class AbilityItemHooks {
    private static final Map<UUID, GrantCache> GRANT_CACHE = new HashMap<>();

    private AbilityItemHooks() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        AbilityData currentData = ModAttachments.get(player);
        GrantCache cache = cacheFor(player);
        syncDynamicSources(player, currentData, cache.items());
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        GRANT_CACHE.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onItemUseFinished(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ItemStack usedStack = event.getItem();
        if (usedStack.isEmpty()) {
            return;
        }

        AbilityData currentData = ModAttachments.get(player);
        if (usedStack.getItem() instanceof AbilityUnlockItem unlockItem) {
            if (GrantConditions.allMatch(player, currentData, usedStack, unlockItem.unlockConditions(usedStack, player))) {
                ResourceLocation sourceId = unlockItem.unlockSourceId(usedStack, player);
                for (ResourceLocation abilityId : unlockItem.unlockedAbilities(usedStack, player)) {
                    AbilityGrantApi.grant(player, abilityId, sourceId);
                }
                for (ResourceLocation passiveId : unlockItem.unlockedPassives(usedStack, player)) {
                    PassiveGrantApi.grant(player, passiveId, sourceId);
                }
                for (ResourceLocation grantedItemId : unlockItem.unlockedGrantedItems(usedStack, player)) {
                    GrantedItemGrantApi.grant(player, grantedItemId, sourceId);
                }
                for (ResourceLocation recipeId : unlockItem.unlockedRecipePermissions(usedStack, player)) {
                    RecipePermissionApi.grant(player, recipeId, sourceId);
                }
            }
        }

        for (ArtifactDefinition artifact : ArtifactApi.matchingArtifacts(usedStack)) {
            if (artifact.unlockOnConsume()
                    && AbilityRequirements.firstFailure(player, currentData, artifact.requirements()).isEmpty()) {
                ArtifactApi.unlock(player, artifact.id());
            }
        }

        if (usedStack.getItem() instanceof AbilityResourceItem resourceItem) {
            if (GrantConditions.allMatch(player, currentData, usedStack, resourceItem.resourceConditions(usedStack, player))) {
                for (Map.Entry<ResourceLocation, Integer> entry : resourceItem.resourceChanges(usedStack, player).entrySet()) {
                    AbilityResourceApi.add(player, entry.getKey(), entry.getValue());
                }
            }
        }

        if (usedStack.has(DataComponents.FOOD)) {
            AbilityData updatedData = ModAttachments.get(player);
            updatedData = PassiveRuntime.onEat(player, updatedData, usedStack);
            updatedData = AbilityResourceRuntime.onEat(player, updatedData, usedStack);
            updatedData = ReactiveTriggerApi.dispatch(
                    player,
                    updatedData,
                    ReactiveRuntimeEvent.itemConsumed(BuiltInRegistries.ITEM.getKey(usedStack.getItem()))
            );
            if (!updatedData.equals(ModAttachments.get(player))) {
                ModAttachments.set(player, updatedData);
            }
        } else {
            AbilityData currentReactiveData = ModAttachments.get(player);
            AbilityData updatedReactiveData = ReactiveTriggerApi.dispatch(
                    player,
                    currentReactiveData,
                    ReactiveRuntimeEvent.itemConsumed(BuiltInRegistries.ITEM.getKey(usedStack.getItem()))
            );
            if (!updatedReactiveData.equals(currentReactiveData)) {
                ModAttachments.set(player, updatedReactiveData);
            }
        }

        UpgradeApi.onItemConsumed(player, usedStack);
        DataDrivenTrackedValueRuleApi.dispatchItemConsumed(player, usedStack);
        TrackedValueApi.onFoodConsumed(player, usedStack);
        TrackedValueApi.applyVanillaFoodSuppression(player);
    }

    private static GrantCache cacheFor(ServerPlayer player) {
        UUID playerId = player.getUUID();
        int inventoryVersion = player.getInventory().getTimesChanged();
        int inventoryIdentity = System.identityHashCode(player.getInventory());
        GrantCache existing = GRANT_CACHE.get(playerId);
        if (existing != null && existing.inventoryVersion() == inventoryVersion && existing.inventoryIdentity() == inventoryIdentity) {
            return existing;
        }

        List<CachedGrantItem> cachedItems = new ArrayList<>();
        cacheGrantingItems(player.getInventory().items, cachedItems);
        cacheGrantingItems(player.getInventory().armor, cachedItems);
        cacheGrantingItems(player.getInventory().offhand, cachedItems);

        GrantCache rebuilt = new GrantCache(inventoryVersion, inventoryIdentity, List.copyOf(cachedItems));
        GRANT_CACHE.put(playerId, rebuilt);
        return rebuilt;
    }

    private static void cacheGrantingItems(Iterable<ItemStack> stacks, List<CachedGrantItem> cachedItems) {
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty() && stack.getItem() instanceof AbilityGrantingItem grantingItem) {
                cachedItems.add(new CachedGrantItem(stack, grantingItem));
            }
        }
    }

    private static void collectGrantingItems(
            ServerPlayer player,
            AbilityData currentData,
            Map<ResourceLocation, Set<ResourceLocation>> sourceAbilities,
            Map<ResourceLocation, Set<ResourceLocation>> sourcePassives,
            Map<ResourceLocation, Set<ResourceLocation>> sourceGrantedItems,
            Map<ResourceLocation, Set<ResourceLocation>> sourceRecipePermissions,
            List<CachedGrantItem> cachedItems
    ) {
        for (CachedGrantItem cachedItem : cachedItems) {
            ItemStack stack = cachedItem.stack();
            AbilityGrantingItem grantingItem = cachedItem.grantingItem();
            ResourceLocation sourceId = grantingItem.grantSourceId(stack, player);
            sourceAbilities.computeIfAbsent(sourceId, key -> new LinkedHashSet<>());
            sourcePassives.computeIfAbsent(sourceId, key -> new LinkedHashSet<>());
            sourceGrantedItems.computeIfAbsent(sourceId, key -> new LinkedHashSet<>());
            sourceRecipePermissions.computeIfAbsent(sourceId, key -> new LinkedHashSet<>());

            if (!GrantConditions.allMatch(player, currentData, stack, grantingItem.grantConditions(stack, player))) {
                continue;
            }

            sourceAbilities.get(sourceId).addAll(grantingItem.grantedAbilities(stack, player));
            sourcePassives.get(sourceId).addAll(grantingItem.grantedPassives(stack, player));
            sourceGrantedItems.get(sourceId).addAll(grantingItem.grantedItems(stack, player));
            sourceRecipePermissions.get(sourceId).addAll(grantingItem.grantedRecipePermissions(stack, player));
        }
    }

    private static void syncDynamicSources(ServerPlayer player, AbilityData currentData, List<CachedGrantItem> cachedItems) {
        Map<ResourceLocation, Set<ResourceLocation>> sourceAbilities = new LinkedHashMap<>();
        Map<ResourceLocation, Set<ResourceLocation>> sourcePassives = new LinkedHashMap<>();
        Map<ResourceLocation, Set<ResourceLocation>> sourceGrantedItems = new LinkedHashMap<>();
        Map<ResourceLocation, Set<ResourceLocation>> sourceRecipePermissions = new LinkedHashMap<>();
        Map<ResourceLocation, Set<ResourceLocation>> sourceActivationBlocks = new LinkedHashMap<>();
        Map<ResourceLocation, Set<ResourceLocation>> sourceStatePolicies = new LinkedHashMap<>();
        Map<ResourceLocation, Set<ResourceLocation>> sourceStateFlags = new LinkedHashMap<>();
        Map<ResourceLocation, Set<ResourceLocation>> sourceGrantBundles = new LinkedHashMap<>(ArtifactApi.activeBundlesBySource(player, currentData));

        collectGrantingItems(
                player,
                currentData,
                sourceAbilities,
                sourcePassives,
                sourceGrantedItems,
                sourceRecipePermissions,
                cachedItems
        );
        for (ContextGrantSnapshot snapshot : ContextGrantApi.collectSnapshots(player, currentData)) {
            mergeSourceValues(sourceAbilities, snapshot.sourceId(), snapshot.abilities());
            mergeSourceValues(sourcePassives, snapshot.sourceId(), snapshot.passives());
            mergeSourceValues(sourceGrantedItems, snapshot.sourceId(), snapshot.grantedItems());
            mergeSourceValues(sourceRecipePermissions, snapshot.sourceId(), snapshot.recipePermissions());
            mergeSourceValues(sourceActivationBlocks, snapshot.sourceId(), snapshot.blockedAbilities());
            mergeSourceValues(sourceStatePolicies, snapshot.sourceId(), snapshot.statePolicies());
            mergeSourceValues(sourceStateFlags, snapshot.sourceId(), snapshot.stateFlags());
        }
        for (ContextGrantSnapshot snapshot : ModeApi.collectSnapshots(currentData)) {
            mergeSourceValues(sourceAbilities, snapshot.sourceId(), snapshot.abilities());
            mergeSourceValues(sourcePassives, snapshot.sourceId(), snapshot.passives());
            mergeSourceValues(sourceGrantedItems, snapshot.sourceId(), snapshot.grantedItems());
            mergeSourceValues(sourceRecipePermissions, snapshot.sourceId(), snapshot.recipePermissions());
            mergeSourceValues(sourceActivationBlocks, snapshot.sourceId(), snapshot.blockedAbilities());
            mergeSourceValues(sourceStatePolicies, snapshot.sourceId(), snapshot.statePolicies());
            mergeSourceValues(sourceStateFlags, snapshot.sourceId(), snapshot.stateFlags());
        }

        Set<ResourceLocation> seenSources = new LinkedHashSet<>();
        for (Map.Entry<ResourceLocation, Set<ResourceLocation>> entry : sourceAbilities.entrySet()) {
            AbilityGrantApi.syncSourceAbilities(player, entry.getKey(), entry.getValue());
            seenSources.add(entry.getKey());
        }
        for (Map.Entry<ResourceLocation, Set<ResourceLocation>> entry : sourcePassives.entrySet()) {
            PassiveGrantApi.syncSourcePassives(player, entry.getKey(), entry.getValue());
            seenSources.add(entry.getKey());
        }
        for (Map.Entry<ResourceLocation, Set<ResourceLocation>> entry : sourceGrantedItems.entrySet()) {
            GrantedItemGrantApi.syncSourceItems(player, entry.getKey(), entry.getValue());
            seenSources.add(entry.getKey());
        }
        for (Map.Entry<ResourceLocation, Set<ResourceLocation>> entry : sourceRecipePermissions.entrySet()) {
            RecipePermissionApi.syncSourcePermissions(player, entry.getKey(), entry.getValue());
            seenSources.add(entry.getKey());
        }
        for (Map.Entry<ResourceLocation, Set<ResourceLocation>> entry : sourceActivationBlocks.entrySet()) {
            AbilityGrantApi.syncActivationBlocks(player, entry.getKey(), entry.getValue());
            seenSources.add(entry.getKey());
        }
        for (Map.Entry<ResourceLocation, Set<ResourceLocation>> entry : sourceStatePolicies.entrySet()) {
            StatePolicyApi.syncSourcePolicies(player, entry.getKey(), entry.getValue());
            seenSources.add(entry.getKey());
        }
        for (Map.Entry<ResourceLocation, Set<ResourceLocation>> entry : sourceStateFlags.entrySet()) {
            StateFlagApi.syncSourceFlags(player, entry.getKey(), entry.getValue());
            seenSources.add(entry.getKey());
        }
        for (Map.Entry<ResourceLocation, Set<ResourceLocation>> entry : sourceGrantBundles.entrySet()) {
            GrantBundleApi.syncSourceBundles(player, entry.getKey(), entry.getValue());
            seenSources.add(entry.getKey());
        }

        seenSources.addAll(UpgradeApi.activeRewardSources(player));
        seenSources.addAll(ArtifactApi.activeUnlockSources(ModAttachments.get(player)));
        AbilityGrantApi.pruneManagedSources(player, seenSources);
    }

    private static void mergeSourceValues(
            Map<ResourceLocation, Set<ResourceLocation>> target,
            ResourceLocation sourceId,
            Collection<ResourceLocation> ids
    ) {
        if (ids.isEmpty()) {
            return;
        }
        target.computeIfAbsent(sourceId, ignored -> new LinkedHashSet<>()).addAll(ids);
    }

    private record CachedGrantItem(ItemStack stack, AbilityGrantingItem grantingItem) {}

    private record GrantCache(
            int inventoryVersion,
            int inventoryIdentity,
            List<CachedGrantItem> items
    ) {}
}

