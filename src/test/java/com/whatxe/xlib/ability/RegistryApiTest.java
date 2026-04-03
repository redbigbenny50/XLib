package com.whatxe.xlib.ability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

class RegistryApiTest {
    private static final ResourceLocation RESOURCE_ID = id("resource_unregister_test");
    private static final ResourceLocation PASSIVE_ID = id("passive_unregister_test");
    private static final ResourceLocation GRANTED_ITEM_ID = id("granted_item_unregister_test");
    private static final ResourceLocation RECIPE_ID = id("recipe_unregister_test");
    private static final ResourceLocation SOURCE_ID = id("source");

    @Test
    void unregisterResourceRemovesDefinitionAndDefaultData() {
        AbilityApi.unregisterResource(RESOURCE_ID);

        AbilityApi.registerResource(AbilityResourceDefinition.builder(RESOURCE_ID)
                .startingAmount(25)
                .build());

        assertEquals(25, AbilityApi.createDefaultData().resourceAmount(RESOURCE_ID));

        AbilityApi.unregisterResource(RESOURCE_ID);

        assertTrue(AbilityApi.findResource(RESOURCE_ID).isEmpty());
        assertEquals(0, AbilityApi.createDefaultData().resourceAmount(RESOURCE_ID));
    }

    @Test
    void sanitizeDataClearsUnregisteredPassiveGrantedItemAndRecipePermissions() {
        PassiveApi.unregisterPassive(PASSIVE_ID);
        GrantedItemApi.unregisterGrantedItem(GRANTED_ITEM_ID);
        RecipePermissionApi.unregisterRestrictedRecipe(RECIPE_ID);

        PassiveApi.registerPassive(PassiveDefinition.builder(PASSIVE_ID, AbilityIcon.ofTexture(id("passive_icon"))).build());
        GrantedItemApi.registerGrantedItem(GrantedItemDefinition.builder(
                GRANTED_ITEM_ID,
                (player, data) -> ItemStack.EMPTY
        ).build());
        RecipePermissionApi.registerRestrictedRecipe(RECIPE_ID);

        AbilityData data = AbilityData.empty()
                .withPassiveGrantSource(PASSIVE_ID, SOURCE_ID, true)
                .withGrantedItemSource(GRANTED_ITEM_ID, SOURCE_ID, true)
                .withRecipePermissionSource(RECIPE_ID, SOURCE_ID, true);

        PassiveApi.unregisterPassive(PASSIVE_ID);
        GrantedItemApi.unregisterGrantedItem(GRANTED_ITEM_ID);
        RecipePermissionApi.unregisterRestrictedRecipe(RECIPE_ID);

        AbilityData sanitized = AbilityApi.sanitizeData(data);

        assertTrue(sanitized.passiveGrantSourcesFor(PASSIVE_ID).isEmpty());
        assertTrue(sanitized.grantedItemSourcesFor(GRANTED_ITEM_ID).isEmpty());
        assertTrue(sanitized.recipePermissionSourcesFor(RECIPE_ID).isEmpty());
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }
}
