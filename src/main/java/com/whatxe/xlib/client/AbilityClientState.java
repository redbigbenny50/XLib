package com.whatxe.xlib.client;

import com.whatxe.xlib.client.screen.AbilityMenuScreen;
import com.whatxe.xlib.client.screen.ProgressionMenuScreen;
import net.minecraft.client.Minecraft;

public final class AbilityClientState {
    private static boolean combatBarEnabled = false;
    private static int lockedHotbarSlot = -1;
    private static int highlightedSlot = -1;
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

        if (minecraft.screen == null) {
            while (ModKeyMappings.OPEN_ABILITY_MENU.consumeClick()) {
                minecraft.setScreen(new AbilityMenuScreen());
            }
            while (ModKeyMappings.OPEN_PROGRESSION_MENU.consumeClick()) {
                minecraft.setScreen(new ProgressionMenuScreen());
            }
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
                highlightedSlot = -1;
            }
        }
    }

    public static void flashSlot(int slot) {
        highlightedSlot = slot;
        highlightTicks = 10;
    }

    public static int highlightedSlot() {
        return highlightTicks > 0 ? highlightedSlot : -1;
    }

    private static void reset() {
        combatBarEnabled = false;
        lockedHotbarSlot = -1;
        highlightedSlot = -1;
        highlightTicks = 0;
    }
}

