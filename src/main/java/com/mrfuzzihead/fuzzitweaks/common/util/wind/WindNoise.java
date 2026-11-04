package com.mrfuzzihead.fuzzitweaks.common.util.wind;

/**
 * Small, deterministic 2D value noise with an analytic gradient, plus a 1D variant for the very slow climate
 * signals (prevailing direction, base speed, gust envelope).
 *
 * <p>
 * This is deliberately not Simplex/Perlin: the wind only needs smooth, non-repeating variation, and value noise
 * with a quintic fade gives a continuous first derivative at cell borders. That matters because the curl of this
 * field drives particle paths - a C1 discontinuity would show up as a visible kink in a particle's trail.
 *
 * <p>
 * The class has no mutable per-sample state apart from the seed. Gradient methods write into a caller-provided
 * array, so the particle update loop allocates nothing here.
 */
public final class WindNoise {

    private static final double HASH_TO_UNIT = 1.0D / 16777216.0D;
    private static final double VALUE_SCALE = 2.0D;

    private final int seed;

    public WindNoise(long seed) {
        this.seed = (int) (seed ^ (seed >>> 32));
    }

    /**
     * Value noise in {@code [-1, 1]}.
     */
    public double value(double x, double z) {
        return sample(x, z, null);
    }

    /**
     * Writes {@code {value, dValue/dX, dValue/dZ}} into {@code out}. The value is in {@code [-1, 1]}; the
     * gradients are in units per input unit.
     */
    public void valueAndGradient(double x, double z, double[] out) {
        sample(x, z, out);
    }

    /**
     * One-dimensional value noise in {@code [-1, 1]}, used for the slow climate channels.
     */
    public double value1D(double x) {
        final int ix = floor(x);
        final double fx = x - ix;
        final double a = corner(ix, 0);
        final double b = corner(ix + 1, 0);
        final double u = fade(fx);
        return (a + (b - a) * u) * VALUE_SCALE - 1.0D;
    }

    /**
     * Fractional Brownian motion built from {@link #valueAndGradient}. Every octave uses the same noise lattice
     * with a per-octave offset, so no extra permutation array is needed.
     *
     * <p>
     * The gradient output is the derivative with respect to the <em>unscaled</em> input coordinates. The
     * frequency multiplier is therefore applied to the gradient before it is accumulated.
     */
    public void fbmAndGradient(double x, double z, int octaves, double lacunarity, double gain, double[] out) {
        double value = 0.0D;
        double gradientX = 0.0D;
        double gradientZ = 0.0D;
        double amplitude = 1.0D;
        double frequency = 1.0D;
        double totalAmplitude = 0.0D;

        for (int i = 0; i < octaves; i++) {
            final double offset = i * 19.19D;
            valueAndGradient(x * frequency + offset, z * frequency + offset * 0.613D, out);
            value += out[0] * amplitude;
            gradientX += out[1] * amplitude * frequency;
            gradientZ += out[2] * amplitude * frequency;
            totalAmplitude += amplitude;
            amplitude *= gain;
            frequency *= lacunarity;
        }

        final double inverse = 1.0D / totalAmplitude;
        out[0] = value * inverse;
        out[1] = gradientX * inverse;
        out[2] = gradientZ * inverse;
    }

    private double sample(double x, double z, double[] out) {
        final int ix = floor(x);
        final int iz = floor(z);
        final double fx = x - ix;
        final double fz = z - iz;

        final double c00 = corner(ix, iz);
        final double c10 = corner(ix + 1, iz);
        final double c01 = corner(ix, iz + 1);
        final double c11 = corner(ix + 1, iz + 1);

        final double u = fade(fx);
        final double v = fade(fz);
        final double bottom = c00 + (c10 - c00) * u;
        final double top = c01 + (c11 - c01) * u;
        final double result = bottom + (top - bottom) * v;

        if (out != null) {
            final double du = fadeDerivative(fx);
            final double dv = fadeDerivative(fz);
            out[0] = result * VALUE_SCALE - 1.0D;
            out[1] = VALUE_SCALE * du * ((c10 - c00) * (1.0D - v) + (c11 - c01) * v);
            out[2] = VALUE_SCALE * dv * ((c01 - c00) * (1.0D - u) + (c11 - c10) * u);
        }
        return result * VALUE_SCALE - 1.0D;
    }

    /**
     * Hash a lattice corner into {@code [0, 1)}. The integer mix is the same for every call, so the same
     * coordinates and seed always produce the same wind on every JVM and every launch.
     */
    private double corner(int x, int z) {
        int hash = seed;
        hash += x * 0x27d4eb2d;
        hash = Integer.rotateLeft(hash, 13);
        hash += z * 0x85ebca6b;
        hash ^= hash >>> 15;
        hash *= 0x2c1b3c6d;
        hash ^= hash >>> 12;
        hash *= 0x297a2d39;
        hash ^= hash >>> 15;
        return (hash >>> 8) * HASH_TO_UNIT;
    }

    private static double fade(double t) {
        return t * t * t * (t * (t * 6.0D - 15.0D) + 10.0D);
    }

    private static double fadeDerivative(double t) {
        return 30.0D * t * t * (t - 1.0D) * (t - 1.0D);
    }

    private static int floor(double value) {
        final int truncated = (int) value;
        return value < truncated ? truncated - 1 : truncated;
    }
}
