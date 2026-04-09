package com.whatxe.xlib.ability;

import com.whatxe.xlib.XLib;
import com.whatxe.xlib.attachment.ModAttachments;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class AbilityGrantApi {
    public static final ResourceLocation COMMAND_SOURCE =
            ResourceLocation.fromNamespaceAndPath(XLib.MODID, "command");
    public static final ResourceLocation COMMAND_ACTIVATION_BLOCK_SOURCE =
            ResourceLocation.fromNamespaceAndPath(XLib.MODID, "command_activation_block");

    private AbilityGrantApi() {}

    public static boolean isRestricted(Player player) {
        return ModAttachments.get(player).abilityAccessRestricted();
    }

    public static boolean canUse(Player player, ResourceLocation abilityId) {
        return canUse(ModAttachments.get(player), abilityId);
    }

    public static boolean canUse(AbilityData data, ResourceLocation abilityId) {
        return data.canUseAbility(abilityId);
    }

    public static boolean canView(Player player, AbilityDefinition ability) {
        return canView(player, ModAttachments.get(player), ability);
    }

    public static boolean canView(Player player, AbilityData data, AbilityDefinition ability) {
        return firstViewFailure(player, data, ability).isEmpty();
    }

    public static boolean canAssign(Player player, AbilityDefinition ability) {
        return canAssign(player, ModAttachments.get(player), ability);
    }

    public static boolean canAssign(Player player, AbilityData data, AbilityDefinition ability) {
        return firstAssignmentFailure(player, data, ability).isEmpty();
    }

    public static boolean canActivate(Player player, AbilityDefinition ability) {
        return canActivate(player, ModAttachments.get(player), ability);
    }

    public static boolean canActivate(Player player, AbilityData data, AbilityDefinition ability) {
        return firstActivationFailure(player, data, ability).isEmpty();
    }

    public static Optional<Component> firstViewFailure(Player player, AbilityData data, AbilityDefinition ability) {
        if (!canUse(data, ability.id())) {
            return Optional.of(Component.translatable("message.xlib.ability_not_granted", ability.displayName()));
        }
        if (isCommandGranted(data, ability.id())) {
            return Optional.empty();
        }
        return ability.firstFailedRenderRequirement(player, data);
    }

    public static Optional<Component> firstAssignmentFailure(Player player, AbilityData data, AbilityDefinition ability) {
        Optional<Component> viewFailure = firstViewFailure(player, data, ability);
        if (viewFailure.isPresent()) {
            return viewFailure;
        }
        StateControlStatus controlStatus = StatePolicyApi.controlStatus(data, ability);
        if (controlStatus.assignmentBlocked()) {
            return Optional.of(Component.translatable("message.xlib.ability_locked", ability.displayName()));
        }
        if (isCommandGranted(data, ability.id())) {
            return Optional.empty();
        }
        return ability.firstFailedAssignRequirement(player, data);
    }

    public static Optional<Component> firstActivationFailure(Player player, AbilityData data, AbilityDefinition ability) {
        if (!canUse(data, ability.id())) {
            return Optional.of(Component.translatable("message.xlib.ability_not_granted", ability.displayName()));
        }
        Optional<Component> profileFailure = ProfileApi.firstAbilityUseFailure(player);
        if (profileFailure.isPresent()) {
            return profileFailure;
        }
        StateControlStatus controlStatus = StatePolicyApi.controlStatus(data, ability);
        if (controlStatus.suppressed()) {
            return Optional.of(Component.translatable("message.xlib.ability_suppressed", ability.displayName()));
        }
        if (controlStatus.silenced()) {
            return Optional.of(Component.translatable("message.xlib.ability_silenced", ability.displayName()));
        }
        if (data.isAbilityActivationBlocked(ability.id())) {
            return Optional.of(Component.translatable("message.xlib.ability_temporarily_blocked", ability.displayName()));
        }
        if (isCommandGranted(data, ability.id())) {
            return Optional.empty();
        }
        return ability.firstFailedActivationRequirement(player, data);
    }

    public static Set<ResourceLocation> grantedAbilities(Player player) {
        return Set.copyOf(ModAttachments.get(player).grantedAbilities());
    }

    public static Set<ResourceLocation> grantSources(Player player, ResourceLocation abilityId) {
        return Set.copyOf(ModAttachments.get(player).abilityGrantSourcesFor(abilityId));
    }

    public static boolean isCommandGranted(Player player, ResourceLocation abilityId) {
        return isCommandGranted(ModAttachments.get(player), abilityId);
    }

    public static Set<ResourceLocation> blockedAbilities(Player player) {
        return Set.copyOf(ModAttachments.get(player).abilityActivationBlockSources().keySet());
    }

    public static Set<ResourceLocation> activationBlockSources(Player player, ResourceLocation abilityId) {
        return Set.copyOf(ModAttachments.get(player).activationBlockSourcesFor(abilityId));
    }

    public static void grant(Player player, ResourceLocation abilityId) {
        grant(player, abilityId, COMMAND_SOURCE);
    }

    public static void grant(Player player, ResourceLocation abilityId, ResourceLocation sourceId) {
        update(player, ModAttachments.get(player).withAbilityGrantSource(abilityId, sourceId, true));
    }

    public static void grant(Player player, Collection<ResourceLocation> abilityIds, ResourceLocation sourceId) {
        AbilityData data = ModAttachments.get(player);
        for (ResourceLocation abilityId : new LinkedHashSet<>(abilityIds)) {
            data = data.withAbilityGrantSource(abilityId, sourceId, true);
        }
        update(player, data);
    }

    public static void revoke(Player player, ResourceLocation abilityId) {
        revoke(player, abilityId, COMMAND_SOURCE);
    }

    public static void revoke(Player player, ResourceLocation abilityId, ResourceLocation sourceId) {
        update(player, sanitize(ModAttachments.get(player).withAbilityGrantSource(abilityId, sourceId, false)));
    }

    public static void clearGrants(Player player) {
        update(player, sanitize(ModAttachments.get(player).clearAbilityGrantSources()));
    }

    public static void blockActivation(Player player, ResourceLocation abilityId) {
        blockActivation(player, abilityId, COMMAND_ACTIVATION_BLOCK_SOURCE);
    }

    public static void blockActivation(Player player, ResourceLocation abilityId, ResourceLocation sourceId) {
        update(player, sanitize(ModAttachments.get(player).withAbilityActivationBlockSource(abilityId, sourceId, true)));
    }

    public static void unblockActivation(Player player, ResourceLocation abilityId) {
        unblockActivation(player, abilityId, COMMAND_ACTIVATION_BLOCK_SOURCE);
    }

    public static void unblockActivation(Player player, ResourceLocation abilityId, ResourceLocation sourceId) {
        update(player, sanitize(ModAttachments.get(player).withAbilityActivationBlockSource(abilityId, sourceId, false)));
    }

    public static void clearActivationBlocks(Player player) {
        update(player, sanitize(ModAttachments.get(player).clearAbilityActivationBlockSources()));
    }

    public static void clearActivationBlockSource(Player player, ResourceLocation sourceId) {
        update(player, sanitize(ModAttachments.get(player).clearAbilityActivationBlockSource(sourceId)));
    }

    public static void syncActivationBlocks(Player player, ResourceLocation sourceId, Collection<ResourceLocation> abilityIds) {
        AbilityData data = ModAttachments.get(player).withManagedGrantSource(sourceId, true);
        Set<ResourceLocation> desiredAbilities = new LinkedHashSet<>(abilityIds);
        for (ResourceLocation abilityId : Set.copyOf(data.abilityActivationBlockSources().keySet())) {
            if (data.activationBlockSourcesFor(abilityId).contains(sourceId) && !desiredAbilities.contains(abilityId)) {
                data = data.withAbilityActivationBlockSource(abilityId, sourceId, false);
            }
        }
        for (ResourceLocation abilityId : desiredAbilities) {
            data = data.withAbilityActivationBlockSource(abilityId, sourceId, true);
        }
        update(player, sanitize(data));
    }

    public static void setRestricted(Player player, boolean restricted) {
        update(player, sanitize(ModAttachments.get(player).withAbilityAccessRestricted(restricted)));
    }

    public static void grantOnly(Player player, ResourceLocation abilityId) {
        AbilityData updatedData = ModAttachments.get(player)
                .clearAbilityGrantSources()
                .withAbilityGrantSource(abilityId, COMMAND_SOURCE, true)
                .withAbilityAccessRestricted(true);
        update(player, updatedData);
    }

    public static void setGrantedAbilities(Player player, Collection<ResourceLocation> abilityIds, boolean restricted) {
        AbilityData updatedData = ModAttachments.get(player).clearAbilityGrantSources();
        for (ResourceLocation abilityId : new LinkedHashSet<>(abilityIds)) {
            updatedData = updatedData.withAbilityGrantSource(abilityId, COMMAND_SOURCE, true);
        }
        update(player, sanitize(updatedData.withAbilityAccessRestricted(restricted)));
    }

    public static void syncSourceAbilities(Player player, ResourceLocation sourceId, Collection<ResourceLocation> abilityIds) {
        AbilityData updatedData = ModAttachments.get(player).withManagedGrantSource(sourceId, true);
        Set<ResourceLocation> desiredAbilities = new LinkedHashSet<>(abilityIds);
        for (ResourceLocation abilityId : Set.copyOf(updatedData.grantedAbilities())) {
            if (updatedData.abilityGrantSourcesFor(abilityId).contains(sourceId) && !desiredAbilities.contains(abilityId)) {
                updatedData = updatedData.withAbilityGrantSource(abilityId, sourceId, false);
            }
        }
        for (ResourceLocation abilityId : desiredAbilities) {
            updatedData = updatedData.withAbilityGrantSource(abilityId, sourceId, true);
        }
        update(player, sanitize(updatedData));
    }

    public static void pruneManagedSources(Player player, Set<ResourceLocation> seenSources) {
        AbilityData updatedData = ModAttachments.get(player);
        ServerPlayer serverPlayer = player instanceof ServerPlayer server ? server : null;
        for (ResourceLocation sourceId : Set.copyOf(updatedData.managedGrantSources())) {
            if (!seenSources.contains(sourceId)) {
                updatedData = revokeAbilitySource(updatedData, sourceId);
                updatedData = updatedData.clearAbilityActivationBlockSource(sourceId);
                updatedData = StatePolicyApi.revokeSourcePolicies(updatedData, sourceId);
                updatedData = StateFlagApi.revokeSourceFlags(updatedData, sourceId);
                updatedData = PassiveGrantApi.revokeSourcePassives(serverPlayer, updatedData, sourceId);
                updatedData = GrantedItemGrantApi.revokeSourceItems(serverPlayer, updatedData, sourceId);
                updatedData = RecipePermissionApi.revokeSourcePermissions(serverPlayer, updatedData, sourceId);
                updatedData = updatedData.withManagedGrantSource(sourceId, false);
            }
        }
        update(player, sanitize(updatedData));
    }

    public static AbilityData sanitize(AbilityData data) {
        if (!data.abilityAccessRestricted()) {
            return data;
        }

        AbilityData sanitized = data;
        for (ResourceLocation containerId : sanitized.containerState().containerIds().isEmpty()
                ? java.util.List.of(AbilitySlotContainerApi.PRIMARY_CONTAINER_ID)
                : sanitized.containerState().containerIds()) {
            int pageCount = Math.max(1, AbilitySlotContainerApi.resolvedPageCount(sanitized, containerId));
            int slotCount = Math.max(1, AbilitySlotContainerApi.resolvedSlotsPerPage(sanitized, containerId));
            for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
                for (int slot = 0; slot < slotCount; slot++) {
                    AbilitySlotReference slotReference = new AbilitySlotReference(containerId, pageIndex, slot);
                    ResourceLocation abilityId = sanitized.abilityInSlot(slotReference).orElse(null);
                    if (abilityId != null && !sanitized.canUseAbility(abilityId)) {
                        sanitized = sanitized.withAbilityInSlot(slotReference, null);
                    }
                }
            }
        }

        for (ResourceLocation activeAbilityId : Set.copyOf(sanitized.activeModes())) {
            if (!sanitized.canUseAbility(activeAbilityId)) {
                sanitized = sanitized.withMode(activeAbilityId, false);
            }
        }

        return sanitized;
    }

    private static AbilityData revokeAbilitySource(AbilityData data, ResourceLocation sourceId) {
        AbilityData updatedData = data;
        for (ResourceLocation abilityId : Set.copyOf(updatedData.grantedAbilities())) {
            if (updatedData.abilityGrantSourcesFor(abilityId).contains(sourceId)) {
                updatedData = updatedData.withAbilityGrantSource(abilityId, sourceId, false);
            }
        }
        return updatedData;
    }

    private static boolean isCommandGranted(AbilityData data, ResourceLocation abilityId) {
        return data.abilityGrantSourcesFor(abilityId).contains(COMMAND_SOURCE);
    }

    private static void update(Player player, AbilityData updatedData) {
        if (!updatedData.equals(ModAttachments.get(player))) {
            ModAttachments.set(player, updatedData);
        }
    }
}

