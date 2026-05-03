package com.whatxe.xlib.presentation;

import net.minecraft.network.chat.Component;

public enum ProgressionNodeLayoutMode {
    LIST("screen.xlib.progression_menu.layout.list"),
    TREE("screen.xlib.progression_menu.layout.tree");

    private final String translationKey;

    ProgressionNodeLayoutMode(String translationKey) {
        this.translationKey = translationKey;
    }

    public Component label() {
        return Component.translatable(this.translationKey);
    }
}
