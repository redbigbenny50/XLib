package com.whatxe.xlib.client;

import com.whatxe.xlib.client.AbilityLoadoutQuickSwitchApi;
import com.whatxe.xlib.ability.ProfileApi;
import com.whatxe.xlib.attachment.ModAttachments;
import com.whatxe.xlib.ability.AbilitySlotReference;
import com.whatxe.xlib.menu.AbilityMenuAccessApi;
import com.whatxe.xlib.menu.ProgressionMenuAccessApi;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

public final class AbilityClientState {
    private static boolean combatBarEnabled = false;
    private static int lockedHotbarSlot = -1;
    private static @Nullable AbilitySlotReference highlightedSlot = null;
    private static int highlightTicks = 0;

    private AbilityClientState() {}

    public static boolean isCombatBarActive() {
        return isCombatBarActive(Minecraft.getInstance());
    }

    public static boolean isCombatBarActive(Minecraft minecraft) {
        return minecraft.player != null
                && !minecraft.player.isSpectator()
                && combatBarEnabled
                && minecraft.screen == null;
    }

    public static void toggleCombatBar(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.player.isSpectator() || minecraft.screen != null) {
            return;
        }

        if (combatBarEnabled) {
            combatBarEnabled = false;
            if (lockedHotbarSlot >= 0) {
                minecraft.player.getInventory().selected = lockedHotbarSlot;
            }
            lockedHotbarSlot = -1;
        } else {
            combatBarEnabled = true;
            lockedHotbarSlot = minecraft.player.getInventory().selected;
            minecraft.player.getInventory().selected = lockedHotbarSlot;
        }
    }

    public static void tick(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.player.isSpectator()) {
            reset();
            return;
        }

        while (ModKeyMappings.TOGGLE_COMBAT_BAR.consumeClick()) {
            toggleCombatBar(minecraft);
        }

        if (minecraft.screen == null) {
            ProfileApi.firstAutoOpenPendingGroupId(ModAttachments.getProfiles(minecraft.player))
                    .ifPresent(groupId -> ProfileSelectionScreenFactoryApi.openActive(
                            minecraft,
                            new ProfileSelectionScreenContext(groupId)
                    ));
            if (minecraft.screen != null) {
                return;
            }
            while (ModKeyMappings.OPEN_ABILITY_MENU.consumeClick()) {
                if (!AbilityMenuAccessApi.decision(minecraft.player).isHidden()) {
                    AbilityMenuScreenFactoryApi.openActive(minecraft);
                }
            }
            while (ModKeyMappings.OPEN_PROGRESSION_MENU.consumeClick()) {
                if (!ProgressionMenuAccessApi.decision(minecraft.player).isHidden()) {
                    ProgressionMenuScreenFactoryApi.openActive(minecraft);
                }
            }
            while (ModKeyMappings.CYCLE_LOADOUT.consumeClick()) {
                AbilityLoadoutQuickSwitchApi.handleActive(minecraft);
            }
            AbilityControlInputHandler.handleRegisteredKeyMappings(minecraft);
        }

        if (combatBarEnabled) {
            if (lockedHotbarSlot < 0) {
                lockedHotbarSlot = minecraft.player.getInventory().selected;
            }
            if (minecraft.screen == null) {
                minecraft.player.getInventory().selected = lockedHotbarSlot;
            }
        }

        if (highlightTicks > 0) {
            highlightTicks--;
            if (highlightTicks == 0) {
                highlightedSlot = null;
            }
        }
    }

    public static void flashSlot(AbilitySlotReference slotReference) {
        highlightedSlot = slotReference;
        highlightTicks = 10;
    }

    public static void flashSlot(int slot) {
        flashSlot(AbilitySlotReference.primary(slot));
    }

    public static @Nullable AbilitySlotReference highlightedSlot() {
        return highlightTicks > 0 ? highlightedSlot : null;
    }

    private static void reset() {
        combatBarEnabled = false;
        lockedHotbarSlot = -1;
        highlightedSlot = null;
        highlightTicks = 0;
    }
}

