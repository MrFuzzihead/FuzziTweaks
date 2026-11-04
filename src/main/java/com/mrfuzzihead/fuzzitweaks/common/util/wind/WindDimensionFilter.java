package com.mrfuzzihead.fuzzitweaks.common.util.wind;

/**
 * Per-dimension allow/deny rule for the wind simulation.
 *
 * <p>
 * The semantics match the Distant Horizons dimension filter: an empty list denies every dimension in
 * allowlist mode and allows every dimension in denylist mode. The default is an allowlist containing only
 * the Overworld (dimension 0).
 */
public final class WindDimensionFilter {

    private WindDimensionFilter() {}

    /**
     * @param dimensions  configured dimension IDs, may be {@code null}
     * @param denylist    {@code true} to treat {@code dimensions} as a denylist, {@code false} as an allowlist
     * @param dimensionId dimension to test
     * @return whether wind should run in {@code dimensionId}
     */
    public static boolean allows(int[] dimensions, boolean denylist, int dimensionId) {
        boolean listed = false;
        if (dimensions != null) {
            for (int id : dimensions) {
                if (id == dimensionId) {
                    listed = true;
                    break;
                }
            }
        }
        return denylist ? !listed : listed;
    }
}
