package com.whatxe.xlib.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class PresentationApiTest {
    private static final ResourceLocation CUSTOM_ABILITY_MENU_ID = id("custom_ability_menu");
    private static final ResourceLocation CUSTOM_PROGRESSION_MENU_ID = id("custom_progression_menu");
    private static final ResourceLocation CUSTOM_COMBAT_HUD_ID = id("custom_combat_hud");

    @AfterEach
    void restoreDefaults() {
        AbilityMenuPresentationApi.restoreDefaults();
        ProgressionMenuPresentationApi.restoreDefaults();
        CombatHudPresentationApi.restoreDefaults();
    }

    @Test
    void abilityMenuPresentationsSupportCustomActivationAndFallback() {
        AbilityMenuPresentation custom = AbilityMenuPresentation.builder(CUSTOM_ABILITY_MENU_ID)
                .entryDetail(AbilityMenuPresentation.EntryDetail.PAGE)
                .showRequirementBreakdown(false)
                .build();

        AbilityMenuPresentationApi.register(custom);
        AbilityMenuPresentationApi.activate(CUSTOM_ABILITY_MENU_ID);

        assertEquals(CUSTOM_ABILITY_MENU_ID, AbilityMenuPresentationApi.active().id());
        assertFalse(AbilityMenuPresentationApi.active().showRequirementBreakdown());

        AbilityMenuPresentationApi.unregister(CUSTOM_ABILITY_MENU_ID);

        assertEquals(AbilityMenuPresentation.defaultPresentation().id(), AbilityMenuPresentationApi.active().id());
    }

    @Test
    void progressionPresentationsExposeLayoutPolicies() {
        ProgressionTreeLayout treeLayout = ProgressionTreeLayout.builder()
                .columnSpacing(168)
                .rowSpacing(112)
                .labelWidth(132)
                .maxLabelLines(4)
                .build();
        ProgressionMenuPresentation custom = ProgressionMenuPresentation.builder(CUSTOM_PROGRESSION_MENU_ID)
                .defaultLayoutMode(ProgressionNodeLayoutMode.TREE)
                .availableLayoutModes(java.util.Set.of(ProgressionNodeLayoutMode.LIST, ProgressionNodeLayoutMode.TREE))
                .showPointSourceHints(false)
                .treeLayout(treeLayout)
                .build();

        ProgressionMenuPresentationApi.register(custom);
        ProgressionMenuPresentationApi.activate(CUSTOM_PROGRESSION_MENU_ID);

        ProgressionMenuPresentation active = ProgressionMenuPresentationApi.active();
        assertEquals(ProgressionNodeLayoutMode.TREE, active.defaultLayoutMode());
        assertTrue(active.availableLayoutModes().contains(ProgressionNodeLayoutMode.LIST));
        assertTrue(active.availableLayoutModes().contains(ProgressionNodeLayoutMode.TREE));
        assertFalse(active.showPointSourceHints());
        assertEquals(168, active.treeLayout().columnSpacing());
        assertEquals(112, active.treeLayout().rowSpacing());
        assertEquals(132, active.treeLayout().labelWidth());
        assertEquals(4, active.treeLayout().maxLabelLines());
    }

    @Test
    void combatHudPresentationsSupportAlternateResourceLabelPolicies() {
        CombatHudPresentation custom = CombatHudPresentation.builder(CUSTOM_COMBAT_HUD_ID)
                .showActiveAbilityName(false)
                .resourceLabelMode(CombatHudPresentation.ResourceLabelMode.LONG)
                .build();

        CombatHudPresentationApi.register(custom);
        CombatHudPresentationApi.activate(CUSTOM_COMBAT_HUD_ID);

        CombatHudPresentation active = CombatHudPresentationApi.active();
        assertEquals(CUSTOM_COMBAT_HUD_ID, active.id());
        assertEquals(CombatHudPresentation.ResourceLabelMode.LONG, active.resourceLabelMode());
        assertFalse(active.showActiveAbilityName());
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }
}
