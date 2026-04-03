package com.whatxe.xlib.integration.recipeviewer;

import com.whatxe.xlib.XLib;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IAdvancedRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

@JeiPlugin
public final class XLibJeiPlugin implements IModPlugin {
    private static final ResourceLocation PLUGIN_UID =
            ResourceLocation.fromNamespaceAndPath(XLib.MODID, "recipe_viewer/jei");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_UID;
    }

    @Override
    public void registerAdvanced(IAdvancedRegistration registration) {
        registerDecorator(registration, RecipeTypes.CRAFTING);
        registerDecorator(registration, RecipeTypes.STONECUTTING);
        registerDecorator(registration, RecipeTypes.SMELTING);
        registerDecorator(registration, RecipeTypes.SMOKING);
        registerDecorator(registration, RecipeTypes.BLASTING);
        registerDecorator(registration, RecipeTypes.CAMPFIRE_COOKING);
        registerDecorator(registration, RecipeTypes.SMITHING);
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        RecipeViewerClientRuntime.setJeiRuntime(jeiRuntime);
    }

    @Override
    public void onRuntimeUnavailable() {
        RecipeViewerClientRuntime.setJeiRuntime(null);
    }

    private static <R extends Recipe<?>> void registerDecorator(
            IAdvancedRegistration registration,
            RecipeType<RecipeHolder<R>> recipeType
    ) {
        registration.addRecipeCategoryDecorator(recipeType, new XLibJeiRecipeDecorator<>());
    }
}
