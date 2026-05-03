package com.whatxe.xlib.ability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class ModeAbilityDefinitionTest {
    private static final ResourceLocation MODE_ID = id("mode_wrapper");
    private static final ResourceLocation GRANTED_ABILITY_ID = id("wrapper_granted_ability");
    private static final ResourceLocation MODE_METADATA_ID = id("mode_wrapper_metadata");
    private static final ResourceLocation MODE_FAMILY_ID = id("family/forms");
    private static final ResourceLocation MODE_GROUP_ID = id("group/transforms");
    private static final ResourceLocation MODE_PAGE_ID = id("page/awakenings");
    private static final ResourceLocation MODE_TAG_ID = id("tag/stateful");
    private static final ResourceLocation MODE_STATE_FLAG_ID = id("state/charged");

    @Test
    void registerModeAbilityRegistersBothTheToggleAbilityAndTheMode() {
        ModeApi.unregisterModeAbility(MODE_ID);
        try {
            ModeAbilityDefinition definition = ModeAbilityDefinition.builder(MODE_ID, AbilityIcon.ofTexture(id("wrapper_icon")))
                    .cooldownTicks(40)
                    .stackable()
                    .grantAbility(GRANTED_ABILITY_ID)
                    .stateFlag(MODE_STATE_FLAG_ID)
                    .action((player, data) -> AbilityUseResult.success(data))
                    .register();

            assertTrue(AbilityApi.findAbility(MODE_ID).isPresent());
            assertTrue(ModeApi.findMode(MODE_ID).isPresent());
            assertTrue(definition.ability().toggleAbility());
            assertEquals(MODE_ID, definition.mode().abilityId());
            assertTrue(definition.mode().stackable());
            assertTrue(definition.mode().grantedAbilities().contains(GRANTED_ABILITY_ID));
            assertTrue(definition.mode().stateFlags().contains(MODE_STATE_FLAG_ID));
        } finally {
            ModeApi.unregisterModeAbility(MODE_ID);
        }
    }

    @Test
    void metadataBuilderMethodsFlowIntoTheWrappedAbilityAndMode() {
        ModeAbilityDefinition definition = ModeAbilityDefinition.builder(MODE_METADATA_ID, AbilityIcon.ofTexture(id("wrapper_metadata_icon")))
                .family(MODE_FAMILY_ID)
                .group(MODE_GROUP_ID)
                .page(MODE_PAGE_ID)
                .tag(MODE_TAG_ID)
                .action((player, data) -> AbilityUseResult.success(data))
                .build();

        assertEquals(MODE_FAMILY_ID, definition.ability().familyId().orElseThrow());
        assertEquals(MODE_GROUP_ID, definition.ability().groupId().orElseThrow());
        assertEquals(MODE_PAGE_ID, definition.ability().pageId().orElseThrow());
        assertTrue(definition.ability().hasTag(MODE_TAG_ID));
        assertEquals(MODE_FAMILY_ID, definition.mode().familyId().orElseThrow());
        assertEquals(MODE_GROUP_ID, definition.mode().groupId().orElseThrow());
        assertEquals(MODE_PAGE_ID, definition.mode().pageId().orElseThrow());
        assertTrue(definition.mode().hasTag(MODE_TAG_ID));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }
}
