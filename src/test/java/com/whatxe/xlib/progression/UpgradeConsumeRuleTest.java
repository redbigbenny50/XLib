package com.whatxe.xlib.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.whatxe.xlib.ability.AbilityData;
import com.whatxe.xlib.ability.GrantCondition;
import com.whatxe.xlib.ability.GrantConditions;
import com.whatxe.xlib.ability.AbilityRequirements;
import java.lang.reflect.Field;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class UpgradeConsumeRuleTest {
    private static final ResourceLocation RULE_ID = id("requirement_rule");
    private static final ResourceLocation MODE_ID = id("consume_mode");
    private static final ResourceLocation RESOURCE_ID = id("consume_resource");

    @Test
    void builderRequirementsAdaptAbilityRequirementsIntoConsumeConditions() throws ReflectiveOperationException {
        UpgradeConsumeRule rule = UpgradeConsumeRule.builder(RULE_ID)
                .requirement(AbilityRequirements.modeActive(MODE_ID))
                .requirements(List.of(AbilityRequirements.resourceAtLeast(RESOURCE_ID, 2)))
                .build();

        AbilityData validData = AbilityData.empty()
                .withMode(MODE_ID, true)
                .withResourceAmount(RESOURCE_ID, 2);
        List<GrantCondition> conditions = conditions(rule);

        assertEquals(2, conditions.size());
        assertTrue(GrantConditions.allMatch(null, validData, null, conditions));
        assertFalse(GrantConditions.allMatch(null, AbilityData.empty().withMode(MODE_ID, true), null, conditions));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }

    @SuppressWarnings("unchecked")
    private static List<GrantCondition> conditions(UpgradeConsumeRule rule) throws ReflectiveOperationException {
        Field field = UpgradeConsumeRule.class.getDeclaredField("conditions");
        field.setAccessible(true);
        return (List<GrantCondition>) field.get(rule);
    }
}
