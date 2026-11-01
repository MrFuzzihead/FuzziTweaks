package com.mrfuzzihead.fuzzitweaks.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Round-trip coverage for the packed chunk key used by the purge planner. */
class ChunkKeyTest {

    @Test
    void packsAndUnpacksPositiveAndNegativeCoordinates() {
        assertKey(0, 0);
        assertKey(12, -34);
        assertKey(-56, 78);
        assertKey(-1, -1);
        assertKey(Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    private static void assertKey(int x, int z) {
        final long key = ChunkKey.of(x, z);
        assertEquals(x, ChunkKey.x(key));
        assertEquals(z, ChunkKey.z(key));
    }
}
