package com.whatxe.xlib.integration.recipeviewer;

import com.whatxe.xlib.ability.RecipePermissionApi;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

public final class RecipeViewerClientRuntime {
    private static @Nullable IJeiRuntime jeiRuntime;
    private static Set<ResourceLocation> syncedJeiHiddenRecipes = Set.of();

    private RecipeViewerClientRuntime() {}

    public static void setJeiRuntime(@Nullable IJeiRuntime runtime) {
        jeiRuntime = runtime;
        syncedJeiHiddenRecipes = Set.of();
    }

    public static void syncClient(Minecraft minecraft) {
        syncJeiHiddenRecipes(minecraft);
    }

    private static void syncJeiHiddenRecipes(Minecraft minecraft) {
        if (jeiRuntime == null || minecraft.player == null || minecraft.level == null) {
            return;
        }

        Set<ResourceLocation> hiddenRecipes = JeiEmiRecipeViewerAddon.hiddenRecipes(minecraft.player);
        if (hiddenRecipes.equals(syncedJeiHiddenRecipes)) {
            return;
        }

        Set<ResourceLocation> toHide = new LinkedHashSet<>(hiddenRecipes);
        toHide.removeAll(syncedJeiHiddenRecipes);

        Set<ResourceLocation> toUnhide = new LinkedHashSet<>(syncedJeiHiddenRecipes);
        toUnhide.removeAll(hiddenRecipes);

        applyJeiVisibility(jeiRuntime.getRecipeManager(), minecraft, toHide, true);
        applyJeiVisibility(jeiRuntime.getRecipeManager(), minecraft, toUnhide, false);
        syncedJeiHiddenRecipes = Set.copyOf(hiddenRecipes);
    }

    private static void applyJeiVisibility(
            IRecipeManager recipeManager,
            Minecraft minecraft,
            Set<ResourceLocation> recipeIds,
            boolean hide
    ) {
        for (ResourceLocation recipeId : recipeIds) {
            RecipeHolder<?> recipe = minecraft.level.getRecipeManager().byKey(recipeId).orElse(null);
            if (recipe == null) {
                continue;
            }

            Optional<RecipeType<?>> recipeType = recipeManager.getRecipeType(recipeId);
            if (recipeType.isEmpty()) {
                continue;
            }

            applyJeiVisibility(recipeManager, recipeType.get(), recipe.value(), hide);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void applyJeiVisibility(IRecipeManager recipeManager, RecipeType recipeType, Object recipe, boolean hide) {
        if (hide) {
            recipeManager.hideRecipes(recipeType, List.of(recipe));
        } else {
            recipeManager.unhideRecipes(recipeType, List.of(recipe));
        }
    }
}
