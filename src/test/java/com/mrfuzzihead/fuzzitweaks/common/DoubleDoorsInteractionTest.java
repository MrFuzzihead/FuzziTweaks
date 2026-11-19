package com.mrfuzzihead.fuzzitweaks.common;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;
import java.util.Collections;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mrfuzzihead.fuzzitweaks.Config;
import com.mrfuzzihead.fuzzitweaks.common.util.ServerModPresence;

import cpw.mods.fml.common.eventhandler.Event.Result;

/**
 * Exercises the decisions inside {@link DoubleDoors#onPlayerInteract} against mocked Minecraft objects.
 *
 * <p>
 * {@code DoubleDoorsTest} covers the pure facing/hinge table; everything else in the handler - is this really the
 * other leaf, is the interaction allowed, what happens when the option is off - only runs when a
 * {@code PlayerInteractEvent} is posted. The handler reads the block, its full metadata and the player from the
 * event, so a mocked {@link World}, {@link BlockDoor} and {@link EntityPlayer} are all it needs; the real
 * {@code Blocks.wooden_door} and its block-state rules are exercised by the in-game test mod instead.
 * </p>
 */
class DoubleDoorsInteractionTest {

    private static final int X = 10;
    private static final int Y = 64;
    private static final int Z = 10;
    /** Facing 0 with hinge 0 puts the other leaf at +Z (see {@code DoubleDoors#partnerOffset}). */
    private static final int PARTNER_X = X;
    private static final int PARTNER_Z = Z + 1;
    private static final int FACE = 1;
    private static final int DOOR_HINGE_BIT = 16;

    private World world;
    private BlockDoor door;
    private EntityPlayer player;
    private DoubleDoors handler;

    @BeforeEach
    void setUpFixture() throws Exception {
        Config.enableDoubleDoors = true;
        ServerModPresence.onHandshakeModList(Collections.emptyMap());

        // Registering the singleton on the real Forge bus needs FML's Loader, which a headless unit test does not
        // have; the handler is stateless apart from its reentrancy flag, so a fresh instance is equivalent.
        final Constructor<DoubleDoors> constructor = DoubleDoors.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        this.handler = constructor.newInstance();

        this.world = mock(World.class);
        this.door = mock(BlockDoor.class);
        this.player = mock(EntityPlayer.class);

        when(this.door.getMaterial()).thenReturn(Material.wood);
        when(
            this.door.onBlockActivated(
                any(),
                anyInt(),
                anyInt(),
                anyInt(),
                any(),
                anyInt(),
                anyFloat(),
                anyFloat(),
                anyFloat())).thenReturn(Boolean.TRUE);
        when(this.world.getBlock(anyInt(), anyInt(), anyInt())).thenReturn(this.door);
        when(this.player.isSneaking()).thenReturn(Boolean.FALSE);
    }

    @Test
    void opensThePartnerLeafThroughItsOwnEvent() {
        stubPair(this.door, DOOR_HINGE_BIT);

        postRightClick();

        verify(this.door).onBlockActivated(
            eq(this.world),
            eq(PARTNER_X),
            eq(Y),
            eq(PARTNER_Z),
            eq(this.player),
            eq(FACE),
            eq(0.0F),
            eq(0.0F),
            eq(0.0F));
    }

    @Test
    void leavesASeparateDoorAlone() {
        // Same hinge on both leaves: the neighbour is a separate door, not the other half.
        stubPair(this.door, 0);

        postRightClick();

        verify(this.door, never())
            .onBlockActivated(any(), anyInt(), anyInt(), anyInt(), any(), anyInt(), eq(0.0F), eq(0.0F), eq(0.0F));
        verify(this.world, never()).markBlockForUpdate(anyInt(), anyInt(), anyInt());
    }

    @Test
    void revertsThePredictionWhenTheOptionIsOff() {
        Config.enableDoubleDoors = false;
        stubPair(this.door, DOOR_HINGE_BIT);

        postRightClick();

        verify(this.door, never())
            .onBlockActivated(any(), anyInt(), anyInt(), anyInt(), any(), anyInt(), eq(0.0F), eq(0.0F), eq(0.0F));
        // The server sends the untouched state back so a client that predicted the leaf open undoes it.
        verify(this.world).markBlockForUpdate(PARTNER_X, Y, PARTNER_Z);
    }

    @Test
    void ignoresIronDoors() {
        when(this.door.getMaterial()).thenReturn(Material.iron);
        stubPair(this.door, DOOR_HINGE_BIT);

        postRightClick();

        verify(this.door, never())
            .onBlockActivated(any(), anyInt(), anyInt(), anyInt(), any(), anyInt(), eq(0.0F), eq(0.0F), eq(0.0F));
    }

