package com.whatxe.xlib.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.whatxe.xlib.XLib;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public final class ModKeyMappings {
    public static final String CATEGORY = "key.categories." + XLib.MODID;

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

    private ModKeyMappings() {}

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(OPEN_ABILITY_MENU);
        event.register(OPEN_PROGRESSION_MENU);
    }
}

