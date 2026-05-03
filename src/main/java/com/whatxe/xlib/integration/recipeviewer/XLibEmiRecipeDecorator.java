package com.whatxe.xlib.integration.recipeviewer;

import com.whatxe.xlib.ability.RecipePermissionApi;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeDecorator;
import dev.emi.emi.api.widget.TextWidget;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class XLibEmiRecipeDecorator implements EmiRecipeDecorator {
    @Override
    public void decorateRecipe(EmiRecipe recipe, WidgetHolder widgets) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        ResourceLocation recipeId = recipe.getId();
        if (recipeId == null && recipe.getBackingRecipe() != null) {
            recipeId = recipe.getBackingRecipe().id();
        }
        if (recipeId == null) {
            return;
        }

        if (RecipePermissionApi.accessState(minecraft.player, recipeId) != RecipePermissionApi.RecipeAccessState.LOCKED) {
            return;
        }

        widgets.addText(Component.translatable("integration.xlib.recipe_locked"), widgets.getWidth() - 4, widgets.getHeight() - 10, 0xE27C7C, true)
                .horizontalAlign(TextWidget.Alignment.END);

        widgets.addTooltipText(JeiEmiRecipeViewerAddon.lockedTooltip(minecraft.player, recipeId), 0, 0, widgets.getWidth(), widgets.getHeight());
    }
}
