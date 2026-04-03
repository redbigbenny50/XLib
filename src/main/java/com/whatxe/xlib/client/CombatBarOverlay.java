package com.whatxe.xlib.client;

import com.whatxe.xlib.XLib;
import com.whatxe.xlib.ability.AbilityApi;
import com.whatxe.xlib.ability.AbilityData;
import com.whatxe.xlib.ability.AbilityDefinition;
import com.whatxe.xlib.ability.AbilityGrantApi;
import com.whatxe.xlib.ability.AbilityLoadoutApi;
import com.whatxe.xlib.ability.AbilityResourceDefinition;
import com.whatxe.xlib.ability.AbilityRuntime;
import com.whatxe.xlib.attachment.ModAttachments;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

public final class CombatBarOverlay {
    private static final int HOTBAR_SIDE_GAP = 6;
    private static final int HOTBAR_VERTICAL_GAP = 3;
    private static final ResourceLocation LAYER_ID =
            ResourceLocation.fromNamespaceAndPath(XLib.MODID, "combat_bar");
    private static final ResourceLocation HOTBAR_SPRITE = ResourceLocation.withDefaultNamespace("hud/hotbar");
    private static final ResourceLocation HOTBAR_SELECTION_SPRITE = ResourceLocation.withDefaultNamespace("hud/hotbar_selection");
    private static final AbilityResourceHudRegistration DEFAULT_RESOURCE_HUD =
            new AbilityResourceHudRegistration(null, AbilityResourceHudLayout.defaultLayout());

    private CombatBarOverlay() {}

