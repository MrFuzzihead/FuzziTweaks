package com.mrfuzzihead.fuzzitweaks.mixins.early;

import net.minecraft.block.BlockChest;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Careful Cast Corrector, {@code BlockChest} half.
 *
 * <p>
 * {@code onNeighborBlockChange} casts the result of {@code World.getTileEntity} straight to
 * {@code TileEntityChest}. A mod that replaces the tile entity at a chest position (or removes it without
 * removing the block) makes that cast throw {@code ClassCastException} during an ordinary neighbour update.
 * CCC rewrote the cast into an {@code instanceof} check; since a {@code CHECKCAST} cannot be removed with a
 * regular injector, this redirects the lookup that feeds it and hands back {@code null} for a foreign tile
 * entity. {@code checkcast null} succeeds and the vanilla {@code != null} guard then simply skips the
 * block-specific update, which is exactly what CCC did.
 */
@Mixin(BlockChest.class)
public abstract class BlockChestMixin {

    @Redirect(
        method = "onNeighborBlockChange",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/World;getTileEntity(III)Lnet/minecraft/tileentity/TileEntity;"),
        require = 1)
    private TileEntity fuzziTweaks$safeChestLookup(World world, int x, int y, int z) {
        final TileEntity tileEntity = world.getTileEntity(x, y, z);
        return tileEntity instanceof TileEntityChest ? tileEntity : null;
    }
}
