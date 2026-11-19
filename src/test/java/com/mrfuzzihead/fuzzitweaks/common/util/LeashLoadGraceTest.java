package com.mrfuzzihead.fuzzitweaks.common.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLeashKnot;
import net.minecraft.entity.EntityLiving;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mrfuzzihead.fuzzitweaks.Config;

/**
 * Exercises the lead grace-window decisions against mocked Minecraft objects.
 *
 * <p>
 * The class only reads a handful of things from the mob and the world - the leash flag, the live anchor, whether
 * the fence's chunk is resident and the block's render type - so a mocked {@link EntityLiving} and {@link World}
 * reproduce every branch the mixin reaches. Only the "world is remote" early return needs a real client world and
 * is left to the game.
 * </p>
 *
 * <p>
 * The pending-state map is keyed by entity identity, so each test gets a fresh mob mock; nothing has to be reset
 * between tests beyond the config value.
 * </p>
 */
class LeashLoadGraceTest {

    private static final int FENCE_X = 0;
    private static final int FENCE_Y = 64;
    private static final int FENCE_Z = 0;
    private static final int RENDER_TYPE_LEASHABLE = 11;

    private World world;
    private EntityLiving mob;
    private int savedGraceTicks;

    @BeforeEach
    void setUpFixture() {
        this.savedGraceTicks = Config.leashLoadGraceTicks;
        Config.leashLoadGraceTicks = 3;
        this.world = mock(World.class);
        this.mob = mock(EntityLiving.class);
        this.mob.worldObj = this.world;
    }

    @AfterEach
    void restoreGraceTicks() {
        Config.leashLoadGraceTicks = this.savedGraceTicks;
    }

    @Test
    void ignoresMobsThatAreNotLeashed() {
        this.mob.getLeashed();

        LeashLoadGrace.onEntityLoaded(this.mob, fenceAnchor());

        assertFalse(LeashLoadGrace.deferAnchorLoss(this.mob, true));
    }

    @Test
    void ignoresTheOptionWhenTheGraceWindowIsDisabled() {
        Config.leashLoadGraceTicks = 0;
        when(this.mob.getLeashed()).thenReturn(Boolean.TRUE);

        LeashLoadGrace.onEntityLoaded(this.mob, fenceAnchor());

        assertFalse(LeashLoadGrace.deferAnchorLoss(this.mob, true));
    }

    @Test
    void holdsTheLeadWhileTheFenceChunkIsNotResident() {
        trackRestoredMob();
        when(this.world.blockExists(FENCE_X, FENCE_Y, FENCE_Z)).thenReturn(Boolean.FALSE);

        assertTrue(LeashLoadGrace.deferAnchorLoss(this.mob, true));
        verify(this.mob, never()).setLeashedToEntity(any(Entity.class), eq(true));
    }

    @Test
    void breaksTheLeadOnceTheGraceWindowHasElapsed() {
        trackRestoredMob();
        when(this.world.blockExists(FENCE_X, FENCE_Y, FENCE_Z)).thenReturn(Boolean.FALSE);

        for (int tick = 0; tick < 3; tick++) {
            LeashLoadGrace.tickLeash(this.mob);
        }

        assertFalse(LeashLoadGrace.deferAnchorLoss(this.mob, true));
        // The forgotten anchor must not be written back on the next save.
        final NBTTagCompound tag = new NBTTagCompound();
        LeashLoadGrace.writeLeashAnchor(this.mob, tag);
        assertFalse(tag.hasKey("Leash"));
    }

    @Test
    void forgetsTheAnchorAsSoonAsVanillaResolvesOne() {
        trackRestoredMob();
        final Entity anchor = mock(Entity.class);
        when(this.mob.getLeashedToEntity()).thenReturn(anchor);

        LeashLoadGrace.tickLeash(this.mob);
        assertFalse(LeashLoadGrace.deferAnchorLoss(this.mob, true));

        // onAnchorResolved is the same decision, taken right after vanilla's recreateLeash.
        LeashLoadGrace.onEntityLoaded(this.mob, fenceAnchor());
        LeashLoadGrace.onAnchorResolved(this.mob);
        assertFalse(LeashLoadGrace.deferAnchorLoss(this.mob, true));
    }

    @Test
    void keepsTheAnchorWhenNoLiveAnchorExists() {
        trackRestoredMob();
        when(this.world.blockExists(FENCE_X, FENCE_Y, FENCE_Z)).thenReturn(Boolean.FALSE);

        LeashLoadGrace.onAnchorResolved(this.mob);

        assertTrue(LeashLoadGrace.deferAnchorLoss(this.mob, true));
    }

    @Test
    void neverDefersAPlayerOrCommandDetach() {
        trackRestoredMob();
        when(this.world.blockExists(FENCE_X, FENCE_Y, FENCE_Z)).thenReturn(Boolean.FALSE);

        assertFalse(LeashLoadGrace.deferAnchorLoss(this.mob, false));
        // The state is dropped with the detach, so a later automatic break is vanilla's.
        assertFalse(LeashLoadGrace.deferAnchorLoss(this.mob, true));
    }

    @Test
    void breaksTheLeadWhenTheResidentFenceIsGone() {
        trackRestoredMob();
        stubResidentFence(0);

        assertFalse(LeashLoadGrace.deferAnchorLoss(this.mob, true));
    }

