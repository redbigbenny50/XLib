package com.whatxe.xlib.ability;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;

public final class DataDrivenDefinitionReaders {
    private DataDrivenDefinitionReaders() {}

    public static Set<ResourceLocation> readLocations(JsonObject object, String singleKey, String pluralKey) {
        Set<ResourceLocation> values = new LinkedHashSet<>();
        if (singleKey != null && object.has(singleKey)) {
            values.add(ResourceLocation.parse(GsonHelper.getAsString(object, singleKey)));
        }
        if (pluralKey != null && object.has(pluralKey)) {
            for (JsonElement element : GsonHelper.getAsJsonArray(object, pluralKey)) {
                values.add(ResourceLocation.parse(element.getAsString()));
            }
        }
        return Set.copyOf(values);
    }

    public static Component readComponent(JsonObject object, String key, Component fallback) {
        return object.has(key) ? parseComponent(object.get(key)) : fallback;
    }

    public static Component parseComponent(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return Component.empty();
        }
        if (element instanceof JsonPrimitive primitive && primitive.isString()) {
            return Component.literal(primitive.getAsString());
        }
        return Component.Serializer.fromJson(element, RegistryAccess.EMPTY);
    }

    public static AbilityIcon readRequiredIcon(JsonObject object) {
        if (!object.has("icon")) {
            throw new IllegalArgumentException("Definitions require an 'icon' field");
        }
        return parseIcon(object.get("icon"));
    }

    public static AbilityIcon readOptionalIcon(JsonObject object) {
        return object.has("icon") ? parseIcon(object.get("icon")) : null;
    }

    public static AbilityIcon parseIcon(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            throw new IllegalArgumentException("Icon definition cannot be null");
        }
        if (element instanceof JsonPrimitive primitive && primitive.isString()) {
            return AbilityIcon.ofTexture(ResourceLocation.parse(primitive.getAsString()));
        }
        JsonObject object = GsonHelper.convertToJsonObject(element, "icon");
        if (object.has("item")) {
            ResourceLocation itemId = ResourceLocation.parse(GsonHelper.getAsString(object, "item"));
            Item item = BuiltInRegistries.ITEM.getOptional(itemId)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown item id in icon: " + itemId));
            return AbilityIcon.ofItem(item);
        }
        if (object.has("texture")) {
            ResourceLocation textureId = ResourceLocation.parse(GsonHelper.getAsString(object, "texture"));
            int width = object.has("width") ? GsonHelper.getAsInt(object, "width") : 16;
            int height = object.has("height") ? GsonHelper.getAsInt(object, "height") : 16;
            if (width <= 0 || height <= 0) {
                throw new IllegalArgumentException("Texture icon width/height must be positive");
            }
            return AbilityIcon.ofTexture(textureId, width, height);
        }
        if (object.has("custom_renderer")) {
            return AbilityIcon.ofCustom(ResourceLocation.parse(GsonHelper.getAsString(object, "custom_renderer")));
        }
        throw new IllegalArgumentException("Icons must declare 'item', 'texture', or 'custom_renderer'");
    }
}
