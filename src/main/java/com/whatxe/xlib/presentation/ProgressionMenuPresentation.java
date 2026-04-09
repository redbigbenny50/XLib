package com.whatxe.xlib.presentation;

import com.whatxe.xlib.XLib;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

public record ProgressionMenuPresentation(
        ResourceLocation id,
        MenuPalette palette,
        NodeLabelMode nodeLabelMode,
        boolean showTrackMetadata,
        boolean showPointSourceHints,
        ProgressionNodeLayoutMode defaultLayoutMode,
        Set<ProgressionNodeLayoutMode> availableLayoutModes,
        ProgressionTreeLayout treeLayout
) {
    public ProgressionMenuPresentation {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(palette, "palette");
        Objects.requireNonNull(nodeLabelMode, "nodeLabelMode");
        Objects.requireNonNull(defaultLayoutMode, "defaultLayoutMode");
        Objects.requireNonNull(availableLayoutModes, "availableLayoutModes");
        Objects.requireNonNull(treeLayout, "treeLayout");
        if (!availableLayoutModes.contains(defaultLayoutMode)) {
            throw new IllegalStateException("defaultLayoutMode must be included in availableLayoutModes");
        }
        availableLayoutModes = Set.copyOf(new LinkedHashSet<>(availableLayoutModes));
    }

    public static Builder builder(ResourceLocation id) {
        return new Builder(id);
    }

    public static ProgressionMenuPresentation defaultPresentation() {
        return builder(ResourceLocation.fromNamespaceAndPath(XLib.MODID, "default_progression_menu")).build();
    }

    public enum NodeLabelMode {
        NAME_ONLY,
        NAME_WITH_STATUS,
        NAME_WITH_COST
    }

    public static final class Builder {
        private final ResourceLocation id;
        private MenuPalette palette = MenuPalette.defaultPalette();
        private NodeLabelMode nodeLabelMode = NodeLabelMode.NAME_WITH_STATUS;
        private boolean showTrackMetadata = false;
        private boolean showPointSourceHints = true;
        private ProgressionNodeLayoutMode defaultLayoutMode = ProgressionNodeLayoutMode.TREE;
        private Set<ProgressionNodeLayoutMode> availableLayoutModes = Set.of(
                ProgressionNodeLayoutMode.LIST,
                ProgressionNodeLayoutMode.TREE
        );
        private ProgressionTreeLayout treeLayout = ProgressionTreeLayout.defaultLayout();

        private Builder(ResourceLocation id) {
            this.id = Objects.requireNonNull(id, "id");
        }

        public Builder palette(MenuPalette palette) {
            this.palette = Objects.requireNonNull(palette, "palette");
            return this;
        }

        public Builder nodeLabelMode(NodeLabelMode nodeLabelMode) {
            this.nodeLabelMode = Objects.requireNonNull(nodeLabelMode, "nodeLabelMode");
            return this;
        }

        public Builder showTrackMetadata(boolean showTrackMetadata) {
            this.showTrackMetadata = showTrackMetadata;
            return this;
        }

        public Builder showPointSourceHints(boolean showPointSourceHints) {
            this.showPointSourceHints = showPointSourceHints;
            return this;
        }

        public Builder defaultLayoutMode(ProgressionNodeLayoutMode defaultLayoutMode) {
            this.defaultLayoutMode = Objects.requireNonNull(defaultLayoutMode, "defaultLayoutMode");
            return this;
        }

        public Builder availableLayoutModes(Set<ProgressionNodeLayoutMode> availableLayoutModes) {
            this.availableLayoutModes = Set.copyOf(Objects.requireNonNull(availableLayoutModes, "availableLayoutModes"));
            return this;
        }

        public Builder treeLayout(ProgressionTreeLayout treeLayout) {
            this.treeLayout = Objects.requireNonNull(treeLayout, "treeLayout");
            return this;
        }

        public ProgressionMenuPresentation build() {
            return new ProgressionMenuPresentation(
                    this.id,
                    this.palette,
                    this.nodeLabelMode,
                    this.showTrackMetadata,
                    this.showPointSourceHints,
                    this.defaultLayoutMode,
                    this.availableLayoutModes,
                    this.treeLayout
            );
        }
    }
}
