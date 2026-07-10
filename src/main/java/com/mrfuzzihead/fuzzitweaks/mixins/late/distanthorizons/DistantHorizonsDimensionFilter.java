package com.mrfuzzihead.fuzzitweaks.mixins.late.distanthorizons;

import com.mrfuzzihead.fuzzitweaks.Config;

/**
 * Shared allow/deny check used by {@link MixinAbstractDhServerLevel} and
 * {@link MixinDhClientLevel} to decide whether Distant Horizons should be allowed to generate LOD
 * data for a given dimension.
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
