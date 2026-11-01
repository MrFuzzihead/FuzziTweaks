package com.mrfuzzihead.fuzzitweaks.common;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.material.Material;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import com.mrfuzzihead.fuzzitweaks.Config;
import com.mrfuzzihead.fuzzitweaks.common.util.ServerModPresence;

import cpw.mods.fml.common.eventhandler.Event.Result;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * Opens both leaves of a double door when either one is right-clicked.
 *
 * <p>
 * Ported from LegacyFixes (MIT, unilock). The original found the other leaf from the door's facing and hinge
 * metadata and then called {@code BlockDoor#onBlockActivated} directly. That works, but a direct call bypasses
 * every other {@code PlayerInteractEvent} handler, which means a claim or protection mod can deny the clicked
 * leaf while the second leaf is forced open anyway. This version posts a real event for the second leaf first and
 * only activates it when that event survives, so protection handlers see both halves of the interaction. A
 * reentrancy flag keeps the synthetic event from being handled by this class again.
 *
 * <p>
 * The server-side activation arrives one network round-trip after the clicked leaf's client prediction, so on its
 * own the pair would visibly open one leaf at a time. The client therefore predicts the second leaf's metadata in
 * the same frame. Prediction is only safe when the server also runs FuzziTweaks, so it is gated on the FML handshake
 * mod list ({@link ServerModPresence}). When the server denies the second leaf, has the option disabled, or the
 * activation returns false, the server sends the unchanged block state back and the client's prediction is undone.
 *
 * <p>
 * Iron doors are skipped (they are redstone-only and {@code onBlockActivated} rejects them); sneaking players are
 * skipped so they can still place blocks against a door. A neighbouring door is only treated as the other leaf when
 * it is the exact same block, faces the same way, is in the same open state, and has the opposite hinge side - a door
 * that fails any of those checks is a separate door and is left alone.
 */
public final class DoubleDoors {

    /** Bits 0-1 of {@code BlockDoor#func_150012_g} are the door facing. */
    private static final int DOOR_DIRECTION_MASK = 3;

    /** Bit 2 of {@code BlockDoor#func_150012_g} is the open flag. */
    private static final int DOOR_OPEN_BIT = 4;

    /** Bit 4 of {@code BlockDoor#func_150012_g} is the hinge side. */
    private static final int DOOR_HINGE_BIT = 16;

    private static final DoubleDoors INSTANCE = new DoubleDoors();

    /** Set while the synthetic partner event is being posted; prevents this handler from re-entering itself. */
    private boolean handling;

    private DoubleDoors() {}

    /**
     * Registers the interaction handler. This is registered even when the option is disabled because the server
     * uses it to undo a client prediction when a client has the option on but the server does not.
     */
    public static void register() {
        MinecraftForge.EVENT_BUS.register(INSTANCE);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (this.handling) {
            return;
        }
        if (event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        final World world = event.world;
        if (world == null || event.entityPlayer == null) {
            return;
        }
        // On a client with the option off there is nothing to predict (and no server-side revert to do).
        if (world.isRemote && !Config.enableDoubleDoors) {
            return;
        }

        final Block clicked = world.getBlock(event.x, event.y, event.z);
        if (!(clicked instanceof BlockDoor)) {
            return;
        }
        final BlockDoor door = (BlockDoor) clicked;
        if (door.getMaterial() == Material.iron) {
            return;
        }

        final int[] partner = findPartner(world, event, door);
        if (partner == null) {
            return;
        }

        // Server with the option off: a client that has the option on may have predicted the second leaf, so put
        // the authoritative block state back. This is a no-op visual update for clients that did not predict.
        if (!Config.enableDoubleDoors) {
            if (!world.isRemote) {
                revertPartnerPrediction(world, partner[0], event.y, partner[1]);
            }
            return;
        }

        final boolean denied = event.isCanceled() || event.getResult() == Result.DENY || event.useBlock == Result.DENY;
        final boolean sneaking = event.entityPlayer.isSneaking();

        if (world.isRemote) {
            // Predict the second leaf in the same frame as the clicked leaf. Only do this when the server has the
            // mod; otherwise the predicted leaf would never be confirmed and would stay open.
            if (!denied && !sneaking && ServerModPresence.hasFuzziTweaks()) {
                predictPartnerToggle(world, door, partner[0], event.y, partner[1]);
            }
            return;
        }

        if (denied) {
            // A client may have predicted before the server-side handler denied the interaction.
            revertPartnerPrediction(world, partner[0], event.y, partner[1]);
            return;
        }
        if (sneaking) {
            return;
        }

        this.handling = true;
        try {
            // Post the event instead of calling onBlockActivated directly, so claim and protection handlers can
            // deny the second leaf just like they can deny the clicked one.
            final PlayerInteractEvent partnerEvent = new PlayerInteractEvent(
                event.entityPlayer,
                PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK,
                partner[0],
                event.y,
                partner[1],
                event.face,
                world);
            MinecraftForge.EVENT_BUS.post(partnerEvent);
            if (partnerEvent.isCanceled() || partnerEvent.getResult() == Result.DENY
                || partnerEvent.useBlock == Result.DENY) {
                revertPartnerPrediction(world, partner[0], event.y, partner[1]);
                return;
            }

            if (!door.onBlockActivated(
                world,
                partner[0],
                event.y,
                partner[1],
                event.entityPlayer,
                partnerEvent.face,
                0.0F,
                0.0F,
                0.0F)) {
                revertPartnerPrediction(world, partner[0], event.y, partner[1]);
            }
        } finally {
            this.handling = false;
        }
    }

    /**
     * Finds the other leaf and verifies that it is really the pair of the clicked door rather than a separate door
     * that happens to be adjacent.
     *
     * @return {@code {x, z}} of the other leaf, or {@code null}
     */
    private static int[] findPartner(World world, PlayerInteractEvent event, BlockDoor door) {
        // func_150012_g merges the upper/lower half metadata, so this works when either half is clicked.
        final int metadata = door.func_150012_g(world, event.x, event.y, event.z);
        final int direction = metadata & DOOR_DIRECTION_MASK;
        final int[] offset = partnerOffset(direction, (metadata & DOOR_HINGE_BIT) != 0);
        if (offset == null) {
            return null;
        }

        final int otherX = event.x + offset[0];
        final int otherZ = event.z + offset[1];
        if (world.getBlock(otherX, event.y, otherZ) != door) {
            return null;
        }

        final int otherMetadata = door.func_150012_g(world, otherX, event.y, otherZ);
        if ((otherMetadata & DOOR_DIRECTION_MASK) != direction) {
            return null;
        }
        if ((otherMetadata & DOOR_OPEN_BIT) != (metadata & DOOR_OPEN_BIT)) {
            return null;
        }
        if ((otherMetadata & DOOR_HINGE_BIT) == (metadata & DOOR_HINGE_BIT)) {
            return null;
        }

        return new int[] { otherX, otherZ };
    }

    /**
     * Toggles the other leaf's lower-half metadata on the client, mirroring {@code BlockDoor#onBlockActivated} but
     * without the sound. The server's packet carries the sound and the authoritative state.
     */
    private static void predictPartnerToggle(World world, BlockDoor door, int x, int y, int z) {
        final int newLowerMetadata = (door.func_150012_g(world, x, y, z) & 7) ^ DOOR_OPEN_BIT;
        if ((world.getBlockMetadata(x, y, z) & 8) == 0) {
            world.setBlockMetadataWithNotify(x, y, z, newLowerMetadata, 2);
            world.markBlockRangeForRenderUpdate(x, y, z, x, y, z);
        } else {
            world.setBlockMetadataWithNotify(x, y - 1, z, newLowerMetadata, 2);
            world.markBlockRangeForRenderUpdate(x, y - 1, z, x, y, z);
        }
    }

    /**
     * Sends the server's current state for the other leaf's lower half back to the tracking clients. Used when a
     * client predicted the leaf open but the server did not actually change it.
     */
    private static void revertPartnerPrediction(World world, int x, int y, int z) {
        final int lowerY = (world.getBlockMetadata(x, y, z) & 8) != 0 ? y - 1 : y;
        world.markBlockForUpdate(x, lowerY, z);
    }

    /**
     * Horizontal offset from one leaf to the other. Vanilla door facings 0/2 run along Z and 1/3 along X; the
     * hinge bit selects which side of that axis the other leaf is on.
     *
     * @return {@code {dx, dz}}, or {@code null} when the facing value is not a valid door direction
     */
    static int[] partnerOffset(int direction, boolean mirrored) {
        final int step = mirrored ? -1 : 1;
        switch (direction) {
            case 0:
                return new int[] { 0, step };
            case 1:
                return new int[] { -step, 0 };
            case 2:
                return new int[] { 0, -step };
            case 3:
                return new int[] { step, 0 };
            default:
                return null;
        }
    }
}
