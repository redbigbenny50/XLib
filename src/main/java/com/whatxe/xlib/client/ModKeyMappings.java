package com.whatxe.xlib.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.whatxe.xlib.ability.AbilityLoadoutFeatureApi;
import com.whatxe.xlib.XLib;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public final class ModKeyMappings {
    public static final String CATEGORY = "key.categories." + XLib.MODID;

    public static final KeyMapping TOGGLE_COMBAT_BAR = new KeyMapping(
            "key." + XLib.MODID + ".toggle_combat_bar",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_ALT,
            CATEGORY
    );
    public static final KeyMapping OPEN_ABILITY_MENU = new KeyMapping(
            "key." + XLib.MODID + ".open_ability_menu",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            CATEGORY
    );
    public static final KeyMapping OPEN_PROGRESSION_MENU = new KeyMapping(
            "key." + XLib.MODID + ".open_progression_menu",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            CATEGORY
    );
    public static final KeyMapping CYCLE_LOADOUT = new KeyMapping(
            "key." + XLib.MODID + ".cycle_loadout",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            CATEGORY
    );

    private ModKeyMappings() {}

    public static void register(RegisterKeyMappingsEvent event) {
        AbilityControlKeyMappingApi.clearRuntimeMappings();
        event.register(TOGGLE_COMBAT_BAR);
        event.register(OPEN_ABILITY_MENU);
        event.register(OPEN_PROGRESSION_MENU);
        if (AbilityLoadoutFeatureApi.shouldRegisterQuickSwitchKeybind()) {
            event.register(CYCLE_LOADOUT);
        }
        for (AbilityControlKeyMappingDefinition definition : AbilityControlKeyMappingApi.definitions()) {
            event.register(AbilityControlKeyMappingApi.bind(definition, CATEGORY));
        }
    }
}

