package com.mrfuzzihead.fuzzitweaks.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

/** Pure tests for the flood-fill selection used by the chunk purge module. */
class ChunkPurgePlannerTest {

    @Test
    void protectsASquareAroundASeed() {
        final Set<Long> loaded = grid(-4, 4, -4, 4);
        final Set<Long> unloadable = plan(loaded, seed(0, 0, 1));

        assertEquals(81, loaded.size());
        assertEquals(9, loaded.size() - unloadable.size());
        assertFalse(unloadable.contains(key(0, 0)));
        assertFalse(unloadable.contains(key(1, 1)));
        assertTrue(unloadable.contains(key(2, 0)));
    }

    @Test
    void zeroRadiusProtectsOnlyTheSeed() {
        final Set<Long> loaded = grid(-2, 2, -2, 2);
        final Set<Long> unloadable = plan(loaded, seed(0, 0, 0));

        assertEquals(1, loaded.size() - unloadable.size());
        assertFalse(unloadable.contains(key(0, 0)));
        assertTrue(unloadable.contains(key(1, 0)));
    }

    @Test
    void doesNotCrossAGapEvenWhenInsideTheRadius() {
        final Set<Long> loaded = grid(-3, 3, -3, 3);
        for (int z = -3; z <= 3; z++) {
            loaded.remove(key(1, z));
        }

        final Set<Long> unloadable = plan(loaded, seed(0, 0, 3));

        assertTrue(unloadable.contains(key(2, 0)));
        assertTrue(unloadable.contains(key(3, 0)));
        assertFalse(unloadable.contains(key(0, 0)));
        assertFalse(unloadable.contains(key(-1, 1)));
    }

    @Test
    void multipleSeedsProtectTheirOwnAreas() {
        final Set<Long> loaded = grid(0, 4, 0, 4);
        final Set<Long> unloadable = plan(loaded, seed(0, 0, 0), seed(4, 4, 0));

        assertEquals(2, loaded.size() - unloadable.size());
        assertFalse(unloadable.contains(key(0, 0)));
        assertFalse(unloadable.contains(key(4, 4)));
        assertTrue(unloadable.contains(key(2, 2)));
    }

    @Test
    void aSeedThatIsNotLoadedProtectsNothing() {
        final Set<Long> loaded = grid(0, 2, 0, 2);
        final Set<Long> unloadable = plan(loaded, seed(99, 99, 10));

        assertEquals(loaded, unloadable);
    }

    @Test
    void anEmptyLoadedSetProducesNoCandidates() {
        assertTrue(plan(new HashSet<>(), seed(0, 0, 5)).isEmpty());
    }

    @Test
    void worksWithNegativeCoordinates() {
        final Set<Long> loaded = grid(-2, 2, -2, 2);
        final Set<Long> unloadable = plan(loaded, seed(-2, -2, 1));

        assertFalse(unloadable.contains(key(-2, -2)));
        assertFalse(unloadable.contains(key(-1, -1)));
        assertTrue(unloadable.contains(key(0, 0)));
    }

    @Test
    void radiusIsChebyshevNotManhattan() {
        final Set<Long> loaded = grid(-2, 2, -2, 2);
        final Set<Long> unloadable = plan(loaded, seed(0, 0, 1));

        assertFalse(unloadable.contains(key(1, 1)));
        assertTrue(unloadable.contains(key(2, 0)));
    }

    private static Set<Long> plan(Set<Long> loaded, ChunkPurgePlanner.Seed... seeds) {
        final List<ChunkPurgePlanner.Seed> list = new ArrayList<>(Arrays.asList(seeds));
        return ChunkPurgePlanner.findUnloadable(loaded, list);
    }

    private static ChunkPurgePlanner.Seed seed(int x, int z, int radius) {
        return ChunkPurgePlanner.Seed.of(x, z, radius);
    }

    private static long key(int x, int z) {
        return ChunkKey.of(x, z);
    }

    private static Set<Long> grid(int minX, int maxX, int minZ, int maxZ) {
        final Set<Long> loaded = new HashSet<>();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                loaded.add(key(x, z));
            }
        }
        return loaded;
    }
}
