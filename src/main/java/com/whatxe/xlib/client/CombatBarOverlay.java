package com.whatxe.xlib.client;

import com.whatxe.xlib.XLib;
import com.whatxe.xlib.ability.AbilityApi;
import com.whatxe.xlib.ability.AbilityData;
import com.whatxe.xlib.ability.AbilityDefinition;
import com.whatxe.xlib.ability.AbilityGrantApi;
import com.whatxe.xlib.ability.AbilityLoadoutApi;
import com.whatxe.xlib.ability.AbilityResourceDefinition;
import com.whatxe.xlib.ability.AbilityRuntime;
import com.whatxe.xlib.ability.AbilitySlotContainerApi;
import com.whatxe.xlib.ability.AbilitySlotContainerDefinition;
import com.whatxe.xlib.ability.AbilitySlotReference;
import com.whatxe.xlib.attachment.ModAttachments;
import com.whatxe.xlib.presentation.CombatHudPresentation;
import com.whatxe.xlib.presentation.CombatHudPresentationApi;
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
    private static final int HUD_TOP_MARGIN = 26;
    private static final int HUD_BOTTOM_MARGIN = 2;
    private static final int HUD_STACK_GAP = 12;
    private static final ResourceLocation LAYER_ID =
            ResourceLocation.fromNamespaceAndPath(XLib.MODID, "combat_bar");
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
        AbilitySlotReference highlightedSlot = AbilityClientState.highlightedSlot();
        boolean detailedHud = CombatBarPreferences.detailMode() == CombatBarPreferences.DetailMode.DETAILED;
        CombatHudPresentation presentation = currentHudPresentation();
        CombatHudRendererApi.renderActive(new CombatHudRenderContext(
                guiGraphics,
                deltaTracker,
                minecraft,
                data,
                presentation,
                highlightedSlot,
                detailedHud
        ));
    }

    static void renderBuiltIn(CombatHudRenderContext context) {
        GuiGraphics guiGraphics = context.guiGraphics();
        Minecraft minecraft = context.minecraft();
        AbilityData data = context.data();
        CombatHudPresentation presentation = context.presentation();
        AbilitySlotReference highlightedSlot = context.highlightedSlot();
        boolean detailedHud = context.detailedHud();
        int centerX = guiGraphics.guiWidth() / 2;
        List<AbilitySlotContainerDefinition> visibleContainers = new ArrayList<>(
                AbilitySlotContainerApi.visibleContainers(minecraft.player, data)
        );
        if (visibleContainers.isEmpty()) {
            return;
        }

        int bottomRowY = guiGraphics.guiHeight() - 22;
        renderResources(guiGraphics, data, centerX, bottomRowY);
        EnumMap<AbilitySlotLayoutAnchor, Integer> anchorStacks = new EnumMap<>(AbilitySlotLayoutAnchor.class);
        for (AbilitySlotLayoutAnchor anchor : AbilitySlotLayoutAnchor.values()) {
            anchorStacks.put(anchor, 0);
        }

        Component activeName = null;
        for (int containerIndex = visibleContainers.size() - 1; containerIndex >= 0; containerIndex--) {
            AbilitySlotContainerDefinition container = visibleContainers.get(containerIndex);
            int slotsPerPage = Math.max(1, AbilitySlotContainerApi.resolvedSlotsPerPage(data, container.id()));
            int activePage = data.activeContainerPage(container.id());
            int pageCount = Math.max(1, AbilitySlotContainerApi.resolvedPageCount(data, container.id()));
            AbilityContainerLayoutDefinition layout = AbilityContainerLayoutApi.resolvedLayout(container.id());
            AbilitySlotLayoutPlanner.LayoutPlan plan = AbilitySlotLayoutPlanner.plan(layout, slotsPerPage, 18, 18, 2);
            int stackIndex = anchorStacks.getOrDefault(layout.anchor(), 0);
            int rowX = hudOriginX(guiGraphics.guiWidth(), layout.anchor(), plan.width(), centerX);
            int rowY = hudOriginY(guiGraphics.guiHeight(), layout.anchor(), plan.height(), stackIndex);
            anchorStacks.put(layout.anchor(), stackIndex + 1);
            int frameColor = container.id().equals(AbilitySlotContainerApi.PRIMARY_CONTAINER_ID)
                    ? presentation.resourceOuterFrameColor()
                    : presentation.resourceInnerFrameColor();
            if (layout.layoutMode() != AbilitySlotLayoutMode.RADIAL) {
                guiGraphics.fill(rowX - 2, rowY - 2, rowX + plan.width() + 2, rowY + plan.height() + 2, frameColor);
                guiGraphics.fill(rowX - 1, rowY - 1, rowX + plan.width() + 1, rowY + plan.height() + 1, 0xAA000000);
            }
            for (AbilitySlotLayoutPlanner.CategoryPlacement category : plan.categories()) {
                guiGraphics.drawString(
                        thisOrMinecraftFont(minecraft),
                        category.label(),
                        rowX + category.x(),
                        rowY + category.y(),
                        presentation.resourceLabelColor(),
                        false
                );
            }

            for (AbilitySlotLayoutPlanner.SlotPlacement placement : plan.slots()) {
                AbilitySlotReference slotReference = new AbilitySlotReference(container.id(), activePage, placement.slotIndex());
                Optional<ResourceLocation> maybeAbilityId = AbilityLoadoutApi.resolvedAbilityId(data, slotReference);
                if (highlightedSlot != null && highlightedSlot.equals(slotReference) && maybeAbilityId.isPresent()) {
                    activeName = AbilityApi.findAbility(maybeAbilityId.get())
                            .filter(ability -> AbilityGrantApi.canView(minecraft.player, data, ability))
                            .map(AbilityDefinition::displayName)
                            .orElse(null);
                }
                renderSlot(
                        guiGraphics,
                        data,
                        slotReference,
                        rowX + placement.x(),
                        rowY + placement.y(),
                        presentation,
                        highlightedSlot != null && highlightedSlot.equals(slotReference),
                        layout.slotMetadata(placement.slotIndex())
                );
            }
        }

        boolean showActiveName = presentation.showActiveAbilityName()
                && (!presentation.activeNameRequiresDetailedHud() || detailedHud);
        if (activeName != null && showActiveName) {
            int labelX = centerX - minecraft.font.width(activeName) / 2;
            int activeStacks = anchorStacks.getOrDefault(AbilitySlotLayoutAnchor.BOTTOM_CENTER, 0);
            int labelY = Math.max(8, bottomRowY - activeStacks * 30 - 16);
            guiGraphics.drawString(minecraft.font, activeName, labelX, labelY, presentation.activeNameColor(), true);
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
                    renderer.render(
                            guiGraphics,
                            minecraft,
                            minecraft.player,
                            data,
                            entry.resource,
                            layout,
                            resourceX + layout.offsetX(),
                            resourceY + layout.offsetY()
                    );
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
                renderer.render(
                        guiGraphics,
                        minecraft,
                        minecraft.player,
                        data,
                        entry.resource,
                        layout,
                        resourceX + layout.offsetX(),
                        resourceY + layout.offsetY()
                );
                resourceY += layout.height() + layout.spacing();
            }
        }
        return aboveHotbarTopY;
    }

    static void renderDefaultResourceBar(
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

    private static void renderSlot(
            GuiGraphics guiGraphics,
            AbilityData data,
            AbilitySlotReference slotReference,
            int x,
            int y,
            CombatHudPresentation presentation,
            boolean highlighted,
            AbilitySlotWidgetMetadata metadata
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        String slotLabel = presentation.showSlotNumbers() ? slotLabel(minecraft, slotReference, metadata) : null;
        int iconX = x + 1;
        int iconY = y + 1;
        int iconSize = 14;
        guiGraphics.fill(
                x - 1,
                y - 1,
                x + 17,
                y + 17,
                highlighted ? CombatBarPreferences.mapHudColor(presentation.resourceShieldOutlineColor()) : 0x66000000
        );
        if (metadata.softLocked()) {
            guiGraphics.fill(x + 11, y + 1, x + 15, y + 5, 0xCCB8752A);
        }

        Optional<ResourceLocation> maybeAbilityId = AbilityLoadoutApi.resolvedAbilityId(data, slotReference);
        if (maybeAbilityId.isEmpty()) {
            if (metadata.shortLabel() != null && slotLabel == null) {
                String shortLabel = metadata.shortLabel().getString();
                guiGraphics.drawString(
                        minecraft.font,
                        shortLabel,
                        x + 8 - minecraft.font.width(shortLabel) / 2,
                        y + 4,
                        presentation.slotLabelColor(),
                        false
                );
            }
            renderSlotLabel(guiGraphics, minecraft, slotLabel, x, y, presentation);
            return;
        }

        Optional<AbilityDefinition> maybeAbility = AbilityApi.findAbility(maybeAbilityId.get());
        if (maybeAbility.isEmpty()) {
            guiGraphics.drawString(minecraft.font, "?", x + 6, y + 4, presentation.missingAbilityColor(), true);
            renderSlotLabel(guiGraphics, minecraft, slotLabel, x, y, presentation);
            return;
        }

        AbilityDefinition ability = maybeAbility.get();
        if (!AbilityGrantApi.canView(minecraft.player, data, ability)) {
            guiGraphics.fill(iconX, iconY, iconX + iconSize, iconY + iconSize, presentation.hiddenAbilityFillColor());
            guiGraphics.drawString(minecraft.font, "?", x + 5, y + 4, presentation.hiddenAbilityTextColor(), true);
            renderSlotLabel(guiGraphics, minecraft, slotLabel, x, y, presentation);
            return;
        }

        AbilityIconRenderer.render(guiGraphics, minecraft, ability.icon(), iconX, iconY, iconSize, iconSize);

        if (data.isModeActive(ability.id())) {
            guiGraphics.fill(iconX, iconY + 12, iconX + iconSize, iconY + iconSize, 0x66000000);
            int activeWidth = ability.durationTicks() > 0
                    ? Math.max(1, Math.round(iconSize * data.activeDurationFor(ability.id()) / (float) ability.durationTicks()))
                    : iconSize;
            guiGraphics.fill(
                    iconX,
                    iconY + 12,
                    iconX + activeWidth,
                    iconY + iconSize,
                    CombatBarPreferences.mapHudColor(presentation.activeDurationColor())
            );
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
            if (presentation.showCooldownText()) {
                guiGraphics.drawString(
                        minecraft.font,
                        cooldownLabel,
                        x + 8 - minecraft.font.width(cooldownLabel) / 2,
                        y + 4,
                        0xFFFFFFFF,
                        true
                );
            }
        }

        if (presentation.showBlockedIndicator() && data.isAbilityActivationBlocked(ability.id())) {
            guiGraphics.fill(iconX, iconY + 10, iconX + 4, iconY + iconSize, CombatBarPreferences.mapHudColor(presentation.blockedIndicatorColor()));
        }
        if (presentation.showModeOverlay() && AbilityLoadoutApi.hasModeOverlay(data, slotReference)) {
            guiGraphics.fill(iconX, iconY, iconX + iconSize, iconY + 2, CombatBarPreferences.mapHudColor(presentation.modeOverlayColor()));
        }
        if (presentation.showComboOverlay() && AbilityLoadoutApi.hasComboOverride(data, slotReference)) {
            guiGraphics.fill(iconX, iconY + 10, iconX + iconSize, iconY + 12, CombatBarPreferences.mapHudColor(presentation.comboOverlayColor()));
        }
        renderSlotLabel(guiGraphics, minecraft, slotLabel, x, y, presentation);
    }

    private static void renderSlotLabel(
            GuiGraphics guiGraphics,
            Minecraft minecraft,
            @org.jetbrains.annotations.Nullable String slotLabel,
            int x,
            int y,
            CombatHudPresentation presentation
    ) {
        if (slotLabel == null || slotLabel.isBlank()) {
            return;
        }
        guiGraphics.drawString(minecraft.font, slotLabel, x + 2, y + 1, presentation.slotLabelColor(), true);
    }

    private static String slotLabel(Minecraft minecraft, AbilitySlotReference slotReference, AbilitySlotWidgetMetadata metadata) {
        if (metadata.shortLabel() != null && !metadata.shortLabel().getString().isBlank()) {
            return metadata.shortLabel().getString();
        }
        return AbilityControlInputHandler.slotHint(minecraft, slotReference).orElse(Integer.toString(slotReference.slotIndex() + 1));
    }

    private static int hudOriginX(int screenWidth, AbilitySlotLayoutAnchor anchor, int width, int centerX) {
        return switch (anchor) {
            case TOP_LEFT, LEFT_MIDDLE, BOTTOM_LEFT -> 8;
            case TOP_RIGHT, RIGHT_MIDDLE, BOTTOM_RIGHT -> screenWidth - width - 8;
            case BOTTOM_CENTER -> centerX - width / 2;
        };
    }

    private static int hudOriginY(int screenHeight, AbilitySlotLayoutAnchor anchor, int height, int stackIndex) {
        return switch (anchor) {
            case TOP_LEFT, TOP_RIGHT -> HUD_TOP_MARGIN + stackIndex * (height + HUD_STACK_GAP);
            case LEFT_MIDDLE, RIGHT_MIDDLE -> Math.max(HUD_TOP_MARGIN, screenHeight / 2 - height / 2 + stackIndex * (height + HUD_STACK_GAP));
            case BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT -> screenHeight - height - HUD_BOTTOM_MARGIN - stackIndex * (height + HUD_STACK_GAP);
        };
    }

    private static net.minecraft.client.gui.Font thisOrMinecraftFont(Minecraft minecraft) {
        return minecraft.font;
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
        CombatHudPresentation presentation = currentHudPresentation();
        boolean longLabels = useLongResourceLabels(presentation, detailedHud);
        boolean showName = layout.showName();
        boolean showValue = layout.showValue();
        String label = showName
                ? (longLabels
                ? fitLabel(minecraft, resource.displayName().getString(), Math.max(20, layout.width() / 2))
                : compactResourceLabel(resource, 3))
                : "";
        String amountLabel = showValue
                ? (longLabels
                ? formatExactAmount(currentAmount) + "/" + formatExactAmount(totalCapacity)
                : compactAmount(currentAmount))
                : "";
        int labelWidth = showName
                ? Math.min(Math.max(20, minecraft.font.width(label) + 6), Math.max(24, layout.width() / 2))
                : 0;
        int valueWidth = showValue
                ? Math.min(Math.max(20, minecraft.font.width(amountLabel) + 6), Math.max(24, layout.width() / 2))
                : 0;
        int barLeft = x + (showName ? labelWidth + 2 : 2);
        int barRight = Math.max(barLeft + 8, outerRight - (showValue ? valueWidth + 2 : 2));
        int barTop = y + 2;
        int barBottom = outerBottom - 2;
        int usableWidth = Math.max(1, barRight - barLeft - 2);
        int fillWidth = (int) Math.round(usableWidth * (currentAmount / totalCapacity));
        int normalCapacityWidth = (int) Math.round(usableWidth * (resource.maxAmount() / (double) totalCapacity));
        int baseFillWidth = (int) Math.round(usableWidth * (Math.min(currentAmount, resource.maxAmount()) / totalCapacity));
        int overflowFillWidth = Math.max(0, fillWidth - normalCapacityWidth);
        int resourceColor = CombatBarPreferences.mapHudColor(resource.color());

        guiGraphics.fill(x, y, outerRight, outerBottom, presentation.resourceOuterFrameColor());
        guiGraphics.fill(x + 1, y + 1, outerRight - 1, outerBottom - 1, presentation.resourceInnerFrameColor());
        guiGraphics.fill(barLeft, barTop, barRight, barBottom, presentation.resourceBackgroundColor());
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
            guiGraphics.fill(x, y, outerRight, y + 1, presentation.resourceShieldOutlineColor());
            guiGraphics.fill(x, outerBottom - 1, outerRight, outerBottom, presentation.resourceShieldOutlineColor());
        }

        if (showName) {
            guiGraphics.drawString(minecraft.font, label, x + 3, y + 3, presentation.resourceLabelColor(), false);
        }
        if (showValue) {
            guiGraphics.drawString(
                    minecraft.font,
                    amountLabel,
                    outerRight - 3 - minecraft.font.width(amountLabel),
                    y + 3,
                    presentation.resourceValueColor(),
                    false
            );
        }
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
        boolean showName = layout.showName();
        boolean showValue = layout.showValue();
        int headerHeight = showValue ? 10 : 2;
        int footerHeight = showName ? Math.min(10, Math.max(8, layout.width() - 4)) : 2;
        int frameRight = x + layout.width();
        int frameBottom = y + layout.height();
        int gaugeTop = y + headerHeight;
        int gaugeBottom = frameBottom - footerHeight - 1;
        int usableHeight = Math.max(1, gaugeBottom - gaugeTop - 1);
        int fillHeight = (int) Math.round(usableHeight * (currentAmount / totalCapacity));
        int normalCapacityHeight = (int) Math.round(usableHeight * (resource.maxAmount() / (double) totalCapacity));
        int baseFillHeight = (int) Math.round(usableHeight * (Math.min(currentAmount, resource.maxAmount()) / totalCapacity));
        int overflowFillHeight = Math.max(0, fillHeight - normalCapacityHeight);
        int innerLeft = x + 2;
        int innerRight = frameRight - 2;
        boolean detailedHud = CombatBarPreferences.detailMode() == CombatBarPreferences.DetailMode.DETAILED;
        CombatHudPresentation presentation = currentHudPresentation();
        boolean longLabels = useLongResourceLabels(presentation, detailedHud);
        int resourceColor = CombatBarPreferences.mapHudColor(resource.color());

        guiGraphics.fill(x, y, frameRight, frameBottom, presentation.resourceOuterFrameColor());
        guiGraphics.fill(x + 1, y + 1, frameRight - 1, frameBottom - 1, presentation.resourceInnerFrameColor());
        guiGraphics.fill(innerLeft, gaugeTop, innerRight, gaugeBottom, presentation.resourceBackgroundColor());
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
        guiGraphics.fill(x + 1, frameBottom - footerHeight, frameRight - 1, frameBottom - 1, presentation.resourceInnerFrameColor());
        if (resource.shieldStyle()) {
            guiGraphics.fill(x, y, frameRight, y + 1, presentation.resourceShieldOutlineColor());
            guiGraphics.fill(x, frameBottom - 1, frameRight, frameBottom, presentation.resourceShieldOutlineColor());
        }

        if (showValue) {
            String amountLabel = longLabels
                    ? formatExactAmount(currentAmount) + "/" + formatExactAmount(totalCapacity)
                    : compactAmount(currentAmount);
            String fittedAmountLabel = fitLabel(minecraft, amountLabel, layout.width() - 2);
            guiGraphics.drawString(
                    minecraft.font,
                    fittedAmountLabel,
                    x + Math.max(1, (layout.width() - minecraft.font.width(fittedAmountLabel)) / 2),
                    y + 2,
                    presentation.resourceValueColor(),
                    false
            );
        }
        if (showName) {
            String shortLabel = longLabels
                    ? fitLabel(minecraft, resource.displayName().getString(), layout.width() - 2)
                    : compactResourceLabel(resource, 2);
            guiGraphics.drawString(
                    minecraft.font,
                    shortLabel,
                    x + Math.max(1, (layout.width() - minecraft.font.width(shortLabel)) / 2),
                    frameBottom - footerHeight + 1,
                    presentation.resourceLabelColor(),
                    false
            );
        }
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

    private static CombatHudPresentation currentHudPresentation() {
        return CombatHudPresentationApi.active();
    }

    private static boolean useLongResourceLabels(CombatHudPresentation presentation, boolean detailedHud) {
        return switch (presentation.resourceLabelMode()) {
            case AUTO -> detailedHud;
            case SHORT -> false;
            case LONG -> true;
        };
    }
}

