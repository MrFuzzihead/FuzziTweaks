package com.mrfuzzihead.fuzzitweaks.common.util;

import java.util.Map;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLeashKnot;
import net.minecraft.entity.EntityLiving;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import com.google.common.collect.MapMaker;
import com.mrfuzzihead.fuzzitweaks.Config;

/**
 * Stops a mob from silently losing its lead to a fence post while its chunk is still being loaded.
 *
 * <p>
 * Vanilla 1.7.10 persists a lead as a single {@code Leash} compound on the mob, and resolves it lazily on the
 * mob's <em>first</em> tick after the chunk is read, in {@code EntityLiving#recreateLeash()}:
 *
 * <ul>
 * <li>{@code readEntityFromNBT} only stashes the anchor into the private {@code field_110170_bx} when the NBT
 * has <em>both</em> {@code Leashed:true} <em>and</em> a {@code Leash} tag.</li>
 * <li>{@code updateLeashedState} runs {@code recreateLeash()} only while that stash is non-null, and otherwise
 * destroys the lead through {@code clearLeashed(true, true)}, which also drops a lead item, as soon as
 * {@code leashedToEntity} is null or dead.</li>
 * <li>The lead knot itself is never written to chunk NBT (it returns false from
 * {@code writeToNBTOptional}), so the only copy is the one rebuilt from the mob. {@code EntityHanging#onUpdate}
 * then re-validates that knot against {@code onValidSurface}, a plain {@code getBlock().getRenderType() == 11}
 * check that reports air while the chunk holding the fence is not resident, and kills it.</li>
 * </ul>
 *
 * <p>
 * So a mob that ticks before the world can hand it a usable anchor is unrecoverable as far as vanilla is
 * concerned, and "the fence is not loaded yet" is indistinguishable from "the fence is gone". This class adds
 * a short grace window on top, but only for a saved <em>fence-post</em> anchor (the {@code X/Y/Z} form):
 *
 * <ul>
 * <li>the anchor is remembered past the single tick vanilla gives it, and the anchor is looked up again every
 * tick of the window;</li>
 * <li>{@code clearLeashed} is a no-op for a restored mob whose fence chunk has not loaded yet. Once that chunk
 * is resident, the block decides what happens: a valid fence is re-attached, while a removed/replaced fence
 * lets vanilla break the lead immediately;</li>
 * <li>the remembered anchor is written back to the mob's NBT while the decision is still pending, so the
 * unrecoverable "flagged as leashed but no anchor to resolve it from" NBT can never reach disk;</li>
 * <li>a successful anchor resolution is forgotten immediately. A later player-driven knot removal therefore
 * cannot be undone by the grace window.</li>
 * </ul>
 *
 * <p>
 * Entity ({@code UUID}) anchors, an empty/malformed {@code Leash} tag, and anchors whose fence is known to be
 * gone are deliberately left to vanilla. They are not load-order problems, and deferring them would only hold
 * an unresolvable lead (or re-create a player-removed knot).
 *
 * <p>
 * Entries only exist for mobs restored while leashed with a fence anchor, a handful at most, and the map
 * is weak-keyed so unloaded mobs stay collectable. This lives outside the mixins package (see
 * {@link DistantHorizonsDimensionFilter})
 * so it is not treated as a mixin itself.
 */
public final class LeashLoadGrace {

    /** Block render type a lead can be tied to; matches {@code ItemLead#onItemUse} and {@code BlockFence}. */
    private static final int RENDER_TYPE_LEASHABLE = 11;

    /**
     * Weak keys keep unloaded mobs collectable and identity comparison is deliberate: {@code Entity.equals}
     * compares only {@code entityId}, which is not unique across dimensions and can be reused after an entity
     * unloads. A plain {@link java.util.WeakHashMap} could therefore leak one mob's anchor into another.
     */
    private static final Map<EntityLiving, State> PENDING = new MapMaker().weakKeys()
        .<EntityLiving, State>makeMap();

    private static final class State {

        /** The raw fence {@code Leash} compound the mob was loaded with. */
        private final NBTTagCompound anchor;

        /** Ticks left during which an unloaded fence chunk may postpone the break. */
        private volatile int graceTicks;

        State(NBTTagCompound anchor, int graceTicks) {
            this.anchor = anchor;
            this.graceTicks = graceTicks;
        }
    }

    private LeashLoadGrace() {}

