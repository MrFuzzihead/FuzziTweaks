package com.mrfuzzihead.fuzzitweaks.common.util;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLeashKnot;
import net.minecraft.entity.EntityLiving;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

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
 * <li>The lead knot itself is never written to chunk NBT (it returns false from {@code
 * writeToNBTOptional}), so the only copy is the one rebuilt from the mob. {@code EntityHanging#onUpdate} then
 * re-validates that knot against {@code onValidSurface}, a plain {@code getBlock().getRenderType() == 11}
 * check that reports air while the chunk holding the fence is not resident, and kills it.</li>
 * </ul>
 *
 * <p>
 * So a mob that ticks before the world can hand it a usable anchor is unrecoverable as far as vanilla is
 * concerned, and "the fence is not loaded yet" is indistinguishable from "the fence is gone". This class adds
 * a short grace window on top:
 *
 * <ul>
 * <li>the anchor is remembered past the single tick vanilla gives it, and the anchor is looked up again every
 * tick of the window, which covers an anchor that only becomes resolvable a few ticks late;</li>
 * <li>{@code clearLeashed} is a no-op for a restored mob whose anchor is merely missing or dead while the
 * window lasts, so nothing is destroyed and no lead item is dropped just because the mob was ticked first. A
 * mob whose fence really is gone behaves exactly like vanilla once the window closes;</li>
 * <li>the remembered anchor is written back to the mob's NBT, so the unrecoverable "flagged as leashed but
 * no anchor to resolve it from" NBT can never reach disk.</li>
 * </ul>
 *
 * <p>
 * Entries only exist for mobs restored while leashed, a handful at most, and the map is weak-keyed so
 * unloaded mobs stay collectable. This lives outside the mixins package (see {@link DistantHorizonsDimensionFilter})
 * so it is not treated as a mixin itself.
 */
public final class LeashLoadGrace {

    /** Block render type a lead can be tied to; matches {@code ItemLead#onItemUse} and {@code BlockFence}. */
    private static final int RENDER_TYPE_LEASHABLE = 11;

    private static final Map<EntityLiving, State> PENDING = Collections.synchronizedMap(new WeakHashMap<>());

    private static final class State {

        /** The raw {@code Leash} compound the mob was loaded with, or null if it had none. */
        private final NBTTagCompound anchor;

        /** Ticks left during which a missing anchor may not destroy the lead. */
        private int graceTicks;

        State(NBTTagCompound anchor, int graceTicks) {
            this.anchor = anchor;
            this.graceTicks = graceTicks;
        }
    }

    private LeashLoadGrace() {}

    /**
     * Starts the grace window for a mob that has just been restored from NBT.
     *
     * @param mob           the mob that was read from NBT
     * @param stashedAnchor the anchor vanilla stashed in {@code EntityLiving.field_110170_bx}, or null when the
     *                      NBT carried no usable {@code Leash} tag
     */
    public static void onEntityLoaded(EntityLiving mob, NBTTagCompound stashedAnchor) {
        if (isClient(mob)) {
            return;
        }

        if (!mob.getLeashed()) {
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
        if (isClient(mob)) {
            return;
        }

        State state = PENDING.get(mob);
        if (state == null) {
            return;
        }

        if (!mob.getLeashed()) {
            PENDING.remove(mob);
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
    }

    /**
     * Decides whether a lead may be broken off a restored mob, called at the head of {@code
     * EntityLiving#clearLeashed}, which every automatic break funnels through (a lost anchor from {@code
     * EntityLiving#updateLeashedState}, being out of range from {@code EntityCreature#updateLeashedState}, the
     * lead being taken off in {@code NetHandlerPlayClient}).
     *
     * <p>
     * Only a mob whose anchor is gone or dead is ever deferred. Those are exactly the load-order failures: a
     * player taking a lead off by hand always has a live anchor (themselves, or the knot they clicked), and a
     * mob that simply wandered too far is left on vanilla's behaviour, so this cannot be used to farm lead
     * items.
     *
     * @return {@code true} to cancel {@code clearLeashed}, keeping the lead and dropping nothing
     */
    public static boolean deferAnchorLoss(EntityLiving mob) {
        if (isClient(mob) || !mob.getLeashed()) {
            return false;
        }

        State state = PENDING.get(mob);
        if (state == null || state.graceTicks <= 0) {
            return false;
        }

        Entity anchor = mob.getLeashedToEntity();
        if (anchor != null && !anchor.isDead) {
            return false;
        }

        // Best effort: normally the fence is right there and this succeeds on the mob's first tick, in which
        // case the mob is anchored again and the grace window is dropped by tickLeash() on the next tick.
        reattach(mob, state.anchor);
        return true;
    }

    /**
     * Writes the remembered anchor into the mob's NBT when vanilla could not. Vanilla only writes the
     * {@code Leash} tag when {@code leashedToEntity} is non-null, so a mob that is flagged as leashed but has
     * no anchor is saved as {@code Leashed:true} with nothing to resolve it from and drops a lead on its very
     * first tick after the next load.
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

        State state = PENDING.get(mob);
        if (state == null || state.anchor == null || state.anchor.hasNoTags()) {
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
        if (anchor == null) {
            return false;
        }

        // Mobs on a lead carry a UUID, not a fence position; vanilla's own lookup is good enough for those.
        if (!anchor.hasKey("X", 99) || !anchor.hasKey("Y", 99) || !anchor.hasKey("Z", 99)) {
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

    private static boolean isClient(EntityLiving mob) {
        return mob.worldObj == null || mob.worldObj.isRemote;
    }
}
