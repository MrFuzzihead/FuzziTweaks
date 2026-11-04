package com.mrfuzzihead.fuzzitweaks.common.util.wind;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Validates the spatial field and, most importantly, that the curl actually bends a particle path. A constant
 * base wind by itself would produce the straight diagonal trails the feature is trying to avoid.
 */
class WindFieldTest {

    @Test
    void zeroTurbulenceAndVerticalIsPureBaseWind() {
        final WindField field = new WindField(11L);
        field.setBase(0.12D, -0.04D);
        field.setTurbulence(0.0D, 0.0D, 40.0D);
        field.advance(25.0D);

        final double[] sample = new double[3];
        for (int i = 0; i < 100; i++) {
            field.sample(i * 7.3D - 300.0D, i * -11.9D + 500.0D, sample);
            assertEquals(0.12D, sample[0], 0.0D);
            assertEquals(-0.04D, sample[1], 0.0D);
            assertEquals(0.0D, sample[2], 0.0D);
        }
    }

    @Test
    void turbulenceVariesOverSpace() {
        final WindField field = new WindField(22L);
        field.setBase(0.05D, 0.02D);
        field.setTurbulence(0.10D, 0.0D, 24.0D);

        final double[] sample = new double[3];
        double maxDeviation = 0.0D;
        for (int i = 0; i < 200; i++) {
            field.sample(i * 13.0D, i * -7.0D, sample);
            maxDeviation = Math.max(maxDeviation, Math.abs(sample[0] - 0.05D));
            maxDeviation = Math.max(maxDeviation, Math.abs(sample[1] - 0.02D));
        }

        assertTrue(maxDeviation > 0.02D, "curl field should vary by more than noise-level amounts");
    }

    @Test
    void fieldIsDeterministic() {
        final WindField first = new WindField(33L);
        final WindField second = new WindField(33L);
        first.setBase(0.08D, 0.03D);
        second.setBase(0.08D, 0.03D);
        first.setTurbulence(0.07D, 0.01D, 32.0D);
        second.setTurbulence(0.07D, 0.01D, 32.0D);

        final double[] a = new double[3];
        final double[] b = new double[3];
        for (int i = 0; i < 100; i++) {
            first.advance(1.0D);
            second.advance(1.0D);
            first.sample(i * 5.5D, i * 9.1D, a);
            second.sample(i * 5.5D, i * 9.1D, b);
            assertEquals(a[0], b[0], 0.0D);
            assertEquals(a[1], b[1], 0.0D);
            assertEquals(a[2], b[2], 0.0D);
        }
    }

    @Test
    void fieldStaysFiniteAndSane() {
        final WindField field = new WindField(44L);
        field.setBase(0.2D, -0.1D);
        field.setTurbulence(0.1D, 0.02D, 40.0D);

        final double[] sample = new double[3];
        for (int z = -50; z <= 50; z++) {
            for (int x = -50; x <= 50; x++) {
                field.sample(x * 17.0D, z * 19.0D, sample);
                assertTrue(Double.isFinite(sample[0]) && Double.isFinite(sample[1]) && Double.isFinite(sample[2]));
                assertTrue(Math.hypot(sample[0], sample[1]) < 2.0D, "wind speed should stay in a sane range");
                assertTrue(Math.abs(sample[2]) <= 0.02D + 1.0E-12D, "vertical wind should be gentle");
            }
        }
    }

    @Test
    void driftingPatternChangesTheFieldAtAFixedPosition() {
        final WindField field = new WindField(55L);
        field.setBase(0.1D, -0.05D);
        field.setTurbulence(0.1D, 0.0D, 30.0D);

        final double[] before = new double[3];
        final double[] after = new double[3];
        field.sample(123.0D, -45.0D, before);
        field.advance(1000.0D);
        field.sample(123.0D, -45.0D, after);

        assertTrue(
            Math.abs(after[0] - before[0]) + Math.abs(after[1] - before[1]) > 1.0E-6D,
            "the turbulence pattern should drift downwind");
    }

    @Test
    void curlBendsATrajectory() {
        final WindField field = new WindField(0xC0FFEEL);
        field.setBase(0.08D, 0.0D);
        field.setTurbulence(0.15D, 0.0D, 24.0D);

        final double[] wind = new double[3];
        double x = 0.0D;
        double z = 0.0D;
        double vx = 0.08D;
        double vz = 0.0D;
        double previousHeading = Math.atan2(vz, vx);
        double totalHeadingChange = 0.0D;

        for (int tick = 0; tick < 600; tick++) {
            field.advance(1.0D);
            field.sample(x, z, wind);
            vx += (wind[0] - vx) * 0.12D;
            vz += (wind[1] - vz) * 0.12D;
            x += vx;
            z += vz;

            if (Math.hypot(vx, vz) > 0.01D) {
                final double heading = Math.atan2(vz, vx);
                totalHeadingChange += Math.abs(wrap(heading - previousHeading));
                previousHeading = heading;
            }
        }

        assertTrue(totalHeadingChange > 0.5D, "trajectory should curve, total heading change=" + totalHeadingChange);
        assertTrue(Math.abs(x) > 1.0D && Math.abs(z) > 1.0D, "particle should actually travel");
    }

    private static double wrap(double radians) {
        while (radians > Math.PI) {
            radians -= Math.PI * 2.0D;
        }
        while (radians < -Math.PI) {
            radians += Math.PI * 2.0D;
        }
        return radians;
    }
}
