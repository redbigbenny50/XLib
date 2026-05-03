package com.whatxe.xlib.ability;

import com.whatxe.xlib.XLib;
import com.whatxe.xlib.attachment.ModAttachments;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public final class GrantedItemGrantApi {
    public static final ResourceLocation COMMAND_SOURCE =
            ResourceLocation.fromNamespaceAndPath(XLib.MODID, "command_granted_item");

    private GrantedItemGrantApi() {}

    public static boolean hasGrantedItem(Player player, ResourceLocation grantedItemId) {
        return ModAttachments.get(player).hasGrantedItem(grantedItemId);
    }

    public static Set<ResourceLocation> grantedItems(Player player) {
        return Set.copyOf(ModAttachments.get(player).grantedItems());
    }

    public static Set<ResourceLocation> grantSources(Player player, ResourceLocation grantedItemId) {
        return Set.copyOf(ModAttachments.get(player).grantedItemSourcesFor(grantedItemId));
    }

    public static void grant(Player player, ResourceLocation grantedItemId) {
        grant(player, grantedItemId, COMMAND_SOURCE);
    }

    public static void grant(Player player, ResourceLocation grantedItemId, ResourceLocation sourceId) {
        update(player, ModAttachments.get(player).withGrantedItemSource(grantedItemId, sourceId, true));
    }

    public static void grant(Player player, Collection<ResourceLocation> grantedItemIds, ResourceLocation sourceId) {
        AbilityData updatedData = ModAttachments.get(player);
        for (ResourceLocation grantedItemId : new LinkedHashSet<>(grantedItemIds)) {
            updatedData = updatedData.withGrantedItemSource(grantedItemId, sourceId, true);
        }
        update(player, updatedData);
    }

    public static void revoke(Player player, ResourceLocation grantedItemId) {
        revoke(player, grantedItemId, COMMAND_SOURCE);
    }

    public static void revoke(Player player, ResourceLocation grantedItemId, ResourceLocation sourceId) {
        update(player, ModAttachments.get(player).withGrantedItemSource(grantedItemId, sourceId, false));
    }

    public static void clearGrantedItems(Player player) {
        update(player, ModAttachments.get(player).clearGrantedItemSources());
    }

    public static void syncSourceItems(Player player, ResourceLocation sourceId, Collection<ResourceLocation> grantedItemIds) {
        AbilityData currentData = ModAttachments.get(player).withManagedGrantSource(sourceId, true);
        AbilityData updatedData = syncSourceItems(player instanceof ServerPlayer server ? server : null, currentData, sourceId, grantedItemIds);
        update(player, updatedData);
    }

    static AbilityData syncSourceItems(
            @Nullable ServerPlayer player,
            AbilityData currentData,
            ResourceLocation sourceId,
            Collection<ResourceLocation> grantedItemIds
    ) {
        AbilityData updatedData = currentData.withManagedGrantSource(sourceId, true);
        Set<ResourceLocation> desiredItems = new LinkedHashSet<>(grantedItemIds);
        for (ResourceLocation grantedItemId : Set.copyOf(updatedData.grantedItems())) {
            if (updatedData.grantedItemSourcesFor(grantedItemId).contains(sourceId) && !desiredItems.contains(grantedItemId)) {
                updatedData = updatedData.withGrantedItemSource(grantedItemId, sourceId, false);
            }
        }
        for (ResourceLocation grantedItemId : desiredItems) {
            updatedData = updatedData.withGrantedItemSource(grantedItemId, sourceId, true);
        }
        return updatedData;
    }

    static AbilityData revokeSourceItems(
            @Nullable ServerPlayer player,
            AbilityData currentData,
            ResourceLocation sourceId
    ) {
        AbilityData updatedData = currentData;
        for (ResourceLocation grantedItemId : Set.copyOf(updatedData.grantedItems())) {
            if (updatedData.grantedItemSourcesFor(grantedItemId).contains(sourceId)) {
                updatedData = updatedData.withGrantedItemSource(grantedItemId, sourceId, false);
            }
        }
        return updatedData;
    }

    private static void update(Player player, AbilityData updatedData) {
        AbilityData previousData = ModAttachments.get(player);
        if (!updatedData.equals(previousData)) {
            ModAttachments.set(player, updatedData);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            AbilityData currentData = ModAttachments.get(serverPlayer);
            GrantedItemRuntime.grantNewlyGrantedItems(serverPlayer, previousData, currentData);
            GrantedItemRuntime.restoreMissingUndroppableItems(serverPlayer, currentData);
            GrantedItemRuntime.reconcile(serverPlayer);
        }
    }
}

