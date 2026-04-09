package com.whatxe.xlib.ability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
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
    private static final ResourceLocation FAMILY_ID = id("family/trials");
    private static final ResourceLocation GROUP_ID = id("group/relics");
    private static final ResourceLocation PAGE_ID = id("page/endgame");
    private static final ResourceLocation TAG_ID = id("tag/boss_gate");

    @Test
    void restrictedRecipeMetadataIsQueryable() throws Exception {
        RecipePermissionApi.unregisterRestrictedRecipe(RECIPE_ID);
        RecipePermissionApi.setDatapackRestrictionsForTesting(Map.of(), Map.of());

        RecipePermissionApi.registerRestrictedRecipe(RestrictedRecipeDefinition.builder(RECIPE_ID)
                .family(FAMILY_ID)
                .group(GROUP_ID)
                .page(PAGE_ID)
                .tag(TAG_ID)
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
        assertEquals(FAMILY_ID, definition.familyId().orElseThrow());
        assertEquals(GROUP_ID, definition.groupId().orElseThrow());
        assertEquals(PAGE_ID, definition.pageId().orElseThrow());
        assertTrue(definition.hasTag(TAG_ID));
        assertEquals(List.of(FAMILY_ID, GROUP_ID, PAGE_ID, TAG_ID), definition.metadataIds());
        assertTrue(RecipePermissionApi.recipesInCategory(CATEGORY_ID).contains(RECIPE_ID));
        assertTrue(RecipePermissionApi.recipesForOutput(OUTPUT_ID).contains(RECIPE_ID));
        assertTrue(RecipePermissionApi.unlockSources(RECIPE_ID).contains(SOURCE_ID));
        assertTrue(definition.recipeTags().contains(RECIPE_TAG_ID));
        assertTrue(definition.unlockAdvancements().contains(ADVANCEMENT_ID));
        assertEquals(3, definition.outputTag().getInt("CustomModelData"));
        assertEquals("Unlock by entering your mode", RecipePermissionApi.unlockHint(RECIPE_ID).orElseThrow().getString());
        assertTrue(RecipePermissionApi.restrictedRecipesInFamily(FAMILY_ID).stream().anyMatch(found -> found.recipeId().equals(RECIPE_ID)));
        assertTrue(RecipePermissionApi.restrictedRecipesInGroup(GROUP_ID).stream().anyMatch(found -> found.recipeId().equals(RECIPE_ID)));
        assertTrue(RecipePermissionApi.restrictedRecipesOnPage(PAGE_ID).stream().anyMatch(found -> found.recipeId().equals(RECIPE_ID)));
        assertTrue(RecipePermissionApi.restrictedRecipesWithTag(TAG_ID).stream().anyMatch(found -> found.recipeId().equals(RECIPE_ID)));

        RecipePermissionApi.unregisterRestrictedRecipe(RECIPE_ID);
        RecipePermissionApi.setDatapackRestrictionsForTesting(Map.of(), Map.of());
    }

    @Test
    void selectorRuleMetadataIsStoredAndQueryable() {
        ResourceLocation ruleId = id("metadata_rule");
        RecipePermissionApi.unregisterRestrictedRule(ruleId);
        RecipePermissionApi.setDatapackRestrictionsForTesting(Map.of(), Map.of());

        try {
            RecipePermissionApi.registerRestrictedRule(RestrictedRecipeRule.builder(ruleId)
                    .family(FAMILY_ID)
                    .group(GROUP_ID)
                    .page(PAGE_ID)
                    .tag(TAG_ID)
                    .output(OUTPUT_ID)
                    .build());

            RestrictedRecipeRule rule = RestrictedRecipeRule.builder(ruleId)
                    .family(FAMILY_ID)
                    .group(GROUP_ID)
                    .page(PAGE_ID)
                    .tag(TAG_ID)
                    .output(OUTPUT_ID)
                    .build();
            assertEquals(FAMILY_ID, rule.familyId().orElseThrow());
            assertEquals(GROUP_ID, rule.groupId().orElseThrow());
            assertEquals(PAGE_ID, rule.pageId().orElseThrow());
            assertTrue(rule.hasTag(TAG_ID));
            assertEquals(List.of(FAMILY_ID, GROUP_ID, PAGE_ID, TAG_ID), rule.metadataIds());
        } finally {
            RecipePermissionApi.unregisterRestrictedRule(ruleId);
            RecipePermissionApi.setDatapackRestrictionsForTesting(Map.of(), Map.of());
        }
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
