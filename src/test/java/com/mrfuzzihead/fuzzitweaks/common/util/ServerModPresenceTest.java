package com.mrfuzzihead.fuzzitweaks.common.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Covers the handshake-side flag used to gate double-door client prediction. The actual FML handshake and the
 * prediction/revert path still need an in-game check.
 */
class ServerModPresenceTest {

    @BeforeEach
    @AfterEach
    void resetFlag() {
        ServerModPresence.clear();
    }

    @Test
    void detectsFuzziTweaksInTheRemoteModList() {
        ServerModPresence.onHandshakeModList(Collections.singletonMap("fuzzitweaks", "1.0.0"));
        assertTrue(ServerModPresence.hasFuzziTweaks());
    }

    @Test
    void rejectsAModListWithoutFuzziTweaks() {
        ServerModPresence.onHandshakeModList(Collections.singletonMap("NotEnoughItems", "2.8.170-GTNH"));
        assertFalse(ServerModPresence.hasFuzziTweaks());
    }

    @Test
    void handlesAMissingModList() {
        ServerModPresence.onHandshakeModList(null);
        assertFalse(ServerModPresence.hasFuzziTweaks());
    }
}
