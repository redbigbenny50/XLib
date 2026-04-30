package com.whatxe.xlib.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class DataDrivenLifecycleStageApiTest {

    @Test
    void parsesMinimalStageUsingFileId() {
        DataDrivenLifecycleStageApi.LoadedLifecycleStageDefinition definition = DataDrivenLifecycleStageApi.parseDefinition(
                id("lifecycle_stages/newborn"),
                JsonParser.parseString("{}")
        );

        assertEquals(id("lifecycle_stages/newborn"), definition.id());
        assertTrue(definition.definition().durationTicks().isEmpty());
        assertTrue(definition.definition().autoTransitions().isEmpty());
        assertTrue(definition.definition().manualTransitionTargets().isEmpty());
        assertTrue(definition.definition().projectedGrantBundles().isEmpty());
    }

    @Test
    void parsesExplicitIdOverridesFileId() {
        DataDrivenLifecycleStageApi.LoadedLifecycleStageDefinition definition = DataDrivenLifecycleStageApi.parseDefinition(
                id("lifecycle_stages/file_name"),
                JsonParser.parseString("""
                        { "id": "xlib_test:stages/explicit" }
                        """)
        );

        assertEquals(id("stages/explicit"), definition.id());
    }

    @Test
    void parsesFullStageDefinition() {
        DataDrivenLifecycleStageApi.LoadedLifecycleStageDefinition loaded = DataDrivenLifecycleStageApi.parseDefinition(
                id("lifecycle_stages/test"),
                JsonParser.parseString("""
                        {
                          "id": "xlib_test:stages/test",
                          "duration_ticks": 600,
                          "auto_transitions": [
                            { "target": "xlib_test:stages/next", "trigger": "timer", "preserve_elapsed": true }
                          ],
                          "manual_transition_targets": ["xlib_test:stages/alt"],
                          "project_state_flags": ["xlib_test:flags/active"],
                          "project_grant_bundles": ["xlib_test:bundles/base"],
                          "project_identities": ["xlib_test:identities/form"],
                          "project_capability_policies": ["xlib_test:policies/restrict"],
                          "project_visual_form": "xlib_test:forms/dragon"
                        }
                        """)
        );

        LifecycleStageDefinition definition = loaded.definition();
        assertEquals(id("stages/test"), loaded.id());
        assertEquals(600, definition.durationTicks().orElseThrow());
        assertEquals(1, definition.autoTransitions().size());

        LifecycleStageTransition transition = definition.autoTransitions().get(0);
        assertEquals(id("stages/next"), transition.targetStageId());
        assertEquals(LifecycleStageTrigger.TIMER, transition.trigger());
        assertTrue(transition.preserveElapsed());

        assertTrue(definition.manualTransitionTargets().contains(id("stages/alt")));
        assertTrue(definition.projectedStateFlags().contains(id("flags/active")));
        assertTrue(definition.projectedGrantBundles().contains(id("bundles/base")));
        assertTrue(definition.projectedIdentities().contains(id("identities/form")));
        assertTrue(definition.projectedCapabilityPolicies().contains(id("policies/restrict")));
        assertEquals(id("forms/dragon"), definition.projectedVisualForm().orElseThrow());
    }

    @Test
    void parsesAllTriggerTypes() {
        for (String triggerName : new String[]{"timer", "manual", "death", "respawn", "advancement", "condition"}) {
            DataDrivenLifecycleStageApi.LoadedLifecycleStageDefinition loaded = DataDrivenLifecycleStageApi.parseDefinition(
                    id("lifecycle_stages/test"),
                    JsonParser.parseString("""
                            {
                              "auto_transitions": [
                                { "target": "xlib_test:stages/next", "trigger": "%s" }
                              ]
                            }
                            """.formatted(triggerName))
            );
            assertEquals(1, loaded.definition().autoTransitions().size());
            assertEquals(
                    LifecycleStageTrigger.valueOf(triggerName.toUpperCase(java.util.Locale.ROOT)),
                    loaded.definition().autoTransitions().get(0).trigger()
            );
        }
    }

    @Test
    void rejectsUnknownTrigger() {
        assertThrows(IllegalArgumentException.class, () -> DataDrivenLifecycleStageApi.parseDefinition(
                id("lifecycle_stages/test"),
                JsonParser.parseString("""
                        { "auto_transitions": [{ "target": "xlib_test:stages/next", "trigger": "explode" }] }
                        """)
        ));
    }

    @Test
    void rejectsNonPositiveDurationTicks() {
        assertThrows(IllegalArgumentException.class, () -> DataDrivenLifecycleStageApi.parseDefinition(
                id("lifecycle_stages/test"),
                JsonParser.parseString("""
                        { "duration_ticks": 0 }
                        """)
        ));
    }

    @Test
    void preserveElapsedDefaultsFalse() {
        DataDrivenLifecycleStageApi.LoadedLifecycleStageDefinition loaded = DataDrivenLifecycleStageApi.parseDefinition(
                id("lifecycle_stages/test"),
                JsonParser.parseString("""
                        { "auto_transitions": [{ "target": "xlib_test:stages/next", "trigger": "death" }] }
                        """)
        );
        assertFalse(loaded.definition().autoTransitions().get(0).preserveElapsed());
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }
}
