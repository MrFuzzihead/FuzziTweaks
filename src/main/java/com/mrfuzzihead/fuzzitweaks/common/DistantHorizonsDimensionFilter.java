package com.mrfuzzihead.fuzzitweaks.common;

import com.mrfuzzihead.fuzzitweaks.Config;

/**
 * Shared allow/deny check used by the Distant Horizons mixins to decide whether DH should be
 * allowed to generate LOD data for a given dimension.
 *
 * <p>
 * This deliberately lives <em>outside</em> the {@code com.mrfuzzihead.fuzzitweaks.mixins}
 * package tree. Helper classes referenced from {@code @Inject} handler bodies must not sit inside
 * a mixin package: the mixin transformer would otherwise try to sweep the class up as a mixin
 * candidate and fail to transform it, surfacing at runtime as a {@link NoClassDefFoundError} /
 * {@code ClassNotFoundException} on every call to the handler.
 */
public final class DistantHorizonsDimensionFilter {

    private DistantHorizonsDimensionFilter() {}

    /**
     * Checks whether Distant Horizons should be denied from generating LOD data for the given
     * dimension ID, based on {@link Config#distantHorizonsDimensionListIsDenylist} and
     * {@link Config#distantHorizonsDimensionIds}.
     */
    public static boolean isGenerationDeniedInDimension(int dimensionId) {
        boolean listed = false;
        for (int id : Config.distantHorizonsDimensionIds) {
            if (id == dimensionId) {
                listed = true;
                break;
            }
        }
        return Config.distantHorizonsDimensionListIsDenylist == listed;
    }
}
