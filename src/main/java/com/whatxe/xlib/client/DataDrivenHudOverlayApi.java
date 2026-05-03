package com.whatxe.xlib.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.whatxe.xlib.XLib;
import com.whatxe.xlib.ability.AbilityApi;
import com.whatxe.xlib.ability.AbilityData;
import com.whatxe.xlib.ability.AbilityDefinition;
import com.whatxe.xlib.ability.AbilityGrantApi;
import com.whatxe.xlib.ability.AbilityRequirement;
import com.whatxe.xlib.ability.AbilityRequirementJsonParser;
import com.whatxe.xlib.ability.AbilityResourceDefinition;
import com.whatxe.xlib.ability.AbilityRuntime;
import com.whatxe.xlib.ability.AbilityScoreboardApi;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

public final class DataDrivenHudOverlayApi {
    private static final Gson GSON = new GsonBuilder().create();
    private static final String DIRECTORY = XLib.MODID + "/hud_overlays";
    private static volatile Map<ResourceLocation, HudOverlayDefinition> loadedOverlays = Map.of();

    private DataDrivenHudOverlayApi() {}

    public static void bootstrap() {}

    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new ReloadListener());
    }

    public static List<ResourceLocation> allOverlayIds() {
        return List.copyOf(loadedOverlays.keySet());
    }

    public static void render(GuiGraphics guiGraphics, Minecraft minecraft, AbilityData data) {
        if (minecraft.player == null || loadedOverlays.isEmpty()) {
            return;
        }

        List<HudOverlayDefinition> sorted = loadedOverlays.values().stream()
                .sorted(Comparator.comparingInt(HudOverlayDefinition::priority).thenComparing(definition -> definition.id().toString()))
                .toList();
        for (HudOverlayDefinition overlay : sorted) {
            try {
                if (overlay.requirement().validate(minecraft.player, data).isPresent()) {
                    continue;
                }
                overlay.render(guiGraphics, minecraft, data);
            } catch (RuntimeException exception) {
                XLib.LOGGER.error("Failed to render HUD overlay {}", overlay.id(), exception);
            }
        }
    }

    static void setLoadedOverlaysForTesting(Map<ResourceLocation, HudOverlayDefinition> overlays) {
        loadedOverlays = Map.copyOf(overlays);
    }

    static HudOverlayDefinition parseDefinition(ResourceLocation fileId, JsonElement element) {
        JsonObject object = GsonHelper.convertToJsonObject(element, "hud overlay");
        String type = GsonHelper.getAsString(object, "type");
        AbilityRequirement requirement = AbilityRequirementJsonParser.parse(
                object.get("when"),
                ClientConditionApi::requireCondition
        );
        HudOverlayAnchor anchor = object.has("anchor")
                ? HudOverlayAnchor.parse(GsonHelper.getAsString(object, "anchor"))
                : HudOverlayAnchor.TOP_LEFT;
        int x = object.has("x") ? GsonHelper.getAsInt(object, "x") : 0;
        int y = object.has("y") ? GsonHelper.getAsInt(object, "y") : 0;
        int priority = object.has("priority") ? GsonHelper.getAsInt(object, "priority") : 0;
        double scale = object.has("scale") ? GsonHelper.getAsDouble(object, "scale") : 1.0D;
        if (scale <= 0.0D) {
            throw new IllegalArgumentException("HUD overlay scale must be positive");
        }

        return switch (type) {
            case "text" -> new TextHudOverlayDefinition(
                    fileId,
                    requirement,
                    anchor,
                    x,
                    y,
                    priority,
                    scale,
                    object.has("text") ? GsonHelper.getAsString(object, "text") : "",
                    object.has("append_score_objective") ? GsonHelper.getAsString(object, "append_score_objective") : null,
                    readColor(object, "color", 0xFFFFFFFF),
                    !object.has("shadow") || GsonHelper.getAsBoolean(object, "shadow")
            );
            case "status_badge" -> new StatusBadgeHudOverlayDefinition(
                    fileId,
                    requirement,
                    anchor,
                    x,
                    y,
                    priority,
                    scale,
                    object.has("text") ? GsonHelper.getAsString(object, "text") : "",
                    object.has("append_score_objective") ? GsonHelper.getAsString(object, "append_score_objective") : null,
                    readColor(object, "text_color", 0xFFFFFFFF),
                    readColor(object, "background_color", 0xC0202020),
                    readColor(object, "border_color", 0),
                    object.has("padding_x") ? GsonHelper.getAsInt(object, "padding_x") : 4,
                    object.has("padding_y") ? GsonHelper.getAsInt(object, "padding_y") : 2,
                    !object.has("shadow") || GsonHelper.getAsBoolean(object, "shadow")
            );
            case "ability_status_badge" -> new AbilityStatusHudOverlayDefinition(
                    fileId,
                    requirement,
                    anchor,
                    x,
                    y,
                    priority,
                    scale,
                    readId(object, "ability"),
                    object.has("text") ? GsonHelper.getAsString(object, "text") : "",
                    !object.has("show_icon") || GsonHelper.getAsBoolean(object, "show_icon"),
                    object.has("icon_size") ? GsonHelper.getAsInt(object, "icon_size") : 16,
                    !object.has("show_when_ready") || GsonHelper.getAsBoolean(object, "show_when_ready"),
                    readColor(object, "text_color", 0xFFFFFFFF),
                    readColor(object, "background_color", 0xC0202020),
                    readColor(object, "border_color", 0),
                    object.has("padding_x") ? GsonHelper.getAsInt(object, "padding_x") : 4,
                    object.has("padding_y") ? GsonHelper.getAsInt(object, "padding_y") : 2,
                    !object.has("shadow") || GsonHelper.getAsBoolean(object, "shadow")
            );
            case "texture" -> new TextureHudOverlayDefinition(
                    fileId,
                    requirement,
                    anchor,
                    x,
                    y,
                    priority,
                    scale,
                    ResourceLocation.parse(GsonHelper.getAsString(object, "texture")),
                    GsonHelper.getAsInt(object, "width"),
                    GsonHelper.getAsInt(object, "height")
            );
            case "resource_bar" -> new ResourceBarHudOverlayDefinition(
                    fileId,
                    requirement,
                    anchor,
                    x,
                    y,
                    priority,
                    readResourceLayout(object, scale),
                    readId(object, "resource")
            );
            case "icon" -> parseIconDefinition(fileId, object, requirement, anchor, x, y, priority, scale);
            default -> throw new IllegalArgumentException("Unknown HUD overlay type: " + type);
        };
    }

    private static ResourceLocation readId(JsonObject object, String key) {
        return ResourceLocation.parse(GsonHelper.getAsString(object, key));
    }

    private static HudOverlayDefinition parseIconDefinition(
            ResourceLocation fileId,
            JsonObject object,
            AbilityRequirement requirement,
            HudOverlayAnchor anchor,
            int x,
            int y,
            int priority,
            double scale
    ) {
        boolean hasAbility = object.has("ability");
        boolean hasItem = object.has("item");
        if (hasAbility == hasItem) {
            throw new IllegalArgumentException("Icon overlays must declare exactly one of 'ability' or 'item'");
        }
        int size = object.has("size") ? GsonHelper.getAsInt(object, "size") : 16;
        if (size <= 0) {
            throw new IllegalArgumentException("HUD icon size must be positive");
        }
        ItemStack itemStack = hasItem
                ? new ItemStack(BuiltInRegistries.ITEM.getOptional(readId(object, "item"))
                        .orElseThrow(() -> new IllegalArgumentException("Unknown item id in HUD icon: " + GsonHelper.getAsString(object, "item"))))
                : null;
        if (hasItem && object.has("count")) {
            itemStack.setCount(Math.max(1, GsonHelper.getAsInt(object, "count")));
        }
        return new IconHudOverlayDefinition(
                fileId,
                requirement,
                anchor,
                x,
                y,
                priority,
                scale,
                size,
                hasAbility ? readId(object, "ability") : null,
                itemStack
        );
    }

    private static AbilityResourceHudLayout readResourceLayout(JsonObject object, double scale) {
        String orientationValue = object.has("orientation")
                ? GsonHelper.getAsString(object, "orientation").trim().toUpperCase(java.util.Locale.ROOT)
                : AbilityResourceHudOrientation.HORIZONTAL.name();
        AbilityResourceHudOrientation orientation = AbilityResourceHudOrientation.valueOf(orientationValue);
        int width = object.has("width")
                ? GsonHelper.getAsInt(object, "width")
                : orientation == AbilityResourceHudOrientation.VERTICAL ? 18 : 104;
        int height = object.has("height")
                ? GsonHelper.getAsInt(object, "height")
                : orientation == AbilityResourceHudOrientation.VERTICAL ? 54 : 14;
        return AbilityResourceHudLayout.builder()
                .anchor(AbilityResourceHudAnchor.ABOVE_HOTBAR_CENTER)
                .orientation(orientation)
                .width(Math.max(1, (int) Math.ceil(width * scale)))
                .height(Math.max(1, (int) Math.ceil(height * scale)))
                .spacing(0)
                .showName(!object.has("show_name") || GsonHelper.getAsBoolean(object, "show_name"))
                .showValue(!object.has("show_value") || GsonHelper.getAsBoolean(object, "show_value"))
                .build();
    }

    private static int readColor(JsonObject object, String key, int fallback) {
        if (!object.has(key)) {
            return fallback;
        }
        JsonElement element = object.get(key);
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            String raw = element.getAsString().trim();
            if (raw.startsWith("#")) {
                raw = raw.substring(1);
            }
            if (raw.startsWith("0x") || raw.startsWith("0X")) {
                raw = raw.substring(2);
            }
            return (int) Long.parseLong(raw, 16);
        }
        return element.getAsInt();
    }

    private static String renderText(String text, String appendScoreObjective, Minecraft minecraft) {
        return text + (appendScoreObjective != null
                ? AbilityScoreboardApi.readScore(minecraft.player, appendScoreObjective).orElse(0)
                : "");
    }

    private static String abilityStatusText(Minecraft minecraft, AbilityData data, AbilityDefinition ability, boolean showWhenReady) {
        if (minecraft.player == null || ability.firstFailedRenderRequirement(minecraft.player, data).isPresent()) {
            return null;
        }
        if (!AbilityGrantApi.canUse(data, ability.id())) {
            return Component.translatable("hud.xlib.ability_status.locked").getString();
        }
        if (data.isModeActive(ability.id())) {
            int remainingTicks = data.activeDurationFor(ability.id());
            return remainingTicks > 0
                    ? Component.translatable("hud.xlib.ability_status.active_with_duration", formatSeconds(remainingTicks)).getString()
                    : Component.translatable("hud.xlib.ability_status.active").getString();
        }

        int cooldownTicks = AbilityRuntime.displayCooldownTicks(ability, data);
        if (cooldownTicks > 0) {
            return Component.translatable("hud.xlib.ability_status.cooldown", formatSeconds(cooldownTicks)).getString();
        }
        if (AbilityGrantApi.firstActivationFailure(minecraft.player, data, ability).isPresent()) {
            return Component.translatable("hud.xlib.ability_status.blocked").getString();
        }
        if (ability.usesCharges()) {
            int charges = data.chargeCountFor(ability.id(), ability.maxCharges());
            if (charges < ability.maxCharges()) {
                return Component.translatable("hud.xlib.ability_status.charges", charges, ability.maxCharges()).getString();
            }
        }
        return showWhenReady ? Component.translatable("hud.xlib.ability_status.ready").getString() : null;
    }

    private static String formatSeconds(int ticks) {
        return String.format(java.util.Locale.ROOT, "%.1f", ticks / 20.0D);
    }

    private static void drawBadgeFrame(
            GuiGraphics guiGraphics,
            int originX,
            int originY,
            int badgeWidth,
            int badgeHeight,
            int backgroundColor,
            int borderColor
    ) {
        guiGraphics.fill(originX, originY, originX + badgeWidth, originY + badgeHeight, backgroundColor);
        if (borderColor != 0) {
            guiGraphics.fill(originX, originY, originX + badgeWidth, originY + 1, borderColor);
            guiGraphics.fill(originX, originY + badgeHeight - 1, originX + badgeWidth, originY + badgeHeight, borderColor);
            guiGraphics.fill(originX, originY, originX + 1, originY + badgeHeight, borderColor);
            guiGraphics.fill(originX + badgeWidth - 1, originY, originX + badgeWidth, originY + badgeHeight, borderColor);
        }
    }

    private static int anchorX(HudOverlayAnchor anchor, int width, int screenWidth) {
        int centerX = screenWidth / 2;
        return switch (anchor) {
            case TOP_LEFT, BOTTOM_LEFT -> 8;
            case TOP_CENTER, CENTER, BOTTOM_CENTER, ABOVE_HOTBAR_CENTER -> centerX - width / 2;
            case TOP_RIGHT, BOTTOM_RIGHT -> screenWidth - width - 8;
            case ABOVE_HOTBAR_LEFT -> centerX - 91;
            case ABOVE_HOTBAR_RIGHT -> centerX + 91 - width;
        };
    }

    private static int anchorY(HudOverlayAnchor anchor, int height, int screenHeight) {
        return switch (anchor) {
            case TOP_LEFT, TOP_CENTER, TOP_RIGHT -> 8;
            case CENTER -> screenHeight / 2 - height / 2;
            case BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT -> screenHeight - height - 8;
            case ABOVE_HOTBAR_LEFT, ABOVE_HOTBAR_CENTER, ABOVE_HOTBAR_RIGHT -> screenHeight - 22 - height - 4;
        };
    }

    sealed interface HudOverlayDefinition permits
            TextHudOverlayDefinition,
            StatusBadgeHudOverlayDefinition,
            AbilityStatusHudOverlayDefinition,
            TextureHudOverlayDefinition,
            ResourceBarHudOverlayDefinition,
            IconHudOverlayDefinition {
        ResourceLocation id();

        AbilityRequirement requirement();

        int priority();

        void render(GuiGraphics guiGraphics, Minecraft minecraft, AbilityData data);
    }

    record TextHudOverlayDefinition(
            ResourceLocation id,
            AbilityRequirement requirement,
            HudOverlayAnchor anchor,
            int x,
            int y,
            int priority,
            double scale,
            String text,
            String appendScoreObjective,
            int color,
            boolean shadow
    ) implements HudOverlayDefinition {
        @Override
        public void render(GuiGraphics guiGraphics, Minecraft minecraft, AbilityData data) {
            Font font = minecraft.font;
            String renderedText = renderText(this.text, this.appendScoreObjective, minecraft);
            int width = (int) Math.ceil(font.width(renderedText) * this.scale);
            int height = (int) Math.ceil(font.lineHeight * this.scale);
            int originX = anchorX(this.anchor, width, guiGraphics.guiWidth()) + this.x;
            int originY = anchorY(this.anchor, height, guiGraphics.guiHeight()) + this.y;
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(originX, originY, 0.0D);
            guiGraphics.pose().scale((float) this.scale, (float) this.scale, 1.0F);
            guiGraphics.drawString(font, renderedText, 0, 0, this.color, this.shadow);
            guiGraphics.pose().popPose();
        }
    }

    record StatusBadgeHudOverlayDefinition(
            ResourceLocation id,
            AbilityRequirement requirement,
            HudOverlayAnchor anchor,
            int x,
            int y,
            int priority,
            double scale,
            String text,
            String appendScoreObjective,
            int textColor,
            int backgroundColor,
            int borderColor,
            int paddingX,
            int paddingY,
            boolean shadow
    ) implements HudOverlayDefinition {
        @Override
        public void render(GuiGraphics guiGraphics, Minecraft minecraft, AbilityData data) {
            Font font = minecraft.font;
            String renderedText = renderText(this.text, this.appendScoreObjective, minecraft);
            int textWidth = font.width(renderedText);
            int textHeight = font.lineHeight;
            int badgeWidth = (int) Math.ceil((textWidth + this.paddingX * 2) * this.scale);
            int badgeHeight = (int) Math.ceil((textHeight + this.paddingY * 2) * this.scale);
            int originX = anchorX(this.anchor, badgeWidth, guiGraphics.guiWidth()) + this.x;
            int originY = anchorY(this.anchor, badgeHeight, guiGraphics.guiHeight()) + this.y;
            drawBadgeFrame(guiGraphics, originX, originY, badgeWidth, badgeHeight, this.backgroundColor, this.borderColor);
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(originX + Math.ceil(this.paddingX * this.scale), originY + Math.ceil(this.paddingY * this.scale), 0.0D);
            guiGraphics.pose().scale((float) this.scale, (float) this.scale, 1.0F);
            guiGraphics.drawString(font, renderedText, 0, 0, this.textColor, this.shadow);
            guiGraphics.pose().popPose();
        }
    }

    record AbilityStatusHudOverlayDefinition(
            ResourceLocation id,
            AbilityRequirement requirement,
            HudOverlayAnchor anchor,
            int x,
            int y,
            int priority,
            double scale,
            ResourceLocation abilityId,
            String text,
            boolean showIcon,
            int iconSize,
            boolean showWhenReady,
            int textColor,
            int backgroundColor,
            int borderColor,
            int paddingX,
            int paddingY,
            boolean shadow
    ) implements HudOverlayDefinition {
        AbilityStatusHudOverlayDefinition {
            if (iconSize <= 0) {
                throw new IllegalArgumentException("Ability status badge icon_size must be positive");
            }
        }

        @Override
        public void render(GuiGraphics guiGraphics, Minecraft minecraft, AbilityData data) {
            AbilityDefinition ability = AbilityApi.findAbility(this.abilityId).orElse(null);
            if (ability == null) {
                return;
            }
            String statusText = abilityStatusText(minecraft, data, ability, this.showWhenReady);
            if (statusText == null) {
                return;
            }

            Font font = minecraft.font;
            String renderedText = this.text + statusText;
            int iconWidth = this.showIcon ? this.iconSize : 0;
            int iconGap = this.showIcon && !renderedText.isEmpty() ? 4 : 0;
            int textWidth = font.width(renderedText);
            int contentWidth = iconWidth + iconGap + textWidth;
            int contentHeight = Math.max(this.showIcon ? this.iconSize : 0, font.lineHeight);
            int badgeWidth = (int) Math.ceil((contentWidth + this.paddingX * 2) * this.scale);
            int badgeHeight = (int) Math.ceil((contentHeight + this.paddingY * 2) * this.scale);
            int originX = anchorX(this.anchor, badgeWidth, guiGraphics.guiWidth()) + this.x;
            int originY = anchorY(this.anchor, badgeHeight, guiGraphics.guiHeight()) + this.y;
            drawBadgeFrame(guiGraphics, originX, originY, badgeWidth, badgeHeight, this.backgroundColor, this.borderColor);

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(originX + Math.ceil(this.paddingX * this.scale), originY + Math.ceil(this.paddingY * this.scale), 0.0D);
            guiGraphics.pose().scale((float) this.scale, (float) this.scale, 1.0F);
            if (this.showIcon) {
                AbilityIconRenderer.render(
                        guiGraphics,
                        minecraft,
                        ability.icon(),
                        0,
                        Math.max(0, (contentHeight - this.iconSize) / 2),
                        this.iconSize,
                        this.iconSize
                );
            }
            guiGraphics.drawString(
                    font,
                    renderedText,
                    iconWidth + iconGap,
                    Math.max(0, (contentHeight - font.lineHeight) / 2),
                    this.textColor,
                    this.shadow
            );
            guiGraphics.pose().popPose();
        }
    }

    record TextureHudOverlayDefinition(
            ResourceLocation id,
            AbilityRequirement requirement,
            HudOverlayAnchor anchor,
            int x,
            int y,
            int priority,
            double scale,
            ResourceLocation texture,
            int width,
            int height
    ) implements HudOverlayDefinition {
        @Override
        public void render(GuiGraphics guiGraphics, Minecraft minecraft, AbilityData data) {
            int scaledWidth = (int) Math.ceil(this.width * this.scale);
            int scaledHeight = (int) Math.ceil(this.height * this.scale);
            int originX = anchorX(this.anchor, scaledWidth, guiGraphics.guiWidth()) + this.x;
            int originY = anchorY(this.anchor, scaledHeight, guiGraphics.guiHeight()) + this.y;
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(originX, originY, 0.0D);
            guiGraphics.pose().scale((float) this.scale, (float) this.scale, 1.0F);
            guiGraphics.blit(this.texture, 0, 0, 0, 0, this.width, this.height, this.width, this.height);
            guiGraphics.pose().popPose();
        }
    }

    record ResourceBarHudOverlayDefinition(
            ResourceLocation id,
            AbilityRequirement requirement,
            HudOverlayAnchor anchor,
            int x,
            int y,
            int priority,
            AbilityResourceHudLayout layout,
            ResourceLocation resourceId
    ) implements HudOverlayDefinition {
        @Override
        public void render(GuiGraphics guiGraphics, Minecraft minecraft, AbilityData data) {
            AbilityResourceDefinition resource = AbilityApi.findResource(this.resourceId).orElse(null);
            if (resource == null) {
                return;
            }
            int width = this.layout.width();
            int height = this.layout.height();
            int originX = anchorX(this.anchor, width, guiGraphics.guiWidth()) + this.x;
            int originY = anchorY(this.anchor, height, guiGraphics.guiHeight()) + this.y;
            CombatBarOverlay.renderDefaultResourceBar(
                    guiGraphics,
                    minecraft,
                    minecraft.player,
                    data,
                    resource,
                    this.layout,
                    originX,
                    originY
            );
        }
    }

    record IconHudOverlayDefinition(
            ResourceLocation id,
            AbilityRequirement requirement,
            HudOverlayAnchor anchor,
            int x,
            int y,
            int priority,
            double scale,
            int size,
            ResourceLocation abilityId,
            ItemStack itemStack
    ) implements HudOverlayDefinition {
        @Override
        public void render(GuiGraphics guiGraphics, Minecraft minecraft, AbilityData data) {
            int scaledSize = (int) Math.ceil(this.size * this.scale);
            int originX = anchorX(this.anchor, scaledSize, guiGraphics.guiWidth()) + this.x;
            int originY = anchorY(this.anchor, scaledSize, guiGraphics.guiHeight()) + this.y;
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(originX, originY, 0.0D);
            guiGraphics.pose().scale((float) this.scale, (float) this.scale, 1.0F);
            if (this.itemStack != null && !this.itemStack.isEmpty()) {
                guiGraphics.renderItem(this.itemStack, 0, 0);
                if (this.itemStack.getCount() > 1) {
                    guiGraphics.renderItemDecorations(minecraft.font, this.itemStack, 0, 0);
                }
            } else if (this.abilityId != null) {
                AbilityApi.findAbility(this.abilityId)
                        .ifPresentOrElse(
                                ability -> AbilityIconRenderer.render(guiGraphics, minecraft, ability.icon(), 0, 0, this.size, this.size),
                                () -> guiGraphics.drawString(minecraft.font, "?", 4, 4, 0xFFFFFFFF, true)
                        );
            }
            guiGraphics.pose().popPose();
        }
    }

    private static final class ReloadListener extends SimpleJsonResourceReloadListener {
        private ReloadListener() {
            super(GSON, DIRECTORY);
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> jsonById, ResourceManager resourceManager, ProfilerFiller profiler) {
            Map<ResourceLocation, HudOverlayDefinition> overlays = new LinkedHashMap<>();
            for (Map.Entry<ResourceLocation, JsonElement> entry : jsonById.entrySet()) {
                try {
                    overlays.put(entry.getKey(), parseDefinition(entry.getKey(), entry.getValue()));
                } catch (RuntimeException exception) {
                    XLib.LOGGER.error("Failed to parse HUD overlay {}", entry.getKey(), exception);
                }
            }
            loadedOverlays = Map.copyOf(overlays);
        }
    }
}
