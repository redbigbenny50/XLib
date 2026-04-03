package com.whatxe.xlib.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.whatxe.xlib.XLib;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.neoforged.fml.loading.FMLPaths;

public final class CombatBarPreferences {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("xlib-client.json");
    private static DetailMode detailMode = DetailMode.COMPACT;
    private static ColorPreset colorPreset = ColorPreset.DEFAULT;
    private static boolean loaded;

    private CombatBarPreferences() {}

    public static void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        if (!Files.exists(CONFIG_PATH)) {
            save();
            return;
        }

        try {
            JsonObject root = GSON.fromJson(Files.readString(CONFIG_PATH, StandardCharsets.UTF_8), JsonObject.class);
            if (root == null) {
                save();
                return;
            }
            if (root.has("detail_mode")) {
                detailMode = DetailMode.valueOf(root.get("detail_mode").getAsString());
            }
            if (root.has("color_preset")) {
                colorPreset = ColorPreset.valueOf(root.get("color_preset").getAsString());
            }
        } catch (RuntimeException | IOException exception) {
            XLib.LOGGER.warn("Failed to load XLib client preferences from {}", CONFIG_PATH, exception);
            detailMode = DetailMode.COMPACT;
            colorPreset = ColorPreset.DEFAULT;
            save();
        }
    }

    public static void save() {
        JsonObject root = new JsonObject();
        root.addProperty("detail_mode", detailMode.name());
        root.addProperty("color_preset", colorPreset.name());
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            XLib.LOGGER.warn("Failed to save XLib client preferences to {}", CONFIG_PATH, exception);
        }
    }

    public static DetailMode detailMode() {
        return detailMode;
    }

    public static int mapHudColor(int color) {
        return colorPreset.mapColor(color);
    }

    public enum DetailMode {
        COMPACT,
        DETAILED
    }

    public enum ColorPreset {
        DEFAULT,
        DEUTERANOPIA,
        PROTANOPIA,
        TRITANOPIA;

        private int mapColor(int color) {
            if (this == DEFAULT) {
                return color;
            }

            int alpha = color >>> 24;
            int red = (color >>> 16) & 0xFF;
            int green = (color >>> 8) & 0xFF;
            int blue = color & 0xFF;

            return switch (this) {
                case DEFAULT -> color;
                case DEUTERANOPIA -> remapByDominantChannel(alpha, red, green, blue, 0xE69F00, 0x56B4E9, 0xCC79A7);
                case PROTANOPIA -> remapByDominantChannel(alpha, red, green, blue, 0xF0E442, 0x0072B2, 0xD55E00);
                case TRITANOPIA -> remapByDominantChannel(alpha, red, green, blue, 0x009E73, 0xE69F00, 0xCC79A7);
            };
        }

        private static int remapByDominantChannel(
                int alpha,
                int red,
                int green,
                int blue,
                int dominantRed,
                int dominantGreen,
                int dominantBlue
        ) {
            if (red >= green && red >= blue) {
                return (alpha << 24) | dominantRed;
            }
            if (green >= red && green >= blue) {
                return (alpha << 24) | dominantGreen;
            }
            return (alpha << 24) | dominantBlue;
        }
    }
}