    public static void registerLayer(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.SELECTED_ITEM_NAME, LAYER_ID, CombatBarOverlay::render);
    }

    private static void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || !AbilityClientState.isCombatBarActive(minecraft)) {
            return;
        }

        AbilityData data = ModAttachments.get(minecraft.player);
        int centerX = guiGraphics.guiWidth() / 2;
        int hotbarY = guiGraphics.guiHeight() - 22;
        int highlightedSlot = AbilityClientState.highlightedSlot();
        boolean detailedHud = CombatBarPreferences.detailMode() == CombatBarPreferences.DetailMode.DETAILED;

        guiGraphics.blitSprite(HOTBAR_SPRITE, centerX - 91, hotbarY, 182, 22);
        if (highlightedSlot >= 0) {
            guiGraphics.blitSprite(HOTBAR_SELECTION_SPRITE, centerX - 92 + highlightedSlot * 20, hotbarY - 1, 24, 23);
        }

        int labelCeilingY = renderResources(guiGraphics, data, centerX, hotbarY);

        Component activeName = null;
        for (int slot = 0; slot < AbilityData.SLOT_COUNT; slot++) {
            Optional<ResourceLocation> maybeAbilityId = AbilityLoadoutApi.resolvedAbilityId(data, slot);
            if (highlightedSlot == slot && maybeAbilityId.isPresent()) {
                activeName = AbilityApi.findAbility(maybeAbilityId.get())
                        .filter(ability -> AbilityGrantApi.canView(minecraft.player, data, ability))
                        .map(AbilityDefinition::displayName)
                        .orElse(null);
            }
            renderSlot(guiGraphics, data, slot, centerX - 90 + slot * 20 + 2, guiGraphics.guiHeight() - 19);
        }

        if (activeName != null && detailedHud) {
            int labelX = centerX - minecraft.font.width(activeName) / 2;
            int labelY = Math.max(8, Math.min(hotbarY - 12, labelCeilingY - 12));
            guiGraphics.drawString(minecraft.font, activeName, labelX, labelY, 0xFFFFFF, true);
        }
    }

    private static int renderResources(GuiGraphics guiGraphics, AbilityData data, int centerX, int hotbarY) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return hotbarY;
        }

        List<ResourceHudEntry> resources = new ArrayList<>();
        for (AbilityResourceDefinition resource : AbilityApi.allResources()) {
            if (resource.shouldRender(minecraft.player, data)) {
                resources.add(new ResourceHudEntry(
                        resource,
                        AbilityResourceHudRegistry.find(resource.id()).orElse(DEFAULT_RESOURCE_HUD)
                ));
            }
        }
        if (resources.isEmpty()) {
            return hotbarY;
        }

        resources.sort(Comparator
                .comparingInt((ResourceHudEntry entry) -> entry.registration.layout().priority())
                .thenComparing(entry -> entry.resource.id().toString()));

        EnumMap<AbilityResourceHudAnchor, List<ResourceHudEntry>> groupedResources = new EnumMap<>(AbilityResourceHudAnchor.class);
        for (AbilityResourceHudAnchor anchor : AbilityResourceHudAnchor.values()) {
            groupedResources.put(anchor, new ArrayList<>());
        }
        for (ResourceHudEntry resource : resources) {
            groupedResources.get(resource.registration.layout().anchor()).add(resource);
        }

        int aboveHotbarTopY = hotbarY;
        for (AbilityResourceHudAnchor anchor : AbilityResourceHudAnchor.values()) {
            List<ResourceHudEntry> entries = groupedResources.get(anchor);
            if (entries.isEmpty()) {
                continue;
            }

            if (anchor.besideHotbar()) {
                int resourceX = sideAnchorStartX(anchor, entries, centerX);
                for (ResourceHudEntry entry : entries) {
                    AbilityResourceHudLayout layout = entry.registration.layout();
                    AbilityResourceHudRenderer renderer = entry.registration.renderer() != null
                            ? entry.registration.renderer()
                            : CombatBarOverlay::renderDefaultResourceBar;
                    int resourceY = hotbarY + 22 - layout.height();
                    renderer.render(guiGraphics, minecraft, minecraft.player, data, entry.resource, layout, resourceX, resourceY);
                    resourceX += layout.width() + layout.spacing();
                }
                continue;
            }

            int totalHeight = totalStackSize(entries, false);
            int resourceY = switch (anchor) {
                case ABOVE_HOTBAR_LEFT -> guiGraphics.guiHeight() - minecraft.gui.leftHeight - totalHeight - HOTBAR_VERTICAL_GAP;
                case ABOVE_HOTBAR_CENTER -> guiGraphics.guiHeight() - Math.max(minecraft.gui.leftHeight, minecraft.gui.rightHeight) - totalHeight - HOTBAR_VERTICAL_GAP;
                case ABOVE_HOTBAR_RIGHT -> guiGraphics.guiHeight() - minecraft.gui.rightHeight - totalHeight - HOTBAR_VERTICAL_GAP;
                case TOP_LEFT, TOP_RIGHT -> 8;
                case LEFT_OF_HOTBAR, RIGHT_OF_HOTBAR -> hotbarY;
            };
            if (anchor.aboveHotbar()) {
                aboveHotbarTopY = Math.min(aboveHotbarTopY, resourceY);
            }

            for (ResourceHudEntry entry : entries) {
                AbilityResourceHudLayout layout = entry.registration.layout();
                int resourceX = resourceX(anchor, layout, centerX, guiGraphics.guiWidth());
                AbilityResourceHudRenderer renderer = entry.registration.renderer() != null
                        ? entry.registration.renderer()
                        : CombatBarOverlay::renderDefaultResourceBar;
                renderer.render(guiGraphics, minecraft, minecraft.player, data, entry.resource, layout, resourceX, resourceY);
                resourceY += layout.height() + layout.spacing();
            }
        }
        return aboveHotbarTopY;
    }

    private static void renderDefaultResourceBar(
            GuiGraphics guiGraphics,
            Minecraft minecraft,
            net.minecraft.world.entity.player.Player player,
            AbilityData data,
            AbilityResourceDefinition resource,
            AbilityResourceHudLayout layout,
            int x,
            int y
    ) {
        double currentAmount = data.resourceAmountExact(resource.id());
        int totalCapacity = Math.max(1, resource.totalCapacity());
        if (layout.orientation() == AbilityResourceHudOrientation.VERTICAL) {
            renderVerticalResourceBar(guiGraphics, minecraft, resource, layout, x, y, currentAmount, totalCapacity);
            return;
        }

        renderHorizontalResourceBar(guiGraphics, minecraft, resource, layout, x, y, currentAmount, totalCapacity);
    }

    private static void renderSlot(GuiGraphics guiGraphics, AbilityData data, int slot, int x, int y) {
        Minecraft minecraft = Minecraft.getInstance();
        String slotLabel = Integer.toString(slot + 1);
        guiGraphics.drawString(minecraft.font, slotLabel, x + 1, y + 8, 0xFFE3C26A, false);

        Optional<ResourceLocation> maybeAbilityId = AbilityLoadoutApi.resolvedAbilityId(data, slot);
        if (maybeAbilityId.isEmpty()) {
            return;
        }

        Optional<AbilityDefinition> maybeAbility = AbilityApi.findAbility(maybeAbilityId.get());
        if (maybeAbility.isEmpty()) {
            guiGraphics.drawString(minecraft.font, "?", x + 6, y + 4, 0xFFFF5555, true);
            return;
        }

        AbilityDefinition ability = maybeAbility.get();
        if (!AbilityGrantApi.canView(minecraft.player, data, ability)) {
            guiGraphics.fill(x, y, x + 16, y + 16, 0xAA1F1F1F);
            guiGraphics.drawString(minecraft.font, "?", x + 5, y + 4, 0xFFE38B8B, true);
            return;
        }

        AbilityIconRenderer.render(guiGraphics, minecraft, ability.icon(), x, y, 16, 16);

        if (data.isModeActive(ability.id())) {
            guiGraphics.fill(x, y + 14, x + 16, y + 16, 0x66000000);
            int activeWidth = ability.durationTicks() > 0
                    ? Math.max(1, Math.round(16.0F * data.activeDurationFor(ability.id()) / ability.durationTicks()))
                    : 16;
            guiGraphics.fill(x, y + 14, x + activeWidth, y + 16, CombatBarPreferences.mapHudColor(0xCC4CAF50));
        }

        if (ability.usesCharges()) {
            String chargeLabel = Integer.toString(data.chargeCountFor(ability.id(), ability.maxCharges()));
            guiGraphics.drawString(
                    minecraft.font,
                    chargeLabel,
                    x + 15 - minecraft.font.width(chargeLabel),
                    y,
                    0xFFFFFFFF,
                    true
            );
        }

        int cooldownTicks = AbilityRuntime.displayCooldownTicks(ability, data);
        if (cooldownTicks > 0) {
            String cooldownLabel = formatCooldown(cooldownTicks);
            guiGraphics.fill(x, y, x + 16, y + 16, 0x99000000);
            guiGraphics.drawString(
                    minecraft.font,
                    cooldownLabel,
                    x + 8 - minecraft.font.width(cooldownLabel) / 2,
                    y + 4,
                    0xFFFFFFFF,
                    true
            );
        }

        if (data.isAbilityActivationBlocked(ability.id())) {
            guiGraphics.fill(x, y + 12, x + 4, y + 16, CombatBarPreferences.mapHudColor(0xFFD45353));
        }
        if (AbilityLoadoutApi.hasModeOverlay(data, slot)) {
            guiGraphics.fill(x, y, x + 16, y + 2, CombatBarPreferences.mapHudColor(0xCC66B3FF));
        }
        if (AbilityLoadoutApi.hasComboOverride(data, slot)) {
            guiGraphics.fill(x, y + 12, x + 16, y + 14, CombatBarPreferences.mapHudColor(0xFFF1C24B));
        }
    }

    private static String formatCooldown(int cooldownTicks) {
        if (cooldownTicks > 20) {
            return Integer.toString((int)Math.ceil(cooldownTicks / 20.0D));
        }
        return String.format(Locale.ROOT, "%.1f", cooldownTicks / 20.0F);
    }

    private static void renderHorizontalResourceBar(
            GuiGraphics guiGraphics,
            Minecraft minecraft,
            AbilityResourceDefinition resource,
            AbilityResourceHudLayout layout,
            int x,
            int y,
            double currentAmount,
            int totalCapacity
    ) {
        int outerRight = x + layout.width();
        int outerBottom = y + layout.height();
        boolean detailedHud = CombatBarPreferences.detailMode() == CombatBarPreferences.DetailMode.DETAILED;
        String label = detailedHud
                ? fitLabel(minecraft, resource.displayName().getString(), Math.max(20, layout.width() / 2))
                : compactResourceLabel(resource, 3);
        String amountLabel = detailedHud
                ? formatExactAmount(currentAmount) + "/" + formatExactAmount(totalCapacity)
                : compactAmount(currentAmount);
        int labelWidth = Math.min(Math.max(20, minecraft.font.width(label) + 6), Math.max(24, layout.width() / 2));
        int valueWidth = Math.min(Math.max(20, minecraft.font.width(amountLabel) + 6), Math.max(24, layout.width() / 2));
        int barLeft = x + labelWidth + 2;
        int barRight = Math.max(barLeft + 8, outerRight - valueWidth - 2);
        int barTop = y + 2;
        int barBottom = outerBottom - 2;
        int usableWidth = Math.max(1, barRight - barLeft - 2);
        int fillWidth = (int) Math.round(usableWidth * (currentAmount / totalCapacity));
        int normalCapacityWidth = (int) Math.round(usableWidth * (resource.maxAmount() / (double) totalCapacity));
        int baseFillWidth = (int) Math.round(usableWidth * (Math.min(currentAmount, resource.maxAmount()) / totalCapacity));
        int overflowFillWidth = Math.max(0, fillWidth - normalCapacityWidth);
        int resourceColor = CombatBarPreferences.mapHudColor(resource.color());

        guiGraphics.fill(x, y, outerRight, outerBottom, 0xC0101010);
        guiGraphics.fill(x + 1, y + 1, outerRight - 1, outerBottom - 1, 0x7A000000);
        guiGraphics.fill(barLeft, barTop, barRight, barBottom, 0x66000000);
        if (baseFillWidth > 0) {
            guiGraphics.fill(barLeft + 1, barTop + 1, barLeft + 1 + baseFillWidth, barBottom - 1, resourceColor);
        }
        if (overflowFillWidth > 0) {
            int overflowColor = lightenColor(resourceColor, 0x28);
            guiGraphics.fill(
                    barLeft + 1 + normalCapacityWidth,
                    barTop + 1,
                    barLeft + 1 + normalCapacityWidth + overflowFillWidth,
                    barBottom - 1,
                    overflowColor
            );
        }
        if (resource.shieldStyle()) {
            guiGraphics.fill(x, y, outerRight, y + 1, 0xCCBFE7FF);
            guiGraphics.fill(x, outerBottom - 1, outerRight, outerBottom, 0xCCBFE7FF);
        }

        guiGraphics.drawString(minecraft.font, label, x + 3, y + 3, 0xFFF6E7BF, false);
        guiGraphics.drawString(
                minecraft.font,
                amountLabel,
                outerRight - 3 - minecraft.font.width(amountLabel),
                y + 3,
                0xFFFFFFFF,
                false
        );
    }

    private static void renderVerticalResourceBar(
            GuiGraphics guiGraphics,
            Minecraft minecraft,
            AbilityResourceDefinition resource,
            AbilityResourceHudLayout layout,
            int x,
            int y,
            double currentAmount,
            int totalCapacity
    ) {
        int footerHeight = Math.min(10, Math.max(8, layout.width() - 4));
        int frameRight = x + layout.width();
        int frameBottom = y + layout.height();
        int gaugeTop = y + 10;
        int gaugeBottom = frameBottom - footerHeight - 1;
        int usableHeight = Math.max(1, gaugeBottom - gaugeTop - 1);
        int fillHeight = (int) Math.round(usableHeight * (currentAmount / totalCapacity));
        int normalCapacityHeight = (int) Math.round(usableHeight * (resource.maxAmount() / (double) totalCapacity));
        int baseFillHeight = (int) Math.round(usableHeight * (Math.min(currentAmount, resource.maxAmount()) / totalCapacity));
        int overflowFillHeight = Math.max(0, fillHeight - normalCapacityHeight);
        int innerLeft = x + 2;
        int innerRight = frameRight - 2;
        boolean detailedHud = CombatBarPreferences.detailMode() == CombatBarPreferences.DetailMode.DETAILED;
        int resourceColor = CombatBarPreferences.mapHudColor(resource.color());

        guiGraphics.fill(x, y, frameRight, frameBottom, 0xC0101010);
        guiGraphics.fill(x + 1, y + 1, frameRight - 1, frameBottom - 1, 0x7A000000);
        guiGraphics.fill(innerLeft, gaugeTop, innerRight, gaugeBottom, 0x66000000);
        if (baseFillHeight > 0) {
            guiGraphics.fill(
                    innerLeft,
                    gaugeBottom - baseFillHeight,
                    innerRight,
                    gaugeBottom,
                    resourceColor
            );
        }
        if (overflowFillHeight > 0) {
            int overflowColor = lightenColor(resourceColor, 0x28);
            guiGraphics.fill(
                    innerLeft,
                    gaugeBottom - fillHeight,
                    innerRight,
                    gaugeBottom - baseFillHeight,
                    overflowColor
            );
        }
        guiGraphics.fill(x + 1, frameBottom - footerHeight, frameRight - 1, frameBottom - 1, 0xB0202020);
        if (resource.shieldStyle()) {
            guiGraphics.fill(x, y, frameRight, y + 1, 0xCCBFE7FF);
            guiGraphics.fill(x, frameBottom - 1, frameRight, frameBottom, 0xCCBFE7FF);
        }

        String amountLabel = detailedHud
                ? formatExactAmount(currentAmount) + "/" + formatExactAmount(totalCapacity)
                : compactAmount(currentAmount);
        guiGraphics.drawString(
                minecraft.font,
                fitLabel(minecraft, amountLabel, layout.width() - 2),
                x + Math.max(1, (layout.width() - minecraft.font.width(fitLabel(minecraft, amountLabel, layout.width() - 2))) / 2),
                y + 2,
                0xFFFFFFFF,
                false
        );
        String shortLabel = detailedHud
                ? fitLabel(minecraft, resource.displayName().getString(), layout.width() - 2)
                : compactResourceLabel(resource, 2);
        guiGraphics.drawString(
                minecraft.font,
                shortLabel,
                x + Math.max(1, (layout.width() - minecraft.font.width(shortLabel)) / 2),
                frameBottom - footerHeight + 1,
                0xFFF6E7BF,
                false
        );
    }

    private static int totalStackSize(List<ResourceHudEntry> entries, boolean horizontal) {
        int totalSize = 0;
        for (int index = 0; index < entries.size(); index++) {
            AbilityResourceHudLayout layout = entries.get(index).registration.layout();
            totalSize += horizontal ? layout.width() : layout.height();
            if (index < entries.size() - 1) {
                totalSize += layout.spacing();
            }
        }
        return totalSize;
    }

    private static int resourceX(
            AbilityResourceHudAnchor anchor,
            AbilityResourceHudLayout layout,
            int centerX,
            int guiWidth
    ) {
        return switch (anchor) {
            case ABOVE_HOTBAR_LEFT -> centerX - 91;
            case ABOVE_HOTBAR_CENTER -> centerX - layout.width() / 2;
            case ABOVE_HOTBAR_RIGHT -> centerX + 91 - layout.width();
            case TOP_LEFT -> 8;
            case TOP_RIGHT -> guiWidth - layout.width() - 8;
            case LEFT_OF_HOTBAR, RIGHT_OF_HOTBAR -> centerX - layout.width() / 2;
        };
    }

    private static int sideAnchorStartX(AbilityResourceHudAnchor anchor, List<ResourceHudEntry> entries, int centerX) {
        return switch (anchor) {
            case LEFT_OF_HOTBAR -> centerX - 91 - HOTBAR_SIDE_GAP - totalStackSize(entries, true);
            case RIGHT_OF_HOTBAR -> centerX + 91 + HOTBAR_SIDE_GAP;
            default -> centerX;
        };
    }

    private static String compactAmount(double amount) {
        int whole = (int) Math.floor(amount + 1.0E-9D);
        if (Math.abs(amount - whole) > 0.001D && amount < 100.0D) {
            return String.format(Locale.ROOT, "%.1f", amount);
        }
        if (whole >= 1000) {
            return (whole / 1000) + "k";
        }
        return Integer.toString(whole);
    }

    private static String compactResourceLabel(AbilityResourceDefinition resource, int maxChars) {
        String name = resource.displayName().getString().trim();
        if (name.isEmpty()) {
            return "?";
        }

        String[] words = name.split("\\s+");
        StringBuilder initials = new StringBuilder(maxChars);
        for (String word : words) {
            if (!word.isEmpty() && initials.length() < maxChars) {
                initials.append(Character.toUpperCase(word.charAt(0)));
            }
        }
        if (initials.length() > 0) {
            return initials.toString();
        }

        return name.substring(0, Math.min(maxChars, name.length())).toUpperCase(Locale.ROOT);
    }

    private static String fitLabel(Minecraft minecraft, String value, int maxWidth) {
        if (minecraft.font.width(value) <= maxWidth) {
            return value;
        }

        String ellipsis = "...";
        String trimmed = value;
        while (!trimmed.isEmpty() && minecraft.font.width(trimmed + ellipsis) > maxWidth) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed.isEmpty() ? ellipsis : trimmed + ellipsis;
    }

    private static String formatExactAmount(double amount) {
        int whole = (int) Math.round(amount);
        if (Math.abs(amount - whole) < 0.001D) {
            return Integer.toString(whole);
        }
        return String.format(Locale.ROOT, "%.2f", amount);
    }

    private static int lightenColor(int color, int amount) {
        int alpha = color >>> 24;
        int red = Math.min(255, ((color >>> 16) & 0xFF) + amount);
        int green = Math.min(255, ((color >>> 8) & 0xFF) + amount);
        int blue = Math.min(255, (color & 0xFF) + amount);
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    private record ResourceHudEntry(
            AbilityResourceDefinition resource,
            AbilityResourceHudRegistration registration
    ) {}
}

