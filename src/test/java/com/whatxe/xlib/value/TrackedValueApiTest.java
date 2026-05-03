package com.whatxe.xlib.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

class TrackedValueApiTest {
    private static final ResourceLocation EVOLUTION_ID = id("value/evolution");
    private static final ResourceLocation CORRUPTION_ID = id("value/corruption");
    private static final ResourceLocation SOURCE_A = id("source/profile");
    private static final ResourceLocation SOURCE_B = id("source/form");

    @Test
    void trackedValuesUseStartingValueClampAndResolveFoodReplacementPriority() {
        unregisterFixtures();
        try {
            TrackedValueApi.register(TrackedValueDefinition.builder(EVOLUTION_ID)
                    .displayName(Component.literal("Evolution"))
                    .minValue(0.0D)
                    .maxValue(100.0D)
                    .startingValue(5.0D)
                    .tickDelta(1.25D)
                    .foodReplacementPriority(10)
                    .build());
            TrackedValueApi.register(TrackedValueDefinition.builder(CORRUPTION_ID)
                    .displayName(Component.literal("Corruption"))
                    .minValue(-20.0D)
                    .maxValue(80.0D)
                    .startingValue(0.0D)
                    .foodReplacementPriority(50)
                    .build());

            TrackedValueData emptyData = TrackedValueData.empty();
            assertEquals(5.0D, TrackedValueApi.value(emptyData, EVOLUTION_ID), 0.0001D);

            TrackedValueData tickedData = TrackedValueApi.tick(emptyData);
            assertEquals(6.25D, TrackedValueApi.value(tickedData, EVOLUTION_ID), 0.0001D);

            TrackedValueData clampedData = TrackedValueApi.sanitize(
                    tickedData.withExactAmount(EVOLUTION_ID, 500.0D)
            );
            assertEquals(100.0D, TrackedValueApi.value(clampedData, EVOLUTION_ID), 0.0001D);
            assertEquals(EVOLUTION_ID, TrackedValueApi.activeFoodReplacementValueId(clampedData).orElseThrow());

            TrackedValueData replacementData = clampedData
                    .withFoodReplacementSource(EVOLUTION_ID, SOURCE_A, true)
                    .withFoodReplacementSource(CORRUPTION_ID, SOURCE_B, true);
            assertEquals(CORRUPTION_ID, TrackedValueApi.activeFoodReplacementValueId(replacementData).orElseThrow());
            assertTrue(TrackedValueApi.sanitize(replacementData).hasFoodReplacement(CORRUPTION_ID));
        } finally {
            unregisterFixtures();
        }
    }

    @Test
    void foodReplacementLevelUsesTrackedValueRange() {
        TrackedValueDefinition definition = TrackedValueDefinition.builder(EVOLUTION_ID)
                .minValue(0.0D)
                .maxValue(200.0D)
                .foodReplacementPriority(100)
                .build();

        assertEquals(0, definition.foodReplacementLevel(0.0D));
        assertEquals(10, definition.foodReplacementLevel(100.0D));
        assertEquals(20, definition.foodReplacementLevel(200.0D));
    }

    private void unregisterFixtures() {
        TrackedValueApi.unregister(EVOLUTION_ID);
        TrackedValueApi.unregister(CORRUPTION_ID);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }
}