    @Test
    void ignoresSneakingPlayers() {
        when(this.player.isSneaking()).thenReturn(Boolean.TRUE);
        stubPair(this.door, DOOR_HINGE_BIT);

        postRightClick();

        verify(this.door, never())
            .onBlockActivated(any(), anyInt(), anyInt(), anyInt(), any(), anyInt(), eq(0.0F), eq(0.0F), eq(0.0F));
    }

    /**
     * Makes the clicked leaf report facing 0, closed, hinge 0, and the neighbour the same door with the given hinge
     * bit. {@code func_150012_g} is the merged lower/upper metadata accessor the handler reads.
     */
    private void stubPair(BlockDoor door, int partnerHinge) {
        when(door.func_150012_g(any(), anyInt(), anyInt(), anyInt())).thenAnswer(invocation -> {
            final int z = (Integer) invocation.getArguments()[3];
            return z == PARTNER_Z ? partnerHinge : 0;
        });
    }

    private void postRightClick() {
        this.handler.onPlayerInteract(
            new PlayerInteractEvent(
                this.player,
                PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK,
                X,
                Y,
                Z,
                FACE,
                this.world));
    }

    @Test
    void ignoresEverythingThatIsNotARightClickOnADoor() {
        // Not a right click at all.
        this.handler.onPlayerInteract(
            new PlayerInteractEvent(
                this.player,
                PlayerInteractEvent.Action.LEFT_CLICK_BLOCK,
                X,
                Y,
                Z,
                FACE,
                this.world));

        // A right click, but the target is not a door.
        when(this.world.getBlock(anyInt(), anyInt(), anyInt())).thenReturn(mock(Block.class));
        postRightClick();

        verify(this.door, never())
            .onBlockActivated(any(), anyInt(), anyInt(), anyInt(), any(), anyInt(), eq(0.0F), eq(0.0F), eq(0.0F));
    }

    @Test
    void ignoresAnEventWithoutAWorld() {
        // Another mod's synthetic event can carry a null world; the handler must not touch the player through it.
        this.handler.onPlayerInteract(
            new PlayerInteractEvent(this.player, PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK, X, Y, Z, FACE, null));

        verify(this.door, never())
            .onBlockActivated(any(), anyInt(), anyInt(), anyInt(), any(), anyInt(), eq(0.0F), eq(0.0F), eq(0.0F));
    }

    @Test
    void doesNotOpenANeighbourThatIsADifferentDoor() {
        // Adjacent door blocks that are not the same pair (different block identity) are separate doors.
        when(this.world.getBlock(PARTNER_X, Y, PARTNER_Z)).thenReturn(mock(BlockDoor.class));
        stubPair(this.door, DOOR_HINGE_BIT);

        postRightClick();

        verify(this.door, never())
            .onBlockActivated(any(), anyInt(), anyInt(), anyInt(), any(), anyInt(), eq(0.0F), eq(0.0F), eq(0.0F));
    }

    @Test
    void revertsThePredictionWhenTheResultIsDenied() {
        stubPair(this.door, DOOR_HINGE_BIT);
        // PlayerInteractEvent is not cancelable in 1.7.10; a protection handler denies it through the result.
        final PlayerInteractEvent denied = interactEvent();
        denied.setResult(Result.DENY);

        this.handler.onPlayerInteract(denied);

        verify(this.world).markBlockForUpdate(PARTNER_X, Y, PARTNER_Z);
        verify(this.door, never())
            .onBlockActivated(any(), anyInt(), anyInt(), anyInt(), any(), anyInt(), eq(0.0F), eq(0.0F), eq(0.0F));
    }

    @Test
    void revertsThePredictionWhenAnotherHandlerDeniesTheBlockUse() {
        stubPair(this.door, DOOR_HINGE_BIT);
        final PlayerInteractEvent denied = interactEvent();
        denied.useBlock = Result.DENY;

        this.handler.onPlayerInteract(denied);

        verify(this.world).markBlockForUpdate(PARTNER_X, Y, PARTNER_Z);
    }

    @Test
    void revertsTheLowerHalfWhenThePartnerWasClickedAsItsUpperHalf() {
        Config.enableDoubleDoors = false;
        stubPair(this.door, DOOR_HINGE_BIT);
        // Bit 8 marks the clicked (and partner) block as the upper half, so the state lives one block down.
        when(this.world.getBlockMetadata(PARTNER_X, Y, PARTNER_Z)).thenReturn(8);

        postRightClick();

        verify(this.world).markBlockForUpdate(PARTNER_X, Y - 1, PARTNER_Z);
    }

    private PlayerInteractEvent interactEvent() {
        return new PlayerInteractEvent(
            this.player,
            PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK,
            X,
            Y,
            Z,
            FACE,
            this.world);
    }
}
