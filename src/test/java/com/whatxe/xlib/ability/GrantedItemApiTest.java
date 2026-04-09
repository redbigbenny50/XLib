package com.whatxe.xlib.ability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

class GrantedItemApiTest {
    private static final ResourceLocation GRANTED_ITEM_ID = id("metadata_item");
    private static final ResourceLocation FAMILY_ID = id("family/relics");
    private static final ResourceLocation GROUP_ID = id("group/artifacts");
    private static final ResourceLocation PAGE_ID = id("page/rare");
    private static final ResourceLocation PRIMARY_TAG_ID = id("tag/bound");
    private static final ResourceLocation SECONDARY_TAG_ID = id("tag/quest");

    @Test
    void grantedItemMetadataIsStoredAndQueryable() {
        GrantedItemApi.unregisterGrantedItem(GRANTED_ITEM_ID);

        try {
            GrantedItemApi.registerGrantedItem(GrantedItemDefinition.builder(
                            GRANTED_ITEM_ID,
                            (player, data) -> ItemStack.EMPTY
                    )
                    .family(FAMILY_ID)
                    .group(GROUP_ID)
                    .page(PAGE_ID)
                    .tag(PRIMARY_TAG_ID)
                    .tag(SECONDARY_TAG_ID)
                    .build());

            GrantedItemDefinition definition = GrantedItemApi.findGrantedItem(GRANTED_ITEM_ID).orElseThrow();
            assertEquals(FAMILY_ID, definition.familyId().orElseThrow());
            assertEquals(GROUP_ID, definition.groupId().orElseThrow());
            assertEquals(PAGE_ID, definition.pageId().orElseThrow());
            assertTrue(definition.hasTag(PRIMARY_TAG_ID));
            assertTrue(definition.hasTag(SECONDARY_TAG_ID));
            assertEquals(List.of(FAMILY_ID, GROUP_ID, PAGE_ID, PRIMARY_TAG_ID, SECONDARY_TAG_ID), definition.metadataIds());

            assertTrue(GrantedItemApi.grantedItemsInFamily(FAMILY_ID).stream().anyMatch(found -> found.id().equals(GRANTED_ITEM_ID)));
            assertTrue(GrantedItemApi.grantedItemsInGroup(GROUP_ID).stream().anyMatch(found -> found.id().equals(GRANTED_ITEM_ID)));
            assertTrue(GrantedItemApi.grantedItemsOnPage(PAGE_ID).stream().anyMatch(found -> found.id().equals(GRANTED_ITEM_ID)));
            assertTrue(GrantedItemApi.grantedItemsWithTag(PRIMARY_TAG_ID).stream().anyMatch(found -> found.id().equals(GRANTED_ITEM_ID)));
        } finally {
            GrantedItemApi.unregisterGrantedItem(GRANTED_ITEM_ID);
        }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }
}
