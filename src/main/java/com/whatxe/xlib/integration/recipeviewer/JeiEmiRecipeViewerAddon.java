package com.whatxe.xlib.integration.recipeviewer;

import com.whatxe.xlib.ability.RecipePermissionApi;
import java.util.Collection;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public final class JeiEmiRecipeViewerAddon {
    private JeiEmiRecipeViewerAddon() {}

    public static List<RecipeViewerEntry> describeRestrictedRecipes(Player player) {
        return RecipePermissionApi.restrictedRecipes(player).stream()
                .sorted()
                .map(recipeId -> describe(player, recipeId))
                .toList();
    }

    public static RecipeViewerEntry describe(Player player, ResourceLocation recipeId) {
        return new RecipeViewerEntry(
                recipeId,
                RecipePermissionApi.accessState(player, recipeId),
                RecipePermissionApi.shouldHideLockedRecipe(player, recipeId),
                RecipePermissionApi.unlockSources(player, recipeId),
                RecipePermissionApi.unlockHint(player, recipeId).orElse(null)
        );
    }

    public static Set<ResourceLocation> hiddenRecipes(Player player) {
        LinkedHashSet<ResourceLocation> hiddenRecipes = new LinkedHashSet<>();
        for (ResourceLocation recipeId : RecipePermissionApi.restrictedRecipes(player)) {
            if (RecipePermissionApi.shouldHideLockedRecipe(player, recipeId)) {
                hiddenRecipes.add(recipeId);
            }
        }
        return Set.copyOf(hiddenRecipes);
    }

    public static Set<ResourceLocation> visibleRestrictedRecipes(Player player) {
        LinkedHashSet<ResourceLocation> visibleRecipes = new LinkedHashSet<>();
        for (ResourceLocation recipeId : RecipePermissionApi.restrictedRecipes(player)) {
            if (!RecipePermissionApi.shouldHideLockedRecipe(player, recipeId)) {
                visibleRecipes.add(recipeId);
            }
        }
        return Set.copyOf(visibleRecipes);
    }

    public static Optional<Component> unlockHint(Player player, ResourceLocation recipeId) {
        if (RecipePermissionApi.accessState(player, recipeId) == RecipePermissionApi.RecipeAccessState.ALLOWED) {
            return Optional.empty();
        }
        return RecipePermissionApi.unlockHint(player, recipeId);
    }

    public static Collection<ResourceLocation> unlockSources(Player player, ResourceLocation recipeId) {
        return RecipePermissionApi.unlockSources(player, recipeId);
    }

    public static List<Component> lockedTooltip(Player player, ResourceLocation recipeId) {
        RecipeViewerEntry entry = describe(player, recipeId);
        if (entry.accessState() != RecipePermissionApi.RecipeAccessState.LOCKED) {
            return List.of();
        }

        List<Component> tooltip = new java.util.ArrayList<>();
        tooltip.add(Component.translatable("integration.xlib.recipe_locked"));
        tooltip.add(Component.translatable("integration.xlib.recipe_locked_reason"));
        if (entry.unlockHint() != null) {
            tooltip.add(entry.unlockHint());
        }
        if (!entry.unlockSources().isEmpty()) {
            tooltip.add(Component.translatable("integration.xlib.recipe_unlock_sources", joinIds(entry.unlockSources())));
        }
        return List.copyOf(tooltip);
    }

    public static String joinIds(Collection<ResourceLocation> ids) {
        StringBuilder builder = new StringBuilder();
        boolean wrote = false;
        for (ResourceLocation id : ids) {
            if (wrote) {
                builder.append(", ");
            }
            builder.append(id);
            wrote = true;
        }
        return wrote ? builder.toString() : "-";
    }
}
