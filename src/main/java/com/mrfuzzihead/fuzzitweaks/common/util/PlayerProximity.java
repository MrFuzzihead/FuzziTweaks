package com.mrfuzzihead.fuzzitweaks.common.util;

import net.minecraft.entity.EntityLiving;

import com.mrfuzzihead.fuzzitweaks.Config;

/**
 * Cheap "is a player close enough to see this?" gate, used to skip purely visual AI goals.
 *
 * <p>
 * The look goals only move a mob's head ({@code EntityAIWatchClosest} towards the closest player,
 * {@code EntityAILookIdle} towards a random nearby spot), so no player can tell whether they ran unless a
 * player is nearby. Skipping them further away therefore costs nothing visually and saves the goal
 * evaluation plus, for the goals that watch non-player classes, a full AABB entity scan.
 *
 * <p>
 * The check is a squared-distance comparison against the world's player list, the same call vanilla
 * {@code EntityAIWatchClosest} already makes for its own search.
 */
public final class PlayerProximity {

    private PlayerProximity() {}

    /**
     * @return {@code true} when the entity should keep running its visual goals: a player is within
     *         {@link Config#lookGoalPlayerRange} blocks, or the entity is on a client world (1.7.10 runs
     *         mob AI on the logical server, so a client world means something else is driving the goals -
     *         leave that alone).
     */
    public static boolean isAnyPlayerWithinRange(EntityLiving entity) {
        if (entity.worldObj.isRemote) {
            return true;
        }

        return entity.worldObj.getClosestPlayerToEntity(entity, Config.lookGoalPlayerRange) != null;
    }
}
