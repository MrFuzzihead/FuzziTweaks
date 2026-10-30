package com.mrfuzzihead.fuzzitweaks.common;

import java.util.Iterator;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;

import com.mrfuzzihead.fuzzitweaks.Config;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * Strips selected vanilla "look" AI goals from living entities as they join the world.
 *
 * <p>
 * Ported from AI Improvements (MIT, BuiltBrokenModding/DarkCow). The original removed the goals on
 * {@code EntityJoinWorldEvent} with an {@code instanceof} check per goal; this version keeps that
 * behaviour (including modded subclasses, for example {@code EntityAIWatchClosest2}) but resolves the
 * two option checks once at startup and bails out on the first {@code EntityJoinWorldEvent} (items, XP
 * orbs, projectiles, ...) instead of walking the task list, and it does not register a listener at all
 * when the main AI Improvements switch is disabled or both options are off.
 *
 * <p>
 * Both goals are purely visual - {@code EntityAIWatchClosest} turns the head towards the closest player,
 * {@code EntityAILookIdle} towards a random nearby point - so removing them costs nothing gameplay-wise,
 * but it also disables head tracking for the affected mobs.
 */
public final class AIGoalStripper {

    private static final AIGoalStripper INSTANCE = new AIGoalStripper();

    /** Cached option state; both false until the module is enabled and configured. */
    private static boolean stripWatchClosest;
    private static boolean stripLookIdle;

    private AIGoalStripper() {}

    /**
     * Resolves the enabled option checks and registers the join handler when the main switch is on and there is
     * something to strip. Call once during pre-init, after the config has been synchronized.
     */
    public static void register() {
        if (!Config.enableAIImprovementsMain) {
            return;
        }

        stripWatchClosest = Config.removeLookAtPlayerGoal;
        stripLookIdle = Config.removeLookIdleGoal;

        if (stripWatchClosest || stripLookIdle) {
            MinecraftForge.EVENT_BUS.register(INSTANCE);
        }
    }

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        // Fast-out for everything that is not a mob (items, XP orbs, projectiles, ...).
        if (!(event.entity instanceof EntityLiving)) {
            return;
        }

        strip(((EntityLiving) event.entity).tasks);
    }

    /**
     * Removes every task entry whose action matches one of the configured goals. The entity has not
     * ticked yet when this runs, so only {@code taskEntries} has to be cleaned up.
     */
    private static void strip(EntityAITasks tasks) {
        final Iterator<?> iterator = tasks.taskEntries.iterator();
        while (iterator.hasNext()) {
            final Object entry = iterator.next();
            if (!(entry instanceof EntityAITasks.EntityAITaskEntry)) {
                continue;
            }

            final EntityAIBase action = ((EntityAITasks.EntityAITaskEntry) entry).action;
            if (action == null) {
                continue;
            }

            // instanceof keeps the original behaviour for modded subclasses (for example
            // EntityAIWatchClosest2) while avoiding a per-task scan of a matcher list.
            if ((stripWatchClosest && action instanceof EntityAIWatchClosest)
                || (stripLookIdle && action instanceof EntityAILookIdle)) {
                iterator.remove();
            }
        }
    }
}
