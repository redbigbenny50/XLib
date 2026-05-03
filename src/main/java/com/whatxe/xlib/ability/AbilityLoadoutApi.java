package com.whatxe.xlib.ability;

import com.whatxe.xlib.attachment.ModAttachments;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public final class AbilityLoadoutApi {
    private AbilityLoadoutApi() {}

    public static Optional<ResourceLocation> assignedAbilityId(AbilityData data, int slot) {
        return assignedAbilityId(data, AbilitySlotReference.primary(slot));
    }

    public static Optional<ResourceLocation> assignedAbilityId(AbilityData data, AbilitySlotReference slotReference) {
        return data.abilityInSlot(slotReference);
    }

    public static Optional<ResourceLocation> assignedAbilityId(AbilityData data, @Nullable ResourceLocation modeId, int slot) {
        return assignedAbilityId(data, modeId, AbilitySlotReference.primary(slot));
    }

    public static Optional<ResourceLocation> assignedAbilityId(
            AbilityData data,
            @Nullable ResourceLocation modeId,
            AbilitySlotReference slotReference
    ) {
        if (modeId == null) {
            return assignedAbilityId(data, slotReference);
        }
        return data.modeAbilityInSlot(modeId, slotReference);
    }

    public static Optional<ResourceLocation> resolvedAbilityId(AbilityData data, int slot) {
        return resolvedAbilityId(data, AbilitySlotReference.primary(slot));
    }

    public static Optional<ResourceLocation> resolvedAbilityId(AbilityData data, AbilitySlotReference slotReference) {
        Optional<ResourceLocation> comboOverride = data.comboOverrideInSlot(slotReference);
        if (comboOverride.isPresent()) {
            return comboOverride;
        }

        Optional<ResourceLocation> modeOverlay = ModeApi.resolveOverlayAbility(data, slotReference);
        if (modeOverlay.isPresent()) {
            return modeOverlay;
        }

        Optional<ResourceLocation> modePresetAbility = resolveModePresetAbility(data, slotReference);
        if (modePresetAbility.isPresent()) {
            return modePresetAbility;
        }

        return data.abilityInSlot(slotReference);
    }

    public static Optional<AbilityDefinition> resolvedAbility(AbilityData data, int slot) {
        return resolvedAbility(data, AbilitySlotReference.primary(slot));
    }

    public static Optional<AbilityDefinition> resolvedAbility(AbilityData data, AbilitySlotReference slotReference) {
        return resolvedAbilityId(data, slotReference).flatMap(AbilityApi::findAbility);
    }

    public static boolean hasModeOverlay(AbilityData data, int slot) {
        return hasModeOverlay(data, AbilitySlotReference.primary(slot));
    }

    public static boolean hasModeOverlay(AbilityData data, AbilitySlotReference slotReference) {
        return ModeApi.resolveOverlayAbility(data, slotReference).isPresent();
    }

    public static boolean hasComboOverride(AbilityData data, int slot) {
        return hasComboOverride(data, AbilitySlotReference.primary(slot));
    }

    public static boolean hasComboOverride(AbilityData data, AbilitySlotReference slotReference) {
        return data.comboOverrideInSlot(slotReference).isPresent();
    }

    public static boolean assign(Player player, int slot, @Nullable ResourceLocation abilityId) {
        return assign(player, AbilitySlotReference.primary(slot), abilityId, null);
    }

    public static boolean assign(Player player, int slot, @Nullable ResourceLocation abilityId, @Nullable ResourceLocation modeId) {
        return assign(player, AbilitySlotReference.primary(slot), abilityId, modeId);
    }

    public static boolean assign(
            Player player,
            AbilitySlotReference slotReference,
            @Nullable ResourceLocation abilityId,
            @Nullable ResourceLocation modeId
    ) {
        if (slotReference.slotIndex() < 0 || slotReference.pageIndex() < 0) {
            return false;
        }

        AbilityData currentData = ModAttachments.get(player);
        if (abilityId != null) {
            AbilityDefinition definition = AbilityApi.findAbility(abilityId).orElse(null);
            if (definition == null) {
                if (player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.displayClientMessage(Component.translatable("message.xlib.ability_missing"), true);
                }
                return false;
            }

            Optional<Component> assignmentFailure = AbilityGrantApi.firstAssignmentFailure(player, currentData, definition);
            if (assignmentFailure.isPresent()) {
                if (player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.displayClientMessage(assignmentFailure.get(), true);
                }
                return false;
            }
        }

        AbilityData updatedData = modeId == null
                ? currentData.withAbilityInSlot(slotReference, abilityId)
                : currentData.withModeAbilityInSlot(modeId, slotReference, abilityId);
        if (!updatedData.equals(currentData)) {
            ModAttachments.set(player, updatedData);
            return true;
        }
        return false;
    }

    public static boolean clear(Player player, int slot) {
        return clear(player, AbilitySlotReference.primary(slot));
    }

    public static boolean clear(Player player, AbilitySlotReference slotReference) {
        return assign(player, slotReference, null, null);
    }

    public static boolean clearModePreset(Player player, ResourceLocation modeId, int slot) {
        return clearModePreset(player, modeId, AbilitySlotReference.primary(slot));
    }

    public static boolean clearModePreset(Player player, ResourceLocation modeId, AbilitySlotReference slotReference) {
        return assign(player, slotReference, null, modeId);
    }

    private static Optional<ResourceLocation> resolveModePresetAbility(AbilityData data, AbilitySlotReference slotReference) {
        for (ModeDefinition mode : ModeApi.activeModes(data)) {
            Optional<ResourceLocation> presetAbility = data.modeAbilityInSlot(mode.abilityId(), slotReference);
            if (presetAbility.isPresent()) {
                return presetAbility;
            }
        }
        return Optional.empty();
    }
}
