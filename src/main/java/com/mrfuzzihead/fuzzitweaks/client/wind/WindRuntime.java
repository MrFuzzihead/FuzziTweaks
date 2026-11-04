package com.mrfuzzihead.fuzzitweaks.client.wind;

import java.lang.ref.WeakReference;

import net.minecraft.client.particle.EntityFX;
import net.minecraft.world.World;

import com.mrfuzzihead.fuzzitweaks.Config;
import com.mrfuzzihead.fuzzitweaks.common.util.wind.WindAdvection;
import com.mrfuzzihead.fuzzitweaks.common.util.wind.WindClimate;
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
 */
public final class WindRuntime {

    private static final double TICKS_PER_SECOND = 20.0D;
    private static final long TICKS_PER_DAY = 24_000L;

    private static final WindClimate.Sample CLIMATE = new WindClimate.Sample();
    private static final double[] SAMPLE = new double[3];

    private static WindClimate climate;
    private static WindField field;
    private static WeakReference<World> lastWorld = new WeakReference<World>(null);
    private static int lastDimension = Integer.MIN_VALUE;
    private static long lastTick = Long.MIN_VALUE;

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
        }

        if (now == lastTick) {
            return;
        }

        final long elapsed = lastTick == Long.MIN_VALUE ? 1L : Math.max(1L, now - lastTick);
        lastTick = now;

        climate.sample(
            now,
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
     * the same dimension and world time. No server seed or packet is needed; this is pure ambience.
     */
    private static long seedForDimension(int dimensionId) {
        long seed = 0x9E3779B97F4A7C15L ^ (long) dimensionId * 0xBF58476D1CE4E5B9L;
        seed ^= 0x5DEECE66DL;
        seed *= 0x2545F4914F6CDD1DL;
        return seed ^ (seed >>> 29);
    }
}
