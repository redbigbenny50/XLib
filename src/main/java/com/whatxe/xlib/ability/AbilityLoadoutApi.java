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
        return data.abilityInSlot(slot);
    }

    public static Optional<ResourceLocation> assignedAbilityId(AbilityData data, @Nullable ResourceLocation modeId, int slot) {
        if (modeId == null) {
            return assignedAbilityId(data, slot);
        }
        return data.modeAbilityInSlot(modeId, slot);
    }

    public static Optional<ResourceLocation> resolvedAbilityId(AbilityData data, int slot) {
        Optional<ResourceLocation> comboOverride = data.comboOverrideInSlot(slot);
        if (comboOverride.isPresent()) {
            return comboOverride;
        }

        Optional<ResourceLocation> modeOverlay = ModeApi.resolveOverlayAbility(data, slot);
        if (modeOverlay.isPresent()) {
            return modeOverlay;
        }

        Optional<ResourceLocation> modePresetAbility = resolveModePresetAbility(data, slot);
        if (modePresetAbility.isPresent()) {
            return modePresetAbility;
        }

        return data.abilityInSlot(slot);
    }

    public static Optional<AbilityDefinition> resolvedAbility(AbilityData data, int slot) {
        return resolvedAbilityId(data, slot).flatMap(AbilityApi::findAbility);
    }

    public static boolean hasModeOverlay(AbilityData data, int slot) {
        return ModeApi.resolveOverlayAbility(data, slot).isPresent();
    }

    public static boolean hasComboOverride(AbilityData data, int slot) {
        return data.comboOverrideInSlot(slot).isPresent();
    }

    public static boolean assign(Player player, int slot, @Nullable ResourceLocation abilityId) {
        return assign(player, slot, abilityId, null);
    }

    public static boolean assign(Player player, int slot, @Nullable ResourceLocation abilityId, @Nullable ResourceLocation modeId) {
        if (slot < 0 || slot >= AbilityData.SLOT_COUNT) {
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
                ? currentData.withAbilityInSlot(slot, abilityId)
                : currentData.withModeAbilityInSlot(modeId, slot, abilityId);
        if (!updatedData.equals(currentData)) {
            ModAttachments.set(player, updatedData);
            return true;
        }
        return false;
    }

    public static boolean clear(Player player, int slot) {
        return assign(player, slot, null);
    }

    public static boolean clearModePreset(Player player, ResourceLocation modeId, int slot) {
        return assign(player, slot, null, modeId);
    }

    private static Optional<ResourceLocation> resolveModePresetAbility(AbilityData data, int slot) {
        for (ModeDefinition mode : ModeApi.activeModes(data)) {
            Optional<ResourceLocation> presetAbility = data.modeAbilityInSlot(mode.abilityId(), slot);
            if (presetAbility.isPresent()) {
                return presetAbility;
            }
        }
        return Optional.empty();
    }
}
