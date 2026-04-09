package com.whatxe.xlib.presentation;

public record MenuPalette(
        int backgroundTopColor,
        int backgroundBottomColor,
        int panelColor,
        int secondaryPanelColor,
        int highlightColor,
        int subduedColor,
        int titleColor,
        int bodyColor,
        int warningColor,
        int emphasisColor,
        int successColor,
        int infoColor,
        int familyColor,
        int groupColor,
        int pageColor,
        int tagColor,
        int requirementColor,
        int rewardColor,
        int scrollbarTrackColor,
        int scrollbarThumbColor
) {
    public static MenuPalette defaultPalette() {
        return new MenuPalette(
                0xD0101010,
                0xE0101010,
                0x33202020,
                0x33202020,
                0xAA2E5E3F,
                0x66303030,
                0xFFFFFF,
                0xD8D8D8,
                0xFFB4A3,
                0xF2E5A3,
                0xA9E6B0,
                0x8FD7FF,
                0xA5DDF7,
                0xB8F3B2,
                0xF2D29C,
                0xDEC2FF,
                0xD8B6FF,
                0x8FD7FF,
                0x66303030,
                0xFF8BCF9A
        );
    }

    public static MenuPalette slatePalette() {
        return new MenuPalette(
                0xD0141820,
                0xE018202B,
                0x3A16202A,
                0x33212B36,
                0xAA355A84,
                0x66404D5E,
                0xF6F7FB,
                0xD4DCEA,
                0xFFB6B0,
                0xF1D7A1,
                0x9ED7BD,
                0x99C9FF,
                0x9FD6FF,
                0xA9E1B2,
                0xE7C28C,
                0xD6B7FF,
                0xD5B4FF,
                0x93D3FF,
                0x66506074,
                0xFF81BED9
        );
    }
}
