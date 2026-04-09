package com.whatxe.xlib.ability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class PassiveApiTest {
    private static final ResourceLocation PASSIVE_ID = id("metadata_passive");
    private static final ResourceLocation FAMILY_ID = id("family/defense");
    private static final ResourceLocation GROUP_ID = id("group/guard");
    private static final ResourceLocation PAGE_ID = id("page/core");
    private static final ResourceLocation PRIMARY_TAG_ID = id("tag/counter");
    private static final ResourceLocation SECONDARY_TAG_ID = id("tag/reactive");

    @Test
    void passiveMetadataIsStoredAndQueryable() {
        PassiveApi.unregisterPassive(PASSIVE_ID);

        try {
            PassiveApi.registerPassive(PassiveDefinition.builder(PASSIVE_ID, AbilityIcon.ofTexture(id("passive_icon")))
                    .family(FAMILY_ID)
                    .group(GROUP_ID)
                    .page(PAGE_ID)
                    .tag(PRIMARY_TAG_ID)
                    .tag(SECONDARY_TAG_ID)
                    .build());

            PassiveDefinition definition = PassiveApi.findPassive(PASSIVE_ID).orElseThrow();
            assertEquals(FAMILY_ID, definition.familyId().orElseThrow());
            assertEquals(GROUP_ID, definition.groupId().orElseThrow());
            assertEquals(PAGE_ID, definition.pageId().orElseThrow());
            assertTrue(definition.hasTag(PRIMARY_TAG_ID));
            assertTrue(definition.hasTag(SECONDARY_TAG_ID));
            assertEquals(
                    List.of(FAMILY_ID, GROUP_ID, PAGE_ID, PRIMARY_TAG_ID, SECONDARY_TAG_ID),
                    definition.metadataIds()
            );

            assertTrue(PassiveApi.passivesInFamily(FAMILY_ID).stream().anyMatch(passive -> passive.id().equals(PASSIVE_ID)));
            assertTrue(PassiveApi.passivesInGroup(GROUP_ID).stream().anyMatch(passive -> passive.id().equals(PASSIVE_ID)));
            assertTrue(PassiveApi.passivesOnPage(PAGE_ID).stream().anyMatch(passive -> passive.id().equals(PASSIVE_ID)));
            assertTrue(PassiveApi.passivesWithTag(PRIMARY_TAG_ID).stream().anyMatch(passive -> passive.id().equals(PASSIVE_ID)));
        } finally {
            PassiveApi.unregisterPassive(PASSIVE_ID);
        }
    }

    @Test
    void passiveHookAndSoundTriggerIntrospectionReflectsAuthoredBehavior() {
        PassiveDefinition definition = PassiveDefinition.builder(PASSIVE_ID, AbilityIcon.ofTexture(id("passive_icon")))
                .ticker((player, data) -> data)
                .onHit((player, data, target) -> data)
                .onArmorChange((player, data, slot, from, to) -> data)
                .sound(PassiveSoundTrigger.GRANTED, id("granted_sound"))
                .build();

        assertEquals(
                Set.of(PassiveDefinition.Hook.TICK, PassiveDefinition.Hook.HIT, PassiveDefinition.Hook.ARMOR_CHANGE),
                definition.authoredHooks()
        );
        assertTrue(definition.hasHook(PassiveDefinition.Hook.TICK));
        assertTrue(definition.hasHook(PassiveDefinition.Hook.HIT));
        assertFalse(definition.hasHook(PassiveDefinition.Hook.JUMP));
        assertEquals(Set.of(PassiveSoundTrigger.GRANTED), definition.soundTriggers());
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }
}
