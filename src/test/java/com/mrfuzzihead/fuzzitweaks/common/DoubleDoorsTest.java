package com.mrfuzzihead.fuzzitweaks.common;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * Verifies the door-leaf offset table that drives the double-door handler. The in-world half of the feature
 * needs a running world, but the facing/hinge mapping is pure and is the part that is easy to get backwards.
 */
class DoubleDoorsTest {

    @Test
    void partnerOffsetFollowsTheDoorFacingAndHinge() {
        // Facing 0 and 2 run along Z; the hinge selects the positive or negative side.
        assertArrayEquals(new int[] { 0, 1 }, DoubleDoors.partnerOffset(0, false));
        assertArrayEquals(new int[] { 0, -1 }, DoubleDoors.partnerOffset(0, true));
        assertArrayEquals(new int[] { 0, -1 }, DoubleDoors.partnerOffset(2, false));
        assertArrayEquals(new int[] { 0, 1 }, DoubleDoors.partnerOffset(2, true));

        // Facing 1 and 3 run along X.
        assertArrayEquals(new int[] { -1, 0 }, DoubleDoors.partnerOffset(1, false));
        assertArrayEquals(new int[] { 1, 0 }, DoubleDoors.partnerOffset(1, true));
        assertArrayEquals(new int[] { 1, 0 }, DoubleDoors.partnerOffset(3, false));
        assertArrayEquals(new int[] { -1, 0 }, DoubleDoors.partnerOffset(3, true));
    }

    @Test
    void invalidFacingHasNoPartner() {
        assertNull(DoubleDoors.partnerOffset(-1, false));
        assertNull(DoubleDoors.partnerOffset(4, false));
    }
}
