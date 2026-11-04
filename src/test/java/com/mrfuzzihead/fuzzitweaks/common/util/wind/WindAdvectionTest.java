package com.mrfuzzihead.fuzzitweaks.common.util.wind;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The bounded push is the difference between "wind moves particles" and "wind either does nothing or destroys
 * every particle's own momentum". These tests pin that behaviour down without touching Minecraft.
 */
class WindAdvectionTest {

    @Test
    void slowParticleApproachesButNeverExceedsTheWind() {
        double velocity = 0.0D;
        final double wind = 0.2D;

        for (int tick = 0; tick < 1000; tick++) {
            velocity = WindAdvection.push(velocity, wind, 0.05D);
            assertTrue(velocity <= wind + 1.0E-12D, "velocity exceeded the wind at tick " + tick);
        }

        assertTrue(velocity > wind * 0.99D, "velocity should converge to the wind");
    }

    @Test
    void fastParticleIsNotSlowedByTheWind() {
        assertEquals(0.8D, WindAdvection.push(0.8D, 0.2D, 0.05D), 0.0D);
        assertEquals(-0.8D, WindAdvection.push(-0.8D, 0.2D, 0.05D), 0.0D);
        assertEquals(0.35D, WindAdvection.push(0.35D, -0.2D, 0.05D), 0.0D);
    }

    @Test
    void slowerOpposingMotionTurnsTowardTheWind() {
        double velocity = -0.05D;
        final double wind = 0.1D;

        for (int tick = 0; tick < 200; tick++) {
            velocity = WindAdvection.push(velocity, wind, 0.1D);
        }

        assertTrue(velocity > 0.0D, "opposing motion should eventually turn downwind");
        assertTrue(velocity <= wind + 1.0E-12D);
    }

    @Test
    void zeroResponseLeavesVelocityUntouched() {
        assertEquals(0.03D, WindAdvection.push(0.03D, 0.2D, 0.0D), 0.0D);
        assertEquals(0.03D, WindAdvection.push(0.03D, 0.2D, -1.0D), 0.0D);
    }

    @Test
    void responseIsClampedToOne() {
        assertEquals(0.2D, WindAdvection.push(0.0D, 0.2D, 5.0D), 1.0E-12D);
    }

    @Test
    void zeroWindDoesNotAccelerateAStillParticle() {
        assertEquals(0.0D, WindAdvection.push(0.0D, 0.0D, 0.05D), 0.0D);
    }
}
