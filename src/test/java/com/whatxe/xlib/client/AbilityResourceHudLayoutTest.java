package com.whatxe.xlib.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class AbilityResourceHudLayoutTest {
    @Test
    void builderSupportsOffsetsAndTextVisibilityFlags() {
        AbilityResourceHudLayout layout = AbilityResourceHudLayout.builder()
                .anchor(AbilityResourceHudAnchor.TOP_RIGHT)
                .orientation(AbilityResourceHudOrientation.HORIZONTAL)
                .width(120)
                .height(12)
                .spacing(5)
                .priority(3)
                .offsetX(9)
                .offsetY(-6)
                .showName(false)
                .showValue(false)
                .build();

        assertEquals(AbilityResourceHudAnchor.TOP_RIGHT, layout.anchor());
        assertEquals(AbilityResourceHudOrientation.HORIZONTAL, layout.orientation());
        assertEquals(120, layout.width());
        assertEquals(12, layout.height());
        assertEquals(5, layout.spacing());
        assertEquals(3, layout.priority());
        assertEquals(9, layout.offsetX());
        assertEquals(-6, layout.offsetY());
        assertFalse(layout.showName());
        assertFalse(layout.showValue());
    }

    @Test
    void registryStoresBuiltInLayoutOverrides() {
        ResourceLocation resourceId = id("focus");
        AbilityResourceHudLayout layout = AbilityResourceHudLayout.builder()
                .anchor(AbilityResourceHudAnchor.ABOVE_HOTBAR_CENTER)
                .offsetX(12)
                .offsetY(4)
                .showName(false)
                .showValue(true)
                .build();

        AbilityResourceHudRegistry.register(resourceId, layout);

        AbilityResourceHudRegistration registration = AbilityResourceHudRegistry.find(resourceId).orElseThrow();
        assertNull(registration.renderer());
        assertEquals(layout, registration.layout());
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }
}
