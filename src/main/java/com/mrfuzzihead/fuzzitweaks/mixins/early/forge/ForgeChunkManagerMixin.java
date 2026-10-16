package com.mrfuzzihead.fuzzitweaks.mixins.early.forge;

import java.util.Map;

import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.ForgeChunkManager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.google.common.cache.Cache;

/**
 * TileEntity Reset Fix, ported to a mixin.
 *
 * <p>
 * {@code ChunkProviderServer.loadChunk} asks {@link ForgeChunkManager#fetchDormantChunk(long, World)} before
 * loading from disk, and {@code unloadQueuedChunks} later parks the unloaded chunk in that cache via
 * {@code putDormantChunk}. By the time a chunk becomes dormant it has already been through
 * {@code Chunk.onChunkUnload} (tile entities detached) and been saved, so serving the same object a second
 * time can resurrect stale tile entity data instead of the fresh copy on disk. The original mod invalidated
 * the just-served entry at exit; this mixin does the same, keeping the cache itself functional rather than
 * disabling it.
 *
 * <p>
 * Only has an effect when Forge's {@code forgeChunkLoading.cfg} sets {@code dormantChunkCacheSize} above 0 -
 * with the default 0 the Guava cache evicts immediately and this is a no-op.
 *
 * <p>
 * {@code remap = false}: {@link ForgeChunkManager} is a Forge class, so neither its members nor
 * {@code fetchDormantChunk} have an obfuscation mapping, and the annotation processor cannot build a refmap
 * entry for the {@code @Inject} target. Minecraft class names are left unmapped in the refmap anyway, so the
 * {@code World}/{@code Chunk} references in the handler stay valid. This mirrors the other non-Minecraft
 * mixins in this mod.
 */
@Mixin(value = ForgeChunkManager.class, remap = false)
public abstract class ForgeChunkManagerMixin {

    @Shadow
    private static Map<World, Cache<Long, Chunk>> dormantChunkCache;

    @Inject(method = "fetchDormantChunk", at = @At("RETURN"), require = 1)
    private static void fuzziTweaks$invalidateFetchedDormantChunk(long coords, World world,
        CallbackInfoReturnable<Chunk> cir) {
        final Cache<Long, Chunk> cache = dormantChunkCache.get(world);
        if (cache != null) {
            cache.invalidate(coords);
        }
    }
}
