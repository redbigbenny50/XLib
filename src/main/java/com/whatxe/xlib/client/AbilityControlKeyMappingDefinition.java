package com.whatxe.xlib.client;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

public record AbilityControlKeyMappingDefinition(
        ResourceLocation id,
        String translationKey,
        InputConstants.Type inputType,
        int defaultCode
) {
    public AbilityControlKeyMappingDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(translationKey, "translationKey");
        Objects.requireNonNull(inputType, "inputType");
    }

    public static AbilityControlKeyMappingDefinition keyboard(ResourceLocation id, String translationKey, int defaultCode) {
        return new AbilityControlKeyMappingDefinition(id, translationKey, InputConstants.Type.KEYSYM, defaultCode);
    }
}
