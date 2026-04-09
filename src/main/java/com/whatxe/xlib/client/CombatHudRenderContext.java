package com.whatxe.xlib.client;

import com.whatxe.xlib.ability.AbilityData;
import com.whatxe.xlib.ability.AbilitySlotReference;
import com.whatxe.xlib.presentation.CombatHudPresentation;
import java.util.Objects;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;

public record CombatHudRenderContext(
        GuiGraphics guiGraphics,
        DeltaTracker deltaTracker,
        Minecraft minecraft,
        AbilityData data,
        CombatHudPresentation presentation,
        @Nullable AbilitySlotReference highlightedSlot,
        boolean detailedHud
) {
    public CombatHudRenderContext {
        Objects.requireNonNull(guiGraphics, "guiGraphics");
        Objects.requireNonNull(deltaTracker, "deltaTracker");
        Objects.requireNonNull(minecraft, "minecraft");
        Objects.requireNonNull(data, "data");
        Objects.requireNonNull(presentation, "presentation");
    }
}