    /**
     * Starts the grace window for a mob that has just been restored from NBT.
     *
     * <p>
     * Only a well-formed fence anchor is tracked. Vanilla can still resolve a player/entity UUID anchor on its
     * own, and a missing or unusable anchor has nothing to recover, so both keep vanilla's immediate behaviour.
     * </p>
     *
     * @param mob           the mob that was read from NBT
     * @param stashedAnchor the anchor vanilla stashed in {@code EntityLiving.field_110170_bx}, or null when the
     *                      NBT carried no usable {@code Leash} tag
     */
    public static void onEntityLoaded(EntityLiving mob, NBTTagCompound stashedAnchor) {
        if (isClient(mob)) {
            return;
        }

        if (!mob.getLeashed() || Config.leashLoadGraceTicks <= 0 || !isFenceAnchor(stashedAnchor)) {
            PENDING.remove(mob);
            return;
        }

        PENDING.put(mob, new State(stashedAnchor, Config.leashLoadGraceTicks));
    }

    /**
     * Per-tick bookkeeping, called at the head of {@code EntityLiving#updateLeashedState}, which runs exactly
     * once per mob per server tick.
     */
    public static void tickLeash(EntityLiving mob) {
        if (isClient(mob) || !mob.getLeashed()) {
            return;
        }

        State state = PENDING.get(mob);
        if (state == null) {
            return;
        }

        Entity anchor = mob.getLeashedToEntity();
        if (anchor != null && !anchor.isDead) {
            // Anchored again, either because vanilla resolved it or because we re-attached it earlier. The
            // anchor is written to NBT by vanilla from here on, so there is nothing left to remember.
            PENDING.remove(mob);
            return;
        }

        if (state.graceTicks > 0) {
            state.graceTicks--;
        }
        if (state.graceTicks <= 0) {
            PENDING.remove(mob);
        }
    }

    /**
     * Forgets the pending state as soon as a live anchor exists. Called after vanilla's {@code recreateLeash}
     * and {@code setLeashedToEntity}, rather than waiting for the next tick, so a knot created during load
     * cannot be re-created if a player breaks it before that next tick.
     */
    public static void onAnchorResolved(EntityLiving mob) {
        if (isClient(mob) || !mob.getLeashed()) {
            PENDING.remove(mob);
            return;
        }

        Entity anchor = mob.getLeashedToEntity();
        if (anchor != null && !anchor.isDead) {
            PENDING.remove(mob);
        }
    }

    /**
     * Decides whether a lead may be broken off a restored mob, called at the head of {@code
     * EntityLiving#clearLeashed}, which every automatic break funnels through (a lost anchor from {@code
     * EntityLiving#updateLeashedState}, being out of range from {@code EntityCreature#updateLeashedState}, the
     * lead being taken off in {@code NetHandlerPlayClient}).
     *
     * <p>
     * Only a restored mob with a fence anchor is ever deferred, and only while the fence's chunk is not loaded
     * (or a loaded fence exists but the knot cannot be attached yet). If the chunk is resident and the block is
     * no longer leashable, the fence really was removed, so vanilla breaks the lead immediately.
     * </p>
     *
     * @param dropLead whether vanilla is about to drop a lead item; a no-drop clear is a player or command
     *                 detach, not an automatic load failure
     * @return {@code true} to cancel {@code clearLeashed}, keeping the lead and dropping nothing
     */
    public static boolean deferAnchorLoss(EntityLiving mob, boolean dropLead) {
        if (isClient(mob)) {
            return false;
        }
        if (!mob.getLeashed()) {
            PENDING.remove(mob);
            return false;
        }

        if (!dropLead) {
            // EntityLeashKnot.interactFirst uses clearLeashed(true, false) for a creative player removing a
            // knot. Never revive that; the normal automatic load failures all pass dropLead=true.
            PENDING.remove(mob);
            return false;
        }

        State state = PENDING.get(mob);
        if (state == null) {
            return false;
        }
        if (state.graceTicks <= 0) {
            PENDING.remove(mob);
            return false;
        }

        Entity anchor = mob.getLeashedToEntity();
        if (anchor != null && !anchor.isDead) {
            PENDING.remove(mob);
            return false;
        }

        if (!isFenceAnchor(state.anchor)) {
            PENDING.remove(mob);
            return false;
        }

        World world = mob.worldObj;
        int x = state.anchor.getInteger("X");
        int y = state.anchor.getInteger("Y");
        int z = state.anchor.getInteger("Z");

        // We cannot tell whether the post is there until its chunk is resident. Hold the lead, but do not
        // spawn a knot into an unloaded chunk; the grace counter bounds how long this can last.
        if (!world.blockExists(x, y, z)) {
            return true;
        }

        if (world.getBlock(x, y, z)
            .getRenderType() != RENDER_TYPE_LEASHABLE) {
            // The chunk is loaded and the post is gone (or replaced). This is a real removal, not a load race.
            PENDING.remove(mob);
            return false;
        }

        // Best effort: normally the fence is right there and this succeeds on the mob's first tick, in which
        // case the mob is anchored again and the grace window is dropped immediately.
        if (reattach(mob, state.anchor)) {
            PENDING.remove(mob);
            return true;
        }

        // The post is present but the knot could not be attached (for example, only a dead knot matched).
        // Keep trying until the grace window closes.
        return true;
    }