    @Test
    void reattachesToAResidentFence() {
        trackRestoredMob();
        stubResidentFence(RENDER_TYPE_LEASHABLE);
        final EntityLeashKnot knot = mock(EntityLeashKnot.class);
        // getKnotForBlock matches the hanging's public block position, which a mock leaves at 0/0/0.
        knot.field_146063_b = FENCE_X;
        knot.field_146064_c = FENCE_Y;
        knot.field_146062_d = FENCE_Z;
        when(this.world.getEntitiesWithinAABB(eq(EntityLeashKnot.class), any(AxisAlignedBB.class)))
            .thenReturn(Collections.singletonList(knot));

        assertTrue(LeashLoadGrace.deferAnchorLoss(this.mob, true));
        verify(this.mob).setLeashedToEntity(knot, true);
        // A resolved anchor is forgotten, so breaking the knot by hand later is not undone.
        assertFalse(LeashLoadGrace.deferAnchorLoss(this.mob, true));
    }

    @Test
    void writesThePendingAnchorBackToNbt() {
        trackRestoredMob();
        when(this.world.blockExists(FENCE_X, FENCE_Y, FENCE_Z)).thenReturn(Boolean.FALSE);
        final NBTTagCompound tag = new NBTTagCompound();

        LeashLoadGrace.writeLeashAnchor(this.mob, tag);

        assertTrue(
            tag.getCompoundTag("Leash")
                .hasKey("X"));
        assertTrue(
            tag.getCompoundTag("Leash")
                .hasKey("Y"));
        assertTrue(
            tag.getCompoundTag("Leash")
                .hasKey("Z"));
    }

    @Test
    void leavesAnAnchorVanillaAlreadyWroteAlone() {
        trackRestoredMob();
        final NBTTagCompound tag = new NBTTagCompound();
        final NBTTagCompound vanillaAnchor = fenceAnchor();
        vanillaAnchor.setInteger("X", FENCE_X + 1);
        tag.setTag("Leash", vanillaAnchor);

        LeashLoadGrace.writeLeashAnchor(this.mob, tag);

        assertTrue(
            tag.getCompoundTag("Leash")
                .getInteger("X") == FENCE_X + 1);
    }

    @Test
    void doesNotPreserveALeadWhoseFenceIsKnownToBeGone() {
        trackRestoredMob();
        stubResidentFence(0);
        final NBTTagCompound tag = new NBTTagCompound();

        LeashLoadGrace.writeLeashAnchor(this.mob, tag);

        assertFalse(tag.hasKey("Leash"));
    }

    @Test
    void doesNotPreserveALeadThatAlreadyHasALiveAnchor() {
        trackRestoredMob();
        when(this.mob.getLeashedToEntity()).thenReturn(mock(Entity.class));
        final NBTTagCompound tag = new NBTTagCompound();

        LeashLoadGrace.writeLeashAnchor(this.mob, tag);

        assertFalse(tag.hasKey("Leash"));
    }

    @Test
    void ignoresUuidAnchors() {
        when(this.mob.getLeashed()).thenReturn(Boolean.TRUE);
        final NBTTagCompound anchor = new NBTTagCompound();
        anchor.setLong("UUIDMost", 1L);
        anchor.setLong("UUIDLeast", 2L);
        anchor.setInteger("X", FENCE_X);
        anchor.setInteger("Y", FENCE_Y);
        anchor.setInteger("Z", FENCE_Z);

        LeashLoadGrace.onEntityLoaded(this.mob, anchor);

        assertFalse(LeashLoadGrace.deferAnchorLoss(this.mob, true));
    }

    @Test
    void ignoresEmptyAndMalformedAnchors() {
        when(this.mob.getLeashed()).thenReturn(Boolean.TRUE);

        LeashLoadGrace.onEntityLoaded(this.mob, null);
        assertFalse(LeashLoadGrace.deferAnchorLoss(this.mob, true));

        LeashLoadGrace.onEntityLoaded(this.mob, new NBTTagCompound());
        assertFalse(LeashLoadGrace.deferAnchorLoss(this.mob, true));

        final NBTTagCompound noPosition = new NBTTagCompound();
        noPosition.setInteger("X", FENCE_X);
        LeashLoadGrace.onEntityLoaded(this.mob, noPosition);
        assertFalse(LeashLoadGrace.deferAnchorLoss(this.mob, true));
    }

    @Test
    void keepsTickingWhileTheAnchorStillHasNoKnot() {
        // A resident fence without any knot entity: the knot can only be created by EntityLeashKnot itself, which
        // needs a real world, so the reattach fails and the lead is held until the window closes.
        trackRestoredMob();
        stubResidentFence(RENDER_TYPE_LEASHABLE);
        when(this.world.getEntitiesWithinAABB(eq(EntityLeashKnot.class), any(AxisAlignedBB.class))).thenReturn(null);

        LeashLoadGrace.tickLeash(this.mob);

        verify(this.mob, times(0)).setLeashedToEntity(any(Entity.class), eq(true));
    }

    private void trackRestoredMob() {
        when(this.mob.getLeashed()).thenReturn(Boolean.TRUE);
        LeashLoadGrace.onEntityLoaded(this.mob, fenceAnchor());
    }

    private void stubResidentFence(int renderType) {
        when(this.world.blockExists(FENCE_X, FENCE_Y, FENCE_Z)).thenReturn(Boolean.TRUE);
        final Block block = mock(Block.class);
        when(block.getRenderType()).thenReturn(renderType);
        when(this.world.getBlock(anyInt(), anyInt(), anyInt())).thenReturn(block);
    }

    private static NBTTagCompound fenceAnchor() {
        final NBTTagCompound anchor = new NBTTagCompound();
        anchor.setInteger("X", FENCE_X);
        anchor.setInteger("Y", FENCE_Y);
        anchor.setInteger("Z", FENCE_Z);
        return anchor;
    }
}
