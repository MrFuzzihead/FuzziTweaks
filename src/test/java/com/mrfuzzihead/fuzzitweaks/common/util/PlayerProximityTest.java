package com.mrfuzzihead.fuzzitweaks.common.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import org.junit.jupiter.api.Test;

import com.mrfuzzihead.fuzzitweaks.Config;

/**
 * Covers the "is anyone close enough to see this head movement" gate that the look-goal mixins consult. The client
 * half of the early return needs a client world, which a headless test cannot build; the server half is the one that
 * decides whether the goals are skipped.
 */
class PlayerProximityTest {

    @Test
    void returnsTrueWhenAPlayerIsInRange() {
        final EntityLiving entity = entityIn(mock(World.class));
        when(entity.worldObj.getClosestPlayerToEntity(entity, Config.lookGoalPlayerRange))
            .thenReturn(mock(EntityPlayer.class));

        assertTrue(PlayerProximity.isAnyPlayerWithinRange(entity));
    }

    @Test
    void returnsFalseWhenNoPlayerIsInRange() {
        final EntityLiving entity = entityIn(mock(World.class));
        when(entity.worldObj.getClosestPlayerToEntity(eq(entity), eq(Config.lookGoalPlayerRange))).thenReturn(null);

        assertFalse(PlayerProximity.isAnyPlayerWithinRange(entity));
    }

    /** The entity only needs a world; {@code worldObj} is a plain field, so a mock can be wired up directly. */
    private static EntityLiving entityIn(World world) {
        final EntityLiving entity = mock(EntityLiving.class);
        entity.worldObj = world;
        return entity;
    }
}
