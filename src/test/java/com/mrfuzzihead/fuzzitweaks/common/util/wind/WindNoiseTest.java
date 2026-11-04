package com.mrfuzzihead.fuzzitweaks.common.util.wind;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The wind model is pure JVM math, so these tests run without Minecraft. They pin down the two properties the
 * particle code depends on: the field must be deterministic, and the analytic gradient must actually be the
 * derivative of the value (the curl is taken from that gradient).
 */
class WindNoiseTest {

    private static final double EPSILON = 1.0E-4D;

    @Test
    void valueStaysInTheDocumentedRange() {
        final WindNoise noise = new WindNoise(1234L);
        for (int z = -100; z <= 100; z += 7) {
            for (int x = -100; x <= 100; x += 7) {
                final double value = noise.value(x / 3.7D, z / 5.1D);
                assertTrue(value >= -1.0D && value < 1.0D, "value outside [-1, 1): " + value);
            }
        }
    }

    @Test
    void oneDimensionalValueStaysInRange() {
        final WindNoise noise = new WindNoise(99L);
        for (int i = -1000; i <= 1000; i++) {
            final double value = noise.value1D(i / 13.0D);
            assertTrue(value >= -1.0D && value < 1.0D, "1D value outside [-1, 1): " + value);
        }
    }

    @Test
    void analyticGradientMatchesFiniteDifference() {
        final WindNoise noise = new WindNoise(0x5EEDL);
        final double[] gradient = new double[3];

        for (int z = -5; z <= 5; z++) {
            for (int x = -5; x <= 5; x++) {
                final double px = x * 1.37D + 0.23D;
                final double pz = z * 2.11D + 0.71D;
                noise.valueAndGradient(px, pz, gradient);

                final double dx = (noise.value(px + EPSILON, pz) - noise.value(px - EPSILON, pz)) / (2.0D * EPSILON);
                final double dz = (noise.value(px, pz + EPSILON) - noise.value(px, pz - EPSILON)) / (2.0D * EPSILON);

                assertEquals(dx, gradient[1], 1.0E-5D, "d/dx at " + px + ", " + pz);
                assertEquals(dz, gradient[2], 1.0E-5D, "d/dz at " + px + ", " + pz);
            }
        }
    }

    @Test
    void fbmGradientMatchesFiniteDifference() {
        final WindNoise noise = new WindNoise(777L);
        final double[] gradient = new double[3];

        for (int i = 0; i < 20; i++) {
            final double px = i * 0.91D - 4.3D;
            final double pz = i * -1.73D + 8.7D;
            final double nx = px / 17.0D;
            final double nz = pz / 23.0D;
            noise.fbmAndGradient(nx, nz, 4, 2.0D, 0.5D, gradient);

            final double dx = (fbmValue(noise, nx + EPSILON, nz) - fbmValue(noise, nx - EPSILON, nz))
                / (2.0D * EPSILON);
            final double dz = (fbmValue(noise, nx, nz + EPSILON) - fbmValue(noise, nx, nz - EPSILON))
                / (2.0D * EPSILON);

            assertEquals(dx, gradient[1], 1.0E-4D, "fbm d/dx");
            assertEquals(dz, gradient[2], 1.0E-4D, "fbm d/dz");
        }
    }

    @Test
    void sameSeedAndCoordinatesGiveTheSameField() {
        final WindNoise a = new WindNoise(42L);
        final WindNoise b = new WindNoise(42L);
        final double[] gradientA = new double[3];
        final double[] gradientB = new double[3];

        for (int i = 0; i < 100; i++) {
            final double x = i * 3.3D;
            final double z = i * -1.7D;
            assertEquals(a.value(x, z), b.value(x, z), 0.0D);
            a.valueAndGradient(x, z, gradientA);
            b.valueAndGradient(x, z, gradientB);
            assertEquals(gradientA[0], gradientB[0], 0.0D);
            assertEquals(gradientA[1], gradientB[1], 0.0D);
            assertEquals(gradientA[2], gradientB[2], 0.0D);
        }
    }

    @Test
    void differentSeedsGiveDifferentPatterns() {
        final WindNoise a = new WindNoise(1L);
        final WindNoise b = new WindNoise(2L);
        assertNotEquals(a.value(3.7D, 9.1D), b.value(3.7D, 9.1D), "seeds must not collapse to one pattern");
    }

    @Test
    void largeCoordinatesStayFinite() {
        final WindNoise noise = new WindNoise(5L);
        final double[] gradient = new double[3];
        noise.valueAndGradient(1.0E7D, -1.0E7D, gradient);
        for (double component : gradient) {
            assertTrue(Double.isFinite(component));
        }
    }

    private static double fbmValue(WindNoise noise, double x, double z) {
        final double[] scratch = new double[3];
        noise.fbmAndGradient(x, z, 4, 2.0D, 0.5D, scratch);
        return scratch[0];
    }
}
