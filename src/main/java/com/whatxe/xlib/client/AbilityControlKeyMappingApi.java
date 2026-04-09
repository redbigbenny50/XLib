package com.whatxe.xlib.client;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;

public final class AbilityControlKeyMappingApi {
    private static final Map<ResourceLocation, AbilityControlKeyMappingDefinition> DEFINITIONS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, KeyMapping> KEY_MAPPINGS = new LinkedHashMap<>();

    private AbilityControlKeyMappingApi() {}

    public static void register(AbilityControlKeyMappingDefinition definition) {
        AbilityControlKeyMappingDefinition resolvedDefinition = Objects.requireNonNull(definition, "definition");
        AbilityControlKeyMappingDefinition previous = DEFINITIONS.putIfAbsent(resolvedDefinition.id(), resolvedDefinition);
        if (previous != null) {
            throw new IllegalStateException("Duplicate control key mapping registration: " + resolvedDefinition.id());
        }
    }

    public static List<AbilityControlKeyMappingDefinition> definitions() {
        return List.copyOf(DEFINITIONS.values());
    }

    public static KeyMapping bind(AbilityControlKeyMappingDefinition definition, String category) {
        KeyMapping keyMapping = new KeyMapping(
                definition.translationKey(),
                net.neoforged.neoforge.client.settings.KeyConflictContext.IN_GAME,
                definition.inputType(),
                definition.defaultCode(),
                category
        );
        KEY_MAPPINGS.put(definition.id(), keyMapping);
        return keyMapping;
    }

    public static Optional<KeyMapping> find(ResourceLocation id) {
        return Optional.ofNullable(KEY_MAPPINGS.get(id));
    }

    public static boolean consumeClick(ResourceLocation id) {
        KeyMapping keyMapping = KEY_MAPPINGS.get(id);
        return keyMapping != null && keyMapping.consumeClick();
    }

    public static boolean matches(ResourceLocation id, InputConstants.Type inputType, int code) {
        KeyMapping keyMapping = KEY_MAPPINGS.get(id);
        return keyMapping != null && keyMapping.getKey().getType() == inputType && keyMapping.getKey().getValue() == code;
    }

    public static void clearRuntimeMappings() {
        KEY_MAPPINGS.clear();
    }
}
