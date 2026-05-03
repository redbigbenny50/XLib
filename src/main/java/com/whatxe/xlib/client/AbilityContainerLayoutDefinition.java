package com.whatxe.xlib.client;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

public final class AbilityContainerLayoutDefinition {
    private final ResourceLocation containerId;
    private final AbilitySlotLayoutMode layoutMode;
    private final AbilitySlotLayoutAnchor anchor;
    private final int columns;
    private final int radialRadius;
    private final boolean showPageTabs;
    private final Map<Integer, AbilitySlotWidgetMetadata> slotMetadata;

    private AbilityContainerLayoutDefinition(
            ResourceLocation containerId,
            AbilitySlotLayoutMode layoutMode,
            AbilitySlotLayoutAnchor anchor,
            int columns,
            int radialRadius,
            boolean showPageTabs,
            Map<Integer, AbilitySlotWidgetMetadata> slotMetadata
    ) {
        this.containerId = containerId;
        this.layoutMode = layoutMode;
        this.anchor = anchor;
        this.columns = columns;
        this.radialRadius = radialRadius;
        this.showPageTabs = showPageTabs;
        this.slotMetadata = Map.copyOf(slotMetadata);
    }

    public static Builder builder(ResourceLocation containerId) {
        return new Builder(containerId);
    }

    public ResourceLocation containerId() {
        return this.containerId;
    }

    public AbilitySlotLayoutMode layoutMode() {
        return this.layoutMode;
    }

    public AbilitySlotLayoutAnchor anchor() {
        return this.anchor;
    }

    public int columns() {
        return this.columns;
    }

    public int radialRadius() {
        return this.radialRadius;
    }

    public boolean showPageTabs() {
        return this.showPageTabs;
    }

    public Map<Integer, AbilitySlotWidgetMetadata> slotMetadata() {
        return this.slotMetadata;
    }

    public AbilitySlotWidgetMetadata slotMetadata(int slotIndex) {
        AbilitySlotWidgetMetadata metadata = this.slotMetadata.get(slotIndex);
        return metadata != null ? metadata : AbilitySlotWidgetMetadata.defaultMetadata();
    }

    public static final class Builder {
        private final ResourceLocation containerId;
        private AbilitySlotLayoutMode layoutMode = AbilitySlotLayoutMode.STRIP;
        private AbilitySlotLayoutAnchor anchor = AbilitySlotLayoutAnchor.BOTTOM_CENTER;
        private int columns = 4;
        private int radialRadius = 34;
        private boolean showPageTabs = true;
        private final Map<Integer, AbilitySlotWidgetMetadata> slotMetadata = new LinkedHashMap<>();

        private Builder(ResourceLocation containerId) {
            this.containerId = Objects.requireNonNull(containerId, "containerId");
        }

        public Builder layoutMode(AbilitySlotLayoutMode layoutMode) {
            this.layoutMode = Objects.requireNonNull(layoutMode, "layoutMode");
            return this;
        }

        public Builder anchor(AbilitySlotLayoutAnchor anchor) {
            this.anchor = Objects.requireNonNull(anchor, "anchor");
            return this;
        }

        public Builder columns(int columns) {
            if (columns <= 0) {
                throw new IllegalArgumentException("columns must be positive");
            }
            this.columns = columns;
            return this;
        }

        public Builder radialRadius(int radialRadius) {
            if (radialRadius <= 0) {
                throw new IllegalArgumentException("radialRadius must be positive");
            }
            this.radialRadius = radialRadius;
            return this;
        }

        public Builder showPageTabs(boolean showPageTabs) {
            this.showPageTabs = showPageTabs;
            return this;
        }

        public Builder slotMetadata(int slotIndex, AbilitySlotWidgetMetadata metadata) {
            if (slotIndex < 0) {
                throw new IllegalArgumentException("slotIndex cannot be negative");
            }
            this.slotMetadata.put(slotIndex, Objects.requireNonNull(metadata, "metadata"));
            return this;
        }

        public AbilityContainerLayoutDefinition build() {
            return new AbilityContainerLayoutDefinition(
                    this.containerId,
                    this.layoutMode,
                    this.anchor,
                    this.columns,
                    this.radialRadius,
                    this.showPageTabs,
                    this.slotMetadata
            );
        }
    }
}
