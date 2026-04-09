package com.whatxe.xlib.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AbilityUiSurfaceApiTest {
    private static final ResourceLocation CUSTOM_ABILITY_MENU_ID = id("custom_ability_menu_screen");
    private static final ResourceLocation CUSTOM_PROGRESSION_MENU_ID = id("custom_progression_menu_screen");
    private static final ResourceLocation CUSTOM_PROFILE_SELECTION_ID = id("custom_profile_selection_screen");
    private static final ResourceLocation CUSTOM_COMBAT_HUD_ID = id("custom_combat_hud_renderer");
    private static final ResourceLocation CUSTOM_LOADOUT_HANDLER_ID = id("custom_loadout_quick_switch");

    @AfterEach
    void restoreDefaults() {
        AbilityMenuScreenFactoryApi.restoreDefaults();
        ProgressionMenuScreenFactoryApi.restoreDefaults();
        ProfileSelectionScreenFactoryApi.restoreDefaults();
        CombatHudRendererApi.restoreDefaults();
        AbilityLoadoutQuickSwitchApi.clearHandlers();
        AbilityMenuSessionStateApi.reset();
        ProgressionMenuSessionStateApi.reset();
    }

    @Test
    void abilityMenuFactoriesSupportCustomActivationAndFallback() {
        AbilityMenuScreenFactoryApi.register(CUSTOM_ABILITY_MENU_ID, DummyAbilityScreen::new);
        AbilityMenuScreenFactoryApi.activate(CUSTOM_ABILITY_MENU_ID);

        assertEquals(CUSTOM_ABILITY_MENU_ID, AbilityMenuScreenFactoryApi.activeFactoryId());
        assertTrue(AbilityMenuScreenFactoryApi.createActive() instanceof DummyAbilityScreen);

        AbilityMenuScreenFactoryApi.unregister(CUSTOM_ABILITY_MENU_ID);

        assertFalse(AbilityMenuScreenFactoryApi.activeFactoryId().equals(CUSTOM_ABILITY_MENU_ID));
        assertNotNull(AbilityMenuScreenFactoryApi.createActive());
    }

    @Test
    void progressionMenuFactoriesSupportCustomActivationAndFallback() {
        ProgressionMenuScreenFactoryApi.register(CUSTOM_PROGRESSION_MENU_ID, DummyProgressionScreen::new);
        ProgressionMenuScreenFactoryApi.activate(CUSTOM_PROGRESSION_MENU_ID);

        assertEquals(CUSTOM_PROGRESSION_MENU_ID, ProgressionMenuScreenFactoryApi.activeFactoryId());
        assertTrue(ProgressionMenuScreenFactoryApi.createActive() instanceof DummyProgressionScreen);

        ProgressionMenuScreenFactoryApi.unregister(CUSTOM_PROGRESSION_MENU_ID);

        assertFalse(ProgressionMenuScreenFactoryApi.activeFactoryId().equals(CUSTOM_PROGRESSION_MENU_ID));
        assertNotNull(ProgressionMenuScreenFactoryApi.createActive());
    }

    @Test
    void profileSelectionFactoriesSupportCustomActivationAndFallback() {
        ProfileSelectionScreenFactoryApi.register(CUSTOM_PROFILE_SELECTION_ID, DummyProfileSelectionScreen::new);
        ProfileSelectionScreenFactoryApi.activate(CUSTOM_PROFILE_SELECTION_ID);

        assertEquals(CUSTOM_PROFILE_SELECTION_ID, ProfileSelectionScreenFactoryApi.activeFactoryId());
        assertTrue(ProfileSelectionScreenFactoryApi.createActive() instanceof DummyProfileSelectionScreen);

        ProfileSelectionScreenFactoryApi.unregister(CUSTOM_PROFILE_SELECTION_ID);

        assertFalse(ProfileSelectionScreenFactoryApi.activeFactoryId().equals(CUSTOM_PROFILE_SELECTION_ID));
        assertNotNull(ProfileSelectionScreenFactoryApi.createActive());
    }

    @Test
    void profileSelectionContextAwareFactoriesReceivePendingGroupState() {
        ResourceLocation pendingGroupId = id("pending_group");
        ProfileSelectionScreenFactoryApi.register(CUSTOM_PROFILE_SELECTION_ID, context ->
                new DummyContextProfileSelectionScreen(context.pendingGroupId()));
        ProfileSelectionScreenFactoryApi.activate(CUSTOM_PROFILE_SELECTION_ID);

        Screen profileScreen = ProfileSelectionScreenFactoryApi.createActive(new ProfileSelectionScreenContext(pendingGroupId));

        assertTrue(profileScreen instanceof DummyContextProfileSelectionScreen);
        assertEquals(pendingGroupId, ((DummyContextProfileSelectionScreen) profileScreen).pendingGroupId());
    }

    @Test
    void contextAwareFactoriesReceiveSharedMenuState() {
        AbilityMenuScreenFactoryApi.register(CUSTOM_ABILITY_MENU_ID, context ->
                new DummyContextAbilityScreen(context.sessionState().selectedSlot()));
        ProgressionMenuScreenFactoryApi.register(CUSTOM_PROGRESSION_MENU_ID, context ->
                new DummyContextProgressionScreen(context.sessionState().layoutMode()));

        AbilityMenuScreenFactoryApi.activate(CUSTOM_ABILITY_MENU_ID);
        ProgressionMenuScreenFactoryApi.activate(CUSTOM_PROGRESSION_MENU_ID);

        Screen abilityScreen = AbilityMenuScreenFactoryApi.createActive(new AbilityMenuScreenContext(
                null,
                AbilityMenuSessionState.of(4, null)
        ));
        Screen progressionScreen = ProgressionMenuScreenFactoryApi.createActive(new ProgressionMenuScreenContext(
                null,
                new ProgressionMenuSessionState(null, null, com.whatxe.xlib.presentation.ProgressionNodeLayoutMode.TREE)
        ));

        assertTrue(abilityScreen instanceof DummyContextAbilityScreen);
        assertEquals(4, ((DummyContextAbilityScreen) abilityScreen).selectedSlot());
        assertTrue(progressionScreen instanceof DummyContextProgressionScreen);
        assertEquals(com.whatxe.xlib.presentation.ProgressionNodeLayoutMode.TREE,
                ((DummyContextProgressionScreen) progressionScreen).layoutMode());
    }

    @Test
    void combatHudRenderersSupportCustomActivationAndFallback() {
        CombatHudRenderer customRenderer = context -> {};
        CombatHudRendererApi.register(CUSTOM_COMBAT_HUD_ID, customRenderer);
        CombatHudRendererApi.activate(CUSTOM_COMBAT_HUD_ID);

        assertEquals(CUSTOM_COMBAT_HUD_ID, CombatHudRendererApi.activeRendererId());
        assertTrue(CombatHudRendererApi.find(CUSTOM_COMBAT_HUD_ID).isPresent());
        assertEquals(customRenderer, CombatHudRendererApi.active());

        CombatHudRendererApi.unregister(CUSTOM_COMBAT_HUD_ID);

        assertFalse(CombatHudRendererApi.activeRendererId().equals(CUSTOM_COMBAT_HUD_ID));
        assertTrue(CombatHudRendererApi.find(CUSTOM_COMBAT_HUD_ID).isEmpty());
        assertNotNull(CombatHudRendererApi.active());
    }

    @Test
    void loadoutQuickSwitchHandlersSupportRegistrationAndRemoval() {
        AbilityLoadoutQuickSwitchHandler handler = minecraft -> true;
        AbilityLoadoutQuickSwitchApi.register(CUSTOM_LOADOUT_HANDLER_ID, handler);

        assertTrue(AbilityLoadoutQuickSwitchApi.find(CUSTOM_LOADOUT_HANDLER_ID).isPresent());
        assertEquals(1, AbilityLoadoutQuickSwitchApi.allHandlerIds().size());

        AbilityLoadoutQuickSwitchApi.unregister(CUSTOM_LOADOUT_HANDLER_ID);

        assertTrue(AbilityLoadoutQuickSwitchApi.find(CUSTOM_LOADOUT_HANDLER_ID).isEmpty());
        assertTrue(AbilityLoadoutQuickSwitchApi.allHandlerIds().isEmpty());
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }

    private static final class DummyAbilityScreen extends Screen {
        private DummyAbilityScreen() {
            super(Component.literal("dummy ability"));
        }
    }

    private static final class DummyProgressionScreen extends Screen {
        private DummyProgressionScreen() {
            super(Component.literal("dummy progression"));
        }
    }

    private static final class DummyProfileSelectionScreen extends Screen {
        private DummyProfileSelectionScreen() {
            super(Component.literal("dummy profile"));
        }
    }

    private static final class DummyContextAbilityScreen extends Screen {
        private final int selectedSlot;

        private DummyContextAbilityScreen(int selectedSlot) {
            super(Component.literal("dummy context ability"));
            this.selectedSlot = selectedSlot;
        }

        private int selectedSlot() {
            return this.selectedSlot;
        }
    }

    private static final class DummyContextProgressionScreen extends Screen {
        private final com.whatxe.xlib.presentation.ProgressionNodeLayoutMode layoutMode;

        private DummyContextProgressionScreen(com.whatxe.xlib.presentation.ProgressionNodeLayoutMode layoutMode) {
            super(Component.literal("dummy context progression"));
            this.layoutMode = layoutMode;
        }

        private com.whatxe.xlib.presentation.ProgressionNodeLayoutMode layoutMode() {
            return this.layoutMode;
        }
    }

    private static final class DummyContextProfileSelectionScreen extends Screen {
        private final ResourceLocation pendingGroupId;

        private DummyContextProfileSelectionScreen(ResourceLocation pendingGroupId) {
            super(Component.literal("dummy context profile"));
            this.pendingGroupId = pendingGroupId;
        }

        private ResourceLocation pendingGroupId() {
            return this.pendingGroupId;
        }
    }
}
