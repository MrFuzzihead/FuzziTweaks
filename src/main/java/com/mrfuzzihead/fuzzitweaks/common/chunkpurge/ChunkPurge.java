package com.mrfuzzihead.fuzzitweaks.common.chunkpurge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.FakePlayer;

import com.mrfuzzihead.fuzzitweaks.Config;
import com.mrfuzzihead.fuzzitweaks.FuzziTweaks;
import com.mrfuzzihead.fuzzitweaks.common.util.ChunkKey;
import com.mrfuzzihead.fuzzitweaks.common.util.ChunkPurgePlanner;
import com.mrfuzzihead.fuzzitweaks.common.util.ChunkPurgePlanner.Seed;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;

/**
 * Server-side chunk purge diagnostics and optional unloading.
 *
 * <p>
 * The module deliberately uses only public {@code ChunkProviderServer} APIs:
 * {@code func_152380_a()} for the loaded list and {@code unloadChunksIfNotNearSpawn(x, z)} for queueing. That is the
 * same path vanilla and Hodgepodge's FastUnload already drain, and it means there are no behaviour mixins or access
 * transformers to conflict with Hodgepodge, ChunkAPI or a custom chunk provider. Chunks are queued, not dropped
 * directly, so the normal save/unload path (including ChunkAPI custom data) runs.
 *
 * <p>
 * Modes:
 *
 * <ul>
 * <li>{@code OFF} - nothing.</li>
 * <li>{@code REPORT} - scan and log only. This is the default; it never unloads anything.</li>
 * <li>{@code PURGE} - scan and queue candidates for unloading.</li>
 * </ul>
 *
 * <p>
 * Protection mirrors the original ChunkPurge: players (view distance plus a margin), Forge chunk tickets, and the
 * spawn area. Dimensions with no players are skipped by default, both to avoid noisy reports and to avoid unloading
 * a dimension that is deliberately kept loaded by a ticket while nobody is online. The loaded set, player list and
 * ticket set are snapshotted at the start of a scan; queueing happens after the plan is complete, so the planner
 * never iterates a list the unload path is mutating.
 */
public final class ChunkPurge {

    private static final ChunkPurge INSTANCE = new ChunkPurge();

    /** Guards against duplicate registration when an integrated server is started more than once in one JVM. */
    private static boolean registered;

    /** Last scan result per dimension, for the status command. */
    private static final Map<Integer, RunStats> LAST_RUNS = new ConcurrentHashMap<>();

    private ChunkPurge() {}

    /** Registers the server tick handler. Called on server start; safe to call more than once. */
    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;

