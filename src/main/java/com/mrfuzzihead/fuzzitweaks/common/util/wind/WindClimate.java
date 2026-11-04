package com.mrfuzzihead.fuzzitweaks.common.util.wind;

/**
 * Slow wind climate: the prevailing direction, base speed and gust envelope that all particles share.
 *
 * <p>
 * Everything is driven by {@link WindNoise#value1D(double)} over {@code world.getTotalWorldTime()}, so the same
 * world time and dimension always produce the same wind. The climate changes over in-game days; the spatial
 * turbulence that actually curves individual trails lives in {@link WindField}.
 *
 * <p>
 * Units are Minecraft ticks and blocks: {@code maxSpeed} is blocks per tick, and {@code patternTicks} /
 * {@code gustTicks} are tick counts. One in-game day is 24,000 ticks (20 real minutes).
 */
public final class WindClimate {

    private final WindNoise noise;

    public WindClimate(long seed) {
        this.noise = new WindNoise(seed);
    }

    /**
     * Samples the climate into {@code out}. {@code baseAngleRadians} is the configured prevailing direction,
     * rotated by the slow direction noise.
     */
    public void sample(long worldTime, double baseAngleRadians, double maxSpeed, double patternTicks, double gustTicks,
        float rainStrength, boolean thundering, Sample out) {
        final double pattern = worldTime / Math.max(1.0D, patternTicks);
        final double gustPhase = worldTime / Math.max(1.0D, gustTicks);

        // Prevailing direction wanders around the configured angle. The slow term changes over a pattern
        // period; the faster term adds day-to-day variety without making the wind spin.
        double directionNoise = 0.70D * noise.value1D(pattern * 0.70D + 5.3D)
            + 0.40D * noise.value1D(pattern * 2.70D + 23.9D)
            + 0.15D * noise.value1D(gustPhase * 2.00D + 41.7D);
        directionNoise = clamp(directionNoise, -1.0D, 1.0D);
        final double angle = baseAngleRadians + Math.PI * directionNoise;
        out.directionX = Math.cos(angle);
        out.directionZ = Math.sin(angle);

        // Average speed is biased low (calm most of the time) with an occasional strong day.
        final double slowSpeed = 0.5D + 0.5D * noise.value1D(pattern * 2.30D + 11.7D);
        final double fastSpeed = 0.5D + 0.5D * noise.value1D(pattern * 7.30D + 61.3D);
        final double speedNoise = 0.70D * slowSpeed + 0.30D * fastSpeed;

        // Gusts are a separate, much faster envelope. Squaring the noise keeps most minutes light and makes
        // the strong peaks stand out, which is what "sometimes subtle, sometimes larger" needs.
        final double slowGust = 0.5D + 0.5D * noise.value1D(gustPhase + 3.1D);
        final double fastGust = 0.5D + 0.5D * noise.value1D(gustPhase * 3.70D + 9.4D);
        final double gustNoise = 0.70D * slowGust + 0.30D * fastGust;
        out.gust = 0.55D + 1.65D * gustNoise * gustNoise;

        final double weather = 1.0D + 0.5D * clamp(rainStrength, 0.0F, 1.0F) + (thundering ? 0.75D : 0.0D);
        out.speed = Math
            .min(maxSpeed * (0.10D + 0.90D * speedNoise * speedNoise) * out.gust * weather, maxSpeed * 3.0D);
    }

    private static double clamp(double value, double min, double max) {
        return value < min ? min : (value > max ? max : value);
    }

    /**
     * Reusable output of {@link #sample}. One instance per client is enough; the sample loop must not allocate.
     */
    public static final class Sample {

        /** Unit vector of the prevailing wind direction. */
        public double directionX;
        public double directionZ;

        /** Base wind speed in blocks per tick before turbulence. */
        public double speed;

        /** Gust multiplier that was applied to {@link #speed}, for diagnostics. */
        public double gust;
    }
}
