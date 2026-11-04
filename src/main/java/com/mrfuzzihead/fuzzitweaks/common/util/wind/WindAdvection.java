package com.mrfuzzihead.fuzzitweaks.common.util.wind;

/**
 * Bounded wind push for one velocity component.
 *
 * <p>
 * The wind is not a force that is added on top of everything: a particle that already moves faster than the
 * local wind (an explosion, a firework spark) keeps its momentum, while a slower particle is eased toward the
 * wind speed and stops there. That avoids both extremes - wind that never moves anything and wind that kills
 * the motion a particle was spawned with - and it cannot overshoot or accumulate without bound.
 */
public final class WindAdvection {

    private WindAdvection() {}

    /**
     * @param velocity current velocity component, any unit
     * @param wind     desired velocity component, same unit
     * @param response fraction of the remaining difference to apply, clamped to {@code [0, 1]}
     * @return the adjusted velocity component
     */
    public static double push(double velocity, double wind, double response) {
        if (response <= 0.0D) {
            return velocity;
        }
        if (Math.abs(velocity) >= Math.abs(wind)) {
            return velocity;
        }
        final double clampedResponse = response > 1.0D ? 1.0D : response;
        return velocity + (wind - velocity) * clampedResponse;
    }
}
