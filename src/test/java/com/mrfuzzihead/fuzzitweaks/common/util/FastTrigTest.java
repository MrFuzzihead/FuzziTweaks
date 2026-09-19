package com.mrfuzzihead.fuzzitweaks.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Validates the ported {@link FastTrig} lookup table against {@link Math#atan2(double, double)}.
 *
 * <p>
 * FastTrig is plain JVM math, so this test needs no Minecraft classes. The error bound (0.02 rad,
 * roughly 1.15 degrees) is the promised accuracy of the 256x256 table and is far tighter than what head
 * rotation could ever show.
 */
class FastTrigTest {

    private static final double MAX_ERROR_RADIANS = 0.02D;
    private static final double TWO_PI = Math.PI * 2.0D;

    @Test
    void matchesMathAtan2OnDenseGrid() {
        for (int yStep = -200; yStep <= 200; yStep++) {
            for (int xStep = -200; xStep <= 200; xStep++) {
                assertClose(yStep / 10.0D, xStep / 10.0D);
            }
        }
    }

    @Test
    void matchesMathAtan2OnAxisAndQuadrantBoundaries() {
        final double[] values = { 0.0D, 1.0D, -1.0D, 3.0D, -3.0D };
        for (final double y : values) {
            for (final double x : values) {
                assertClose(y, x);
            }
        }
    }

    @Test
    void matchesMathAtan2AcrossMagnitudes() {
        final double[] magnitudes = { 1.0E-6D, 1.0D, 1.0E6D };
        final double[] slopes = { 0.0D, 0.001D, 0.25D, 1.0D, 2.0D, 1000.0D };

        for (final double magnitude : magnitudes) {
            for (final double slope : slopes) {
                assertClose(slope * magnitude, magnitude);
                assertClose(slope * magnitude, -magnitude);
                assertClose(-slope * magnitude, magnitude);
                assertClose(-slope * magnitude, -magnitude);
            }
        }
    }

    @Test
    void handlesZeroVector() {
        // 1 / 0 -> +Inf, (int) NaN -> 0, so the table entry for (0, 0) is returned.
        assertEquals(0.0F, FastTrig.atan2(0.0D, 0.0D), 0.0F);
    }

    @Test
    void collapsesNegativeZeroYOntoPositiveY() {
        // Faithful to the ported algorithm: -0.0 is not caught by the "y < 0" test, so the result is
        // +PI rather than Math.atan2's -PI. Same angle modulo 2*PI, and the look helper wraps the
        // resulting yaw with MathHelper.wrapAngleTo180_float, so nothing observable changes.
        assertEquals((float) Math.PI, FastTrig.atan2(-0.0D, -1.0D), 1.0E-6F);
        assertEquals((float) Math.PI, FastTrig.atan2(0.0D, -1.0D), 1.0E-6F);
    }

    /**
     * Compares on the circle: an angle of {@code PI + 0.001} and one of {@code -PI + 0.001} are the same
     * direction, which is all the look helper (via {@code MathHelper.wrapAngleTo180_float}) cares about.
     */
    private static void assertClose(double y, double x) {
        final double exact = Math.atan2(y, x);
        final double actual = FastTrig.atan2(y, x);

        double error = Math.abs(exact - actual) % TWO_PI;
        if (error > Math.PI) {
            error = TWO_PI - error;
        }

        final double angularError = error;
        assertTrue(
            angularError < MAX_ERROR_RADIANS,
            () -> "FastTrig.atan2(" + y
                + ", "
                + x
                + ") = "
                + actual
                + " differs from Math.atan2 = "
                + exact
                + " by "
                + angularError
                + " rad (limit "
                + MAX_ERROR_RADIANS
                + ")");
    }
}
