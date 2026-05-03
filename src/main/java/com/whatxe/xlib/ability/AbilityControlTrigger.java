package com.whatxe.xlib.ability;

import java.util.Locale;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public record AbilityControlTrigger(
        AbilityControlTriggerType type,
        int code,
        boolean shift,
        boolean control,
        boolean alt,
        @Nullable ResourceLocation keyMappingId
) {
    public AbilityControlTrigger {
        Objects.requireNonNull(type, "type");
        if (code < 0 && type != AbilityControlTriggerType.KEY_MAPPING) {
            throw new IllegalArgumentException("code cannot be negative for " + type);
        }
    }

    public static AbilityControlTrigger numberRow(int zeroBasedIndex) {
        return new AbilityControlTrigger(AbilityControlTriggerType.NUMBER_ROW, zeroBasedIndex, false, false, false, null);
    }

    public static AbilityControlTrigger key(int glfwKeyCode) {
        return new AbilityControlTrigger(AbilityControlTriggerType.KEYSYM, glfwKeyCode, false, false, false, null);
    }

    public static AbilityControlTrigger key(int glfwKeyCode, boolean shift, boolean control, boolean alt) {
        return new AbilityControlTrigger(AbilityControlTriggerType.KEYSYM, glfwKeyCode, shift, control, alt, null);
    }

    public static AbilityControlTrigger mouseButton(int button) {
        return new AbilityControlTrigger(AbilityControlTriggerType.MOUSE_BUTTON, button, false, false, false, null);
    }

    public static AbilityControlTrigger keyMapping(ResourceLocation keyMappingId) {
        return new AbilityControlTrigger(AbilityControlTriggerType.KEY_MAPPING, -1, false, false, false, Objects.requireNonNull(keyMappingId, "keyMappingId"));
    }

    public String hintLabel() {
        String base = switch (this.type) {
            case NUMBER_ROW -> Integer.toString(this.code + 1);
            case KEYSYM -> keyName(this.code);
            case MOUSE_BUTTON -> "M" + (this.code + 1);
            case KEY_MAPPING -> this.keyMappingId != null ? this.keyMappingId.getPath().toUpperCase(Locale.ROOT) : "?";
        };
        StringBuilder builder = new StringBuilder();
        if (this.control) {
            builder.append("Ctrl+");
        }
        if (this.shift) {
            builder.append("Shift+");
        }
        if (this.alt) {
            builder.append("Alt+");
        }
        return builder.append(base).toString();
    }

    private static String keyName(int glfwKeyCode) {
        if (glfwKeyCode >= 32 && glfwKeyCode <= 126) {
            return Character.toString((char) glfwKeyCode).toUpperCase(Locale.ROOT);
        }
        return "Key" + glfwKeyCode;
    }
}
