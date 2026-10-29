package com.mrfuzzihead.fuzzitweaks.mixins.late.mcpatcher;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import net.minecraft.util.ResourceLocation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.falsepattern.mcpatcher.internal.modules.mob.MobEngine;
import com.falsepattern.mcpatcher.internal.modules.mob.MobInfo;
import com.falsepattern.mcpatcher.internal.modules.mob.TrackedEntity;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;

@Mixin(value = MobEngine.class, remap = false)
public abstract class MobEngineMixin {

    /** The entity currently being rendered (set by {@code nextEntity}). */
    @Shadow(remap = false)
    private static TrackedEntity currentEntity;

    /**
     * Sentinel stored in {@link #fix$cache} when a resource has no random
     * variants, or when its scan failed. Using a sentinel avoids a separate
     * negative-cache collection and lets us use a single
     * {@link ConcurrentHashMap} for all outcomes.
     */
    @Unique
    private static final ObjectList<MobInfo> fix$NO_VARIANTS = new ObjectArrayList<>(0);

    /**
     * Thread-safe replacement for the original {@code Object2ObjectOpenHashMap}.
     * Values are either {@link #fix$NO_VARIANTS} or a non-empty list.
     */
    @Unique
    private static final ConcurrentHashMap<ResourceLocation, ObjectList<MobInfo>> fix$cache = new ConcurrentHashMap<>();

    /**
     * Tracks in-flight background scans so we never submit the same scan twice
     * while the first one is still running. Each task removes itself from this
     * map in a {@code finally} block.
     */
    @Unique
    private static final ConcurrentHashMap<ResourceLocation, Future<?>> fix$pendingScans = new ConcurrentHashMap<>();

    /**
     * Incremented before every resource reload. A scan only publishes its
     * result if this still matches the value captured when it was submitted.
     */
    @Unique
    private static final AtomicLong fix$generation = new AtomicLong();

    /**
     * Serializes generation bumps/cache clears with result publication. Without
     * this, a scan could pass the generation check just before a reload and
     * publish stale data just after the cache was cleared.
     */
    @Unique
    private static final Object fix$cacheLock = new Object();

    @Unique
    private static final Logger fix$LOG = LogManager.getLogger("FuzziTweaks/MCPatcher");

    /**
     * Single daemon thread used for all resource-pack scanning.
     * Priority is kept one notch below normal so it does not compete with
     * the render thread.
     */
    @Unique
    private static final ExecutorService fix$SCAN_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "MCPatcher-MobScanner");
        t.setDaemon(true);
        t.setPriority(Thread.NORM_PRIORITY - 1);
        return t;
    });

    /**
     * @author FuzziTweaks
     * @reason Replaces the original blocking scan with a non-blocking lookup.
     *         <p>
     *         Original problem: the original code called
     *         {@code cache.computeIfAbsent(original, MobInfo::getInfoFor)} directly on
     *         the render thread. {@code MobInfo::getInfoFor} performs I/O (reading
     *         resource-pack streams), which can stall the render thread for several
     *         frames on the first encounter of each texture.
     *         </p>
     *
     *         <p>
     *         Fix: check the concurrent cache first; if the result is already known,
     *         return it immediately. Otherwise, submit a background scan and return the
     *         original texture for this frame so the render thread is never blocked.
     *         </p>
     */
    @Overwrite(remap = false)
    public static ResourceLocation getTexture(ResourceLocation original) {
        ObjectList<MobInfo> infos = fix$cache.get(original);
        if (infos != null) {
            // Sentinel -> confirmed no variants (or a cached scan failure).
            if (infos == fix$NO_VARIANTS) {
                return original;
            }
            // Scan complete -> find the first matching rule.
            for (MobInfo info : infos) {
                if (info.matches(currentEntity)) {
                    return info.getTextureFor(currentEntity);
                }
            }
            return original;
        }

        // Not yet scanned. Submit a background task if one is not already in
        // flight, then return the original so this frame is never stalled.
        Future<?> pending = fix$pendingScans.get(original);
        if (pending != null) {
            if (!pending.isDone()) {
                return original;
            }
            // A task that finished between the cache lookup and this check may
            // have published a result after that lookup. Removing the done
            // future lets the next frame pick it up (or retry a failed scan).
            fix$pendingScans.remove(original, pending);
        }

        long generation = fix$generation.get();
        AtomicReference<Future<?>> self = new AtomicReference<>();
        FutureTask<Void> task = new FutureTask<>(() -> fix$scan(original, generation, self), null);
        self.set(task);

        if (fix$pendingScans.putIfAbsent(original, task) == null) {
            try {
                fix$SCAN_EXECUTOR.execute(task);
            } catch (RejectedExecutionException e) {
                fix$pendingScans.remove(original, task);
                fix$LOG.warn("Failed to schedule mob texture scan for {}", original, e);
            }
        }

        return original;
    }

    /**
     * Scans one texture and publishes the result only for the current reload
     * generation. Failures are cached as {@link #fix$NO_VARIANTS} so a broken
     * resource pack cannot cause a retry and log message every frame.
     *
     * <p>
     * The task always removes itself from {@link #fix$pendingScans} in a
     * {@code finally} block. The conditional removal prevents an old task that
     * ignored an interrupt from removing a newer task for the same texture.
     * </p>
     */
    @Unique
    private static void fix$scan(ResourceLocation original, long generation, AtomicReference<Future<?>> self) {
        try {
            ObjectList<MobInfo> result = MobInfo.getInfoFor(original);
            synchronized (fix$cacheLock) {
                if (fix$generation.get() == generation) {
                    fix$cache.put(original, result != null ? result : fix$NO_VARIANTS);
                }
            }
        } catch (Exception e) {
            boolean current;
            synchronized (fix$cacheLock) {
                current = fix$generation.get() == generation;
                if (current) {
                    fix$cache.put(original, fix$NO_VARIANTS);
                }
            }
            if (current) {
                fix$LOG.warn("Failed to scan mob textures for {}", original, e);
            }
        } finally {
            // A cancelled scan must not leave the shared worker interrupted,
            // otherwise the next queued scan can fail immediately.
            Thread.interrupted();
            Future<?> task = self.get();
            if (task != null) {
                fix$pendingScans.remove(original, task);
            }
        }
    }

    /**
     * Invalidates in-flight scans and clears the concurrent cache
     * <em>before</em> the original method body runs.
     *
     * <p>
     * The original body still clears the old {@code cache} and
     * {@code negativeCache} collections; we let it do so to avoid any
     * leftover state in those unused maps.
     * </p>
     */
    @Inject(method = "reloadResources", at = @At("HEAD"), remap = false)
    private static void fix$cancelPendingScans(CallbackInfo ci) {
        synchronized (fix$cacheLock) {
            fix$generation.incrementAndGet();
            for (Future<?> future : fix$pendingScans.values()) {
                future.cancel(true);
            }
            fix$pendingScans.clear();
            fix$cache.clear();
        }
    }
}
