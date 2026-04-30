package com.whatxe.xlib.form;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class DataDrivenVisualFormApiTest {

    @Test
    void parsesMinimalFormWithRequiredKind() {
        DataDrivenVisualFormApi.LoadedVisualFormDefinition loaded = DataDrivenVisualFormApi.parseDefinition(
                id("visual_forms/dragon"),
                JsonParser.parseString("""
                        { "kind": "creature" }
                        """)
        );

        assertEquals(id("visual_forms/dragon"), loaded.id());
        assertEquals(VisualFormKind.CREATURE, loaded.definition().kind());
        assertEquals(FirstPersonPolicy.DEFAULT, loaded.definition().firstPersonPolicy());
        assertEquals(1.0f, loaded.definition().renderScale(), 0.001f);
        assertTrue(loaded.definition().modelProfileId().isEmpty());
        assertTrue(loaded.definition().cueRouteProfileId().isEmpty());
        assertTrue(loaded.definition().hudProfileId().isEmpty());
        assertTrue(loaded.definition().soundProfileId().isEmpty());
    }

    @Test
    void parsesExplicitIdOverridesFileId() {
        DataDrivenVisualFormApi.LoadedVisualFormDefinition loaded = DataDrivenVisualFormApi.parseDefinition(
                id("visual_forms/file_name"),
                JsonParser.parseString("""
                        { "id": "xlib_test:forms/explicit", "kind": "humanoid" }
                        """)
        );
        assertEquals(id("forms/explicit"), loaded.id());
    }

    @Test
    void parsesAllKindValues() {
        for (VisualFormKind kind : VisualFormKind.values()) {
            DataDrivenVisualFormApi.LoadedVisualFormDefinition loaded = DataDrivenVisualFormApi.parseDefinition(
                    id("visual_forms/test"),
                    JsonParser.parseString("""
                            { "kind": "%s" }
                            """.formatted(kind.name().toLowerCase(java.util.Locale.ROOT)))
            );
            assertEquals(kind, loaded.definition().kind());
        }
    }

    @Test
    void parsesAllFirstPersonPolicyValues() {
        for (FirstPersonPolicy policy : FirstPersonPolicy.values()) {
            DataDrivenVisualFormApi.LoadedVisualFormDefinition loaded = DataDrivenVisualFormApi.parseDefinition(
                    id("visual_forms/test"),
                    JsonParser.parseString("""
                            { "kind": "abstract", "first_person_policy": "%s" }
                            """.formatted(policy.name().toLowerCase(java.util.Locale.ROOT)))
            );
            assertEquals(policy, loaded.definition().firstPersonPolicy());
        }
    }

    @Test
    void parsesFullForm() {
        DataDrivenVisualFormApi.LoadedVisualFormDefinition loaded = DataDrivenVisualFormApi.parseDefinition(
                id("visual_forms/test"),
                JsonParser.parseString("""
                        {
                          "id": "xlib_test:forms/full",
                          "kind": "humanoid",
                          "model_profile": "xlib_test:profiles/dragon_model",
                          "cue_route_profile": "xlib_test:profiles/dragon_cue",
                          "hud_profile": "xlib_test:profiles/dragon_hud",
                          "sound_profile": "xlib_test:profiles/dragon_sound",
                          "first_person_policy": "hidden",
                          "render_scale": 1.5
                        }
                        """)
        );

        assertEquals(id("forms/full"), loaded.id());
        assertEquals(VisualFormKind.HUMANOID, loaded.definition().kind());
        assertEquals(id("profiles/dragon_model"), loaded.definition().modelProfileId().orElseThrow());
        assertEquals(id("profiles/dragon_cue"), loaded.definition().cueRouteProfileId().orElseThrow());
        assertEquals(id("profiles/dragon_hud"), loaded.definition().hudProfileId().orElseThrow());
        assertEquals(id("profiles/dragon_sound"), loaded.definition().soundProfileId().orElseThrow());
        assertEquals(FirstPersonPolicy.HIDDEN, loaded.definition().firstPersonPolicy());
        assertEquals(1.5f, loaded.definition().renderScale(), 0.001f);
    }

    @Test
    void rejectsMissingKind() {
        assertThrows(Exception.class, () -> DataDrivenVisualFormApi.parseDefinition(
                id("visual_forms/test"),
                JsonParser.parseString("{}")
        ));
    }

    @Test
    void rejectsUnknownKind() {
        assertThrows(IllegalArgumentException.class, () -> DataDrivenVisualFormApi.parseDefinition(
                id("visual_forms/test"),
                JsonParser.parseString("""
                        { "kind": "mech" }
                        """)
        ));
    }

    @Test
    void rejectsNonPositiveRenderScale() {
        assertThrows(IllegalArgumentException.class, () -> DataDrivenVisualFormApi.parseDefinition(
                id("visual_forms/test"),
                JsonParser.parseString("""
                        { "kind": "creature", "render_scale": 0.0 }
                        """)
        ));
    }

    @Test
    void rejectsUnknownFirstPersonPolicy() {
        assertThrows(IllegalArgumentException.class, () -> DataDrivenVisualFormApi.parseDefinition(
                id("visual_forms/test"),
                JsonParser.parseString("""
                        { "kind": "creature", "first_person_policy": "invisible" }
                        """)
        ));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }
}
