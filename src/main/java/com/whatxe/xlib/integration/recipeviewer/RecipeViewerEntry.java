package com.whatxe.xlib.integration.recipeviewer;

import com.whatxe.xlib.ability.RecipePermissionApi;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public record RecipeViewerEntry(
        ResourceLocation recipeId,
        RecipePermissionApi.RecipeAccessState accessState,
        boolean hiddenWhenLocked,
        Set<ResourceLocation> unlockSources,
        @Nullable Component unlockHint
) {}
