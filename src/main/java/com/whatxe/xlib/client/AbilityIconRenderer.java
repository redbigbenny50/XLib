package com.whatxe.xlib.client;

import com.whatxe.xlib.ability.AbilityIcon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public final class AbilityIconRenderer {
    private AbilityIconRenderer() {}

    public static void render(GuiGraphics guiGraphics, Minecraft minecraft, AbilityIcon icon, int x, int y, int width, int height) {
        switch (icon.kind()) {
            case ITEM -> {
                if (icon.item() != null) {
                    guiGraphics.renderItem(icon.item().getDefaultInstance(), x, y);
                }
            }
            case TEXTURE -> {
                if (icon.texture() != null) {
                    guiGraphics.blit(icon.texture(), x, y, 0, 0, width, height, width, height);
                }
            }
            case CUSTOM -> {
                if (icon.customRendererId() != null) {
                    AbilityCustomIconRegistry.find(icon.customRendererId())
                            .ifPresent(renderer -> renderer.render(guiGraphics, minecraft, icon, x, y, width, height));
                }
            }
        }
    }
}

