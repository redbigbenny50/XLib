package com.whatxe.xlib.binding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class EntityBindingApiTest {
    private static final ResourceLocation BINDING_ID = id("binding/leash_link");
    private static final ResourceLocation BINDING_ID_2 = id("binding/parasite_attach");
    private static final ResourceLocation STALE_BINDING_ID = id("binding/stale");
    private static final ResourceLocation SOURCE_A = id("source/form_system");

    @Test
    void registrationAndLookup() {
        try {
            EntityBindingApi.register(EntityBindingDefinition.builder(BINDING_ID, EntityBindingKind.LINK)
                    .primaryRole("controller").secondaryRole("target")
                    .build());

            assertTrue(EntityBindingApi.findDefinition(BINDING_ID).isPresent());
            assertFalse(EntityBindingApi.findDefinition(STALE_BINDING_ID).isPresent());
        } finally {
            unregisterFixtures();
        }
    }

    @Test
    void duplicateRegistrationThrows() {
        try {
            EntityBindingApi.register(EntityBindingDefinition.builder(BINDING_ID, EntityBindingKind.CONTROL).build());
            assertThrows(IllegalStateException.class, () ->
                    EntityBindingApi.register(EntityBindingDefinition.builder(BINDING_ID, EntityBindingKind.LINK).build()));
        } finally {
            unregisterFixtures();
        }
    }

    @Test
    void unregisterRemovesDefinition() {
        EntityBindingApi.register(EntityBindingDefinition.builder(BINDING_ID, EntityBindingKind.ATTACHMENT).build());
        assertTrue(EntityBindingApi.findDefinition(BINDING_ID).isPresent());
        EntityBindingApi.unregister(BINDING_ID);
        assertFalse(EntityBindingApi.findDefinition(BINDING_ID).isPresent());
    }

    @Test
    void builderDefaultsAreCorrect() {
        EntityBindingDefinition def = EntityBindingDefinition.builder(BINDING_ID, EntityBindingKind.TETHER).build();
        assertEquals("primary", def.primaryRole());
        assertEquals("secondary", def.secondaryRole());
        assertTrue(def.durationTicks().isEmpty());
        assertEquals(EntityBindingStackingPolicy.SINGLE, def.stackingPolicy());
        assertEquals(EntityBindingSymmetry.DIRECTED, def.symmetry());
        assertTrue(def.breakConditions().isEmpty());
        assertEquals(EntityBindingCompletionMode.INSTANT, def.completionMode());
        assertEquals(EntityBindingTickPolicy.NONE, def.tickPolicy());
    }

    @Test
    void builderWithDurationAutoEnablesTickPolicy() {
        EntityBindingDefinition def = EntityBindingDefinition.builder(BINDING_ID, EntityBindingKind.LINK)
                .durationTicks(200)
                .build();
        assertTrue(def.durationTicks().isPresent());
        assertEquals(200, def.durationTicks().get());
        assertEquals(EntityBindingTickPolicy.TICK_DURATION, def.tickPolicy());
    }

    @Test
    void builderWithBreakConditions() {
        EntityBindingDefinition def = EntityBindingDefinition.builder(BINDING_ID, EntityBindingKind.CONTROL)
                .breakOn(EntityBindingBreakCondition.PRIMARY_DIES)
                .breakOn(EntityBindingBreakCondition.SECONDARY_DIES)
                .build();
        assertTrue(def.breakConditions().contains(EntityBindingBreakCondition.PRIMARY_DIES));
        assertTrue(def.breakConditions().contains(EntityBindingBreakCondition.SECONDARY_DIES));
        assertEquals(2, def.breakConditions().size());
    }

    @Test
    void entityBindingDataEmptyHasNoPrimaryOrSecondary() {
        EntityBindingData data = EntityBindingData.empty();
        assertTrue(data.primaryBindings().isEmpty());
        assertTrue(data.secondaryRefs().isEmpty());
        assertFalse(data.hasPrimaryBinding(UUID.randomUUID()));
        assertFalse(data.hasSecondaryRef(UUID.randomUUID()));
    }

    @Test
    void entityBindingDataMutationsAreImmutable() {
        UUID instanceId = UUID.randomUUID();
        EntityBindingData empty = EntityBindingData.empty();

        EntityBindingData withRef = empty.withSecondaryRef(instanceId);
        assertTrue(withRef.hasSecondaryRef(instanceId));
        assertFalse(empty.hasSecondaryRef(instanceId));

        EntityBindingData withoutRef = withRef.withoutSecondaryRef(instanceId);
        assertFalse(withoutRef.hasSecondaryRef(instanceId));
        assertTrue(withRef.hasSecondaryRef(instanceId));
    }

    @Test
    void entityBindingDataWithBindingIsImmutable() {
        UUID instanceId = UUID.randomUUID();
        EntityBindingState state = new EntityBindingState(
                instanceId, BINDING_ID,
                UUID.randomUUID(), UUID.randomUUID(),
                SOURCE_A, 0L, java.util.Optional.empty(),
                EntityBindingStatus.ACTIVE, new net.minecraft.nbt.CompoundTag(), 0
        );
        EntityBindingData empty = EntityBindingData.empty();
        EntityBindingData withBinding = empty.withBinding(state);
        assertTrue(withBinding.hasPrimaryBinding(instanceId));
        assertFalse(empty.hasPrimaryBinding(instanceId));

        EntityBindingData withoutBinding = withBinding.withoutBinding(instanceId);
        assertFalse(withoutBinding.hasPrimaryBinding(instanceId));
        assertTrue(withBinding.hasPrimaryBinding(instanceId));
    }

    @Test
    void allDefinitionsReturnsRegisteredOnes() {
        try {
            EntityBindingApi.register(EntityBindingDefinition.builder(BINDING_ID, EntityBindingKind.LINK).build());
            EntityBindingApi.register(EntityBindingDefinition.builder(BINDING_ID_2, EntityBindingKind.ATTACHMENT).build());
            assertEquals(2, EntityBindingApi.all().size());
        } finally {
            unregisterFixtures();
        }
    }

    @Test
    void findOnEmptyCacheReturnsEmpty() {
        assertTrue(EntityBindingApi.find(UUID.randomUUID()).isEmpty());
    }

    private void unregisterFixtures() {
        EntityBindingApi.unregister(BINDING_ID);
        EntityBindingApi.unregister(BINDING_ID_2);
        EntityBindingApi.unregister(STALE_BINDING_ID);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("test", path);
    }
}
