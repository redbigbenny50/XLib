package com.whatxe.xlib.network;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ProtocolVersionTest {
    @Test
    void protocolCompatibilityRequiresMatchingSeriesAndNonFutureRevision() {
        ModPayloads.ProtocolVersion current = new ModPayloads.ProtocolVersion(1, 5);

        assertTrue(current.compatibleWith(new ModPayloads.ProtocolVersion(1, 4)));
        assertTrue(current.compatibleWith(new ModPayloads.ProtocolVersion(1, 5)));
        assertFalse(current.compatibleWith(new ModPayloads.ProtocolVersion(1, 6)));
        assertFalse(current.compatibleWith(new ModPayloads.ProtocolVersion(2, 1)));
    }
}
