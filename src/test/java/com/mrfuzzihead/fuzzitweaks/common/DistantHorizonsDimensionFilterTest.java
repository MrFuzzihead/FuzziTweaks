package com.mrfuzzihead.fuzzitweaks.common;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mrfuzzihead.fuzzitweaks.Config;

/**
 * Exercises the allowlist/denylist interpretation used by both Distant Horizons mixins.
 */
class DistantHorizonsDimensionFilterTest {

    private int[] originalIds;
    private boolean originalDenylist;

    @BeforeEach
    void saveConfig() {
        originalIds = Config.distantHorizonsDimensionIds;
        originalDenylist = Config.distantHorizonsDimensionListIsDenylist;
    }

    @AfterEach
    void restoreConfig() {
        Config.distantHorizonsDimensionIds = originalIds;
        Config.distantHorizonsDimensionListIsDenylist = originalDenylist;
    }

    @Test
    void emptyDenylistAllowsEveryDimension() {
        Config.distantHorizonsDimensionIds = new int[0];
        Config.distantHorizonsDimensionListIsDenylist = true;

        assertFalse(DistantHorizonsDimensionFilter.isGenerationDeniedInDimension(0));
        assertFalse(DistantHorizonsDimensionFilter.isGenerationDeniedInDimension(-1));
        assertFalse(DistantHorizonsDimensionFilter.isGenerationDeniedInDimension(42));
    }

    @Test
    void denylistRejectsOnlyListedDimensions() {
        Config.distantHorizonsDimensionIds = new int[] { 0, -1 };
        Config.distantHorizonsDimensionListIsDenylist = true;

        assertTrue(DistantHorizonsDimensionFilter.isGenerationDeniedInDimension(0));
        assertTrue(DistantHorizonsDimensionFilter.isGenerationDeniedInDimension(-1));
        assertFalse(DistantHorizonsDimensionFilter.isGenerationDeniedInDimension(1));
        assertFalse(DistantHorizonsDimensionFilter.isGenerationDeniedInDimension(42));
    }

    @Test
    void emptyAllowlistRejectsEveryDimension() {
        Config.distantHorizonsDimensionIds = new int[0];
        Config.distantHorizonsDimensionListIsDenylist = false;

        assertTrue(DistantHorizonsDimensionFilter.isGenerationDeniedInDimension(0));
        assertTrue(DistantHorizonsDimensionFilter.isGenerationDeniedInDimension(-1));
        assertTrue(DistantHorizonsDimensionFilter.isGenerationDeniedInDimension(42));
    }

    @Test
    void allowlistAllowsOnlyListedDimensions() {
        Config.distantHorizonsDimensionIds = new int[] { 0, 42 };
        Config.distantHorizonsDimensionListIsDenylist = false;

        assertFalse(DistantHorizonsDimensionFilter.isGenerationDeniedInDimension(0));
        assertFalse(DistantHorizonsDimensionFilter.isGenerationDeniedInDimension(42));
        assertTrue(DistantHorizonsDimensionFilter.isGenerationDeniedInDimension(-1));
        assertTrue(DistantHorizonsDimensionFilter.isGenerationDeniedInDimension(1));
    }
}
