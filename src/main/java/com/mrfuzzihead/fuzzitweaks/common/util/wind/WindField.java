package com.mrfuzzihead.fuzzitweaks.common.util.wind;

/**
 * Spatial wind field: adds a divergence-free-ish curl field on top of the climate's base wind.
 *
 * <p>
 * The curl is taken from an fBm scalar potential {@code psi(x, z)}:
 *
 * <pre>
 *     windX =  baseX + turbulence * dpsi/dz
 *     windZ =  baseZ - turbulence * dpsi/dx
 * </pre>
 *
 * That is what turns a straight diagonal drift into a curve: as a particle moves, the local direction of the
 * field changes, and the particle's existing velocity makes it follow the change with some inertia instead of
 * snapping to a new heading. A separate, much weaker vertical noise adds gentle updrafts and downdrafts.
 *
 * <p>
 * The field pattern drifts slowly downwind. Without that, a particle travelling exactly with the base wind
 * would see a frozen turbulence field; with it, the eddies move with the air mass.
 *
 * <p>
 * Not thread-safe: one instance is used from the client thread.
 */
public final class WindField {

    private static final int OCTAVES = 4;
    private static final double LACUNARITY = 2.0D;
    private static final double GAIN = 0.5D;
    private static final double DRIFT_FACTOR = 0.35D;

    private final WindNoise noise;
    private final double[] gradient = new double[3];

    private double baseX;
    private double baseZ;
    private double turbulence;
    private double vertical;
    private double scale = 40.0D;
    private double driftX;
    private double driftZ;

    public WindField(long seed) {
        this.noise = new WindNoise(seed);
    }

    /** Base wind in blocks per tick. */
    public void setBase(double x, double z) {
        this.baseX = x;
        this.baseZ = z;
    }

    /**
     * @param horizontalTurbulence curl amplitude in blocks per tick
     * @param verticalTurbulence   vertical noise amplitude in blocks per tick
     * @param turbulenceScale      size of the largest turbulence cell in blocks
     */
    public void setTurbulence(double horizontalTurbulence, double verticalTurbulence, double turbulenceScale) {
        this.turbulence = Math.max(0.0D, horizontalTurbulence);
        this.vertical = Math.max(0.0D, verticalTurbulence);
        this.scale = Math.max(1.0D, turbulenceScale);
    }

    /**
     * Advances the drifting turbulence pattern. {@code ticks} is the elapsed client ticks since the previous
     * call; clamping is the caller's job.
     */
    public void advance(double ticks) {
        if (ticks <= 0.0D) {
            return;
        }
        driftX += baseX * DRIFT_FACTOR * ticks;
        driftZ += baseZ * DRIFT_FACTOR * ticks;
    }

    /**
     * Writes {@code {windX, windZ, windY}} in blocks per tick into {@code out}.
     */
    public void sample(double x, double z, double[] out) {
        final double u = (x + driftX) / scale;
        final double v = (z + driftZ) / scale;
        noise.fbmAndGradient(u, v, OCTAVES, LACUNARITY, GAIN, gradient);

        out[0] = baseX + turbulence * gradient[2];
        out[1] = baseZ - turbulence * gradient[1];

        if (vertical <= 0.0D) {
            out[2] = 0.0D;
        } else {
            final double verticalScale = scale * 1.7D;
            out[2] = vertical
                * noise.value((x + driftX * 1.3D) / verticalScale + 17.0D, (z + driftZ * 1.3D) / verticalScale - 9.0D);
        }
    }
}
