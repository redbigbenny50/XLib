package com.whatxe.xlib.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.mojang.blaze3d.platform.InputConstants;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

class ModKeyMappingsTest {
    @Test
    void combatBarToggleKeybindDefaultsToLeftAlt() {
        assertEquals("key.xlib.toggle_combat_bar", ModKeyMappings.TOGGLE_COMBAT_BAR.getName());
        assertSame(InputConstants.Type.KEYSYM, ModKeyMappings.TOGGLE_COMBAT_BAR.getKey().getType());
        assertEquals(GLFW.GLFW_KEY_LEFT_ALT, ModKeyMappings.TOGGLE_COMBAT_BAR.getKey().getValue());
        assertEquals(ModKeyMappings.CATEGORY, ModKeyMappings.TOGGLE_COMBAT_BAR.getCategory());
    }
}
