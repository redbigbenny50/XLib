package com.whatxe.xlib.cue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.whatxe.xlib.ability.AbilityData;
import com.whatxe.xlib.ability.AbilityDefinition;
import com.whatxe.xlib.ability.AbilityEndReason;
import com.whatxe.xlib.ability.AbilityIcon;
import com.whatxe.xlib.ability.AbilityUseResult;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class XLibRuntimeCueTest {
    private static final ResourceLocation ABILITY_ID = id("charge_release");

    @Test
    void cueFactoriesPreserveProgressAndReasonMetadata() {
        XLibRuntimeCue progressCue = XLibRuntimeCue.chargeProgress(ABILITY_ID, 12, 40);
        XLibRuntimeCue releaseCue = XLibRuntimeCue.release(ABILITY_ID, AbilityEndReason.DURATION_EXPIRED, 40, 40);
        XLibRuntimeCue stateExitCue = XLibRuntimeCue.stateExit(ABILITY_ID, AbilityEndReason.REPLACED_BY_EXCLUSIVE);

        assertEquals(XLibRuntimeCueType.CHARGE_PROGRESS, progressCue.type());
        assertEquals(12, progressCue.progress());
        assertEquals(40, progressCue.maxProgress());
        assertTrue(progressCue.hasProgress());

        assertEquals(XLibRuntimeCueType.RELEASE, releaseCue.type());
        assertEquals(ABILITY_ID, releaseCue.abilityId());
        assertEquals(AbilityEndReason.DURATION_EXPIRED, releaseCue.endReason());

        assertEquals(XLibRuntimeCueType.STATE_EXIT, stateExitCue.type());
        assertEquals(ABILITY_ID, stateExitCue.stateId());
        assertEquals(AbilityEndReason.REPLACED_BY_EXCLUSIVE, stateExitCue.endReason());
        assertFalse(stateExitCue.hasProgress());
    }

    @Test
    void chargeReleaseAbilitiesExposeResolvedChargeProgress() {
        AbilityDefinition definition = AbilityDefinition.builder(ABILITY_ID, AbilityIcon.ofTexture(id("charge_release_icon")))
                .chargeRelease(40, (player, data, reason, chargedTicks, maxChargeTicks) -> AbilityUseResult.success(data))
                .build();

        AbilityData partiallyCharged = AbilityData.empty().withActiveDuration(ABILITY_ID, 25);

        assertTrue(definition.isChargeReleaseAbility());
        assertEquals(40, definition.chargeReleaseMaxTicks());
        assertEquals(15, definition.resolvedChargeReleaseTicks(partiallyCharged, null));
        assertEquals(40, definition.resolvedChargeReleaseTicks(partiallyCharged, AbilityEndReason.DURATION_EXPIRED));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }
}
