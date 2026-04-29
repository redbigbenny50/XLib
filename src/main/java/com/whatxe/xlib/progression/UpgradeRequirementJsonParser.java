package com.whatxe.xlib.progression;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

public final class UpgradeRequirementJsonParser {
    private UpgradeRequirementJsonParser() {}

    public static UpgradeRequirement parse(JsonElement element) {
        return parse(element, "$");
    }

    private static UpgradeRequirement parse(JsonElement element, String path) {
        try {
            if (element == null || element.isJsonNull()) {
                return always();
            }
            if (element.isJsonPrimitive()) {
                JsonPrimitive primitive = element.getAsJsonPrimitive();
                if (primitive.isBoolean()) {
                    return primitive.getAsBoolean() ? always() : never();
                }
                throw new IllegalArgumentException("Expected a boolean, object, or array upgrade requirement declaration");
            }
            if (element.isJsonArray()) {
                return UpgradeRequirements.all(parseArray(element.getAsJsonArray(), path));
            }

            JsonObject object = GsonHelper.convertToJsonObject(element, "upgrade requirement");
            if (!object.has("type")) {
                throw new IllegalArgumentException("Missing 'type' field");
            }
            String type = GsonHelper.getAsString(object, "type");
            return switch (type) {
                case "always" -> always();
                case "never" -> never();
                case "all" -> UpgradeRequirements.all(parseChildren(object, "requirements", path));
                case "any" -> UpgradeRequirements.any(parseChildren(object, "requirements", path));
                case "not" -> invert(parse(readRequiredElement(object, "requirement", path), path + ".requirement"));
                case "advancement" -> UpgradeRequirements.advancement(readId(object, "advancement", path));
                case "counter_at_least" -> UpgradeRequirements.counterAtLeast(
                        readId(object, "counter", path),
                        GsonHelper.getAsInt(object, "amount")
                );
                case "points_at_least" -> UpgradeRequirements.pointsAtLeast(
                        readId(object, object.has("point_type") ? "point_type" : "point", path),
                        GsonHelper.getAsInt(object, "amount")
                );
                case "node_unlocked" -> UpgradeRequirements.nodeUnlocked(readId(object, "node", path));
                case "any_node_unlocked" -> UpgradeRequirements.anyNodeUnlocked(readIds(object, "node", "nodes", path));
                case "identity_active" -> UpgradeRequirements.identityActive(readId(object, "identity", path));
                case "track_completed" -> UpgradeRequirements.trackCompleted(readId(object, "track", path));
                default -> throw new IllegalArgumentException("Unknown upgrade requirement type: " + type);
            };
        } catch (RuntimeException exception) {
            throw withPath(path, exception);
        }
    }

    private static UpgradeRequirement always() {
        return UpgradeRequirement.of(Component.literal("Always"), (player, data) -> java.util.Optional.empty());
    }

    private static UpgradeRequirement never() {
        Component description = Component.literal("Unavailable");
        return UpgradeRequirement.of(description, (player, data) -> java.util.Optional.of(description));
    }

    private static UpgradeRequirement invert(UpgradeRequirement requirement) {
        Component description = Component.literal("Not ").append(requirement.description());
        return UpgradeRequirement.of(description, (player, data) -> requirement.validate(player, data).isPresent()
                ? java.util.Optional.empty()
                : java.util.Optional.of(description));
    }

    private static List<UpgradeRequirement> parseArray(JsonArray array, String path) {
        List<UpgradeRequirement> requirements = new ArrayList<>();
        for (int index = 0; index < array.size(); index++) {
            requirements.add(parse(array.get(index), path + "[" + index + "]"));
        }
        if (requirements.isEmpty()) {
            throw new IllegalArgumentException("Upgrade requirement arrays must contain at least one child requirement");
        }
        return List.copyOf(requirements);
    }

    private static List<UpgradeRequirement> parseChildren(JsonObject object, String key, String path) {
        if (!object.has(key)) {
            throw new IllegalArgumentException("Missing '" + key + "' array");
        }
        return parseArray(GsonHelper.getAsJsonArray(object, key), path + "." + key);
    }

    private static JsonElement readRequiredElement(JsonObject object, String key, String path) {
        if (!object.has(key)) {
            throw new IllegalArgumentException("Missing '" + key + "' field");
        }
        return object.get(key);
    }

    private static List<ResourceLocation> readIds(JsonObject object, String singleKey, String pluralKey, String path) {
        List<ResourceLocation> values = new ArrayList<>();
        if (singleKey != null && object.has(singleKey)) {
            values.add(readId(object, singleKey, path));
        }
        if (pluralKey != null && object.has(pluralKey)) {
            JsonArray array = GsonHelper.getAsJsonArray(object, pluralKey);
            for (int index = 0; index < array.size(); index++) {
                try {
                    values.add(ResourceLocation.parse(array.get(index).getAsString()));
                } catch (RuntimeException exception) {
                    throw new IllegalArgumentException(
                            "Invalid resource id in '" + pluralKey + "[" + index + "]': " + exception.getMessage(),
                            exception
                    );
                }
            }
        }
        if (values.isEmpty()) {
            throw new IllegalArgumentException("Expected at least one id entry in '" + (pluralKey != null ? pluralKey : singleKey) + "'");
        }
        return List.copyOf(values);
    }

    private static ResourceLocation readId(JsonObject object, String key, String path) {
        Objects.requireNonNull(path, "path");
        if (!object.has(key)) {
            throw new IllegalArgumentException("Missing '" + key + "' field");
        }
        try {
            return ResourceLocation.parse(GsonHelper.getAsString(object, key));
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Invalid resource id in '" + key + "': " + exception.getMessage(), exception);
        }
    }

    private static RequirementParseException withPath(String path, RuntimeException exception) {
        if (exception instanceof RequirementParseException parseException) {
            return parseException;
        }
        return new RequirementParseException("Invalid upgrade requirement at " + path + ": " + exception.getMessage(), exception);
    }

    private static final class RequirementParseException extends IllegalArgumentException {
        private RequirementParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
