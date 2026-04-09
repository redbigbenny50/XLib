package com.whatxe.xlib.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.whatxe.xlib.ability.AbilityControlBinding;
import com.whatxe.xlib.ability.AbilityControlProfileApi;
import com.whatxe.xlib.ability.AbilityControlTrigger;
import com.whatxe.xlib.ability.AbilityControlTriggerType;
import com.whatxe.xlib.ability.AbilityData;
import com.whatxe.xlib.attachment.ModAttachments;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

public final class AbilityControlInputHandler {
    private AbilityControlInputHandler() {}

    public static boolean handleKeyPress(Minecraft minecraft, int key, int modifiers) {
        if (minecraft.player == null) {
            return false;
        }
        AbilityData data = ModAttachments.get(minecraft.player);
        for (AbilityControlBinding binding : AbilityControlProfileApi.activeBindings(minecraft.player, data)) {
            if (matchesKey(binding.trigger(), key, modifiers) && AbilityControlActionHandlerApi.handle(minecraft, data, binding.action())) {
                return true;
            }
        }
        return false;
    }

    public static boolean handleMouseButton(Minecraft minecraft, int button, int modifiers) {
        if (minecraft.player == null) {
            return false;
        }
        AbilityData data = ModAttachments.get(minecraft.player);
        for (AbilityControlBinding binding : AbilityControlProfileApi.activeBindings(minecraft.player, data)) {
            if (matchesMouse(binding.trigger(), button, modifiers) && AbilityControlActionHandlerApi.handle(minecraft, data, binding.action())) {
                return true;
            }
        }
        return false;
    }

    public static void handleRegisteredKeyMappings(Minecraft minecraft) {
        if (minecraft.player == null) {
            return;
        }
        AbilityData data = ModAttachments.get(minecraft.player);
        for (AbilityControlBinding binding : AbilityControlProfileApi.activeBindings(minecraft.player, data)) {
            AbilityControlTrigger trigger = binding.trigger();
            if (trigger.type() == AbilityControlTriggerType.KEY_MAPPING
                    && trigger.keyMappingId() != null
                    && AbilityControlKeyMappingApi.consumeClick(trigger.keyMappingId())) {
                AbilityControlActionHandlerApi.handle(minecraft, data, binding.action());
            }
        }
    }

    public static Optional<String> slotHint(Minecraft minecraft, com.whatxe.xlib.ability.AbilitySlotReference slotReference) {
        if (minecraft.player == null) {
            return Optional.empty();
        }
        AbilityData data = ModAttachments.get(minecraft.player);
        for (AbilityControlBinding binding : AbilityControlProfileApi.activeBindings(minecraft.player, data)) {
            if (binding.action().type() == com.whatxe.xlib.ability.AbilityControlActionType.ACTIVATE_SLOT
                    && slotReference.containerId().equals(binding.action().containerId())
                    && slotReference.pageIndex() == data.activeContainerPage(slotReference.containerId())
                    && slotReference.slotIndex() == binding.action().slotIndex()) {
                return Optional.of(binding.trigger().hintLabel());
            }
        }
        return Optional.empty();
    }

    private static boolean matchesKey(AbilityControlTrigger trigger, int key, int modifiers) {
        if (trigger.type() == AbilityControlTriggerType.NUMBER_ROW) {
            int expectedKey = GLFW.GLFW_KEY_1 + trigger.code();
            int expectedKeypad = GLFW.GLFW_KEY_KP_1 + trigger.code();
            return (key == expectedKey || key == expectedKeypad) && modifiersMatch(trigger, modifiers);
        }
        if (trigger.type() == AbilityControlTriggerType.KEYSYM) {
            return trigger.code() == key && modifiersMatch(trigger, modifiers);
        }
        if (trigger.type() == AbilityControlTriggerType.KEY_MAPPING && trigger.keyMappingId() != null) {
            return AbilityControlKeyMappingApi.matches(trigger.keyMappingId(), InputConstants.Type.KEYSYM, key) && modifiersMatch(trigger, modifiers);
        }
        return false;
    }

    private static boolean matchesMouse(AbilityControlTrigger trigger, int button, int modifiers) {
        return trigger.type() == AbilityControlTriggerType.MOUSE_BUTTON
                && trigger.code() == button
                && modifiersMatch(trigger, modifiers);
    }

    private static boolean modifiersMatch(AbilityControlTrigger trigger, int modifiers) {
        boolean shift = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
        boolean control = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        boolean alt = (modifiers & GLFW.GLFW_MOD_ALT) != 0;
        return trigger.shift() == shift && trigger.control() == control && trigger.alt() == alt;
    }
}
