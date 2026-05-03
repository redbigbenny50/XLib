package com.whatxe.xlib.classification;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class EntityClassificationApiTest {
    private static final ResourceLocation PLAYER_ID = ResourceLocation.withDefaultNamespace("player");
    private static final ResourceLocation ZOMBIE_ID = ResourceLocation.withDefaultNamespace("zombie");
    private static final ResourceLocation DIRECT_TAG_ID = id("tags/xenomorph_like");
    private static final ResourceLocation INHERITED_TAG_ID = id("tags/undead_like");
    private static final ResourceLocation SOURCE_A = id("source/profile");
    private static final ResourceLocation SOURCE_B = id("source/stage");

    @Test
    void syntheticTagsAndEntityTypesRespectMatchModes() {
        ResolvedEntityClassificationState state = new ResolvedEntityClassificationState(
                Set.of(ZOMBIE_ID),
                Set.of(DIRECT_TAG_ID),
                Set.of(INHERITED_TAG_ID)
        );

        assertTrue(state.countsAsEntity(PLAYER_ID, PLAYER_ID, EntityClassificationMatchMode.REAL_ONLY));
        assertFalse(state.countsAsEntity(PLAYER_ID, ZOMBIE_ID, EntityClassificationMatchMode.REAL_ONLY));
        assertTrue(state.countsAsEntity(PLAYER_ID, ZOMBIE_ID, EntityClassificationMatchMode.SYNTHETIC_ONLY));
        assertTrue(state.countsAsEntity(PLAYER_ID, ZOMBIE_ID, EntityClassificationMatchMode.MERGED));

        assertTrue(state.matchesEntityTag(Set.of(), DIRECT_TAG_ID, EntityClassificationMatchMode.SYNTHETIC_ONLY));
        assertTrue(state.matchesEntityTag(Set.of(), INHERITED_TAG_ID, EntityClassificationMatchMode.SYNTHETIC_ONLY));
        assertFalse(state.matchesEntityTag(Set.of(), DIRECT_TAG_ID, EntityClassificationMatchMode.REAL_ONLY));
    }

    @Test
    void classificationDataClearsSourceScopedState() {
        EntityClassificationData data = EntityClassificationData.empty()
                .withSyntheticEntityType(ZOMBIE_ID, SOURCE_A, true)
                .withSyntheticTag(DIRECT_TAG_ID, SOURCE_B, true)
                .clearSource(SOURCE_B);
        assertTrue(data.hasSyntheticEntityType(ZOMBIE_ID));
        assertFalse(data.hasSyntheticTag(DIRECT_TAG_ID));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }
}
