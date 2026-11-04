package com.mrfuzzihead.fuzzitweaks.common.util.wind;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Pins down the allowlist/denylist semantics, including the empty-list behaviour that the config comments and
 * README promise.
 */
class WindDimensionFilterTest {

    @Test
    void defaultAllowlistOnlyAllowsTheOverworld() {
        final int[] dimensions = { 0 };
        assertTrue(WindDimensionFilter.allows(dimensions, false, 0));
        assertFalse(WindDimensionFilter.allows(dimensions, false, -1));
        assertFalse(WindDimensionFilter.allows(dimensions, false, 1));
        assertFalse(WindDimensionFilter.allows(dimensions, false, 42));
    }

    @Test
    void denylistBlocksListedDimensionsAndAllowsTheRest() {
        final int[] dimensions = { 0, -1 };
        assertFalse(WindDimensionFilter.allows(dimensions, true, 0));
        assertFalse(WindDimensionFilter.allows(dimensions, true, -1));
        assertTrue(WindDimensionFilter.allows(dimensions, true, 1));
        assertTrue(WindDimensionFilter.allows(dimensions, true, 42));
    }

    @Test
    void emptyAllowlistAllowsNothing() {
        assertFalse(WindDimensionFilter.allows(new int[0], false, 0));
        assertFalse(WindDimensionFilter.allows(null, false, 0));
    }

    @Test
    void emptyDenylistAllowsEverything() {
        assertTrue(WindDimensionFilter.allows(new int[0], true, 0));
        assertTrue(WindDimensionFilter.allows(null, true, -1));
        assertTrue(WindDimensionFilter.allows(new int[0], true, 42));
    }
}