    /**
     * Writes the remembered anchor into the mob's NBT when vanilla could not. Vanilla only writes the
     * {@code Leash} tag when {@code leashedToEntity} is non-null, so a mob that is flagged as leashed but has
     * no anchor is saved as {@code Leashed:true} with nothing to resolve it from and drops a lead on its very
     * first tick after the next load.
     *
     * <p>
     * This is intentionally limited to a still-pending fence anchor. A live anchor is written by vanilla, and
     * an unresolvable or already-removed fence is not preserved just to reset the grace window on every save.
     * </p>
     *
     * @param mob the mob being saved
     * @param tag the NBT vanilla has already written for it
     */
    public static void writeLeashAnchor(EntityLiving mob, NBTTagCompound tag) {
        if (isClient(mob) || !mob.getLeashed()) {
            return;
        }

        NBTBase written = tag.getTag("Leash");
        if (written instanceof NBTTagCompound && !((NBTTagCompound) written).hasNoTags()) {
            return;
        }

        Entity liveAnchor = mob.getLeashedToEntity();
        if (liveAnchor != null && !liveAnchor.isDead) {
            // Vanilla already has a usable anchor to write; do not replace it with the remembered one.
            return;
        }

        State state = PENDING.get(mob);
        if (state == null || state.graceTicks <= 0 || !isFenceAnchor(state.anchor)) {
            return;
        }

        World world = mob.worldObj;
        int x = state.anchor.getInteger("X");
        int y = state.anchor.getInteger("Y");
        int z = state.anchor.getInteger("Z");

        if (world.blockExists(x, y, z) && world.getBlock(x, y, z)
            .getRenderType() != RENDER_TYPE_LEASHABLE) {
            // The fence is known to be gone. Do not preserve a lead that vanilla is about to break.
            return;
        }

        tag.setTag("Leash", state.anchor);
    }

    /**
     * Resolves a fence anchor and ties the mob to it, creating the knot entity if the world does not have one
     * yet, which is the normal case since knots are never written to chunk NBT.
     *
     * @return {@code true} when the mob ends up with a live anchor
     */
    private static boolean reattach(EntityLiving mob, NBTTagCompound anchor) {
        if (!isFenceAnchor(anchor)) {
            return false;
        }

        World world = mob.worldObj;
        int x = anchor.getInteger("X");
        int y = anchor.getInteger("Y");
        int z = anchor.getInteger("Z");

        EntityLeashKnot knot = EntityLeashKnot.getKnotForBlock(world, x, y, z);
        if (knot != null && !knot.isDead) {
            return attach(mob, knot);
        }

        // The fence's own chunk may not be resident yet, in which case getBlock() reports air and the knot
        // spawned here would be killed again by EntityHanging#onUpdate. Wait for it instead.
        if (!world.blockExists(x, y, z)) {
            return false;
        }

        if (world.getBlock(x, y, z)
            .getRenderType() != RENDER_TYPE_LEASHABLE) {
            return false;
        }

        return attach(mob, EntityLeashKnot.func_110129_a(world, x, y, z));
    }

    private static boolean attach(EntityLiving mob, EntityLeashKnot knot) {
        if (knot == null || knot.isDead) {
            return false;
        }

        // The same call vanilla's recreateLeash() makes, except that it also tells clients about the new
        // anchor, so the lead renders straight away instead of only once the mob re-leashes by hand.
        mob.setLeashedToEntity(knot, true);
        return true;
    }

    /**
     * Matches the fence branch of {@code EntityLiving#recreateLeash()}: UUID anchors take precedence, then the
     * {@code X/Y/Z} block position. A missing or empty tag is not recoverable.
     */
    private static boolean isFenceAnchor(NBTTagCompound anchor) {
        if (anchor == null || anchor.hasNoTags()) {
            return false;
        }

        if (anchor.hasKey("UUIDMost", 4) && anchor.hasKey("UUIDLeast", 4)) {
            return false;
        }

        return anchor.hasKey("X", 99) && anchor.hasKey("Y", 99) && anchor.hasKey("Z", 99);
    }

    private static boolean isClient(EntityLiving mob) {
        return mob.worldObj == null || mob.worldObj.isRemote;
    }
}
