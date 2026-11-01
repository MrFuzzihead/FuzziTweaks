package com.mrfuzzihead.fuzzitweaks.common.util;

/**
 * Packs chunk coordinates into a {@code long} using the same layout as
 * {@link net.minecraft.world.ChunkCoordIntPair#chunkXZ2Int(int, int)} so the planner can work without boxing chunk
 * objects. The low 32 bits are X and the high 32 bits are Z, which also keeps negative coordinates intact.
 */
public final class ChunkKey {

    private ChunkKey() {}

    public static long of(int x, int z) {
        return (long) x & 0xFFFFFFFFL | ((long) z & 0xFFFFFFFFL) << 32;
    }

    public static int x(long key) {
        return (int) key;
    }

    public static int z(long key) {
        return (int) (key >> 32);
    }
}
