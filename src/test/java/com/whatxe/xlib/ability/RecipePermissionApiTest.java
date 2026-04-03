package com.whatxe.xlib.ability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class RecipePermissionApiTest {
    private static final ResourceLocation RECIPE_ID = id("recipe");
    private static final ResourceLocation RECIPE_TAG_ID = id("recipe_tag");
    private static final ResourceLocation CATEGORY_ID = id("category");
    private static final ResourceLocation OUTPUT_ID = id("output");
    private static final ResourceLocation SOURCE_ID = id("source");
    private static final ResourceLocation ADVANCEMENT_ID = id("unlock_advancement");

    @Test
    void restrictedRecipeMetadataIsQueryable() throws Exception {
        RecipePermissionApi.unregisterRestrictedRecipe(RECIPE_ID);
        RecipePermissionApi.setDatapackRestrictionsForTesting(Map.of(), Map.of());

        RecipePermissionApi.registerRestrictedRecipe(RestrictedRecipeDefinition.builder(RECIPE_ID)
                .recipeTag(RECIPE_TAG_ID)
                .category(CATEGORY_ID)
                .output(OUTPUT_ID)
                .unlockSource(SOURCE_ID)
                .unlockAdvancement(ADVANCEMENT_ID)
                .outputTag(TagParser.parseTag("{CustomModelData:3}"))
                .unlockHint(Component.literal("Unlock by entering your mode"))
                .hiddenWhenLocked(false)
                .build());

        RestrictedRecipeDefinition definition = RecipePermissionApi.findRestrictedRecipe(RECIPE_ID).orElseThrow();
        assertTrue(RecipePermissionApi.recipesInCategory(CATEGORY_ID).contains(RECIPE_ID));
        assertTrue(RecipePermissionApi.recipesForOutput(OUTPUT_ID).contains(RECIPE_ID));
        assertTrue(RecipePermissionApi.unlockSources(RECIPE_ID).contains(SOURCE_ID));
        assertTrue(definition.recipeTags().contains(RECIPE_TAG_ID));
        assertTrue(definition.unlockAdvancements().contains(ADVANCEMENT_ID));
        assertEquals(3, definition.outputTag().getInt("CustomModelData"));
        assertEquals("Unlock by entering your mode", RecipePermissionApi.unlockHint(RECIPE_ID).orElseThrow().getString());

        RecipePermissionApi.unregisterRestrictedRecipe(RECIPE_ID);
        RecipePermissionApi.setDatapackRestrictionsForTesting(Map.of(), Map.of());
    }

    @Test
    void datapackExactDefinitionsOverrideCodeDefinitions() {
        RecipePermissionApi.unregisterRestrictedRecipe(RECIPE_ID);
        RecipePermissionApi.setDatapackRestrictionsForTesting(Map.of(), Map.of());

        RecipePermissionApi.registerRestrictedRecipe(RestrictedRecipeDefinition.builder(RECIPE_ID)
                .unlockHint(Component.literal("Code"))
                .hiddenWhenLocked(true)
                .build());
        RecipePermissionApi.setDatapackRestrictionsForTesting(
                Map.of(RECIPE_ID, RestrictedRecipeDefinition.builder(RECIPE_ID)
                        .unlockHint(Component.literal("Datapack"))
                        .hiddenWhenLocked(false)
                        .build()),
                Map.of()
        );

        RestrictedRecipeDefinition definition = RecipePermissionApi.findRestrictedRecipe(RECIPE_ID).orElseThrow();
        assertEquals("Datapack", definition.unlockHint().getString());
        assertTrue(!definition.hiddenWhenLocked());

        RecipePermissionApi.unregisterRestrictedRecipe(RECIPE_ID);
        RecipePermissionApi.setDatapackRestrictionsForTesting(Map.of(), Map.of());
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }
}
