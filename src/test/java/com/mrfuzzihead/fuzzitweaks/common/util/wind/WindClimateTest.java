package com.mrfuzzihead.fuzzitweaks.common.util.wind;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Pins down the climate behaviour the particle hook relies on: a unit direction, a bounded speed, quiet and
 * gusty periods, and a storm multiplier. Everything is deterministic from world time.
 */
class WindClimateTest {

    private static final long DAY = 24_000L;
    private static final double MAX_SPEED = 0.1D; // blocks per tick, deliberately high for the bounds test
    private static final double PATTERN_TICKS = 1.5D * DAY;
    private static final double GUST_TICKS = 45.0D * 20.0D;

    @Test
    void directionIsUnitLengthAndSpeedIsBounded() {
        final WindClimate climate = new WindClimate(123L);
        final WindClimate.Sample sample = new WindClimate.Sample();

        for (long time = 0; time < 10L * DAY; time += 137L) {
            climate.sample(time, Math.toRadians(35.0D), MAX_SPEED, PATTERN_TICKS, GUST_TICKS, 0.0F, false, sample);

            assertEquals(1.0D, Math.hypot(sample.directionX, sample.directionZ), 1.0E-9D);
            assertTrue(sample.speed >= 0.0D, "negative wind speed at " + time);
            assertTrue(sample.speed <= MAX_SPEED * 3.0D + 1.0E-12D, "wind speed above the hard cap at " + time);
            assertTrue(sample.gust >= 0.55D && sample.gust <= 2.21D, "gust outside its envelope at " + time);
        }
    }

    @Test
    void climateHasQuietAndStrongPeriods() {
        final WindClimate climate = new WindClimate(456L);
        final WindClimate.Sample sample = new WindClimate.Sample();

        double min = Double.MAX_VALUE;
        double max = 0.0D;
        for (long time = 0; time < 30L * DAY; time += 50L) {
            climate.sample(time, 0.0D, MAX_SPEED, PATTERN_TICKS, GUST_TICKS, 0.0F, false, sample);
            min = Math.min(min, sample.speed);
            max = Math.max(max, sample.speed);
        }

        assertTrue(min < MAX_SPEED * 0.25D, "no quiet wind periods; min=" + min);
        assertTrue(max > MAX_SPEED, "no gust above the base speed; max=" + max);
    }

    @Test
    void rainAndThunderIncreaseTheAverageSpeed() {
        final WindClimate climate = new WindClimate(789L);
        final WindClimate.Sample sample = new WindClimate.Sample();

        double clearTotal = 0.0D;
        double stormTotal = 0.0D;
        int count = 0;
        for (long time = 0; time < 5L * DAY; time += 250L) {
            climate.sample(time, 0.0D, MAX_SPEED, PATTERN_TICKS, GUST_TICKS, 0.0F, false, sample);
            clearTotal += sample.speed;
            climate.sample(time, 0.0D, MAX_SPEED, PATTERN_TICKS, GUST_TICKS, 1.0F, true, sample);
            stormTotal += sample.speed;
            count++;
        }

        assertTrue(stormTotal > clearTotal * 1.5D, "storm wind should clearly exceed clear-weather wind");
        assertTrue(count > 0);
    }

    @Test
    void sameTimeAndSeedGiveTheSameSample() {
        final WindClimate a = new WindClimate(0xABCDEFL);
        final WindClimate b = new WindClimate(0xABCDEFL);
        final WindClimate.Sample first = new WindClimate.Sample();
        final WindClimate.Sample second = new WindClimate.Sample();

        for (long time = 0; time < DAY; time += 1000L) {
            a.sample(time, 1.2D, MAX_SPEED, PATTERN_TICKS, GUST_TICKS, 0.3F, false, first);
            b.sample(time, 1.2D, MAX_SPEED, PATTERN_TICKS, GUST_TICKS, 0.3F, false, second);
            assertEquals(first.directionX, second.directionX, 0.0D);
            assertEquals(first.directionZ, second.directionZ, 0.0D);
            assertEquals(first.speed, second.speed, 0.0D);
            assertEquals(first.gust, second.gust, 0.0D);
        }
    }

    @Test
    void differentSeedsGiveDifferentWind() {
        final WindClimate a = new WindClimate(1L);
        final WindClimate b = new WindClimate(2L);
        final WindClimate.Sample first = new WindClimate.Sample();
        final WindClimate.Sample second = new WindClimate.Sample();

        a.sample(DAY, 0.0D, MAX_SPEED, PATTERN_TICKS, GUST_TICKS, 0.0F, false, first);
        b.sample(DAY, 0.0D, MAX_SPEED, PATTERN_TICKS, GUST_TICKS, 0.0F, false, second);

        assertNotEquals(first.directionX, second.directionX);
        assertNotEquals(first.speed, second.speed);
    }

    @Test
    void directionWandersButStaysUnitLength() {
        final WindClimate climate = new WindClimate(321L);
        final WindClimate.Sample sample = new WindClimate.Sample();
        double minAngle = Double.MAX_VALUE;
        double maxAngle = -Double.MAX_VALUE;
        int samples = 0;

        for (long time = 0; time < 10L * DAY; time += 333L) {
            climate.sample(time, 0.0D, MAX_SPEED, PATTERN_TICKS, GUST_TICKS, 0.0F, false, sample);
            final double angle = Math.atan2(sample.directionZ, sample.directionX);
            minAngle = Math.min(minAngle, angle);
            maxAngle = Math.max(maxAngle, angle);
            samples++;
        }

        assertTrue(samples > 0);
        assertTrue(maxAngle - minAngle > 0.25D, "prevailing direction should not be fixed");
    }
}
