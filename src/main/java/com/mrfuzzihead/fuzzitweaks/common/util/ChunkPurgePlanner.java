package com.mrfuzzihead.fuzzitweaks.common.util;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Pure selection logic for the chunk purge module.
 *
 * <p>
 * Every watcher (player, Forge ticket, spawn area) is a {@link Seed}: a chunk coordinate plus a Chebyshev radius.
 * Starting from each seed, a four-neighbour flood fill walks only chunks that are both loaded and inside that seed's
 * radius. Every visited chunk is protected; every loaded chunk that no seed reaches is a purge candidate.
 *
 * <p>
 * A per-seed visited set is deliberate. Radii differ between players, tickets and spawn, so a chunk already protected
 * by a small ticket radius must still be allowed to expand outward when a larger player radius reaches it. A shared
 * visited set would silently stop that expansion and under-protect chunks near players.
 */
public final class ChunkPurgePlanner {

    private static final int[][] NEIGHBOURS = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };

    private ChunkPurgePlanner() {}

    /**
     * @param loaded all loaded chunk keys in the dimension
     * @param seeds  watcher seeds; a seed whose chunk is not loaded protects nothing
     * @return loaded chunks that are not connected to any seed within its radius
     */
    public static Set<Long> findUnloadable(Set<Long> loaded, List<Seed> seeds) {
        final Set<Long> protectedChunks = new HashSet<>();
        for (Seed seed : seeds) {
            protect(loaded, seed, protectedChunks);
        }

        final Set<Long> unloadable = new HashSet<>();
        for (Long key : loaded) {
            if (!protectedChunks.contains(key)) {
                unloadable.add(key);
            }
        }
        return unloadable;
    }

    private static void protect(Set<Long> loaded, Seed seed, Set<Long> protectedChunks) {
        if (seed == null || !loaded.contains(seed.key())) {
            return;
        }

        final int radius = Math.max(0, seed.radius());
        final int seedX = ChunkKey.x(seed.key());
        final int seedZ = ChunkKey.z(seed.key());
        final Set<Long> visited = new HashSet<>();
        final ArrayDeque<Long> queue = new ArrayDeque<>();

        visited.add(seed.key());
        queue.add(seed.key());

        while (!queue.isEmpty()) {
            final long key = queue.poll();
            protectedChunks.add(key);

            final int x = ChunkKey.x(key);
            final int z = ChunkKey.z(key);
            for (int[] direction : NEIGHBOURS) {
                final int neighbourX = x + direction[0];
                final int neighbourZ = z + direction[1];
                if (Math.abs(neighbourX - seedX) > radius || Math.abs(neighbourZ - seedZ) > radius) {
                    continue;
                }

                final long neighbour = ChunkKey.of(neighbourX, neighbourZ);
                if (!loaded.contains(neighbour) || !visited.add(neighbour)) {
                    continue;
                }
                queue.add(neighbour);
            }
        }
    }

    /**
     * A watcher: the chunk it occupies plus the number of chunks around it that must stay loaded.
     */
    public static final class Seed {

        private final long key;
        private final int radius;

        public static Seed of(int x, int z, int radius) {
            return new Seed(ChunkKey.of(x, z), radius);
        }

        public Seed(long key, int radius) {
            this.key = key;
            this.radius = radius;
        }

        public long key() {
            return this.key;
        }

        public int radius() {
            return this.radius;
        }
    }
}
