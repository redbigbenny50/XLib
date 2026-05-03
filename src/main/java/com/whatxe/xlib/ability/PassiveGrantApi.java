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

public final class PassiveGrantApi {
    public static final ResourceLocation COMMAND_SOURCE =
            ResourceLocation.fromNamespaceAndPath(XLib.MODID, "command_passive");

    private PassiveGrantApi() {}

    public static boolean hasPassive(Player player, ResourceLocation passiveId) {
        return ModAttachments.get(player).hasPassive(passiveId);
    }

    public static boolean hasPassive(AbilityData data, ResourceLocation passiveId) {
        return data.hasPassive(passiveId);
    }

    public static Set<ResourceLocation> grantedPassives(Player player) {
        return Set.copyOf(ModAttachments.get(player).grantedPassives());
    }

    public static void grant(Player player, ResourceLocation passiveId) {
        grant(player, passiveId, COMMAND_SOURCE);
    }

    public static void grant(Player player, ResourceLocation passiveId, ResourceLocation sourceId) {
        ServerPlayer serverPlayer = player instanceof ServerPlayer server ? server : null;
        AbilityData updatedData = applyGrantSourceChange(serverPlayer, ModAttachments.get(player), passiveId, sourceId, true);
        update(player, updatedData);
    }

    public static void grant(Player player, Collection<ResourceLocation> passiveIds, ResourceLocation sourceId) {
        for (ResourceLocation passiveId : new LinkedHashSet<>(passiveIds)) {
            grant(player, passiveId, sourceId);
        }
    }

    public static void revoke(Player player, ResourceLocation passiveId) {
        revoke(player, passiveId, COMMAND_SOURCE);
    }

    public static void revoke(Player player, ResourceLocation passiveId, ResourceLocation sourceId) {
        ServerPlayer serverPlayer = player instanceof ServerPlayer server ? server : null;
        AbilityData updatedData = applyGrantSourceChange(serverPlayer, ModAttachments.get(player), passiveId, sourceId, false);
        update(player, updatedData);
    }

    public static void clearPassives(Player player) {
        AbilityData currentData = ModAttachments.get(player);
        AbilityData updatedData = currentData;
        if (player instanceof ServerPlayer serverPlayer) {
            for (ResourceLocation passiveId : Set.copyOf(currentData.grantedPassives())) {
                PassiveDefinition passive = PassiveApi.findPassive(passiveId).orElse(null);
                if (passive != null) {
                    updatedData = passive.onRevoked(serverPlayer, updatedData);
                }
            }
        }
        update(player, updatedData.clearPassiveGrantSources());
    }

    public static void syncSourcePassives(Player player, ResourceLocation sourceId, Collection<ResourceLocation> passiveIds) {
        AbilityData currentData = ModAttachments.get(player).withManagedGrantSource(sourceId, true);
        AbilityData updatedData = player instanceof ServerPlayer serverPlayer
                ? syncSourcePassives(serverPlayer, currentData, sourceId, passiveIds)
                : syncSourcePassives(null, currentData, sourceId, passiveIds);
        update(player, updatedData);
    }

    static AbilityData syncSourcePassives(
            @Nullable ServerPlayer player,
            AbilityData currentData,
            ResourceLocation sourceId,
            Collection<ResourceLocation> passiveIds
    ) {
        AbilityData updatedData = currentData.withManagedGrantSource(sourceId, true);
        Set<ResourceLocation> desiredPassives = new LinkedHashSet<>(passiveIds);
        for (ResourceLocation passiveId : Set.copyOf(updatedData.grantedPassives())) {
            if (updatedData.passiveGrantSourcesFor(passiveId).contains(sourceId) && !desiredPassives.contains(passiveId)) {
                updatedData = applyGrantSourceChange(player, updatedData, passiveId, sourceId, false);
            }
        }
        for (ResourceLocation passiveId : desiredPassives) {
            updatedData = applyGrantSourceChange(player, updatedData, passiveId, sourceId, true);
        }
        return updatedData;
    }

    static AbilityData revokeSourcePassives(
            @Nullable ServerPlayer player,
            AbilityData currentData,
            ResourceLocation sourceId
    ) {
        AbilityData updatedData = currentData;
        for (ResourceLocation passiveId : Set.copyOf(updatedData.grantedPassives())) {
            if (updatedData.passiveGrantSourcesFor(passiveId).contains(sourceId)) {
                updatedData = applyGrantSourceChange(player, updatedData, passiveId, sourceId, false);
            }
        }
        return updatedData;
    }

    private static AbilityData applyGrantSourceChange(
            @Nullable ServerPlayer player,
            AbilityData currentData,
            ResourceLocation passiveId,
            ResourceLocation sourceId,
            boolean granted
    ) {
        boolean wasGranted = currentData.hasPassive(passiveId);
        AbilityData updatedData = currentData.withPassiveGrantSource(passiveId, sourceId, granted);
        boolean isGranted = updatedData.hasPassive(passiveId);

        if (player == null || wasGranted == isGranted) {
            return updatedData;
        }

        PassiveDefinition passive = PassiveApi.findPassive(passiveId).orElse(null);
        if (passive == null) {
            return updatedData;
        }

        if (isGranted) {
            if (passive.firstFailedGrantRequirement(player, updatedData).isPresent()) {
                return currentData;
            }
            AbilityData grantedData = passive.onGranted(player, updatedData);
            passive.playSounds(player, PassiveSoundTrigger.GRANTED);
            return grantedData;
        }

        AbilityData revokedData = passive.onRevoked(player, updatedData);
        passive.playSounds(player, PassiveSoundTrigger.REVOKED);
        return revokedData;
    }

    private static void update(Player player, AbilityData updatedData) {
        if (!updatedData.equals(ModAttachments.get(player))) {
            ModAttachments.set(player, updatedData);
        }
    }
}

