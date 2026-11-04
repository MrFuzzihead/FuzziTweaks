package com.mrfuzzihead.fuzzitweaks.client.wind;

import java.lang.ref.WeakReference;

import net.minecraft.client.particle.EntityFX;
import net.minecraft.world.World;

import com.mrfuzzihead.fuzzitweaks.Config;
import com.mrfuzzihead.fuzzitweaks.common.util.wind.WindAdvection;
import com.mrfuzzihead.fuzzitweaks.common.util.wind.WindClimate;
import com.mrfuzzihead.fuzzitweaks.common.util.wind.WindDimensionFilter;
import com.mrfuzzihead.fuzzitweaks.common.util.wind.WindField;

/**
 * Client-side glue between the pure wind model and {@code EntityFX} particles.
 *
 * <p>
 * {@code EffectRenderer.updateEffects()} ticks every particle, including modded {@code EntityFX} subclasses
 * such as campfire smoke and Railcraft smoker puffs. The mixin wraps that call and hands each particle here
 * before vanilla runs its own update.
 *
 * <p>
 * The climate is refreshed once per client tick on first use, not once per particle. A particle that is
 * already moving faster than the local wind (explosions, fireworks) is left alone instead of being slowed
 * toward the wind speed; slower particles are pushed toward it. That keeps the wind from eating the motion a
 * particle was spawned with while still letting long-lived smoke and rain drift and curve.
 *
 * <p>
 * The climate and the drifting turbulence pattern run on a wind clock that advances by at most
 * {@link #MAX_ADVANCE_TICKS} per client tick. Sleeping or {@code /time} would otherwise jump the pattern by
 * thousands of ticks and visibly snap every trail in the area.
 */
public final class WindRuntime {

    private static final double TICKS_PER_SECOND = 20.0D;
    private static final long TICKS_PER_DAY = 24_000L;

    /**
     * Largest number of ticks the wind clock may advance between two updates. Ten seconds is long enough to
     * cover a normal lag spike or chunk-load stall and short enough that a sleep or time command does not jump
     * the turbulence pattern across hundreds of blocks.
     */
    private static final long MAX_ADVANCE_TICKS = 200L;

    private static final WindClimate.Sample CLIMATE = new WindClimate.Sample();
    private static final double[] SAMPLE = new double[3];

    private static WindClimate climate;
    private static WindField field;
    private static WeakReference<World> lastWorld = new WeakReference<World>(null);
    private static int lastDimension = Integer.MIN_VALUE;
    private static long lastTick = Long.MIN_VALUE;
    private static long windTime = Long.MIN_VALUE;

    private WindRuntime() {}

    /**
     * Called once per particle per client tick, before {@code EntityFX.onUpdate()}.
     */
    public static void applyTo(EntityFX particle) {
        if (!Config.enableWindParticles || particle == null) {
            return;
        }

        final World world = particle.worldObj;
        if (world == null || !world.isRemote) {
            return;
        }
        if (!isEnabledInDimension(world.provider.dimensionId)) {
            return;
        }

        final double response = Config.windParticleResponse;
        if (response <= 0.0D) {
            return;
        }

        update(world);
        field.sample(particle.posX, particle.posZ, SAMPLE);

        particle.motionX = WindAdvection.push(particle.motionX, SAMPLE[0], response);
        particle.motionZ = WindAdvection.push(particle.motionZ, SAMPLE[1], response);
        if (Config.windVerticalStrength > 0.0F) {
            particle.motionY = WindAdvection.push(particle.motionY, SAMPLE[2], response * 0.5D);
        }
    }

    /**
     * Whether the configured dimension allow/deny list permits wind in {@code dimensionId}. Public so the debug
     * command reports exactly what the particle hook uses.
     */
    public static boolean isEnabledInDimension(int dimensionId) {
        return WindDimensionFilter.allows(Config.windDimensions, Config.windDimensionListIsDenylist, dimensionId);
    }