        FMLCommonHandler.instance()
            .bus()
            .register(INSTANCE);
        if (Config.chunkPurgeMode != Config.ChunkPurgeMode.OFF) {
            FuzziTweaks.LOG.info(
                "ChunkPurge mode={} interval={} ticks dimensions={}{}",
                Config.chunkPurgeMode,
                Config.chunkPurgeIntervalTicks,
                Arrays.toString(Config.chunkPurgeDimensions),
                Config.chunkPurgeMode == Config.ChunkPurgeMode.REPORT ? " (diagnostic only)" : "");
        }
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != Phase.END || Config.chunkPurgeMode == Config.ChunkPurgeMode.OFF) {
            return;
        }
        if (!(event.world instanceof WorldServer)) {
            return;
        }

        final WorldServer world = (WorldServer) event.world;
        final int interval = Math.max(1, Config.chunkPurgeIntervalTicks);
        if (world.getTotalWorldTime() % interval != 0L) {
            return;
        }

        scan(world, Config.chunkPurgeMode, true);
    }

    /**
     * Runs one scan.
     *
     * @param mode                 mode to use; the command can force REPORT on a PURGE-configured server
     * @param respectDimensionList whether the configured dimension allowlist applies. The tick handler respects it;
     *                             an explicit command dimension does not.
     */
    public static RunStats scan(WorldServer world, Config.ChunkPurgeMode mode, boolean respectDimensionList) {
        final long start = System.nanoTime();
        final int dimension = world.provider.dimensionId;

        if (mode == Config.ChunkPurgeMode.OFF) {
            return skipped(world, mode, start, "mode is OFF");
        }
        if (respectDimensionList && !isDimensionEnabled(dimension)) {
            return skipped(world, mode, start, "dimension is not in the configured list");
        }
        if (world.levelSaving) {
            return skipped(world, mode, start, "world is saving or unloading");
        }
        if (!(world.getChunkProvider() instanceof ChunkProviderServer)) {
            return skipped(world, mode, start, "chunk provider is not ChunkProviderServer");
        }
        if (!Config.chunkPurgeProcessEmptyDimensions && world.playerEntities.isEmpty()) {
            return skipped(world, mode, start, "no players in dimension");
        }

        final ChunkProviderServer provider = (ChunkProviderServer) world.getChunkProvider();
        final List<Chunk> loadedChunks = provider.func_152380_a();
        final Set<Long> loaded = new HashSet<>(Math.max(16, loadedChunks.size() * 2));
        for (Chunk chunk : loadedChunks) {
            loaded.add(ChunkKey.of(chunk.xPosition, chunk.zPosition));
        }

        final List<Seed> seeds = new ArrayList<>();
        final int playerRadius = getPlayerProtectionRadius();
        for (EntityPlayer player : world.playerEntities) {
            if (!(player instanceof EntityPlayerMP) || player instanceof FakePlayer) {
                continue;
            }
            seeds.add(Seed.of(player.chunkCoordX, player.chunkCoordZ, playerRadius));
        }

        for (ChunkCoordIntPair ticket : world.getPersistentChunks()
            .keySet()) {
            seeds.add(Seed.of(ticket.chunkXPos, ticket.chunkZPos, Config.chunkPurgeTicketMargin));
        }

        final boolean spawnProtected = Config.chunkPurgeProtectSpawn && world.provider.canRespawnHere()
            && DimensionManager.shouldLoadSpawn(dimension);
        if (spawnProtected) {
            seeds.add(
                Seed.of(
                    world.getSpawnPoint().posX >> 4,
                    world.getSpawnPoint().posZ >> 4,
                    8 + Config.chunkPurgeSpawnMargin));
        }

        final Set<Long> unloadable = ChunkPurgePlanner.findUnloadable(loaded, seeds);

        int queued = 0;
        if (mode == Config.ChunkPurgeMode.PURGE) {
            final int max = Config.chunkPurgeMaxPerRun;
            for (Long key : unloadable) {
                if (max > 0 && queued >= max) {
                    break;
                }

                // Re-queueing a chunk that is already in the provider's queue is a no-op. Keeping no separate
                // pending set is deliberate: a leaking mod can reload a queued chunk before the drain reaches it,
                // and a pending entry would then suppress re-queueing it forever.
                provider.unloadChunksIfNotNearSpawn(ChunkKey.x(key), ChunkKey.z(key));
                queued++;
            }
        }

        final RunStats stats = new RunStats(
            dimension,
            mode,
            world.getTotalWorldTime(),
            loaded.size(),
            loaded.size() - unloadable.size(),
            unloadable.size(),
            queued,
            System.nanoTime() - start,
            null);
        LAST_RUNS.put(dimension, stats);
        log(world, stats, unloadable, seeds.size(), playerRadius, spawnProtected);
        return stats;
    }

    public static RunStats getLastRun(int dimension) {
        return LAST_RUNS.get(dimension);
    }

    private static RunStats skipped(WorldServer world, Config.ChunkPurgeMode mode, long start, String reason) {
        final RunStats stats = new RunStats(
            world.provider.dimensionId,
            mode,
            world.getTotalWorldTime(),
            0,
            0,
            0,
            0,
            System.nanoTime() - start,
            reason);
        LAST_RUNS.put(world.provider.dimensionId, stats);
        if (Config.chunkPurgeDebug) {
            FuzziTweaks.LOG.info("ChunkPurge [{}] skipped: {}", world.provider.dimensionId, reason);
        }
        return stats;
    }

    private static void log(WorldServer world, RunStats stats, Set<Long> unloadable, int seedCount, int playerRadius,
        boolean spawnProtected) {
        final boolean report = stats.mode == Config.ChunkPurgeMode.REPORT;
        if (!Config.chunkPurgeDebug && (report ? unloadable.isEmpty() : stats.queued == 0)) {
            return;
        }

        FuzziTweaks.LOG.info(
            "ChunkPurge {} [{}] loaded={} protected={} unloadable={} queued={} elapsed={}ms{}",
            stats.mode,
            stats.dimension,
            stats.loaded,
            stats.protectedChunks,
            stats.unloadable,
            stats.queued,
            stats.elapsedMillis(),
            sample(unloadable));

        if (Config.chunkPurgeDebug) {
            FuzziTweaks.LOG.info(
                "ChunkPurge [{}] seeds={} playerRadius={} spawnProtected={}",
                stats.dimension,
                seedCount,
                playerRadius,
                spawnProtected);
        }
    }

    private static String sample(Set<Long> unloadable) {
        final int limit = Config.chunkPurgeReportSampleSize;
        if (limit <= 0 || unloadable.isEmpty()) {
            return "";
        }

        final StringBuilder builder = new StringBuilder(" sample=");
        int count = 0;
        for (Long key : unloadable) {
            if (count >= limit) {
                builder.append(", ...");
                break;
            }
            if (count > 0) {
                builder.append(", ");
            }
            builder.append('(')
                .append(ChunkKey.x(key))
                .append(',')
                .append(ChunkKey.z(key))
                .append(')');
            count++;
        }
        return builder.toString();
    }

    public static int getPlayerProtectionRadius() {
        if (Config.chunkPurgePlayerProtectionRadius > 0) {
            return Config.chunkPurgePlayerProtectionRadius;
        }

        final MinecraftServer server = MinecraftServer.getServer();
        final int viewDistance = server == null ? 10
            : server.getConfigurationManager()
                .getViewDistance();
        return Math.max(0, viewDistance + Config.chunkPurgePlayerMargin);
    }

    private static boolean isDimensionEnabled(int dimension) {
        for (int configured : Config.chunkPurgeDimensions) {
            if (configured == dimension) {
                return true;
            }
        }
        return false;
    }

    /** Immutable result of one scan, used by the status command. */
    public static final class RunStats {

        public final int dimension;
        public final Config.ChunkPurgeMode mode;
        public final long tick;
        public final int loaded;
        public final int protectedChunks;
        public final int unloadable;
        public final int queued;
        public final long elapsedNanos;
        /** Null for a completed scan; a short reason when the scan was skipped. */
        public final String skipReason;

        RunStats(int dimension, Config.ChunkPurgeMode mode, long tick, int loaded, int protectedChunks, int unloadable,
            int queued, long elapsedNanos, String skipReason) {
            this.dimension = dimension;
            this.mode = mode;
            this.tick = tick;
            this.loaded = loaded;
            this.protectedChunks = protectedChunks;
            this.unloadable = unloadable;
            this.queued = queued;
            this.elapsedNanos = elapsedNanos;
            this.skipReason = skipReason;
        }

        public long elapsedMillis() {
            return this.elapsedNanos / 1_000_000L;
        }
    }
}
