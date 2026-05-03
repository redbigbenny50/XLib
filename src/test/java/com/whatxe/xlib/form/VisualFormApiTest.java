package com.whatxe.xlib.form;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class VisualFormApiTest {
    private static final ResourceLocation FORM_ID = id("form/beast");
    private static final ResourceLocation FORM_ID_2 = id("form/spirit");
    private static final ResourceLocation STALE_FORM_ID = id("form/stale");
    private static final ResourceLocation SOURCE_A = id("source/lifecycle");

    @Test
    void registrationAndLookup() {
        try {
            VisualFormApi.register(VisualFormDefinition.builder(FORM_ID, VisualFormKind.CREATURE).build());
            assertTrue(VisualFormApi.findDefinition(FORM_ID).isPresent());
            assertFalse(VisualFormApi.findDefinition(STALE_FORM_ID).isPresent());
        } finally {
            unregisterFixtures();
        }
    }

    @Test
    void duplicateRegistrationThrows() {
        try {
            VisualFormApi.register(VisualFormDefinition.builder(FORM_ID, VisualFormKind.CREATURE).build());
            assertThrows(IllegalStateException.class, () ->
                    VisualFormApi.register(VisualFormDefinition.builder(FORM_ID, VisualFormKind.SPIRIT).build()));
        } finally {
            unregisterFixtures();
        }
    }

    @Test
    void unregisterRemovesDefinition() {
        VisualFormApi.register(VisualFormDefinition.builder(FORM_ID, VisualFormKind.HUMANOID).build());
        assertTrue(VisualFormApi.findDefinition(FORM_ID).isPresent());
        VisualFormApi.unregister(FORM_ID);
        assertFalse(VisualFormApi.findDefinition(FORM_ID).isPresent());
    }

    @Test
    void builderDefaultsAreCorrect() {
        VisualFormDefinition def = VisualFormDefinition.builder(FORM_ID, VisualFormKind.VEHICLE).build();
        assertEquals(VisualFormKind.VEHICLE, def.kind());
        assertTrue(def.modelProfileId().isEmpty());
        assertTrue(def.cueRouteProfileId().isEmpty());
        assertTrue(def.hudProfileId().isEmpty());
        assertEquals(FirstPersonPolicy.DEFAULT, def.firstPersonPolicy());
        assertEquals(1.0f, def.renderScale(), 0.001f);
        assertTrue(def.soundProfileId().isEmpty());
    }

    @Test
    void builderWithOptions() {
        VisualFormDefinition def = VisualFormDefinition.builder(FORM_ID, VisualFormKind.CREATURE)
                .modelProfile(id("model/xenomorph"))
                .cueRouteProfile(id("cue/beast"))
                .firstPersonPolicy(FirstPersonPolicy.HIDDEN)
                .renderScale(1.5f)
                .build();
        assertTrue(def.modelProfileId().isPresent());
        assertEquals(id("model/xenomorph"), def.modelProfileId().get());
        assertEquals(FirstPersonPolicy.HIDDEN, def.firstPersonPolicy());
        assertEquals(1.5f, def.renderScale(), 0.001f);
    }

    @Test
    void visualFormDataEmptyHasNoForms() {
        VisualFormData data = VisualFormData.empty();
        assertTrue(data.activeForms().isEmpty());
        assertFalse(data.hasForm(FORM_ID));
        assertTrue(data.primaryForm().isEmpty());
    }

    @Test
    void visualFormDataWithFormMutationsAreImmutable() {
        VisualFormData empty = VisualFormData.empty();
        VisualFormData withForm = empty.withForm(FORM_ID, SOURCE_A);

        assertTrue(withForm.hasForm(FORM_ID));
        assertFalse(empty.hasForm(FORM_ID));

        VisualFormData withoutForm = withForm.withoutForm(FORM_ID);
        assertFalse(withoutForm.hasForm(FORM_ID));
        assertTrue(withForm.hasForm(FORM_ID));
    }

    @Test
    void clearSourceRemovesOwnedForms() {
        VisualFormData data = VisualFormData.empty()
                .withForm(FORM_ID, SOURCE_A)
                .withForm(FORM_ID_2, id("source/other"));

        VisualFormData cleared = data.clearSource(SOURCE_A);
        assertFalse(cleared.hasForm(FORM_ID));
        assertTrue(cleared.hasForm(FORM_ID_2));
    }

    @Test
    void retainRegisteredDropsUnknownForms() {
        try {
            VisualFormApi.register(VisualFormDefinition.builder(FORM_ID, VisualFormKind.SPIRIT).build());
            VisualFormData data = VisualFormData.empty()
                    .withForm(FORM_ID, SOURCE_A)
                    .withForm(STALE_FORM_ID, SOURCE_A);

            VisualFormData sanitized = VisualFormApi.sanitize(data);
            assertTrue(sanitized.hasForm(FORM_ID));
            assertFalse(sanitized.hasForm(STALE_FORM_ID));
        } finally {
            unregisterFixtures();
        }
    }

    @Test
    void allDefinitionsReturnsRegisteredOnes() {
        try {
            VisualFormApi.register(VisualFormDefinition.builder(FORM_ID, VisualFormKind.CREATURE).build());
            VisualFormApi.register(VisualFormDefinition.builder(FORM_ID_2, VisualFormKind.SPIRIT).build());
            assertEquals(2, VisualFormApi.all().size());
        } finally {
            unregisterFixtures();
        }
    }

    private void unregisterFixtures() {
        VisualFormApi.unregister(FORM_ID);
        VisualFormApi.unregister(FORM_ID_2);
        VisualFormApi.unregister(STALE_FORM_ID);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("test", path);
    }
}
