package com.whatxe.xlib.presentation;

import com.whatxe.xlib.XLib;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

public record AbilityMenuPresentation(
        ResourceLocation id,
        MenuPalette palette,
        EntryDetail entryDetail,
        boolean showMetadataDetails,
        boolean showRequirementBreakdown
) {
    public AbilityMenuPresentation {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(palette, "palette");
        Objects.requireNonNull(entryDetail, "entryDetail");
    }

    public static Builder builder(ResourceLocation id) {
        return new Builder(id);
    }

    public static AbilityMenuPresentation defaultPresentation() {
        return builder(ResourceLocation.fromNamespaceAndPath(XLib.MODID, "default_ability_menu")).build();
    }

    public static AbilityMenuPresentation catalogDensePresentation() {
        return builder(ResourceLocation.fromNamespaceAndPath(XLib.MODID, "catalog_dense_ability_menu"))
                .palette(MenuPalette.slatePalette())
                .entryDetail(EntryDetail.GROUP)
                .showRequirementBreakdown(false)
                .build();
    }

    public enum EntryDetail {
        NONE,
        FAMILY,
        GROUP,
        PAGE
    }

    public static final class Builder {
        private final ResourceLocation id;
        private MenuPalette palette = MenuPalette.defaultPalette();
        private EntryDetail entryDetail = EntryDetail.NONE;
        private boolean showMetadataDetails = false;
        private boolean showRequirementBreakdown = true;

        private Builder(ResourceLocation id) {
            this.id = Objects.requireNonNull(id, "id");
        }

        public Builder palette(MenuPalette palette) {
            this.palette = Objects.requireNonNull(palette, "palette");
            return this;
        }

        public Builder entryDetail(EntryDetail entryDetail) {
            this.entryDetail = Objects.requireNonNull(entryDetail, "entryDetail");
            return this;
        }

        public Builder showMetadataDetails(boolean showMetadataDetails) {
            this.showMetadataDetails = showMetadataDetails;
            return this;
        }

        public Builder showRequirementBreakdown(boolean showRequirementBreakdown) {
            this.showRequirementBreakdown = showRequirementBreakdown;
            return this;
        }

        public AbilityMenuPresentation build() {
            return new AbilityMenuPresentation(
                    this.id,
                    this.palette,
                    this.entryDetail,
                    this.showMetadataDetails,
                    this.showRequirementBreakdown
            );
        }
    }
}
