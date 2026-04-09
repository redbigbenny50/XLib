package com.whatxe.xlib.presentation;

import com.whatxe.xlib.XLib;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

public record CombatHudPresentation(
        ResourceLocation id,
        boolean showActiveAbilityName,
        boolean activeNameRequiresDetailedHud,
        boolean showSlotNumbers,
        boolean showCooldownText,
        boolean showBlockedIndicator,
        boolean showModeOverlay,
        boolean showComboOverlay,
        ResourceLabelMode resourceLabelMode,
        int activeNameColor,
        int slotLabelColor,
        int missingAbilityColor,
        int hiddenAbilityFillColor,
        int hiddenAbilityTextColor,
        int activeDurationColor,
        int blockedIndicatorColor,
        int modeOverlayColor,
        int comboOverlayColor,
        int resourceOuterFrameColor,
        int resourceInnerFrameColor,
        int resourceBackgroundColor,
        int resourceLabelColor,
        int resourceValueColor,
        int resourceShieldOutlineColor
) {
    public CombatHudPresentation {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(resourceLabelMode, "resourceLabelMode");
    }

    public static Builder builder(ResourceLocation id) {
        return new Builder(id);
    }

    public static CombatHudPresentation defaultPresentation() {
        return builder(ResourceLocation.fromNamespaceAndPath(XLib.MODID, "default_combat_hud")).build();
    }

    public static CombatHudPresentation minimalPresentation() {
        return builder(ResourceLocation.fromNamespaceAndPath(XLib.MODID, "minimal_combat_hud"))
                .showActiveAbilityName(false)
                .showSlotNumbers(false)
                .showComboOverlay(false)
                .resourceLabelMode(ResourceLabelMode.SHORT)
                .build();
    }

    public enum ResourceLabelMode {
        AUTO,
        SHORT,
        LONG
    }

    public static final class Builder {
        private final ResourceLocation id;
        private boolean showActiveAbilityName = true;
        private boolean activeNameRequiresDetailedHud = true;
        private boolean showSlotNumbers = true;
        private boolean showCooldownText = true;
        private boolean showBlockedIndicator = true;
        private boolean showModeOverlay = true;
        private boolean showComboOverlay = true;
        private ResourceLabelMode resourceLabelMode = ResourceLabelMode.AUTO;
        private int activeNameColor = 0xFFFFFF;
        private int slotLabelColor = 0xFFE3C26A;
        private int missingAbilityColor = 0xFFFF5555;
        private int hiddenAbilityFillColor = 0xAA1F1F1F;
        private int hiddenAbilityTextColor = 0xFFE38B8B;
        private int activeDurationColor = 0xCC4CAF50;
        private int blockedIndicatorColor = 0xFFD45353;
        private int modeOverlayColor = 0xCC66B3FF;
        private int comboOverlayColor = 0xFFF1C24B;
        private int resourceOuterFrameColor = 0xC0101010;
        private int resourceInnerFrameColor = 0x7A000000;
        private int resourceBackgroundColor = 0x66000000;
        private int resourceLabelColor = 0xFFF6E7BF;
        private int resourceValueColor = 0xFFFFFFFF;
        private int resourceShieldOutlineColor = 0xCCBFE7FF;

        private Builder(ResourceLocation id) {
            this.id = Objects.requireNonNull(id, "id");
        }

        public Builder showActiveAbilityName(boolean showActiveAbilityName) {
            this.showActiveAbilityName = showActiveAbilityName;
            return this;
        }

        public Builder activeNameRequiresDetailedHud(boolean activeNameRequiresDetailedHud) {
            this.activeNameRequiresDetailedHud = activeNameRequiresDetailedHud;
            return this;
        }

        public Builder showSlotNumbers(boolean showSlotNumbers) {
            this.showSlotNumbers = showSlotNumbers;
            return this;
        }

        public Builder showCooldownText(boolean showCooldownText) {
            this.showCooldownText = showCooldownText;
            return this;
        }

        public Builder showBlockedIndicator(boolean showBlockedIndicator) {
            this.showBlockedIndicator = showBlockedIndicator;
            return this;
        }

        public Builder showModeOverlay(boolean showModeOverlay) {
            this.showModeOverlay = showModeOverlay;
            return this;
        }

        public Builder showComboOverlay(boolean showComboOverlay) {
            this.showComboOverlay = showComboOverlay;
            return this;
        }

        public Builder resourceLabelMode(ResourceLabelMode resourceLabelMode) {
            this.resourceLabelMode = Objects.requireNonNull(resourceLabelMode, "resourceLabelMode");
            return this;
        }

        public Builder activeNameColor(int activeNameColor) {
            this.activeNameColor = activeNameColor;
            return this;
        }

        public Builder slotLabelColor(int slotLabelColor) {
            this.slotLabelColor = slotLabelColor;
            return this;
        }

        public Builder missingAbilityColor(int missingAbilityColor) {
            this.missingAbilityColor = missingAbilityColor;
            return this;
        }

        public Builder hiddenAbilityFillColor(int hiddenAbilityFillColor) {
            this.hiddenAbilityFillColor = hiddenAbilityFillColor;
            return this;
        }

        public Builder hiddenAbilityTextColor(int hiddenAbilityTextColor) {
            this.hiddenAbilityTextColor = hiddenAbilityTextColor;
            return this;
        }

        public Builder activeDurationColor(int activeDurationColor) {
            this.activeDurationColor = activeDurationColor;
            return this;
        }

        public Builder blockedIndicatorColor(int blockedIndicatorColor) {
            this.blockedIndicatorColor = blockedIndicatorColor;
            return this;
        }

        public Builder modeOverlayColor(int modeOverlayColor) {
            this.modeOverlayColor = modeOverlayColor;
            return this;
        }

        public Builder comboOverlayColor(int comboOverlayColor) {
            this.comboOverlayColor = comboOverlayColor;
            return this;
        }

        public Builder resourceOuterFrameColor(int resourceOuterFrameColor) {
            this.resourceOuterFrameColor = resourceOuterFrameColor;
            return this;
        }

        public Builder resourceInnerFrameColor(int resourceInnerFrameColor) {
            this.resourceInnerFrameColor = resourceInnerFrameColor;
            return this;
        }

        public Builder resourceBackgroundColor(int resourceBackgroundColor) {
            this.resourceBackgroundColor = resourceBackgroundColor;
            return this;
        }

        public Builder resourceLabelColor(int resourceLabelColor) {
            this.resourceLabelColor = resourceLabelColor;
            return this;
        }

        public Builder resourceValueColor(int resourceValueColor) {
            this.resourceValueColor = resourceValueColor;
            return this;
        }

        public Builder resourceShieldOutlineColor(int resourceShieldOutlineColor) {
            this.resourceShieldOutlineColor = resourceShieldOutlineColor;
            return this;
        }

        public CombatHudPresentation build() {
            return new CombatHudPresentation(
                    this.id,
                    this.showActiveAbilityName,
                    this.activeNameRequiresDetailedHud,
                    this.showSlotNumbers,
                    this.showCooldownText,
                    this.showBlockedIndicator,
                    this.showModeOverlay,
                    this.showComboOverlay,
                    this.resourceLabelMode,
                    this.activeNameColor,
                    this.slotLabelColor,
                    this.missingAbilityColor,
                    this.hiddenAbilityFillColor,
                    this.hiddenAbilityTextColor,
                    this.activeDurationColor,
                    this.blockedIndicatorColor,
                    this.modeOverlayColor,
                    this.comboOverlayColor,
                    this.resourceOuterFrameColor,
                    this.resourceInnerFrameColor,
                    this.resourceBackgroundColor,
                    this.resourceLabelColor,
                    this.resourceValueColor,
                    this.resourceShieldOutlineColor
            );
        }
    }
}
