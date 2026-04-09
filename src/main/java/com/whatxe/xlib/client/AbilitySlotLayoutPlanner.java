package com.whatxe.xlib.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class AbilitySlotLayoutPlanner {
    private AbilitySlotLayoutPlanner() {}

    public static LayoutPlan plan(
            AbilityContainerLayoutDefinition layout,
            int slotCount,
            int slotWidth,
            int slotHeight,
            int slotSpacing
    ) {
        return switch (layout.layoutMode()) {
            case GRID -> planGrid(layout, slotCount, slotWidth, slotHeight, slotSpacing);
            case RADIAL -> planRadial(layout, slotCount, slotWidth, slotHeight);
            case CATEGORIZED -> planCategorized(layout, slotCount, slotWidth, slotHeight, slotSpacing);
            case STRIP -> planStrip(slotCount, slotWidth, slotHeight, slotSpacing);
        };
    }

    public static List<PageTabPlacement> planPageTabs(
            AbilityContainerLayoutDefinition layout,
            LayoutPlan plan,
            int pageCount,
            int tabWidth,
            int tabHeight,
            int spacing
    ) {
        if (!layout.showPageTabs() || pageCount <= 1) {
            return List.of();
        }

        List<PageTabPlacement> placements = new ArrayList<>();
        int totalWidth = pageCount * tabWidth + Math.max(0, pageCount - 1) * spacing;
        int startX = Math.max(0, (plan.width() - totalWidth) / 2);
        int y = layout.layoutMode() == AbilitySlotLayoutMode.RADIAL ? plan.height() + spacing : -tabHeight - spacing;
        for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
            placements.add(new PageTabPlacement(pageIndex, startX + pageIndex * (tabWidth + spacing), y));
        }
        return List.copyOf(placements);
    }

    private static LayoutPlan planStrip(int slotCount, int slotWidth, int slotHeight, int slotSpacing) {
        List<SlotPlacement> placements = new ArrayList<>();
        for (int slotIndex = 0; slotIndex < slotCount; slotIndex++) {
            placements.add(new SlotPlacement(
                    slotIndex,
                    slotIndex * (slotWidth + slotSpacing),
                    0,
                    null
            ));
        }
        int width = slotCount == 0 ? slotWidth : slotCount * slotWidth + Math.max(0, slotCount - 1) * slotSpacing;
        return new LayoutPlan(width, slotHeight, List.copyOf(placements), List.of());
    }

    private static LayoutPlan planGrid(
            AbilityContainerLayoutDefinition layout,
            int slotCount,
            int slotWidth,
            int slotHeight,
            int slotSpacing
    ) {
        int columns = Math.max(1, Math.min(layout.columns(), Math.max(1, slotCount)));
        int rows = Math.max(1, (int) Math.ceil(slotCount / (double) columns));
        List<SlotPlacement> placements = new ArrayList<>();
        for (int slotIndex = 0; slotIndex < slotCount; slotIndex++) {
            int column = slotIndex % columns;
            int row = slotIndex / columns;
            placements.add(new SlotPlacement(
                    slotIndex,
                    column * (slotWidth + slotSpacing),
                    row * (slotHeight + slotSpacing),
                    null
            ));
        }
        int width = columns * slotWidth + Math.max(0, columns - 1) * slotSpacing;
        int height = rows * slotHeight + Math.max(0, rows - 1) * slotSpacing;
        return new LayoutPlan(width, height, List.copyOf(placements), List.of());
    }

    private static LayoutPlan planRadial(
            AbilityContainerLayoutDefinition layout,
            int slotCount,
            int slotWidth,
            int slotHeight
    ) {
        int radius = Math.max(layout.radialRadius(), Math.max(slotWidth, slotHeight));
        int centerX = radius + slotWidth / 2;
        int centerY = radius + slotHeight / 2;
        List<SlotPlacement> placements = new ArrayList<>();
        if (slotCount == 1) {
            placements.add(new SlotPlacement(0, radius, radius, null));
        } else {
            for (int slotIndex = 0; slotIndex < slotCount; slotIndex++) {
                double angle = -Math.PI / 2.0D + (Math.PI * 2.0D * slotIndex) / slotCount;
                int x = (int) Math.round(centerX + Math.cos(angle) * radius - slotWidth / 2.0D);
                int y = (int) Math.round(centerY + Math.sin(angle) * radius - slotHeight / 2.0D);
                placements.add(new SlotPlacement(slotIndex, x, y, null));
            }
        }
        int size = radius * 2 + Math.max(slotWidth, slotHeight);
        return new LayoutPlan(size, size, List.copyOf(placements), List.of());
    }

    private static LayoutPlan planCategorized(
            AbilityContainerLayoutDefinition layout,
            int slotCount,
            int slotWidth,
            int slotHeight,
            int slotSpacing
    ) {
        List<SlotPlacement> placements = new ArrayList<>();
        List<CategoryPlacement> categories = new ArrayList<>();
        int x = 0;
        @Nullable String previousCategoryKey = null;
        for (int slotIndex = 0; slotIndex < slotCount; slotIndex++) {
            AbilitySlotWidgetMetadata metadata = layout.slotMetadata(slotIndex);
            @Nullable Component categoryLabel = metadata.categoryLabel();
            String categoryKey = categoryLabel == null ? "" : categoryLabel.getString().toLowerCase(Locale.ROOT);
            if (slotIndex > 0 && !categoryKey.equals(previousCategoryKey)) {
                x += slotSpacing * 2;
            }
            if (categoryLabel != null && !categoryKey.equals(previousCategoryKey)) {
                categories.add(new CategoryPlacement(x, -10, categoryLabel));
            }
            placements.add(new SlotPlacement(slotIndex, x, 0, categoryLabel));
            x += slotWidth + slotSpacing;
            previousCategoryKey = categoryKey;
        }
        int width = slotCount == 0 ? slotWidth : Math.max(slotWidth, x - slotSpacing);
        return new LayoutPlan(width, slotHeight, List.copyOf(placements), List.copyOf(categories));
    }

    public record LayoutPlan(
            int width,
            int height,
            List<SlotPlacement> slots,
            List<CategoryPlacement> categories
    ) {}

    public record SlotPlacement(
            int slotIndex,
            int x,
            int y,
            @Nullable Component categoryLabel
    ) {}

    public record CategoryPlacement(
            int x,
            int y,
            Component label
    ) {}

    public record PageTabPlacement(
            int pageIndex,
            int x,
            int y
    ) {}
}
