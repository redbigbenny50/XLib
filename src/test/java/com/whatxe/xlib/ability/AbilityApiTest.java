package com.whatxe.xlib.ability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class AbilityApiTest {
    private static final ResourceLocation ABILITY_ID = id("unregister_test");
    private static final ResourceLocation STALE_ABILITY_ID = id("stale_unregister_test");
    private static final ResourceLocation METADATA_ABILITY_ID = id("metadata_test");
    private static final ResourceLocation FAMILY_ID = id("family/striker");
    private static final ResourceLocation GROUP_ID = id("group/offense");
    private static final ResourceLocation PAGE_ID = id("page/core");
    private static final ResourceLocation PRIMARY_TAG_ID = id("tag/melee");
    private static final ResourceLocation SECONDARY_TAG_ID = id("tag/burst");

    @Test
    void unregisterAbilityRemovesDefinitionAndDefaultLoadout() {
        AbilityApi.unregisterAbility(ABILITY_ID);

        AbilityDefinition definition = AbilityDefinition.builder(ABILITY_ID, AbilityIcon.ofTexture(id("ability_icon")))
                .action((player, data) -> AbilityUseResult.success(data))
                .build();

        AbilityApi.registerAbility(definition);
        AbilityApi.setDefaultAbility(0, ABILITY_ID);

        assertTrue(AbilityApi.findAbility(ABILITY_ID).isPresent());
        assertEquals(ABILITY_ID, AbilityApi.createDefaultData().abilityInSlot(0).orElseThrow());

        AbilityApi.unregisterAbility(ABILITY_ID);

        assertTrue(AbilityApi.findAbility(ABILITY_ID).isEmpty());
        assertTrue(AbilityApi.createDefaultData().abilityInSlot(0).isEmpty());
    }

    @Test
    void sanitizeDataClearsStateForUnregisteredAbilities() {
        AbilityApi.unregisterAbility(STALE_ABILITY_ID);

        AbilityDefinition definition = AbilityDefinition.builder(STALE_ABILITY_ID, AbilityIcon.ofTexture(id("stale_ability_icon")))
                .action((player, data) -> AbilityUseResult.success(data))
                .build();

        AbilityApi.registerAbility(definition);

        AbilityData data = AbilityData.empty()
                .withAbilityInSlot(0, STALE_ABILITY_ID)
                .withCooldown(STALE_ABILITY_ID, 40)
                .withMode(STALE_ABILITY_ID, true)
                .withActiveDuration(STALE_ABILITY_ID, 20)
                .withChargeCount(STALE_ABILITY_ID, 1)
                .withChargeRecharge(STALE_ABILITY_ID, 10)
                .withAbilityGrantSource(STALE_ABILITY_ID, id("source"), true)
                .withAbilityActivationBlockSource(STALE_ABILITY_ID, id("block_source"), true);

        AbilityApi.unregisterAbility(STALE_ABILITY_ID);
        AbilityData sanitized = AbilityApi.sanitizeData(data);

        assertTrue(sanitized.abilityInSlot(0).isEmpty());
        assertEquals(0, sanitized.cooldownFor(STALE_ABILITY_ID));
        assertFalse(sanitized.isModeActive(STALE_ABILITY_ID));
        assertEquals(0, sanitized.activeDurationFor(STALE_ABILITY_ID));
        assertTrue(sanitized.abilityGrantSourcesFor(STALE_ABILITY_ID).isEmpty());
        assertTrue(sanitized.activationBlockSourcesFor(STALE_ABILITY_ID).isEmpty());
    }

    @Test
    void abilityMetadataIsStoredAndQueryable() {
        AbilityApi.unregisterAbility(METADATA_ABILITY_ID);

        try {
            AbilityApi.registerAbility(AbilityDefinition.builder(METADATA_ABILITY_ID, AbilityIcon.ofTexture(id("metadata_icon")))
                    .family(FAMILY_ID)
                    .group(GROUP_ID)
                    .page(PAGE_ID)
                    .tag(PRIMARY_TAG_ID)
                    .tag(SECONDARY_TAG_ID)
                    .action((player, data) -> AbilityUseResult.success(data))
                    .build());

            AbilityDefinition definition = AbilityApi.findAbility(METADATA_ABILITY_ID).orElseThrow();
            assertEquals(FAMILY_ID, definition.familyId().orElseThrow());
            assertEquals(GROUP_ID, definition.groupId().orElseThrow());
            assertEquals(PAGE_ID, definition.pageId().orElseThrow());
            assertTrue(definition.hasTag(PRIMARY_TAG_ID));
            assertTrue(definition.hasTag(SECONDARY_TAG_ID));
            assertEquals(
                    List.of(FAMILY_ID, GROUP_ID, PAGE_ID, PRIMARY_TAG_ID, SECONDARY_TAG_ID),
                    definition.metadataIds()
            );

            assertTrue(AbilityApi.abilitiesInFamily(FAMILY_ID).stream().anyMatch(ability -> ability.id().equals(METADATA_ABILITY_ID)));
            assertTrue(AbilityApi.abilitiesInGroup(GROUP_ID).stream().anyMatch(ability -> ability.id().equals(METADATA_ABILITY_ID)));
            assertTrue(AbilityApi.abilitiesOnPage(PAGE_ID).stream().anyMatch(ability -> ability.id().equals(METADATA_ABILITY_ID)));
            assertTrue(AbilityApi.abilitiesWithTag(PRIMARY_TAG_ID).stream().anyMatch(ability -> ability.id().equals(METADATA_ABILITY_ID)));
        } finally {
            AbilityApi.unregisterAbility(METADATA_ABILITY_ID);
        }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }
}

