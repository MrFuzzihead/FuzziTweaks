package com.mrfuzzihead.fuzzitweaks.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

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
 * goal matchers once at startup and bails out on the first {@code EntityJoinWorldEvent} (items, XP
 * orbs, projectiles, ...) instead of walking the task list, and it does not register a listener at all
 * when both options are disabled.
 *
 * <p>
 * Both goals are purely visual - {@code EntityAIWatchClosest} turns the head towards the closest player,
 * {@code EntityAILookIdle} towards a random nearby point - so removing them costs nothing gameplay-wise,
 * but it also disables head tracking for the affected mobs.
 */
public final class AIGoalStripper {

    private static final AIGoalStripper INSTANCE = new AIGoalStripper();

    /**
     * Classes whose goal instances (or subclasses, matching the original's {@code instanceof} behaviour)
     * are removed on join. Empty while both options are disabled.
     */
    private static List<Class<?>> goalMatchers = Collections.emptyList();

    private AIGoalStripper() {}

    /**
     * Resolves the enabled goal matchers and registers the join handler when there is something to strip.
     * Call once during pre-init, after the config has been synchronized.
     */
    public static void register() {
        final List<Class<?>> matchers = new ArrayList<>(2);
        if (Config.removeLookAtPlayerGoal) {
            matchers.add(EntityAIWatchClosest.class);
        }
        if (Config.removeLookIdleGoal) {
            matchers.add(EntityAILookIdle.class);
        }

        goalMatchers = matchers;

        if (!matchers.isEmpty()) {
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

            for (int i = 0; i < goalMatchers.size(); i++) {
                if (goalMatchers.get(i)
                    .isAssignableFrom(action.getClass())) {
                    iterator.remove();
                    break;
                }
            }
        }
    }
}