    /**
     * Captures the current climate and the local wind at a position for the debug command. This is not used by
     * the particle path; it is deliberately allowed to allocate.
     */
    public static DebugInfo debug(World world, double x, double z) {
        final DebugInfo info = new DebugInfo();
        info.masterEnabled = Config.enableWindParticles;
        info.dimensionId = world.provider.dimensionId;
        info.denylist = Config.windDimensionListIsDenylist;
        info.dimensionIds = Config.windDimensions == null ? new int[0] : Config.windDimensions.clone();
        info.enabledInDimension = isEnabledInDimension(info.dimensionId);

        if (!info.masterEnabled || !info.enabledInDimension) {
            return info;
        }

        update(world);
        info.worldTime = world.getTotalWorldTime();
        info.windTime = windTime;
        info.gust = CLIMATE.gust;
        info.baseSpeedBps = CLIMATE.speed * TICKS_PER_SECOND;
        info.baseDirectionDegrees = Math.toDegrees(Math.atan2(CLIMATE.directionZ, CLIMATE.directionX));

        field.sample(x, z, SAMPLE);
        info.localSpeedBps = Math.hypot(SAMPLE[0], SAMPLE[1]) * TICKS_PER_SECOND;
        info.localDirectionDegrees = Math.toDegrees(Math.atan2(SAMPLE[1], SAMPLE[0]));
        info.verticalBps = SAMPLE[2] * TICKS_PER_SECOND;
        return info;
    }

    private static void update(World world) {
        final long now = world.getTotalWorldTime();
        final int dimension = world.provider.dimensionId;

        if (world != lastWorld.get() || dimension != lastDimension) {
            final long seed = seedForDimension(dimension);
            climate = new WindClimate(seed);
            field = new WindField(seed);
            lastWorld = new WeakReference<World>(world);
            lastDimension = dimension;
            lastTick = Long.MIN_VALUE;
            windTime = Long.MIN_VALUE;
        }

        if (now == lastTick) {
            return;
        }

        final long elapsed = lastTick == Long.MIN_VALUE ? 1L
            : Math.min(MAX_ADVANCE_TICKS, Math.max(1L, now - lastTick));
        lastTick = now;
        if (windTime == Long.MIN_VALUE) {
            windTime = now;
        } else {
            windTime += elapsed;
        }

        climate.sample(
            windTime,
            Math.toRadians(Config.windPrevailingDirectionDegrees),
            Config.windMaxSpeed / TICKS_PER_SECOND,
            Config.windPatternDays * TICKS_PER_DAY,
            Config.windGustSeconds * TICKS_PER_SECOND,
            world.getRainStrength(1.0F),
            world.isThundering(),
            CLIMATE);

        field.setBase(CLIMATE.directionX * CLIMATE.speed, CLIMATE.directionZ * CLIMATE.speed);
        field.setTurbulence(
            Config.windTurbulence / TICKS_PER_SECOND,
            Config.windVerticalStrength / TICKS_PER_SECOND,
            Config.windTurbulenceScale);
        field.advance(elapsed);
    }

    /**
     * Dimension-specific seed, so each dimension gets its own wind pattern while the pattern stays stable for
     * the same dimension and wind clock. No server seed or packet is needed; this is pure ambience.
     */
    private static long seedForDimension(int dimensionId) {
        long seed = 0x9E3779B97F4A7C15L ^ (long) dimensionId * 0xBF58476D1CE4E5B9L;
        seed ^= 0x5DEECE66DL;
        seed *= 0x2545F4914F6CDD1DL;
        return seed ^ (seed >>> 29);
    }

    /** Snapshot for the {@code /fuzzitweaks wind} command. */
    public static final class DebugInfo {

        public boolean masterEnabled;
        public boolean enabledInDimension;
        public boolean denylist;
        public int[] dimensionIds = new int[0];
        public int dimensionId;

        public long worldTime;
        public long windTime;

        /** Gust multiplier applied to the base climate speed. */
        public double gust;

        /** Base climate wind speed in blocks per second. */
        public double baseSpeedBps;
        public double baseDirectionDegrees;

        /** Local wind at the queried position, base plus curl, in blocks per second. */
        public double localSpeedBps;
        public double localDirectionDegrees;
        public double verticalBps;
    }
}
