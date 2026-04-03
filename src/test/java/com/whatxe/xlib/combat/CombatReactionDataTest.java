package com.whatxe.xlib.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class CombatReactionDataTest {
    @Test
    void recentHitWindowTracksAttackerDamageAndExpiry() {
        UUID attackerId = UUID.randomUUID();
        CombatReactionData data = CombatReactionData.empty().withRecentHit(100L, attackerId, 7.5F);

        assertEquals(attackerId, data.lastAttacker());
        assertEquals(7.5F, data.lastDamage());
        assertTrue(data.recentHitWithin(104L, 4));
        assertFalse(data.recentHitWithin(106L, 4));
    }
}
