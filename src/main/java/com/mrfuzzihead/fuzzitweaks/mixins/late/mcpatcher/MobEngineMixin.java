package com.mrfuzzihead.fuzzitweaks.mixins.late.mcpatcher;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import net.minecraft.util.ResourceLocation;

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
     * Sentinel stored in {@link #fix$cache} when a resource has been fully
     * scanned and confirmed to have no random variants.
     * Using a sentinel avoids a separate negative-cache collection and lets
     * us use a single {@link ConcurrentHashMap} for all outcomes.
     */
    @Unique
    private static final ObjectList<MobInfo> fix$NO_VARIANTS = new ObjectArrayList<>(0);

    /**
     * Thread-safe replacement for the original {@code Object2ObjectOpenHashMap}.
     * Keyed by the original {@link ResourceLocation}; values are either
     * {@link #fix$NO_VARIANTS} (confirmed no variants) or a non-empty list.
     */
    @Unique
    private static final ConcurrentHashMap<ResourceLocation, ObjectList<MobInfo>> fix$cache = new ConcurrentHashMap<>();

    /**
     * Tracks in-flight background scans so we never submit the same scan twice
     * while the first one is still running.
     */
    @Unique
    private static final ConcurrentHashMap<ResourceLocation, Future<?>> fix$pendingScans = new ConcurrentHashMap<>();

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
            // Sentinel → confirmed no variants for this texture.
            if (infos == fix$NO_VARIANTS) {
                return original;
            }
            // Scan complete → find the first matching rule.
            for (MobInfo info : infos) {
                if (info.matches(currentEntity)) {
                    return info.getTextureFor(currentEntity);
                }
            }
            return original;
        }

        // Not yet scanned. Submit a background task if one is not already in
        // flight, then return the original so this frame is never stalled.
        fix$pendingScans.computeIfAbsent(original, loc -> fix$SCAN_EXECUTOR.submit(() -> {
            ObjectList<MobInfo> result = MobInfo.getInfoFor(loc);
            fix$cache.put(loc, result != null ? result : fix$NO_VARIANTS);
            fix$pendingScans.remove(loc);
        }));

        return original;
    }

    /**
     * Cancels every in-flight scan and clears the new concurrent cache
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
        for (Future<?> future : fix$pendingScans.values()) {
            future.cancel(true);
        }
        fix$pendingScans.clear();
        fix$cache.clear();
    }
}
