package com.whatxe.xlib.integration.recipeviewer;

import com.whatxe.xlib.ability.RecipePermissionApi;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.category.extensions.IRecipeCategoryDecorator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

public final class XLibJeiRecipeDecorator<R extends Recipe<?>> implements IRecipeCategoryDecorator<RecipeHolder<R>> {
    private static final int LOCK_COLOR = 0xE27C7C;
    private static final int LABEL_BOTTOM_OFFSET = 10;
    private static final int LABEL_PADDING = 4;

    @Override
    public void draw(
            RecipeHolder<R> recipe,
            IRecipeCategory<RecipeHolder<R>> category,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphics guiGraphics,
            double mouseX,
            double mouseY
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || RecipePermissionApi.accessState(minecraft.player, recipe.id()) != RecipePermissionApi.RecipeAccessState.LOCKED) {
            return;
        }

        Component lockedLabel = Component.translatable("integration.xlib.recipe_locked");
        Font font = minecraft.font;
        int x = category.getWidth() - font.width(lockedLabel) - LABEL_PADDING;
        int y = category.getHeight() - LABEL_BOTTOM_OFFSET;
        guiGraphics.drawString(font, lockedLabel, x, y, LOCK_COLOR, true);
    }

    @Override
    public void decorateTooltips(
            ITooltipBuilder tooltip,
            RecipeHolder<R> recipe,
            IRecipeCategory<RecipeHolder<R>> category,
            IRecipeSlotsView recipeSlotsView,
            double mouseX,
            double mouseY
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || RecipePermissionApi.accessState(minecraft.player, recipe.id()) != RecipePermissionApi.RecipeAccessState.LOCKED) {
            return;
        }
        if (!isOverLockLabel(minecraft.font, category, mouseX, mouseY)) {
            return;
        }

        tooltip.addAll(JeiEmiRecipeViewerAddon.lockedTooltip(minecraft.player, recipe.id()));
    }

    private static boolean isOverLockLabel(Font font, IRecipeCategory<?> category, double mouseX, double mouseY) {
        Component lockedLabel = Component.translatable("integration.xlib.recipe_locked");
        int width = font.width(lockedLabel);
        int x = category.getWidth() - width - LABEL_PADDING;
        int y = category.getHeight() - LABEL_BOTTOM_OFFSET;
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + font.lineHeight;
    }
}
